const Input = (() => {
  const keys = {};
  let lastDirection = 'NONE';
  let onAttackCallback   = null;
  let onTrapCallback     = null;
  let onWeaponCallback   = null;

  const MOVE_KEYS = [
    'KeyW','KeyA','KeyS','KeyD','Space',
    'KeyQ','KeyE','KeyR',
    'Digit1','Digit2','Digit3'
  ];

  function init(onAttack, onTrap, onWeapon) {
    onAttackCallback = onAttack;
    onTrapCallback   = onTrap;
    onWeaponCallback = onWeapon;

    window.addEventListener('keydown', (e) => {
      if (MOVE_KEYS.includes(e.code)) e.preventDefault();
      keys[e.code] = true;

      if (e.code === 'Space' && onAttackCallback) onAttackCallback();

      // Trap placement
      if (e.code === 'KeyQ' && onTrapCallback) onTrapCallback('BANANA');
      if (e.code === 'KeyE' && onTrapCallback) onTrapCallback('SPRING');
      if (e.code === 'KeyR' && onTrapCallback) onTrapCallback('GLUE');

      // Weapon switch
      if (e.code === 'Digit1' && onWeaponCallback) onWeaponCallback('frying_pan');
      if (e.code === 'Digit2' && onWeaponCallback) onWeaponCallback('fish_slap');
      if (e.code === 'Digit3' && onWeaponCallback) onWeaponCallback('banana_throw');
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