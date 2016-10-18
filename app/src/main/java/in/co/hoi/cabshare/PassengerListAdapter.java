package in.co.hoi.cabshare;

/**
 * Created by Ujjwal on 8/6/2015.
 */

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

public class PassengerListAdapter extends BaseAdapter {

    /*********** Declare Used Variables *********/
    private Activity activity;
    private List<Passenger> data;
    private static LayoutInflater inflater=null;
    public Resources res;
    Passenger tempValues=null;
    int i=0;
    String authenticationHeader;

    /*************  CustomAdapter Constructor *****************/
    public PassengerListAdapter(Activity a, List<Passenger> d,Resources resLocal) {

        /********** Take passed values **********/
        activity = a;
        data=d;
        res = resLocal;

        /***********  Layout inflator to call external xml layout () ***********/
        inflater = ( LayoutInflater )activity.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    @Override
    public int getCount() {
        if(data.size()<=0)
            return 1;
        return data.size();
    }

    @Override
    public Passenger getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return data.get(position).getId();
    }

    /********* Create a holder Class to contain inflated xml file elements *********/
    public static class ViewHolder{
        //public TextView name;
        public ImageView passengerPic;
        public Button stopRide;
        public int id;
        public LinearLayout passengerLayout;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        View v  = convertView;
        final ViewHolder holder;

        if(convertView==null){

            /****** Inflate tabitem.xml file for each row ( Defined below ) *******/
            v = inflater.inflate(R.layout.listitem, null);

            /****** View Holder Object to contain tabitem.xml file elements ******/

            holder = new ViewHolder();
            holder.passengerPic=(ImageView)v.findViewById(R.id.passenger_pic);
            holder.stopRide=(Button)v.findViewById(R.id.stop_ride);
            holder.passengerLayout=(LinearLayout)v.findViewById(R.id.pic_layout);

            /************  Set holder with LayoutInflater ************/
            v.setTag( holder );
        }
        else
            holder=(ViewHolder)v.getTag();

        if(data.size()>0) {
            /***** Get each Model object from Arraylist ********/
            tempValues = null;
            tempValues = (Passenger) data.get(position);

            /************  Set Model values in Holder elements ***********/

            if(position % 2 != 0) holder.passengerLayout.setBackgroundColor(R.color.grey);
            holder.passengerPic.setImageBitmap(tempValues.passengerpic);
            holder.id = tempValues.getId();
            holder.stopRide.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        ((MainActivity)activity).createRatingDialog(holder.id);
                    } catch (Exception e) {
                        Toast.makeText(activity.getApplicationContext(), "Stop Ride Failed", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
        return v;
    }

}

