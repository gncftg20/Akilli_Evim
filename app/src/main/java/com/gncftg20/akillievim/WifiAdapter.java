package com.gncftg20.akillievim;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class WifiAdapter extends RecyclerView.Adapter<WifiAdapter.WifiViewHolder> {
    private List<String> wifiList = new ArrayList<>();
    private OnWifiClickListener listener;

    public interface OnWifiClickListener {
        void onWifiClick(String ssid);
    }

    public WifiAdapter(OnWifiClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public WifiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new WifiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WifiViewHolder holder, int position) {
        String ssid = wifiList.get(position);
        holder.textView.setText(ssid);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onWifiClick(ssid);
            }
        });
    }

    @Override
    public int getItemCount() {
        return wifiList.size();
    }

    public void updateWifiList(List<String> newList) {
        wifiList.clear();
        wifiList.addAll(newList);
        notifyDataSetChanged();
    }

    static class WifiViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        WifiViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
} 