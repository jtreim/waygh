package com.example.waygh;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.lang.reflect.Array;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView elapsedTimeText;
    private TextView latitudeText;
    private TextView longitudeText;

    private FusedLocationProviderClient locationClient;
    private ArrayList<Pair<Date, Location>> routePoints;
    private Polyline route;
    private Location startPos;
    private Date startTime;

    private Handler gpsPoller;

    public static final int MY_PERMISSIONS_REQUEST_ACCESS_LOCATION = 1;
    private static final LatLng PROVO = new LatLng(40.2338, -111.6585);
//    private static final LatLng OREM = new LatLng(41.296898, -112.694649);
//    private static final LatLng PG = new LatLng(42.340845, -113.717548);
//    private static final LatLng RANDOM = new LatLng(40.400000, -111.720000);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        elapsedTimeText = this.findViewById(R.id.timerTextView);
        latitudeText = this.findViewById(R.id.latitudeTextView);
        longitudeText = this.findViewById(R.id.longitudeTextView);
        mapFragment.getMapAsync(this);
        routePoints = new ArrayList<>();
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        addLocationPoint();

        gpsPoller = new Handler();
        final int delay = 1000;
        gpsPoller.postDelayed(new Runnable() {
            @Override
            public void run() {
                addLocationPoint();
                gpsPoller.postDelayed(this, delay);
            }
        }, delay);
    }


    public ArrayList<LatLng> getPoints(){
        ArrayList<LatLng> result = new ArrayList<>();
        for(Pair p : routePoints){
            Location l = (Location)p.second;
            result.add(new LatLng(l.getLatitude(), l.getLongitude()));
        }
        return result;
    }

    public void addLocationPoint(){
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            locationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if(location != null && mMap != null){
                        Date currentTime = Calendar.getInstance().getTime();
                        routePoints.add(new Pair<>(currentTime, location));
                        LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
                        longitudeText.setText(Double.toString(pos.longitude));
                        latitudeText.setText(Double.toString(pos.latitude));
                        if(routePoints.size() > 1){
                            route.setPoints(getPoints());
                        } else if (routePoints.size() == 1){
                            startTime = Calendar.getInstance().getTime();
                            startPos = location;
                            PolylineOptions polylineOptions = new PolylineOptions();
                            polylineOptions.add(pos);
                            route = mMap.addPolyline(polylineOptions);
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 19.0f));
                        }
                        long diff = Calendar.getInstance().getTime().getTime() - startTime.getTime();
                        long diffSec = diff / 1000 % 60;
                        long diffMin = diff / (60 * 1000) % 60;
                        long diffHour = diff / (60 * 60 * 1000) % 24;
                        StringBuilder elapsedTime = new StringBuilder();
                        elapsedTime.append(diffHour);
                        elapsedTime.append(" hr ");
                        elapsedTime.append(diffMin);
                        elapsedTime.append(" min ");
                        elapsedTime.append(diffSec);
                        elapsedTime.append(" sec");
                        elapsedTimeText.setText(elapsedTime.toString());
                    }
                }
            });
            locationClient.getLastLocation();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String [] { Manifest.permission.ACCESS_FINE_LOCATION },
                    MY_PERMISSIONS_REQUEST_ACCESS_LOCATION);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_LOCATION) {
            if (permissions.length == 1 &&
                    permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION) &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addLocationPoint();
            } else {
                // Permission was denied. Display an error message.
            }

        }
    }

        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera. In this case,
         * we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to install
         * it inside the SupportMapFragment. This method will only be triggered once the user has
         * installed Google Play services and returned to the app.
         */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(PROVO, 12.0f));
    }
}
