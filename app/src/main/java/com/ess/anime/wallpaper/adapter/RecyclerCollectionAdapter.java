package com.ess.anime.wallpaper.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Priority;
import com.ess.anime.wallpaper.R;
import com.ess.anime.wallpaper.bean.CollectionBean;
import com.ess.anime.wallpaper.glide.GlideApp;
import com.ess.anime.wallpaper.listener.OnTouchScaleListener;
import com.ess.anime.wallpaper.utils.FileUtils;
import com.ess.anime.wallpaper.utils.UIUtils;
import com.ess.anime.wallpaper.utils.VibratorUtils;
import com.mixiaoxiao.smoothcompoundbutton.SmoothCheckBox;

import java.util.ArrayList;

public class RecyclerCollectionAdapter extends RecyclerView.Adapter<RecyclerCollectionAdapter.MyViewHolder> {

    private Context mContext;
    private ArrayList<CollectionBean> mCollectionList;
    private ArrayList<CollectionBean> mChooseList;
    private boolean mEditing;
    private OnActionListener mActionListener;

    public RecyclerCollectionAdapter(Context context, ArrayList<CollectionBean> collectionList) {
        mContext = context;
        mCollectionList = collectionList;
        mChooseList = new ArrayList<>();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.recyclerview_item_collection, parent, false);
        MyViewHolder holder = new MyViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        final CollectionBean collectionBean = mCollectionList.get(position);
        String imageUrl = collectionBean.url;

        // 编辑模式选择框
        int visible = mEditing ? View.VISIBLE : View.GONE;
        holder.cbChoose.setVisibility(visible);
        holder.cbChoose.setChecked(mEditing && collectionBean.isChecked, true, false);
        holder.cbChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = holder.cbChoose.isChecked();
                setChooseState(collectionBean, isChecked);
            }
        });

        // 编辑模式放大查看
        holder.ivEnlarge.setVisibility(visible);
        holder.ivEnlarge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActionListener != null) {
                    mActionListener.onEnlarge(position);
                }
            }
        });

        // 图片格式标记
        int tagResId = 0;
        if (FileUtils.isImageType(imageUrl) && imageUrl.toLowerCase().endsWith("gif")) {
            tagResId = R.drawable.ic_tag_gif;
        } else if (FileUtils.isVideoType(imageUrl)) {
            tagResId = R.drawable.ic_tag_video;
        }
        holder.ivTag.setImageResource(tagResId);

        // 图片
        int slideLength = (int) ((UIUtils.getScreenWidth(mContext) - UIUtils.dp2px(mContext, 6)) / 3f);
        holder.ivCollection.getLayoutParams().width = slideLength;
        holder.ivCollection.getLayoutParams().height = slideLength;
        GlideApp.with(mContext)
                .asBitmap()
                .load(imageUrl)
                .priority(Priority.HIGH)
                .into(holder.ivCollection);

        holder.ivCollection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditing) {
                    boolean isChecked = !holder.cbChoose.isChecked();
                    holder.cbChoose.setChecked(isChecked, true, false);
                    setChooseState(collectionBean, isChecked);
                    if (mActionListener != null) {
                        mActionListener.onItemClick();
                    }
                } else {
                    if (mActionListener != null) {
                        mActionListener.onFullScreen(holder.ivCollection, position);
                    }
                }
            }
        });

        holder.ivCollection.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!mEditing) {
                    mEditing = true;
                    setChooseState(collectionBean, true);
                    notifyDataSetChanged();
                    VibratorUtils.Vibrate(mContext, 12);
                    if (mActionListener != null) {
                        mActionListener.onEdit();
                    }
                }
                return false;
            }
        });

        holder.ivCollection.setOnTouchListener(new OnTouchScaleListener(0.96f));
    }

    @Override
    public int getItemCount() {
        return mCollectionList == null ? 0 : mCollectionList.size();
    }

    public void addData(CollectionBean collectionBean) {
        mCollectionList.add(0, collectionBean);
        notifyItemInserted(0);
        notifyItemRangeChanged(0, mCollectionList.size());
    }

    public void removeData(CollectionBean collectionBean) {
        int position = mCollectionList.indexOf(collectionBean);
        if (position != -1) {
            mCollectionList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(0, mCollectionList.size());
        }
    }

    public void removeDatas(ArrayList<CollectionBean> deleteList) {
        for (CollectionBean collectionBean : deleteList) {
            int position = mCollectionList.indexOf(collectionBean);
            if (position != -1) {
                mCollectionList.remove(position);
                notifyItemRemoved(position);
            }
        }
        notifyItemRangeChanged(0, mCollectionList.size());
    }

    private void setChooseState(CollectionBean collectionBean, boolean isChecked) {
        collectionBean.isChecked = isChecked;
        if (isChecked) {
            if (!mChooseList.contains(collectionBean)) {
                mChooseList.add(collectionBean);
            }
        } else {
            mChooseList.remove(collectionBean);
        }
    }

    public ArrayList<CollectionBean> getCollectionList() {
        return mCollectionList;
    }

    public ArrayList<CollectionBean> getChooseList() {
        return mChooseList;
    }

    public int getChooseCount() {
        return mChooseList.size();
    }

    private void resetChooseList() {
        for (CollectionBean collectionBean : mCollectionList) {
            setChooseState(collectionBean, false);
        }
    }

    public void chooseAll() {
        for (CollectionBean collectionBean : mCollectionList) {
            setChooseState(collectionBean, true);
        }
        notifyDataSetChanged();
    }

    public void cancelChooseAll() {
        resetChooseList();
        notifyDataSetChanged();
    }

    public void beginEdit() {
        this.mEditing = true;
        notifyDataSetChanged();
    }

    public void cancelEdit(boolean notify) {
        this.mEditing = false;
        resetChooseList();
        if (notify) {
            notifyDataSetChanged();
        }
    }

    public boolean isEditing() {
        return mEditing;
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivCollection;
        private SmoothCheckBox cbChoose;
        private ImageView ivEnlarge;
        private ImageView ivTag;

        public MyViewHolder(View itemView) {
            super(itemView);
            ivCollection = (ImageView) itemView.findViewById(R.id.iv_collection);
            cbChoose = (SmoothCheckBox) itemView.findViewById(R.id.cb_choose);
            ivEnlarge = (ImageView) itemView.findViewById(R.id.iv_enlarge);
            ivTag = (ImageView) itemView.findViewById(R.id.iv_tag);
        }
    }

    public interface OnActionListener {
        //收藏界面adapter中点击后通知进入全屏查看模式
        void onFullScreen(ImageView imageView, int position);

        //收藏界面adapter中长按后通知toolbar切换编辑模式
        void onEdit();

        //收藏界面adapter中编辑模式下点击Enlarge图标放大查看图片
        void onEnlarge(int position);

        //收藏界面adapter中编辑模式下每次点击item同步更新选中数量
        void onItemClick();
    }

    public void setOnActionListener(OnActionListener listener) {
        mActionListener = listener;
    }
}
