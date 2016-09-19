package com.lanyi.library.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.FrameLayout;

/**
 * Author : Kay
 * Date   : 2015/6/8
 */
public class HelloLayout extends FrameLayout {

    //定义可以下拉刷新的高度，默认的高度都根据此变量设置
    private static final int CONSTANT_HEIGHT_REFRESH = 180;

    //刷新的主体
    private View viewTarget;
    //刷新、加载的动画效果 drawable
    private Hello drawable;

    //可以刷新的高度
    private int iRefreshHeight = CONSTANT_HEIGHT_REFRESH;
    //正在刷新时现显示的高度，小于刷新的最低限界高度
    private int iRefreshingHeight = CONSTANT_HEIGHT_REFRESH * 3 / 4;
    //用于处理是否隐藏了下拉刷新，只有两个值 0 或 iRefreshingHeight;
    private int iShowHeight = 0;
    //可以加载的高度
    private int iLoadHeight = CONSTANT_HEIGHT_REFRESH * 3 / 4;
    //正在加载的高度
    private int iLoadingHeight = CONSTANT_HEIGHT_REFRESH * 3 / 4;
    //是否处于下拉刷新
    private boolean bRefrshing = false;
    //是否显示正在刷新,下拉刷新提示框可以隐藏，此变量用来控制
    private boolean bRefrshingVisible = false;
    //是否正在加载更多
    private boolean bLoading = false;
    //是否显示加载更多,加载更多提示框可以隐藏，此变量用来控制
    private boolean bLoadingVisible = false;
    //已经加载全部
    private boolean bLoadAll = false;
    //target top端偏移是否存在
    private boolean top;
    //target bottom端偏移是否存在
    private boolean bottom;

    //touch事件是否交由自己处理，用于intercept的返回值
    private boolean bHandleBySelf = false;
    //viewTarget在拖动的时的偏移量
    private int iTargetOffset;
    //松手时，body距离顶部的高度
    private int iBackHeight;
    //用于记录切换手指时上一手指位置
    private float fPreY;
    //当前位置
    private float fNowY;
    //开始位置
    private float fDownY;
    //有效手指id
    private int iActivePointerId;
    //target可以的最大偏移量
    private float fMaxOffsetHeight;
    //认为滚动的最低限界
    private int iTouchSlop;
    private Animation mAnimBack;

    //回调监听接口,可以用于控制是否允许下拉刷新或加载更多
    private OnHelloRefreshListener mHelloRefreshListener = null;
    private OnHelloLoadListener mHelloLoadListener = null;

    public HelloLayout(Context context) {
        this(context, null);
        init(context);
    }

    public HelloLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HelloLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    //初始化
    private void init(Context context) {

        iTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop() / 3;

        setWillNotDraw(false);
        ViewCompat.setChildrenDrawingOrderEnabled(this, true);
    }

    /**
     * 指定响应hellolayout的TargetView
     *
     * @param target
     */
    public void specifyTarget(View target) {

        if (target == null)
            throw new NullPointerException("target is null");

        if (findViewById(target.getId()) == null)
            throw new NullPointerException("target is not the childview of hellolayout");

        viewTarget = target;
    }

    /**
     * 指定target，设置target可点击，避免TextView等类型的子空间不响应Touch导致逻辑失败
     *
     * @return true表示target不为空
     */
    private boolean prepare() {

        if (viewTarget != null)
            return true;

        //若指定id的view未找到,默认选择child0
        if (viewTarget == null && getChildCount() > 0)
            viewTarget = getChildAt(0);
        /*
        若viewTarget即子view若为TextView等不响应TouchEvent的控件,
        即使this.onInterceptTouchEvent返回false,将Event事件分发给子View,
        Event仍然会被重新分配给this.onTouchEvent,
        致使this.onInterceptTouchEvent无法接受到后续的MOVE,
        因此设置子view可点击，强迫子控件处理，this.onInterceptTouchEvent分发下去的Event
        */
        if (viewTarget != null) {
            viewTarget.setClickable(true);
            return true;
        }

        return false;
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (!prepare()) return;
        fMaxOffsetHeight = getMeasuredHeight();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        if (!prepare()) {
            return super.dispatchTouchEvent(event);
        }


        final int action = MotionEventCompat.getActionMasked(event);
        int index = MotionEventCompat.getActionIndex(event);
        switch (action) {

            case MotionEvent.ACTION_DOWN:
                fPreY = MotionEventCompat.getY(event, index);
                iActivePointerId = MotionEventCompat.getPointerId(event, index);
                Log.d("Kay", "down_________y=" + event.getY() + " top=" + viewTarget.getTop() + "__view Scroll=" + viewTarget.getScrollY());
                break;

            case MotionEvent.ACTION_MOVE:
                //滑动有效点(手指)改变时index改变id不改变
                int actionIndex = MotionEventCompat.getActionIndex(event);
                int actionId = MotionEventCompat.getPointerId(event, actionIndex);
                //检查有效触点是否改变
                //未执行up事件,切换滑动手指,重置fPreY修正到有效滑动手指落点
                if (iActivePointerId != actionId) {
                    iActivePointerId = actionId;
                    fPreY = MotionEventCompat.getY(event, actionIndex);
                }
                fNowY = MotionEventCompat.getY(event, actionIndex);
                float diffY = fNowY - fPreY;
//                Log.d("Kay", "滑动--距离:" + (int) diffY
//                        + " 方向:" + (diffY > 0 ? "从上往下" : "从下往上")
//                        + " 子View--从上往下:" + ViewCompat.canScrollVertically(viewTarget, -1)
//                        + " 从下往上:" + ViewCompat.canScrollVertically(viewTarget, 1));
                //手指由上往下滑动
                fPreY = fNowY;
                Log.d("Kay", "move_________y=" + event.getY() + " offset=" + diffY + " top=" + viewTarget.getTop() + "__view Scroll=" + viewTarget.getScrollY());
                //viewTarget 没有偏移的情况下
                if (viewTarget.getTop() == 0) {
                    //viewTarget 不能由上往下滚动,且手指移动方向为由上往下
                    if ((!ViewCompat.canScrollVertically(viewTarget, -1) && diffY > iTouchSlop)
                            //viewTarget 不能由下往上滚动,且手指移动方向为由下往上
                            || (!ViewCompat.canScrollVertically(viewTarget, 1) && diffY < -iTouchSlop)) {
                        viewTarget.clearAnimation();
                        setTargetOffset(getOffsetWithRes((int) diffY));
                        return true;
                    }
                }
                //只要viewTarget处于偏移中,所用的touch拦截,不交给viewTarget
                else {
                    viewTarget.clearAnimation();
                    setTargetOffset(getOffsetWithRes((int) diffY));
                    return true;
                }
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                Log.d("Kay", "up_________y=" + event.getY() + " top=" + viewTarget.getTop() + "__view Scroll=" + viewTarget.getScrollY());
                back();
                break;
        }

        return super.dispatchTouchEvent(event);
    }

