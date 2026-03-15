const Input = (() => {
  const keys = {};
  let lastDirection = 'NONE';
  let onAttackCallback = null;

  const MOVE_KEYS = [
    'ArrowUp','ArrowDown','ArrowLeft','ArrowRight',
    'KeyW','KeyA','KeyS','KeyD','Space'
  ];

  function init(onAttack) {
    onAttackCallback = onAttack;

    window.addEventListener('keydown', (e) => {
      // Prevent arrow keys from scrolling or moving browser focus
      if (MOVE_KEYS.includes(e.code)) e.preventDefault();
      keys[e.code] = true;
      if (e.code === 'Space' && onAttackCallback) onAttackCallback();
    });

    window.addEventListener('keyup', (e) => {
      keys[e.code] = false;
    });
  }

  function getDirection() {
    const up    = keys['KeyW'];
    const down  = keys['KeyS'];
    const left  = keys['KeyA'];
    const right = keys['KeyD'];

    if (up && left)    return 'UP_LEFT';
    if (up && right)   return 'UP_RIGHT';
    if (down && left)  return 'DOWN_LEFT';
    if (down && right) return 'DOWN_RIGHT';
    if (up)    return 'UP';
    if (down)  return 'DOWN';
    if (left)  return 'LEFT';
    if (right) return 'RIGHT';
    return 'NONE';
  }

  function pollAndSend() {
    const dir = getDirection();
    if (dir !== lastDirection) {
      lastDirection = dir;
      Network.sendMove(dir);
    }
    return dir;
  }

  return { init, pollAndSend, getDirection };
})();