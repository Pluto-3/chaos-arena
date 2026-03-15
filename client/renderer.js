const Renderer = (() => {
  let canvas = null;
  let ctx = null;
  let staticWalls = [];
  let myPlayerId = null;

  // Player colours — up to 5 players
  const PLAYER_COLORS = ['#e94560', '#00b4d8', '#06d6a0', '#ffd166', '#c77dff'];
  const playerColorMap = {};
  let colorIndex = 0;

  function init(canvasEl, walls, playerId) {
    canvas    = canvasEl;
    ctx       = canvas.getContext('2d');
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

  function draw(state) {
    if (!ctx) return;

    // Background
    ctx.fillStyle = '#1a1a2e';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    drawStaticWalls();
    drawStructures(state.structures || []);
    drawTraps(state.traps || []);
    drawPlayers(state.players || []);
  }

  function drawStaticWalls() {
    ctx.fillStyle = '#2a2a4e';
    ctx.strokeStyle = '#4a4a6e';
    ctx.lineWidth = 1;
    for (const wall of staticWalls) {
      ctx.fillRect(wall.x, wall.y, wall.width, wall.height);
      ctx.strokeRect(wall.x, wall.y, wall.width, wall.height);
    }
  }

  function drawStructures(structures) {
    for (const s of structures) {
      switch (s.type) {
        case 'WALL':
          ctx.fillStyle = '#8b5e3c';
          ctx.strokeStyle = '#a0522d';
          break;
        case 'JUMP_PAD':
          ctx.fillStyle = '#06d6a0';
          ctx.strokeStyle = '#04a87d';
          break;
        case 'TURRET':
          ctx.fillStyle = '#c77dff';
          ctx.strokeStyle = '#9b5de5';
          break;
        default:
          ctx.fillStyle = '#888';
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

      switch (trap.type) {
        case 'BANANA':
          ctx.fillStyle = '#ffd166';
          ctx.font = '20px serif';
          ctx.textAlign = 'center';
          ctx.textBaseline = 'middle';
          ctx.fillText('🍌', 0, 0);
          break;
        case 'SPRING':
          ctx.fillStyle = '#00b4d8';
          ctx.beginPath();
          ctx.arc(0, 0, 10, 0, Math.PI * 2);
          ctx.fill();
          ctx.fillStyle = '#fff';
          ctx.font = '12px serif';
          ctx.textAlign = 'center';
          ctx.textBaseline = 'middle';
          ctx.fillText('🌀', 0, 0);
          break;
        case 'GLUE':
          ctx.fillStyle = '#a8dadc';
          ctx.beginPath();
          ctx.arc(0, 0, 10, 0, Math.PI * 2);
          ctx.fill();
          ctx.font = '12px serif';
          ctx.textAlign = 'center';
          ctx.textBaseline = 'middle';
          ctx.fillText('🕸️', 0, 0);
          break;
      }
      ctx.restore();
    }
  }

  function drawPlayers(players) {
    for (const player of players) {
      if (!player.alive) continue;

      const color  = getPlayerColor(player.id);
      const isMe   = player.id === myPlayerId;
      const radius = 16;

      // Glue effect — draw sticky ring
      if (player.glued) {
        ctx.strokeStyle = '#a8dadc';
        ctx.lineWidth   = 3;
        ctx.beginPath();
        ctx.arc(player.x, player.y, radius + 4, 0, Math.PI * 2);
        ctx.stroke();
      }

      // Player circle
      ctx.fillStyle   = color;
      ctx.strokeStyle = isMe ? '#fff' : '#000';
      ctx.lineWidth   = isMe ? 3 : 1.5;
      ctx.beginPath();
      ctx.arc(player.x, player.y, radius, 0, Math.PI * 2);
      ctx.fill();
      ctx.stroke();

      // Name tag
      ctx.fillStyle   = '#fff';
      ctx.font        = `bold 11px Segoe UI`;
      ctx.textAlign   = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(player.name, player.x, player.y - radius - 8);

      // Health bar
      const barWidth = 32;
      const hpPct    = player.health / 100;
      ctx.fillStyle  = '#333';
      ctx.fillRect(player.x - barWidth / 2, player.y + radius + 4, barWidth, 4);
      ctx.fillStyle  = hpPct > 0.5 ? '#06d6a0' : hpPct > 0.25 ? '#ffd166' : '#e94560';
      ctx.fillRect(player.x - barWidth / 2, player.y + radius + 4, barWidth * hpPct, 4);
    }
  }

  return { init, draw, getPlayerColor, getStaticWalls: () => staticWalls };
})();