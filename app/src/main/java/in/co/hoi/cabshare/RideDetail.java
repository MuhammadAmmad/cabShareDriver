package in.co.hoi.cabshare;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by lenovo pc on 25-05-2015.
 */
public class RideDetail {
    LatLng rideCoords;
    CarDetail carDetail;
    String driverName;
    String driverPic;
    String driverPhone;
    String driverbgc;
    int occupancy;
    float billingrate;
    int rideRequestId;

    public RideDetail(Double clat, Double clong, CarDetail details, String name, String pic, String phone, String bgc, int passenger, float billingrate, int id){
        this.rideCoords = new LatLng(clat, clong);
        carDetail = details;
        driverName = name;
        driverPic = pic;
        driverPhone = phone;
        driverbgc = bgc;
        occupancy = passenger;
        this.billingrate = billingrate;
        rideRequestId = id;
    }
}
