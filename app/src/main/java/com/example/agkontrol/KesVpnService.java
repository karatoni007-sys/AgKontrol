package com.example.agkontrol;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Sahte bir VPN açar, tüm trafiği içine çeker ve hiçbir yere iletmez.
 * Böylece Wi-Fi ve Ethernet dahil bütün internet erişimi kesilir. Root gerekmez.
 */
public class KesVpnService extends VpnService {

    private ParcelFileDescriptor tun;
    private volatile boolean calisiyor = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onPlanaAl();

        if (tun == null) {
            try {
                Builder b = new Builder();
                b.setSession("Ag Kes");
                b.addAddress("10.255.0.2", 32);
                b.addRoute("0.0.0.0", 0);
                try {
                    b.addAddress("fd00:1:2:3::2", 128);
                    b.addRoute("::", 0);
                } catch (Exception ignored) {
                    // IPv6 desteklenmiyorsa sorun değil
                }
                tun = b.establish();
            } catch (Exception e) {
                tun = null;
            }

            if (tun == null) {
                stopSelf();
                return START_NOT_STICKY;
            }

            calisiyor = true;
            final FileInputStream giris = new FileInputStream(tun.getFileDescriptor());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] tampon = new byte[32767];
                    try {
                        // Gelen paketleri okuyup çöpe atıyoruz.
                        while (calisiyor && giris.read(tampon) >= 0) {
                            // hiçbir şey yapma
                        }
                    } catch (IOException ignored) {
                    }
                }
            }).start();
        }
        return START_NOT_STICKY;
    }

    private void onPlanaAl() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel kanal = new NotificationChannel(
                "kes", "Ag Kes", NotificationManager.IMPORTANCE_LOW);
        nm.createNotificationChannel(kanal);
        Notification n = new Notification.Builder(this, "kes")
                .setContentTitle("İnternet kesik")
                .setContentText("Açmak için uygulamada AÇ düğmesine basın")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build();
        startForeground(1, n);
    }

    @Override
    public void onRevoke() {
        // Kullanıcı VPN'i ayarlardan kapatırsa buraya gelir.
        stopSelf();
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        calisiyor = false;
        try {
            if (tun != null) {
                tun.close();
            }
        } catch (IOException ignored) {
        }
        tun = null;
        super.onDestroy();
    }
}
