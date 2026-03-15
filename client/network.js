const Network = (() => {
  let ws = null;
  let onMessageCallback = null;

  function connect(onMessage) {
    onMessageCallback = onMessage;
    ws = new WebSocket('ws://localhost:8080/ws/game');
    ws.onopen    = () => console.log('Connected to Chaos Arena server');
    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      if (onMessageCallback) onMessageCallback(data);
    };
    ws.onerror = (e) => console.error('WebSocket error:', e);
    ws.onclose = () => console.log('Disconnected from server');
  }

  function send(payload) {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(payload));
    }
  }

  function join(name, roomCode)          { send({ type: 'JOIN_ROOM', name, roomCode }); }
  function sendMove(direction)           { send({ type: 'MOVE', direction }); }
  function sendAttack(targetId)          { send({ type: 'ATTACK', targetId }); }
  function sendBuild(structureType, x, y){ send({ type: 'BUILD', structureType, x, y }); }
  function sendTrap(trapType, x, y)      { send({ type: 'PLACE_TRAP', trapType, x, y }); }
  function sendReaction(emoji)           { send({ type: 'REACTION', emoji }); }
  function sendWeapon(weapon)            { send({ type: 'SWITCH_WEAPON', weapon }); }

  return { connect, join, sendMove, sendAttack, sendBuild, sendTrap, sendReaction, sendWeapon };
})();