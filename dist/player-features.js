// Player Features Extension: Picture-in-Picture, Fullscreen & Advanced Settings Drawer (Screenshot 4)
(function () {
  let sleepTimerId = null;
  let sleepTimerEndTime = null;
  let forceFill = false;
  let stretchCover = false;
  let autoOrientation = false;
  let loopPlayback = false;
  let randomPlayback = false;
  let autoNext = true;
  let selectedSpeed = 1.0;
  let sleepTimerLabel = '未设置';

  function getVideo() {
    return document.querySelector('video');
  }

  function getPlayerContainer() {
    const v = getVideo();
    if (!v) return null;
    return v.closest('.aspect-video') || v.parentElement;
  }

  // Apply display aspect ratio mode
  function updateObjectFit() {
    const v = getVideo();
    if (!v) return;
    if (forceFill) {
      v.style.objectFit = 'fill';
    } else if (stretchCover) {
      v.style.objectFit = 'cover';
    } else {
      v.style.objectFit = 'contain';
    }
  }

  // Fullscreen implementation
  window.togglePlayerFullscreen = function () {
    const container = getPlayerContainer() || getVideo();
    if (!container) return;

    if (window.Android && typeof window.Android.toggleFullscreen === 'function') {
      window.Android.toggleFullscreen();
    }

    if (document.fullscreenElement) {
      document.exitFullscreen().catch(() => {});
      container.classList.remove('web-fullscreen-mode');
    } else {
      if (container.requestFullscreen) {
        container.requestFullscreen().catch(() => {
          container.classList.toggle('web-fullscreen-mode');
        });
      } else {
        container.classList.toggle('web-fullscreen-mode');
      }
    }
  };

  // Picture-in-Picture implementation
  window.togglePlayerPiP = async function () {
    const v = getVideo();
    if (!v) return;

    if (window.Android && typeof window.Android.enterPip === 'function') {
      window.Android.enterPip();
      return;
    }

    try {
      if (document.pictureInPictureElement) {
        await document.exitPictureInPicture();
      } else if (document.pictureInPictureEnabled && v.requestPictureInPicture) {
        await v.requestPictureInPicture();
      } else {
        showToast('当前设备环境不支持画中画API');
      }
    } catch (e) {
      console.warn('PiP error:', e);
      showToast('无法启动画中画: ' + (e.message || ''));
    }
  };

  // Simple toast notification
  function showToast(msg) {
    let toast = document.getElementById('aurora-toast');
    if (!toast) {
      toast = document.createElement('div');
      toast.id = 'aurora-toast';
      toast.style.cssText =
        'position: fixed; top: 24px; left: 50%; transform: translateX(-50%); background: rgba(20,20,24,0.95); color: #f8fafc; border: 1px solid rgba(245,158,11,0.4); padding: 8px 16px; border-radius: 9999px; font-size: 13px; z-index: 9999999; box-shadow: 0 10px 25px rgba(0,0,0,0.5); pointer-events: none; transition: opacity 0.3s;';
      document.body.appendChild(toast);
    }
    toast.textContent = msg;
    toast.style.opacity = '1';
    clearTimeout(toast._timer);
    toast._timer = setTimeout(() => {
      toast.style.opacity = '0';
    }, 2500);
  }

  // Create & mount Settings Panel matching Screenshot 4
  function createSettingsDrawer() {
    if (document.getElementById('aurora-settings-drawer')) return;

    const drawer = document.createElement('div');
    drawer.id = 'aurora-settings-drawer';
    drawer.style.cssText =
      'display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.65); z-index: 999999; backdrop-filter: blur(10px); -webkit-backdrop-filter: blur(10px); align-items: flex-end; justify-content: center; font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;';

    drawer.innerHTML = `
      <div id="aurora-settings-content" style="width: 100%; max-width: 540px; background: rgba(18, 18, 22, 0.96); border-top-left-radius: 20px; border-top-right-radius: 20px; border: 1px solid rgba(255,255,255,0.1); border-bottom: none; box-shadow: 0 -10px 30px rgba(0,0,0,0.8); max-height: 85vh; overflow-y: auto; padding: 20px 20px 30px; color: #e2e8f0;">
        <!-- Header -->
        <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid rgba(255,255,255,0.08); padding-bottom: 14px; margin-bottom: 16px;">
          <div style="display: flex; align-items: center; gap: 8px;">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#f59e0b" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
            <span style="font-size: 16px; font-weight: 600; color: #fff;">播放器设置</span>
          </div>
          <button id="close-settings-btn" style="background: none; border: none; color: #94a3b8; font-size: 22px; cursor: pointer; padding: 4px 8px;">✕</button>
        </div>

        <!-- 1. 显示 (Display) -->
        <div style="font-size: 13px; color: #94a3b8; font-weight: 500; margin-bottom: 10px;">显示</div>
        <div style="display: flex; flex-direction: column; gap: 4px; margin-bottom: 18px;">
          <!-- 强制铺满全屏 -->
          <div style="display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; background: rgba(255,255,255,0.03); border-radius: 10px;">
            <div style="display: flex; align-items: center; gap: 10px;">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/></svg>
              <span style="font-size: 14px; color: #f1f5f9;">强制铺满全屏</span>
            </div>
            <label class="aurora-switch">
              <input type="checkbox" id="toggle-force-fill">
              <span class="aurora-slider"></span>
            </label>
          </div>

          <!-- 拉伸以铺满全屏 -->
          <div style="display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; background: rgba(255,255,255,0.03); border-radius: 10px;">
            <div style="display: flex; align-items: center; gap: 10px;">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 3 21 3 21 9"/><polyline points="9 21 3 21 3 15"/><line x1="21" y1="3" x2="14" y2="10"/><line x1="3" y1="21" x2="10" y2="14"/></svg>
              <span style="font-size: 14px; color: #f1f5f9;">拉伸以铺满全屏</span>
            </div>
            <label class="aurora-switch">
              <input type="checkbox" id="toggle-stretch-cover">
              <span class="aurora-slider"></span>
            </label>
          </div>

          <!-- 自适应横竖屏 -->
          <div style="display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; background: rgba(255,255,255,0.03); border-radius: 10px;">
            <div style="display: flex; align-items: center; gap: 10px;">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect width="14" height="20" x="5" y="2" rx="2" ry="2"/><path d="M12 18h.01"/></svg>
              <span style="font-size: 14px; color: #f1f5f9;">自适应横竖屏</span>
            </div>
            <label class="aurora-switch">
              <input type="checkbox" id="toggle-orientation">
              <span class="aurora-slider"></span>
            </label>
          </div>
        </div>

        <!-- 2. 播放 (Playback) -->
        <div style="font-size: 13px; color: #94a3b8; font-weight: 500; margin-bottom: 10px;">播放</div>
        <div style="display: flex; flex-direction: column; gap: 4px; margin-bottom: 18px;">
          <!-- 开启循环播放 -->
          <div style="display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; background: rgba(255,255,255,0.03); border-radius: 10px;">
            <div style="display: flex; align-items: center; gap: 10px;">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m17 2 4 4-4 4"/><path d="M3 11v-1a4 4 0 0 1 4-4h14"/><path d="m7 22-4-4 4-4"/><path d="M21 13v1a4 4 0 0 1-4 4H3"/></svg>
              <span style="font-size: 14px; color: #f1f5f9;">开启循环播放</span>
            </div>
            <label class="aurora-switch">
              <input type="checkbox" id="toggle-loop">
              <span class="aurora-slider"></span>
            </label>
          </div>

          <!-- 开启随机播放 -->
          <div style="display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; background: rgba(255,255,255,0.03); border-radius: 10px;">
            <div style="display: flex; align-items: center; gap: 10px;">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M2 18h1.4c1.3 0 2.5-.6 3.3-1.7l6.1-8.6c.7-1.1 2-1.7 3.3-1.7H22"/><path d="m18 2 4 4-4 4"/><path d="M2 6h1.9c1.5 0 2.9.9 3.6 2.2"/><path d="M22 18h-5.9c-1.3 0-2.6-.7-3.3-1.8l-.5-.8"/><path d="m18 14 4 4-4 4"/></svg>
              <span style="font-size: 14px; color: #f1f5f9;">开启随机播放</span>
            </div>
            <label class="aurora-switch">
              <input type="checkbox" id="toggle-random">
              <span class="aurora-slider"></span>
            </label>
          </div>

          <!-- 自动播放下一集 -->
          <div style="display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; background: rgba(255,255,255,0.03); border-radius: 10px;">
            <div style="display: flex; align-items: center; gap: 10px;">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="5 4 15 12 5 20 5 4"/><line x1="19" y1="5" x2="19" y2="19"/></svg>
              <span style="font-size: 14px; color: #f1f5f9;">自动播放下一集</span>
            </div>
            <label class="aurora-switch">
              <input type="checkbox" id="toggle-autonext" checked>
              <span class="aurora-slider"></span>
            </label>
          </div>

          <!-- 定时关闭 -->
          <div id="sleep-timer-btn" style="display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; background: rgba(255,255,255,0.03); border-radius: 10px; cursor: pointer;">
            <div style="display: flex; align-items: center; gap: 10px;">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
              <span style="font-size: 14px; color: #f1f5f9;">定时关闭</span>
            </div>
            <div style="display: flex; align-items: center; gap: 6px;">
              <span id="sleep-timer-status" style="font-size: 13px; color: #94a3b8;">${sleepTimerLabel}</span>
              <span style="color: #64748b; font-size: 14px;">›</span>
            </div>
          </div>
        </div>

        <!-- 3. 轨道 (Tracks) -->
        <div style="font-size: 13px; color: #94a3b8; font-weight: 500; margin-bottom: 10px;">轨道</div>
        <div style="display: flex; flex-direction: column; gap: 4px; margin-bottom: 22px;">
          <!-- 音轨 -->
          <div id="audio-track-btn" style="display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; background: rgba(255,255,255,0.03); border-radius: 10px; cursor: pointer;">
            <div style="display: flex; align-items: center; gap: 10px;">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg>
              <span style="font-size: 14px; color: #f1f5f9;">音轨</span>
            </div>
            <span style="font-size: 13px; color: #94a3b8;">立体声 (2ch)</span>
          </div>

          <!-- 字幕 -->
          <div id="subtitle-track-btn" style="display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; background: rgba(255,255,255,0.03); border-radius: 10px; cursor: pointer;">
            <div style="display: flex; align-items: center; gap: 10px;">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 6h16a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z"/><line x1="8" y1="11" x2="8" y2="13"/><line x1="16" y1="11" x2="16" y2="13"/><line x1="12" y1="11" x2="12" y2="13"/></svg>
              <span style="font-size: 14px; color: #f1f5f9;">字幕</span>
            </div>
            <span style="font-size: 13px; color: #94a3b8;">没有可选字幕</span>
          </div>
        </div>

        <!-- 4. 播放速度 (Playback Speed) matching Screenshot 4 -->
        <div style="font-size: 13px; color: #94a3b8; font-weight: 500; margin-bottom: 12px;">播放速度</div>
        <div id="speed-selector-bar" style="display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; background: rgba(0,0,0,0.4); border-radius: 12px; border: 1px solid rgba(255,255,255,0.06);">
          ${[0.5, 1.0, 1.25, 1.5, 2.0, 3.0]
            .map(
              (spd) => `
            <button class="speed-opt-btn" data-speed="${spd}" style="background: none; border: none; color: ${
                spd === selectedSpeed ? '#38bdf8' : '#94a3b8'
              }; font-size: 15px; font-weight: 600; cursor: pointer; padding: 6px 10px; position: relative;">
              ${spd === 1.0 ? '1.0' : spd}
              ${
                spd === selectedSpeed
                  ? '<div style="position: absolute; bottom: 0; left: 50%; transform: translateX(-50%); width: 14px; height: 2px; background: #38bdf8; border-radius: 9999px;"></div>'
                  : ''
              }
            </button>
          `
            )
            .join('')}
        </div>
      </div>
    `;

    document.body.appendChild(drawer);

    // Style switch switches
    const styleEl = document.createElement('style');
    styleEl.textContent = `
      .aurora-switch {
        position: relative;
        display: inline-block;
        width: 44px;
        height: 24px;
        margin: 0;
      }
      .aurora-switch input {
        opacity: 0;
        width: 0;
        height: 0;
      }
      .aurora-slider {
        position: absolute;
        cursor: pointer;
        top: 0; left: 0; right: 0; bottom: 0;
        background-color: rgba(255,255,255,0.18);
        transition: .25s;
        border-radius: 24px;
      }
      .aurora-slider:before {
        position: absolute;
        content: "";
        height: 18px;
        width: 18px;
        left: 3px;
        bottom: 3px;
        background-color: white;
        transition: .25s;
        border-radius: 50%;
      }
      input:checked + .aurora-slider {
        background-color: #f59e0b;
      }
      input:checked + .aurora-slider:before {
        transform: translateX(20px);
      }
      .web-fullscreen-mode {
        position: fixed !important;
        inset: 0 !important;
        width: 100vw !important;
        height: 100vh !important;
        z-index: 999999 !important;
        background: #000 !important;
        border-radius: 0 !important;
      }
      .web-fullscreen-mode video {
        width: 100% !important;
        height: 100% !important;
      }
    `;
    document.head.appendChild(styleEl);

    // Event Bindings
    drawer.addEventListener('click', (e) => {
      if (e.target === drawer) {
        drawer.style.display = 'none';
      }
    });

    document.getElementById('close-settings-btn').addEventListener('click', () => {
      drawer.style.display = 'none';
    });

    // 1. Force fill switch
    const forceFillCheck = document.getElementById('toggle-force-fill');
    forceFillCheck.addEventListener('change', (e) => {
      forceFill = e.target.checked;
      if (forceFill) {
        stretchCover = false;
        document.getElementById('toggle-stretch-cover').checked = false;
      }
      updateObjectFit();
      showToast(forceFill ? '已开启强制铺满全屏' : '已恢复标准比例');
    });

    // 2. Stretch cover switch
    const stretchCheck = document.getElementById('toggle-stretch-cover');
    stretchCheck.addEventListener('change', (e) => {
      stretchCover = e.target.checked;
      if (stretchCover) {
        forceFill = false;
        document.getElementById('toggle-force-fill').checked = false;
      }
      updateObjectFit();
      showToast(stretchCover ? '已开启拉伸以铺满' : '已恢复标准比例');
    });

    // 3. Auto orientation switch
    const orientCheck = document.getElementById('toggle-orientation');
    orientCheck.addEventListener('change', (e) => {
      autoOrientation = e.target.checked;
      if (autoOrientation && screen.orientation && screen.orientation.unlock) {
        screen.orientation.unlock().catch(() => {});
      }
      showToast(autoOrientation ? '自适应横竖屏已开启' : '锁定当前方向');
    });

    // 4. Loop playback
    const loopCheck = document.getElementById('toggle-loop');
    loopCheck.addEventListener('change', (e) => {
      loopPlayback = e.target.checked;
      const v = getVideo();
      if (v) v.loop = loopPlayback;
      showToast(loopPlayback ? '已开启循环播放' : '已关闭循环播放');
    });

    // 5. Random playback
    const randomCheck = document.getElementById('toggle-random');
    randomCheck.addEventListener('change', (e) => {
      randomPlayback = e.target.checked;
      showToast(randomPlayback ? '已开启随机连播' : '已恢复顺序连播');
    });

    // 6. Auto next episode
    const autoNextCheck = document.getElementById('toggle-autonext');
    autoNextCheck.addEventListener('change', (e) => {
      autoNext = e.target.checked;
      showToast(autoNext ? '已开启自动连播' : '已关闭自动连播');
    });

    // 7. Sleep timer picker
    document.getElementById('sleep-timer-btn').addEventListener('click', () => {
      openSleepTimerMenu();
    });

    // 8. Audio & Subtitles
    document.getElementById('audio-track-btn').addEventListener('click', () => {
      showToast('当前音频轨道: 立体声 (2ch)');
    });
    document.getElementById('subtitle-track-btn').addEventListener('click', () => {
      showToast('当前无外挂字幕轨道');
    });

    // 9. Speed selection bar
    drawer.querySelectorAll('.speed-opt-btn').forEach((btn) => {
      btn.addEventListener('click', () => {
        const spd = parseFloat(btn.dataset.speed);
        selectedSpeed = spd;
        const v = getVideo();
        if (v) v.playbackRate = spd;

        drawer.querySelectorAll('.speed-opt-btn').forEach((b) => {
          const s = parseFloat(b.dataset.speed);
          if (s === selectedSpeed) {
            b.style.color = '#38bdf8';
            b.innerHTML = `${s === 1.0 ? '1.0' : s}<div style="position: absolute; bottom: 0; left: 50%; transform: translateX(-50%); width: 14px; height: 2px; background: #38bdf8; border-radius: 9999px;"></div>`;
          } else {
            b.style.color = '#94a3b8';
            b.innerHTML = `${s === 1.0 ? '1.0' : s}`;
          }
        });
        showToast(`倍速已调整为 ${spd}x`);
      });
    });
  }

  // Sleep timer dialog
  function openSleepTimerMenu() {
    const options = [
      { label: '关闭定时器', minutes: 0 },
      { label: '15 分钟后', minutes: 15 },
      { label: '30 分钟后', minutes: 30 },
      { label: '45 分钟后', minutes: 45 },
      { label: '60 分钟后', minutes: 60 },
      { label: '90 分钟后', minutes: 90 },
      { label: '播完当前集', minutes: -1 },
    ];

    let modal = document.getElementById('aurora-timer-modal');
    if (!modal) {
      modal = document.createElement('div');
      modal.id = 'aurora-timer-modal';
      modal.style.cssText =
        'position: fixed; inset: 0; background: rgba(0,0,0,0.7); z-index: 1000000; display: flex; align-items: center; justify-content: center; padding: 20px;';
      document.body.appendChild(modal);
    }

    modal.innerHTML = `
      <div style="background: #1c1c24; border-radius: 16px; border: 1px solid rgba(255,255,255,0.1); width: 100%; max-width: 320px; padding: 18px; color: #fff; box-shadow: 0 10px 30px rgba(0,0,0,0.8);">
        <div style="font-size: 16px; font-weight: 600; margin-bottom: 14px; text-align: center;">选择定时关闭时间</div>
        <div style="display: flex; flex-direction: column; gap: 6px;">
          ${options
            .map(
              (opt) => `
            <button class="timer-select-opt" data-minutes="${opt.minutes}" style="background: rgba(255,255,255,0.04); border: none; color: #e2e8f0; padding: 12px; border-radius: 8px; font-size: 14px; cursor: pointer; text-align: left; transition: background 0.2s;">
              ${opt.label}
            </button>
          `
            )
            .join('')}
        </div>
        <button id="cancel-timer-btn" style="width: 100%; margin-top: 12px; background: none; border: 1px solid rgba(255,255,255,0.1); color: #94a3b8; padding: 10px; border-radius: 8px; font-size: 13px; cursor: pointer;">取消</button>
      </div>
    `;
    modal.style.display = 'flex';

    modal.querySelector('#cancel-timer-btn').onclick = () => {
      modal.style.display = 'none';
    };

    modal.querySelectorAll('.timer-select-opt').forEach((btn) => {
      btn.onclick = () => {
        const m = parseInt(btn.dataset.minutes, 10);
        modal.style.display = 'none';
        setSleepTimer(m);
      };
    });
  }

  function setSleepTimer(minutes) {
    clearTimeout(sleepTimerId);
    sleepTimerId = null;
    sleepTimerEndTime = null;

    const statusEl = document.getElementById('sleep-timer-status');

    if (minutes === 0) {
      sleepTimerLabel = '未设置';
      if (statusEl) statusEl.textContent = '未设置';
      showToast('已取消定时关闭');
      return;
    }

    if (minutes === -1) {
      sleepTimerLabel = '播完当前集';
      if (statusEl) statusEl.textContent = '播完当前集';
      showToast('将在当前集播放完毕后关闭');
      const v = getVideo();
      if (v) {
        v.addEventListener(
          'ended',
          () => {
            v.pause();
            showToast('已按定时设定停止播放');
            sleepTimerLabel = '未设置';
            if (statusEl) statusEl.textContent = '未设置';
          },
          { once: true }
        );
      }
      return;
    }

    sleepTimerEndTime = Date.now() + minutes * 60 * 1000;
    sleepTimerLabel = `${minutes}分钟后`;
    if (statusEl) statusEl.textContent = sleepTimerLabel;
    showToast(`已开启定时: ${minutes} 分钟后停止播放`);

    sleepTimerId = setTimeout(() => {
      const v = getVideo();
      if (v) v.pause();
      showToast('定时关闭时间到，已自动停止播放');
      sleepTimerLabel = '未设置';
      if (statusEl) statusEl.textContent = '未设置';
    }, minutes * 60 * 1000);
  }

  // Open settings drawer
  window.openPlayerSettings = function () {
    createSettingsDrawer();
    const drawer = document.getElementById('aurora-settings-drawer');
    if (drawer) drawer.style.display = 'flex';
  };

  // Video Ended Handler: support Loop, Random, and Auto-Next
  function setupVideoListeners() {
    const v = getVideo();
    if (!v || v._listenersAttached) return;
    v._listenersAttached = true;

    v.addEventListener('ended', () => {
      if (loopPlayback) {
        v.currentTime = 0;
        v.play().catch(() => {});
        return;
      }

      if (randomPlayback) {
        // Pick a random episode button from playlist
        const epBtns = Array.from(document.querySelectorAll('button')).filter(
          (b) => b.textContent && /^\d+$/.test(b.textContent.trim())
        );
        if (epBtns.length > 1) {
          const randBtn = epBtns[Math.floor(Math.random() * epBtns.length)];
          randBtn.click();
          showToast('随机连播下一集');
          return;
        }
      }

      if (autoNext) {
        // Find next episode button
        const epBtns = Array.from(document.querySelectorAll('button')).filter(
          (b) => b.textContent && /^\d+$/.test(b.textContent.trim())
        );
        // Find active button (amber color)
        let activeIdx = epBtns.findIndex(
          (b) =>
            b.className.includes('amber') ||
            b.style.backgroundColor.includes('amber')
        );
        if (activeIdx !== -1 && activeIdx + 1 < epBtns.length) {
          epBtns[activeIdx + 1].click();
          showToast('自动连播下一集');
        }
      }
    });
  }

  // Inject buttons into Player Controls bar: Settings ⚙, PiP 📺, Fullscreen ⛶
  function injectControls() {
    const v = getVideo();
    if (!v) return;
    setupVideoListeners();

    // Look for player controls bar (bottom row of controls)
    const container = getPlayerContainer();
    if (!container) return;

    if (container.querySelector('#aurora-injected-controls')) return;

    const controlGroup = document.createElement('div');
    controlGroup.id = 'aurora-injected-controls';
    controlGroup.style.cssText =
      'position: absolute; top: 12px; right: 12px; display: flex; align-items: center; gap: 8px; z-index: 50;';

    // Settings Button (⚙)
    const settingsBtn = document.createElement('button');
    settingsBtn.title = '播放器设置 (全屏/画中画/倍速/定时)';
    settingsBtn.style.cssText =
      'background: rgba(0,0,0,0.6); color: #e2e8f0; border: 1px solid rgba(255,255,255,0.15); border-radius: 8px; padding: 6px 10px; cursor: pointer; display: flex; align-items: center; gap: 4px; font-size: 12px; backdrop-filter: blur(4px); transition: background 0.2s;';
    settingsBtn.innerHTML = `
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
      <span>设置</span>
    `;
    settingsBtn.onclick = (e) => {
      e.stopPropagation();
      window.openPlayerSettings();
    };

    // PiP Button (画中画)
    const pipBtn = document.createElement('button');
    pipBtn.title = '画中画小窗播放';
    pipBtn.style.cssText =
      'background: rgba(0,0,0,0.6); color: #e2e8f0; border: 1px solid rgba(255,255,255,0.15); border-radius: 8px; padding: 6px 10px; cursor: pointer; display: flex; align-items: center; gap: 4px; font-size: 12px; backdrop-filter: blur(4px); transition: background 0.2s;';
    pipBtn.innerHTML = `
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect width="18" height="18" x="3" y="3" rx="2"/><rect width="8" height="6" x="11" y="11" rx="1"/></svg>
      <span>画中画</span>
    `;
    pipBtn.onclick = (e) => {
      e.stopPropagation();
      window.togglePlayerPiP();
    };

    // Fullscreen Button (全屏)
    const fsBtn = document.createElement('button');
    fsBtn.title = '全屏播放';
    fsBtn.style.cssText =
      'background: rgba(0,0,0,0.6); color: #f59e0b; border: 1px solid rgba(245,158,11,0.3); border-radius: 8px; padding: 6px 10px; cursor: pointer; display: flex; align-items: center; gap: 4px; font-size: 12px; backdrop-filter: blur(4px); transition: background 0.2s; font-weight: 600;';
    fsBtn.innerHTML = `
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/></svg>
      <span>全屏</span>
    `;
    fsBtn.onclick = (e) => {
      e.stopPropagation();
      window.togglePlayerFullscreen();
    };

    controlGroup.appendChild(settingsBtn);
    controlGroup.appendChild(pipBtn);
    controlGroup.appendChild(fsBtn);
    container.appendChild(controlGroup);
  }

  // Observe player mounting
  const observer = new MutationObserver(() => {
    injectControls();
  });

  document.addEventListener('DOMContentLoaded', () => {
    injectControls();
    observer.observe(document.body, { childList: true, subtree: true });
  });

  // Also check periodically during startup
  setInterval(injectControls, 1000);
})();
