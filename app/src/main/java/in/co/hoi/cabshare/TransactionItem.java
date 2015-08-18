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
    String detail;

    int genRideRequestId;
    int rideRequestId;
    int cancellationId;

    int voucherId;
    int creditpgId;
    int cancellationRequestId;

    String recordDateTime;

    TransactionItem(JSONObject jData){
        try{
            id = jData.getInt("id");
            amount = jData.getDouble("amount");
            status = jData.getString("status");
            userId = jData.getInt("userID");

            if(jData.has("debittype")){
                detail = jData.getString("debittype");
            }else{
                detail = jData.getString("credittype");
            }


            if(jData.isNull("genriderequestid")){
                genRideRequestId = 0;
            }
            else genRideRequestId = jData.getInt("genriderequestid");

            if(!jData.isNull("riderequestID")) rideRequestId = jData.getInt("riderequestID");
            else rideRequestId = 0;

            if(!jData.isNull("cancellationID")) cancellationId = jData.getInt("cancellationID");
            else cancellationId = 0;

            if(!jData.isNull("voucherID")) voucherId = jData.getInt("voucherID");
            else voucherId = 0;


            if(!jData.isNull("creditpgID"))creditpgId = jData.getInt("creditpgID");
            else creditpgId = 0;


            if(!jData.isNull("cancellationRequestID")) cancellationRequestId = jData.getInt("cancellationRequestID");
            else cancellationRequestId = 0;

            if(!jData.isNull("recorddatetime"))recordDateTime = jData.getString("recorddatetime");
            else recordDateTime = "";

        }catch(Exception e){
            Log.d("EXCEPTION TRANS_ITEM", e.getMessage());
        }
    }
}
