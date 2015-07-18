package in.co.hoi.cabshare;

import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;

/**
 * Created by Ujjwal on 31-05-2015.
 */
public class DistanceDurationInfo {

    private LatLng srcCoordinate;
    private LatLng dstCoordinate;
    private float[] distanceDurationInfo;

    public DistanceDurationInfo(LatLng src, LatLng dst){
        srcCoordinate = new LatLng(src.latitude, src.longitude);
        dstCoordinate = new LatLng(dst.latitude, dst.longitude);
        distanceDurationInfo = new float[2];
    }

    public float[] getDistanceDurationInfo(){
        return distanceDurationInfo;
    }

    public float[] googleDistanceDurationInfo() throws ExecutionException, InterruptedException {

        DirectionsApiTask distDurInfo = new DirectionsApiTask();

        String res = distDurInfo.execute().get();

        return distanceDurationInfo;
    }

    public float[] defaultDistanceDurationInfo(){
        double diffLat = Math.abs(srcCoordinate.latitude-dstCoordinate.latitude);
        double diffLong = Math.abs(srcCoordinate.longitude-dstCoordinate.longitude);
        distanceDurationInfo[0] = (float)(142.6 * Math.sqrt(diffLat*diffLat + diffLong*diffLong));
        distanceDurationInfo[1] = (float)(225.73 * Math.sqrt(diffLat*diffLat + diffLong*diffLong));
        return distanceDurationInfo;
    }


    /*
        * Asynchronous task to get the estimated distance and duration using
        * Google directions API
    */
    private class DirectionsApiTask extends AsyncTask<Void, Void, String> {

        private HttpResponse response;
        StringBuilder stringBuilder = new StringBuilder();
        private String mKey = "key=AIzaSyBrl-RRzrwGisYJpFI2QhpcCeknXg_bSmw";


        @Override
        protected String doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.
            try {

                String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + srcCoordinate.latitude + "," +
                       srcCoordinate.longitude + "&destination=" + dstCoordinate.latitude + "," + dstCoordinate.longitude +
                       "&mode=driving&sensor=false&units=metric"+"&"+mKey;

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
                try{if(inputStream != null)inputStream.close();}catch(Exception squish){}
            }

            if (result != null) {

                JSONObject jsonObject = new JSONObject();
                try {

                    jsonObject = new JSONObject(result);

                    JSONArray array = jsonObject.getJSONArray("routes");

                    JSONObject routes = array.getJSONObject(0);

                    JSONArray legs = routes.getJSONArray("legs");

                    JSONObject steps = legs.getJSONObject(0);

                    JSONObject distance = steps.getJSONObject("distance");

                    JSONObject duration = steps.getJSONObject("duration");

                    distanceDurationInfo[0] = Integer.parseInt(distance.getString("value")) / 1000;
                    distanceDurationInfo[1] = Integer.parseInt(duration.getString("value")) / 60;

                    System.out.println(distanceDurationInfo[0] + " " + distanceDurationInfo[1]);

                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
            else{
                //Todo when no data comes from Distance matrix api
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
}
