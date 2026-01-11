var exec = require('cordova/exec');

function BiometricCordova() {}

/**
 * scanCrypto(rightFingerCode, leftFingerCode, instructions, op, success, error)
 *
 * rightFingerCode: "02"
 * leftFingerCode : "07"
 * instructions   : "eikon" (o lo que uses)
 * op             : boolean (en tu caso déjalo false)
 */
BiometricCordova.prototype.scanCrypto = function (rightFingerCode, leftFingerCode, instructions, op, success, error) {
  // Soportar también que te manden un objeto como primer parámetro
  // scanCrypto({ rightFingerCode, leftFingerCode, instructions, op }, success, error)
  if (typeof rightFingerCode === 'object' && rightFingerCode !== null) {
    var o = rightFingerCode;
    success = leftFingerCode;
    error = instructions;

    rightFingerCode = o.rightFingerCode;
    leftFingerCode = o.leftFingerCode;
    instructions = o.instructions;
    op = o.op;
  }

  // Normaliza "file" al formato que tu Activity parsea:
  // Si ya viene como ["x"] lo deja, si viene como "x" lo envuelve.
  var fileExtra = (instructions == null) ? '[""]' : String(instructions);
  if (!fileExtra.trim().startsWith('[')) {
    fileExtra = '["' + fileExtra + '"]';
  }

  // En tu Activity: hright/hleft se limpian con replace de [,],"
  // Así que podemos enviarlo como ["02"] para ser 100% compatibles.
  var hrightExtra = (rightFingerCode == null) ? '[""]' : '["' + String(rightFingerCode) + '"]';
  var hleftExtra  = (leftFingerCode == null) ? '[""]' : '["' + String(leftFingerCode) + '"]';

  var options = {
    file: fileExtra,
    hright: hrightExtra,
    hleft: hleftExtra,
    op: (op === true)
  };

  // OJO: el "service" DEBE ser el feature name del plugin.xml => "BiometricCordova"
  exec(success, error, 'BiometricCordova', 'scanCrypto', [options]);
};

// Instala en window.plugins y también en cordova.plugins
BiometricCordova.install = function () {
  window.plugins = window.plugins || {};
  window.plugins.BiometricCordova = new BiometricCordova();

  cordova.plugins = cordova.plugins || {};
  cordova.plugins.BiometricCordova = window.plugins.BiometricCordova;

  return window.plugins.BiometricCordova;
};

cordova.addConstructor(BiometricCordova.install);

module.exports = new BiometricCordova();
