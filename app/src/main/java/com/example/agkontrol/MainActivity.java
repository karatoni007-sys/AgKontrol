package com.example.agkontrol;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;

public class MainActivity extends Activity {

    // Cihazınızda arayüz adları farklıysa burayı değiştirin.
    private static final String WIFI = "wlan0";
    private static final String ETHERNET = "eth0";

    private TextView txtDurum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtDurum = (TextView) findViewById(R.id.txtDurum);
        Button btnKes = (Button) findViewById(R.id.btnKes);
        Button btnAc = (Button) findViewById(R.id.btnAc);

        btnKes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uygula("down");
            }
        });

        btnAc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uygula("up");
            }
        });
    }

    private void uygula(final String yon) {
        txtDurum.setText("Çalışıyor...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String sonuc;
                try {
                    int wifi = root("ifconfig " + WIFI + " " + yon);
                    int eth = root("ifconfig " + ETHERNET + " " + yon);
                    String islem = yon.equals("down") ? "kapatıldı" : "açıldı";
                    sonuc = "Wi-Fi: " + (wifi == 0 ? islem : "HATA")
                            + "  |  Ethernet: " + (eth == 0 ? islem : "HATA");
                } catch (Exception e) {
                    final String hata = (e instanceof IOException)
                            ? "Root yetkisi bulunamadı (su çalışmadı)."
                            : "Hata: " + e.getMessage();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtDurum.setText(hata);
                        }
                    });
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtDurum.setText(sonuc);
                    }
                });
            }
        }).start();
    }

    private int root(String komut) throws Exception {
        Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", komut});
        return p.waitFor();
    }
}
