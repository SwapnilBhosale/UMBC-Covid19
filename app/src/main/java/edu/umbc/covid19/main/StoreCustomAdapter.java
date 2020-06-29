package edu.umbc.covid19.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import edu.umbc.covid19.R;

public class StoreCustomAdapter extends BaseAdapter {

    private List<StoreData> dataSet;
    Context mContext;
    private final LayoutInflater mInflater;


    public StoreCustomAdapter(List<StoreData> data, Context context) {
        this.dataSet = data;
        this.mContext=context;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    public void setList(List<StoreData> list){
        this.dataSet = list;
    }


    @Override
    public int getCount() {
        return dataSet.size();
    }

    @Override
    public Object getItem(int i) {
        return dataSet.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View vi=view;
        final int pos = i;
        if(vi == null){

            // create  viewholder object for list_rowcell View.
            viewHolder = new ViewHolder();
            // inflate list_rowcell for each row
            vi = mInflater.inflate(R.layout.store_item,null);
            viewHolder.image_view = (ImageView) vi.findViewById(R.id.icon2);
            viewHolder.text_view = (TextView) vi.findViewById(R.id.storeName);
            viewHolder.text_view_1 = (TextView) vi.findViewById(R.id.Count);

            vi.setTag(viewHolder);
        }else {
            viewHolder= (ViewHolder) vi.getTag();
        }

        viewHolder.text_view.setText(String.valueOf(dataSet.get(pos).getCOUNT()));
        viewHolder.text_view_1.setText(String.valueOf(dataSet.get(pos).getSTORE()));
        return vi;
    }

    ViewHolder viewHolder = null;
    private static class ViewHolder{
        ImageView image_view;
        TextView text_view,text_view_1;

    }


}
