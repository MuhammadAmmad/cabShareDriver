package in.co.hoi.cabshare;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.List;

/**
 * Created by Ujjwal on 27-06-2015.
 */
public class WalletListAdapter  extends BaseAdapter implements View.OnClickListener {

    private Activity activity;
    private List<TransactionItem> data;
    private static LayoutInflater inflater=null;
    public Resources res;
    TransactionItem tempValues=null;
    int i=0;

    public WalletListAdapter(Activity a, List<TransactionItem> d,Resources resLocal) {

        /********** Take passed values **********/
        activity = a;
        data=d;
        res = resLocal;

        /***********  Layout inflator to call external xml layout () ***********/
        inflater = ( LayoutInflater )activity.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    @Override
    public int getCount() {
        if(data.size()<=0)
            return 1;
        return data.size();
    }

    @Override
    public TransactionItem getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    /********* Create a holder Class to contain inflated xml file elements *********/
    public static class ViewHolder{

        public TextView amount;
        public TextView detail;
        public TextView time;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v  = convertView;
        ViewHolder holder;

        if(convertView==null){

            /****** Inflate tabitem.xml file for each row ( Defined below ) *******/
            v = inflater.inflate(R.layout.wallet_list_item, null);

            /****** View Holder Object to contain tabitem.xml file elements ******/

            holder = new ViewHolder();
            holder.amount=(TextView)v.findViewById(R.id.trans_cost);
            holder.detail = (TextView) v.findViewById(R.id.trans_detail);
            holder.time=(TextView)v.findViewById(R.id.trans_time);

            /************  Set holder with LayoutInflater ************/
            v.setTag( holder );
        }
        else
            holder=(ViewHolder)v.getTag();

        if(data.size()>0)
        {
            /***** Get each Model object from Arraylist ********/
            tempValues=null;
            tempValues = ( TransactionItem ) data.get( position );

            /************  Set Model values in Holder elements ***********/
            System.out.println(tempValues.detail + tempValues.recordDateTime + tempValues.amount);
            holder.detail.setText(tempValues.detail);
            holder.time.setText(tempValues.recordDateTime);
            holder.amount.setText(res.getString(R.string.Rs)+" "+tempValues.amount);
            v.setOnClickListener(new OnItemClickListener( position ));
        }
        return v;
    }

    @Override
    public void onClick(View v) {
        Log.v("CustomAdapter", "=====Row button clicked=====");
    }

    private class OnItemClickListener  implements View.OnClickListener {
        private int mPosition;

        OnItemClickListener(int position){
            mPosition = position;
        }

        @Override
        public void onClick(View arg0) {
            //Todo show the details of transaction
        }
    }
}
