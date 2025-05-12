package com.gncftg20.akillievim;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Kullanıcı bilgilerini veritabanına kaydet
                        String userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                        Map<String, Object> user = new HashMap<>();
                        user.put("name", name);
                        user.put("surname", surname);
                        user.put("email", email);

                        mDatabase.child(userId).setValue(user)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(RegisterActivity.this, "Kayıt başarılı", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> Toast.makeText(RegisterActivity.this,
                                        "Kullanıcı bilgileri kaydedilemedi: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(RegisterActivity.this, "Kayıt başarısız: " +
                                Objects.requireNonNull(task.getException()).getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
} 