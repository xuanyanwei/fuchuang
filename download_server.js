import http from 'http';
import fs from 'fs';
import path from 'path';

const PORT = 3000;
const APK_PATH = '/app/applet/app/build/outputs/apk/debug/app-debug.apk';

const server = http.createServer((req, res) => {
  const url = req.url || '/';

  // Direct APK download routes
  if (url === '/aurora-player.apk' || url === '/download-apk' || url.startsWith('/aurora-player.apk') || url.startsWith('/download-apk')) {
    if (fs.existsSync(APK_PATH)) {
      const stat = fs.statSync(APK_PATH);
      res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Length': stat.size,
        'Content-Disposition': 'attachment; filename="aurora-player.apk"; filename*=UTF-8\'\'%E6%9E%81%E5%85%89%E6%92%AD%E6%94%BE%E5%99%A8.apk',
        'Cache-Control': 'no-cache'
      });
      fs.createReadStream(APK_PATH).pipe(res);
      return;
    } else {
      res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('APK 正在生成中，请稍候刷新重试。');
      return;
    }
  }

  // Download landing page
  const html = `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>极光流媒体播放器 - APK 安装包直接下载</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background-color: #090A0E;
      color: #FFFFFF;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 24px 16px;
    }
    .card {
      background: #14161F;
      border: 1px solid #282B37;
      border-radius: 20px;
      max-width: 480px;
      width: 100%;
      padding: 36px 28px;
      text-align: center;
      box-shadow: 0 12px 40px rgba(0, 0, 0, 0.7);
    }
    .badge {
      display: inline-block;
      background: #FFA000;
      color: #000;
      font-weight: 800;
      font-size: 14px;
      padding: 4px 12px;
      border-radius: 6px;
      margin-bottom: 16px;
      letter-spacing: 1px;
    }
    h1 {
      font-size: 24px;
      font-weight: 700;
      margin-bottom: 8px;
    }
    .desc {
      color: #9096A4;
      font-size: 14px;
      line-height: 1.6;
      margin-bottom: 24px;
    }
    .info-box {
      background: #0F1017;
      border-radius: 12px;
      padding: 16px;
      margin-bottom: 24px;
      text-align: left;
      font-size: 13px;
    }
    .info-row {
      display: flex;
      justify-content: space-between;
      margin-bottom: 8px;
    }
    .info-row:last-child { margin-bottom: 0; }
    .info-label { color: #9096A4; }
    .info-val { color: #FFA000; font-weight: 600; }
    .btn-download {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      background: #FFA000;
      color: #000000;
      font-size: 17px;
      font-weight: 700;
      padding: 16px 24px;
      border-radius: 12px;
      text-decoration: none;
      transition: transform 0.15s, background-color 0.15s;
      box-shadow: 0 6px 20px rgba(255, 160, 0, 0.35);
    }
    .btn-download:hover {
      background: #FFB300;
      transform: translateY(-2px);
    }
    .btn-download:active {
      transform: translateY(1px);
    }
    .tips {
      margin-top: 20px;
      color: #656C7D;
      font-size: 12px;
      line-height: 1.5;
    }
  </style>
</head>
<body>
  <div class="card">
    <div class="badge">极光</div>
    <h1>极光流媒体播放器</h1>
    <p class="desc">专业流媒体与 M3U8 播放器，原生 Media3 硬解架构，支持自定义请求头与全屏沉浸播放。</p>

    <div class="info-box">
      <div class="info-row">
        <span class="info-label">安装包文件:</span>
        <span class="info-val">aurora-player.apk</span>
      </div>
      <div class="info-row">
        <span class="info-label">应用版本:</span>
        <span class="info-val">1.0.0 (Kotlin + Media3)</span>
      </div>
      <div class="info-row">
        <span class="info-label">构建架构:</span>
        <span class="info-val">Android 7.0 - Android 15+</span>
      </div>
      <div class="info-row">
        <span class="info-label">文件状态:</span>
        <span class="info-val" style="color: #10B981;">● 已就绪</span>
      </div>
    </div>

    <a href="/aurora-player.apk" class="btn-download" download="aurora-player.apk">
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
        <polyline points="7 10 12 15 17 10"></polyline>
        <line x1="12" y1="15" x2="12" y2="3"></line>
      </svg>
      立即下载 APK 安装包
    </a>

    <p class="tips">
      提示：下载完成后在手机通知栏或文件管理器中点击即可安装（若提示未知来源应用，点击允许即可）。
    </p>
  </div>
</body>
</html>`;

  res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
  res.end(html);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Download server running at http://0.0.0.0:${PORT}`);
});
