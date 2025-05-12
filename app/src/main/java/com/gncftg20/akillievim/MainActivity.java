package com.gncftg20.akillievim;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import com.google.android.material.switchmaterial.SwitchMaterial;






import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.*;



public class MainActivity extends AppCompatActivity {

    private DatabaseReference rootRef;

    private Button buttonbaglan;
    private SwitchMaterial switchCihaz1, switchCihaz2, switchCihaz3, switchCihaz4;
    private SwitchMaterial switchEkomod, switchKombiOnOff;
    private SeekBar seekBarHedefDerece;
    private TextView textHedefDerece, textMevcutDerece;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);
        rootRef = FirebaseDatabase.getInstance().getReference("Kontrol");

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

        // Cihaz kontrolleri
        setupSwitchMaterial(switchCihaz1, "cihaz1");
        setupSwitchMaterial(switchCihaz2, "cihaz2");
        setupSwitchMaterial(switchCihaz3, "cihaz3");
        setupSwitchMaterial(switchCihaz4, "cihaz4");

        // Termostat Switchleri
        setupSwitchMaterial(switchEkomod, "ekomod");
        setupSwitchMaterial(switchKombiOnOff, "kombionoff");

        // Hedef derece
        rootRef.child("termostat").child("hedefderece").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long derece = snapshot.getValue(Long.class);
                if (derece != null) {
                    seekBarHedefDerece.setProgress(derece.intValue());
                    textHedefDerece.setText("Hedef Sıcaklık: " + derece + "°C");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        seekBarHedefDerece.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textHedefDerece.setText("Hedef Sıcaklık: " + progress + "°C");
                rootRef.child("termostat").child("hedefderece").setValue(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        // Mevcut derece
        rootRef.child("termostat").child("mevcutderece").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long mevcut = snapshot.getValue(Long.class);
                if (mevcut != null) {
                    textMevcutDerece.setText("Mevcut Sıcaklık: " + mevcut + "°C");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void setupSwitchMaterial(SwitchMaterial sw, String cihazKey) {
        rootRef.child(cihazKey).child("durum").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean durum = snapshot.getValue(Boolean.class);
                if (durum != null) sw.setChecked(durum);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });

        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            rootRef.child(cihazKey).child("durum").setValue(isChecked);
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





}
