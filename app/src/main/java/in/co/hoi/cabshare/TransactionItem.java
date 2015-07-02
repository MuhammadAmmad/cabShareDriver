package in.co.hoi.cabshare;

import android.util.Log;

import org.json.JSONObject;

/**
 * Created by Ujjwal on 28-06-2015.
 */
public class TransactionItem {
    int id;
    double amount;
    String status;
    int userId;
    String debitType;
    int genRideRequestId;
    int rideRequestId;
    int cancellationId;
    String recordDateTime;

    TransactionItem(JSONObject jData){
        try{
            id = jData.getInt("id");
            amount = jData.getDouble("amount");
            status = jData.getString("status");
            userId = jData.getInt("userID");
            debitType = jData.getString("debittype");

            if(jData.has("genriderequestid")) genRideRequestId = jData.getInt("genriderequestid");
            else genRideRequestId = 0;

            if(jData.has("riderequestID")) rideRequestId = jData.getInt("riderequestID");
            else rideRequestId = 0;

            if(jData.has("cancellationID")) cancellationId = jData.getInt("cancellationID");
            else cancellationId = 0;

            if(jData.has("recorddatetime"))recordDateTime = jData.getString("recorddatetime");
            else recordDateTime = "";

        }catch(Exception e){
            Log.d("EXCEPTION TRANS_ITEM", e.getMessage());
        }
    }
}
