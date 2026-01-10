package biometric.entel;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import biometric.entel.ScanActionCryptoActivity;

public class EntelFingerPlugin extends CordovaPlugin {

 private static final String TAG = "EntelBiometricPlugin-Cordova";
    private static final int REQ_SCAN_CRYPTO = 11001;

    private CallbackContext pendingCallback;

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) {
        if (!"scanCrypto".equals(action)) {
            callbackContext.error("\"" + action + "\" is not a recognized action.");
            return false;
        }

        pendingCallback = callbackContext;

        try {
            JSONObject options = (args != null && args.length() > 0) ? args.getJSONObject(0) : new JSONObject();

            // Valores “limpios” (los normalizamos para ScanActionCryptoActivity)
            String instructions = optFirstNonEmpty(options, "instructions", "file");
            String hright = optFirstNonEmpty(options, "hright", "rightFingerCode", "right_finger");
            String hleft  = optFirstNonEmpty(options, "hleft", "leftFingerCode", "left_finger");
            boolean op = options.optBoolean("op", false);

            if (isNullOrEmpty(instructions)) {
                callbackContext.error("Missing parameter: instructions (or file).");
                return true;
            }

            if (!op && (isNullOrEmpty(hright) || isNullOrEmpty(hleft))) {
                callbackContext.error("Missing parameters: hright and hleft are required when op=false.");
                return true;
            }

            Context appCtx = cordova.getActivity().getApplicationContext();
            Intent intent = new Intent(appCtx, ScanActionCryptoActivity.class);

            // IMPORTANT: ScanActionCryptoActivity hace substring(2, len-2) sobre "file"
            // Así que lo enviamos como ["valor"] para que sea seguro.
            intent.putExtra("file", normalizeToBracketedString(instructions));
            intent.putExtra("op", op);

            if (!op) {
                intent.putExtra("hright", normalizeToBracketedString(hright));
                intent.putExtra("hleft", normalizeToBracketedString(hleft));
            }

            Log.d(TAG, "Launching ScanActionCryptoActivity. op=" + op);

            cordova.startActivityForResult(this, intent, REQ_SCAN_CRYPTO);
            return true;

        } catch (JSONException e) {
            callbackContext.error("JSON error: " + e.getMessage());
            return true;
        } catch (Exception e) {
            callbackContext.error("Unexpected error: " + e.getMessage());
            return true;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQ_SCAN_CRYPTO) {
            return;
        }

        if (pendingCallback == null) {
            Log.w(TAG, "No pending callback (maybe already resolved/rejected).");
            return;
        }

        if (resultCode == Activity.RESULT_OK && data != null) {
            try {
                JSONObject resp = new JSONObject();
                resp.put("huellab64", data.getStringExtra("huellab64"));
                resp.put("serialnumber", data.getStringExtra("serialnumber"));
                resp.put("fingerprint_brand", data.getStringExtra("fingerprint_brand"));
                resp.put("bioversion", data.getStringExtra("bioversion"));

                pendingCallback.success(resp);
            } catch (JSONException e) {
                pendingCallback.error("ERROR: " + e.getMessage());
            } finally {
                pendingCallback = null;
            }
        } else {
            pendingCallback.error("CANCEL");
            pendingCallback = null;
        }
    }

    // ---------------- helpers ----------------

    private static boolean isNullOrEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * ScanActionCryptoActivity espera strings “tipo array” para luego limpiarlos:
     * - file: substring(2, len-2)
     * - hright/hleft: replace([, ], ")
     * Si ya te llega en formato [""], lo dejamos tal cual.
     */
    private static String normalizeToBracketedString(String value) {
        if (value == null) return null;

        String v = value.trim();
        if (v.startsWith("[") && v.endsWith("]")) {
            return v;
        }

        // Escapamos comillas por si el valor las trae
        v = v.replace("\"", "\\\"");
        return "[\"" + v + "\"]";
    }

    private static String optFirstNonEmpty(JSONObject obj, String... keys) {
        for (String k : keys) {
            if (k == null) continue;
            String v = obj.optString(k, null);
            if (!isNullOrEmpty(v)) return v;
        }
        return null;
    }


}
