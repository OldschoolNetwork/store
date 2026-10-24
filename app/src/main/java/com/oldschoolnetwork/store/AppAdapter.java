package com.oldschoolnetwork.store;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_APP = 1;
    private List<Object> items;
    public interface OnAppClickListener { void onClick(AppItem app); }
    private OnAppClickListener clickListener;
    public void setOnAppClickListener(OnAppClickListener listener) { clickListener = listener; }

    public AppAdapter(List<Object> items) {
        this.items = items != null ? items : new ArrayList<>();
        setHasStableIds(true);
    }

    public void setItems(List<Object> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        if (items == null || position < 0 || position >= items.size()) return position;
        Object obj = items.get(position);
        if(obj instanceof CategoryItem)return Long.MIN_VALUE+((CategoryItem)obj).slug.hashCode();
        if (obj instanceof String) {
            return ((String) obj).hashCode();
        } else if (obj instanceof AppItem) {
            return ((AppItem) obj).getId();
        }
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof String || items.get(position) instanceof CategoryItem) {
            return TYPE_HEADER;
        }
        return TYPE_APP;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_store_app, parent, false);
            return new AppViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_HEADER) {
            Object item=items.get(position);
            CategoryItem category=item instanceof CategoryItem?(CategoryItem)item:null;
            String title = category!=null?category.name:(String)item;
            ((HeaderViewHolder) holder).title.setText(title);
            ((HeaderViewHolder)holder).title.setCompoundDrawables(null,null,null,null);
            if(category!=null){android.graphics.drawable.Drawable arrow=androidx.appcompat.content.res.AppCompatResources.getDrawable(holder.itemView.getContext(),R.drawable.ic_arrow_right);int size=Math.round(18*holder.itemView.getResources().getDisplayMetrics().density);arrow.setBounds(0,0,size,size);((HeaderViewHolder)holder).title.setCompoundDrawables(null,null,arrow,null);((HeaderViewHolder)holder).title.setCompoundDrawablePadding(size/2);}
            holder.itemView.setOnClickListener(category!=null?v->{if(v.getContext() instanceof StoreMainActivity)((StoreMainActivity)v.getContext()).openCategory(category.slug);}:title.startsWith("Could not update catalog") ? v -> StoreRepository.getInstance().fetchCatalog(true) : null);
        } else {
            final AppItem app = (AppItem) items.get(position);
            AppViewHolder appHolder = (AppViewHolder) holder;

            appHolder.title.setText(app.getTitle());
            appHolder.developer.setText(app.getDeveloper());
            appHolder.rating.setText(app.getRating());
            appHolder.downloads.setText(app.getDownloads());
            appHolder.price.setText(app.getPrice());

            float density = appHolder.itemView.getContext().getResources().getDisplayMetrics().density;
            int cornerRadiusPx = (int) (12 * density);

            app.loadIconInto(appHolder.icon.getContext(), appHolder.icon, cornerRadiusPx);

            if (app.getPrice().equalsIgnoreCase("Free")) {
                appHolder.price.setTextColor(Color.parseColor("#4CAF50"));
            } else {
                appHolder.price.setTextColor(Color.parseColor("#FFFFFF"));
            }

            appHolder.itemView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onClick(app);
                else if (v.getContext() instanceof StoreMainActivity) {
                    ((StoreMainActivity) v.getContext()).openAppDetails(app);
                }
            });
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof AppViewHolder) {
            Glide.with(((AppViewHolder) holder).itemView.getContext()).clear(((AppViewHolder) holder).icon);
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.categoryTitle);
        }
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        TextView title, developer, rating, downloads, price;
        ImageView icon;
        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.itemAppTitle);
            developer = itemView.findViewById(R.id.itemAppUser);
            rating = itemView.findViewById(R.id.itemRating);
            downloads = itemView.findViewById(R.id.itemDownloads);
            price = itemView.findViewById(R.id.itemPrice);
            icon = itemView.findViewById(R.id.itemAppIcon);
        }
    }
}
