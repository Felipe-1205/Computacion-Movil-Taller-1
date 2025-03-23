package com.example.talle3;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.talle3.databinding.ActivityMainBinding;
import com.example.talle3.databinding.ActivityPruebitasBinding;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.util.List;

public class Pruebitas extends AppCompatActivity {

    private ActivityPruebitasBinding binding;

    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPruebitasBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        locationReader reader = new locationReader(this);
        List<Ubicaciones> locations = null;
        try {
            locations = reader.readLocations();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
// Itera sobre la lista de locations para hacer lo que necesites
        for (Ubicaciones location : locations) {
            Log.d("LOCATION", location.getName());
            Log.d("LOCATION", String.valueOf(location.getLatitude()));
            Log.d("LOCATION", String.valueOf(location.getLongitude()));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int itemClicked = item.getItemId();
        if(itemClicked == R.id.menuLogOut){
            mAuth.signOut();
            Intent intent = new Intent(Pruebitas.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }else if (itemClicked == R.id.menuSetAvailable){
            Toast.makeText(this, "Estoy disponible", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }
}