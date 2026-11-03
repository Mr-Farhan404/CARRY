package com.carry.app.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.carry.app.R;
import com.carry.app.models.AdditionalPaymentStatus;
import com.carry.app.models.Payment;
import com.carry.app.models.ProductRequest;
import com.carry.app.models.RequestStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    public interface OnRequestClickListener {
        void onRequestClick(ProductRequest request);
    }

    private final Context context;
    private final List<ProductRequest> requests = new ArrayList<>();
    private final OnRequestClickListener clickListener;

    public RequestAdapter(Context context, OnRequestClickListener clickListener) {
        this.context = context;
        this.clickListener = clickListener;
    }

    public void setRequests(List<ProductRequest> list) {
        this.requests.clear();
        if (list != null) {
            this.requests.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        ProductRequest request = requests.get(position);

        holder.tvOrderNumber.setText(String.format("#REQ-%d", request.getId()));
        holder.tvOrderTypeBadge.setText(request.getOrderType() != null ? request.getOrderType().name() : "MANUAL");

        RequestStatus status = request.getStatus() != null ? request.getStatus() : RequestStatus.REQUESTED;
        holder.tvRequestStatusBadge.setText(status.getLabel());
        applyStatusBadgeColor(holder.tvRequestStatusBadge, status);

        int qty = request.getQuantity() != null ? request.getQuantity() : 1;
        holder.tvRequestProductName.setText(String.format("%s (x%d)", request.getProductName(), qty));

        String area = request.getPickupArea() != null ? request.getPickupArea().getDisplayName() : "Khulna City";
        holder.tvPickupArea.setText(String.format("Pickup: %s", area));

        // Price details
        BigDecimal budgetOrPrice = request.getUnitPriceSnapshot() != null ?
                request.getUnitPriceSnapshot().multiply(BigDecimal.valueOf(qty)) :
                request.getBudget();
        BigDecimal delivery = request.getDeliveryCharge();
        BigDecimal total = (budgetOrPrice != null ? budgetOrPrice : BigDecimal.ZERO).add(delivery != null ? delivery : BigDecimal.ZERO);
        holder.tvPriceDetails.setText(String.format("Total: ৳%.0f (Delivery: ৳%.0f)", total, delivery));

        // Payment status
        Payment payment = request.getPayment();
        if (payment != null && payment.getStatus() != null) {
            String method = payment.getPaymentMethod() != null ? payment.getPaymentMethod().getDisplayName() : "MFS";
            holder.tvPaymentStatus.setText(String.format("%s • %s", method, payment.getStatus().getLabel()));
            if (payment.getStatus().name().contains("VERIFIED") || payment.getStatus().name().contains("SIMULATED")) {
                holder.tvPaymentStatus.setTextColor(ContextCompat.getColor(context, R.color.status_green));
            } else {
                holder.tvPaymentStatus.setTextColor(ContextCompat.getColor(context, R.color.status_amber));
            }
        } else {
            holder.tvPaymentStatus.setText("Deposit: Pending");
            holder.tvPaymentStatus.setTextColor(ContextCompat.getColor(context, R.color.text_muted));
        }

        // Need more banner
        if (payment != null && payment.getAdditionalPaymentStatus() == AdditionalPaymentStatus.REQUESTED) {
            holder.layoutNeedMoreBanner.setVisibility(View.VISIBLE);
            holder.tvNeedMoreNotice.setText(String.format("⚠️ Carrier requested additional ৳%.0f", payment.getAdditionalAmount()));
            holder.tvNeedMoreReason.setText(payment.getNeedMoreReason() != null ? "Reason: " + payment.getNeedMoreReason() : "Tap to review");
        } else {
            holder.layoutNeedMoreBanner.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onRequestClick(request);
            }
        });
    }

    private void applyStatusBadgeColor(TextView badge, RequestStatus status) {
        switch (status) {
            case DELIVERED:
                badge.setBackgroundResource(R.drawable.bg_badge_success);
                badge.setTextColor(ContextCompat.getColor(context, R.color.status_green));
                break;
            case CANCELLED:
                badge.setBackgroundResource(R.drawable.bg_badge_danger);
                badge.setTextColor(ContextCompat.getColor(context, R.color.status_red));
                break;
            case COLLECTED:
            case RETURNING:
            case READY_FOR_DELIVERY:
                badge.setBackgroundResource(R.drawable.bg_badge_warning);
                badge.setTextColor(ContextCompat.getColor(context, R.color.status_amber));
                break;
            case ACCEPTED:
            case REQUESTED:
            default:
                badge.setBackgroundResource(R.drawable.bg_badge_primary);
                badge.setTextColor(ContextCompat.getColor(context, R.color.primary));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderNumber;
        TextView tvOrderTypeBadge;
        TextView tvRequestStatusBadge;
        TextView tvRequestProductName;
        TextView tvPickupArea;
        TextView tvPriceDetails;
        TextView tvPaymentStatus;
        LinearLayout layoutNeedMoreBanner;
        TextView tvNeedMoreNotice;
        TextView tvNeedMoreReason;

        RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderNumber = itemView.findViewById(R.id.tvOrderNumber);
            tvOrderTypeBadge = itemView.findViewById(R.id.tvOrderTypeBadge);
            tvRequestStatusBadge = itemView.findViewById(R.id.tvRequestStatusBadge);
            tvRequestProductName = itemView.findViewById(R.id.tvRequestProductName);
            tvPickupArea = itemView.findViewById(R.id.tvPickupArea);
            tvPriceDetails = itemView.findViewById(R.id.tvPriceDetails);
            tvPaymentStatus = itemView.findViewById(R.id.tvPaymentStatus);
            layoutNeedMoreBanner = itemView.findViewById(R.id.layoutNeedMoreBanner);
            tvNeedMoreNotice = itemView.findViewById(R.id.tvNeedMoreNotice);
            tvNeedMoreReason = itemView.findViewById(R.id.tvNeedMoreReason);
        }
    }
}
