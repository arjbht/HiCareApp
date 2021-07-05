package com.ab.hicarerun.adapter;

import android.content.Context;
import android.content.ServiceConnection;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.ab.hicarerun.R;
import com.ab.hicarerun.databinding.ItemChemicalTowerAdapterBinding;
import com.ab.hicarerun.databinding.ProductUnitsAdapterBinding;
import com.ab.hicarerun.handler.OnListItemClickHandler;
import com.ab.hicarerun.network.models.ChemicalModel.ServiceChemicalData;
import com.ab.hicarerun.network.models.ChemicalModel.TowerData;
import com.ab.hicarerun.network.models.ProductModel.ServicePlanUnits;
import com.ab.hicarerun.viewmodel.AccountAreaViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Arjun Bhatt on 6/27/2021.
 */
public class ChemicalTowerAdapter extends RecyclerView.Adapter<ChemicalTowerAdapter.ViewHolder> {
    private OnListItemClickHandler onItemClickHandler;
    private final Context mContext;
    private List<ServiceChemicalData> items = null;
    private int selectedPos = 0;

    public ChemicalTowerAdapter(Context context) {
        if (items == null) {
            items = new ArrayList<>();
        }
        this.mContext = context;
    }

    @NotNull
    @Override
    public ChemicalTowerAdapter.ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        ItemChemicalTowerAdapterBinding mItemChemicalTowerAdapterBinding =
                DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),
                        R.layout.item_chemical_tower_adapter, parent, false);
        return new ChemicalTowerAdapter.ViewHolder(mItemChemicalTowerAdapterBinding);
    }

    @Override
    public void onBindViewHolder(@NotNull ChemicalTowerAdapter.ViewHolder holder, final int position) {
        try {
            if (selectedPos == position) {
                holder.mItemChemicalTowerAdapterBinding.cardView.setCardBackgroundColor(mContext.getResources().getColor(R.color.colorPrimary));
                holder.mItemChemicalTowerAdapterBinding.lnrTower.setBackground(mContext.getResources().getDrawable(R.drawable.green_round_border));
                holder.mItemChemicalTowerAdapterBinding.txtTower.setTextColor(mContext.getResources().getColor(R.color.colorPrimary));
            } else {
                holder.mItemChemicalTowerAdapterBinding.cardView.setCardBackgroundColor(mContext.getResources().getColor(R.color.greyclose));
                holder.mItemChemicalTowerAdapterBinding.lnrTower.setBackground(mContext.getResources().getDrawable(R.drawable.white_round_border));
                holder.mItemChemicalTowerAdapterBinding.txtTower.setTextColor(mContext.getResources().getColor(R.color.greyclose));
            }
            if (items.get(position).getAreaType().equals("Common Area")) {
                if (items.get(position).getTowerName() != null) {
                    holder.mItemChemicalTowerAdapterBinding.txtTower.setText(items.get(position).getTowerName());
                } else {
                    holder.mItemChemicalTowerAdapterBinding.txtTower.setText("Common Area");
                }
            } else {
                if (items.get(position).getTowerName() != null) {
                    holder.mItemChemicalTowerAdapterBinding.txtTower.setText(items.get(position).getTowerName());
                } else {
                    holder.mItemChemicalTowerAdapterBinding.txtTower.setText("Tower " + String.valueOf(items.get(position).getTower()));
                }
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickHandler.onItemClick(position);
                    selectedPos = position;
                    notifyDataSetChanged();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOnItemClickHandler(OnListItemClickHandler onItemClickHandler) {
        this.onItemClickHandler = onItemClickHandler;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setData(List<ServiceChemicalData> data) {
        items.clear();
        items.addAll(data);
    }

    public ServiceChemicalData getItem(int position) {
        return items.get(position);
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemChemicalTowerAdapterBinding mItemChemicalTowerAdapterBinding;

        public ViewHolder(ItemChemicalTowerAdapterBinding mItemChemicalTowerAdapterBinding) {
            super(mItemChemicalTowerAdapterBinding.getRoot());
            this.mItemChemicalTowerAdapterBinding = mItemChemicalTowerAdapterBinding;
        }
    }
}
