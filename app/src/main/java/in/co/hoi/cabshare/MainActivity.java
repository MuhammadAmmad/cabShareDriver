package in.co.hoi.cabshare;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

public class MainActivity extends ActionBarActivity implements android.location.LocationListener,GoogleMap.OnMapClickListener,LoaderCallbacks<Cursor>,GoogleMap.OnMarkerDragListener {

    private static String authenticationHeader;
    private String userData;
    /*
     * variables for source and destination on map
     */
	private GoogleMap mGoogleMap;
    private Marker sourceMarker;
    private Marker destinationMarker;
    private Marker cabMarker;
    private LatLng sourceCoordinate;
    private LatLng destinationCoordinate;
    private LatLng currentLocationCoordinate;
    private LatLng cabLatestCoordinate;


    /*
     *   variables for navigation drawer
     */
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    CustomDrawerAdapter adapter;
    List<DrawerItem> dataList;
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private ArrayAdapter<String> mAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private String mActivityTitle;
    private boolean checkForGPS;

    private boolean inRide;
    private boolean awaitingRide;
    private boolean ratingPending;
    
    private CabBookingDetails cabBookingDetails;
    private  RideDetail rideDetails;
    private int rideRequestId;
    private int cabArrivalDuration;
    private int cabDistanceAway;
    private final static int BASECHARGE = 30;

    Timer timer;
    TimerTask timerTask;



    final Handler handler = new Handler();

