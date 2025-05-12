package com.gncftg20.akillievim;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.ArrayAdapter;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EspWifiConfigActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_ADVERTISE
    };

    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private EditText editTextSSID, editTextPassword;
    private ArrayAdapter<String> deviceListAdapter;
    private List<BluetoothDevice> deviceList;

    private static final UUID WIFI_SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    private static final UUID WIFI_SSID_CHARACTERISTIC_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");
    private static final UUID WIFI_PASSWORD_CHARACTERISTIC_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a9");

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_esp_wifi_config);

        // İzinleri kontrol et
        if (!checkPermissions()) {
            requestPermissions();
        }

        initializeBluetooth();
        initializeViews();
    }

    private boolean checkPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void initializeBluetooth() {
        var bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bu cihaz Bluetooth'u desteklemiyor", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            Toast.makeText(this, "BLE tarayıcı başlatılamadı", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeViews() {
        deviceList = new ArrayList<>();
        Button buttonConnect = findViewById(R.id.buttonConnect);
        editTextSSID = findViewById(R.id.editTextSSID);
        editTextPassword = findViewById(R.id.editTextPassword);
        ListView deviceListView = findViewById(R.id.deviceListView);

        deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        deviceListView.setAdapter(deviceListAdapter);

        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < deviceList.size()) {
                BluetoothDevice device = deviceList.get(position);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                connectToDevice(device);
            }
        });

        buttonConnect.setOnClickListener(v -> {
            if (checkPermissions()) {
                startScanningForBLEDevices();
            } else {
                requestPermissions();
            }
        });
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void startScanningForBLEDevices() {
        deviceList.clear();
        deviceListAdapter.clear();

        ScanCallback scanCallback = new ScanCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice device = result.getDevice();
                String name = device.getName();
                String address = device.getAddress();

                if (name != null /*&& name.contains("ESP32")*/) {
                    runOnUiThread(() -> {
                        if (!deviceList.contains(device)) {
                            deviceList.add(device);
                            deviceListAdapter.add(name + " - " + address);
                        }
                    });
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                runOnUiThread(() -> Toast.makeText(EspWifiConfigActivity.this, "Tarama başarısız: " + errorCode, Toast.LENGTH_SHORT).show());
            }
        };

        bluetoothLeScanner.startScan(scanCallback);
    }



    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void connectToDevice(BluetoothDevice device) {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
        Toast.makeText(this, "Bağlanılıyor...", Toast.LENGTH_SHORT).show();
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                runOnUiThread(() -> Toast.makeText(EspWifiConfigActivity.this, "Bağlantı başarılı!", Toast.LENGTH_SHORT).show());

                gatt.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                runOnUiThread(() -> Toast.makeText(EspWifiConfigActivity.this, "Bağlantı kesildi", Toast.LENGTH_SHORT).show());
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService wifiService = gatt.getService(WIFI_SERVICE_UUID);
                if (wifiService != null) {
                    sendWifiCredentials(wifiService);
                } else {
                    runOnUiThread(() -> Toast.makeText(EspWifiConfigActivity.this, "WiFi servisi bulunamadı", Toast.LENGTH_SHORT).show());
                }
            }
        }
    };

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void sendWifiCredentials(BluetoothGattService service) {
        String ssid = editTextSSID.getText().toString();
        String password = editTextPassword.getText().toString();

        if (ssid.isEmpty() || password.isEmpty()) {
            runOnUiThread(() -> Toast.makeText(this, "Lütfen WiFi bilgilerini girin", Toast.LENGTH_SHORT).show());
            return;
        }

        BluetoothGattCharacteristic ssidChar = service.getCharacteristic(WIFI_SSID_CHARACTERISTIC_UUID);
        BluetoothGattCharacteristic passwordChar = service.getCharacteristic(WIFI_PASSWORD_CHARACTERISTIC_UUID);

        if (ssidChar != null && passwordChar != null) {
            ssidChar.setValue(ssid);
            passwordChar.setValue(password);

            bluetoothGatt.writeCharacteristic(ssidChar);
            bluetoothGatt.writeCharacteristic(passwordChar);

            runOnUiThread(() -> Toast.makeText(this, "WiFi bilgileri gönderildi!", Toast.LENGTH_SHORT).show());
        } else {
            runOnUiThread(() -> Toast.makeText(this, "WiFi karakteristikleri bulunamadı", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothGatt != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            bluetoothGatt.close();
        }
    }
}
