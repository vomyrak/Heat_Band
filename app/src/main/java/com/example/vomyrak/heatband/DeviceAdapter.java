package com.example.vomyrak.heatband;

import android.bluetooth.BluetoothDevice;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import static com.example.vomyrak.heatband.ScanActivity.discoveredDevices;
/**
 * Created by VomyraK on 07/03/2018.
 * This java document is used to create recycler view as specified by Android API
 * for displaying list with many items.
 */




public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.AdapterViewHolder> {

    private final RecyclerViewClickListener mListener;
    private static int viewHolderCount;
    private int mNumberItems;
    public interface RecyclerViewClickListener{
        void onListItemClick(int clickedItemIndex);
    }

    //Create device adapter
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
        String displayedName = discoveredDevices.get(position).getName();
        String displayedAddress = discoveredDevices.get(position).getAddress();
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
        if (discoveredDevices == null) return 0;
        return discoveredDevices.size();
    }

    public void setDeviceData(BluetoothDevice data){
        discoveredDevices.add(data);
        notifyDataSetChanged();
    }
}
