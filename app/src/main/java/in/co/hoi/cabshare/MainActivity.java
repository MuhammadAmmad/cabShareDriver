package in.co.hoi.cabshare;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Explode;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.ui.IconGenerator;

import org.apache.commons.codec.DecoderException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

public class MainActivity extends ActionBarActivity implements android.location.LocationListener, GoogleMap.OnMapClickListener{

    //Todo remove next location icon
    //Todo goto present location after startride


    /*
     * Constants
     */
    private static String authenticationHeader;
    private final static String BASE_SERVER_URL = "http://www.hoi.co.in/dapi/";
    public static final int NOTIFICATION_ID = 1;

    ProgressDialog mProgress;
    Boolean setup = false;
    Handler mHandler;
    LatLng nextDestination;
    String resp = "";
    boolean nextDestinationChanged = false;
    boolean nextPassengerChanged = false;
    boolean nextPathSet = false;
    private boolean checkUpdateRide = false;

    /*
     * Markers for Google Map
     */
    private GoogleMap mGoogleMap;
    private CharSequence mTitle;

    /*
     * Miscellaneous variables
     */
    ObscuredSharedPreferences prefs;
    private LatLng currentLocationCoordinate;
    Fragment displayFragment;

    /*
     * Ride variables
     */
    double distanceTravelled;
    double distanceCharge;
    long upTime;
    double estimatedFare;
    int counter;
    double[] incomeRate = {0, 10, 11.5, 13, 14.5};
    int distanceFare = 0;
    int timeFare = 0;
    int totalFare = 0;
    List<PendingPayItem> pendingPayList;

    /*
     * Variables for passengers Ongoing and Waiting
     */
    private List<Passenger> passengers = new ArrayList<Passenger>();
    private NextPassenger nextPassenger = null;
    private boolean arrived = false;
    private TextView mNextDestination;
    private LinearLayout mNextDestinationView;

    /*
     * Activity related functions
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        estimatedFare = 0.0;
        upTime = System.currentTimeMillis();
        distanceTravelled = 0.0;
        counter = 0;

        prefs = ObscuredSharedPreferences.getPrefs(this, "HoiCabDrivers", Context.MODE_PRIVATE);
        mNextDestination = (TextView) findViewById(R.id.destination);
        mNextDestinationView = (LinearLayout) findViewById(R.id.destinationView);
        mNextDestinationView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoDestLocation();
            }
        });

        setupMapFragment();

        if (getIntent().hasExtra("authenticationHeader"))
            authenticationHeader = getIntent().getStringExtra("authenticationHeader");


        String data = getIntent().getStringExtra("userData");
        JSONObject response = null;
        try {
            response = new JSONObject(data);
            parseResponse(response);
            if(passengers.size() > 0){
                String destinationAddress = response.getString("next_address1") + response.getString("next_address2");
                nextDestination = new LatLng(response.getDouble("next_lat"), response.getDouble("next_long"));
                nextPassenger = setNextPassenger(response.getInt("nextriderid"), response.getString("nextriderphone"),
                        nextDestination, destinationAddress);
                arrived = response.getBoolean("next_arrived");
                setUpHome(destinationAddress);
                if (currentLocationCoordinate != null)
                    setDestination();
            }
            else{
                nextPassenger = null;
                nextDestination = null;
                arrived = false;
                setUpHome("No Destination");
            }
        } catch (JSONException e) {
            Log.e("Exception", e.getMessage());
            Toast.makeText(getApplicationContext(), "Some Problem Occured", Toast.LENGTH_SHORT).show();
            recreate();
        }
    }

    /*
     * Activity UI Handling Functions
     */

    private void setupMapFragment() {
        //Setting up map fragment
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mGoogleMap = fragment.getMap();
        mGoogleMap.setOnMapClickListener(this);
        mGoogleMap.setMyLocationEnabled(true);

        // Creating a criteria object to retrieve provider
        Criteria criteria = new Criteria();

        // Getting the name of the best provider
        String provider = locationManager.getBestProvider(criteria, true);

        // Getting Current Location
        Location location = getLastKnownLocation(locationManager);

        if (location != null) {
            currentLocationCoordinate = new LatLng(location.getLatitude(), location.getLongitude());
            onLocationChanged(location);
        }
        locationManager.requestLocationUpdates(provider, 20000, 0, this);
    }

    public void setDestination() {

        if(mGoogleMap != null) mGoogleMap.clear();
        if(nextDestination == null) return;
        //Todo update map fragment
        if(currentLocationCoordinate != null && (nextDestination.latitude != currentLocationCoordinate.latitude || nextDestination.longitude != currentLocationCoordinate.longitude)){
            try{
                findDirections(currentLocationCoordinate.latitude, currentLocationCoordinate.longitude,
                        nextDestination.latitude, nextDestination.longitude, GMapV2Direction.MODE_DRIVING);
                nextPathSet = true;
            }catch(Exception e){
                Log.e("Exception", e.getMessage());
                nextPathSet = false;
            }
        }
        IconGenerator iconFactory = new IconGenerator(this);
        iconFactory.setColor(Color.GREEN);
        addIcon(iconFactory, "Next", nextDestination);
    }

