function BiometricCordova() {}

/**
 * scanCrypto(rightFingerCode, leftFingerCode, op, successCallback, errorCallback)
 *
 * rightFingerCode: "02", "2", etc.
 * leftFingerCode : "07", "7", etc.
 * op: boolean (en tu Activity, cuando op=false lee hright/hleft)
 */
BiometricCordova.prototype.scanCrypto = function (rightFingerCode, leftFingerCode, successCallback, errorCallback) {

  var options = {
    // OJO: tu ScanActionCryptoActivity espera strings con formato ["02"] etc.
    hright: '["' + String(rightFingerCode) + '"]',
    hleft:  '["' + String(leftFingerCode) + '"]',
    // op: (op === true)
    // file eliminado a propósito
  };

  cordova.exec(successCallback, errorCallback, "BiometricCordova", "scanCrypto", [options]);
};

// Instalación en window.plugins
BiometricCordova.install = function () {
  window.plugins = window.plugins || {};
  window.plugins.BiometricCordova = new BiometricCordova();

  // Alias opcional por compatibilidad si antes llamabas a EntelBiometricPlugin
  window.plugins.EntelBiometricPlugin = window.plugins.BiometricCordova;

  return window.plugins.BiometricCordova;
};

cordova.addConstructor(BiometricCordova.install);

module.exports = new BiometricCordova();
