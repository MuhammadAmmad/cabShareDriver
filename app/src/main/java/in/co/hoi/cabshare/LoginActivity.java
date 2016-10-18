package in.co.hoi.cabshare;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.maps.model.LatLng;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends Activity implements LoaderCallbacks<Cursor> {

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    ObscuredSharedPreferences prefs;
    private String authenticationHeader;
    JSONObject facebookLoginDetails;
    private int numberOfFriends = -1;
    private LatLng currentLoc;
    ProgressDialog processDialog;

    private UserLoginTask mAuthTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = ObscuredSharedPreferences.getPrefs(this, "HoiCabDrivers", Context.MODE_PRIVATE);
        setContentView(R.layout.activity_login);

        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);
        ckeckForServices();

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if ((id == R.id.login) || (id == EditorInfo.IME_NULL)) {
                    try {
                        attemptLogin();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    attemptLogin();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void ckeckForServices() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.d("LOCATION MANAGER", "GPS Not Enabled");
            Intent callGPSSettingIntent = new Intent(
                    android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(callGPSSettingIntent);
            super.finish();
        }
        else {
            String email = prefs.getString("username", null);
            String password = prefs.getString("password", null);
            if(email!="" && password != ""){
                mEmailView.setText(email);
                mPasswordView.setText(password);
                Location location = getLastKnownLocation(locationManager);
                if(location != null) {
                    currentLoc = new LatLng(location.getLatitude(), location.getLongitude());
                    autoLogin();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Login Failed", Toast.LENGTH_SHORT).show();
                }

            }
        }
    }

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


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() throws ExecutionException, InterruptedException {

        if (mAuthTask != null) {
            return;
        }
        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);
        }
    }

    public void autoLogin() {

        if (mAuthTask != null) {
            return;
        }
        // Store values at the time of the login attempt.
        String email = prefs.getString("username", null);
        String password = prefs.getString("password", null);

        mEmailView.setText(email);
        mPasswordView.setText(password);


        if (email != null && password != null) {
            if (isEmailValid(email) && isPasswordValid(password)) {
                mAuthTask = new UserLoginTask(email, password);
                mAuthTask.execute((Void) null);
            }
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Grow the logic further to check for corporate emails
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        return (password.length() > 4);
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<String>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, String> {

        private final String mEmail;
        private final String mPassword;
        private HttpResponse response;
        JSONObject jObject;
        ProgressDialog asyncDialog = new ProgressDialog(LoginActivity.this);

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected String doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm");
            String currentDateandTime = sdf.format(new Date());
            Encryptor encryptor = new Encryptor();
            String encrptedkey = "";

            try {
                encrptedkey = encryptor.getEncryptedKeyValue(currentDateandTime);
            } catch (Exception e) {
                e.printStackTrace();
            }
            jObject = new JSONObject();
            try {
                HttpPost request = new HttpPost("http://www.hoi.co.in/dapi/check");
                String credentials = mEmail + ":" + mPassword;
                String base64EncodedCredentials = authenticationHeader = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                request.addHeader("Authorization", "Basic " + base64EncodedCredentials);
                request.addHeader("androidkey", encrptedkey);
                request.addHeader("Content-Type", "application/json");

                if(currentLoc == null){
                    return "nolocation";
                }else{
                    jObject.accumulate("ridelastlong", Math.round(currentLoc.longitude*10000.0)/10000.0);
                    jObject.accumulate("ridelastlat", Math.round(currentLoc.latitude*10000.0)/10000.0);
                }

                jObject.accumulate("requestdatetime", currentDateandTime);
                jObject.accumulate("estimateddistance", 0.0f);
                jObject.accumulate("estimatedtime", 0);
                jObject.accumulate("estimatedfare", 0);
                jObject.accumulate("state", "");

                request.setEntity(new StringEntity(jObject.toString()));
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
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
                result = sb.toString();
            } catch (Exception e) {
                //Log.d("Exception: ", e.getMessage());
                result = "failed";
            } finally {
                try {
                    if (inputStream != null) inputStream.close();
                } catch (Exception squish) {
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(String data) {
            mAuthTask = null;
            if (data != null && !data.equals("failed")) {
                try {
                    jObject = new JSONObject(data);
                    prefs.edit().clear().commit();
                    prefs.edit().putString("username", mEmail).commit();
                    prefs.edit().putString("password", mPassword).commit();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("userData", data);
                    intent.putExtra("authenticationHeader", authenticationHeader);


                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getApplicationContext().startActivity(intent);
                    LoginActivity.this.finish();
                } catch (JSONException e) {
                    mPasswordView.setError(getString(R.string.error_incorrect_password));
                    e.printStackTrace();
                }

            }
            else if(data != null && data.equals("nolocation")){
                Toast.makeText(getApplicationContext(),"Couldn't find current location", Toast.LENGTH_SHORT).show();
            }else {
                if (!mPasswordView.getText().toString().isEmpty()) {
                    mPasswordView.setError(getString(R.string.error_incorrect_password));
                    mPasswordView.requestFocus();
                }
                Toast.makeText(getApplicationContext(),"Login Failed", Toast.LENGTH_SHORT).show();
            }

        super.onPostExecute(data);
    }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
        }


    }
}