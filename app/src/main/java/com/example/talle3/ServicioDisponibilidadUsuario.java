package com.example.talle3;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import android.Manifest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ServicioDisponibilidadUsuario extends Service {
    private static final String CHANNEL_ID = "MyApp";
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private NotificationManagerCompat notificationManager;
    private int notificationId = 0;
    @Override
    public void onCreate() {
        super.onCreate();
        mAuth = FirebaseAuth.getInstance();
        createNotificationChannel();
        notificationManager = NotificationManagerCompat.from(this);
        setupDatabaseListener();
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    } private void setupDatabaseListener() {
        databaseReference = FirebaseDatabase.getInstance().getReference().child("users");
        databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                User user = dataSnapshot.getValue(User.class);
                if (user != null && user.getDisponible() == 1) {
                    user.setUserID(dataSnapshot.getKey());
                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    if(user.getUserID()!=currentUser.getUid()){
                        sendNotification(user);
                    }
                }
            }
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, @Nullable String previousChildName) {}
            @Override
            public void onChildRemoved( DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved( DataSnapshot dataSnapshot, @Nullable String previousChildName) {}
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("TAG", "setupDatabaseListener:onCancelled", error.toException());
            }
        });
    }
    private void sendNotification(User user) {
        try {
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
            mBuilder.setSmallIcon(R.drawable.ic_notification);
            mBuilder.setContentTitle("Usuario disponible");
            mBuilder.setContentText(user.getName() + " está disponible");
            mBuilder.setAutoCancel(true);
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if(currentUser!=null){
                Intent intent = new Intent(this, realTimeUbi.class);
                intent.putExtra("USER_ID", user.getUserID());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
                mBuilder.setContentIntent(pendingIntent);
            } else {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
                mBuilder.setContentIntent(pendingIntent);
            }
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            notificationManager.notify(notificationId++, mBuilder.build());
        } catch (Exception e) {
            Log.e("sos", "Error al enviar la notificación: " + e.getMessage());
        }

    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "My Channel";
            String description = "Channel Description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}

