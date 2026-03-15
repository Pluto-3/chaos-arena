const Game = (() => {
  let myPlayerId     = null;
  let gameState      = { players: {}, structures: [], traps: [] };
  let loopStarted    = false;
  let predictionSeeded = false;
  let localX = 0;
  let localY = 0;
  let lastFrameTime  = 0;
  let prevHealthMap  = {};

  const SPEED  = 5.0;
  const CANVAS = document.getElementById('game-canvas');

  function toArray(players) {
    if (!players) return [];
    if (Array.isArray(players)) return players;
    return Object.values(players);
  }

  // ── UI ────────────────────────────────────────────────────────────
  const joinScreen      = document.getElementById('join-screen');
  const gameScreen      = document.getElementById('game-screen');
  const roundoverScreen = document.getElementById('roundover-screen');
  const joinError       = document.getElementById('join-error');
  const hudRoom         = document.getElementById('hud-room');
  const hudHealth       = document.getElementById('hud-health');
  const hudTimer        = document.getElementById('hud-timer');
  const hudPlayers      = document.getElementById('hud-players');
  const eventLog        = document.getElementById('event-log');
  const roundoverWinner = document.getElementById('roundover-winner');
  const roundoverMsgs   = document.getElementById('roundover-messages');
  const roundoverCD     = document.getElementById('roundover-countdown');

  document.getElementById('btn-join').addEventListener('click', () => {
    const name     = document.getElementById('input-name').value.trim();
    const roomCode = document.getElementById('input-roomcode').value.trim();
    if (!name) { joinError.textContent = 'Enter a name first!'; return; }
    Network.connect(onMessage);
    setTimeout(() => Network.join(name, roomCode), 300);
  });

  document.querySelectorAll('.react-btn').forEach(btn => {
    btn.addEventListener('click', () => Network.sendReaction(btn.dataset.emoji));
  });

  // ── Server messages ───────────────────────────────────────────────
  function onMessage(msg) {
    switch (msg.type) {

      case 'JOIN_ACK':
        myPlayerId = msg.playerId;
        Renderer.init(CANVAS, msg.staticWalls, myPlayerId);
        hudRoom.textContent = 'Room: ' + msg.roomCode;
        showScreen('game');
        startGameLoop();
        break;

      case 'ERROR':
        joinError.textContent = msg.message;
        break;

      case 'STATE_UPDATE':
        // Detect damage — flash and show floating number
        const incoming = toArray(msg.players);
        incoming.forEach(p => {
          const prev = prevHealthMap[p.id];
          if (prev !== undefined && p.health < prev) {
            const dmg = prev - p.health;
            Renderer.flashDamage(p.id);
            Renderer.addFloatingText(p.x, p.y - 20, '-' + dmg, '#e94560');
          }
          prevHealthMap[p.id] = p.health;
        });

        gameState = msg;

        if (!predictionSeeded) {
          const me = incoming.find(p => p.id === myPlayerId);
          if (me) {
            localX = me.x;
            localY = me.y;
            predictionSeeded = true;
          }
        }

        updateHUD(msg);
        break;

      case 'PLAYER_DEAD':
        logEvent(msg.message);
        // Show big floating skull at dead player position
        const dead = toArray(gameState.players).find(p => p.id === msg.entityId);
        if (dead) Renderer.addFloatingText(dead.x, dead.y - 30, '💀 DEAD', '#e94560');
        break;

      case 'TRAP_TRIGGERED':
        logEvent(msg.message);
        break;

      case 'REACTION':
        logEvent(msg.playerName + ' ' + msg.emoji);
        break;

      case 'ROUND_END':
        showRoundOver(msg);
        break;

      case 'ROUND_START':
        predictionSeeded = false;
        prevHealthMap    = {};
        showScreen('game');
        break;
    }
  }

  // ── Prediction ────────────────────────────────────────────────────
  function applyLocalPrediction(dir) {
    if (!predictionSeeded) return;

    const now   = performance.now();
    const delta = Math.min(now - lastFrameTime, 100);
    lastFrameTime = now;
    const scale = delta / 50;
    const spd   = SPEED * scale;

    let dx = 0, dy = 0;
    switch (dir) {
      case 'UP':         dy = -spd; break;
      case 'DOWN':       dy =  spd; break;
      case 'LEFT':       dx = -spd; break;
      case 'RIGHT':      dx =  spd; break;
      case 'UP_LEFT':    dx = -spd * 0.707; dy = -spd * 0.707; break;
      case 'UP_RIGHT':   dx =  spd * 0.707; dy = -spd * 0.707; break;
      case 'DOWN_LEFT':  dx = -spd * 0.707; dy =  spd * 0.707; break;
      case 'DOWN_RIGHT': dx =  spd * 0.707; dy =  spd * 0.707; break;
      default: return;
    }

    const newX = localX + dx;
    const newY = localY + dy;
    if (!collidesWithWalls(newX, localY)) localX = newX;
    if (!collidesWithWalls(localX, newY)) localY = newY;

    localX = Math.max(56, Math.min(744, localX));
    localY = Math.max(56, Math.min(544, localY));
  }

  function collidesWithWalls(x, y) {
    const radius = 16;
    for (const w of Renderer.getStaticWalls()) {
      const cx = Math.max(w.x, Math.min(x, w.x + w.width));
      const cy = Math.max(w.y, Math.min(y, w.y + w.height));
      const dx = x - cx;
      const dy = y - cy;
      if ((dx * dx + dy * dy) <= (radius * radius)) return true;
    }
    return false;
  }

  // ── Attack ────────────────────────────────────────────────────────
  function attackNearest() {
    const players = toArray(gameState.players);
    let nearest = null;
    let minDist = Infinity;
    for (const p of players) {
      if (p.id === myPlayerId || !p.alive) continue;
      const dx = p.x - localX;
      const dy = p.y - localY;
      const dist = Math.sqrt(dx * dx + dy * dy);
      if (dist < minDist) { minDist = dist; nearest = p; }
    }
    if (nearest && minDist <= 80) Network.sendAttack(nearest.id);
  }

  // ── Trap placement ────────────────────────────────────────────────
  function placeTrap(trapType) {
    Network.sendTrap(trapType, localX, localY);
    logEvent('You placed a ' + trapType.toLowerCase() + ' trap 🪤');
  }

  // ── Weapon switch ─────────────────────────────────────────────────
  function switchWeapon(weapon) {
    Network.sendWeapon(weapon);
    const icons = { frying_pan: '🍳', fish_slap: '🐟', banana_throw: '🍌' };
    logEvent('Switched to ' + weapon.replace('_', ' ') + ' ' + (icons[weapon] || ''));
  }

  // ── Game loop ─────────────────────────────────────────────────────
  function startGameLoop() {
    if (loopStarted) return;
    loopStarted = true;
    Input.init(attackNearest, placeTrap, switchWeapon);
    lastFrameTime = performance.now();

    function loop() {
      Input.pollAndSend();
      const dir = Input.getDirection();
      applyLocalPrediction(dir);

      const renderState = {
        ...gameState,
        players: toArray(gameState.players).map(p =>
          p.id === myPlayerId ? { ...p, x: localX, y: localY } : p
        )
      };

      Renderer.draw(renderState);
      requestAnimationFrame(loop);
    }

    loop();
  }

  // ── HUD ───────────────────────────────────────────────────────────
  function updateHUD(state) {
    const players = toArray(state.players);
    const me = players.find(p => p.id === myPlayerId);
    if (me) hudHealth.textContent = '❤️ ' + me.health;
    const secs = Math.ceil((state.timeLeft || 0) / 20);
    const mins = Math.floor(secs / 60);
    const s    = secs % 60;
    hudTimer.textContent   = `⏱ ${mins}:${s.toString().padStart(2, '0')}`;
    hudPlayers.textContent = 'Players: ' + players.filter(p => p.alive).length;
  }

  // ── Event log ─────────────────────────────────────────────────────
  function logEvent(msg) {
    const el = document.createElement('div');
    el.className   = 'event-entry';
    el.textContent = msg;
    eventLog.appendChild(el);
    setTimeout(() => el.remove(), 4000);
  }

  // ── Round over ────────────────────────────────────────────────────
  function showRoundOver(msg) {
    showScreen('roundover');
    roundoverWinner.textContent = '🏆 ' + msg.winner;
    roundoverMsgs.innerHTML = '';
    (msg.messages || []).forEach(m => {
      const el = document.createElement('div');
      el.textContent = m;
      roundoverMsgs.appendChild(el);
    });
    let cd = 5;
    roundoverCD.textContent = 'Next round in ' + cd + '...';
    const t = setInterval(() => {
      cd--;
      if (cd <= 0) { clearInterval(t); showScreen('game'); }
      else roundoverCD.textContent = 'Next round in ' + cd + '...';
    }, 1000);
  }

  // ── Screen switcher ───────────────────────────────────────────────
  function showScreen(name) {
    joinScreen.classList.add('hidden');
    gameScreen.classList.add('hidden');
    roundoverScreen.classList.add('hidden');
    if (name === 'join')      joinScreen.classList.remove('hidden');
    if (name === 'game')      gameScreen.classList.remove('hidden');
    if (name === 'roundover') roundoverScreen.classList.remove('hidden');
  }

  return {};
})();