package com.example.vomyrak.heatband;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by VomyraK on 07/03/2018.
 */

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.AdapterViewHolder> {

    private ArrayList<BluetoothDevice> mDeviceData = new ArrayList<>();
    private final RecyclerViewClickListener mListener;
    private static int viewHolderCount;
    private int mNumberItems;
    public interface RecyclerViewClickListener{
        void onListItemClick(int clickedItemIndex);
    }

    public DeviceAdapter(RecyclerViewClickListener listener){
        mListener = listener;
    }

    public class AdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public final TextView mDeviceName;
        public final TextView mDeviceAddress;

        public AdapterViewHolder(View view){
            super(view);
            this.mDeviceName = view.findViewById(R.id.found_bluetooth_device_name);
            this.mDeviceAddress = view.findViewById(R.id.found_bluetooth_devices_address);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int clickedPosition = getAdapterPosition();
            mListener.onListItemClick(clickedPosition);
        }
    }

    @Override
    public void onBindViewHolder(AdapterViewHolder holder, int position) {
        String displayedName = mDeviceData.get(position).getName();
        String displayedAddress = mDeviceData.get(position).getAddress();
        holder.mDeviceName.setText(displayedName);
        holder.mDeviceAddress.setText(displayedAddress);
    }



    @Override
    public AdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new AdapterViewHolder(view);
    }

    @Override
    public int getItemCount() {
        if (mDeviceData == null) return 0;
        return mDeviceData.size();
    }

    public void setDeviceData(BluetoothDevice data){
        mDeviceData.add(data);
        notifyDataSetChanged();
    }
}
