package in.co.hoi.cabshare;

import android.graphics.Bitmap;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Ujjwal on 8/9/2015.
 */
public class NextPassenger {
    int id;
    String phone;
    Bitmap image;
    LatLng destination;
    String address;
    boolean waiting;

    public NextPassenger(int id, String phone, Bitmap image, LatLng destination, String address, boolean waiting){
        this.id = id;
        this.address = address;
        this.phone = phone;
        this.image = image;
        this.destination = destination;
        this.waiting = waiting;
    }

    public NextPassenger(NextPassenger tmpPassenger) {
        id = tmpPassenger.getId();
        phone = tmpPassenger.getPhone();
        image = tmpPassenger.getImage();
        destination = tmpPassenger.getDestination();
        address = tmpPassenger.getAddress();
        waiting = tmpPassenger.isWaiting();
    }

    public int getId(){return id;}
    public String getAddress(){return address;}
    public LatLng getDestination(){return destination;}
    public String getPhone(){return phone;}
    public Bitmap getImage(){return image;}
    public Boolean isWaiting(){return waiting;}
}
