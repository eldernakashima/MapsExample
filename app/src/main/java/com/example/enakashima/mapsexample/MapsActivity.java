package com.example.enakashima.mapsexample;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.turbomanage.httpclient.AsyncCallback;
import com.turbomanage.httpclient.BasicHttpClient;
import com.turbomanage.httpclient.HttpResponse;
import com.turbomanage.httpclient.ParameterMap;
import com.turbomanage.httpclient.android.AndroidHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private Button find;
    private FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        remoteConfig.setConfigSettings(new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(true)
                .build());

        HashMap<String,Object> defaults = new HashMap<>();
        defaults.put("color","#00008B");
        remoteConfig.setDefaults(defaults);

        remoteConfig.fetch(0);
        find = findViewById(R.id.find);
        final Task<Void> fetch = remoteConfig.fetch(0);
        fetch.addOnSuccessListener(this, new OnSuccessListener<Void>() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onSuccess(Void aVoid) {
                remoteConfig.activateFetched();
                find.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(remoteConfig.getString("color"))));
                System.out.println("Color ---- " + remoteConfig.getString("color"));
            }
        });

        find.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GetRequest getRequest = new GetRequest();
                getRequest.execute("http://bcadb276.ngrok.io/");
            }
        });

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
        Marker myPos = mMap.addMarker(new MarkerOptions().position(new LatLng(-23.557096, -46.730211)).title("me"));

        // Set a listener for marker click.
        mMap.setOnMarkerClickListener(this);
        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
            if (marker.getTitle().contains("SP")){
                FirebaseCrash.log("Não vá a este local!");
                FirebaseCrash.report(new Exception("Pessimo kart"));
        }
        return false;
    }

    private class GetRequest extends AsyncTask<String, Long, String> {
        protected String doInBackground(String... urls) {
            try {
                HttpRequest request =  HttpRequest.get(urls[0]);
                System.out.println("URL: " + urls[0]);
                String response = "0";
                if (request.ok()) {
                    response = HttpRequest.get(urls[0]).body();
                }
                return response;
            } catch (HttpRequest.HttpRequestException exception) {
                return null;
            }
        }

        protected void onProgressUpdate(Long... progress) {
        }

        protected void onPostExecute(String response) {
            JSONArray karts = null;
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            try {
                karts = new JSONArray(response);
                for (int i = 0; i < karts.length(); i++) {
                    JSONObject item = karts.getJSONObject(i);
                    LatLng latLng = new LatLng(item.getDouble("lat"),item.getDouble("long"));
                    mMap.addMarker(new MarkerOptions().position(latLng).title(item.getString("name")));
                    builder.include(latLng);
                }
                LatLngBounds bounds = builder.build();
                int padding = 50; // offset from edges of the map in pixels
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                mMap.animateCamera(cu);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
