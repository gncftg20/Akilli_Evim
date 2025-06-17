package com.gncftg20.akillievim;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.gncftg20.akillievim.models.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText emailEditText, passwordEditText;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> signInLauncher;
    private DatabaseReference mDatabase;
    private static final int RC_SIGN_IN = 123;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("users");

        // Google Sign-In yapılandırması
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("446189604516-btv00tdptim8pn63jjir9229d3o14fld.apps.googleusercontent.com") // Firebase Console'dan alınan Web Client ID
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Activity Result Launcher'ı başlat
        signInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            firebaseAuthWithGoogle(account.getIdToken());
                        }
                    } catch (ApiException e) {
                        Toast.makeText(LoginActivity.this, "Google girişi başarısız: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        // UI elemanlarını bağla
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        MaterialButton loginButton = findViewById(R.id.loginButton);
        MaterialButton googleSignInButton = findViewById(R.id.googleSignInButton);

        loginButton.setOnClickListener(v -> loginUser());
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
        
        // Kayıt ol butonuna tıklandığında RegisterActivity'ye yönlendir
        findViewById(R.id.registerTextView).setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w(TAG, "Google girişi başarısız", e);
                Toast.makeText(LoginActivity.this, "Google ile giriş başarısız: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Kullanıcı bilgilerini veritabanına kaydet
                            saveUserToDatabase(user);
                        }
                    } else {
                        Log.w(TAG, "Firebase kimlik doğrulama başarısız", task.getException());
                        Toast.makeText(LoginActivity.this, "Kimlik doğrulama başarısız: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToDatabase(FirebaseUser firebaseUser) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        String userId = firebaseUser.getUid();

        // Kullanıcı bilgilerini hazırla
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", firebaseUser.getDisplayName() != null ? 
                firebaseUser.getDisplayName().split(" ")[0] : "");
        userData.put("surname", firebaseUser.getDisplayName() != null && 
                firebaseUser.getDisplayName().split(" ").length > 1 ? 
                firebaseUser.getDisplayName().split(" ")[1] : "");
        userData.put("email", firebaseUser.getEmail());

        // Kullanıcı verilerini veritabanına kaydet
        usersRef.child(userId).setValue(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Kullanıcı verileri başarıyla kaydedildi");
                    // Cihaz kontrol verilerini oluştur
                    setupDeviceControls(userId);
                    // Ana ekrana yönlendir
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Kullanıcı verileri kaydedilemedi", e);
                    Toast.makeText(LoginActivity.this, 
                            "Kullanıcı verileri kaydedilemedi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
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
        cihaz2.put("ad", "Cihaz 2");
        cihaz2.put("durum", false);
        cihazlar.put("cihaz2", cihaz2);

        // Cihaz 3
        Map<String, Object> cihaz3 = new HashMap<>();
        cihaz3.put("ad", "Cihaz 3");
        cihaz3.put("durum", false);
        cihazlar.put("cihaz3", cihaz3);

        // Cihaz 4
        Map<String, Object> cihaz4 = new HashMap<>();
        cihaz4.put("ad", "Cihaz 4");
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
                    Log.d(TAG, "Cihaz kontrol verileri başarıyla kaydedildi");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Cihaz kontrol verileri kaydedilemedi: " + e.getMessage());
                });
    }

    private void loginUser() {
        String email = Objects.requireNonNull(emailEditText.getText()).toString().trim();
        String password = Objects.requireNonNull(passwordEditText.getText()).toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Lütfen e-posta ve şifrenizi girin", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                        // Kullanıcı bilgilerini kontrol et
                        mDatabase.child(userId).get().addOnCompleteListener(dbTask -> {
                            if (dbTask.isSuccessful() && dbTask.getResult().exists()) {
                                Toast.makeText(LoginActivity.this, "Giriş başarılı", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            } else {
                                // Kullanıcı bilgileri bulunamadı, oturumu kapat
                                mAuth.signOut();
                                Toast.makeText(LoginActivity.this,
                                        "Kullanıcı bilgileri bulunamadı. Lütfen tekrar kayıt olun.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        String errorMessage = "Giriş başarısız: ";
                        if (task.getException() != null) {
                            String message = task.getException().getMessage();
                            if (message != null) {
                                if (message.contains("no user record")) {
                                    errorMessage += "Bu e-posta adresi ile kayıtlı kullanıcı bulunamadı";
                                } else if (message.contains("password is invalid")) {
                                    errorMessage += "Şifre hatalı";
                                } else {
                                    errorMessage += message;
                                }
                            }
                        }
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}