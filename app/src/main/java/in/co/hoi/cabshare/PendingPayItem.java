package in.co.hoi.cabshare;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Ujjwal on 8/21/2015.
 */
public class PendingPayItem {
    private int id;
    private int driverId;
    private int genRideId;
    private int amount;
    private String status;
    private String dateTime;

    public PendingPayItem(JSONObject data) throws JSONException{
        if(data.has("id") && !data.isNull("id"))
            id = data.getInt("id");
        if(data.has("driverid") && !data.isNull("driverid"))
            driverId = data.getInt("driverid");
        if(data.has("genrideid") && !data.isNull("genrideid"))
            genRideId = data.getInt("genrideid");
        if(data.has("amount") && !data.isNull("amount"))
            amount = data.getInt("amount");
        if(data.has("datetime") && !data.isNull("datetime"))
            dateTime = data.getString("datetime");
        if(data.has("status") && !data.isNull("status"))
            status = data.getString("status");
    }

    public int getId(){
        return id;
    }

    public int getDriverId(){
        return driverId;
    }

    public int getGenRideId(){
        return genRideId;
    }

    public int getAmount(){
        return amount;
    }

    public String getStatus(){
        return status;
    }

    public String getDateTime(){
        return dateTime;
    }
}

