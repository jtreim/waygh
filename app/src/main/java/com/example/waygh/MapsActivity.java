package com.example.waygh;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
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
    private TextView mElapsedTimeText;
    private TextView mLatitudeText;
    private TextView mLongitudeText;

    private LocationCallback mLocationCallback;
    private boolean mRequestingLocationUpdates;
    private FusedLocationProviderClient mLocationClient;
    private ArrayList<Pair<Date, Location>> mRoutePoints;
    private Polyline mRoute;
    private Circle mCurrentPosCircle;
    private Location mStartPos;
    private Date mStartTime;

    public static final int MY_PERMISSIONS_REQUEST_ACCESS_LOCATION = 1;
    private static final LatLng PROVO = new LatLng(40.2338, -111.6585);
    private static final double POS_CIRCLE_SIZE = 5;

    protected LocationRequest createLocationRequest(){
        LocationRequest request = LocationRequest.create();
        request.setInterval(1000);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return request;
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(mRequestingLocationUpdates){
            if(ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String [] { Manifest.permission.ACCESS_FINE_LOCATION },
                        MY_PERMISSIONS_REQUEST_ACCESS_LOCATION);
            }
        }
    }

    private void startLocationUpdates() {
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationClient.requestLocationUpdates(createLocationRequest(), mLocationCallback,
                    Looper.getMainLooper());
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String [] { Manifest.permission.ACCESS_FINE_LOCATION },
                    MY_PERMISSIONS_REQUEST_ACCESS_LOCATION);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mStartPos = new Location("gps");
        mStartPos.setLatitude(PROVO.latitude);
        mStartPos.setLongitude(PROVO.longitude);
        mRequestingLocationUpdates = false;
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mElapsedTimeText = this.findViewById(R.id.timerTextView);
        mLatitudeText = this.findViewById(R.id.latitudeTextView);
        mLongitudeText = this.findViewById(R.id.longitudeTextView);
        mapFragment.getMapAsync(this);
        mRoutePoints = new ArrayList<>();
        mLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setInitialLocation();

        mLocationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult){
                if(locationResult == null){
                    return;
                }
                for(Location location : locationResult.getLocations()){
                    addLocationPoint(location);
                }
                ArrayList<LatLng> path = getPoints();
                mRoute.setPoints(path);
                mCurrentPosCircle.setCenter(path.get(path.size() - 1));
            }
        };
    }


    private void setInitialLocation(){
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        mRequestingLocationUpdates = true;
                        mStartTime = Calendar.getInstance().getTime();
                        mRoutePoints.add(new Pair<>(mStartTime, location));
                        LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
                        mLatitudeText.setText(Double.toString(pos.latitude));
                        mLongitudeText.setText(Double.toString(pos.longitude));
                        mStartPos = location;
                        mElapsedTimeText.setText("0 hr 0 min 0 sec");
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 19.0f));
                        startLocationUpdates();
                    }
                }
            });
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String [] { Manifest.permission.ACCESS_FINE_LOCATION },
                    MY_PERMISSIONS_REQUEST_ACCESS_LOCATION);
        }
        mLocationClient.getLastLocation();
    }

    public ArrayList<LatLng> getPoints(){
        ArrayList<LatLng> result = new ArrayList<>();
        for(Pair p : mRoutePoints){
            Location l = (Location)p.second;
            result.add(new LatLng(l.getLatitude(), l.getLongitude()));
        }
        return result;
    }

    public void addLocationPoint(Location location){
        if(location != null && mMap != null){
            Date currentTime = Calendar.getInstance().getTime();
            mRoutePoints.add(new Pair<>(currentTime, location));
            LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
            mLatitudeText.setText(Double.toString(pos.latitude));
            mLongitudeText.setText(Double.toString(pos.longitude));
            long diff = Calendar.getInstance().getTime().getTime() - mStartTime.getTime();
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
            mElapsedTimeText.setText(elapsedTime.toString());
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_LOCATION) {
            if (permissions.length == 1 &&
                    permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION) &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED && mRoutePoints.size() > 0) {
                startLocationUpdates();
            } else if(permissions.length == 1 &&
                    permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION) &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED){
                setInitialLocation();
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
        LatLng pos = new LatLng(mStartPos.getLatitude(), mStartPos.getLongitude());
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.add(pos);
        if(mMap != null){
            mRoute = mMap.addPolyline(polylineOptions);
            mCurrentPosCircle = mMap.addCircle(new CircleOptions()
            .center(pos)
            .radius(POS_CIRCLE_SIZE)
            .fillColor(R.attr.colorPrimary));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 19.0f));
        }
    }
}
