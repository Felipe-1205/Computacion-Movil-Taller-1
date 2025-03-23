package com.example.talle3;

import static android.content.ContentValues.TAG;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.location.Location;
import android.os.Bundle;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.talle3.databinding.ActivityMapaBinding;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.io.IOException;

import java.util.Date;
import java.util.List;



public class Mapa extends AppCompatActivity implements OnMapReadyCallback {

    private FirebaseAuth mAuth;
    FirebaseDatabase database;
    public static final String PATH_USERS="users/";

    private static final double RADIUS_OF_EARTH_KM = 6371;
    private GoogleMap mMap;
    private ActivityMapaBinding binding;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    private Boolean settingsOK;


    SensorManager sensorManager;
    Sensor lightSensor;
    SensorEventListener lightSensorListener;


    //Posisción
    private double currentLatitude;
    private double currentLongitude;
    private double targetLatitude;
    private double targetLongitude;


    String[] names = new String[100];
    double[] latitudes = new double[100];
    double[] longitudes = new double[100];

    ActivityResultLauncher<IntentSenderRequest> getLocationSettings =
            registerForActivityResult(
                    new ActivityResultContracts.StartIntentSenderForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            //Log.i(IndexActivity.TAG, "Result from settings: "+result.getResultCode());
                            if (result.getResultCode() == RESULT_OK) {
                                settingsOK = true;
                                startLocationUpdates();
                            } else {
                                Log.e("GPS", "No GPS available");
                            }
                        }
                    });


    ActivityResultLauncher<String> locationPermission = registerForActivityResult(

            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    initView(result);
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.setHasOptionsMenu(true);
        mapFragment.getMapAsync(this);
        mLocationRequest = createLocationRequest();
        checkLocationSettings();
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        mapFragment.setHasOptionsMenu(true);


        //Localiacion en segundo plano
        FirebaseUser user = mAuth.getCurrentUser();
        Intent intent = new Intent(Mapa.this, ServicioLocalizacion.class);
        //assert user != null;
        intent.putExtra("USER_ID", user.getUid());
        startService(intent);


        binding.button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), UsuariosDisponibles.class);
                startActivity(intent);
            }
        });




        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                Log.i("LOCATION", "Location update in the callback: " + location);
                if (location != null) {

                    final LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    Date date = new Date();
                    String timestamp = date.toString();
                    currentLatitude = location.getLatitude();
                    currentLongitude = location.getLongitude();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (ActivityCompat.checkSelfPermission(Mapa.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(Mapa.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                return;
                            }
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));
                            mMap.setMyLocationEnabled(true);
                            mMap.addMarker(new MarkerOptions().position(latLng).title("Mi ubicación"));
                        }
                    });
                }
            }
        };
        //Manejo sensor de luminosidad
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        lightSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (mMap != null) {
                    if (event.values[0] < 5000) {
                        Log.i("MAPS", "DARK MAP " + event.values[0]);
                        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(Mapa.this, R.raw.dark_style_map));
                    } else {
                        Log.i("MAPS", "LIGHT MAP " + event.values[0]);
                        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(Mapa.this, R.raw.light_style_map));
                    }
                }
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

    }
    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(lightSensorListener, lightSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }
    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(lightSensorListener);
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        verifyPermission();

        locationReader reader = new locationReader(this);
        List<Ubicaciones> locations = null;
        try {
            locations = reader.readLocations();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
// Itera sobre la lista de locations para hacer lo que necesites
        int i = 0;
        for (Ubicaciones location : locations) {
            names[i] = location.getName();
            latitudes[i] = location.getLatitude();
            longitudes[i] = location.getLongitude();
            final LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.addMarker(new MarkerOptions().position(latLng).title(location.getName()));
            i++;
        }
    }



    public void initView(Boolean result) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(Mapa.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(Mapa.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                if (location != null) {
                                    final LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                    currentLatitude = location.getLatitude();
                                    currentLongitude = location.getLongitude();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (ActivityCompat.checkSelfPermission(Mapa.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(Mapa.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                                return;
                                            }
                                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));
                                            mMap.setMyLocationEnabled(true);
                                            mMap.addMarker(new MarkerOptions().position(latLng).title("Mi ubicación"));
                                        }
                                    });
                                }
                            }
                        });
            }
        }).start();


    }

    public void verifyPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            //If i don´t have the permission
            //Si ya lo habia pedido pero lo nego
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
                //Justification
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Nuestra aplicación requiere acceso a la ubicación de su dispositivo para poder proporcionarle información precisa y en tiempo real" )
                        .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                            }
                        });
                builder.create().show();
            }else {
                //Pide el permiso pero sin justificación
                locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }

        }else {

            //Si esta aceptadp
            initView(true);

        }
    }


    private LocationRequest createLocationRequest(){
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(10000)
                .setFastestInterval(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setSmallestDisplacement(30);
        return locationRequest;
    }

    private void checkLocationSettings() {
        LocationSettingsRequest.Builder builder = new
                LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                //Log.i(IndexActivity.TAG, "GPS is ON");
                settingsOK = true;
                startLocationUpdates();
            }
        });
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (((ApiException) e).getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED) {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    IntentSenderRequest isr = new IntentSenderRequest.Builder(resolvable.getResolution()).build();
                    getLocationSettings.launch(isr);
                } else {
                    Log.e("GPS","No GPS available");

                }
            }
        });
    }
    private void startLocationUpdates(){

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED){
            if(settingsOK) {
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
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
                            Toast.makeText(Mapa.this, "ya estás disponible", Toast.LENGTH_SHORT).show();
                        }else{
                            myUser.setDisponible(1);
                            myRef.setValue(myUser); // actualiza el valor en la base de datos
                            // Muestra un mensaje de éxito
                            Toast.makeText(Mapa.this, "Ahora estás disponible", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(Mapa.this, "ya no estás disponible", Toast.LENGTH_SHORT).show();
                        }else{
                            myUser.setDisponible(0);
                            myRef.setValue(myUser); // actualiza el valor en la base de datos
                            // Muestra un mensaje de éxito
                            Toast.makeText(Mapa.this, "Ahora no estás disponible", Toast.LENGTH_SHORT).show();
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



}
