package com.radicaldynamic.groupinform.adapters;

import java.util.ArrayList;
import java.util.Map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.radicaldynamic.groupinform.R;
import com.radicaldynamic.groupinform.documents.FormDocument;

public class BrowserListAdapter extends ArrayAdapter<FormDocument>
{       
    private Context mContext;
    private ArrayList<FormDocument> mItems;
    private Map<String, String> mInstanceTallies;
    private Spinner mSpinner;

    public BrowserListAdapter(Context context, int textViewResourceId, ArrayList<FormDocument> items, Map<String, String> instanceTallies, Spinner spinner) {
        super(context, textViewResourceId, items);
        mContext = context;
        mItems = items;           
        mInstanceTallies = instanceTallies;
        mSpinner = spinner;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View v = convertView;        

        if (v == null) {            
            LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.browser_list_item, null);
        } 

        FormDocument f = mItems.get(position);

        if (f != null) {
            TextView tt = (TextView) v.findViewById(R.id.toptext);
            TextView bt = (TextView) v.findViewById(R.id.bottomtext);

            if (tt != null) {
                tt.setText(f.getName());
            }

            if (!mInstanceTallies.isEmpty()) {             
                if (bt != null) {
                    String descriptor = mSpinner.getSelectedItem().toString().toLowerCase();

                    // Correct plural words (this only works in very simple circumstances using English)
                    if (mInstanceTallies.get(f.getId()).equals("1")) {                           
                        descriptor = descriptor.substring(0, descriptor.length() - 1);
                    }

                    bt.setText(mInstanceTallies.get(f.getId()) + " " + descriptor);
                }
            }
        }

        return v;
    }
}