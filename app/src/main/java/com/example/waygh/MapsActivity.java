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

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient locationClient;
    private ArrayList<Pair<LocalTime, Location>> routePoints;
    private Polyline route;
    private Marker currentPos;
    private Marker startPos;
    private LocalTime startTime;
    private int index;

    private Handler gpsPoller;

    public static final int MY_PERMISSIONS_REQUEST_ACCESS_LOCATION = 1;
    private static final LatLng PROVO = new LatLng(40.2338, -111.6585);
    private static final LatLng OREM = new LatLng(41.296898, -112.694649);
    private static final LatLng PG = new LatLng(42.340845, -113.717548);
    private static final LatLng RANDOM = new LatLng(40.400000, -111.720000);
    private ArrayList<LatLng> points;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        routePoints = new ArrayList<>();
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        points = new ArrayList<>();
        points.add(PROVO);
        points.add(OREM);
        points.add(PG);
        points.add(RANDOM);
        index = 1;
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
                        LocalTime currentTime = LocalTime.now();
//                        routePoints.add(new Pair<>(currentTime, location));
                        Location next = new Location("");
                        next.setLatitude(points.get(index).latitude);
                        next.setLongitude(points.get(index).longitude);
                        routePoints.add(new Pair<>(currentTime, next));
                        index = (index + 1) % points.size();
                        LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
                        if(currentPos != null){
                            currentPos.setPosition(pos);
                            LocalTime diff = LocalTime.now().minusNanos(startTime.toNanoOfDay());
                            currentPos.setTitle("Elapsed time: " + diff.format(
                                    DateTimeFormatter.ofPattern("H:m")));
                            route.setPoints(getPoints());
//                            Toast.makeText(getParent(),
//                                    "Last position added: " + routePoints.get(routePoints.size() - 1),
//                                    Toast.LENGTH_SHORT).show();
                        } else if (routePoints.size() > 0){
                            startTime = LocalTime.now();
                            Location last = routePoints.get(routePoints.size() - 1).second;
                            LatLng lastPos = new LatLng(last.getLatitude(), last.getLongitude());
                            MarkerOptions opt = new MarkerOptions();
//                          startPos = mMap.addMarker(opt.position(lastPos).title(
//                                  "Started: " + startTime.format(
//                                          DateTimeFormatter.ofPattern("H:m"))));
//                          currentPos = mMap.addMarker(opt.position(lastPos).title(
//                                  "Elapsed time: 00:00"));
                            startPos = mMap.addMarker(opt.position(PROVO).title(
                                    "Started: " + startTime.format(
                                            DateTimeFormatter.ofPattern("H:m"))));
                            currentPos = mMap.addMarker(opt
                                    .position(lastPos)
                                    .title("Elapsed time: 00:00"));
                            PolylineOptions polylineOptions = new PolylineOptions();
                            polylineOptions.add(startPos.getPosition());
                            route = mMap.addPolyline(polylineOptions);
                        } else {
                            currentPos = mMap.addMarker(new MarkerOptions().position(PROVO).title(
                                    currentTime.format(DateTimeFormatter.ISO_TIME)));
                        }
//                      mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 19.0f));
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
