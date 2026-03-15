const Renderer = (() => {
  let canvas      = null;
  let ctx         = null;
  let staticWalls = [];
  let myPlayerId  = null;

  const PLAYER_COLORS = ['#e94560', '#00b4d8', '#06d6a0', '#ffd166', '#c77dff'];
  const playerColorMap = {};
  let colorIndex = 0;

  // Damage flash effect per player
  const damageFlash = {};

  // Floating text effects
  const floatingTexts = [];

  function init(canvasEl, walls, playerId) {
    canvas      = canvasEl;
    ctx         = canvas.getContext('2d');
    staticWalls = walls;
    myPlayerId  = playerId;
    canvas.width  = 800;
    canvas.height = 600;
  }

  function getPlayerColor(id) {
    if (!playerColorMap[id]) {
      playerColorMap[id] = PLAYER_COLORS[colorIndex % PLAYER_COLORS.length];
      colorIndex++;
    }
    return playerColorMap[id];
  }

  function flashDamage(playerId) {
    damageFlash[playerId] = 8; // frames to flash
  }

  function addFloatingText(x, y, text, color) {
    floatingTexts.push({ x, y, text, color, life: 60, vy: -1.5 });
  }

  function draw(state) {
    if (!ctx) return;

    ctx.fillStyle = '#1a1a2e';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    drawStaticWalls();
    drawStructures(state.structures || []);
    drawTraps(state.traps || []);
    drawPlayers(state.players || []);
    drawFloatingTexts();
  }

  function drawStaticWalls() {
    ctx.fillStyle   = '#2a2a4e';
    ctx.strokeStyle = '#4a4a6e';
    ctx.lineWidth   = 1;
    for (const wall of staticWalls) {
      ctx.fillRect(wall.x, wall.y, wall.width, wall.height);
      ctx.strokeRect(wall.x, wall.y, wall.width, wall.height);
    }
  }

  function drawStructures(structures) {
    for (const s of structures) {
      switch (s.type) {
        case 'WALL':     ctx.fillStyle = '#8b5e3c'; ctx.strokeStyle = '#a0522d'; break;
        case 'JUMP_PAD': ctx.fillStyle = '#06d6a0'; ctx.strokeStyle = '#04a87d'; break;
        case 'TURRET':   ctx.fillStyle = '#c77dff'; ctx.strokeStyle = '#9b5de5'; break;
        default:         ctx.fillStyle = '#888';
      }
      ctx.lineWidth = 2;
      ctx.fillRect(s.x, s.y, s.width || 40, s.height || 40);
      ctx.strokeRect(s.x, s.y, s.width || 40, s.height || 40);

      // Durability bar
      const pct = (s.durability / 100);
      ctx.fillStyle = '#333';
      ctx.fillRect(s.x, s.y - 8, 40, 4);
      ctx.fillStyle = pct > 0.5 ? '#06d6a0' : '#e94560';
      ctx.fillRect(s.x, s.y - 8, 40 * pct, 4);
    }
  }

  function drawTraps(traps) {
    for (const trap of traps) {
      if (!trap.active) continue;
      ctx.save();
      ctx.translate(trap.x, trap.y);
      ctx.font = '22px serif';
      ctx.textAlign    = 'center';
      ctx.textBaseline = 'middle';
      switch (trap.type) {
        case 'BANANA': ctx.fillText('🍌', 0, 0); break;
        case 'SPRING': ctx.fillText('🌀', 0, 0); break;
        case 'GLUE':   ctx.fillText('🕸️', 0, 0); break;
      }
      ctx.restore();
    }
  }

  function drawPlayers(players) {
    for (const player of players) {
      const color  = getPlayerColor(player.id);
      const isMe   = player.id === myPlayerId;
      const radius = 16;

      if (!player.alive) {
        // Draw ghost / dead player
        ctx.save();
        ctx.globalAlpha = 0.3;
        ctx.fillStyle   = color;
        ctx.beginPath();
        ctx.arc(player.x, player.y, radius, 0, Math.PI * 2);
        ctx.fill();
        ctx.globalAlpha = 1;
        ctx.font = '20px serif';
        ctx.textAlign    = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText('💀', player.x, player.y);
        ctx.restore();
        continue;
      }

      // Glue effect ring
      if (player.glued) {
        ctx.strokeStyle = '#a8dadc';
        ctx.lineWidth   = 3;
        ctx.beginPath();
        ctx.arc(player.x, player.y, radius + 5, 0, Math.PI * 2);
        ctx.stroke();
      }

      // Damage flash — briefly turn player white
      const flashing = damageFlash[player.id] > 0;
      if (flashing) {
        damageFlash[player.id]--;
        ctx.fillStyle = '#ffffff';
      } else {
        ctx.fillStyle = color;
      }

      ctx.strokeStyle = isMe ? '#fff' : '#000';
      ctx.lineWidth   = isMe ? 3 : 1.5;
      ctx.beginPath();
      ctx.arc(player.x, player.y, radius, 0, Math.PI * 2);
      ctx.fill();
      ctx.stroke();

      // Weapon icon above player
      ctx.font = '14px serif';
      ctx.textAlign    = 'center';
      ctx.textBaseline = 'middle';
      const weaponIcon = player.weapon === 'frying_pan' ? '🍳'
                       : player.weapon === 'fish_slap'  ? '🐟' : '🍌';
      ctx.fillText(weaponIcon, player.x + radius, player.y - radius);

      // Name tag
      ctx.fillStyle    = '#fff';
      ctx.font         = 'bold 11px Segoe UI';
      ctx.textAlign    = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(player.name, player.x, player.y - radius - 10);

      // Health bar
      const barWidth = 32;
      const hpPct    = player.health / 100;
      ctx.fillStyle  = '#333';
      ctx.fillRect(player.x - barWidth / 2, player.y + radius + 4, barWidth, 4);
      ctx.fillStyle  = hpPct > 0.5 ? '#06d6a0' : hpPct > 0.25 ? '#ffd166' : '#e94560';
      ctx.fillRect(player.x - barWidth / 2, player.y + radius + 4, barWidth * hpPct, 4);
    }
  }

  function drawFloatingTexts() {
    for (let i = floatingTexts.length - 1; i >= 0; i--) {
      const ft = floatingTexts[i];
      ft.y    += ft.vy;
      ft.life -= 1;
      if (ft.life <= 0) { floatingTexts.splice(i, 1); continue; }
      ctx.globalAlpha  = ft.life / 60;
      ctx.fillStyle    = ft.color || '#fff';
      ctx.font         = 'bold 14px Segoe UI';
      ctx.textAlign    = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(ft.text, ft.x, ft.y);
      ctx.globalAlpha  = 1;
    }
  }

  return {
    init,
    draw,
    getPlayerColor,
    getStaticWalls: () => staticWalls,
    flashDamage,
    addFloatingText
  };
})();