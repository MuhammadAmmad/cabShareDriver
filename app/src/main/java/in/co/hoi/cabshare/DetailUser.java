package in.co.hoi.cabshare;

import android.util.Log;

import org.json.JSONObject;

/**
 * Created by Ujjwal on 24-06-2015.
 */
public class DetailUser {

    String name;
    String username;
    String phone;
    String displayPic;
    Double availableINR;

    DetailUser(JSONObject userData){
        try{
            name = userData.getString("name");
            username = userData.getString("username");
            phone = userData.getString("phone");
            displayPic = userData.getString("displaypic");
            availableINR = userData.getDouble("availableinr");
        }catch(Exception e){
            Log.d("Exception", "User Data");
        }
    }

}
