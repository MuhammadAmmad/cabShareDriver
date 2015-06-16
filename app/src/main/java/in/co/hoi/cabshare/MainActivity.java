package in.co.hoi.cabshare;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
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
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
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
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends ActionBarActivity implements android.location.LocationListener,GoogleMap.OnMapClickListener,LoaderCallbacks<Cursor>,GoogleMap.OnMarkerDragListener {

    private static String authenticationHeader;
    /*
     * variables for source and destination on map
     */
	private GoogleMap mGoogleMap;
    private Marker sourceMarker;
    private Marker destinationMarker;
    private Marker cabMarker;
    private LatLng currentLocationCoordinate;
    private LatLng cabLatestCoordinate;


    /*
     *   variables for navigation drawer
     */

    private Toolbar toolbar;                              // Declaring the Toolbar Object

    RecyclerView mRecyclerView;                           // Declaring RecyclerView
    RecyclerView.Adapter mAdapter;                        // Declaring Adapter For Recycler View
    RecyclerView.LayoutManager mLayoutManager;            // Declaring Layout Manager as a linear layout manager
    DrawerLayout Drawer;                                  // Declaring DrawerLayout

    ActionBarDrawerToggle mDrawerToggle;                  // Declaring Action Bar Drawer Toggle


    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    List<DrawerItem> dataList;
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private String mActivityTitle;
    private boolean checkForGPS;

    private boolean inRide;
    private boolean awaitingRide;
    private boolean ratingPending;
    ObscuredSharedPreferences prefs;
    
    private CabBookingDetails cabBookingDetails;
    private  RideDetail rideDetails;

    private int rideRequestId;
    private int cabArrivalDuration;
    private int cabDistanceAway;
    private final static int BASECHARGE = 30;

    Timer timer;
    TimerTask timerTask;

    // fare related variables
    private float cabFare = 0.0f;
    private float billingRate = 10f;
    private float distanceTravelled = 0.0f;
    private float durationTravelled = 0;
    private int stateCounter = 0;

    //rating related variables
    private int driverRating = 0;
    private int pass1Rating = 0;
    private int pass2Rating = 0;
    private int pass3Rating = 0;
    private int pass4Rating = 0;



    JSONObject cabBookingData;
    JSONObject cabAwaitingData;
    JSONObject userData;


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

        prefs= ObscuredSharedPreferences.getPrefs(this, "Hoi Cabs", Context.MODE_PRIVATE);
        String TITLES[] = {"Home","Wallet","Contact Us","Complaint","Help"};
        int ICONS[] = {R.drawable.ic_home,R.drawable.ic_wallet,R.drawable.ic_contact,R.drawable.ic_comp,R.drawable.ic_favorite};
        String NAME ="", EMAIL="", PICURL="";

        try {

            userData = new JSONObject(getIntent().getStringExtra("userData"));
            inRide = userData.getBoolean("inaride");
            awaitingRide = userData.getBoolean("awaitingride");
            ratingPending = userData.getBoolean("hastorateprevious");
            rideRequestId  = userData.getInt("genriderequestid");
            NAME = userData.getString("name");
            EMAIL = userData.getString("username");
            PICURL = userData.getString("displaypic");
            if(getIntent().hasExtra("authenticationHeader"))
                authenticationHeader = getIntent().getStringExtra("authenticationHeader");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //Updating the user state in applciation
        try {
        if(awaitingRide){
            if(prefs.contains("CabBookingDetail")) {
                cabBookingData = new JSONObject(prefs.getString("CabBookingDetail", null));
                getCabBookingData();
            }

            if(prefs.contains("CabAwaitingDetail")) {
                cabAwaitingData = new JSONObject(prefs.getString("CabAwaitingDetail", null));
                getCabAwaitingData();
            }
        }
        } catch (JSONException e) {
            e.printStackTrace();
        }




        checkForGPS = false;

        // Initializing Navigation Drawer




        mRecyclerView = (RecyclerView) findViewById(R.id.RecyclerView); // Assigning the RecyclerView Object to the xml View

        mRecyclerView.setHasFixedSize(true);                            // Letting the system know that the list objects are of fixed size

        mAdapter = new MyAdapter(TITLES,ICONS,NAME,EMAIL,R.drawable.male_user);       // Creating the Adapter of MyAdapter class(which we are going to see in a bit)
        // And passing the titles,icons,header view name, header view email,
        // and header view profile picture

        mRecyclerView.setAdapter(mAdapter);                              // Setting the adapter to RecyclerView

        mLayoutManager = new LinearLayoutManager(this);                 // Creating a layout Manager

        mRecyclerView.setLayoutManager(mLayoutManager);                 // Setting the layout Manager


        Drawer = (DrawerLayout) findViewById(R.id.drawer_layout);        // Drawer object Assigned to the view
        mDrawerToggle = new ActionBarDrawerToggle(this, Drawer,
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

        Drawer.setDrawerListener(mDrawerToggle); // Drawer Listener set to the Drawer toggle
        mDrawerToggle.syncState();               // Finally we set the drawer toggle sync State

        /*
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



        mDrawerLayout.setDrawerListener(mDrawerToggle);


        if (savedInstanceState == null) {
            SelectItem(0);
        }*/

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


                try {
                    onCreateView();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        getIntent().setAction("Already created");
        }

    private void addHorizontalScrollView(){

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(220, 220);
        de.hdodenhof.circleimageview.CircleImageView c1 = new CircleImageView(getApplicationContext());
        c1.setLayoutParams(layoutParams);
        c1.setImageResource(R.drawable.male_user);
        c1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createRatingDialog();
            }
        });


        de.hdodenhof.circleimageview.CircleImageView c2 = new CircleImageView(getApplicationContext());
        c2.setLayoutParams(layoutParams);
        c2.setImageResource(R.drawable.male_user);
        c2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createRatingDialog();
            }
        });

        de.hdodenhof.circleimageview.CircleImageView c3 = new CircleImageView(getApplicationContext());
        c3.setLayoutParams(layoutParams);
        c3.setImageResource(R.drawable.male_user);
        c3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createRatingDialog();
            }
        });

        de.hdodenhof.circleimageview.CircleImageView c4 = new CircleImageView(getApplicationContext());
        c4.setLayoutParams(layoutParams);
        c4.setImageResource(R.drawable.male_user);
        c4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createRatingDialog();
            }
        });

        de.hdodenhof.circleimageview.CircleImageView cUnknown = new CircleImageView(getApplicationContext());
        cUnknown.setLayoutParams(layoutParams);
        cUnknown.setImageResource(R.drawable.male_user);
        cUnknown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createRatingDialog();
            }
        });
        LinearLayout hv = (LinearLayout) findViewById(R.id.pasengerlist);


        hv.addView(c1);
        hv.addView(c2);
        hv.addView(c3);
        hv.addView(c4);
        hv.addView(cUnknown);
    }

    private void removeHorizontalScrollView(){
        HorizontalScrollView hv = (HorizontalScrollView) findViewById(R.id.horizontalScrollView);
        hv.removeAllViews();
    }

    private void createRatingDialog(){

    }

    private void onCreateView () throws JSONException {
        System.out.println("Creating View depending on status");
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        ImageButton bi = (ImageButton)findViewById(R.id.shareCabRates);
        Button b1 = (Button) findViewById(R.id.search_source_button);
        Button b2 = (Button) findViewById(R.id.search_destination_button);
        Button b3 = (Button) findViewById(R.id.book_ride_button);
        Button b4 = (Button) findViewById(R.id.start_ride_button);
        ImageButton iBsrc = (ImageButton)findViewById(R.id.save_source);
        ImageButton iBdst = (ImageButton)findViewById(R.id.save_destination);




        /*
            Create view for user when he is waiting for his/her ride
         */
        if(awaitingRide){
            b1.setVisibility(View.VISIBLE);
            b2.setVisibility(View.INVISIBLE);
            b3.setVisibility(View.VISIBLE);
            b4.setVisibility(View.VISIBLE);
            bi.setVisibility(View.INVISIBLE);
            iBsrc.setVisibility(View.INVISIBLE);
            iBdst.setVisibility(View.INVISIBLE);

            b1.setText("Track Your Ride");
            b3.setText("Cancel Cab");
            b4.setText("Start Ride");
            setTitle("Awaiting Ride");

            if(sourceMarker!= null) sourceMarker.remove();
            if(destinationMarker!= null) destinationMarker.remove();

            startTimer();

        }
        /*
            View for user when he is in ride
         */
        else if(inRide){

            bi.setVisibility(View.INVISIBLE);
            b1.setVisibility(View.INVISIBLE);
            b2.setVisibility(View.INVISIBLE);
            b3.setVisibility(View.VISIBLE);
            b4.setVisibility(View.INVISIBLE);
            iBsrc.setVisibility(View.INVISIBLE);
            iBdst.setVisibility(View.VISIBLE);
            addHorizontalScrollView();



            b3.setText("End Ride");
            setTitle("In Ride");

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

            setTitle("Rating");
            bi.setVisibility(View.INVISIBLE);
            b1.setVisibility(View.INVISIBLE);
            b2.setVisibility(View.INVISIBLE);
            b3.setVisibility(View.INVISIBLE);
            b4.setVisibility(View.INVISIBLE);
            iBsrc.setVisibility(View.INVISIBLE);
            iBdst.setVisibility(View.INVISIBLE);
            //b2.setVisibility(View.VISIBLE);
            //b1.setText("Set PickUp Loaction");
        }
        /*
            View for user when he is booking ride
         */
        else{
            bi.setVisibility(View.VISIBLE);
            b1.setVisibility(View.VISIBLE);
            b2.setVisibility(View.VISIBLE);
            b3.setVisibility(View.VISIBLE);
            iBsrc.setVisibility(View.VISIBLE);
            iBdst.setVisibility(View.VISIBLE);
            b4.setVisibility(View.INVISIBLE);
            b1.setText("Set PickUp Location");
            b2.setText("Set Drop Location");
            b3.setText("Book Cab");

            setTitle("Book Cab");
            removeHorizontalScrollView();
            mGoogleMap.setMyLocationEnabled(false);
            if(currentLocationCoordinate == null){


                // Creating a criteria object to retrieve provider
                Criteria criteria = new Criteria();

                System.out.println("check:Check3");
                // Getting the name of the best provider

                String provider = locationManager.getBestProvider(criteria, true);

                Location location = getLastKnownLocation(locationManager);

                currentLocationCoordinate = new LatLng(location.getLatitude(), location.getLongitude());

            }

            IconGenerator iconFactory = new IconGenerator(this);
            iconFactory.setColor(Color.GREEN);
            sourceMarker = addIcon(iconFactory, "PickUp", new LatLng(currentLocationCoordinate.latitude, currentLocationCoordinate.longitude));
            sourceMarker.isDraggable();

            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(currentLocationCoordinate, 16);
            mGoogleMap.animateCamera(yourLocation);
        }
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first
    }

    public void saveSource(View view){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final Double lat = sourceMarker.getPosition().latitude;
        final Double lon = sourceMarker.getPosition().longitude;
        final String address = getAddress(lat,lon)[0];
        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View dview = inflater.inflate(R.layout.save_dialog, null);
        final Spinner addtype = (Spinner)dview.findViewById(R.id.address_type);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.address_type, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        addtype.setAdapter(adapter);
        TextView tvadd = (TextView) dview.findViewById(R.id.address);
        tvadd.setText(address);
        // Add the buttons
        builder.setView(dview)
                // Add action buttons
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // todo when user want to save the location

                        try {
                            JSONObject jObject = new JSONObject();
                            jObject.put("latitude", lat);
                            jObject.put("longitude", lon);
                            jObject.put("address", address);
                            if(prefs.contains(addtype.getSelectedItem().toString())){
                                prefs.edit().remove(addtype.getSelectedItem().toString());
                            }
                            prefs.edit().putString(addtype.getSelectedItem().toString(), jObject.toString()).commit();


                        } catch (JSONException e) {
                            //todo when exception in saving json occurs

                        }


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
        dialog.show();

    }




    public void saveDestination(View view){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final Double lat = destinationMarker.getPosition().latitude;
        final Double lon = destinationMarker.getPosition().longitude;
        final String address = getAddress(lat,lon)[0];


        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View dview = inflater.inflate(R.layout.save_dialog, null);
        final Spinner addtype = (Spinner)dview.findViewById(R.id.address_type);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.address_type, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        addtype.setAdapter(adapter);
        TextView tvadd = (TextView) dview.findViewById(R.id.address);
        tvadd.setText(address);
        // Add the buttons
        builder.setView(dview)
                // Add action buttons
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Double lat = destinationMarker.getPosition().latitude;
                        Double lon = destinationMarker.getPosition().longitude;
                        String address = getAddress(lat, lon)[0];
                        try {
                            JSONObject jObject = new JSONObject();
                            jObject.put("latitude", lat);
                            jObject.put("longitude", lon);
                            jObject.put("address", address);
                            if (prefs.contains(addtype.getSelectedItem().toString())) {
                                prefs.edit().remove(addtype.getSelectedItem().toString());
                            }
                            prefs.edit().putString(addtype.getSelectedItem().toString(), jObject.toString()).commit();


                        } catch (JSONException e) {
                            //todo when exception in saving json occurs

                        }

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

        dialog.show();
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

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void showCabRates(View view){
        System.out.println("popshow");

        // Inflate the popup_layout.xml
        LinearLayout viewGroup = (LinearLayout) this.findViewById(R.id.popup);
        LayoutInflater layoutInflater = (LayoutInflater) this
                .getSystemService(this.LAYOUT_INFLATER_SERVICE);
        View layout = layoutInflater.inflate(R.layout.popup_layout, viewGroup);

        layout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);



        // Creating the PopupWindow
        final PopupWindow popup = new PopupWindow(this);
        popup.setContentView(layout);
        popup.setWidth(layout.getMeasuredWidth());
        popup.setHeight(layout.getMeasuredHeight());
        popup.setFocusable(true);

        // Some offset to align the popup a bit to the right, and a bit down, relative to button's position.
        int OFFSET_X = 30;
        int OFFSET_Y = 30;

        // Clear the default translucent background
        popup.setBackgroundDrawable(new BitmapDrawable());

        // Displaying the popup at the specified location, + offsets.
        popup.showAtLocation(layout, Gravity.CENTER, 0, 0);

        // Getting a reference to Close button, and close the popup when clicked.
        Button close = (Button) layout.findViewById(R.id.close_popup);
        close.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                popup.dismiss();
            }
        });
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
                String[] str = {"Home", "Office", "Favorite1", "Favorite2", "Favorite3"};
                for(int i = 0 ; i < 5 ; i ++){
                    if(prefs.contains(str[i])){
                        try{
                            JSONObject jsonObject = new JSONObject(prefs.getString(str[i],null));
                            if(jsonObject != null) intent.putExtra(str[i], jsonObject.toString());
                        }catch (JSONException e){
                        }
                    }
                }

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
                String[] str = {"Home", "Office", "Favorite1", "Favorite2", "Favorite3"};
                for(int i = 0 ; i < 5 ; i ++){
                    if(prefs.contains(str[i])){
                        try{
                            JSONObject jsonObject = new JSONObject(prefs.getString(str[i],null));
                            if(jsonObject != null) intent.putExtra(str[i], jsonObject.toString());
                        }catch (JSONException e){
                        }
                    }
                }
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
                iconFactory.setColor(Color.GREEN);
                sourceMarker = addIcon(iconFactory, "PickUp", new LatLng(latitude, longitude));
                sourceMarker.isDraggable();
            } else if (requestCode == 3) {
                iconFactory.setColor(Color.RED);
                destinationMarker = addIcon(iconFactory, "Drop",new LatLng(latitude, longitude));
                destinationMarker.isDraggable();
            }

            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 16);
            mGoogleMap.animateCamera(yourLocation);
        }
    }

    public void confirmRide(View view) throws ExecutionException, InterruptedException, JSONException {

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

            DistanceDurationInfo getDistDurInfoTask = new DistanceDurationInfo(new LatLng(cabBookingDetails.origin_latitude,
                    cabBookingDetails.origin_longitude),cabLatestCoordinate);
            float[] distdurInfo = getDistDurInfoTask.googleDistanceDurationInfo();

            if(hours == 0 && minutes > 0 && minutes < 5){
                cost = 0;
            }

            else if(distdurInfo[1] < 10){
                cost = 100;
            }

            else
                cost = BASECHARGE;

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            // Get the layout inflater
            LayoutInflater inflater = this.getLayoutInflater();
            View dView = inflater.inflate(R.layout.cancel_ride_dialog, null);
            // Add the buttons

            builder.setView(dView)
                    // Add action buttons
                    .setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            // todo when user cancels ride

                            awaitingRide = inRide = ratingPending = false;
                            CancelRideTask cancelRide = new CancelRideTask(authenticationHeader);
                            try {
                                String cancelRideResult = cancelRide.execute("http://www.hoi.co.in/api/cancelride/"+rideRequestId).get();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            }
                            stoptimertask();
                            try {
                                onCreateView();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    })
                    .setNegativeButton(R.string.cont, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //nothing to be done when user cancels his action
                        }
                    });
            // Set other dialog properties
            // Create the AlertDialog



            AlertDialog dialog = builder.create();
            final TextView tvCost = (TextView) dView.findViewById(R.id.cancelcost);
            tvCost.setText("Rs " + String.valueOf(cost));
            dialog.show();


        }else if(inRide){
            inRide = awaitingRide = false;
            CloseRideTask closeRide = new CloseRideTask(authenticationHeader);
            String str_result = closeRide.execute("http://www.hoi.co.in/api/closeride/"+rideRequestId).get();
            onCreateView();
        }
        else if(ratingPending){
            //do nothing
        }
        else {
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

                LatLng sourceCoordinate = new LatLng(sLatitude, sLongitude);
                LatLng destinationCoordinate = new LatLng(dLatitude, dLongitude);

                DistanceDurationInfo getDistDurInfoTask = new DistanceDurationInfo(sourceCoordinate, destinationCoordinate);
                float[] journeyDetails  = getDistDurInfoTask.googleDistanceDurationInfo();


                minFare = (int)Math.max(Math.ceil(3.5 * journeyDetails[0]
                        + 0.5 * journeyDetails[1]), minFare);
                maxFare = (int)Math.max(Math.ceil(10 * journeyDetails[0]
                        + 0.5 * journeyDetails[1]), maxFare);
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
                                try {
                                    String res = bookCab.execute("http://www.hoi.co.in/api/createriderequest").get();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                } catch (ExecutionException e) {
                                    e.printStackTrace();
                                }

                                awaitingRide = true;
                                try {
                                    onCreateView();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

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

    public void startRide(View view) throws ExecutionException, InterruptedException, JSONException {
        if(awaitingRide){
            awaitingRide = false;
            inRide = true;
            StartRideTask startRide = new StartRideTask(authenticationHeader);
            String str_result = startRide.execute("http://www.hoi.co.in/api/startride/"+rideRequestId).get();
            stoptimertask();
            onCreateView();
        }
        else if(inRide){

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
        float segmentDistance = calculateDistance(currentLocationCoordinate.latitude, currentLocationCoordinate.longitude,
                latitude, longitude);
        int segmentDuration = (int) calculateTime(currentLocationCoordinate.latitude, currentLocationCoordinate.longitude,
                latitude, longitude);
        distanceTravelled = distanceTravelled + segmentDistance;
        durationTravelled = durationTravelled + segmentDuration;

        cabFare = cabFare + billingRate * segmentDistance + 0.5f * segmentDuration;

        // Creating a LatLng object for the current location
        currentLocationCoordinate = new LatLng(latitude, longitude);

        UpdateRideTask updateRide = new UpdateRideTask(authenticationHeader);
        updateRide.execute("http://www.hoi.co.in/api/update/"+rideRequestId);

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

    public void getCabBookingData() throws JSONException {
        cabBookingDetails = new CabBookingDetails(cabBookingData.getDouble("origin_latitude"),cabBookingData.getDouble("origin_longitude"),
                cabBookingData.getString("origin_address1"),cabBookingData.getString("origin_address2"),cabBookingData.getDouble("destination_latitude"),
                cabBookingData.getDouble("destination_longitude"),cabBookingData.getString("destination_address1"),
                cabBookingData.getString("destination_address2"));
        cabBookingDetails.requestdatetime = cabBookingData.getString("requestdatetime");
    }

    public void getCabAwaitingData() throws  JSONException{
        JSONObject carInfo  = cabAwaitingData.getJSONObject("carinfo");

        CarDetail carDetails = new CarDetail(carInfo.getInt("id"),carInfo.getString("regnumber"),
                carInfo.getString("model"), carInfo.getString("make"),carInfo.getString("color"));

        rideDetails = new RideDetail(cabAwaitingData.getDouble("ridelastlong"),cabAwaitingData.getDouble("ridelastlat"),
                carDetails, cabAwaitingData.getString("drivername"), cabAwaitingData.getString("driverpic"),cabAwaitingData.getString("driverphone"),
                cabAwaitingData.getString("driverbgc"),cabAwaitingData.getInt("occupancy"), (float) cabAwaitingData.getDouble("billingrate"),
                cabAwaitingData.getInt("riderequestid"));

        cabLatestCoordinate = new LatLng(cabAwaitingData.getDouble("ridelastlat"), cabAwaitingData.getDouble("ridelastlong"));

        rideRequestId = cabAwaitingData.getInt("riderequestid");
    }


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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }
/*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return false;
    }
*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_search:

                return true;
            case R.id.action_settings:
                applicationLogout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void applicationLogout(){
        prefs.edit().remove("username");
        prefs.edit().remove("password");
        prefs.edit().commit();
        super.finish();

    }

    /*
     * Represents an asynchronous cab booking task used to authenticate
     * the ride.
     */
    private class CabBookingTask extends AsyncTask<String, Void, String> {

        private final String authHeader;
        private HttpResponse response;
        private JSONObject jObject;

        CabBookingTask(String authHeader) {
            this.authHeader = authHeader;
        }

        @Override
        protected String doInBackground(String... url) {
            // TODO: attempt authentication against a network service.
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm");
            String currentDateandTime = sdf.format(new Date());
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
                cabBookingData = new JSONObject();
                cabBookingData.accumulate("origin_latitude", cabBookingDetails.origin_latitude);
                cabBookingData.accumulate("origin_longitude", cabBookingDetails.origin_longitude);
                cabBookingData.accumulate("origin_address1", cabBookingDetails.origin_address1);
                cabBookingData.accumulate("origin_address2", cabBookingDetails.origin_address2);
                cabBookingData.accumulate("destination_latitude", cabBookingDetails.destination_latitude);
                cabBookingData.accumulate("destination_longitude", cabBookingDetails.destination_longitude);
                cabBookingData.accumulate("destination_address1", cabBookingDetails.destination_address1);
                cabBookingData.accumulate("destination_address2", cabBookingDetails.destination_address2);
                cabBookingData.accumulate("requestdatetime", cabBookingDetails.requestdatetime);

                // 4. convert JSONObject to JSON to String
                json = cabBookingData.toString();

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

                try {
                    cabAwaitingData = new JSONObject(data);
                    getCabAwaitingData();

                } catch (JSONException e) {
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

    private class StartRideTask extends AsyncTask<String, Void, String> {

        private final String authHeader;
        private HttpResponse response;

        StartRideTask(String authHeader) {
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

                System.out.println("ch1");
                HttpPost request = new HttpPost(url[0]);
                request.addHeader("Authorization", "Basic " + authHeader);
                request.addHeader("androidkey",encrptedkey);
                request.addHeader("Content-Type", "application/json");

                String json = "";

                // 3. build jsonObject
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("ridelastlat", currentLocationCoordinate.latitude);
                jsonObject.accumulate("ridelastlong", currentLocationCoordinate.longitude);
                jsonObject.accumulate("requestdatetime", currentDateandTime);

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


            JSONObject jObject;

            if (data != null) {

                try{
                    jObject = new JSONObject(data);
                    cabLatestCoordinate = new LatLng(jObject.getDouble("ridelastlat"),jObject.getDouble("ridelastlong"));
                    billingRate = (float)jObject.getDouble("billingrate");

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

    private class UpdateRideTask extends AsyncTask<String, Void, String> {

        private final String authHeader;
        private HttpResponse response;

        UpdateRideTask(String authHeader) {
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

                System.out.println("ch1");
                HttpPost request = new HttpPost(url[0]);
                request.addHeader("Authorization", "Basic " + authHeader);
                request.addHeader("androidkey",encrptedkey);
                request.addHeader("Content-Type", "application/json");

                String json = "";

                // 3. build jsonObject
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("ridelastlat", currentLocationCoordinate.latitude);
                jsonObject.accumulate("ridelastlonglong", currentLocationCoordinate.longitude);
                jsonObject.accumulate("requestdatetime", currentDateandTime);
                jsonObject.accumulate("estimateddistance", distanceTravelled);
                jsonObject.accumulate("estimatedtime", durationTravelled);
                jsonObject.accumulate("estimatedfare", cabFare);

                if((++stateCounter)%10 == 0){
                    Geocoder gcd = new Geocoder(MainActivity.this, Locale.getDefault());
                    List<Address> addresses = gcd.getFromLocation(currentLocationCoordinate.latitude, currentLocationCoordinate.longitude, 1);
                    if (addresses.size() > 0)
                        jsonObject.accumulate("state", addresses.get(0).getAdminArea());
                    else
                        jsonObject.accumulate("state", "");
                }
                else jsonObject.accumulate("state", "");

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


            JSONObject jObject;

            if (data != null) {

                try{
                    jObject = new JSONObject(data);
                    cabLatestCoordinate = new LatLng(jObject.getDouble("ridelastlat"),jObject.getDouble("ridelastlong"));

                    billingRate = (float) jObject.getDouble("billingrate");

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

    private class CancelRideTask extends AsyncTask<String, Void, String> {

        private final String authHeader;
        private HttpResponse response;

        CancelRideTask(String authHeader) {
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

                HttpClient httpclient = new DefaultHttpClient();
                try {
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
                System.out.println("Cancel Ride :"+result);
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

                    billingRate = (float) jObject.getDouble("billingrate");


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

    private class CloseRideTask extends AsyncTask<String, Void, String> {

        private final String authHeader;
        private HttpResponse response;

        CloseRideTask(String authHeader) {
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

                System.out.println("ch1");
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


            JSONObject jObject;

            if (data != null) {

                try{
                    jObject = new JSONObject(data);
                    //driverRating = jObject.getInt("driverrating");
                   // cabLatestCoordinate = new LatLng(jObject.getDouble("ridelastlat"),jObject.getDouble("ridelastlong"));

                  //  billingRate = (float) jObject.getDouble("billingrate");

                    //IconGenerator iconFactory = new IconGenerator(MainActivity.this);
                    //iconFactory.setColor(Color.YELLOW);
                    //cabMarker = addIcon(iconFactory, "HOI CAB", cabLatestCoordinate);

//                    CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(cabLatestCoordinate, 15);
  //                  mGoogleMap.animateCamera(yourLocation);
                    //System.out.println(driverRating);


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



