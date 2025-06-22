package com.gncftg20.akillievim;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private String userId;
    private TextInputEditText nameEditText, surnameEditText, emailEditText;
    private TextInputEditText currentPasswordEditText, newPasswordEditText, confirmPasswordEditText;
    private MaterialButton saveButton;
    private MaterialButton logoutButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Firebase referanslarını başlat
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        // UI elemanlarını bağla
        initializeViews();
        
        // Mevcut kullanıcı bilgilerini yükle
        loadUserData();
    }

    private void initializeViews() {
        nameEditText = findViewById(R.id.nameEditText);
        surnameEditText = findViewById(R.id.surnameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        currentPasswordEditText = findViewById(R.id.currentPasswordEditText);
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        saveButton = findViewById(R.id.saveButton);
        logoutButton = findViewById(R.id.logoutButton);


        saveButton.setOnClickListener(v -> saveChanges());
        logoutButton.setOnClickListener(v -> logout());
    }

    
    private void loadUserData() {
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String surname = snapshot.child("surname").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);

                    nameEditText.setText(name);
                    surnameEditText.setText(surname);
                    emailEditText.setText(email);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("SettingsActivity", "Veri yükleme hatası: " + error.getMessage());
                Toast.makeText(SettingsActivity.this, 
                    "Kullanıcı bilgileri yüklenirken hata oluştu", 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveChanges() {
        String name = Objects.requireNonNull(nameEditText.getText()).toString().trim();
        String surname = Objects.requireNonNull(surnameEditText.getText()).toString().trim();
        String email = Objects.requireNonNull(emailEditText.getText()).toString().trim();
        String currentPassword = Objects.requireNonNull(currentPasswordEditText.getText()).toString().trim();
        String newPassword = Objects.requireNonNull(newPasswordEditText.getText()).toString().trim();
        String confirmPassword = Objects.requireNonNull(confirmPasswordEditText.getText()).toString().trim();

        // Boş alan kontrolü
        if (name.isEmpty() || surname.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show();
            return;
        }

        // Şifre değişikliği yapılacaksa
        if (!currentPassword.isEmpty() || !newPassword.isEmpty() || !confirmPassword.isEmpty()) {
            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Şifre değişikliği için tüm alanları doldurun", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "Yeni şifreler eşleşmiyor", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 6) {
                Toast.makeText(this, "Şifre en az 6 karakter olmalıdır", Toast.LENGTH_SHORT).show();
                return;
            }

            // Şifre değişikliği
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                user.updatePassword(newPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            updateUserProfile(name, surname, email);
                        } else {
                            Toast.makeText(SettingsActivity.this,
                                "Şifre değiştirilemedi: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                        }
                    });
            }
        } else {
            // Sadece profil bilgilerini güncelle
            updateUserProfile(name, surname, email);
        }
    }

    private void updateUserProfile(String name, String surname, String email) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("users/" + userId + "/name", name);
        updates.put("users/" + userId + "/surname", surname);
        updates.put("users/" + userId + "/email", email);

        mDatabase.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SettingsActivity.this, 
                        "Profil başarıyla güncellendi", 
                        Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SettingsActivity.this,
                        "Profil güncellenirken hata oluştu: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                });
    }

    private void updateDeviceStatus(String deviceId, boolean status) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("users/" + userId + "/Kontrol/" + deviceId + "/durum", status);
        
        mDatabase.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("SettingsActivity", deviceId + " durumu güncellendi: " + status);
                })
                .addOnFailureListener(e -> {
                    Log.e("SettingsActivity", "Güncelleme hatası: " + e.getMessage());
                    Toast.makeText(SettingsActivity.this, 
                        "Ayar güncellenirken hata oluştu: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
    }

    private void updateThermostatSetting(String setting, Object value) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("users/" + userId + "/Kontrol/termostat/" + setting, value);
        
        mDatabase.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("SettingsActivity", "Termostat ayarı güncellendi: " + setting);
                })
                .addOnFailureListener(e -> {
                    Log.e("SettingsActivity", "Güncelleme hatası: " + e.getMessage());
                    Toast.makeText(SettingsActivity.this, 
                        "Ayar güncellenirken hata oluştu: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        // Firebase'den çıkış yap
        mAuth.signOut();
        
        // Login ekranına yönlendir
        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
} 