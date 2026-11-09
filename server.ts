import express from 'express';
import type { Request, Response } from 'express';
import cors from 'cors';
import path from 'path';
import { fileURLToPath } from 'url';
import { Readable } from 'stream';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = 3000;
const staticDir = path.join(__dirname, 'dist');

// Enable CORS for all incoming API requests
app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// 1. Fetch Playlist API (used by player to fetch M3U / M3U8 playlist content)
app.post('/api/fetch-playlist', async (req: Request, res: Response) => {
  try {
    const { url, headers: customHeaders } = req.body;
    if (!url || typeof url !== 'string') {
      return res.status(400).json({ error: 'Missing or invalid url parameter' });
    }

    const requestHeaders: Record<string, string> = {
      'User-Agent':
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
      Accept: '*/*',
    };

    if (customHeaders && typeof customHeaders === 'object') {
      Object.assign(requestHeaders, customHeaders);
    }

    const response = await fetch(url, {
      headers: requestHeaders,
      redirect: 'follow',
    });

    if (!response.ok) {
      return res.status(response.status).json({
        error: `Remote server responded with HTTP ${response.status} ${response.statusText}`,
      });
    }

    const content = await response.text();
    return res.json({ content, status: response.status });
  } catch (err: any) {
    console.error('Error in /api/fetch-playlist:', err);
    return res.status(500).json({
      error: err.message || 'Failed to fetch playlist',
    });
  }
});

// Helper function to rewrite M3U8 playlists for proxying
function rewriteM3u8Playlist(
  playlistText: string,
  baseUrl: string,
  customHeaders: Record<string, string>
): string {
  const lines = playlistText.split(/\r?\n/);
  const headersParam = Object.keys(customHeaders).length > 0
    ? `&headers=${encodeURIComponent(JSON.stringify(customHeaders))}`
    : '';

  const createProxyUrl = (targetUri: string) => {
    try {
      const resolved = new URL(targetUri, baseUrl).href;
      return `/api/proxy-stream?url=${encodeURIComponent(resolved)}${headersParam}`;
    } catch {
      return targetUri;
    }
  };

  const rewrittenLines = lines.map((line) => {
    const trimmed = line.trim();
    if (!trimmed) return line;

    // Check for tag lines that include URIs (e.g. #EXT-X-KEY or #EXT-X-MAP)
    if (trimmed.startsWith('#')) {
      if (trimmed.includes('URI=')) {
        return line.replace(/URI=["']([^"']+)["']/g, (_, uriMatch) => {
          return `URI="${createProxyUrl(uriMatch)}"`;
        });
      }
      return line;
    }

    // Media segment or variant playlist URI
    return createProxyUrl(trimmed);
  });

  return rewrittenLines.join('\n');
}

// 2. Proxy Stream API (proxies media segments, M3U8 streams, and video files with custom headers & CORS)
app.get('/api/proxy-stream', async (req: Request, res: Response) => {
  const targetUrl = req.query.url as string;
  if (!targetUrl) {
    return res.status(400).send('Missing url parameter');
  }

  let customHeaders: Record<string, string> = {};
  if (req.query.headers) {
    try {
      customHeaders = JSON.parse(req.query.headers as string);
    } catch (e) {
      console.warn('Failed to parse headers query parameter:', e);
    }
  }

  const forwardHeaders: Record<string, string> = {
    'User-Agent':
      'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    Accept: '*/*',
  };

  Object.assign(forwardHeaders, customHeaders);

  // Forward Range request if present (for seeking / video chunk buffering)
  if (req.headers.range) {
    forwardHeaders['Range'] = req.headers.range as string;
  }

  const abortController = new AbortController();
  req.on('close', () => {
    abortController.abort();
  });

  try {
    const upstreamRes = await fetch(targetUrl, {
      headers: forwardHeaders,
      redirect: 'follow',
      signal: abortController.signal,
    });

    const contentType = upstreamRes.headers.get('content-type') || '';
    const isM3u8 =
      contentType.includes('mpegurl') ||
      contentType.includes('x-mpegURL') ||
      targetUrl.toLowerCase().includes('.m3u8') ||
      targetUrl.toLowerCase().includes('.m3u');

    // Set CORS headers for player
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', '*');

    // Forward caching and content descriptors
    const headersToForward = [
      'content-range',
      'accept-ranges',
      'last-modified',
      'etag',
    ];

    for (const h of headersToForward) {
      const val = upstreamRes.headers.get(h);
      if (val) {
        res.setHeader(h, val);
      }
    }

    if (isM3u8) {
      const text = await upstreamRes.text();
      // If it looks like valid M3U8, rewrite it
      if (text.includes('#EXTM3U')) {
        const rewritten = rewriteM3u8Playlist(text, targetUrl, customHeaders);
        res.setHeader('Content-Type', 'application/vnd.apple.mpegurl');
        res.status(upstreamRes.status).send(rewritten);
        return;
      } else {
        res.setHeader('Content-Type', contentType || 'application/vnd.apple.mpegurl');
        res.status(upstreamRes.status).send(text);
        return;
      }
    }

    // Binary media stream (e.g. .ts, .mp4, .aac, .m4s)
    res.status(upstreamRes.status);
    if (contentType) {
      res.setHeader('Content-Type', contentType);
    }
    const contentLength = upstreamRes.headers.get('content-length');
    if (contentLength) {
      res.setHeader('Content-Length', contentLength);
    }

    if (upstreamRes.body) {
      const nodeStream = Readable.fromWeb(upstreamRes.body as any);
      nodeStream.pipe(res);
    } else {
      res.end();
    }
  } catch (err: any) {
    if (abortController.signal.aborted) {
      return;
    }
    console.error('Error proxying stream:', targetUrl, err.message);
    if (!res.headersSent) {
      res.status(502).send(`Error proxying stream: ${err.message}`);
    }
  }
});

// Explicit APK download routes
app.get('/aurora-player.apk', (_req: Request, res: Response) => {
  const apkPath = path.join(staticDir, 'aurora-player.apk');
  res.setHeader('Content-Type', 'application/vnd.android.package-archive');
  res.setHeader('Content-Disposition', 'attachment; filename="aurora-player.apk"; filename*=UTF-8\'\'%E6%9E%81%E5%85%89%E6%92%AD%E6%94%BE%E5%99%A8.apk');
  res.sendFile(apkPath);
});

app.get('/download-apk', (_req: Request, res: Response) => {
  const apkPath = path.join(staticDir, 'aurora-player.apk');
  res.setHeader('Content-Type', 'application/vnd.android.package-archive');
  res.setHeader('Content-Disposition', 'attachment; filename="aurora-player.apk"; filename*=UTF-8\'\'%E6%9E%81%E5%85%89%E6%92%AD%E6%94%BE%E5%99%A8.apk');
  res.sendFile(apkPath);
});

// Serve static frontend assets from /dist
app.use(express.static(staticDir));

// Fallback to index.html for SPA routes
app.use((_req: Request, res: Response) => {
  res.sendFile(path.join(staticDir, 'index.html'));
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Aurora Streaming Player server running on http://0.0.0.0:${PORT}`);
});
