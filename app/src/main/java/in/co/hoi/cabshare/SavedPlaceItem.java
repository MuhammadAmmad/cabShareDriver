package in.co.hoi.cabshare;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Ujjwal on 01-06-2015.
 */
public class SavedPlaceItem {

        private LatLng coordinates = new LatLng(0,0);
        private String address1="";
        private String address2="";

        /*********** Set Methods ******************/

        public SavedPlaceItem(double lat, double lon, String add1, String add2)
        {
            this.coordinates = new LatLng(lat, lon);
            this.address1 = add1;
            this.address2 = add2;
        }



        /*********** Get Methods ****************/

        public String getAddress1()
        {
            return this.address1;
        }

        public String getAddress2()
        {
            return this.address2;
        }

        public LatLng getCoordinates()
        {
            return this.coordinates;
        }

}
