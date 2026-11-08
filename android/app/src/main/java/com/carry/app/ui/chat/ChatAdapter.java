package com.carry.app.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.carry.app.R;
import com.carry.app.models.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class ChatAdapter extends ListAdapter<ChatMessage, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_OUTGOING = 1;
    private static final int VIEW_TYPE_INCOMING = 2;

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault());

    private final String currentFirebaseUid;

    public ChatAdapter(String currentFirebaseUid) {
        super(DIFF_CALLBACK);
        this.currentFirebaseUid = currentFirebaseUid != null ? currentFirebaseUid : "";
    }

    private static final DiffUtil.ItemCallback<ChatMessage> DIFF_CALLBACK = new DiffUtil.ItemCallback<ChatMessage>() {
        @Override
        public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            if (oldItem.getId() != null && newItem.getId() != null) {
                return Objects.equals(oldItem.getId(), newItem.getId());
            }
            return Objects.equals(oldItem.getCreatedAt(), newItem.getCreatedAt())
                    && Objects.equals(oldItem.getText(), newItem.getText());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return Objects.equals(oldItem.getText(), newItem.getText())
                    && Objects.equals(oldItem.getSenderUid(), newItem.getSenderUid())
                    && Objects.equals(oldItem.getCreatedAt(), newItem.getCreatedAt());
        }
    };

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = getItem(position);
        if (message == null) return VIEW_TYPE_INCOMING;

        boolean isMine = message.isMine() || (!currentFirebaseUid.isEmpty() && currentFirebaseUid.equals(message.getSenderUid()));
        return isMine ? VIEW_TYPE_OUTGOING : VIEW_TYPE_INCOMING;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_OUTGOING) {
            View view = inflater.inflate(R.layout.item_chat_outgoing, parent, false);
            return new OutgoingViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_chat_incoming, parent, false);
            return new IncomingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = getItem(position);
        if (message == null) return;

        String formattedTime = formatTimestamp(message.getCreatedAt());

        if (holder instanceof OutgoingViewHolder) {
            OutgoingViewHolder vh = (OutgoingViewHolder) holder;
            vh.tvMessageText.setText(message.getText());
            vh.tvMessageTime.setText(formattedTime);
        } else if (holder instanceof IncomingViewHolder) {
            IncomingViewHolder vh = (IncomingViewHolder) holder;
            vh.tvSenderName.setText(message.getSenderName());
            vh.tvMessageText.setText(message.getText());
            vh.tvMessageTime.setText(formattedTime);
        }
    }

    private String formatTimestamp(Date date) {
        if (date == null) return "";
        try {
            long now = System.currentTimeMillis();
            long time = date.getTime();
            // If within same day (less than 24 hours and same calendar day), show time
            if (Math.abs(now - time) < 24 * 60 * 60 * 1000L) {
                return TIME_FORMAT.format(date);
            } else {
                return DATE_TIME_FORMAT.format(date);
            }
        } catch (Exception e) {
            return "";
        }
    }

    static class OutgoingViewHolder extends RecyclerView.ViewHolder {
        final TextView tvMessageText;
        final TextView tvMessageTime;

        OutgoingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
        }
    }

    static class IncomingViewHolder extends RecyclerView.ViewHolder {
        final TextView tvSenderName;
        final TextView tvMessageText;
        final TextView tvMessageTime;

        IncomingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
        }
    }
}