    //ObscuredSharedPreferences sharedPreferences;
    //public static final String HOI_OBSCURED_PREFERENCES = "Hoi Cabs" ;

/*
 * Activity related functions
 */
    @Override
	protected void onCreate(Bundle savedInstanceState) {

        cabDistanceAway = 10000;
        cabArrivalDuration = 10000;

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        //Updating the user state in applciation

        inRide = getIntent().getBooleanExtra("inaride",false);
        awaitingRide = getIntent().getBooleanExtra("awaitingride",false);
        ratingPending = getIntent().getBooleanExtra("hastorateprevious",false);

        if(getIntent().hasExtra("authenticationHeader"))
            authenticationHeader = getIntent().getStringExtra("authenticationHeader");

        rideRequestId = getIntent().getIntExtra("genriderequestid", 0);

        System.out.println(rideRequestId);

        System.out.println(inRide + " " + awaitingRide + " " + ratingPending );

        checkForGPS = false;

        // Initializing Navigation Drawer
        dataList = new ArrayList<DrawerItem>();
        mTitle = mDrawerTitle = getTitle();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
                GravityCompat.START);
        fillNavigationDrawer();
        adapter = new CustomDrawerAdapter(this, R.layout.custom_drawer_item,
                dataList);
        mDrawerList.setAdapter(adapter);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        //sharedPreferences = ObscuredSharedPreferences.getPrefs(this,HOI_OBSCURED_PREFERENCES, Context.MODE_PRIVATE);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open,
                R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to
                // onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to
                // onPrepareOptionsMenu()
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            SelectItem(0);
        }

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            checkForGPS = true;
            System.out.println("GPS not enabled");
            Intent callGPSSettingIntent = new Intent(
                    android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(callGPSSettingIntent);
            onPause();
        }
        else {

            // Getting LocationManager object from System Service LOCATION_SERVICE
            //LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

            if (status != ConnectionResult.SUCCESS) { // Google Play Services are not available

                int requestCode = 10;
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
                dialog.show();
                System.out.println("check:Connectionfail");

            } else { // Google Play Services are available

                SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                mGoogleMap = fragment.getMap();
                mGoogleMap.setOnMarkerDragListener(this);
                mGoogleMap.setOnMapClickListener(this);
                System.out.println("check:Check1");
                mGoogleMap.setMyLocationEnabled(false);

                System.out.println("check:Check2");


                // Creating a criteria object to retrieve provider
                Criteria criteria = new Criteria();

                System.out.println("check:Check3");
                // Getting the name of the best provider

                String provider = locationManager.getBestProvider(criteria, true);

                System.out.println("check:Check4");
                // Getting Current Location
                Location location = getLastKnownLocation(locationManager);

                if (location != null) {
                    currentLocationCoordinate = new LatLng(location.getLatitude(), location.getLongitude());
                    onLocationChanged(location);

                }
                System.out.println("check:Check6");
                locationManager.requestLocationUpdates(provider, 20000, 0, this);

                //Creating view depending on the status of the user


                onCreateView();
            }
        }
        getIntent().setAction("Already created");
        }

    private void onCreateView (){
        System.out.println("Creating View depending on status");
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Button b1 = (Button) findViewById(R.id.search_source_button);
        Button b2 = (Button) findViewById(R.id.search_destination_button);
        Button b3 = (Button) findViewById(R.id.book_ride_button);
        Button b4 = (Button) findViewById(R.id.start_ride_button);

        /*
            Create view for user when he is waiting for his/her ride
         */
        if(awaitingRide){
            b1.setVisibility(View.VISIBLE);
            b2.setVisibility(View.INVISIBLE);
            b3.setVisibility(View.VISIBLE);
            b4.setVisibility(View.VISIBLE);

            b1.setText("Track Your Ride");
            b3.setText("Cancel Cab");
            b4.setText("Start Ride");

            if(sourceMarker!= null) sourceMarker.remove();
            if(destinationMarker!= null) destinationMarker.remove();
            startTimer();

        }
        /*
            View for user when he is in ride
         */
        else if(inRide){

            b1.setVisibility(View.INVISIBLE);
            b2.setVisibility(View.INVISIBLE);
            b3.setVisibility(View.INVISIBLE);
            b4.setVisibility(View.VISIBLE);
            b4.setText("End Ride");

            mGoogleMap.setMyLocationEnabled(true);

            // Creating a criteria object to retrieve provider
            Criteria criteria = new Criteria();

            System.out.println("check:Check3");
            // Getting the name of the best provider

            String provider = locationManager.getBestProvider(criteria, true);

            Location location = getLastKnownLocation(locationManager);

            currentLocationCoordinate = new LatLng(location.getLatitude(), location.getLongitude());
            locationManager.requestLocationUpdates(provider, 20000, 0, this);

            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(currentLocationCoordinate, 15);
            mGoogleMap.animateCamera(yourLocation);
        }
        /*
            View for user when he has not rated previous co-Passenger
         */
        else if(ratingPending){

            b1.setVisibility(View.INVISIBLE);
            b2.setVisibility(View.INVISIBLE);
            b3.setVisibility(View.INVISIBLE);
            b4.setVisibility(View.INVISIBLE);
            //b2.setVisibility(View.VISIBLE);
            //b1.setText("Set PickUp Loaction");
        }
        /*
            View for user when he is booking ride
         */
        else{

            b1.setVisibility(View.VISIBLE);
            b2.setVisibility(View.VISIBLE);
            b3.setVisibility(View.VISIBLE);
            b4.setVisibility(View.INVISIBLE);
            b1.setText("Set PickUp Loaction");
            b2.setText("Set Drop Location");
            b3.setText("Book Ride");
            if(currentLocationCoordinate == null){
                mGoogleMap.setMyLocationEnabled(true);

                // Creating a criteria object to retrieve provider
                Criteria criteria = new Criteria();

                System.out.println("check:Check3");
                // Getting the name of the best provider

                String provider = locationManager.getBestProvider(criteria, true);

                Location location = getLastKnownLocation(locationManager);

                currentLocationCoordinate = new LatLng(location.getLatitude(), location.getLongitude());

            }

            IconGenerator iconFactory = new IconGenerator(this);

            sourceCoordinate = new LatLng(currentLocationCoordinate.latitude, currentLocationCoordinate.longitude);
            iconFactory.setColor(Color.GREEN);
            sourceMarker = addIcon(iconFactory, "PickUp", sourceCoordinate);
            sourceMarker.isDraggable();

            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(currentLocationCoordinate, 16);
            mGoogleMap.animateCamera(yourLocation);
        }
    }

    @Override
    public void onPause() {
        System.out.println("Activity is paused");
        super.onPause();  // Always call the superclass method first
    }


    /*
     * Button press method call
     */
    @Override
    public void onResume(){
        if(checkForGPS == true) {
            checkForGPS = false;
            String action = getIntent().getAction();
            // Prevent endless loop by adding a unique action, don't restart if action is present
            if (action == null || !action.equals("Already created")) {

                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            }
            // Remove the unique action so the next time onResume is called it will restart
            else
                getIntent().setAction(null);
        }
        super.onResume();
    }

    public void sendMessageSource(View view)
    {
        if(awaitingRide){
            TrackRideTask trackRide = new TrackRideTask(authenticationHeader);
            trackRide.execute("http://www.hoi.co.in/api/track/"+rideRequestId);
        }
        else {
            if (currentLocationCoordinate == null)
                Toast.makeText(getApplicationContext(), "Please wait!! Searching current location",
                        Toast.LENGTH_LONG).show();
            else {
                if (sourceMarker != null) sourceMarker.remove();
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                intent.putExtra("Location", "S");
                intent.putExtra("CurrentLatitude", Double.toString(currentLocationCoordinate.latitude));
                intent.putExtra("CurrentLongitude", Double.toString(currentLocationCoordinate.longitude));
                startActivityForResult(intent, 2);
            }
        }
    }

    public void sendMessageDestination(View view)
    {

            if (currentLocationCoordinate == null)
                Toast.makeText(getApplicationContext(), "Please wait!! Searching current location",
                        Toast.LENGTH_LONG).show();
            else {
                if (destinationMarker != null) destinationMarker.remove();
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                intent.putExtra("Location", "D");
                intent.putExtra("CurrentLatitude", Double.toString(currentLocationCoordinate.latitude));
                intent.putExtra("CurrentLongitude", Double.toString(currentLocationCoordinate.longitude));
                startActivityForResult(intent, 3);
            }

    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        System.out.println("check1");
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_CANCELED) {
            Bundle bundle = data.getExtras();

            System.out.println("check2");
            Double latitude = Double.parseDouble(bundle.getString("latitude"));
            System.out.println("check3");
            Double longitude = Double.parseDouble(bundle.getString("longitude"));
            IconGenerator iconFactory = new IconGenerator(this);

            System.out.println("check4");
            // check if the request code is same as what is passed  here it is 2
            if (requestCode == 2) {

                sourceCoordinate = new LatLng(latitude, longitude);
                iconFactory.setColor(Color.GREEN);
                sourceMarker = addIcon(iconFactory, "PickUp", sourceCoordinate);
                sourceMarker.isDraggable();
            } else if (requestCode == 3) {
                destinationCoordinate = new LatLng(latitude, longitude);
                iconFactory.setColor(Color.RED);
                destinationMarker = addIcon(iconFactory, "Drop", destinationCoordinate);
                destinationMarker.isDraggable();

            }
            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 16);
            mGoogleMap.animateCamera(yourLocation);
        }
    }

    public void confirmRide(View view) throws ExecutionException, InterruptedException {

        if(awaitingRide){
            float cost = 100.0f;
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm");
            String currentDateandTime = sdf.format(new Date());
            int minutes = 0, hours = 0;
            if(cabBookingDetails != null) {
                minutes = Integer.parseInt(currentDateandTime.substring(14)) -
                        Integer.parseInt(cabBookingDetails.requestdatetime.substring(14));

                hours = Integer.parseInt(currentDateandTime.substring(11, 12)) -
                        Integer.parseInt(cabBookingDetails.requestdatetime.substring(11, 12));
            }
            DistanceDurationInfo getDistDurInfoTask = new DistanceDurationInfo(sourceCoordinate.latitude, sourceCoordinate.longitude,
                    cabLatestCoordinate.latitude, cabLatestCoordinate.longitude);
            getDistDurInfoTask.execute();

            String str_result = getDistDurInfoTask.execute().get();

            if(hours == 0 && minutes > 0 && minutes < 10){
                cost = 0;
            }
            else if(cabArrivalDuration/60 > 10){
                cost = BASECHARGE;
            }
            else
                cost = 100;

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            // Get the layout inflater
            LayoutInflater inflater = this.getLayoutInflater();
            view = inflater.inflate(R.layout.cancel_ride_dialog, null);
            // Add the buttons
            builder.setView(view)
                    // Add action buttons
                    .setPositiveButton(R.string.cancel_ride, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            // todo when user cancels ride

                            awaitingRide = inRide = ratingPending = false;
                            stoptimertask();
                            onCreateView();

                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //nothing to be done when user cancels his action
                        }
                    });
            // Set other dialog properties
            // Create the AlertDialog

            AlertDialog dialog = builder.create();
            TextView tvCost = (TextView) findViewById(R.id.cancel_cost);
            tvCost.setText("Cost for Cacellation : " + cost);
            dialog.show();
        }else if(inRide){
            // do nothing
        }
        else if(ratingPending){
            //do nothing
        }
        else {
            System.out.println("Confirm Ride Dialogue");
            int minFare = BASECHARGE;
            int maxFare = BASECHARGE;

            if (sourceMarker != null && destinationMarker != null) {
                final Double sLatitude = sourceMarker.getPosition().latitude;
                final Double sLongitude = sourceMarker.getPosition().longitude;
                final String[] sAddress = getAddress(sourceMarker.getPosition().latitude, sourceMarker.getPosition().longitude);
                final Double dLatitude = destinationMarker.getPosition().latitude;
                final Double dLongitude = destinationMarker.getPosition().longitude;
                final String[] dAddress = getAddress(destinationMarker.getPosition().latitude, destinationMarker.getPosition().longitude);


                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                /*
                minFare = (int)Math.max(Math.ceil(3.5 * calculateDistance(sLatitude, sLongitude, dLatitude, dLongitude)
                            + 0.5 * calculateTime(sLatitude, sLongitude, dLatitude, dLongitude)), minFare);


                maxFare = (int) Math.max(Math.ceil(10 * calculateDistance(sLatitude, sLongitude, dLatitude, dLongitude)
                        + 0.5 * calculateTime(sLatitude, sLongitude, dLatitude, dLongitude)), 30);
                */

                DistanceDurationInfo getDistDurInfoTask = new DistanceDurationInfo(sourceCoordinate.latitude, sourceCoordinate.longitude,
                        destinationCoordinate.latitude, destinationCoordinate.longitude);
                String str_result = getDistDurInfoTask.execute().get();

                minFare = (int)Math.max(Math.ceil(3.5 * cabDistanceAway/1000
                        + 0.5 * cabArrivalDuration/60), minFare);
                maxFare = (int)Math.max(Math.ceil(10 * cabDistanceAway/1000
                        + 0.5 * cabArrivalDuration/60), maxFare);
                // Get the layout inflater
                LayoutInflater inflater = this.getLayoutInflater();
                view = inflater.inflate(R.layout.confirm_dialog, null);
                // Add the buttons
                builder.setView(view)
                        // Add action buttons
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // todo when user confirms ride
                                cabBookingDetails = new CabBookingDetails(sLatitude, sLongitude, sAddress[0], sAddress[1], dLatitude,
                                        dLongitude, dAddress[0], dAddress[1]);

                                CabBookingTask bookCab = new CabBookingTask(authenticationHeader);
                                bookCab.execute("http://www.hoi.co.in/api/createriderequest");

                                awaitingRide = true;
                                onCreateView();

                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //todo when user cancels ride
                            }
                        });
                // Set other dialog properties
                // Create the AlertDialog
                AlertDialog dialog = builder.create();

                TextView sourceAdd1 = (TextView) view.findViewById(R.id.source_address1);
                TextView sourceAdd2 = (TextView) view.findViewById(R.id.source_address2);
                sourceAdd1.setText("PickUp:" + sAddress[0]);
                sourceAdd2.setText(sAddress[1]);
                TextView destinationAdd1 = (TextView) view.findViewById(R.id.destination_address1);
                destinationAdd1.setText("Drop:" + dAddress[0]);
                TextView destinationAdd2 = (TextView) view.findViewById(R.id.destination_address2);
                destinationAdd2.setText(dAddress[1]);
                TextView fare = (TextView) view.findViewById(R.id.ride_fare);

                fare.setText("FARE : "+ minFare + " to " + maxFare);


                dialog.show();
            }else {

                Toast.makeText(getApplicationContext(), "PickUp or Drop Location not set!!",
                        Toast.LENGTH_LONG).show();
            }
        }

    }

    public void startRide(View view){
        if(awaitingRide){
            awaitingRide = false;
            inRide = true;
            onCreateView();
        }
        else if(inRide){
            inRide = awaitingRide = false;
            onCreateView();
            //Todo check for rating pending for driver and co-pasengers
        }
    }


    /*
     * Navigation Drawer related funtions
     * dataList.add(new DrawerItem("Settings", R.drawable.ic_action_settings));
     */

    private void fillNavigationDrawer() {
        dataList.add(new DrawerItem("Book Ride", R.drawable.ic_action_ride));
        dataList.add(new DrawerItem("Like Us", R.drawable.ic_action_good));
        dataList.add(new DrawerItem("Favorite Places", R.drawable.ic_action_labels));
        dataList.add(new DrawerItem("Locate Ride", R.drawable.ic_action_search));
        dataList.add(new DrawerItem("Groups", R.drawable.ic_action_group));
        dataList.add(new DrawerItem("About Us", R.drawable.ic_action_about));
        dataList.add(new DrawerItem("Contact Us", R.drawable.ic_action_email));
        dataList.add(new DrawerItem("Help", R.drawable.ic_action_help));
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    public void SelectItem(int position) {

        Fragment fragment = null;
        Bundle args = new Bundle();
        switch (position) {
            case 0:
                fragment = new FragmentOne();
                args.putString(FragmentOne.ITEM_NAME, dataList.get(position)
                        .getItemName());
                args.putInt(FragmentOne.IMAGE_RESOURCE_ID, dataList.get(position)
                        .getImgResID());
                break;
            case 1:
                fragment = new FragmentTwo();
                args.putString(FragmentTwo.ITEM_NAME, dataList.get(position)
                        .getItemName());
                args.putInt(FragmentTwo.IMAGE_RESOURCE_ID, dataList.get(position)
                        .getImgResID());
                break;
            case 2:
                fragment = new FragmentThree();
                args.putString(FragmentThree.ITEM_NAME, dataList.get(position)
                        .getItemName());
                args.putInt(FragmentThree.IMAGE_RESOURCE_ID, dataList.get(position)
                        .getImgResID());
                break;
            case 3:
                fragment = new FragmentOne();
                args.putString(FragmentOne.ITEM_NAME, dataList.get(position)
                        .getItemName());
                args.putInt(FragmentOne.IMAGE_RESOURCE_ID, dataList.get(position)
                        .getImgResID());
                break;
            case 4:
                fragment = new FragmentTwo();
                args.putString(FragmentTwo.ITEM_NAME, dataList.get(position)
                        .getItemName());
                args.putInt(FragmentTwo.IMAGE_RESOURCE_ID, dataList.get(position)
                        .getImgResID());
                break;
            case 5:
                fragment = new FragmentThree();
                args.putString(FragmentThree.ITEM_NAME, dataList.get(position)
                        .getItemName());
                args.putInt(FragmentThree.IMAGE_RESOURCE_ID, dataList.get(position)
                        .getImgResID());
                break;
            case 6:
                fragment = new FragmentOne();
                args.putString(FragmentOne.ITEM_NAME, dataList.get(position)
                        .getItemName());
                args.putInt(FragmentOne.IMAGE_RESOURCE_ID, dataList.get(position)
                        .getImgResID());
                break;
            case 7:
                fragment = new FragmentTwo();
                args.putString(FragmentTwo.ITEM_NAME, dataList.get(position)
                        .getItemName());
                args.putInt(FragmentTwo.IMAGE_RESOURCE_ID, dataList.get(position)
                        .getImgResID());
                break;
            default:
                break;
        }

        fragment.setArguments(args);
        FragmentManager frgManager = getFragmentManager();
        frgManager.beginTransaction().replace(R.id.content_frame, fragment)
                .commit();

        mDrawerList.setItemChecked(position, true);
        setTitle(dataList.get(position).getItemName());
        mDrawerLayout.closeDrawer(mDrawerList);

    }

    private class DrawerItemClickListener implements
            ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            SelectItem(position);

        }
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

    private Marker addIcon(IconGenerator iconFactory, String text, LatLng position) {
        MarkerOptions markerOptions = new MarkerOptions().
                icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(text))).
                position(position).
                anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV()).draggable(true);

        return mGoogleMap.addMarker(markerOptions);
    }

    @Override
    public void onLocationChanged(Location location) {

        System.out.println("check:Location not null");

        // Getting latitude of the current location
        double latitude = location.getLatitude();

        // Getting longitude of the current location
        double longitude = location.getLongitude();

        // Creating a LatLng object for the current location
        currentLocationCoordinate = new LatLng(latitude, longitude);

        CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(currentLocationCoordinate, 15);
        mGoogleMap.animateCamera(yourLocation);



    }

    public String[] getAddress(double latitude, double longitude){
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        String[] address = new String[2];
        address[0] = address[1] = "";
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(latitude,longitude,1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(addresses != null){

            if(addresses.get(0).getAddressLine(0) != null) address[0] = addresses.get(0).getAddressLine(0);
            if(addresses.get(0).getLocality() != null) address[1] = addresses.get(0).getLocality();
            if(addresses.get(0).getAdminArea() != null) address[1] = address[1] +","+ addresses.get(0).getAdminArea();
            if(addresses.get(0).getPostalCode() != null) address[1] = address[1] +","+ addresses.get(0).getPostalCode();
            return address;
        }

        return new String[]{"Address could not be found","Address could not be found"};
    }
    
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
	public Loader<Cursor> onCreateLoader(int arg0, Bundle query) {
		CursorLoader cLoader = null;
		if(arg0==0)
			cLoader = new CursorLoader(getBaseContext(), PlaceProvider.SEARCH_URI, null, null, new String[]{ query.getString("query") }, null);
		else if(arg0==1)
			cLoader = new CursorLoader(getBaseContext(), PlaceProvider.DETAILS_URI, null, null, new String[]{ query.getString("query") }, null);
		return cLoader;
		
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// TODO Auto-generated method stub		
	}

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
            // TODO Auto-generated method stub
            LatLng dragPosition = marker.getPosition();
            double dragLat = dragPosition.latitude;
            double dragLong = dragPosition.longitude;
    }

    @Override
    public void onMapClick(LatLng latLng) {

        // TODO Auto-generated method stub
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
    }


    /*
     * Awaiting ride user state related methods
     */

    public void startTimer() {
        //set a new Timer
        timer = new Timer();
        //initialize the TimerTask's job
        initializeTimerTask();
        //schedule the timer, the TimerTask will run every half minute
        timer.schedule(timerTask, 2000, 30000); //
    }

    public void stoptimertask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        TrackRideTask trackRide = new TrackRideTask(authenticationHeader);
                        trackRide.execute("http://www.hoi.co.in/api/track/"+rideRequestId);
                    }
                });
            }
        };
    }

    /*
     * Booking ride related methods
     */

    private float calculateDistance(Double slat, Double slong, Double dlat, Double dlong){
        float distance =  0.0f;
        distance = (float)(142.6 * Math.sqrt(Math.pow(Math.abs(slat-dlat),2)+Math.pow(Math.abs(slong-dlong),2)));
        return distance;
    }

    private float calculateTime(Double slat, Double slong, Double dlat, Double dlong){
        float time =  0.0f;
        time = (float)(225.73 * Math.sqrt(Math.pow(Math.abs(slat-dlat),2)+Math.pow(Math.abs(slong-dlong),2)));
        return time;
    }

    /*
     * Asynchronous task to get the estimated distance and duration using
     * Google directions API
     */
    private class DistanceDurationInfo extends AsyncTask<Void, Void, String> {

        private HttpResponse response;
        StringBuilder stringBuilder = new StringBuilder();
        private String mKey = "key=AIzaSyBrl-RRzrwGisYJpFI2QhpcCeknXg_bSmw";
        private double slat, slong, dlat, dlong;

        public DistanceDurationInfo(double slat, double slong, double dlat, double dlong){
            this.slat = slat;
            this.slong = slong;
            this.dlat = dlat;
            this.dlong = dlong;
        }


        @Override
        protected String doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.
            try {

                String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + slat + "," +
                        slong + "&destination=" + dlat + "," + dlong + "&mode=driving&sensor=false&units=metric"+"&"+mKey;

                HttpPost httppost = new HttpPost(url);

                HttpClient client = new DefaultHttpClient();
                stringBuilder = new StringBuilder();


                response = client.execute(httppost);

            } catch (ClientProtocolException e) {
            } catch (IOException e) {
            }



            System.out.println("ch3");
            InputStream inputStream = null;
            String result = null;
            try {
                System.out.println("ch4");
                HttpEntity entity = response.getEntity();

                inputStream = entity.getContent();
                // json is UTF-8 by default
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                StringBuilder sb = new StringBuilder();

                System.out.println("ch5");
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
                System.out.println(result);
                try{if(inputStream != null)inputStream.close();}catch(Exception squish){}
            }

            return result;
        }



        @Override
        protected void onPostExecute(final String data) {


            if (data != null) {

                JSONObject jsonObject = new JSONObject();
                try {

                    jsonObject = new JSONObject(data);

                    JSONArray array = jsonObject.getJSONArray("routes");

                    JSONObject routes = array.getJSONObject(0);

                    JSONArray legs = routes.getJSONArray("legs");

                    JSONObject steps = legs.getJSONObject(0);

                    JSONObject distance = steps.getJSONObject("distance");

                    JSONObject duration = steps.getJSONObject("duration");

                    cabDistanceAway = Integer.parseInt(distance.getString("value"));
                    cabArrivalDuration = Integer.parseInt(duration.getString("value"));
                    System.out.println(cabDistanceAway + " " + cabArrivalDuration);
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            } else {
                //Todo if no data come from server

            }
        }

        @Override
        protected void onCancelled() {

        }
    }

    /*
     * Represents an asynchronous cab booking task used to authenticate
     * the ride.
     */
    private class CabBookingTask extends AsyncTask<String, Void, String> {

        private final String authHeader;
        private HttpResponse response;

        CabBookingTask(String authHeader) {
            this.authHeader = authHeader;
        }

        @Override
        protected String doInBackground(String... url) {
            // TODO: attempt authentication against a network service.
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm");
            String currentDateandTime = sdf.format(new Date());
            System.out.println(currentDateandTime);
            cabBookingDetails.requestdatetime = currentDateandTime;
            System.out.println(cabBookingDetails.requestdatetime);
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

                System.out.println("ch1");
                HttpPost request = new HttpPost(url[0]);
                request.addHeader("Authorization", "Basic " + authHeader);
                request.addHeader("androidkey",encrptedkey);
                request.addHeader("Content-Type", "application/json");

                String json = "";

                // 3. build jsonObject
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("origin_latitude", cabBookingDetails.origin_latitude);
                jsonObject.accumulate("origin_longitude", cabBookingDetails.origin_longitude);
                jsonObject.accumulate("origin_address1", cabBookingDetails.origin_address1);
                jsonObject.accumulate("origin_address2", cabBookingDetails.origin_address2);
                jsonObject.accumulate("destination_latitude", cabBookingDetails.destination_latitude);
                jsonObject.accumulate("destination_longitude", cabBookingDetails.destination_longitude);
                jsonObject.accumulate("destination_address1", cabBookingDetails.destination_address1);
                jsonObject.accumulate("destination_address2", cabBookingDetails.destination_address2);
                jsonObject.accumulate("requestdatetime", cabBookingDetails.requestdatetime);

                // 4. convert JSONObject to JSON to String
                json = jsonObject.toString();
                System.out.println(json);

                // 5. set json to StringEntity

                request.setEntity(new StringEntity(json));
                HttpClient httpclient = new DefaultHttpClient();
                try {
                    System.out.println("ch2");
                    response = httpclient.execute(request);

                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            System.out.println("ch3");
            InputStream inputStream = null;
            String result = null;
            try {
                System.out.println("ch4");
                HttpEntity entity = response.getEntity();

                inputStream = entity.getContent();
                // json is UTF-8 by default
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                StringBuilder sb = new StringBuilder();

                System.out.println("ch5");
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
                System.out.println(result);
                try{if(inputStream != null)inputStream.close();}catch(Exception squish){}
            }

            return result;
        }

        @Override
        protected void onPostExecute(final String data) {


            if (data != null) {

                System.out.println(data);
                //ParserTask rideDataParserTask = new ParserTask();
               // rideDataParserTask.execute(data);

            } else {
                //Todo if no data come from server

            }
        }

        @Override
        protected void onCancelled() {

        }
    }

    /** A class to parse the Ride Details send by server in JSON format*/
    private class ParserTask extends AsyncTask<String, Integer, HashMap<String,String>>{


        @Override
        protected HashMap<String, String> doInBackground(String... jsonData) {

            JSONObject jObject;
            HashMap<String, String> list = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                RideDetailParser rideDetailParser = new RideDetailParser();
                // Getting the parsed data as a List construct
                list = rideDetailParser.parse(jObject);
            }catch(Exception e){
                Log.d("Exception", e.toString());
            }
            return list;
        }

        @Override
        protected void onPostExecute(HashMap<String, String> result) {

            CarDetail carDetails = new CarDetail(Integer.parseInt(result.get("carid")), result.get("carregnumber"),
                    result.get("carmodel"), result.get("carmake"), result.get("carcolor"));

            rideDetails = new RideDetail(Double.parseDouble(result.get("ridelastlat")),Double.parseDouble(result.get("ridelastlong")),
                    carDetails, result.get("drivername"), result.get("driverpic"), result.get("driverphone"),
                    result.get("driverbgc"), Integer.parseInt(result.get("occupancy")), (float) Double.parseDouble(result.get("billingrate")),
                    Integer.parseInt(result.get("riderequestid")));

            cabLatestCoordinate = new LatLng(Double.parseDouble(result.get("ridelastlat")),Double.parseDouble(result.get("ridelastlong")));

            rideRequestId = Integer.parseInt(result.get("riderequestid"));

            //Todo after getting the name value pairs of ride
        }
    }

    private class TrackRideTask extends AsyncTask<String, Void, String> {

        private final String authHeader;
        private HttpResponse response;

        TrackRideTask(String authHeader) {
            this.authHeader = authHeader;
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
                request.addHeader("Authorization", "Basic " + authHeader);
                request.addHeader("androidkey",encrptedkey);

                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
                nameValuePairs.add(new BasicNameValuePair("counter", currentDateandTime));

                request.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                HttpClient httpclient = new DefaultHttpClient();
                try {
                    System.out.println("ch2");
                    response = httpclient.execute(request);

                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            InputStream inputStream = null;
            String result = null;
            try {
                System.out.println("ch4");
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
                System.out.println(result);
                try{if(inputStream != null)inputStream.close();}catch(Exception squish){}
            }

            return result;
        }

        @Override
        protected void onPostExecute(final String data) {

            JSONObject jObject;

            if (data != null) {

                try{
                    jObject = new JSONObject(data);
                    cabLatestCoordinate = new LatLng(jObject.getDouble("ridelastlat"),jObject.getDouble("ridelastlong"));

                    IconGenerator iconFactory = new IconGenerator(MainActivity.this);
                    iconFactory.setColor(Color.YELLOW);
                    cabMarker = addIcon(iconFactory, "HOI CAB", cabLatestCoordinate);

                    CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(cabLatestCoordinate, 15);
                    mGoogleMap.animateCamera(yourLocation);


                }catch(Exception e){
                    Log.d("Exception", e.toString());
                }

            } else {
                //Todo if no data come from server

            }
        }

        @Override
        protected void onCancelled() {

        }
    }

}



