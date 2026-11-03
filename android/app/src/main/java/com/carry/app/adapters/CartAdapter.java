package com.carry.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.carry.app.R;
import com.carry.app.models.CartItem;
import com.carry.app.models.Product;

import java.util.ArrayList;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    public interface OnCartItemInteractionListener {
        void onQuantityChanged(Long productId, int newQuantity);
        void onItemRemoved(Long productId);
    }

    private final Context context;
    private final List<CartItem> cartItems = new ArrayList<>();
    private final OnCartItemInteractionListener interactionListener;

    public CartAdapter(Context context, OnCartItemInteractionListener interactionListener) {
        this.context = context;
        this.interactionListener = interactionListener;
    }

    public void setItems(List<CartItem> items) {
        this.cartItems.clear();
        if (items != null) {
            this.cartItems.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        Product product = item.getProduct();
        if (product == null) return;

        holder.tvCartItemName.setText(product.getName());
        holder.tvCartItemUnitPrice.setText(String.format("৳%.0f each", product.getEstimatedPrice()));
        holder.tvCartQuantity.setText(String.valueOf(item.getQuantity()));

        if (product.getImageUrl() != null && !product.getImageUrl().trim().isEmpty()) {
            Glide.with(context)
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.ic_shop)
                    .error(R.drawable.ic_shop)
                    .centerCrop()
                    .into(holder.ivCartItemImg);
        } else {
            holder.ivCartItemImg.setImageResource(R.drawable.ic_shop);
        }

        holder.btnCartMinus.setOnClickListener(v -> {
            int newQty = item.getQuantity() - 1;
            if (interactionListener != null) {
                if (newQty <= 0) {
                    interactionListener.onItemRemoved(product.getId());
                } else {
                    interactionListener.onQuantityChanged(product.getId(), newQty);
                }
            }
        });

        holder.btnCartPlus.setOnClickListener(v -> {
            int newQty = item.getQuantity() + 1;
            if (interactionListener != null) {
                interactionListener.onQuantityChanged(product.getId(), newQty);
            }
        });

        holder.btnCartDelete.setOnClickListener(v -> {
            if (interactionListener != null) {
                interactionListener.onItemRemoved(product.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCartItemImg;
        TextView tvCartItemName;
        TextView tvCartItemUnitPrice;
        ImageButton btnCartMinus;
        TextView tvCartQuantity;
        ImageButton btnCartPlus;
        ImageButton btnCartDelete;

        CartViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCartItemImg = itemView.findViewById(R.id.ivCartItemImg);
            tvCartItemName = itemView.findViewById(R.id.tvCartItemName);
            tvCartItemUnitPrice = itemView.findViewById(R.id.tvCartItemUnitPrice);
            btnCartMinus = itemView.findViewById(R.id.btnCartMinus);
            tvCartQuantity = itemView.findViewById(R.id.tvCartQuantity);
            btnCartPlus = itemView.findViewById(R.id.btnCartPlus);
            btnCartDelete = itemView.findViewById(R.id.btnCartDelete);
        }
    }
}
