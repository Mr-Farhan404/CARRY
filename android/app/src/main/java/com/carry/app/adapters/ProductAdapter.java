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
import com.carry.app.models.Product;
import com.google.android.material.button.MaterialButton;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    public interface OnProductActionListener {
        void onAddToCart(Product product, int quantity);
        void onOrderNow(Product product, int quantity);
    }

    private final Context context;
    private final List<Product> products = new ArrayList<>();
    private final Map<Long, Integer> quantities = new HashMap<>();
    private final OnProductActionListener actionListener;

    public ProductAdapter(Context context, OnProductActionListener actionListener) {
        this.context = context;
        this.actionListener = actionListener;
    }

    public void setProducts(List<Product> newProducts) {
        this.products.clear();
        if (newProducts != null) {
            this.products.addAll(newProducts);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);
        Long id = product.getId();

        int qty = quantities.getOrDefault(id, 1);

        holder.tvProductName.setText(product.getName());
        holder.tvProductDescription.setText(product.getDescription() != null ? product.getDescription() : "");

        String catName = product.getCategory() != null ? product.getCategory().name() : "PRODUCT";
        holder.tvCategoryBadge.setText(catName);

        BigDecimal price = product.getEstimatedPrice();
        holder.tvProductPrice.setText(String.format("৳%.0f", price));

        // Delivery fee badge
        BigDecimal delCharge = product.getBaseDeliveryCharge() != null ? product.getBaseDeliveryCharge() : new BigDecimal("30");
        holder.tvDeliveryFeeBadge.setText(String.format("+৳%.0f delivery", delCharge));

        // Quantity count
        holder.tvQuantity.setText(String.valueOf(qty));

        // Dynamic ⚡ Order Now button text showing subtotal for current quantity
        BigDecimal itemTotal = price.multiply(BigDecimal.valueOf(qty));
        holder.btnOrderNow.setText(String.format("⚡ Order Now (৳%.0f)", itemTotal));

        // Load image with Glide
        if (product.getImageUrl() != null && !product.getImageUrl().trim().isEmpty()) {
            Glide.with(context)
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.ic_shop)
                    .error(R.drawable.ic_shop)
                    .centerCrop()
                    .into(holder.ivProductImage);
        } else {
            holder.ivProductImage.setImageResource(R.drawable.ic_shop);
        }

        // Quantity minus
        holder.btnQtyMinus.setOnClickListener(v -> {
            int currentQty = quantities.getOrDefault(id, 1);
            if (currentQty > 1) {
                int newQty = currentQty - 1;
                quantities.put(id, newQty);
                holder.tvQuantity.setText(String.valueOf(newQty));
                BigDecimal updatedTotal = price.multiply(BigDecimal.valueOf(newQty));
                holder.btnOrderNow.setText(String.format("⚡ Order Now (৳%.0f)", updatedTotal));
            }
        });

        // Quantity plus
        holder.btnQtyPlus.setOnClickListener(v -> {
            int currentQty = quantities.getOrDefault(id, 1);
            int newQty = currentQty + 1;
            quantities.put(id, newQty);
            holder.tvQuantity.setText(String.valueOf(newQty));
            BigDecimal updatedTotal = price.multiply(BigDecimal.valueOf(newQty));
            holder.btnOrderNow.setText(String.format("⚡ Order Now (৳%.0f)", updatedTotal));
        });

        // Add to Cart click
        holder.btnAddToCart.setOnClickListener(v -> {
            int selectedQty = quantities.getOrDefault(id, 1);
            if (actionListener != null) {
                actionListener.onAddToCart(product, selectedQty);
            }
        });

        // Direct Order Now click
        holder.btnOrderNow.setOnClickListener(v -> {
            int selectedQty = quantities.getOrDefault(id, 1);
            if (actionListener != null) {
                actionListener.onOrderNow(product, selectedQty);
            }
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductImage;
        TextView tvCategoryBadge;
        TextView tvDeliveryFeeBadge;
        TextView tvProductName;
        TextView tvProductDescription;
        TextView tvProductPrice;
        ImageButton btnQtyMinus;
        TextView tvQuantity;
        ImageButton btnQtyPlus;
        MaterialButton btnAddToCart;
        MaterialButton btnOrderNow;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvCategoryBadge = itemView.findViewById(R.id.tvCategoryBadge);
            tvDeliveryFeeBadge = itemView.findViewById(R.id.tvDeliveryFeeBadge);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductDescription = itemView.findViewById(R.id.tvProductDescription);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            btnQtyMinus = itemView.findViewById(R.id.btnQtyMinus);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            btnQtyPlus = itemView.findViewById(R.id.btnQtyPlus);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);
            btnOrderNow = itemView.findViewById(R.id.btnOrderNow);
        }
    }
}
