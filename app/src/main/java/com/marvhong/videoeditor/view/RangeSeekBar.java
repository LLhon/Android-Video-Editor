package com.marvhong.videoeditor.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import com.marvhong.videoeditor.R;
import com.marvhong.videoeditor.utils.UIUtils;
import java.text.DecimalFormat;


/**
 * ================================================
 * 作    者：顾修忠-guxiuzhong@youku.com/gfj19900401@163.com
 * 版    本：
 * 创建日期：2017/4/4-下午1:22
 * 描    述：
 * 修订历史：
 * ================================================
 */

public class RangeSeekBar extends View {

    private static final String TAG = RangeSeekBar.class.getSimpleName();
    private double absoluteMinValuePrim, absoluteMaxValuePrim;
    private double normalizedMinValue = 0d;//点坐标占总长度的比例值，范围从0-1
    private double normalizedMaxValue = 1d;//点坐标占总长度的比例值，范围从0-1
    private long min_cut_time = 3000;
    private double normalizedMinValueTime = 0d;
    private double normalizedMaxValueTime = 1d;// normalized：规格化的--点坐标占总长度的比例值，范围从0-1
    private int mScaledTouchSlop;
    private Bitmap mBitmapBlack;
    private Bitmap mBitmapPro;
    private Paint paint;
    private Paint rectPaint;
    private Paint mGrayPaint;
    private Paint mShadowPaint;
    private int thumbWidth;
    private int thumbHeight;
    private float thumbHalfWidth;
    private final float padding = 0;
    private int top_bottom_border_height; //上下两边边框高度
    private int left_right_gray_width; //左右两边灰色线条宽度
    private int left_right_gray_height; //左右两边灰色线条高度
    private int left_right_gray_margin; //左右两边灰色线条距离顶部的间距

    private float thumbPaddingTop = 0;
    private float thumbPressPaddingTop = 0;
    private boolean isTouchDown;
    public static final int INVALID_POINTER_ID = 255;
    public static final int ACTION_POINTER_INDEX_MASK = 0x0000ff00, ACTION_POINTER_INDEX_SHIFT = 8;
    private int mActivePointerId = INVALID_POINTER_ID;
    private float mDownMotionX;
    private boolean mIsDragging;
    private Thumb pressedThumb;
    private boolean isMin;
    private double min_width = 1;//最小裁剪距离
    private boolean notifyWhileDragging = false;

    public enum Thumb {
        MIN, MAX
    }

    public RangeSeekBar(Context context) {
        super(context);
    }

    public RangeSeekBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RangeSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RangeSeekBar(Context context, long absoluteMinValuePrim, long absoluteMaxValuePrim) {
        super(context);
        this.absoluteMinValuePrim = absoluteMinValuePrim;
        this.absoluteMaxValuePrim = absoluteMaxValuePrim;
        setFocusable(true);
        setFocusableInTouchMode(true);
        init();
    }

