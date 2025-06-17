package com.gncftg20.akillievim;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.gncftg20.akillievim.models.User;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {
    private TextInputEditText nameEditText, surnameEditText, emailEditText, passwordEditText;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("users");

        // UI elemanlarını bağla
        nameEditText = findViewById(R.id.nameEditText);
        surnameEditText = findViewById(R.id.surnameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        MaterialButton registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(v -> registerUser());
        findViewById(R.id.loginTextView).setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String name = Objects.requireNonNull(nameEditText.getText()).toString().trim();
        String surname = Objects.requireNonNull(surnameEditText.getText()).toString().trim();
        String email = Objects.requireNonNull(emailEditText.getText()).toString().trim();
        String password = Objects.requireNonNull(passwordEditText.getText()).toString().trim();

        if (name.isEmpty() || surname.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Şifre en az 6 karakter olmalıdır", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                        User user = new User(name, surname, email);

                        // Kullanıcı bilgilerini kaydet
                        mDatabase.child(userId).setValue(user)
                                .addOnSuccessListener(aVoid -> {
                                    // Cihaz kontrol verilerini kaydet
                                    setupDeviceControls(userId);
                                    
                                    Toast.makeText(RegisterActivity.this, "Kayıt başarılı", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(RegisterActivity.this,
                                            "Kullanıcı bilgileri kaydedilemedi: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                    mAuth.getCurrentUser().delete();
                                });
                    } else {
                        String errorMessage = "Kayıt başarısız: ";
                        if (task.getException() != null) {
                            String message = task.getException().getMessage();
                            if (message != null && message.contains("email address is already in use")) {
                                errorMessage += "Bu e-posta adresi zaten kullanımda";
                            } else {
                                errorMessage += message;
                            }
                        }
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupDeviceControls(String userId) {
        DatabaseReference kontrolRef = mDatabase.child(userId).child("Kontrol");

        // Cihaz durumlarını ayarla
        Map<String, Object> cihazlar = new HashMap<>();
        
        // Cihaz 1
        Map<String, Object> cihaz1 = new HashMap<>();
        cihaz1.put("ad", "Cihaz 1");
        cihaz1.put("durum", true);
        cihazlar.put("cihaz1", cihaz1);

        // Cihaz 2
        Map<String, Object> cihaz2 = new HashMap<>();
        cihaz1.put("ad", "Cihaz 2");
        cihaz2.put("durum", false);
        cihazlar.put("cihaz2", cihaz2);

        // Cihaz 3
        Map<String, Object> cihaz3 = new HashMap<>();
        cihaz1.put("ad", "Cihaz 3");
        cihaz3.put("durum", false);
        cihazlar.put("cihaz3", cihaz3);

        // Cihaz 4
        Map<String, Object> cihaz4 = new HashMap<>();
        cihaz1.put("ad", "Cihaz 4");
        cihaz4.put("durum", false);
        cihazlar.put("cihaz4", cihaz4);

        // Termostat ayarları
        Map<String, Object> termostat = new HashMap<>();
        termostat.put("ekomod", true);
        termostat.put("hedefderece", 24);
        termostat.put("kombionoff", true);
        termostat.put("mevcutderece", 0);
        termostat.put("nem", 0);
        cihazlar.put("termostat", termostat);

        // Tüm verileri veritabanına yaz
        kontrolRef.setValue(cihazlar)
                .addOnSuccessListener(aVoid -> {
                    Log.d("RegisterActivity", "Cihaz kontrol verileri başarıyla kaydedildi");
                })
                .addOnFailureListener(e -> {
                    Log.e("RegisterActivity", "Cihaz kontrol verileri kaydedilemedi: " + e.getMessage());
                });
    }
} 