    /**
     * 根据滑动距离附加阻力后计算位移偏量,滑动距离越大，阻力越大
     *
     * @param offset 每次手指滑动移偏量
     * @return 实际偏移量
     */
    private int getOffsetWithRes(int offset) {

        float curOffset = Math.abs(Math.min(iTargetOffset, fMaxOffsetHeight - 5));
        //如果offset为增长增量使用系数阻力
        if (curOffset * offset > 0) {
            float leftScale = (fMaxOffsetHeight - curOffset) / fMaxOffsetHeight;
            return (int) (offset * (float) (1.0f - Math.pow((1.0f - leftScale), 0.6f)));
        }
        //如果是减少增量使用线性阻力
        else {
            return offset / 2;
        }
    }


    /**
     * 设置并开始返回动画
     */
    public void back() {
        //设置返回时的最大高度
        iBackHeight = iTargetOffset;
        if (mAnimBack == null) {
            mAnimBack = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    offsetBack(interpolatedTime);
                }
            };
            mAnimBack.setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));
            mAnimBack.setInterpolator(new DecelerateInterpolator(2f));
            mAnimBack.setAnimationListener(new Animation.AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    iTargetOffset = viewTarget.getTop();
                }
            });
        }
        mAnimBack.reset();
        viewTarget.startAnimation(mAnimBack);
    }

    /**
     * 根据给定的offset设置target的偏移量
     *
     * @param offset
     */
    private void setTargetOffset(int offset) {

        //获取当前偏移量
        int curOffset = viewTarget.getTop();
        //添加offset偏移增量后发生0点越界,修正offset = -curOffset
        //例如 curOffset = -4 offset = 5,增量后 curOffset = 1越过了0点,则需要修正offset  = -curOffset = 4
        if (((curOffset + offset) * curOffset) < 0) {
            offset = -curOffset;
        }

        viewTarget.offsetTopAndBottom(offset);
        iTargetOffset = viewTarget.getTop();
        if (drawable != null) {
            drawable.move(iTargetOffset, getMeasuredWidth(), getMeasuredHeight(), bRefrshing, bLoading, bLoadAll);
        }


    }

    /**
     * 根据animation的 interpolate 设置 target的偏移量
     */
    private void offsetBack(float interpolatedTime) {

        int targetOffset = iBackHeight - (int) ((iBackHeight - iShowHeight) * interpolatedTime);
        int offset = targetOffset - iTargetOffset;
        setTargetOffset(offset);
    }


    /**
     * 重置下拉刷新和加载更多的条件，下拉刷新完成时调用
     */
    public void reset() {

        bLoadAll = false;
        complete();
    }

    /**
     * 加载更多完成是调用
     */
    public void complete() {

        bRefrshing = false;
        bRefrshingVisible = false;
        bLoading = false;
        bLoadingVisible = false;
        iShowHeight = 0;
        back();
    }

    /**
     * 已经加载全部，不再允许加载更多
     */
    public void hasLoadAll() {

        bLoadAll = true;
    }

    public void setOnHelloRefreshListener(OnHelloRefreshListener listener) {
        mHelloRefreshListener = listener;

        if (drawable != null) {
            if (mHelloRefreshListener != null) {
                drawable.refresh(true);
            } else {
                drawable.refresh(false);
            }
        }
    }

    public void setOnHelloLoadListener(OnHelloLoadListener listener) {
        mHelloLoadListener = listener;

        if (drawable != null) {
            if (mHelloLoadListener != null) {
                drawable.load(true);
            } else {
                drawable.load(false);
            }
        }
    }


    /**
     * 下拉刷新接口
     */
    public interface OnHelloRefreshListener {

        void onRefresh();
    }

    /**
     * 加载更多接口
     */
    public interface OnHelloLoadListener {

        void onLoad();
    }

    /**
     *
     */
    public abstract static class Hello extends Drawable {

        public abstract void move(int offset, int width, int height, boolean refreshing, boolean loading, boolean loadAll);

        public abstract void refresh(boolean refresh);

        public abstract void load(boolean load);

    }

    public void setHello(Hello hello) {

        this.drawable = hello;
        setBackgroundDrawable(drawable);
    }

}
