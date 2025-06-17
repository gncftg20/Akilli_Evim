package com.gncftg20.akillievim;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.switchmaterial.SwitchMaterial;






import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {

    private DatabaseReference rootRef;
    private FirebaseAuth mAuth;

    private Button buttonbaglan;
    private SwitchMaterial switchCihaz1, switchCihaz2, switchCihaz3, switchCihaz4;
    private SwitchMaterial switchEkomod, switchKombiOnOff;
    private SeekBar seekBarHedefDerece;
    private TextView textHedefDerece, textMevcutDerece,textnem;
    private TextView textCihaz1Ad, textCihaz2Ad, textCihaz3Ad, textCihaz4Ad;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);
        
        // Firebase Auth ve Database referanslarını başlat
        mAuth = FirebaseAuth.getInstance();
        String userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        rootRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("Kontrol");

        // Ayarlar butonunu bağla
        findViewById(R.id.ProfilAyarları).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });

        // UI Elemanlarını bağla
        switchCihaz1 = findViewById(R.id.switch_cihaz1);
        switchCihaz2 = findViewById(R.id.switch_cihaz2);
        switchCihaz3 = findViewById(R.id.switch_cihaz3);
        switchCihaz4 = findViewById(R.id.switch_cihaz4);
        buttonbaglan= findViewById(R.id.button2);
        buttonbaglan.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EspWifiConfigActivity.class);
            startActivity(intent);
        });
        switchEkomod = findViewById(R.id.switch_ekomod);
        switchKombiOnOff = findViewById(R.id.switch_kombionoff);
        seekBarHedefDerece = findViewById(R.id.seekbar_hedef_derece);
        textHedefDerece = findViewById(R.id.text_hedef_derece);
        textMevcutDerece = findViewById(R.id.text_mevcut_derece);
        textnem = findViewById(R.id.text_nem);
        // Cihaz isimleri için TextView'ları bağla
        textCihaz1Ad = findViewById(R.id.text_cihaz1_ad);
        textCihaz2Ad = findViewById(R.id.text_cihaz2_ad);
        textCihaz3Ad = findViewById(R.id.text_cihaz3_ad);
        textCihaz4Ad = findViewById(R.id.text_cihaz4_ad);

        // Cihaz isimlerini dinle ve güncelle
        setupCihazAdi(textCihaz1Ad, "cihaz1");
        setupCihazAdi(textCihaz2Ad, "cihaz2");
        setupCihazAdi(textCihaz3Ad, "cihaz3");
        setupCihazAdi(textCihaz4Ad, "cihaz4");

        // Cihaz kontrolleri
        setupSwitchMaterial(switchCihaz1, "cihaz1/durum");
        setupSwitchMaterial(switchCihaz2, "cihaz2/durum");
        setupSwitchMaterial(switchCihaz3, "cihaz3/durum");
        setupSwitchMaterial(switchCihaz4, "cihaz4/durum");

        // Termostat Switchleri
        setupSwitchMaterial(switchEkomod, "termostat/ekomod");
        setupSwitchMaterial(switchKombiOnOff, "termostat/kombionoff");

        final float minDeger = 16.0f;
        final float maxDeger = 30.0f;
        final float adim = 0.1f;

        int seekBarMax = (int) ((maxDeger - minDeger) / adim);
        seekBarHedefDerece.setMax(seekBarMax);

        seekBarHedefDerece.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float derece = minDeger + (progress * adim);
                float yuvarlanmisDerece = Math.round(derece * 10) / 10.0f;
                rootRef.child("termostat").child("hedefderece").setValue(yuvarlanmisDerece);
                textHedefDerece.setText("Hedef Sıcaklık: " + String.format("%.1f", derece) + "°C");
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        rootRef.child("termostat").child("hedefderece").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double derece = snapshot.getValue(Double.class);
                if (derece != null) {
                    int progress = (int) Math.round((derece - minDeger) / adim);
                    seekBarHedefDerece.setProgress(progress);
                    textHedefDerece.setText("Hedef Sıcaklık: " + String.format("%.1f", derece) + "°C");
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Mevcut derece
        rootRef.child("termostat").child("mevcutderece").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                float mevcut = snapshot.getValue(float.class);
                if (mevcut != 0) {
                    textMevcutDerece.setText("Oda  Sıcaklığı: " + String.format("%.1f", mevcut) + "°C");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
        rootRef.child("termostat").child("nem").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int nem = snapshot.getValue(int.class);
                if (nem != 0) {
                    textnem.setText("Nem: %" + nem);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }


    private void setupSwitchMaterial(SwitchMaterial sw, String cihazKey) {
        rootRef.child(cihazKey).addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean durum = snapshot.getValue(Boolean.class);
                if (durum != null) sw.setChecked(durum);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });

        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            rootRef.child(cihazKey).setValue(isChecked);
        });
    }

    private void setupTermostatSwitch(Switch sw, String key) {
        rootRef.child("termostat").child(key).addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean durum = snapshot.getValue(Boolean.class);
                if (durum != null) sw.setChecked(durum);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });

        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            rootRef.child("termostat").child(key).setValue(isChecked);
        });
    }

    private void setupCihazAdi(TextView textView, String cihazKey) {
        // Mevcut cihaz adını dinle
        rootRef.child(cihazKey).child("ad").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String ad = snapshot.getValue(String.class);
                if (ad != null) {
                    textView.setText(ad);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        // TextView'a tıklandığında ismi güncelle
        textView.setOnClickListener(v -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("Cihaz Adını Değiştir");

            final android.widget.EditText input = new android.widget.EditText(this);
            input.setText(textView.getText());
            builder.setView(input);

            builder.setPositiveButton("Tamam", (dialog, which) -> {
                String yeniAd = input.getText().toString().trim();
                if (!yeniAd.isEmpty()) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put(cihazKey + "/ad", yeniAd);
                    rootRef.updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(MainActivity.this, "Cihaz adı güncellendi", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(MainActivity.this, "Güncelleme başarısız: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            });
            builder.setNegativeButton("İptal", (dialog, which) -> dialog.cancel());

            builder.show();
        });
    }





}
