package com.learnerdev.sharenearn;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by uttam on 18-Feb-18.
 */

public class ItemsListAdapter extends RecyclerView.Adapter<ItemsListAdapter.ViewHolder> {
    private List<Item> itemsList;
    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        final TextView tvItemId;
        final TextView tvItemName;
        final TextView tvItemDetails;
        final TextView tvItemLocation;
        public ViewHolder(View view) {
            super(view);
            tvItemId=view.findViewById(R.id.tv_row_item_id);
            tvItemName=view.findViewById(R.id.tv_row_item_name);
            tvItemDetails=view.findViewById(R.id.tv_row_item_details);
            tvItemLocation=view.findViewById(R.id.tv_row_item_location);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public ItemsListAdapter(List<Item> itemsList) {
        this.itemsList = itemsList;
    }

    @Override
    public ItemsListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_list_row, parent, false);
        return new ItemsListAdapter.ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ItemsListAdapter.ViewHolder holder, int position) {
       //TODO Warning : Edit any hardcoded strings
        Item item=itemsList.get(position);
        holder.tvItemId.setText(item.getObjectId());
        holder.tvItemName.setText(item.getItemName()+": ");
        holder.tvItemDetails.setText(item.getItemDetails());
        holder.tvItemLocation.setText(item.getLocation().getLongitude()+"");
    }

    @Override
    public int getItemCount() {
    return itemsList.size();
    }
}