    private void init() {
        mScaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        thumbWidth = UIUtils.dp2Px(8);
        thumbHeight = UIUtils.dp2Px(62);
        thumbHalfWidth = thumbWidth / 2;
        left_right_gray_width = UIUtils.dp2Px(2);
        left_right_gray_height = UIUtils.dp2Px(20);
        left_right_gray_margin = UIUtils.dp2Px(21);
        top_bottom_border_height = UIUtils.dp2Px(3);

        mBitmapBlack = BitmapFactory.decodeResource(getResources(), R.drawable.video_overlay_black);
        mBitmapPro = BitmapFactory.decodeResource(getResources(), R.drawable.video_overlay_trans);
        //绘制黑色遮罩
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        //绘制白色边框
        rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectPaint.setStyle(Paint.Style.FILL);
        rectPaint.setColor(Color.parseColor("#ffffff"));
        //绘制左右两边灰色线条
        mGrayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGrayPaint.setColor(getContext().getResources().getColor(R.color.cccccc));
        //绘制半透明遮罩
        mShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mShadowPaint.setColor(getContext().getResources().getColor(R.color.ff7f000000));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 300;
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec)) {
            width = MeasureSpec.getSize(widthMeasureSpec);
        }
        int height = 120;
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)) {
            height = MeasureSpec.getSize(heightMeasureSpec);
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float bg_middle_left = 0;
        float bg_middle_right = getWidth() - getPaddingRight();
        float scale = (bg_middle_right - bg_middle_left) / mBitmapPro.getWidth();

        float rangeL = normalizedToScreen(normalizedMinValue);
        float rangeR = normalizedToScreen(normalizedMaxValue);
        float scale_pro = (rangeR - rangeL) / mBitmapPro.getWidth();
        if (scale_pro > 0) {
            try {
                Matrix pro_mx = new Matrix();
                pro_mx.postScale(scale_pro, 1f);
                Bitmap m_bitmap_pro_new = Bitmap.createBitmap(mBitmapPro, 0, 0, mBitmapPro.getWidth(),
                        mBitmapPro.getHeight(), pro_mx, true);

                //画中间的透明遮罩
                canvas.drawBitmap(m_bitmap_pro_new, rangeL, thumbPaddingTop, paint);

                Matrix mx = new Matrix();
                mx.postScale(scale, 1f);
                Bitmap m_bitmap_black_new = Bitmap
                    .createBitmap(mBitmapBlack, 0, 0, mBitmapBlack.getWidth(), mBitmapBlack.getHeight(), mx, true);

                //画左边的半透明遮罩
                Bitmap m_bg_new1 = Bitmap
                    .createBitmap(m_bitmap_black_new, 0, 0, (int) (rangeL - bg_middle_left) + (int) thumbWidth / 2, mBitmapBlack.getHeight());
                canvas.drawBitmap(m_bg_new1, bg_middle_left, thumbPaddingTop, paint);

                //画右边的半透明遮罩
                Bitmap m_bg_new2 = Bitmap
                    .createBitmap(m_bitmap_black_new, (int) (rangeR - thumbWidth / 2), 0, (int) (getWidth() - rangeR) + (int) thumbWidth / 2, mBitmapBlack.getHeight());
                canvas.drawBitmap(m_bg_new2, (int) (rangeR - thumbWidth / 2), thumbPaddingTop, paint);

                //画上下的矩形
                canvas.drawRect(rangeL, thumbPaddingTop, rangeR, thumbPaddingTop + dip2px(2), rectPaint);
                canvas.drawRect(rangeL, getHeight() - dip2px(2), rangeR, getHeight(), rectPaint);

                //画左右thumb
//                drawThumb(normalizedToScreen(normalizedMinValue), false, canvas, true);
//                drawThumb(normalizedToScreen(normalizedMaxValue), false, canvas, false);
                canvas.drawRect(normalizedToScreen(normalizedMinValue),
                    thumbPaddingTop,
                    normalizedToScreen(normalizedMinValue) + thumbWidth,
                    thumbPaddingTop + thumbHeight, rectPaint);
                canvas.drawRect(normalizedToScreen(normalizedMaxValue) - thumbWidth,
                    thumbPaddingTop,
                    normalizedToScreen(normalizedMaxValue),
                    thumbPaddingTop + thumbHeight, rectPaint);
                canvas.drawRect(normalizedToScreen(normalizedMinValue) + top_bottom_border_height,
                    thumbPaddingTop + left_right_gray_margin,
                    normalizedToScreen(normalizedMinValue) + top_bottom_border_height + left_right_gray_width,
                    thumbPaddingTop + left_right_gray_margin + left_right_gray_height, mGrayPaint);
                canvas.drawRect(normalizedToScreen(normalizedMaxValue) - thumbWidth + top_bottom_border_height,
                    thumbPaddingTop + left_right_gray_margin,
                    normalizedToScreen(normalizedMaxValue) - thumbWidth + top_bottom_border_height + left_right_gray_width,
                    thumbPaddingTop + left_right_gray_margin + left_right_gray_height, mGrayPaint);
            } catch (Exception e) {
                // 当pro_scale非常小，例如width=12，Height=48，pro_scale=0.01979065时，
                // 宽高按比例计算后值为0.237、0.949，系统强转为int型后宽就变成0了。就出现非法参数异常
                Log.e(TAG,
                        "IllegalArgumentException--width=" + mBitmapPro.getWidth() + "Height=" + mBitmapPro.getHeight()
                                + "scale_pro=" + scale_pro, e);
            }
        }
    }


    private void drawThumb(float screenCoord, boolean pressed, Canvas canvas, boolean isLeft) {
//        canvas.drawBitmap(pressed ? thumbPressedImage : (isLeft ? thumbImageLeft : thumbImageRight), screenCoord - (isLeft ? 0 : thumbWidth), (pressed ? thumbPressPaddingTop : thumbPaddingTop), paint);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isTouchDown) {
            return super.onTouchEvent(event);
        }
        if (event.getPointerCount() > 1) {
            return super.onTouchEvent(event);
        }

        if (!isEnabled())
            return false;
        if (absoluteMaxValuePrim <= min_cut_time) {
            return super.onTouchEvent(event);
        }
        int pointerIndex;// 记录点击点的index
        final int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                //记住最后一个手指点击屏幕的点的坐标x，mDownMotionX
                mActivePointerId = event.getPointerId(event.getPointerCount() - 1);
                pointerIndex = event.findPointerIndex(mActivePointerId);
                mDownMotionX = event.getX(pointerIndex);
                // 判断touch到的是最大值thumb还是最小值thumb
                pressedThumb = evalPressedThumb(mDownMotionX);
                if (pressedThumb == null)
                    return super.onTouchEvent(event);
                setPressed(true);// 设置该控件被按下了
                onStartTrackingTouch();// 置mIsDragging为true，开始追踪touch事件
                trackTouchEvent(event);
                attemptClaimDrag();
                if (listener != null) {
                    listener.onRangeSeekBarValuesChanged(this, getSelectedMinValue(), getSelectedMaxValue(), MotionEvent.ACTION_DOWN, isMin, pressedThumb);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (pressedThumb != null) {
                    if (mIsDragging) {
                        trackTouchEvent(event);
                    } else {
                        // Scroll to follow the motion event
                        pointerIndex = event.findPointerIndex(mActivePointerId);
                        final float x = event.getX(pointerIndex);// 手指在控件上点的X坐标
                        // 手指没有点在最大最小值上，并且在控件上有滑动事件
                        if (Math.abs(x - mDownMotionX) > mScaledTouchSlop) {
                            setPressed(true);
                            Log.e(TAG, "没有拖住最大最小值");// 一直不会执行？
                            invalidate();
                            onStartTrackingTouch();
                            trackTouchEvent(event);
                            attemptClaimDrag();
                        }
                    }
                    if (notifyWhileDragging && listener != null) {
                        listener.onRangeSeekBarValuesChanged(this, getSelectedMinValue(), getSelectedMaxValue(), MotionEvent.ACTION_MOVE, isMin, pressedThumb);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                }

                invalidate();
                if (listener != null) {
                    listener.onRangeSeekBarValuesChanged(this, getSelectedMinValue(), getSelectedMaxValue(), MotionEvent.ACTION_UP, isMin, pressedThumb);
                }
                pressedThumb = null;// 手指抬起，则置被touch到的thumb为空
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                final int index = event.getPointerCount() - 1;
                // final int index = ev.getActionIndex();
                mDownMotionX = event.getX(index);
                mActivePointerId = event.getPointerId(index);
                invalidate();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate(); // see above explanation
                break;
            default:
                break;
        }
        return true;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;

        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mDownMotionX = ev.getX(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }
    }

    private void trackTouchEvent(MotionEvent event) {
        if (event.getPointerCount() > 1) return;
        Log.e(TAG, "trackTouchEvent: " + event.getAction() + " x: " + event.getX());
        final int pointerIndex = event.findPointerIndex(mActivePointerId);// 得到按下点的index
        float x = 0;
        try {
            x = event.getX(pointerIndex);
        } catch (Exception e) {
            return;
        }
        if (Thumb.MIN.equals(pressedThumb)) {
            // screenToNormalized(x)-->得到规格化的0-1的值
            setNormalizedMinValue(screenToNormalized(x, 0));
        } else if (Thumb.MAX.equals(pressedThumb)) {
            setNormalizedMaxValue(screenToNormalized(x, 1));
        }
    }

    private double screenToNormalized(float screenCoord, int position) {
        int width = getWidth();
        if (width <= 2 * padding) {
            // prevent division by zero, simply return 0.
            return 0d;
        } else {
            isMin = false;
            double current_width = screenCoord;
            float rangeL = normalizedToScreen(normalizedMinValue);
            float rangeR = normalizedToScreen(normalizedMaxValue);
            double min = min_cut_time / (absoluteMaxValuePrim - absoluteMinValuePrim) * (width - thumbWidth * 2);

            if (absoluteMaxValuePrim > 5 * 60 * 1000) {//大于5分钟的精确小数四位
                DecimalFormat df = new DecimalFormat("0.0000");
                min_width = Double.parseDouble(df.format(min));
            } else {
                min_width = Math.round(min + 0.5d);
            }
            if (position == 0) {
                if (isInThumbRangeLeft(screenCoord, normalizedMinValue, 0.5)) {
                    return normalizedMinValue;
                }

                float rightPosition = (getWidth() - rangeR) >= 0 ? (getWidth() - rangeR) : 0;
                double left_length = getValueLength() - (rightPosition + min_width);


                if (current_width > rangeL) {
                    current_width = rangeL + (current_width - rangeL);
                } else if (current_width <= rangeL) {
                    current_width = rangeL - (rangeL - current_width);
                }

                if (current_width > left_length) {
                    isMin = true;
                    current_width = left_length;
                }

                if (current_width < thumbWidth * 2 / 3) {
                    current_width = 0;
                }

                double resultTime = (current_width - padding) / (width - 2 * thumbWidth);
                normalizedMinValueTime = Math.min(1d, Math.max(0d, resultTime));
                double result = (current_width - padding) / (width - 2 * padding);
                return Math.min(1d, Math.max(0d, result));// 保证该该值为0-1之间，但是什么时候这个判断有用呢？
            } else {
                if (isInThumbRange(screenCoord, normalizedMaxValue, 0.5)) {
                    return normalizedMaxValue;
                }

                double right_length = getValueLength() - (rangeL + min_width);
                if (current_width > rangeR) {
                    current_width = rangeR + (current_width - rangeR);
                } else if (current_width <= rangeR) {
                    current_width = rangeR - (rangeR - current_width);
                }

                double paddingRight = getWidth() - current_width;

                if (paddingRight > right_length) {
                    isMin = true;
                    current_width = getWidth() - right_length;
                    paddingRight = right_length;
                }

                if (paddingRight < thumbWidth * 2 / 3) {
                    current_width = getWidth();
                    paddingRight = 0;
                }

                double resultTime = (paddingRight - padding) / (width - 2 * thumbWidth);
                resultTime = 1 - resultTime;
                normalizedMaxValueTime = Math.min(1d, Math.max(0d, resultTime));
                double result = (current_width - padding) / (width - 2 * padding);
                return Math.min(1d, Math.max(0d, result));// 保证该该值为0-1之间，但是什么时候这个判断有用呢？
            }

        }
    }

    private int getValueLength() {
        return (getWidth() - 2 * thumbWidth);
    }

    /**
     * 计算位于哪个Thumb内
     *
     * @param touchX touchX
     * @return 被touch的是空还是最大值或最小值
     */
    private Thumb evalPressedThumb(float touchX) {
        Thumb result = null;
        boolean minThumbPressed = isInThumbRange(touchX, normalizedMinValue, 2);// 触摸点是否在最小值图片范围内
        boolean maxThumbPressed = isInThumbRange(touchX, normalizedMaxValue, 2);
        if (minThumbPressed && maxThumbPressed) {
            // 如果两个thumbs重叠在一起，无法判断拖动哪个，做以下处理
            // 触摸点在屏幕右侧，则判断为touch到了最小值thumb，反之判断为touch到了最大值thumb
            result = (touchX / getWidth() > 0.5f) ? Thumb.MIN : Thumb.MAX;
        } else if (minThumbPressed) {
            result = Thumb.MIN;
        } else if (maxThumbPressed) {
            result = Thumb.MAX;
        }
        return result;
    }

    private boolean isInThumbRange(float touchX, double normalizedThumbValue, double scale) {
        // 当前触摸点X坐标-最小值图片中心点在屏幕的X坐标之差<=最小点图片的宽度的一般
        // 即判断触摸点是否在以最小值图片中心为原点，宽度一半为半径的圆内。
        return Math.abs(touchX - normalizedToScreen(normalizedThumbValue)) <= thumbHalfWidth * scale;
    }

    private boolean isInThumbRangeLeft(float touchX, double normalizedThumbValue, double scale) {
        // 当前触摸点X坐标-最小值图片中心点在屏幕的X坐标之差<=最小点图片的宽度的一般
        // 即判断触摸点是否在以最小值图片中心为原点，宽度一半为半径的圆内。
        return Math.abs(touchX - normalizedToScreen(normalizedThumbValue) - thumbWidth) <= thumbHalfWidth * scale;
    }

    /**
     * 试图告诉父view不要拦截子控件的drag
     */
    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    void onStartTrackingTouch() {
        mIsDragging = true;
    }


    void onStopTrackingTouch() {
        mIsDragging = false;
    }

    public void setMin_cut_time(long min_cut_time) {
        this.min_cut_time = min_cut_time;
    }


    private float normalizedToScreen(double normalizedCoord) {
        return (float) (getPaddingLeft() + normalizedCoord * (getWidth() - getPaddingLeft() - getPaddingRight()));
    }

    private double valueToNormalized(long value) {
        if (0 == absoluteMaxValuePrim - absoluteMinValuePrim) {
            return 0d;
        }
        return (value - absoluteMinValuePrim) / (absoluteMaxValuePrim - absoluteMinValuePrim);
    }

    public void setSelectedMinValue(long value) {
        if (0 == (absoluteMaxValuePrim - absoluteMinValuePrim)) {
            setNormalizedMinValue(0d);
        } else {
            setNormalizedMinValue(valueToNormalized(value));
        }
    }

    public void setSelectedMaxValue(long value) {
        if (0 == (absoluteMaxValuePrim - absoluteMinValuePrim)) {
            setNormalizedMaxValue(1d);
        } else {
            setNormalizedMaxValue(valueToNormalized(value));
        }
    }

    public void setNormalizedMinValue(double value) {
        normalizedMinValue = Math.max(0d, Math.min(1d, Math.min(value, normalizedMaxValue)));
        invalidate();// 重新绘制此view
    }


    public void setNormalizedMaxValue(double value) {
        normalizedMaxValue = Math.max(0d, Math.min(1d, Math.max(value, normalizedMinValue)));
        invalidate();// 重新绘制此view
    }


    public long getSelectedMinValue() {
        return normalizedToValue(normalizedMinValueTime);
    }

    public long getSelectedMaxValue() {
        return normalizedToValue(normalizedMaxValueTime);
    }

    private long normalizedToValue(double normalized) {
        return (long) (absoluteMinValuePrim + normalized
                * (absoluteMaxValuePrim - absoluteMinValuePrim));
    }

    /**
     * 供外部activity调用，控制是都在拖动的时候打印log信息，默认是false不打印
     */
    public boolean isNotifyWhileDragging() {
        return notifyWhileDragging;
    }


    public void setNotifyWhileDragging(boolean flag) {
        this.notifyWhileDragging = flag;
    }

    public int dip2px(int dip) {
        float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) ((float) dip * scale + 0.5F);
    }

    public void setTouchDown(boolean touchDown) {
        isTouchDown = touchDown;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable("SUPER", super.onSaveInstanceState());
        bundle.putDouble("MIN", normalizedMinValue);
        bundle.putDouble("MAX", normalizedMaxValue);
        bundle.putDouble("MIN_TIME", normalizedMinValueTime);
        bundle.putDouble("MAX_TIME", normalizedMaxValueTime);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcel) {
        final Bundle bundle = (Bundle) parcel;
        super.onRestoreInstanceState(bundle.getParcelable("SUPER"));
        normalizedMinValue = bundle.getDouble("MIN");
        normalizedMaxValue = bundle.getDouble("MAX");
        normalizedMinValueTime = bundle.getDouble("MIN_TIME");
        normalizedMaxValueTime = bundle.getDouble("MAX_TIME");
    }

    private OnRangeSeekBarChangeListener listener;

    public interface OnRangeSeekBarChangeListener {
        void onRangeSeekBarValuesChanged(RangeSeekBar bar, long minValue, long maxValue, int action,
            boolean isMin, Thumb pressedThumb);
    }

    public void setOnRangeSeekBarChangeListener(OnRangeSeekBarChangeListener listener) {
        this.listener = listener;
    }
}
