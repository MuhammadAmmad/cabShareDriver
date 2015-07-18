package in.co.hoi.cabshare;

import android.app.AlertDialog;
import android.app.Dialog;
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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
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
import java.text.ParseException;
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

public class MainActivity extends ActionBarActivity implements android.location.LocationListener,GoogleMap.OnMapClickListener,LoaderCallbacks<Cursor>,GoogleMap.OnMarkerDragListener {

    /*
     * Constants
     */
    private final static int BASECHARGE = 30;
    private static String authenticationHeader;
    private final static String BASE_SERVER_URL = "http://www.hoi.co.in/api/";

    /*
     * Markers for Google Map
     */
	private GoogleMap mGoogleMap;
    private Marker sourceMarker;
    private Marker destinationMarker;
    private Marker cabMarker;

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

    private DetailCar carDetails;
    private DetailCurrentRide currentRideDetails;
    private DetailDriver driverDetails;
    private DetailUser userDetails;
    private boolean inRide;
    private boolean awaitingRide;
    private boolean ratingPending;
    private int rideRequestId;
    private int cabArrivalDuration;
    private int cabDistanceAway;
    CabBookingDetails cabBookingDetails;
    private Double balance;

    /*
     * Miscellaneous variables
     */
    ObscuredSharedPreferences prefs;
    private LatLng currentLocationCoordinate;
    private LatLng cabLatestCoordinate;
    Timer timer;
    TimerTask timerTask;
    final Handler handler = new Handler();
    int defaultView = 1;
    View fragmentMainView;
    View fragmentWalletView;
    ProgressDialog processDialog;

    /*
     * Variable for fare calculation
     */
    private float billingRate = 10f;
    private int stateCounter = 0;
    private double currentRideFare = 0.0;

    /*
     * variable for user and driver rating
     */
    private int driverRating = 0;
    List<CoPassenger> coPassengers;

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
        String TITLES[] = {"Home","Wallet","Change Password","Contact Us","Complaint","LogOut"};
        int ICONS[] = {R.drawable.ic_home,R.drawable.ic_wallet,R.drawable.ic_edit,R.drawable.ic_contact,R.drawable.ic_comp,R.drawable.ic_logout};

        try {

            JSONObject userData = new JSONObject(getIntent().getStringExtra("userData"));
            userDetails = new DetailUser(userData);

            inRide = userData.getBoolean("inaride");
            awaitingRide = userData.getBoolean("awaitingride");
            ratingPending = userData.getBoolean("hastorateprevious");
            rideRequestId  = userData.getInt("genriderequestid");
            balance = userData.getDouble("availableinr");

            if(getIntent().hasExtra("authenticationHeader"))
                authenticationHeader = getIntent().getStringExtra("authenticationHeader");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        /*
         * Updating the user state if user already booked the ride
         */
        if(inRide || awaitingRide){
            HttpRequestTask rideRequestInfo = new HttpRequestTask();
            try {
                Log.d("CODE_FLOW", "CAB DETAILS_REQESTED");
                String res = rideRequestInfo.execute("http://www.hoi.co.in/api/riderequestinfo/"+rideRequestId).get();
                System.out.println(res);
                JSONObject jObject, genRideRequestInfo, rideResponceInfo;
                try {
                    jObject = new JSONObject(res);
                    genRideRequestInfo = jObject.getJSONObject("grri");
                    rideResponceInfo = jObject.getJSONObject("rrd");
                    currentRideDetails = new DetailCurrentRide(genRideRequestInfo);
                    carDetails = new DetailCar(rideResponceInfo.getJSONObject("carinfo"));
                    driverDetails = new DetailDriver(jObject);
                    cabLatestCoordinate = new LatLng(rideResponceInfo.getDouble("ridelastlong"), rideResponceInfo.getDouble("ridelastlat"));
                } catch (Exception e) {
                    Log.d("Exception", e.toString());
                }
                Log.d("CODE_FLOW", "CAB DETAILS_RECEIVED");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        // Initializing Navigation Drawer
        mRecyclerView = (RecyclerView) findViewById(R.id.RecyclerView); // Assigning the RecyclerView Object to the xml View
        mRecyclerView.setHasFixedSize(true);                            // Letting the system know that the list objects are of fixed size
        mAdapter = new MyAdapter(TITLES,ICONS,userDetails.name,userDetails.phone,userDetails.displayPic,getApplicationContext());       // Creating the Adapter of MyAdapter class(which we are going to see in a bit)

        // And passing the titles,icons,header view name, header view email,
        // and header view profile picture
        mRecyclerView.setAdapter(mAdapter);                              // Setting the adapter to RecyclerView
        final GestureDetector mGestureDetector = new GestureDetector(MainActivity.this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }
        });

        try {
            drawerItemSelectedAction(defaultView);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
                View child = recyclerView.findChildViewUnder(motionEvent.getX(),motionEvent.getY());
                if(child!=null && mGestureDetector.onTouchEvent(motionEvent)){
                    Drawer.closeDrawers();
                    try {
                        drawerItemSelectedAction(recyclerView.getChildPosition(child));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(MainActivity.this,"The Item Clicked is: "+recyclerView.getChildPosition(child),Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            }
        });
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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        Drawer.setDrawerListener(mDrawerToggle); // Drawer Listener set to the Drawer toggle
        mDrawerToggle.syncState();               // Finally we set the drawer toggle sync State
    }

    private boolean checkForConnectivity(){
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());
        if (status != ConnectionResult.SUCCESS) { // Google Play Services are not available
            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
            dialog.show();
            Log.d("Connectivity", "Failed");
            return false;
        }
        return true;
    }

