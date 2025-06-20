package com.gncftg20.akillievim;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.gncftg20.akillievim.databinding.ActivityEspWifiConfigBinding;
import com.gncftg20.akillievim.models.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EspWifiConfigActivity extends AppCompatActivity {

    private ActivityEspWifiConfigBinding binding;
    private WifiAdapter wifiAdapter;

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_WIFI_STATE
    };

    private WifiManager wifiManager;
    private final String espSSID = "ESP32_Config";
    private final String espPassword = "12345678";

    private FirebaseAuth auth;
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEspWifiConfigBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeFirebase();
        setupUI();
        checkPermissions();
        
        // ESP32 ağına otomatik bağlanma
        if (hasPermissions()) {
            connectToESP32AP();
        }
    }

    private void initializeFirebase() {
        auth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("users");
        loadUserInfo();
    }

    @SuppressLint("SetTextI18n")
    private void setupUI() {
        wifiAdapter = new WifiAdapter(ssid -> 
            binding.textViewWifiSsid.setText("WiFi SSID: " + ssid));
        
        binding.wifiRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.wifiRecyclerView.setAdapter(wifiAdapter);

        binding.buttonScan.setOnClickListener(v -> scanWiFiNetworks());
        binding.buttonSend.setOnClickListener(v -> validateAndSendCredentials());
    }

    private void checkPermissions() {
        if (!hasPermissions()) {
            requestPermissions(REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
        }
    }

    private boolean hasPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && !hasPermissions()) {
            Toast.makeText(this, "İzinler reddedildi!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void scanWiFiNetworks() {
        if (wifiManager == null) {
            wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (wifiManager.startScan()) {
                List<ScanResult> results = wifiManager.getScanResults();
                updateWifiList(results);
            } else {
                showToast("WiFi taraması başlatılamadı");
            }
        } else {
            showToast("WiFi taraması için konum izni gerekli");
            requestPermissions(REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
        }
    }

    private void updateWifiList(List<ScanResult> results) {
        List<String> ssidList = new ArrayList<>();
        for (ScanResult result : results) {
            if (!result.SSID.isEmpty() && !ssidList.contains(result.SSID)) {
                ssidList.add(result.SSID);
            }
        }
        wifiAdapter.updateWifiList(ssidList);
        showToast("WiFi ağları güncellendi");
    }

    private void connectToESP32AP() {
        if (wifiManager == null) {
            wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                .setSsid(espSSID)
                .setWpa2Passphrase(espPassword)
                .build();

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build();

        connectivityManager.requestNetwork(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                Log.d("WiFiConnect", "Bağlantı kuruldu: " + espSSID);
                runOnUiThread(() -> {
                    Toast.makeText(EspWifiConfigActivity.this, "ESP32_Config ağına bağlandı!", Toast.LENGTH_SHORT).show();
                });
                connectivityManager.unregisterNetworkCallback(this);
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                showToast("ESP32_Config ağına bağlanılamadı!");
            }
        });
    }

    private void validateAndSendCredentials() {
        String ssid = binding.textViewWifiSsid.getText().toString().replace("WiFi SSID: ", "");
        String password = Objects.requireNonNull(binding.editTextWifiPassword.getText()).toString().trim();
        String userPassword = Objects.requireNonNull(binding.editTextUserPassword.getText()).toString().trim();

        if (ssid.isEmpty() || password.isEmpty() || userPassword.isEmpty()) {
            showToast("Lütfen tüm alanları doldurun");
            return;
        }

        if (auth.getCurrentUser() == null) {
            showToast("Lütfen giriş yapın");
            return;
        }

        // Kullanıcı şifresini doğrula
        FirebaseUser user = auth.getCurrentUser();
        String email = user.getEmail();
        if (email != null) {

                databaseRef.child(Objects.requireNonNull(auth.getUid())).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User userData = snapshot.getValue(User.class);
                        if (userData != null) {
                            sendToEsp32(ssid, password, userData);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showToast("Kullanıcı bilgileri alınamadı");
                    }

                });

        } else {
            showToast("E-posta alınamadı");
        }

    }

    private void sendToEsp32(String ssid, String password, User user) {
        showProgress(true);
        new Thread(() -> {
            try {
                URL url = new URL("http://192.168.4.1/wifi");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String params = String.format(
                        "ssid=%s&pass=%s&email=%s&uid=%s&userPass=%s",
                        URLEncoder.encode(ssid, "UTF-8"),
                        URLEncoder.encode(password, "UTF-8"),
                        URLEncoder.encode(user.getEmail(), "UTF-8"),
                        URLEncoder.encode(Objects.requireNonNull(auth.getCurrentUser()).getUid(), "UTF-8"),
                        URLEncoder.encode(Objects.requireNonNull(binding.editTextUserPassword.getText()).toString(), "UTF-8")
                );

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(params.getBytes());
                }

                runOnUiThread(() -> {
                    try {
                        if (conn.getResponseCode() == 200) {
                            showToast("Başarıyla gönderildi");
                        } else {
                            showToast("Gönderim hatası: " + conn.getResponseCode());
                        }
                    } catch (IOException e) {
                        showToast("IOException: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                showToast("Hata: " + e.getMessage());
            } finally {
                showProgress(false);
            }
        }).start();
    }

    private void loadUserInfo() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            databaseRef.child(user.getUid()).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    User userData = task.getResult().getValue(User.class);
                    if (userData != null) {
                        binding.textViewUserName.setText("Kullanıcı: " + userData.getName());
                        binding.textViewUserEmail.setText("E-posta: " + user.getEmail());
                        binding.textViewUserUid.setText("UID: " + user.getUid());
                    }
                }
            });
        }
    }

    private void showProgress(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        setUIEnabled(!show);
    }

    private void setUIEnabled(boolean enabled) {
        binding.buttonScan.setEnabled(enabled);
        binding.buttonSend.setEnabled(enabled);
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
}
