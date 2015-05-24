package in.co.hoi.cabshare;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by lenovo pc on 24-05-2015.
 */
public class RideDetailParser {


        public HashMap<String,String> parse(JSONObject jObject){

            HashMap<String, String> rideData = new HashMap<String, String>();
            try {
                JSONObject carInfo  = jObject.getJSONObject("carinfo");
                
                rideData.put("ridelastlong", Double.toString(jObject.getDouble("ridelastlong")));
                rideData.put("ridelastlat", Double.toString(jObject.getDouble("ridelastlat")));
                
                rideData.put("drivername", jObject.getString("drivername"));
                rideData.put("driverpic", jObject.getString("driverpic"));
                rideData.put("driverphone", jObject.getString("driverphone"));
                rideData.put("driverbgc", jObject.getString("driverbgc"));
                
                rideData.put("occupancy", Integer.toString(jObject.getInt("occupancy")));
                rideData.put("billingrate", Double.toString(jObject.getDouble("billingrate")));
                rideData.put("riderequestid", Integer.toString(jObject.getInt("riderequestid")));

                rideData.put("carid",Integer.toString(carInfo.getInt("id")));
                rideData.put("carregnumber",carInfo.getString("regnumber"));
                rideData.put("carmodel",carInfo.getString("model"));
                rideData.put("carmake",carInfo.getString("make"));
                rideData.put("carcolor",carInfo.getString("color"));

                
            } catch (JSONException e) {
                e.printStackTrace();
            }

            /** Returning hash map for the json object send as response to
             *  HttpPost request in LoginActivity
             */
            return rideData;
        }

    }