package in.co.hoi.cabshare;

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

/**
 * Created by Ujjwal on 8/21/2015.
 */

public class PendingPayListAdapter  extends BaseAdapter{



    /*********** Declare Used Variables *********/
    private Activity activity;
    private List<PendingPayItem> data;
    private static LayoutInflater inflater=null;
    public Resources res;
    PendingPayItem tempValues=null;
    int i=0;
    String authenticationHeader;

    /*************  CustomAdapter Constructor *****************/
    public PendingPayListAdapter(Activity a, List<PendingPayItem> d,Resources resLocal) {

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
    public PendingPayItem getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return data.get(position).getId();
    }

    /********* Create a holder Class to contain inflated xml file elements *********/
    public static class ViewHolder{
        public TextView idTV;
        public TextView amountTV;
        public TextView statusTV;
        public TextView genIdTV;
        public TextView dateTV;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        View v  = convertView;
        final ViewHolder holder;

        if(convertView==null){

            /****** Inflate tabitem.xml file for each row ( Defined below ) *******/
            v = inflater.inflate(R.layout.pending_pay_list_item, null);

            /****** View Holder Object to contain tabitem.xml file elements ******/

            holder = new ViewHolder();
            holder.idTV = (TextView) v.findViewById(R.id.payId);
            holder.genIdTV = (TextView) v.findViewById(R.id.genId);
            holder.amountTV = (TextView) v.findViewById(R.id.amount);
            holder.dateTV = (TextView) v.findViewById(R.id.date);
            holder.statusTV = (TextView) v.findViewById(R.id.status);

            /************  Set holder with LayoutInflater ************/
            v.setTag( holder );
        }
        else
            holder=(ViewHolder)v.getTag();

        if(data.size()>0) {
            /***** Get each Model object from Arraylist ********/
            tempValues = null;
            tempValues = (PendingPayItem) data.get(position);

            /************  Set Model values in Holder elements ***********/
            holder.idTV.setText("#"+tempValues.getId());
            holder.genIdTV.setText("ID:"+tempValues.getGenRideId());
            holder.dateTV.setText(tempValues.getDateTime());
            holder.statusTV.setText(tempValues.getStatus());
            holder.amountTV.setText("\u20B9 "+tempValues.getAmount());
        }
        return v;
    }

}


