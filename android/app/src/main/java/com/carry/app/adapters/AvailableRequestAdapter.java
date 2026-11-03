package com.carry.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.carry.app.R;
import com.carry.app.models.ProductRequest;
import com.google.android.material.button.MaterialButton;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AvailableRequestAdapter extends RecyclerView.Adapter<AvailableRequestAdapter.AvailableViewHolder> {

    public interface OnAcceptDeliveryListener {
        void onAccept(ProductRequest request);
    }

    private final Context context;
    private final List<ProductRequest> availableList = new ArrayList<>();
    private final OnAcceptDeliveryListener acceptListener;

    public AvailableRequestAdapter(Context context, OnAcceptDeliveryListener acceptListener) {
        this.context = context;
        this.acceptListener = acceptListener;
    }

    public void setRequests(List<ProductRequest> list) {
        this.availableList.clear();
        if (list != null) {
            this.availableList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AvailableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_available_request, parent, false);
        return new AvailableViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvailableViewHolder holder, int position) {
        ProductRequest request = availableList.get(position);

        String customer = request.getCustomerName() != null ? request.getCustomerName() : "KUET Student";
        holder.tvAvailCustomer.setText(String.format("Requested by %s", customer));

        BigDecimal deliveryEarnings = request.getDeliveryCharge() != null ? request.getDeliveryCharge() : new BigDecimal("30.00");
        holder.tvAvailEarningBadge.setText(String.format("Earn ৳%.0f", deliveryEarnings));

        int qty = request.getQuantity() != null ? request.getQuantity() : 1;
        holder.tvAvailProductName.setText(String.format("%s (x%d)", request.getProductName(), qty));

        String area = request.getPickupArea() != null ? request.getPickupArea().getDisplayName() : "Khulna City";
        holder.tvAvailPickupArea.setText(String.format("Pickup: %s", area));

        String shopOrNotes = request.getInstructions();
        if (request.getPreferredShop() != null && !request.getPreferredShop().trim().isEmpty()) {
            shopOrNotes = "Shop: " + request.getPreferredShop() + (shopOrNotes != null ? " • " + shopOrNotes : "");
        }
        holder.tvAvailInstructions.setText(shopOrNotes != null ? shopOrNotes : "Standard campus delivery");

        BigDecimal budget = request.getUnitPriceSnapshot() != null ?
                request.getUnitPriceSnapshot().multiply(BigDecimal.valueOf(qty)) :
                request.getBudget();
        holder.tvAvailBudget.setText(String.format("Item Budget: ৳%.0f", budget != null ? budget : BigDecimal.ZERO));

        holder.btnAcceptDelivery.setOnClickListener(v -> {
            if (acceptListener != null) {
                acceptListener.onAccept(request);
            }
        });
    }

    @Override
    public int getItemCount() {
        return availableList.size();
    }

    static class AvailableViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvailCustomer;
        TextView tvAvailEarningBadge;
        TextView tvAvailProductName;
        TextView tvAvailPickupArea;
        TextView tvAvailInstructions;
        TextView tvAvailBudget;
        MaterialButton btnAcceptDelivery;

        AvailableViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvailCustomer = itemView.findViewById(R.id.tvAvailCustomer);
            tvAvailEarningBadge = itemView.findViewById(R.id.tvAvailEarningBadge);
            tvAvailProductName = itemView.findViewById(R.id.tvAvailProductName);
            tvAvailPickupArea = itemView.findViewById(R.id.tvAvailPickupArea);
            tvAvailInstructions = itemView.findViewById(R.id.tvAvailInstructions);
            tvAvailBudget = itemView.findViewById(R.id.tvAvailBudget);
            btnAcceptDelivery = itemView.findViewById(R.id.btnAcceptDelivery);
        }
    }
}
