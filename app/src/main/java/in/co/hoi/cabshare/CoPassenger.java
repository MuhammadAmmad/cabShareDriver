package in.co.hoi.cabshare;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.concurrent.ExecutionException;

/**
 * Created by Ujjwal on 24-06-2015.
 */
public class CoPassenger {
    String name;
    String picURL;
    int id;
    Bitmap pic;

    CoPassenger(String name, String url, int id) throws ExecutionException, InterruptedException {
        this.name = name;
        picURL = url;
        this.id = id;
        pic = new DownloadImageTask().execute(picURL).get();
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
