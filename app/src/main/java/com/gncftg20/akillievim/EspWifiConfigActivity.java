package com.gncftg20.akillievim;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class EspWifiConfigActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private WifiManager wifiManager;
    private EditText editTextSSID, editTextPassword;
    private ArrayAdapter<String> wifiListAdapter;
    private List<ScanResult> wifiList;
    Button buttonFetchFromEsp;

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_esp_wifi_config);

        // UI Bağlantıları
        editTextSSID = findViewById(R.id.editTextSSID);
        editTextPassword = findViewById(R.id.editTextPassword);
        Button buttonScan = findViewById(R.id.buttonScan);
        Button buttonSend = findViewById(R.id.buttonSend);
        ListView wifiListView = findViewById(R.id.wifiListView);
        buttonFetchFromEsp = findViewById(R.id.buttonFetchFromEsp);
        buttonFetchFromEsp.setOnClickListener(v -> fetchNetworksFromEsp32());

        // WiFi Manager başlat
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        wifiList = new ArrayList<>();
        wifiListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        wifiListView.setAdapter(wifiListAdapter);

        // İzinleri kontrol et
        if (!hasPermissions()) {
            requestPermissions();
        }

        // WiFi Ağı tarama
        buttonScan.setOnClickListener(v -> {


                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    wifiManager.startScan();
                    wifiList = wifiManager.getScanResults();
                    wifiListAdapter.clear();
                    for (ScanResult result : wifiList) {
                        wifiListAdapter.add(result.SSID);
                    }
                    wifiListAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Ağlar tarandı", Toast.LENGTH_SHORT).show();
                }


        });

        // Listeden SSID seçilince inputa yaz
        wifiListView.setOnItemClickListener((parent, view, position, id) -> {
            ScanResult selected = wifiList.get(position);
            editTextSSID.setText(selected.SSID);
        });

        // ESP32'ye HTTP ile veri gönder
        buttonSend.setOnClickListener(v -> {
            String ssid = editTextSSID.getText().toString();
            String password = editTextPassword.getText().toString();

            if (ssid.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Lütfen SSID ve şifre girin", Toast.LENGTH_SHORT).show();
                return;
            }

            sendCredentialsToEsp32(ssid, password);
        });
    }

    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION
        }, PERMISSION_REQUEST_CODE);
    }
    private void fetchNetworksFromEsp32() {
        new Thread(() -> {
            try {
                URL url = new URL("http://192.168.4.1/scan"); // ESP32’nin tarama endpoint’i
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONArray jsonArray = new JSONArray(response.toString());
                    runOnUiThread(() -> {
                        wifiListAdapter.clear();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            try {
                                String ssid = jsonArray.getJSONObject(i).getString("ssid");
                                wifiListAdapter.add(ssid);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        wifiListAdapter.notifyDataSetChanged();
                        Toast.makeText(this, "ESP32 WiFi ağları yüklendi", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "ESP32 cevap vermedi!", Toast.LENGTH_SHORT).show());
                }
                conn.disconnect();
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void sendCredentialsToEsp32(String ssid, String password) {
        new Thread(() -> {
            try {
                URL url = new URL("http://192.168.4.1/wifi"); // ESP32'nin AP IP'si
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setDoOutput(true);

                String postData = "ssid=" + URLEncoder.encode(ssid, "UTF-8") +
                        "&password=" + URLEncoder.encode(password, "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                runOnUiThread(() -> {
                    if (responseCode == 200) {
                        Toast.makeText(this, "WiFi bilgileri ESP32'ye gönderildi!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Gönderim başarısız. Kod: " + responseCode, Toast.LENGTH_SHORT).show();
                    }
                });

                conn.disconnect();

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
