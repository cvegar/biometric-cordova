package biometric.entel;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.content.Context;

import org.apache.cordova.CordovaPlugin;

//import javax.naming.Context;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class echoes a string called from JavaScript.
 */
public class BiometricCordova extends CordovaPlugin {
    

   private static final String TAG = "BiometricCordova";
    private static final int CAPTURE_REQUEST = 1001;
    private CallbackContext currentCallbackContext; // Unificado para manejar todos los callbacks


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
         // Verify that the user sent a 'show' action
      if (!action.equals("getbiometricwsq")) {
          callbackContext.error("\"" + action + "\" is not a recognized action.");
          return false;
      }

      currentCallbackContext= callbackContext

      String LeftFinger;
      String RightFinger;
      //boolean Liveness;
      int Type;
      try {
          JSONObject options = args.getJSONObject(0);
          LeftFinger = options.getInt("leftFingerCode");
          RightFinger = options.getInt("righFingerCode");
          //Liveness = options.getBoolean("liveness");
          Type = options.getInt("type");
          Log.d(TAG,"JsonOpened Left " + String.valueOf(LeftFinger) + "Right " + String.valueOf(RightFinger) );
      } catch (JSONException e) {
          callbackContext.error("Error encountered: " + e.getMessage());
          return false;
      }
      Context appCtx = cordova.getActivity().getApplicationContext();
      Intent getwsqIntent = new Intent(appCtx, ScanActionCryptoActivity.class);
      getwsqIntent.putExtra("LeftFinger", LeftFinger);
      getwsqIntent.putExtra("RightFinger", RightFinger);
      //getwsqIntent.putExtra("Liveness", Liveness);
      getwsqIntent.putExtra("Type", Type);
      cordova.startActivityForResult(this, getwsqIntent, x);
      
      return true;
    }


  @Override
  public void onActivityResult (int requestCode, int resultCode, Intent data)
  {
      Log.d(TAG,"OnActivityResult code = " + String.valueOf(requestCode));

      if(requestCode == BIOM_REQ_CODE){

          if(data != null){

            if(resultCode == Activity.RESULT_OK){
		    

                String wsqb64 = (String) data.getExtras().get("base64String");
                String hand = (String) data.getExtras().get("hand");
                String img = (String) data.getExtras().get("img");
                String minutia = (String) data.getExtras().get("minutia");
                CryptoUtil.loadKeys();
                String wsqEncrypted;
                try {
                    // Solo la encriptación está dentro del bloque try-catch
                    wsqEncrypted = CryptoUtil.encrypt_(wsqb64);
                } catch (Exception e) {
                    // Si ocurre un error en la encriptación, se lanza una RuntimeException
                    throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
                }
                // Crear la respuesta en JSON fuera del bloque try-catch
                JSONObject cordoResponse = new JSONObject();
		    
                try {
                    cordoResponse.put("wsq", wsqEncrypted);
                    //cordoResponse.put("hand", hand);
                    cordoResponse.put("img", img);
					cordoResponse.put("minutia", minutia);

                    scanCallbackContext.success(cordoResponse);
                    Log.d(TAG, "WsqB64: " + wsqb64);
                    Log.d(TAG, "Hand: " + hand);
		            Log.d(TAG, "img: " + img);
					Log.d(TAG, "minutia: " + minutia);
			  
                } catch (JSONException e) {
                    scanCallbackContext.error("ERROR");
                    Log.d(TAG, "ERROR: JSONException");
                    e.printStackTrace();
                }


            }else{
                String status = (String) data.getExtras().get("status");
                scanCallbackContext.error("CANCEL");
                Log.d(TAG, "Status: " + status);
            }

          }else{

              Log.d(TAG, "Data is empty");
              
          }

      }else{

          Log.d(TAG, "Result code not recognized");

      }

    
  }


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.currentCallbackContext = callbackContext;
        Log.d(TAG, "Ejecutando acción: " + action);

        try {
            if (action == null || action.isEmpty()) {
                callbackContext.error("Acción no válida");
                return false;
            }

            // Verificar permisos primero
            if (!hasBiometricPermission()) {
                callbackContext.error("Permisos biométricos no concedidos");
                return false;
            }

            switch (action) {
                case "launchScanCrypto":
                    return handleLaunchScanCrypto(callbackContext);
                    
                case "captureFingerprint":
                    return handleCaptureFingerprint(args, callbackContext);
                    
                default:
                    callbackContext.error("Acción no reconocida: " + action);
                    return false;
            }
        } catch (JSONException e) {
            callbackContext.error("Error en parámetros: " + e.getMessage());
            return false;
        } catch (Exception e) {
            callbackContext.error("Error inesperado: " + e.getMessage());
            return false;
        }
    }

    private boolean hasBiometricPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return cordova.getActivity().checkSelfPermission(android.Manifest.permission.USE_BIOMETRIC) 
                == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean handleCaptureFingerprint(JSONArray args, CallbackContext callbackContext) throws JSONException {
        try {
            String instructions = args.optString(0, "Coloca tu dedo en el lector");
            String rightFinger = args.optString(1, "thumb_right");
            String leftFinger = args.optString(2, "index_left");

            Context context = cordova.getActivity().getApplicationContext();
            Intent intent = new Intent(context, CaptureFingerprintActivity.class);
            intent.putExtra("instructions", instructions);
            intent.putExtra("right_finger", rightFinger);
            intent.putExtra("left_finger", leftFinger);

            cordova.setActivityResultCallback(this);
            cordova.startActivityForResult(this, intent, CAPTURE_REQUEST);

            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            
            return true;
        } catch (Exception e) {
            callbackContext.error("Error iniciando captura: " + e.getMessage());
            return false;
        }
    }

    private boolean handleLaunchScanCrypto(CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Lanzando escaneo criptográfico");
            Context context = cordova.getActivity().getApplicationContext();
            Intent intent = new Intent(context, ScanActionCryptoActivity.class);
            
            // Usar el mismo método que para captureFingerprint
            cordova.setActivityResultCallback(this);
            cordova.startActivityForResult(this, intent, CAPTURE_REQUEST);
            
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error al lanzar escaneo cripto: " + e.getMessage());
            callbackContext.error(e.getMessage());
            return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (currentCallbackContext == null) {
            Log.e(TAG, "CallbackContext es nulo");
            return;
        }

        try {
            JSONObject result = new JSONObject();
            
            if (requestCode == CAPTURE_REQUEST) {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String wsqBase64 = data.getStringExtra("finger");
                   String serialNumber = data.getStringExtra("serialnumber");
                    if (serialNumber == null) {
                        serialNumber = "N/A";
                    }
                    if (wsqBase64 != null && !wsqBase64.isEmpty()) {
                        result.put("success", true);
                        result.put("wsq", wsqBase64);
                        result.put("serialNumber", serialNumber);
                        result.put("message", "Captura exitosa");
                        currentCallbackContext.success(result);
                    } else {
                        result.put("success", false);
                        result.put("message", "Datos WSQ no recibidos");
                        currentCallbackContext.error(result);
                    }
                } else {
                    result.put("success", false);
                    result.put("message", resultCode == Activity.RESULT_CANCELED ? 
                        "Captura cancelada" : "Error en el dispositivo");
                    currentCallbackContext.error(result);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error procesando resultado: " + e.getMessage());
            currentCallbackContext.error("Error procesando resultado: " + e.getMessage());
        } finally {
            currentCallbackContext = null; // Limpiar después de usar
        }
    }
}
