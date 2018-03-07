package com.example.vomyrak.heatband;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by VomyraK on 07/03/2018.
 */

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.AdapterViewHolder> {

    private String[] mDeviceData;
    public DeviceAdapter(){}

    public class AdapterViewHolder extends RecyclerView.ViewHolder{
        public final TextView mDeviceTextView;

        public AdapterViewHolder(View view){
            super(view);
            this.mDeviceTextView = (TextView) view.findViewById(R.id.found_bluetooth_devices);
        }
    }

    @Override
    public void onBindViewHolder(AdapterViewHolder holder, int position) {
        String newString = mDeviceData[position];
        holder.mDeviceTextView.setText(newString);
    }

    @Override
    public AdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, shouldAttachToParentImmediately);
        AdapterViewHolder adapterViewHolder = new AdapterViewHolder(view);
        return adapterViewHolder;
    }

    @Override
    public int getItemCount() {
        if (mDeviceData == null) return 0;
        return mDeviceData.length;
    }

    public void setDeviceData(String[] data){
        mDeviceData = new String[]{"1","1","1","1","1","1","1","1","1",};
        notifyDataSetChanged();
    }
}
