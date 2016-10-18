package in.co.hoi.cabshare;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

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
import org.w3c.dom.Text;

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

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Ujjwal on 27-06-2015.
 */
public class FragmentMain extends Fragment {

    private NextPassenger nextPassenger;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = null;

        nextPassenger = ((MainActivity)getActivity()).getNextPassenger();
        if (nextPassenger != null && nextPassenger.getId() != 0) {
            if (nextPassenger.isWaiting()) {
                view = inflater.inflate(R.layout.fragment_main_pickup, container, false);
                Button startRide = (Button) view.findViewById(R.id.startRide);
                boolean arrived = ((MainActivity)getActivity()).getArrivedState();
                if(arrived)
                    startRide.setText(getResources().getText(R.string.start_ride));
                else
                    startRide.setText(getResources().getText(R.string.arrive_ride));
            } else {
                view = inflater.inflate(R.layout.fragment_main_drop, container, false);
            }
            de.hdodenhof.circleimageview.CircleImageView passengerPic = (de.hdodenhof.circleimageview.CircleImageView)
                    view.findViewById(R.id.passengerPic);
            passengerPic.setImageBitmap(nextPassenger.getImage());
        }
        else{
            view = inflater.inflate(R.layout.fragment_main, container, false);
        }

        return view;
    }

    public void changeStatetoArrived(){
        Button startRide = (Button) getView().findViewById(R.id.startRide);
        startRide.setText(getResources().getText(R.string.start_ride));
    }

}
