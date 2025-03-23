package com.example.talle3;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.talle3.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private static final int PERMISSION_REQUEST_CODE = 123;

    private static final String TAG = "lOGIN";


    protected void onResume() {
        super.onResume();
        mAuth.signOut();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        requestNotificationPermission();
        Intent intent2 = new Intent(MainActivity.this, ServicioDisponibilidadUsuario.class);
        startService(intent2);
        mAuth = FirebaseAuth.getInstance();

        binding.registro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), Registro.class);
                startActivity(intent);
            }
        });

    }


    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
        binding.iniciarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(validateForm()){
                    sigin();
                }
            }
        });

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, Registro.class);
                startActivity(intent);
            }
        };
        SpannableString spannableString = new SpannableString("Registrarse");
        spannableString.setSpan(clickableSpan, 0, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        binding.registro.setText(spannableString);
        binding.registro.setMovementMethod(LinkMovementMethod.getInstance());
    }
    private void updateUI(FirebaseUser currentUser){
        if(currentUser!=null){
            Log.e("activado", "currentUser es nulo");
            Intent intent = new Intent(getBaseContext(), Mapa.class);
            //intent.putExtra("user", currentUser.getDisplayName().toString());
            startActivity(intent);
        } else {
            binding.usuarioIn.setText("");
            binding.contraIn.setText("");
        }
    }
    private void sigin(){
        mAuth.signInWithEmailAndPassword(binding.usuarioIn.getText().toString().trim(), binding.contraIn.getText().toString().trim()).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "signInWithEmail:success");
                    FirebaseUser user = mAuth.getCurrentUser();
                    updateUI(user);
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.getException());
                    Toast.makeText(MainActivity.this, "Authentication failed.",
                            Toast.LENGTH_SHORT).show();
                    updateUI(null);
                }
            }
        });
    }
    private boolean validateForm() {
        boolean valid = true;
        String email =binding.usuarioIn.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            binding.usuarioIn.setError("Required.");
            valid = false;
        } else if (!email.contains("@") || !email.contains(".") || email.length() < 5) {
            valid = false;
        } else {
            binding.usuarioIn.setError(null);
        }
        String password = binding.contraIn.getText().toString().trim();
        if (TextUtils.isEmpty(password)) {
            binding.contraIn.setError("Required.");
            valid = false;
        } else {
            binding.contraIn.setError(null);
        }
        return valid;
    }
    private void requestNotificationPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.POST_NOTIFICATIONS)) {
                Toast.makeText(MainActivity.this, "sin notificaciones no se avisara de actividad", Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                Toast.makeText(MainActivity.this, "las notificaciones fueron denegadas", Toast.LENGTH_SHORT).show();
            }
        }
    }
}