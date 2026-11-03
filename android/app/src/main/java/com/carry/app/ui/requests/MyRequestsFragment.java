package com.carry.app.ui.requests;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.carry.app.R;
import com.carry.app.adapters.RequestAdapter;
import com.carry.app.api.ApiClient;
import com.carry.app.api.ApiService;
import com.carry.app.models.ProductRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyRequestsFragment extends Fragment implements RequestAdapter.OnRequestClickListener {

    private SwipeRefreshLayout swipeRefreshRequests;
    private RecyclerView rvMyRequests;
    private View layoutRequestsEmpty;
    private RequestAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_requests, container, false);

        swipeRefreshRequests = view.findViewById(R.id.swipeRefreshRequests);
        rvMyRequests = view.findViewById(R.id.rvMyRequests);
        layoutRequestsEmpty = view.findViewById(R.id.layoutRequestsEmpty);

        adapter = new RequestAdapter(requireContext(), this);
        rvMyRequests.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMyRequests.setAdapter(adapter);

        swipeRefreshRequests.setOnRefreshListener(this::loadMyRequests);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMyRequests();
    }

    public void loadMyRequests() {
        if (!isAdded()) return;
        swipeRefreshRequests.setRefreshing(true);

        ApiService api = ApiClient.getService(requireContext());
        api.getMyRequests().enqueue(new Callback<List<ProductRequest>>() {
            @Override
            public void onResponse(Call<List<ProductRequest>> call, Response<List<ProductRequest>> response) {
                if (!isAdded()) return;
                swipeRefreshRequests.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<ProductRequest> list = response.body();
                    adapter.setRequests(list);
                    layoutRequestsEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    layoutRequestsEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<ProductRequest>> call, Throwable t) {
                if (!isAdded()) return;
                swipeRefreshRequests.setRefreshing(false);
                layoutRequestsEmpty.setVisibility(View.VISIBLE);
                Toast.makeText(requireContext(), "Failed to load requests: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestClick(ProductRequest request) {
        Intent intent = new Intent(requireContext(), RequestDetailActivity.class);
        intent.putExtra("request_id", request.getId());
        intent.putExtra("request_data", request);
        startActivity(intent);
    }
}
