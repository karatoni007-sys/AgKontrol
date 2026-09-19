package com.example.agkontrol;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;

public class MainActivity extends Activity {

    // Cihazınızda arayüz adları farklıysa burayı değiştirin (root yöntemi için).
    private static final String WIFI = "wlan0";
    private static final String ETHERNET = "eth0";
    // Termux'un sshd'sini root olarak başlatan komut (Termux portu: 8022).
    private static final String TERMUX_BIN = "/data/data/com.termux/files/usr/bin";
    private static final String SSHD_KOMUT =
            "export PATH=" + TERMUX_BIN + ":$PATH; "
            + "export LD_LIBRARY_PATH=/data/data/com.termux/files/usr/lib; "
            + "export HOME=/data/data/com.termux/files/home; "
            + TERMUX_BIN + "/sshd";
    private static final int VPN_ISTEK = 100;

    private TextView txtDurum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtDurum = (TextView) findViewById(R.id.txtDurum);
        Button btnKes = (Button) findViewById(R.id.btnKes);
        Button btnAc = (Button) findViewById(R.id.btnAc);
        Button btnSsh = (Button) findViewById(R.id.btnSsh);

        btnSsh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sshDegistir();
            }
        });

        btnKes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uygula(false);
            }
        });

        btnAc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uygula(true);
            }
        });
    }

    private void uygula(final boolean ac) {
        txtDurum.setText("Çalışıyor...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String yon = ac ? "up" : "down";
                final String islem = ac ? "açıldı" : "kapatıldı";
                try {
                    // 1) Root ile dene
                    int wifi = root("ifconfig " + WIFI + " " + yon);
                    int eth = root("ifconfig " + ETHERNET + " " + yon);
                    final String sonuc = "[Root] Wi-Fi: " + (wifi == 0 ? islem : "HATA")
                            + "  |  Ethernet: " + (eth == 0 ? islem : "HATA");
                    yaz(sonuc);
                } catch (IOException e) {
                    // 2) Root yok: VPN yöntemi
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (ac) {
                                vpnDurdur();
                            } else {
                                vpnBaslat();
                            }
                        }
                    });
                } catch (Exception e) {
                    yaz("Hata: " + e.getMessage());
                }
            }
        }).start();
    }

    private void sshDegistir() {
        txtDurum.setText("Çalışıyor...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // pidof: sshd çalışıyorsa 0 döner
                    if (root("pidof sshd") == 0) {
                        root("killall sshd || pkill sshd");
                        yaz("SSH kapatıldı");
                    } else {
                        root(SSHD_KOMUT);
                        Thread.sleep(1000);
                        if (root("pidof sshd") == 0) {
                            yaz("SSH açıldı (port 8022)");
                        } else {
                            yaz("SSH başlatılamadı (Termux'ta openssh kurulu mu?)");
                        }
                    }
                } catch (IOException e) {
                    yaz("SSH için root gerekli.");
                } catch (Exception e) {
                    yaz("Hata: " + e.getMessage());
                }
            }
        }).start();
    }

    private void vpnBaslat() {
        try {
            Intent izin = VpnService.prepare(this);
            if (izin != null) {
                startActivityForResult(izin, VPN_ISTEK);
            } else {
                vpnServisiniBaslat();
            }
        } catch (ActivityNotFoundException e) {
            txtDurum.setText("Bu cihaz VPN iznini desteklemiyor.");
        } catch (Exception e) {
            txtDurum.setText("Hata: " + e.getMessage());
        }
    }

    private void vpnServisiniBaslat() {
        startForegroundService(new Intent(this, KesVpnService.class));
        txtDurum.setText("[VPN] İnternet KESİLDİ (Wi-Fi ve Ethernet)");
    }

    private void vpnDurdur() {
        stopService(new Intent(this, KesVpnService.class));
        txtDurum.setText("[VPN] İnternet AÇILDI");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_ISTEK) {
            if (resultCode == RESULT_OK) {
                vpnServisiniBaslat();
            } else {
                txtDurum.setText("VPN izni verilmedi, internet kesilemedi.");
            }
        }
    }

    private void yaz(final String metin) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtDurum.setText(metin);
            }
        });
    }

    private int root(String komut) throws Exception {
        Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", komut});
        return p.waitFor();
    }
}
