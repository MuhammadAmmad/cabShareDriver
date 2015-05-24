package in.co.hoi.cabshare;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by lenovo pc on 20-05-2015.
 */
public class UserDataParser {


    public HashMap<String,String> parse(JSONObject jObject){

        HashMap<String, String> userData = new HashMap<String, String>();
        try {
            userData.put("code", Integer.toString(jObject.getInt("code")));
            userData.put("status", jObject.getString("status"));
            userData.put("username", jObject.getString("username"));
            userData.put("name", jObject.getString("name"));
            userData.put("phone", jObject.getString("phone"));
            userData.put("displaypic", jObject.getString("displaypic"));
            userData.put("availableinr", Double.toString(jObject.getDouble("availableinr")));
            userData.put("hastorateprevious", Boolean.toString(jObject.getBoolean("hastorateprevious")));
            userData.put("inaride", Boolean.toString(jObject.getBoolean("inaride")));
            userData.put("awaitingride", Boolean.toString(jObject.getBoolean("awaitingride")));
            userData.put("genriderequestid", Integer.toString(jObject.getInt("genriderequestid")));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        /** Returning hash map for the json object send as response to
         *  HttpPost request in LoginActivity
         */
        return userData;
    }

}
