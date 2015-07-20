package com.madbeeapp.android.FriendsListFragment;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.madbeeapp.android.Helpers.FriendContactListHelper;
import com.madbeeapp.android.Helpers.TypefaceHelper;
import com.madbeeapp.android.MainActivity.MainActivity;
import com.madbeeapp.android.R;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class FriendsContactListViewCardsAdapter extends BaseAdapter {

    public static ListViewHolder mHolder = null;
    String mType;
    ArrayList<FriendContactListHelper> mFriendContactListHelpers;
    ImageLoader mImageLoader;
    private Context mContext;
    private MainActivity mainActivity;

    public FriendsContactListViewCardsAdapter(Context context, MainActivity mainActivity, String type, ArrayList<FriendContactListHelper> friendContactListHelpers) {
        super();
        mContext = context;
        this.mainActivity = mainActivity;
        mType = type;
        mFriendContactListHelpers = friendContactListHelpers;
        mImageLoader = new ImageLoader(context, mainActivity.getListPreferredItemHeight()) {
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
                } catch (FileNotFoundException ignored) {
                }
                return null;
            }
        };
    }

    @Override
    public int getCount() {
        return mFriendContactListHelpers.size();
    }

    @Override
    public Object getItem(int i) {
        return mFriendContactListHelpers.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    /**
     * Returns the individual row/child in the list/grid.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.friends_list_view_item, parent, false);
            mHolder = new ListViewHolder();
            mHolder.titleText = (TextView) convertView.findViewById(R.id.listViewTitleText);
            mHolder.subText = (TextView) convertView.findViewById(R.id.listViewSubText);
            mHolder.leftImage = (ImageView) convertView.findViewById(R.id.listViewLeftIcon);
            mHolder.titleText.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));
            mHolder.subText.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));
            mHolder.width = mainActivity.getListPreferredItemHeight();
            mHolder.paint = new Paint();
            mHolder.paint.setTextSize(mHolder.width / 3);
            mHolder.paint.setColor(mContext.getResources().getColor(android.R.color.white));
            mHolder.paint.setTextAlign(Paint.Align.LEFT);
            mHolder.paint.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));
            mHolder.baseline = (mHolder.width * 3) / 5;
            mHolder.image = Bitmap.createBitmap(mHolder.width, mHolder.width, Bitmap.Config.ARGB_8888);
            mHolder.canvas = new Canvas(mHolder.image);
            mHolder.colorArray = new ArrayList<>();
            mHolder.colorArray.add(mContext.getResources().getColor(R.color.red_500));
            mHolder.colorArray.add(mContext.getResources().getColor(R.color.green_500));
            mHolder.colorArray.add(mContext.getResources().getColor(R.color.orange_500));
            mHolder.colorArray.add(mContext.getResources().getColor(R.color.purple_500));
            mHolder.colorArray.add(mContext.getResources().getColor(R.color.blue_500));
            mHolder.colorArray.add(mContext.getResources().getColor(R.color.indigo_500));
            mHolder.colorArray.add(mContext.getResources().getColor(R.color.teal_500));
            mHolder.colorArray.add(mContext.getResources().getColor(R.color.lime_500));
            mHolder.colorArray.add(mContext.getResources().getColor(R.color.brown_500));
            mHolder.colorArray.add(mContext.getResources().getColor(R.color.blue_grey_500));
            convertView.setTag(mHolder);
        } else {
            mHolder = (ListViewHolder) convertView.getTag();
        }

        mHolder.titleText.setText(mFriendContactListHelpers.get(position).getName());
        if (mFriendContactListHelpers.get(position).isPresent()) {
            mHolder.subText.setText("Total Songs: " + mFriendContactListHelpers.get(position).getExtra());
            mHolder.subText.setTextColor(mContext.getResources().getColor(R.color.black_light));
        } else {
            mHolder.subText.setText("Send Invitation");
            mHolder.subText.setTextColor(mContext.getResources().getColor(R.color.primary));
        }
        if (mFriendContactListHelpers.get(position).getSubTitle() != null) {
            mImageLoader.loadImage(mFriendContactListHelpers.get(position).getSubTitle(), mHolder.leftImage);
        } else {
            mHolder.canvas.drawColor(mHolder.colorArray.get(position % 10));
            String temp;
            if (mFriendContactListHelpers.get(position).getName().contains(" ")) {
                String split[] = mFriendContactListHelpers.get(position).getName().split(" ");
                temp = split[0].substring(0, 1).toUpperCase() + split[split.length - 1].substring(0, 1).toUpperCase();
            } else {
                temp = mFriendContactListHelpers.get(position).getName().substring(0, 1).toUpperCase();
            }
            temp = temp.substring(0, 1).toUpperCase();
            mHolder.canvas.drawText(temp, mHolder.width * 2 / 5, mHolder.baseline, mHolder.paint);
            mHolder.leftImage.setImageBitmap(mHolder.image);
        }
        return convertView;
    }

    /**
     * Holder subclass for FriendsSongListViewCardsAdapter.
     *
     * @author Arpit Gandhi
     */
    static class ListViewHolder {
        public TextView titleText;
        public TextView subText;
        public ImageView leftImage;
        ArrayList<Integer> colorArray;
        Canvas canvas;
        int width;
        Paint paint;
        float baseline;
        Bitmap image;
    }
}
