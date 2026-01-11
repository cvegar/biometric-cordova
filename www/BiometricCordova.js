var exec = require('cordova/exec');

function asBracketArrayString(value) {
  // La Activity hace substring(2, len-2), así que necesita ["..."]
  // Si ya viene con ["..."], lo dejamos igual.
  if (value === null || value === undefined) return '[""]';

  var s = String(value).trim();
  if (s.startsWith('["') && s.endsWith('"]')) return s;

  // escapamos comillas dobles dentro del string, por seguridad
  s = s.replace(/"/g, '\\"');
  return '["' + s + '"]';
}

var BiometricCordova = {
  /**
   * scanCrypto(rightFingerCode, leftFingerCode, instructions, op, success, error)
   *
   * rightFingerCode: "02"
   * leftFingerCode : "07"
   * instructions   : "1" o "eikon" (lo que uses en tu flujo nativo)
   * op             : false (recomendado para OutSystems)
   */
  scanCrypto: function (rightFingerCode, leftFingerCode, instructions, op, success, error) {
    var options = {
      file: asBracketArrayString(instructions),
      hright: asBracketArrayString(rightFingerCode),
      hleft: asBracketArrayString(leftFingerCode),
      op: !!op
    };

    // OJO: service debe coincidir con <feature name="BiometricCordova">
    exec(success, error, 'BiometricCordova', 'scanCrypto', [options]);
  }
};

// Export principal (lo que Cordova clobberá a cordova.plugins.BiometricCordova)
module.exports = BiometricCordova;

// Alias opcional para que también funcione window.plugins.BiometricCordova
document.addEventListener('deviceready', function () {
  window.plugins = window.plugins || {};
  window.plugins.BiometricCordova = BiometricCordova;
}, false);
