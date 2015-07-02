package in.co.hoi.cabshare;

import android.util.Log;

import org.json.JSONObject;

/**
 * Created by Ujjwal on 24-06-2015.
 */
public class DetailDriver {

    String name;
    String picURL;
    String phone;
    String backgroundCheck;

    DetailDriver(JSONObject driverData){
        try{
            name = driverData.getString("drivername");
            picURL = driverData.getString("driverpic");
            phone = driverData.getString("driverphone");
            backgroundCheck = driverData.getString("driverbgc");
        }catch(Exception e){
            Log.d("Exception", "Driver Data");
        }
    }
}
