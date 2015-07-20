package com.madbeeapp.android.Drawers;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.madbeeapp.android.FriendsListFragment.ImageLoader;
import com.madbeeapp.android.Helpers.TypefaceHelper;
import com.madbeeapp.android.MainActivity.DrawerHelper;
import com.madbeeapp.android.MainActivity.MainActivity;
import com.madbeeapp.android.R;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class NavigationDrawerAdapter extends BaseAdapter {

    ArrayList<DrawerHelper> mList;
    ImageLoader imageLoader;
    int selectedPosition;
    private Context mContext;

    public NavigationDrawerAdapter(Context context, ArrayList<DrawerHelper> list, int selectedPosition) {
        super();
        mContext = context;
        mList = list;
        this.selectedPosition = selectedPosition;

        imageLoader = new ImageLoader(context, ((MainActivity) mContext).getListPreferredItemHeight()) {
            @Override
            protected Bitmap processBitmap(Object data) {
                AssetFileDescriptor afd;
                try {
                    Uri thumbUri;
                    thumbUri = Uri.parse((String) data);
                    afd = mContext.getContentResolver().openAssetFileDescriptor(thumbUri, "r");

                    FileDescriptor fileDescriptor = afd.getFileDescriptor();

                    if (fileDescriptor != null) {
                        return decodeSampledBitmapFromDescriptor(
                                fileDescriptor, getImageSize(), getImageSize());
                    }
                } catch (FileNotFoundException ignore) {
                }
                return null;
            }
        };
        imageLoader.setLoadingImage(R.drawable.ic_contact);
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int i) {
        return mList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        SongsListViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.sliding_menu_browsers_layout, parent, false);
            holder = new SongsListViewHolder();
            holder.title = (TextView) convertView.findViewById(R.id.nav_drawer_item_title);
            holder.icon = (ImageView) convertView.findViewById(R.id.nav_drawer_item_icon);
            convertView.setTag(holder);
        } else {
            holder = (SongsListViewHolder) convertView.getTag();
        }

        holder.title.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));

        String title = mList.get(position).getTitle();
        if (title.equals("Trash")) {
            String styledText = "<font color='red'>Trash</font>";
            holder.title.setText(Html.fromHtml(styledText), TextView.BufferType.SPANNABLE);
        } else {
            holder.title.setText(title);
        }

        if (position == selectedPosition) {
            holder.title.setTextColor(mContext.getResources().getColor(R.color.teal_600));
        } else {
            holder.title.setTextColor(mContext.getResources().getColor(R.color.black_dark));
        }
        if (mList.get(position).getType().equals("friend")) {
            if (mList.get(position).getNumber().equals("trending")) {
                holder.icon.setImageResource(R.drawable.ic_trending);
            } else if (mList.get(position).getNumber().equals("likes")) {
                holder.icon.setImageResource(R.drawable.ic_like);
            } else if (mList.get(position).getNumber().equals("toptrending")) {
                holder.icon.setImageResource(R.drawable.ic_trending);
            } else {
                imageLoader.loadImage(mList.get(position).getIcon(), holder.icon);
            }
        } else {
            holder.icon.setImageResource(Integer.parseInt(mList.get(position).getIcon()));
        }
        return convertView;
    }

    static class SongsListViewHolder {
        public TextView title;
        public ImageView icon;
    }
}