    public void setUpHome(String address) {
        displayFragment = new FragmentMain();
        mNextDestination.setText(address);
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().replace(R.id.content_frame, displayFragment).commit();
        setTitle("Home");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    private Marker addIcon(IconGenerator iconFactory, String text, LatLng position) {
        MarkerOptions markerOptions = new MarkerOptions().
                icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(text))).
                position(position).
                anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV()).draggable(true);

        return mGoogleMap.addMarker(markerOptions);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_search:
                return true;
            case R.id.action_pending_pay:
                getPendingPay();
                return true;
            case R.id.action_money_so_far:
                getMoneyMade();
                return true;
            case R.id.start_accepting_ride:
                requestStartAcceptingRide();
                return true;
            case R.id.stop_accepting_ride:
                requestStopAcceptingRide();
                return true;
            case R.id.logout:
                try {
                    applicationLogout();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Logout Failed", Toast.LENGTH_SHORT).show();
                    Log.e("Exception", e.getMessage());
                    recreate();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /*
     * Driver Action Function
     */

    private void getPendingPay() {
        //Todo create dialog to show pending pay

        if(!isConnceted()) {
            Toast.makeText(getApplicationContext(), "No Internet Connectivity", Toast.LENGTH_SHORT).show();
            return;
        }
        setup = false;
        mProgress = ProgressDialog.show(this, "",
                "Please Wait..", true);
        mProgress.setCancelable(false);



        new Thread(new Runnable() {
            @Override
            public void run()
            {
                //Code to execute in other thread
                try{
                    String res = new HttpRequestTask().execute("http://www.hoi.co.in/dapi/pendingpay").get(120000, TimeUnit.MILLISECONDS);
                    JSONArray jsonArray = new JSONArray(res);
                    pendingPayList = new ArrayList<PendingPayItem>();
                    for(int i = 0; i < jsonArray.length() ; i++){
                        PendingPayItem tmp = new PendingPayItem((JSONObject) jsonArray.get(i));
                        pendingPayList.add(tmp);
                    }
                    setup = true;
                }catch(Exception e){
                    Log.e("Exception", e.getMessage());
                }


                //Code to execute in main thread
                mHandler = new Handler(getApplicationContext().getMainLooper());
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        mProgress.dismiss();
                        if (setup) {
                            Dialog dialog = new Dialog(MainActivity.this);
                            dialog.setContentView(R.layout.list_passenger);
                            ListView pendingPayListView = (ListView) dialog.findViewById(R.id.lv);
                            PendingPayListAdapter pendingPayListAdapter = new PendingPayListAdapter(MainActivity.this, pendingPayList, getResources());
                            pendingPayListView.setAdapter(pendingPayListAdapter);
                            dialog.setCancelable(true);
                            dialog.setTitle("Pending Pay");
                            dialog.show();
                            setup = false;
                        }else {
                            Toast.makeText(getApplicationContext(),"pending pay failed",Toast.LENGTH_SHORT ).show();
                        }
                    }
                };
                mHandler.postDelayed(myRunnable, 500);
                Log.d("CODE", "Pending Pay");
            }
        }).start();
    }

    private void getMoneyMade(){
        //Todo show money made so far
        if(!isConnceted()) {
            Toast.makeText(getApplicationContext(), "No Internet Connectivity", Toast.LENGTH_SHORT).show();
            return;
        }
        setup = false;
        mProgress = ProgressDialog.show(this, "",
                "Please Wait..", true);
        mProgress.setCancelable(false);

        new Thread(new Runnable() {
            @Override
            public void run()
            {
                //Code to execute in other thread
                try{
                    String res = new HttpRequestTask().execute("http://www.hoi.co.in/dapi/moneysofar/").get(120000, TimeUnit.MILLISECONDS);
                    JSONObject response = new JSONObject(res);
                    distanceFare = response.getInt("totaldistfare");
                    timeFare = response.getInt("totaltimefare");
                    totalFare = response.getInt("totalfare");
                    setup = true;
                }catch(Exception e){
                    Log.e("Exception", e.getMessage());
                }


                //Code to execute in main thread
                mHandler = new Handler(getApplicationContext().getMainLooper());
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        mProgress.dismiss();
                        if (setup) {
                            createMoneyMadeDialog();
                            setup = false;
                        } else {
                            Toast.makeText(getApplicationContext(), "Money Made Details failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                };
                mHandler.postDelayed(myRunnable, 500);
                Log.d("CODE", "MoneyMade");
            }
        }).start();
    }

    public void gotoDestLocation(){
        if(nextDestination != null){
            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(nextDestination, 15);
            mGoogleMap.animateCamera(yourLocation);
        }
        else{
            Toast.makeText(getApplicationContext(), "No Destination Found", Toast.LENGTH_LONG).show();
        }
    }

    public void requestCancelRide(View view){
        if(!isConnceted()) {
            Toast.makeText(getApplicationContext(), "No Internet Connectivity", Toast.LENGTH_SHORT).show();
            return;
        }
        setup = false;
        if(arrived) {

            mProgress = ProgressDialog.show(this, "",
                    "Please Wait..", true);
            mProgress.setCancelable(false);

            new Thread(new Runnable() {
                @Override
                public void run()
                {
                    //Code to execute in other thread
                    try{
                        String res = new HttpRequestTask(getPostRequestData()).execute("http://www.hoi.co.in/dapi/cancelride/" + nextPassenger.getId()).get(120000, TimeUnit.MILLISECONDS);
                        JSONObject response = new JSONObject(res);

                        if(response.getBoolean("requestaccepted")){
                            JSONObject rideInfo = response.getJSONObject("rideinfo");
                            if( rideInfo.isNull("rider1id") || (rideInfo.getInt("rider1id") < 0)){
                                setup = false;
                            }else{
                                arrived = rideInfo.getBoolean("next_arrived");
                                parseResponse(rideInfo);
                                getAppState(rideInfo);
                                setup = true;

                            }
                        }else{
                            resp = response.getString("message");
                        }
                    }catch(Exception e){
                        Log.e("Exception", e.getMessage());
                    }


                    //Code to execute in main thread
                    mHandler = new Handler(getApplicationContext().getMainLooper());
                    Runnable myRunnable = new Runnable() {
                        @Override
                        public void run() {
                            mProgress.dismiss();
                            if (setup) {
                                if(nextDestinationChanged || !nextPathSet){
                                    nextPathSet = false;
                                    setDestination();
                                    nextDestinationChanged = false;
                                }
                                if(nextPassengerChanged){
                                    if(nextPassenger == null){
                                        setUpHome("No Destination");
                                        nextPassengerChanged = false;
                                    }else{
                                        setUpHome(nextPassenger.getAddress());
                                        nextPassengerChanged = false;
                                    }
                                }
                                setup = false;
                            } else if(resp!= ""){
                                Toast.makeText(getApplicationContext(),resp, Toast.LENGTH_LONG).show();
                                resp = "";
                            }else {
                                Toast.makeText(getApplicationContext(),"Error Occured, CancelRide Failed", Toast.LENGTH_LONG).show();
                            }
                        }
                    };
                    mHandler.postDelayed(myRunnable, 500);
                    Log.d("CODE", "Stop Ride");
                }
            }).start();
        }
        else{
            Toast.makeText(getApplicationContext(), "You haven't Arrived", Toast.LENGTH_LONG).show();
        }
    }

    public void requestStartRide(View view){
        if(!isConnceted()) {
            Toast.makeText(getApplicationContext(), "No Internet Connectivity", Toast.LENGTH_SHORT).show();
            return;
        }
        setup = false;
        if(arrived){

            mProgress = ProgressDialog.show(this, "",
                    "Please Wait..", true);
            mProgress.setCancelable(false);

            new Thread(new Runnable() {
                @Override
                public void run()
                {
                    //Code to execute in other thread
                    try{
                        JSONObject data = new JSONObject();
                        data.accumulate("ridelastlong", Math.round(currentLocationCoordinate.longitude*10000.0)/10000.0);
                        data.accumulate("ridelastlat", Math.round(currentLocationCoordinate.latitude * 10000.0)/10000.0);
                        String res = new HttpRequestTask(data).execute("http://www.hoi.co.in/dapi/startride/"+nextPassenger.getId()).get(120000, TimeUnit.MILLISECONDS);
                        JSONObject response = new JSONObject(res);
                        if(response.getBoolean("requestaccepted")){
                            JSONObject rideInfo = response.getJSONObject("rideinfo");
                            if( rideInfo.isNull("rider1id") || (rideInfo.getInt("rider1id") < 0)){
                                setup = false;
                            }else{
                                arrived = rideInfo.getBoolean("next_arrived");
                                parseResponse(rideInfo);
                                getAppState(rideInfo);
                                setup = true;
                            }
                        }else{
                            resp = response.getString("message");
                        }
                    }catch(Exception e){
                        Log.e("Exception", e.getMessage());
                    }


                    //Code to execute in main thread
                    mHandler = new Handler(getApplicationContext().getMainLooper());
                    Runnable myRunnable = new Runnable() {
                        @Override
                        public void run() {
                            mProgress.dismiss();
                            if (setup) {
                                if(nextDestinationChanged|| !nextPathSet){
                                    nextPathSet = false;
                                    setDestination();
                                    nextDestinationChanged = false;
                                }

                                if(nextPassengerChanged){
                                    if(nextPassenger == null){
                                        setUpHome("No Destination");
                                        nextPassengerChanged = false;
                                    }else{
                                        setUpHome(nextPassenger.getAddress());
                                        nextPassengerChanged = false;
                                    }
                                }

                                setup = false;
                            } else if(resp!= ""){
                                Toast.makeText(getApplicationContext(),resp, Toast.LENGTH_LONG).show();
                                resp = "";
                            }else {
                                Toast.makeText(getApplicationContext(),"Error Occured, StartRide Failed", Toast.LENGTH_LONG).show();
                            }
                        }
                    };
                    mHandler.postDelayed(myRunnable,500);
                    Log.d("CODE", "Stop Ride");
                }
            }).start();

        }else{
            mProgress = ProgressDialog.show(this, "",
                    "Please Wait..", true);
            mProgress.setCancelable(false);

            new Thread(new Runnable() {
                @Override
                public void run()
                {
                    //Code to execute in other thread
                    try{
                        String res = new HttpRequestTask(getPostRequestData()).execute("http://www.hoi.co.in/dapi/updatearrival/"+nextPassenger.getId()).get(120000, TimeUnit.MILLISECONDS);
                        JSONObject response = new JSONObject(res);
                        if(response.getBoolean("requestaccepted")){
                            JSONObject rideInfo = response.getJSONObject("rideinfo");
                            if( rideInfo.isNull("rider1id") || (rideInfo.getInt("rider1id") < 0)){
                                setup = false;
                            }else{
                                arrived = rideInfo.getBoolean("next_arrived");
                                parseResponse(rideInfo);
                                getAppState(rideInfo);
                                setup = true;
                            }
                        }else{
                            resp = response.getString("message");
                        }
                    }catch(Exception e){
                        Log.e("Exception", e.getMessage());
                    }


                    //Code to execute in main thread
                    mHandler = new Handler(getApplicationContext().getMainLooper());
                    Runnable myRunnable = new Runnable() {
                        @Override
                        public void run() {
                            mProgress.dismiss();
                            if (setup){
                                ((FragmentMain) displayFragment).changeStatetoArrived();
                                if(nextDestinationChanged|| !nextPathSet){
                                    nextPathSet = false;
                                    setDestination();
                                    nextDestinationChanged = false;
                                }
                                if(nextPassengerChanged){
                                    if(nextPassenger == null){
                                        setUpHome("No Destination");
                                        nextPassengerChanged = false;
                                    }else{
                                        setUpHome(nextPassenger.getAddress());
                                        nextPassengerChanged = false;
                                    }
                                }
                                setup = false;
                            } else if(resp!= ""){
                                Toast.makeText(getApplicationContext(), resp, Toast.LENGTH_LONG).show();
                                resp = "";
                            }else {
                                Toast.makeText(getApplicationContext(),"Error occured, Arrived Failed", Toast.LENGTH_LONG).show();
                            }
                        }
                    };
                    mHandler.postDelayed(myRunnable, 2000);
                    Log.d("CODE", "Arrived Ride");
                }
            }).start();
        }
    }

    public void requestStopRide(final int id){
        if(!isConnceted()) {
            Toast.makeText(getApplicationContext(), "No Internet Connectivity", Toast.LENGTH_SHORT).show();
            return;
        }
        setup = false;
        mProgress = ProgressDialog.show(this, "",
                "Please Wait..", true);
        mProgress.setCancelable(false);

        new Thread(new Runnable() {
            @Override
            public void run()
            {
                //Code to execute in other thread
                try{
                    JSONObject data = new JSONObject();
                    data.accumulate("ridelastlong", Math.round(currentLocationCoordinate.longitude*10000.0)/10000.0);
                    data.accumulate("ridelastlat", Math.round(currentLocationCoordinate.latitude * 10000.0)/10000.0);
                    String res = new HttpRequestTask(data).execute("http://www.hoi.co.in/dapi/stopride/"+id).get(120000, TimeUnit.MILLISECONDS);
                    JSONObject response = new JSONObject(res);

                    if(response.getBoolean("requestaccepted")){
                        JSONObject rideInfo = response.getJSONObject("rideinfo");
                        if( rideInfo.isNull("rider1id") || (rideInfo.getInt("rider1id") < 0)){
                            setup = false;
                        }else{
                            parseResponse(rideInfo);
                            getAppState(rideInfo);
                            setup = true;
                        }
                    }else{
                        resp = response.getString("message");
                    }
                }catch(Exception e){
                    Log.e("Exception", e.getMessage());
                }


                //Code to execute in main thread
                mHandler = new Handler(getApplicationContext().getMainLooper());
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        mProgress.dismiss();
                        if (setup) {
                            if(nextDestinationChanged || !nextPathSet){
                                nextPathSet = false;
                                setDestination();
                                nextDestinationChanged = false;
                            }
                            if(nextPassengerChanged){
                                if(nextPassenger == null){
                                    setUpHome("No Destination");
                                    nextPassengerChanged = false;
                                }else{
                                    setUpHome(nextPassenger.getAddress());
                                    nextPassengerChanged = false;
                                }
                            }
                            setup = false;
                        } else if(resp!= ""){
                            Toast.makeText(getApplicationContext(), resp, Toast.LENGTH_LONG).show();
                            resp = "";
                        }else {
                            Toast.makeText(getApplicationContext(), "Error Occured, StopRide Failed", Toast.LENGTH_LONG).show();
                        }
                    }
                };
                mHandler.postDelayed(myRunnable, 2000);
                Log.d("CODE", "Stop Ride");
            }
        }).start();
    }

    public void makecallPassenger(View view){callNumber(nextPassenger.getPhone());}

    public void requestStartAcceptingRide() {
        if(!isConnceted()) {
            Toast.makeText(getApplicationContext(), "No Internet Connectivity", Toast.LENGTH_SHORT).show();
            return;
        }
        setup = false;
        mProgress = ProgressDialog.show(this, "",
                "Please Wait..", true);
        mProgress.setCancelable(false);

        new Thread(new Runnable() {
            @Override
            public void run()
            {
                //Code to execute in other thread
                try{
                    String res = new HttpRequestTask().execute("http://www.hoi.co.in/dapi/startacceptingride").get(120000, TimeUnit.MILLISECONDS);
                    JSONObject rideInfo = new JSONObject(res);
                    if( rideInfo.isNull("rider1id") || (rideInfo.getInt("rider1id") < 0)){
                        setup = false;
                    }else{
                        parseResponse(rideInfo);
                        getAppState(rideInfo);
                        setup = true;
                    }
                }catch(Exception e){
                    Log.e("Exception", e.getMessage());
                }


                //Code to execute in main thread
                mHandler = new Handler(getApplicationContext().getMainLooper());
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        mProgress.dismiss();
                        if (setup) {
                            if(nextDestinationChanged|| !nextPathSet){
                                nextPathSet = false;
                                setDestination();
                                nextDestinationChanged = false;
                            }
                            if(nextPassengerChanged){
                                if(nextPassenger == null){
                                    setUpHome("No Destination");
                                    nextPassengerChanged = false;
                                }else{
                                    setUpHome(nextPassenger.getAddress());
                                    nextPassengerChanged = false;
                                }
                            }
                            setup = false;
                        }else {
                            Toast.makeText(getApplicationContext(), "Error Occured, Start Accepting Ride failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                };
                mHandler.postDelayed(myRunnable, 500);
                Log.d("CODE", "Start Accepting Ride");
            }
        }).start();
    }

    public void requestStopAcceptingRide() {
        if(!isConnceted()) {
            Toast.makeText(getApplicationContext(), "No Internet Connectivity", Toast.LENGTH_SHORT).show();
            return;
        }
        setup = false;
        mProgress = ProgressDialog.show(this, "",
                "Please Wait..", true);
        mProgress.setCancelable(false);

        new Thread(new Runnable() {
            @Override
            public void run()
            {
                //Code to execute in other thread
                try{
                    String res = new HttpRequestTask().execute("http://www.hoi.co.in/dapi/stopacceptingride").get(120000, TimeUnit.MILLISECONDS);
                    JSONObject rideInfo = new JSONObject(res);
                    if( rideInfo.isNull("rider1id") || (rideInfo.getInt("rider1id") < 0)){
                        setup = false;
                    }else{
                        parseResponse(rideInfo);
                        getAppState(rideInfo);
                        setup = true;
                    }
                }catch(Exception e){
                    Log.e("Exception", e.getMessage());
                }


                //Code to execute in main thread
                mHandler = new Handler(getApplicationContext().getMainLooper());
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        mProgress.dismiss();
                        if (setup) {
                            if(nextDestinationChanged|| !nextPathSet){
                                nextPathSet = false;
                                setDestination();
                                nextDestinationChanged = false;
                            }
                            if(nextPassengerChanged){
                                if(nextPassenger == null){
                                    setUpHome("No Destination");
                                    nextPassengerChanged = false;
                                }else{
                                    setUpHome(nextPassenger.getAddress());
                                    nextPassengerChanged = false;
                                }
                            }
                            setup = false;
                        }else {
                            Toast.makeText(getApplicationContext(), "Error Occured, Stop Accepting Ride failed", Toast.LENGTH_LONG).show();
                        }
                    }
                };
                mHandler.postDelayed(myRunnable,500);
                Log.d("CODE", "Stop Ride");
            }
        }).start();
    }

    public void requestRefresh(View view){
        updateLocation();
    }

    private void applicationLogout() throws ExecutionException, InterruptedException, JSONException {
        String res = (new HttpRequestTask()).execute("http://www.hoi.co.in/dapi/logout").get();
        JSONObject data = new JSONObject(res);
        if(data.getBoolean("loggedout") == true){
            Toast.makeText(getApplicationContext(), data.getString("reason"), Toast.LENGTH_LONG).show();
            prefs.edit().remove("username");
            prefs.edit().remove("password");
            prefs.edit().commit();
            super.finish();
        }else {
            createAlertDialog("Logout Failed", data.getString("reason"));
        }
    }

    private void callNumber(String phoneNumber){
        String uri = "tel:" + phoneNumber.trim();
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse(uri));
        startActivity(intent);
    }

 /*
  * Functions to get the location of the user and set marker for pickup and drop location
  */

    private Location getLastKnownLocation(LocationManager mLocationManager) {
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l = mLocationManager.getLastKnownLocation(provider);

            if (l == null) {
                continue;
            }
            if (bestLocation == null
                    || l.getAccuracy() < bestLocation.getAccuracy()) {
                bestLocation = l;
            }
        }
        if (bestLocation == null) {
            return null;
        }
        return bestLocation;
    }

    @Override
    public void onLocationChanged(Location location) {
        // Getting latitude of the current location
        double latitude = location.getLatitude();

        // Getting longitude of the current location
        double longitude = location.getLongitude();

        if(currentLocationCoordinate != null){
            double segmentTravelled =  calculateDistance(currentLocationCoordinate.latitude, currentLocationCoordinate.longitude,
                    latitude, longitude);

            //todo evaluate income
            distanceTravelled += segmentTravelled;
            distanceCharge += distanceCharge + segmentTravelled * incomeRate[passengers.size()];
            estimatedFare = distanceCharge + 0.5 * estimatedTime();

            currentLocationCoordinate = new LatLng(latitude, longitude);

            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(currentLocationCoordinate, 15);
            mGoogleMap.animateCamera(yourLocation);

            if(!checkUpdateRide){
                checkUpdateRide = true;
                updateLocation();
            }
        }
    }

    private void updateLocation(){
        if(!isConnceted()){
            return;
        }
        setup = false;
        counter++;
        new Thread(new Runnable() {
            @Override
            public void run()
            {
                //Code to execute in other thread

                try{
                    JSONObject jObject = new JSONObject();
                    jObject.accumulate("ridelastlat", Math.round(currentLocationCoordinate.latitude * 10000.0)/10000.0);
                    jObject.accumulate("ridelastlong", Math.round(currentLocationCoordinate.longitude * 10000.0)/10000.0);
                    jObject.accumulate("estimateddistance", Math.round(distanceTravelled*100.0)/100.0);
                    jObject.accumulate("estimatedtime", estimatedTime());
                    jObject.accumulate("estimatedfare", Math.round(estimatedFare * 100.0)/100.0);
                    try{
                        if(counter % 10 == 0){
                            Geocoder gcd = new Geocoder(MainActivity.this, Locale.getDefault());
                            List<Address> addresses = gcd.getFromLocation(currentLocationCoordinate.latitude,
                                    currentLocationCoordinate.longitude, 1);
                            if (addresses.size() > 0 && addresses.get(0).getAdminArea() != null)
                                jObject.accumulate("state", addresses.get(0).getAdminArea());
                            else
                                jObject.accumulate("state", "");
                            counter = 0;
                        }
                        else{
                            jObject.accumulate("state", "");
                        }
                    }catch(Exception e){
                        jObject.accumulate("state", "");
                    }
                    System.out.println(jObject.toString());
                    String res = new HttpRequestTask(jObject).execute("http://www.hoi.co.in/dapi/updatelocation").get();
                    if(res!= null) System.out.println(res);
                    JSONObject data = new JSONObject(res);
                    if( data.isNull("rider1id") || (data.getInt("rider1id") < 0)){
                        setup = false;
                    }else{
                        setup = true;
                        arrived = data.getBoolean("next_arrived");
                        parseResponse(data);
                        getAppState(data);
                    }
                }catch(Exception e){
                    e.printStackTrace();
                    checkUpdateRide = false;
                }
                //Code to execute in main thread
                mHandler = new Handler(getApplicationContext().getMainLooper());
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (setup) {
                            if(nextDestinationChanged|| !nextPathSet){
                                nextPathSet = false;
                                setDestination();
                                nextDestinationChanged = false;
                            }
                            if(nextPassengerChanged) {
                                if(nextPassenger == null){
                                    setUpHome("No Destination");
                                    nextPassengerChanged = false;
                                }else{
                                    setUpHome(nextPassenger.getAddress());
                                    nextPassengerChanged = false;
                                }
                            }
                            setup = false;
                        }
                        else{
                            Log.d("Code", "Update Failed");
                        }
                        checkUpdateRide = false;

                    }
                };
                mHandler.post(myRunnable);
            }
        }).start();
    }

    private void getAppState(JSONObject data) throws JSONException{

        if(passengers.size() > 0) {
            if (nextDestination == null || nextDestination.latitude != data.getDouble("next_lat") ||
                    nextDestination.longitude != data.getDouble("next_long")) {
                nextDestination = new LatLng(data.getDouble("next_lat"), data.getDouble("next_long"));
                nextDestinationChanged = true;
            } else {
                nextDestinationChanged = false;
            }
            String destinationAddress = data.getString("next_address1") + data.getString("next_address2");
            if (nextPassenger == null || nextPassenger.getId() != data.getInt("nextriderid")) {
                nextPassenger = setNextPassenger(data.getInt("nextriderid"), data.getString("nextriderphone"),
                        nextDestination, destinationAddress);
                nextPassengerChanged = true;
            } else if (nextDestinationChanged) {
                nextPassenger = setNextPassenger(data.getInt("nextriderid"), data.getString("nextriderphone"),
                        nextDestination, destinationAddress);
                nextPassengerChanged = true;
            } else {
                nextPassengerChanged = false;
            }
        } else{
            if(nextPassenger != null) {
                nextPassengerChanged = true;
                nextDestinationChanged = true;
            }else{
                nextPassengerChanged = false;
                nextDestinationChanged = false;
            }
            nextPassenger = null;
            nextDestination = null;
        }
    }


    /*
     * evaluation methods
     */
    private float calculateDistance(Double slat, Double slong, Double dlat, Double dlong) {
        float distance = 0.0f;
        distance = (float) (142.6 * Math.sqrt(Math.pow(Math.abs(slat - dlat), 2) + Math.pow(Math.abs(slong - dlong), 2)));
        return distance;
    }

    private float calculateTime(Double slat, Double slong, Double dlat, Double dlong) {
        float time = 0.0f;
        time = (float) (225.73 * Math.sqrt(Math.pow(Math.abs(slat - dlat), 2) + Math.pow(Math.abs(slong - dlong), 2)));
        return time;
    }

    private int estimatedTime(){
        return (int) (System.currentTimeMillis() - upTime)/60000;
    }

    /*
     * Dialogs Methods
     */

    public void createAlertDialog(String title, String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View dView = inflater.inflate(R.layout.dialog_alert, null);
        // Add the buttons
        TextView msg = (TextView) dView.findViewById(R.id.alert_message);
        TextView alertTitle = (TextView) dView.findViewById(R.id.alertTitle);
        alertTitle.setText(title);
        msg.setText(message);
        builder.setView(dView)
                // Add action buttons
                .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(Dialog.BUTTON_NEGATIVE).setVisibility(View.INVISIBLE);

    }

    public void startStopRideProcess(View view) {
        try{
            createRatingDialog(nextPassenger.getId());
        }catch(Exception e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Stop Ride failed", Toast.LENGTH_LONG).show();
        }
    }

    public void createRatingDialog(final int passengerid) throws ExecutionException, InterruptedException {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View dView = inflater.inflate(R.layout.dialog_rating, null);
        final RatingBar rating = (RatingBar) dView.findViewById(R.id.rating_bar);
        // Add the buttons

        builder.setView(dView)
                // Add action buttons
                .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        requestStopRide(passengerid);
                        new HttpRequestTask().execute("http://www.hoi.co.in/dapi/rateuserbydriver/" + passengerid + "/" + rating.getRating());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //nothing to be done when user cancels his action
                        dialog.dismiss();
                    }
                });
        // Set other dialog properties
        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        de.hdodenhof.circleimageview.CircleImageView pic = (de.hdodenhof.circleimageview.CircleImageView)
                dView.findViewById(R.id.co_passenger_dp);
        if(passengerid == nextPassenger.getId())
            pic.setImageBitmap(nextPassenger.getImage());
        else{
            for (int i = 0 ; i < passengers.size(); i++){
                if(passengerid == passengers.get(i).getId())
                    pic.setImageBitmap(passengers.get(i).getPassengerpic()); 
            }
        }
        dialog.show();
    }

    public void createMoneyMadeDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View dView = inflater.inflate(R.layout.dialog_money_made, null);
        // Add the buttons
        TextView distTV = (TextView) dView.findViewById(R.id.distance);
        TextView durTV = (TextView) dView.findViewById(R.id.duration);
        TextView moneyTV = (TextView) dView.findViewById(R.id.income);

        distTV.setText("\u20B9 "+distanceFare);
        durTV.setText("\u20B9 "+timeFare);
        moneyTV.setText("\u20B9 "+totalFare);
        builder.setView(dView)
                // Add action buttons
                .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(Dialog.BUTTON_NEGATIVE).setVisibility(View.INVISIBLE);
    }

    /*
     * Miscellaneous Methods
     */

    private boolean isConnceted() {
        ConnectivityManager cm =
                (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
    }


    /*
     * Received data parser and modifying function
     */
    public void passengerList(View view) {
        Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.list_passenger);

        ListView passengerList = (ListView) dialog.findViewById(R.id.lv);
        PassengerListAdapter passengerListAdapter = new PassengerListAdapter(MainActivity.this, passengers, getResources());
        passengerList.setAdapter(passengerListAdapter);
        dialog.setCancelable(true);
        dialog.setTitle("Passengers");
        dialog.show();
    }

    public void parseResponse(JSONObject userData){
        passengers = new ArrayList<Passenger>();
        try {
            if (!userData.isNull("rider1id")&& userData.getInt("rider1id") != 0) {
                passengers.add(new Passenger(userData.getString("rider1pic"), userData.getInt("rider1id"), userData.getBoolean("rider1upcoming")));
            }

            if (!userData.isNull("rider2id")&& userData.getInt("rider2id") != 0) {
                passengers.add(new Passenger(userData.getString("rider2pic"), userData.getInt("rider2id"), userData.getBoolean("rider2upcoming")));
            }

            if (!userData.isNull("rider3id")&& userData.getInt("rider3id") != 0){
                passengers.add(new Passenger(userData.getString("rider3pic"), userData.getInt("rider3id"), userData.getBoolean("rider3upcoming")));
            }
            if (!userData.isNull("rider4id")&& userData.getInt("rider4id") != 0) {
                passengers.add(new Passenger(userData.getString("rider4pic"), userData.getInt("rider4id"), userData.getBoolean("rider4upcoming")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("Passenger Size", "" + passengers.size());
    }

    public NextPassenger setNextPassenger(int id, String phone, LatLng location, String address){
        int i = 0;
        if(passengers.size() <= 0) {
            Log.d("No passengers", passengers.size() + "");
            return null;

        }
        for(; i < passengers.size(); i++){
            if(id == passengers.get(i).getId())
                break;
        }

        return new NextPassenger(id,phone,passengers.get(i).getPassengerpic(),
                location, address,passengers.get(i).isWaiting);
    }

    /*
     * Draw Path related functions
     */

    public void findDirections(double fromPositionDoubleLat, double fromPositionDoubleLong, double toPositionDoubleLat, double toPositionDoubleLong, String mode)
    {
        Map<String, String> map = new HashMap<String, String>();
        map.put(GetDirectionsAsyncTask.USER_CURRENT_LAT, String.valueOf(fromPositionDoubleLat));
        map.put(GetDirectionsAsyncTask.USER_CURRENT_LONG, String.valueOf(fromPositionDoubleLong));
        map.put(GetDirectionsAsyncTask.DESTINATION_LAT, String.valueOf(toPositionDoubleLat));
        map.put(GetDirectionsAsyncTask.DESTINATION_LONG, String.valueOf(toPositionDoubleLong));
        map.put(GetDirectionsAsyncTask.DIRECTIONS_MODE, mode);

        GetDirectionsAsyncTask asyncTask = new GetDirectionsAsyncTask();
        asyncTask.execute(map);
    }

    public void handleGetDirectionsResult(ArrayList<LatLng> directionPoints)
    {
        Polyline newPolyline;
        GoogleMap mMap = ((SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        PolylineOptions rectLine = new PolylineOptions().width(3).color(Color.BLUE);
        for(int i = 0 ; i < directionPoints.size() ; i++)
        {
            rectLine.add(directionPoints.get(i));
        }
        newPolyline = mMap.addPolyline(rectLine);
    }

    /*
     * getter methods
     */
    public NextPassenger getNextPassenger(){return nextPassenger;}

    public boolean getArrivedState(){
        return arrived;
    }

    private JSONObject getPostRequestData() throws JSONException, IOException {
        JSONObject data = new JSONObject();
        data.accumulate("ridelastlong", Math.round(currentLocationCoordinate.longitude*10000.0)/10000.0);
        data.accumulate("ridelastlat", Math.round(currentLocationCoordinate.latitude * 10000.0)/10000.0);
        data.accumulate("estimateddistance", Math.round(distanceTravelled*100.0)/100.0);
        data.accumulate("estimatedtime", estimatedTime());
        data.accumulate("estimatedfare", Math.round(estimatedFare*100.0)/100.0);
        Geocoder gcd = new Geocoder(MainActivity.this, Locale.getDefault());
        List<Address> addresses = gcd.getFromLocation(currentLocationCoordinate.latitude, currentLocationCoordinate.longitude, 1);
        if (addresses.size() > 0)
            data.accumulate("state", addresses.get(0).getAdminArea());
        else
            data.accumulate("state", "");
        return data;
    }

    /*
     * Asynchronous task
     */

    public class HttpRequestTask extends AsyncTask<String, Void, String> {

        private HttpResponse response;
        JSONObject data;
        boolean check;

        HttpRequestTask(JSONObject data){
            this.data = data;
            check = true;
        }

        HttpRequestTask(){
            this.data = new JSONObject();
            check = false;
        }

        @Override
        protected String doInBackground(String... url) {
            // TODO: attempt authentication against a network service.
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm");
            String currentDateandTime = sdf.format(new Date());
            Encryptor encryptor = new Encryptor();
            String encrptedkey = "";

            try {
                encrptedkey = encryptor.getEncryptedKeyValue(currentDateandTime);
            } catch (InvalidKeyException e1) {
                e1.printStackTrace();
            } catch (IllegalBlockSizeException e1) {
                e1.printStackTrace();
            } catch (BadPaddingException e1) {
                e1.printStackTrace();
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            } catch (DecoderException e1) {
                e1.printStackTrace();
            }

            try {

                HttpPost request = new HttpPost(url[0]);
                request.addHeader("Authorization", "Basic " + authenticationHeader);
                request.addHeader("androidkey", encrptedkey);

                if(check){
                    data.accumulate("requestdatetime", currentDateandTime);
                    System.out.println(data.toString());
                    request.addHeader("Content-Type", "application/json");
                    request.setEntity(new StringEntity(data.toString()));
                }
                else{
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
                    nameValuePairs.add(new BasicNameValuePair("counter", currentDateandTime));
                    request.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                }
                HttpClient httpclient = new DefaultHttpClient();

                try {
                    response = httpclient.execute(request);
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            InputStream inputStream = null;
            String result = null;
            try {
                HttpEntity entity = response.getEntity();
                inputStream = entity.getContent();

                // json is UTF-8 by default
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null)
                {
                    sb.append(line + "\n");
                }
                result = sb.toString();
            } catch (Exception e) {
                // Oops
            }
            finally {
                try{if(inputStream != null)inputStream.close();}catch(Exception squish){}
            }
            return result;
        }

        @Override
        protected void onPostExecute(String data) {}

        @Override
        protected void onCancelled() {

        }
    }

    public class GetDirectionsAsyncTask extends AsyncTask<Map<String, String>, Object, ArrayList> {
        public static final String USER_CURRENT_LAT = "user_current_lat";
        public static final String USER_CURRENT_LONG = "user_current_long";
        public static final String DESTINATION_LAT = "destination_lat";
        public static final String DESTINATION_LONG = "destination_long";
        public static final String DIRECTIONS_MODE = "directions_mode";
        private Exception exception;
        private ProgressDialog progressDialog;

        public void onPreExecute()
        {
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Path to next Stop");
            progressDialog.show();
        }

        @Override
        public void onPostExecute(ArrayList result)
        {
            progressDialog.dismiss();
            if (exception == null)
            {
                handleGetDirectionsResult(result);
            }
            else
            {
                processException();
            }
        }

        @Override
        protected ArrayList doInBackground(Map<String, String>... params)
        {
            Map<String, String> paramMap = params[0];
            try
            {
                LatLng fromPosition = new LatLng(Double.valueOf(paramMap.get(USER_CURRENT_LAT)) , Double.valueOf(paramMap.get(USER_CURRENT_LONG)));
                LatLng toPosition = new LatLng(Double.valueOf(paramMap.get(DESTINATION_LAT)) , Double.valueOf(paramMap.get(DESTINATION_LONG)));
                GMapV2Direction md = new GMapV2Direction();
                Document doc = md.getDocument(fromPosition, toPosition, paramMap.get(DIRECTIONS_MODE));
                ArrayList directionPoints = md.getDirection(doc);
                return directionPoints;
            }
            catch (Exception e)
            {
                exception = e;
                return null;
            }
        }

        private void processException()
        {
            Toast.makeText(getApplicationContext(), getString(R.string.error_when_retrieving_data), Toast.LENGTH_LONG).show();
        }
    }



    /*
     * Imported methods ... unused
     */

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onMapClick(LatLng latLng) {

        // TODO Auto-generated method stub
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
    }
}