    private void setMapFragment(){
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mGoogleMap = fragment.getMap();
        mGoogleMap.setOnMarkerDragListener(this);
        mGoogleMap.setOnMapClickListener(this);
        mGoogleMap.setMyLocationEnabled(false);

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

    private void drawerItemSelectedAction(int childPosition) throws InterruptedException, ExecutionException, JSONException {

        String barTitle = "";
        switch (childPosition){
            case 0: //Todo show the user profile
                    break;
            case 1: //Todo check for connectivity for Google map api
                    if(!checkForConnectivity()) onStop();
                    setMapFragment();
                    if(inRide)
                        setStateInRide();
                    else if(awaitingRide)
                        setStateAwaitingRide();
                    else
                        setStateBookRide();
                    break;
            case 2: setUpWallet();
                    break;
            case 3: createChangePassDialog();
                    break; //Todo change password dialog
            case 4: setUpContact();
                    break;
            case 5: break;
            case 6: applicationLogout();
                    break;
        }
    }

    private void createChangePassDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View dView = inflater.inflate(R.layout.dialog_changepass, null);
        // Add the buttons

        final EditText oldPass = (EditText) dView.findViewById(R.id.old_pass);
        final EditText newPass = (EditText) dView.findViewById(R.id.new_pass);
        final EditText confPass = (EditText) dView.findViewById(R.id.confirm_pass);

        builder.setView(dView)
                // Add action buttons
                .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
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
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String res = "Password not changed";
                if (oldPass.getText().toString().isEmpty()) {
                    oldPass.setError("No Password Entered");
                } else if (oldPass.getText().toString().equals(prefs.getString("password", ""))) {
                    oldPass.setError("Password Mismatch with Old Password");
                } else if (newPass.getText().toString().isEmpty()) {
                    newPass.setError("No Password Entered");
                } else if (!newPass.getText().toString().equals(confPass.getText().toString())) {
                    confPass.setError("Password Mismatch");
                } else {
                    JSONObject data = new JSONObject();
                    try {

                        data.accumulate("password", newPass.getText().toString());
                        res = new HttpRequestTask(data).execute(BASE_SERVER_URL + "updatepassword/" + userDetails.phone).get();

                        Toast.makeText(MainActivity.this, res, Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            getApplicationContext().startActivity(intent);
                            MainActivity.this.finish();
                    } catch (Exception e) {
                        Log.e("EXCEPTION", e.getMessage());
                    }
                }
            }
        });
    }

    public void createRatingDialog(String url, final int passenger){
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
                        new HttpRequestTask().execute(BASE_SERVER_URL +"rateuser/" + rideRequestId + "/" + coPassengers.get(passenger).id
                                + "/" + rating.getRating());
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
        dialog.setTitle("Rate Co-Passengers");
        de.hdodenhof.circleimageview.CircleImageView pic = (de.hdodenhof.circleimageview.CircleImageView)
                dView.findViewById(R.id.co_passenger_dp);
        new DownloadImageTask(pic).execute(url).execute();
        dialog.show();
    }

    public void driverRatingDialog(String url){
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
        dialog.setTitle("Rate Driver");
        de.hdodenhof.circleimageview.CircleImageView pic = (de.hdodenhof.circleimageview.CircleImageView)
                dView.findViewById(R.id.co_passenger_dp);
        new DownloadImageTask(pic).execute(url).execute();
        dialog.show();
    }

    public void createRideSummaryDialog(JSONObject rideSummaryData){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View dView = inflater.inflate(R.layout.ride_summary_dialog, null);
        builder.setView(dView);
        final RatingBar rating = (RatingBar) dView.findViewById(R.id.rating_bar);
        // Set other dialog properties
        // Create the AlertDialog
        final AlertDialog dialog = builder.create();
        try{
            TextView sourceAdd = (TextView) dView.findViewById(R.id.source);
            sourceAdd.setText(rideSummaryData.getString("origin"));

            TextView destinationAdd = (TextView) dView.findViewById(R.id.destination);
            destinationAdd.setText(rideSummaryData.getString("destination"));

            TextView distance = (TextView) dView.findViewById(R.id.distance);
            distance.setText("" + rideSummaryData.getDouble("distancecoveredkms") + " km");

            TextView duration = (TextView) dView.findViewById(R.id.duration);
            distance.setText("" + rideSummaryData.getDouble("timetakenmins") + " min");

            TextView tolltax = (TextView) dView.findViewById(R.id.tolltax);
            distance.setText("Rs " + ((double) Math.round(rideSummaryData.getDouble("tolltax")* 100) / 100));

            Double totalFare =rideSummaryData.getDouble("totaldistancefare") + rideSummaryData.getDouble("totaltimefare") + rideSummaryData.getDouble("tolltax");
            TextView fare = (TextView) dView.findViewById(R.id.fare);
            distance.setText("Rs " + ((double) Math.round(totalFare * 100) / 100));

        }catch(Exception e){
            Log.d("EXCEPTION", e.getMessage());
        }

        Button dialogClose = (Button) dView.findViewById(R.id.ride_summary_close);
        dialogClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(rating.getRating() != 0)
                    new HttpRequestTask().execute(BASE_SERVER_URL +"ratedriver/" + rideRequestId + "/" + rating.getRating());
                dialog.dismiss();
                //Todo show the pending rating dialog to user
            }
        });
        dialog.show();
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

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void showCabRates(View view){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View dView = inflater.inflate(R.layout.popup_layout, null);
        builder.setView(dView);

        final AlertDialog dialog = builder.create();

        Button dialogClose = (Button) dView.findViewById(R.id.close_popup);
        dialogClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                //Todo show the pending rating dialog to user
            }
        });
        dialog.show();
    }

    public void sendMessageSource(View view) {

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
            for (int i = 0; i < 5; i++) {
                if (prefs.contains(str[i])) {
                    try {
                        JSONObject jsonObject = new JSONObject(prefs.getString(str[i], null));
                        if (jsonObject != null) intent.putExtra(str[i], jsonObject.toString());
                    } catch (JSONException e) {
                    }
                }
            }
            startActivityForResult(intent, 2);
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
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_CANCELED) {
            Bundle bundle = data.getExtras();

            Double latitude = Double.parseDouble(bundle.getString("latitude"));
            Double longitude = Double.parseDouble(bundle.getString("longitude"));
            IconGenerator iconFactory = new IconGenerator(this);

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

    private void setStateAwaitingRide(){

        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mGoogleMap = fragment.getMap();
        mGoogleMap.setOnMarkerDragListener(this);
        mGoogleMap.setOnMapClickListener(this);
        mGoogleMap.setMyLocationEnabled(false);

        awaitingRide = true;
        inRide = false;
        ratingPending = false;

        Fragment displayFragment = new FragmentMain();
        FragmentManager fm = getSupportFragmentManager();

        Bundle args = new Bundle();
        args.putBoolean("awaitingride", awaitingRide);
        args.putBoolean("inRide", inRide);
        args.putBoolean("ratindpending", ratingPending);
        args.putString("drivername",driverDetails.name);
        args.putString("driverpic",driverDetails.picURL);
        args.putString("drivermobile",driverDetails.phone);
        args.putString("carno",carDetails.regNumber);
        args.putString("carmodel",carDetails.model + " " + carDetails.make);


        displayFragment.setArguments(args);

        fm.beginTransaction().replace(R.id.content_frame,displayFragment).commit();
        fm.executePendingTransactions();
        setTitle("Awaiting Ride");

        initializeTimerTask();
        startTimer();
    }

    private void setStateInRide() throws ExecutionException, InterruptedException {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        stoptimertask();
        cabMarker.remove();

        awaitingRide = false;
        inRide = true;
        ratingPending = false;

        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mGoogleMap = fragment.getMap();
        mGoogleMap.setOnMarkerDragListener(this);
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
            locationManager.requestLocationUpdates(provider, 20000, 0, this);
            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(currentLocationCoordinate, 15);
            mGoogleMap.animateCamera(yourLocation);
        }

        //Getting copassenger details
        getCopassengerInfo();

        Fragment displayFragment = new FragmentMain();
        FragmentManager fm = getSupportFragmentManager();

        Bundle args = new Bundle();
        args.putBoolean("awaitingride", awaitingRide);
        args.putBoolean("inRide", inRide);
        args.putBoolean("ratindpending", ratingPending);
        args.putInt("numofcopassengers", coPassengers.size());

        for(int i = 0; i < coPassengers.size(); i++){
           args.putString("url"+i,coPassengers.get(i).picURL);
        }

        displayFragment.setArguments(args);

        fm.beginTransaction().replace(R.id.content_frame,displayFragment).commit();
        fm.executePendingTransactions();
        setTitle("In Ride");
    }

    private void getCopassengerInfo() throws ExecutionException, InterruptedException {
        HttpRequestTask coPassengerInfoTask = new HttpRequestTask();

        Log.d("CODE_FLOW", "REQUEST FOR CO PASSENGER INFO");
        String res = coPassengerInfoTask.execute(BASE_SERVER_URL+"copassengerinfo/"+rideRequestId).get();
        JSONObject jObject;
        try{
            jObject = new JSONObject(res);
            Log.d("DATA_CO_PASSENGER_INFO", res);
            coPassengers = new ArrayList<CoPassenger>();

            if(jObject.getInt("riderequestid1") != 0)
                coPassengers.add(new CoPassenger(jObject.getString("CoPassenger1name"),jObject.getString("CoPassenger1picURL"),
                        jObject.getInt("riderequestid1")));

            if(jObject.getInt("riderequestid2") != 0)
                coPassengers.add(new CoPassenger(jObject.getString("CoPassenger2name"),jObject.getString("CoPassenger2picURL"),
                        jObject.getInt("riderequestid2")));

            if(jObject.getInt("riderequestid3") != 0)
                coPassengers.add(new CoPassenger(jObject.getString("CoPassenger3name"),jObject.getString("CoPassenger3picURL"),
                        jObject.getInt("riderequestid3")));

            if(jObject.getInt("riderequestid4") != 0)
                coPassengers.add(new CoPassenger(jObject.getString("CoPassenger4name"),jObject.getString("CoPassenger4picURL"),
                        jObject.getInt("riderequestid4")));
            Log.d("CODE_FLOW", "CO PASSENGERS DATA RECEIVED");
        }catch(Exception e){
            Log.d("Exception", e.toString());
        }
        Log.d("CODE_FLOW", "RECEIVED CO PASSENGER INFO");
    }

    private void setStateBookRide(){
        inRide = false;
        awaitingRide = false;

        Fragment displayFragment = new FragmentMain();
        FragmentManager fm = getSupportFragmentManager();

        Bundle args = new Bundle();
        args.putBoolean("awaitingride", awaitingRide);
        args.putBoolean("inRide", inRide);
        args.putBoolean("ratindpending", ratingPending);

        displayFragment.setArguments(args);

        fm.beginTransaction().replace(R.id.content_frame,displayFragment).commit();
        fm.executePendingTransactions();

        setTitle("Book Ride");
        IconGenerator iconFactory = new IconGenerator(this);
        iconFactory.setColor(Color.GREEN);
        sourceMarker = addIcon(iconFactory, "PickUp", new LatLng(currentLocationCoordinate.latitude, currentLocationCoordinate.longitude));
        sourceMarker.isDraggable();

        CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(currentLocationCoordinate, 15);
        mGoogleMap.animateCamera(yourLocation);

    }

    public void requestBookRide(View view) throws ExecutionException, InterruptedException {
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
            float[] journeyDetails = getDistDurInfoTask.googleDistanceDurationInfo();


            minFare = (int) Math.max(Math.ceil(3.5 * journeyDetails[0]
                    + 0.5 * journeyDetails[1]), minFare);
            maxFare = (int) Math.max(Math.ceil(10 * journeyDetails[0]
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

                            CabBookingTask bookCab = new CabBookingTask();
                            try {
                                String res = bookCab.execute("http://www.hoi.co.in/api/createriderequest").get();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            }
                            if(sourceMarker!= null) sourceMarker.remove();
                            if(destinationMarker!= null) destinationMarker.remove();
                            setStateAwaitingRide();
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
            fare.setText("FARE : " + minFare + " to " + maxFare);
            dialog.show();
        } else {

            Toast.makeText(getApplicationContext(), "PickUp or Drop Location not set!!",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void requestCancelRide(View view) throws ExecutionException, InterruptedException, JSONException, ParseException {

        float cost = BASECHARGE;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date currentTime = sdf.parse(sdf.format(new Date()));
        Date bookingTime = sdf.parse((currentRideDetails.bookingTime));
        long difference = currentTime.getTime() - bookingTime.getTime();
        int min = (int) (difference) / (1000*60);

        DistanceDurationInfo getDistDurInfoTask = new DistanceDurationInfo(currentRideDetails.sourceCoordinates, cabLatestCoordinate);
        float[] distdurInfo = getDistDurInfoTask.defaultDistanceDurationInfo();

        if (min < 5.0)
            cost = 0;

        if (distdurInfo[1] < 10)
            cost = 100;

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

                        HttpRequestTask cancelRide = new HttpRequestTask();
                        try {
                            String cancelRideResult = cancelRide.execute("http://www.hoi.co.in/api/cancelride/" + rideRequestId).get();
                            System.out.println("Cancel Ride Details" + cancelRideResult);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                        stoptimertask();
                        setStateBookRide();
                        cabMarker.remove();
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
        tvCost.setText(getResources().getString(R.string.Rs) + " " + String.valueOf(cost));
        dialog.show();
    }

    /*
     * Navigation Drawer related funtions
     * dataList.add(new DrawerItem("Settings", R.drawable.ic_action_settings));
     */

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

        // Getting latitude of the current location
        double latitude = location.getLatitude();

        // Getting longitude of the current location
        double longitude = location.getLongitude();

        if(inRide){
            float segmentDistance = calculateDistance(currentLocationCoordinate.latitude, currentLocationCoordinate.longitude,
                    latitude, longitude);
            int segmentDuration = (int) calculateTime(currentLocationCoordinate.latitude, currentLocationCoordinate.longitude,
                    latitude, longitude);

            currentRideDetails.distanceIncurred += segmentDistance;
            currentRideDetails.timeIncurred += segmentDuration;
            currentRideDetails.costIncurred += billingRate * segmentDistance + 0.5f * segmentDuration;

            // Creating a LatLng object for the current location
            currentLocationCoordinate = new LatLng(latitude, longitude);

            UpdateRideTask updateRide = new UpdateRideTask();
            updateRide.execute("http://www.hoi.co.in/api/update/"+rideRequestId);

            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(currentLocationCoordinate, 15);
            mGoogleMap.animateCamera(yourLocation);
        }
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
	public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {}

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
                        HttpRequestTask trackRide = new HttpRequestTask();
                        try {
                            String res = trackRide.execute("http://www.hoi.co.in/api/track/"+rideRequestId).get();
                            System.out.println(res);
                            JSONObject jObject = new JSONObject(res);
                            cabLatestCoordinate = new LatLng(jObject.getDouble("ridelastlat"),jObject.getDouble("ridelastlong"));
                            IconGenerator iconFactory = new IconGenerator(MainActivity.this);
                            iconFactory.setColor(Color.YELLOW);
                            cabMarker = addIcon(iconFactory, "HOI CAB", cabLatestCoordinate);

                            CameraUpdate cabLocation = CameraUpdateFactory.newLatLngZoom(cabLatestCoordinate, 15);
                            mGoogleMap.animateCamera(cabLocation);

                            if(jObject.getBoolean("rideunderway") == true){
                                setStateInRide();

                                StartRideTask startRide = new StartRideTask(authenticationHeader);
                                String str_result = startRide.execute("http://www.hoi.co.in/api/startride/"+rideRequestId).get();
                                stoptimertask();

                            }else if(jObject.getBoolean("ridecancelled") == true){
                                stoptimertask();
                                setStateBookRide();
                            }
                        } catch (Exception e) {
                            Log.e("EXCEPTION", e.getMessage());
                        }
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
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

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
     * Wallet Fragment
     */

    private void setUpWallet(){
        Fragment displayFragment = new FragmentWallet();
        FragmentManager fm = getSupportFragmentManager();

        Bundle args = new Bundle();
        args.putString("authenticationheader", authenticationHeader);
        args.putDouble("balance",balance);
        displayFragment.setArguments(args);

        fm.beginTransaction().replace(R.id.content_frame,displayFragment).commit();
        setTitle("Wallet");
    }

    /*
     * Contact Fragment
     */

    public void makecall(View view) {
        String uri = "tel:" + "00919654965311".trim();
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse(uri));
        startActivity(intent);
    }

    public void makeemail(View view) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@hoi.co.in"});
        i.putExtra(Intent.EXTRA_SUBJECT, "reg: Hoi Cabs");

        try {
            startActivity(Intent.createChooser(i, "Send mail..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setUpContact() {
        Fragment displayFragment = new FragmentContact();
        FragmentManager fm = getSupportFragmentManager();

        fm.beginTransaction().replace(R.id.content_frame,displayFragment).commit();
        fm.executePendingTransactions();
        setTitle("Contact Us");
    }

    private void startProcessDialog(String message){
        //Process running dialog
        processDialog = new ProgressDialog(MainActivity.this);
        processDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        processDialog.setMessage(message);
        processDialog.setIndeterminate(true);
        processDialog.setCanceledOnTouchOutside(false);
        processDialog.show();
    }

    private void dismissProcessDialog(){
        processDialog.dismiss();
    }

    /*
     * Represents an asynchronous cab booking task used to authenticate
     * the ride.
     */

    private class CabBookingTask extends AsyncTask<String, Void, String> {

        private HttpResponse response;
        private JSONObject cabBookingData;

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
                request.addHeader("Authorization", "Basic " + authenticationHeader);
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
                cabBookingData.accumulate("requestdatetime", currentDateandTime);

                // 4. convert JSONObject to JSON to String
                json = cabBookingData.toString();

                // 5. set json to StringEntity

                request.setEntity(new StringEntity(json));
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
            JSONObject jObject;
            try {
                jObject = new JSONObject(result);
                currentRideDetails = new DetailCurrentRide(cabBookingData, jObject);
                carDetails = new DetailCar(jObject.getJSONObject("carinfo"));
                driverDetails = new DetailDriver(jObject);
                cabLatestCoordinate = new LatLng(jObject.getDouble("ridelastlat"), jObject.getDouble("ridelastlong"));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(final String data) {
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
                    //Todo show current location
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

        private HttpResponse response;

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
                request.addHeader("androidkey",encrptedkey);
                request.addHeader("Content-Type", "application/json");

                String json = "";

                // 3. build jsonObject
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("ridelastlat", currentLocationCoordinate.latitude);
                jsonObject.accumulate("ridelastlonglong", currentLocationCoordinate.longitude);
                jsonObject.accumulate("requestdatetime", currentDateandTime);
                jsonObject.accumulate("estimateddistance", currentRideDetails.distanceIncurred);
                jsonObject.accumulate("estimatedtime", currentRideDetails.timeIncurred);
                jsonObject.accumulate("estimatedfare", currentRideDetails.costIncurred);

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

                // 5. set json to StringEntity

                request.setEntity(new StringEntity(json));
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
        protected void onPostExecute(final String data) {
            JSONObject jObject;

            if (data != null) {

                //Todo if the ride has been closed
                try{
                    jObject = new JSONObject(data);
                    if (jObject.getBoolean("rideratingawaited")) {
                        //Todo show the bill summary
                        setStateBookRide();
                        String res = (new HttpRequestTask()).execute(BASE_SERVER_URL+"closeride/"+rideRequestId).get();
                        try {
                            JSONObject rideSummary = new JSONObject(res);
                            //Todo show account summary
                            createRideSummaryDialog(rideSummary);
                        } catch (JSONException e) {
                            Log.e("EXCEPTION",e.getMessage());
                        }
                        //Todo rating of pending copassengers and driver
                    }
                    else {
                        cabLatestCoordinate = new LatLng(jObject.getDouble("ridelastlat"), jObject.getDouble("ridelastlong"));
                        billingRate = (float) jObject.getDouble("billingrate");
                    }
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

    private class HttpRequestTask extends AsyncTask<String, Void, String> {

        private HttpResponse response;
        JSONObject data;

        HttpRequestTask(JSONObject data){
            this.data = data;
        }

        HttpRequestTask(){
            this.data = new JSONObject();
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

                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
                nameValuePairs.add(new BasicNameValuePair("counter", currentDateandTime));
                data.accumulate("counter",currentDateandTime);

                request.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                if(data != null){
                    request.addHeader("Content-Type", "application/json");
                    request.setEntity(new StringEntity(data.toString()));
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
        protected void onPostExecute(final String data) {
        }

        @Override
        protected void onCancelled() {

        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }
}



