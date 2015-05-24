package in.co.hoi.cabshare;

/**
 * Created by lenovo pc on 24-05-2015.
 */
public class CabBookingDetails {
    Double origin_latitude;

    Double origin_longitude;

    String origin_address1;

    String origin_address2;

    Double destination_latitude;

    Double destination_longitude;

    String destination_address1;

    String destination_address2;

    String requestdatetime;

    public CabBookingDetails(Double sLat, Double sLong, String sa1, String sa2, Double dLat, Double dLong, String da1, String da2){
        origin_latitude = sLat;
        origin_longitude = sLong;
        origin_address1 = sa1;
        origin_address2 = sa2;
        destination_latitude = dLat;
        destination_longitude = dLong;
        destination_address1 = da1;
        destination_address2 = da2;
        requestdatetime = "";

    }
}
