package com.example.talle3;

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
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.talle3.databinding.ActivityRegistroBinding;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Date;
import java.util.Random;

public class Registro extends AppCompatActivity {

    private ActivityRegistroBinding binding;
    private FirebaseAuth mAuth;
    FirebaseDatabase database;

    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageRef = storage.getReference();
    DatabaseReference myRef;

    private static final String TAG = "Regis";

    private Boolean settingsOK;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    private InputStream stream;

    Uri fUri;

    public static final String PATH_USERS="users/";



    private Uri uriCamera;

    ActivityResultLauncher<String> mGetContentGallery = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uriLocal) {
                    //Load image on a view...
                    try {
                        loadImage(uriLocal);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

    ActivityResultLauncher<Uri> mGetContentCamera =
            registerForActivityResult(new ActivityResultContracts.TakePicture(),
                    new ActivityResultCallback<Boolean>() {
                        @Override
                        public void onActivityResult(Boolean result) {
                            //Load image on a view
                            try {
                                loadImage(uriCamera);
                            } catch (FileNotFoundException e) {
                                throw new RuntimeException(e);
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistroBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        mLocationRequest = createLocationRequest();
        checkLocationSettings();
        initFile();
        database = FirebaseDatabase.getInstance();


        mAuth = FirebaseAuth.getInstance();
        binding.imageBton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGetContentGallery.launch("image/*");
            }
        });


        binding.camBton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGetContentCamera.launch(uriCamera);
            }
        });
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                Log.i("LOCATION", "Location update in the callback: " + location);
                if (location != null) {
                    binding.latitud.setText(String.valueOf(location.getLatitude()));
                    binding.longitd.setText(String.valueOf(location.getLongitude()));
                }
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        verifyPermission();
        binding.reg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(validateForm()){
                    if(binding.tyc.isChecked())
                    {
                        regis();
                    }
                    else {
                        Toast.makeText(Registro.this, "Acepte Terminos y condiciones", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void regis(){
        mAuth.createUserWithEmailAndPassword(binding.emailReg.getText().toString().trim(), binding.contraReg.getText().toString().trim()).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                FirebaseUser user = mAuth.getCurrentUser();
                if(task.isSuccessful()){
                    Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());
                    if(user!=null){ //Update user Info
                        UserProfileChangeRequest.Builder upcrb = new UserProfileChangeRequest.Builder();
                        upcrb.setDisplayName(binding.nombreReg.getText().toString()+" "+binding.apellidoReg.getText().toString());
                        upcrb.setPhotoUri(Uri.parse("path/to/pic"));//fake uri, use Firebase Storage
                        user.updateProfile(upcrb.build());

                        // Create a User object with the user's data
                        User myUser = new User();
                        myUser.setName(binding.nombreReg.getText().toString() +" "+binding.apellidoReg.getText().toString());
                        myUser.setEmail(binding.emailReg.getText().toString());
                        myUser.setLongitud(binding.longitd.getText().toString() );
                        myUser.setLatitude(binding.latitud.getText().toString());
                        myUser.setDisponible(0);

                        // Upload the profile image to Firebase Storage
                        storageRef  = storage.getReference("Image1" + new Random().nextInt(50));
                        storageRef.putFile(fUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                // Get the URL of the uploaded image and add it to the User object
                                storageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        myUser.setProfileImageURL(uri.toString());
                                        // Save the User object in the database
                                        myRef=database.getReference(PATH_USERS+user.getUid());
                                        myRef.setValue(myUser);
                                        updateUI(user);
                                    }
                                });
                            }
                        });
                    }
                }
                if (!task.isSuccessful()) {
                    Toast.makeText(Registro.this, "error registrando usuario", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, task.getException().getMessage());
                }
            }
        });
    }


    private void updateUI(FirebaseUser currentUser){

        if(currentUser!=null){
            Log.e("Desactivado", "textobox es nulo");
            Intent intent = new Intent(getBaseContext(), Mapa.class);
            intent.putExtra("user", currentUser.getDisplayName());
            startActivity(intent);
        } else {
            binding.emailReg.setText("");
            binding.contraReg.setText("");
        }
    }
    private boolean validateForm() {
        boolean valid = true;
        String email =binding.emailReg.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            binding.emailReg.setError("Required.");
            valid = false;
        } else if (!email.contains("@") || !email.contains(".") || email.length() < 5) {
            valid = false;
        } else {
            binding.emailReg.setError(null);
        }
        String password = binding.contraReg.getText().toString().trim();
        if (TextUtils.isEmpty(password)) {
            binding.contraReg.setError("Required.");
            valid = false;
        } else {
            binding.contraReg.setError(null);
        }
        return valid;
    }


    //Imagenes
    public void loadImage(Uri uriLocal) throws FileNotFoundException {
        fUri = uriLocal;
        final InputStream imageStream = getContentResolver().openInputStream(uriLocal);
        stream = getContentResolver().openInputStream(uriLocal);
        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
        binding.imageGalelry.setImageBitmap(selectedImage);
    }

    public void initFile(){
        File file = new File(getFilesDir(), "picFromCamera");
        uriCamera = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", file);
    }

    //Permiso de ubicación

    public void verifyPermission(){
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            //If i don´t have the permission
            //Si ya lo habia pedido pero lo nego
            if (shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)){
                //Justification
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Nuestra aplicación requiere acceso a la ubicación de su dispositivo para poder proporcionarle información precisa y en tiempo real" )
                        .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                locationPermission.launch(android.Manifest.permission.ACCESS_FINE_LOCATION);
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


    public void initView(Boolean result) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(Registro.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(Registro.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                if (location != null) {
                                    final LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                    binding.latitud.setText(String.valueOf(location.getLatitude()));
                                    binding.longitd.setText(String.valueOf(location.getLongitude()));
                                }
                            }
                        });
            }
        }).start();
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

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    private void startLocationUpdates(){

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED){
            if(settingsOK) {
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
            }
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
}