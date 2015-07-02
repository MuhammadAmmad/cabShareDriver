package in.co.hoi.cabshare;

import android.util.Log;
import org.json.JSONObject;

/**
 * Created by Ujjwal on 25-05-2015.
 */
public class DetailCar {
    int id;
    String regNumber;
    String model;
    String make;
    String color;

    public DetailCar(JSONObject carInfo){
        try {
            this.id = carInfo.getInt("id");
            this.regNumber = carInfo.getString("regnumber");
            this.model = carInfo.getString("model");
            this.make = carInfo.getString("make");
            this.color = carInfo.getString("color");
        }
        catch(Exception e){
            Log.d("Exception", "Car Details");
        }
    }

}
