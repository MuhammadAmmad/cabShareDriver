package in.co.hoi.cabshare;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Ujjwal on 27-06-2015.
 */
public class FragmentMain extends Fragment {

    boolean inRide;
    boolean awaitingRide;
    boolean ratingPending;
    int numOfcoPassengers;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = null;
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(220, 220);

        de.hdodenhof.circleimageview.CircleImageView c1,c2,c3,c4;
        c1 = new CircleImageView(getActivity().getApplicationContext());
        c1.setLayoutParams(layoutParams);
        c1.setImageResource(R.drawable.male_user);
        c2 = new CircleImageView(getActivity().getApplicationContext());
        c2.setLayoutParams(layoutParams);
        c2.setImageResource(R.drawable.male_user);
        c3 = new CircleImageView(getActivity().getApplicationContext());
        c3.setLayoutParams(layoutParams);
        c3.setImageResource(R.drawable.male_user);
        c4 = new CircleImageView(getActivity().getApplicationContext());
        c4.setLayoutParams(layoutParams);
        c4.setImageResource(R.drawable.male_user);

        inRide = getArguments().getBoolean("inride");
        awaitingRide = getArguments().getBoolean("awaitingride");
        ratingPending = getArguments().getBoolean("ratingpending");

        if(inRide){
            view = inflater.inflate(R.layout.fragment_inride, container, false);
            HorizontalScrollView horizontalScrollView = (HorizontalScrollView)view.findViewById(R.id.horizontalScrollView);

            numOfcoPassengers = getArguments().getInt("numofcopassengers");
            if(numOfcoPassengers >= 1){
                c1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((MainActivity)getActivity()).createRatingDialog(getArguments().getString("url1"));
                    }
                });
                new DownloadImageTask(c1).execute(getArguments().getString("url1"));
            }

            if(numOfcoPassengers >= 2){
                c2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((MainActivity)getActivity()).createRatingDialog(getArguments().getString("url2"));
                    }
                });
                new DownloadImageTask(c2).execute(getArguments().getString("url2"));
            }

            if(numOfcoPassengers >= 3){
                c3.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((MainActivity)getActivity()).createRatingDialog(getArguments().getString("url3"));
                    }
                });
                new DownloadImageTask(c3).execute(getArguments().getString("url3"));
            }

            if(numOfcoPassengers == 4){
                c4.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((MainActivity)getActivity()).createRatingDialog(getArguments().getString("url4"));
                    }
                });
                new DownloadImageTask(c4).execute(getArguments().getString("url4"));
            }

            de.hdodenhof.circleimageview.CircleImageView cUnknown = new CircleImageView(getActivity().getApplicationContext());
            cUnknown.setLayoutParams(layoutParams);
            cUnknown.setImageResource(R.drawable.male_user);
            cUnknown.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Todo report unknnown passenger in ride
                }
            });

            LinearLayout hv = (LinearLayout) view.findViewById(R.id.pasengerlist);
            hv.addView(c1);
            hv.addView(c2);
            hv.addView(c3);
            hv.addView(c4);
            hv.addView(cUnknown);

            Log.d("CODE_FLOW", "Horizontal View Created");
        }
        else if(awaitingRide){
            //Todo put driver details
            view = inflater.inflate(R.layout.fragment_awatingride, container, false);
        }
        else {
            view = inflater.inflate(R.layout.fragment_bookingride, container, false);
            if(ratingPending){
                //Todo rating pending
            }
        }
        return view;
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
