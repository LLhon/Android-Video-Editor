package com.marvhong.videoeditor.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.marvhong.videoeditor.R;
import com.marvhong.videoeditor.model.LocalVideoModel;
import com.marvhong.videoeditor.utils.UIUtils;
import com.marvhong.videoeditor.utils.VideoUtil;
import java.util.List;

/**
 * @author LLhon
 * @Project diaoyur_android
 * @Package com.marvhong.videoeditor
 * @Date 2018/8/21 15:16
 * @description
 */
public class VideoGridAdapter extends RecyclerView.Adapter<VideoGridAdapter.VideoHolder> {

    private Context mContext;
    private List<LocalVideoModel> mDatas;
    private OnItemClickListener mOnItemClickListener;

    public VideoGridAdapter(Context context, List<LocalVideoModel> data) {
        mContext = context;
        mDatas = data;
    }

    public void setData(List<LocalVideoModel> datas) {
        mDatas = datas;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mDatas == null ? 0 : mDatas.size();
    }

    @Override
    public VideoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new VideoHolder(
            LayoutInflater.from(mContext).inflate(R.layout.item_grid_video, null, false));
    }

    @Override
    public void onBindViewHolder(VideoHolder holder, int position) {
        LocalVideoModel model = mDatas.get(position);
        Glide.with(mContext)
            .load(VideoUtil.getVideoFilePath(model.getVideoPath()))
            .into(holder.mIv);

        holder.mTvDuration.setText(VideoUtil.convertSecondsToTime(model.getDuration() / 1000));
        holder.itemView.setOnClickListener(v -> {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(position, model);
            }
        });
    }

    class VideoHolder extends RecyclerView.ViewHolder {

        ImageView mIv;
        TextView mTvDuration;

        public VideoHolder(View itemView) {
            super(itemView);
            mIv = itemView.findViewById(R.id.iv);
            mTvDuration = itemView.findViewById(R.id.tv_duration);
            int size = UIUtils.getScreenWidth() / 4;
            FrameLayout.LayoutParams params = (LayoutParams) mIv.getLayoutParams();
            params.width = size;
            params.height = size;
            mIv.setLayoutParams(params);
        }
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(int position, LocalVideoModel model);
    }
}
