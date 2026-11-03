package com.carry.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.carry.app.R;
import com.carry.app.models.RequestStatus;
import com.carry.app.models.StatusUpdate;

import java.util.ArrayList;
import java.util.List;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.TimelineViewHolder> {

    private final Context context;
    private final List<StatusUpdate> updates = new ArrayList<>();

    public TimelineAdapter(Context context) {
        this.context = context;
    }

    public void setUpdates(List<StatusUpdate> list) {
        this.updates.clear();
        if (list != null) {
            this.updates.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TimelineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_timeline, parent, false);
        return new TimelineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimelineViewHolder holder, int position) {
        StatusUpdate update = updates.get(position);

        RequestStatus status = update.getStatus() != null ? update.getStatus() : RequestStatus.REQUESTED;
        holder.tvTimelineStatus.setText(status.getLabel());

        String time = update.getCreatedAt() != null ? update.getCreatedAt().replace("T", " ") : "";
        if (time.length() > 16) {
            time = time.substring(0, 16);
        }
        holder.tvTimelineTime.setText(time);

        String note = update.getNote();
        if (note == null || note.trim().isEmpty()) {
            note = status.getDescription();
        }
        holder.tvTimelineNote.setText(note);
    }

    @Override
    public int getItemCount() {
        return updates.size();
    }

    static class TimelineViewHolder extends RecyclerView.ViewHolder {
        ImageView ivTimelineDot;
        TextView tvTimelineStatus;
        TextView tvTimelineTime;
        TextView tvTimelineNote;

        TimelineViewHolder(@NonNull View itemView) {
            super(itemView);
            ivTimelineDot = itemView.findViewById(R.id.ivTimelineDot);
            tvTimelineStatus = itemView.findViewById(R.id.tvTimelineStatus);
            tvTimelineTime = itemView.findViewById(R.id.tvTimelineTime);
            tvTimelineNote = itemView.findViewById(R.id.tvTimelineNote);
        }
    }
}
