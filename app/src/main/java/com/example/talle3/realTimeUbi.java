package com.example.talle3;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

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
import com.example.talle3.databinding.ActivityUbicacionUsuarioBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class realTimeUbi extends FragmentActivity implements OnMapReadyCallback {

    private static final double RADIUS_OF_EARTH_KM = 6371;
    private GoogleMap mMap;
    private ActivityUbicacionUsuarioBinding binding;
    private FusedLocationProviderClient mFusedLocationClient;

    private FirebaseAuth mAuth;



    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private Boolean settingsOK;
    String userId;
    SensorManager sensorManager;
    Sensor lightSensor;
    SensorEventListener lightSensorListener;
    //Posisción
    private double currentLatitude;
    private double currentLongitude;
    private double targetLatitude;
    private double targetLongitude;
    DatabaseReference ref;

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

        binding = ActivityUbicacionUsuarioBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Otener el USER_ID del usuario seleccionado
        userId = getIntent().getStringExtra("USER_ID");
        Log.d("UserID", "ID: " + userId  );
        mAuth = FirebaseAuth.getInstance();


        if (userId != null) {
            ref = FirebaseDatabase.getInstance().getReference().child("users").child(userId);
            // rest of the code that uses the ref object
        } else {
            Log.e("realTimeUbi", "USER_ID extra not provided in intent");
        }


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mLocationRequest = createLocationRequest();
        checkLocationSettings();
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
                            if (ActivityCompat.checkSelfPermission(realTimeUbi.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(realTimeUbi.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                return;
                            }
                            mMap.setMyLocationEnabled(true);
                            mMap.addMarker(new MarkerOptions().position(latLng).title("Mi nueva ubicación"));
                        }
                    });
                }
            }
        };

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        lightSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (mMap != null) {
                    if (event.values[0] < 5000) {
                        Log.i("MAPS", "DARK MAP " + event.values[0]);
                        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(realTimeUbi.this, R.raw.dark_style_map));
                    } else {
                        Log.i("MAPS", "LIGHT MAP " + event.values[0]);
                        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(realTimeUbi.this, R.raw.light_style_map));
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Obtener la ubicación del usuario desde el snapshot de Firebase
                    String latitudStr = dataSnapshot.child("latitude").getValue(String.class);
                    String longitudStr = dataSnapshot.child("longitud").getValue(String.class);
                    String imageUrl = dataSnapshot.child("profileImageUrl").getValue(String.class);

                    // Convertir a double si es posible
                    double latitud, longitud;
                    try {
                        latitud = Double.parseDouble(latitudStr);
                        longitud = Double.parseDouble(longitudStr);
                    } catch (NumberFormatException e) {
                        // Manejar el error si la conversión falla
                        Log.e("Firebase", "Error al convertir latitud o longitud a double: " + e.getMessage());
                        return;
                    }
                    mMap.clear();
                    // Actualizar la ubicación del usuario en el mapa
                    LatLng ubicacion = new LatLng(latitud, longitud);
                    mMap.addMarker(new MarkerOptions().position(ubicacion).title("Ubicación de " + dataSnapshot.child("name").getValue(String.class)));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(ubicacion));
                    double finalDistance = distance(currentLatitude, currentLongitude,Double.parseDouble(latitudStr), Double.parseDouble(longitudStr));
                    Toast toast = Toast.makeText(realTimeUbi.this, "La distancie desde su posición actual es: " + String.valueOf(finalDistance) + " km", Toast.LENGTH_LONG);
                    toast.setDuration(Toast.LENGTH_LONG);
                    toast.show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Manejar errores de Firebase
            }
        });

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
        if (mMap != null) {
            mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(LatLng latLng) {
                    mMap.clear();
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.title(geoCoderSearchLatLang(latLng));
                    targetLatitude = latLng.latitude;
                    targetLongitude = latLng.longitude;
                    mMap.addMarker(markerOptions); //Agregar marcador al mapa
                    double finalDistance = distance(currentLatitude, currentLongitude, targetLatitude, targetLongitude);
                    Toast toast = Toast.makeText(realTimeUbi.this, "La distancie desde su posición actual es: " + String.valueOf(finalDistance) + " km", Toast.LENGTH_LONG);
                    toast.setDuration(Toast.LENGTH_LONG);
                    toast.show();
                }
            });
        }
    }
    private String geoCoderSearchLatLang(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                return address.getAddressLine(0); // Devuelve la dirección completa
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ""; // Devuelve una cadena vacía si no se pudo obtener la dirección
    }

    public void initView(Boolean result) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(realTimeUbi.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(realTimeUbi.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                                            if (ActivityCompat.checkSelfPermission(realTimeUbi.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(realTimeUbi.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
    public double distance(double lat1, double long1, double lat2, double long2) {
        double latDistance = Math.toRadians(lat1 - lat2);
        double lngDistance = Math.toRadians(long1 - long2);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double result = RADIUS_OF_EARTH_KM * c;
        return Math.round(result*100.0)/100.0;
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
                Toast.makeText(this, "Estás disponible", Toast.LENGTH_SHORT).show();
                // Aquí puedes agregar el código para establecer al usuario como disponible
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}