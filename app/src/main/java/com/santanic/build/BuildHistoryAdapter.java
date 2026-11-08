package com.santanic.build;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class BuildHistoryAdapter extends RecyclerView.Adapter<BuildHistoryAdapter.ViewHolder> {
    private final List<String[]> data;

    public BuildHistoryAdapter(List<String[]> data) { this.data = data; }

    @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(v);
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String[] item = data.get(position);
        holder.tvName.setText(item[0]);
        holder.tvTime.setText(item[1]);
        holder.tvStatus.setText(item[2]);
        
        if (item[2].equals("Berhasil")) {
            holder.tvStatus.setTextColor(Color.GREEN);
            holder.indicator.setBackgroundColor(Color.GREEN);
        } else {
            holder.tvStatus.setTextColor(Color.RED);
            holder.indicator.setBackgroundColor(Color.RED);
        }
    }

    @Override public int getItemCount() { return data.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTime, tvStatus;
        View indicator;
        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvBuildName);
            tvTime = v.findViewById(R.id.tvBuildTime);
            tvStatus = v.findViewById(R.id.tvStatus);
            indicator = v.findViewById(R.id.statusIndicator);
        }
    }
}