package in.co.hoi.cabshare;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.concurrent.ExecutionException;

/**
 * Created by Ujjwal on 8/6/2015.
 */
public class Passenger {
    String url;
    int id;
    boolean isWaiting;
    Bitmap passengerpic;

    public Passenger(String picUrl, int id, boolean isWaiting) throws ExecutionException, InterruptedException {
        this.url = picUrl;
        this.id = id;
        this.isWaiting = isWaiting;
        this.passengerpic = new DownloadImageTask().execute(url).get();

    }



    public int getId(){
        return id;
    }
    public boolean isPassengerWaiting(){
        return isWaiting;
    }
    public Bitmap getPassengerpic(){
        return passengerpic;
    }
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        public DownloadImageTask() {
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
        }
    }
}
