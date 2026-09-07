package com.aegis.vpn;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "AegisVpn")
public class AegisVpnPlugin extends Plugin {
    private static final int VPN_REQUEST_CODE = 0x0F;
    private PluginCall savedCall;

    @PluginMethod
    public void startVpn(PluginCall call) {
        Activity activity = getActivity();
        Intent prepareIntent = VpnService.prepare(activity);
        if (prepareIntent != null) {
            savedCall = call;
            startActivityForResult(call, prepareIntent, "handleVpnResult");
        } else {
            // Already prepared, start service immediately
            startService();
            JSObject ret = new JSObject();
            ret.put("status", "connected");
            call.resolve(ret);
        }
    }

    @PluginMethod
    public void stopVpn(PluginCall call) {
        Activity activity = getActivity();
        Intent intent = new Intent(activity, AegisVpnService.class);
        intent.setAction(AegisVpnService.ACTION_DISCONNECT);
        activity.startService(intent);

        JSObject ret = new JSObject();
        ret.put("status", "disconnected");
        call.resolve(ret);
    }

    private void handleVpnResult(PluginCall call, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            startService();
            JSObject ret = new JSObject();
            ret.put("status", "connected");
            call.resolve(ret);
        } else {
            call.reject("User declined VPN permission request.");
        }
    }

    private void startService() {
        Activity activity = getActivity();
        Intent intent = new Intent(activity, AegisVpnService.class);
        intent.setAction(AegisVpnService.ACTION_CONNECT);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            activity.startForegroundService(intent);
        } else {
            activity.startService(intent);
        }
    }
}
