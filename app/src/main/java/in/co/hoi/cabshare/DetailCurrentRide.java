package in.co.hoi.cabshare;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

/**
 * Created by Ujjwal on 24-06-2015.
 */
public class DetailCurrentRide {

    String bookingTime;
    String startTime;
    String lastResponceTime;
    LatLng sourceCoordinates;
    LatLng destinationCoordinates;
    Double distanceIncurred;
    Double timeIncurred;
    Double waitingMins;
    Double refundAmount;
    Double tollTax;
    Double costIncurred;
    LatLng cabLastCoordinates;

    public DetailCurrentRide(JSONObject rideInfo){
        try {
            bookingTime = rideInfo.getString("bookingtime");
            sourceCoordinates = new LatLng(rideInfo.getDouble("originx"),rideInfo.getDouble("originy"));
            destinationCoordinates = new LatLng(rideInfo.getDouble("corx"),rideInfo.getDouble("cory"));

            cabLastCoordinates = new LatLng(rideInfo.getDouble("lastlat"),rideInfo.getDouble("lastlong"));

            startTime = rideInfo.getString("starttime");
            distanceIncurred = rideInfo.getDouble("distanceincurred");
            costIncurred = rideInfo.getDouble("costincurred");
            timeIncurred = rideInfo.getDouble("timeincurred");

            lastResponceTime = rideInfo.getString("lastresponcetime");
            waitingMins = rideInfo.getDouble("waitingmins");
            refundAmount = rideInfo.getDouble("refundamount");
            tollTax = rideInfo.getDouble("tolltax");
        }
        catch(Exception e){
            Log.d("Exception", "Car Details");
        }
    }

    public DetailCurrentRide(JSONObject cabBookingDetails, JSONObject cabBookingResponce){
        try {
            bookingTime = cabBookingDetails.getString("requestdatetime");
            sourceCoordinates = new LatLng(cabBookingDetails.getDouble("origin_latitude"),cabBookingDetails.getDouble("origin_longitude"));
            destinationCoordinates = new LatLng(cabBookingDetails.getDouble("destination_latitude"),cabBookingDetails.getDouble("destination_longitude"));

            cabLastCoordinates = new LatLng(cabBookingResponce.getDouble("ridelastlat"),cabBookingResponce.getDouble("ridelastlong"));
            startTime = "";
            distanceIncurred = 0.0;
            costIncurred = 0.0;
            timeIncurred = 0.0;

            lastResponceTime = "";
            waitingMins = 0.0;
            refundAmount = 0.0;
            tollTax = 0.0;
        }
        catch(Exception e){
            Log.d("Exception", "Car Details");
        }
    }


}

