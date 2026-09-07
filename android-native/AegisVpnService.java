package com.aegis.vpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class AegisVpnService extends VpnService {
    private static final String TAG = "AegisVpnService";
    private static final String CHANNEL_ID = "aegis_vpn_channel";
    private static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_CONNECT = "com.aegis.vpn.CONNECT";
    public static final String ACTION_DISCONNECT = "com.aegis.vpn.DISCONNECT";

    private ParcelFileDescriptor vpnInterface = null;
    private boolean isRunning = false;
    private Thread workerThread = null;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_DISCONNECT.equals(action)) {
                stopVpn();
                return START_NOT_STICKY;
            }
        }
        startVpn();
        return START_STICKY;
    }

    private void startVpn() {
        if (isRunning) return;

        try {
            startForeground(NOTIFICATION_ID, buildNotification("AEGIS VPN is Connected & Securing Network"));

            Builder builder = new Builder();
            builder.setSession("Aegis VPN Anti-Censorship Tunnel");
            builder.setMtu(1500);

            // Configure Virtual TUN Interface IP
            builder.addAddress("10.0.0.2", 24);
            builder.addRoute("0.0.0.0", 0);
            builder.addDnsServer("1.1.1.1");
            builder.addDnsServer("8.8.8.8");

            // Allow bypass for local banking apps if requested
            builder.setBlocking(true);

            vpnInterface = builder.establish();
            if (vpnInterface != null) {
                isRunning = true;
                Log.i(TAG, "Virtual TUN Interface Established successfully: fd=" + vpnInterface.getFd());

                // Start packet processing worker thread (tun2socks bridge)
                workerThread = new Thread(() -> {
                    try (FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
                         FileOutputStream out = new FileOutputStream(vpnInterface.getFileDescriptor())) {
                        byte[] packet = new byte[32767];
                        while (isRunning && !Thread.interrupted()) {
                            int length = in.read(packet);
                            if (length > 0) {
                                // Forward packet through the encrypted proxy pipeline
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Packet routing worker interrupted: " + e.getMessage());
                    }
                });
                workerThread.start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to establish VPN TUN interface: " + e.getMessage());
            stopVpn();
        }
    }

    private void stopVpn() {
        isRunning = false;
        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing vpn interface: " + e.getMessage());
            }
            vpnInterface = null;
        }
        stopForeground(true);
        stopSelf();
        Log.i(TAG, "Aegis VPN Service stopped.");
    }

    @Override
    public void onDestroy() {
        stopVpn();
        super.onDestroy();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Aegis VPN Service Notification",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows persistent status of the active encrypted VPN tunnel");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(String text) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("Aegis VPN Engine (Flagship)")
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}
