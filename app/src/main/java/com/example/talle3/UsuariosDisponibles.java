package com.example.talle3;
import static android.content.ContentValues.TAG;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.talle3.adapters.usuariosAdapter;
import com.example.talle3.databinding.ActivityUsuariosDisponiblesBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class UsuariosDisponibles extends AppCompatActivity {

    private FirebaseAuth mAuth;
    FirebaseDatabase database;
    public static final String PATH_USERS="users/";

    private ActivityUsuariosDisponiblesBinding binding;
    private usuariosAdapter mUserAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUsuariosDisponiblesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mAuth = FirebaseAuth.getInstance();
        initUsers();
        List<User> users = new ArrayList<>();
        mUserAdapter = new usuariosAdapter(this, R.layout.contactsrow, users);
        binding.listDisponibles.setAdapter(mUserAdapter);
    }

    public void initUsers(){
        database = FirebaseDatabase.getInstance();
        List<User> users = new ArrayList<>();
        // Realiza una consulta para obtener los usuarios disponibles
        Query query = database.getReference(PATH_USERS).orderByChild("disponible").equalTo(1);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Itera sobre los resultados de la consulta
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    // Obtiene el objeto User correspondiente al usuario actual
                    User user = userSnapshot.getValue(User.class);
                    if (user != null) {
                        // Obtiene el nombre y la foto de perfil del usuario actual
                        String name = user.getName();
                        user.setUserID(userSnapshot.getKey());
                        //String photoUrl = user.getPhotoUrl();
                        // Aquí puedes hacer lo que necesites con el nombre y la foto del usuario actual
                        Log.d("DATAUSERAVALIABLE", "Nombre: " + name + ", Foto de perfil: " );
                        users.add(user);
                    }
                }
                mUserAdapter.clear();
                mUserAdapter.addAll(users);
                mUserAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Error de lectura de la base de datos
                Log.w("TAG", "Error al leer los datos.", error.toException());
            }
        });

    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuLogOut:
                mAuth.signOut();
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            case R.id.menuSetAvailable:
                FirebaseUser user = mAuth.getCurrentUser();
                DatabaseReference myRef = database.getReference(PATH_USERS + user.getUid());
                myRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Recupera los datos del usuario y haz lo que necesites con ellos
                        User myUser = dataSnapshot.getValue(User.class);
                        String name = myUser.getName();
                        String email = myUser.getEmail();

                        if (myUser.getDisponible() == 1) {
                            Toast.makeText(UsuariosDisponibles.this, "ya estás disponible", Toast.LENGTH_SHORT).show();
                        }else{
                            myUser.setDisponible(1);
                            myRef.setValue(myUser); // actualiza el valor en la base de datos
                            // Muestra un mensaje de éxito
                            Toast.makeText(UsuariosDisponibles.this, "Ahora estás disponible", Toast.LENGTH_SHORT).show();
                        }
                        myRef.removeEventListener(this);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Error de lectura de la base de datos
                        Log.w(TAG, "Error al leer los datos.", error.toException());
                    }

                });
                return true;
            case R.id.menuSetnotAvailable:
                user = mAuth.getCurrentUser();
                myRef = database.getReference(PATH_USERS + user.getUid());
                myRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Recupera los datos del usuario y haz lo que necesites con ellos
                        User myUser = dataSnapshot.getValue(User.class);
                        String name = myUser.getName();
                        String email = myUser.getEmail();

                        if (myUser.getDisponible() == 0) {
                            Toast.makeText(UsuariosDisponibles.this, "ya no estás disponible", Toast.LENGTH_SHORT).show();
                        }else{
                            myUser.setDisponible(0);
                            myRef.setValue(myUser); // actualiza el valor en la base de datos
                            // Muestra un mensaje de éxito
                            Toast.makeText(UsuariosDisponibles.this, "Ahora no estás disponible", Toast.LENGTH_SHORT).show();
                        }
                        myRef.removeEventListener(this);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Error de lectura de la base de datos
                        Log.w(TAG, "Error al leer los datos.", error.toException());
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

}