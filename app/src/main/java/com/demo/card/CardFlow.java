package com.demo.card;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ListAdapter;
import android.widget.OverScroller;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class CardFlow extends ViewGroup {

    private static final int TOUCH_STATE_REST = 0;
    private static final int TOUCH_STATE_SCROLL = 1;

    private int mDividerSize;

    private OverScroller mScroller;
    private VelocityTracker mVelocityTracker;
    private int mTouchSlop;
    private int mTouchState = TOUCH_STATE_REST;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private int mLastY, mLastX;

    private int mOverscrollDistance;
    private int mOverflingDistance;

    private int mOverScrollState = -1;
    private int mOverScrollRange;
    private int mOverScrollRefreshThres;

    private boolean isUnableToDrag = false;

    private OnScrollListener mOnScrollListener;

    public CardFlow(Context context) {
        super(context);
    }

    public CardFlow(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScroller = new OverScroller(context);
        ViewConfiguration config = ViewConfiguration.get(context);
        mTouchSlop = config.getScaledTouchSlop();
        mMinFlingVelocity = config.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = config.getScaledMaximumFlingVelocity();

        mOverscrollDistance = 50;
        mOverflingDistance = 50;

        mDividerSize = Utils.dp2px(15);

        setOverScrollMode(OVER_SCROLL_ALWAYS);
    }

    public void setAdapter(ListAdapter adapter) {
        for (int i = 0; i < adapter.getCount(); i++) {
            View content = adapter.getView(i, null, null);
            Card card = (Card) LayoutInflater.from(getContext()).inflate(R.layout.card, null);
            card.addView(content);
            addView(card);
        }
    }

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.mOnScrollListener = onScrollListener;
    }

    public void setDividerSize(int dividerSize) {
        mDividerSize = dividerSize;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec);
    }

    private int mTotalChildHeight = 0;

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        int top = getPaddingTop();
        mTotalChildHeight = 0;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                int childLeft = getPaddingLeft();
                int childRight = childLeft + child.getMeasuredWidth();
                int childTop = top;
                int childBottom = top + child.getMeasuredHeight();
                child.layout(childLeft, childTop, childRight, childBottom);
                top = childBottom + mDividerSize;
                mTotalChildHeight += child.getMeasuredHeight() + mDividerSize;
            }
        }
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (getVisibility() != View.VISIBLE) {
            return false;
        }
        if ((ev.getAction() & MotionEvent.ACTION_MASK) != MotionEvent.ACTION_DOWN) {
            if (isUnableToDrag) {
                return false;
            }
        }
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mLastY = (int) ev.getY();
                mLastX = (int) ev.getX();
                isUnableToDrag = false;
                if (mScroller.isFinished()) {
                    mTouchState = TOUCH_STATE_REST;
                    notifyScrollStateChangeListener(OnScrollListener.SCROLL_STATE_IDLE);
                } else {
                    mScroller.abortAnimation();
                    mTouchState = TOUCH_STATE_SCROLL;
                    notifyScrollStateChangeListener(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                    requestParentDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int diffY = (int) Math.abs(ev.getY() - mLastY);
                int diffX = (int) Math.abs(ev.getX() - mLastX);
                if (diffY > mTouchSlop && diffY > diffX) {
                    mTouchState = TOUCH_STATE_SCROLL;
                    notifyScrollStateChangeListener(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                    mLastY = (int) ev.getY();
                    requestParentDisallowInterceptTouchEvent(true);
                } else if (diffX > mTouchSlop) {
                    isUnableToDrag = true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                mTouchState = TOUCH_STATE_REST;
                notifyScrollStateChangeListener(OnScrollListener.SCROLL_STATE_IDLE);
                break;
        }
        return mTouchState != TOUCH_STATE_REST;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getVisibility() != View.VISIBLE) {
            return false;
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mLastY = (int) event.getY();
                mLastX = (int) event.getX();
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int y = (int) event.getY();
                if (mTouchState == TOUCH_STATE_REST) {
                    int diffY = Math.abs(y - mLastY);
                    int diffX = (int) Math.abs(event.getX() - mLastX);
                    if (diffY > mTouchSlop && diffY > diffX) {
                        mTouchState = TOUCH_STATE_SCROLL;
                        notifyScrollStateChangeListener(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                        mLastY = y;
                        requestParentDisallowInterceptTouchEvent(true);
                    }
                }

                if (mTouchState == TOUCH_STATE_SCROLL) {
                    int diff = y - mLastY;
                    int scrollY = getScrollY();
                    if (diff > 0) {
                        notifyScrollListener(true);
                        if (overScrollBy(0, -diff, 0, getScrollY(), 0, getScrollRange(), 0, scrollY > diff ? mOverScrollRange : mOverscrollDistance, true)) {
                            mVelocityTracker.clear();
                        }
                    } else if (diff < 0) {
                        notifyScrollListener(false);
                        if (overScrollBy(0, -diff, 0, getScrollY(), 0, getScrollRange(), 0, mOverScrollRange, true)) {
                            mVelocityTracker.clear();
                        }
                    }
                    mLastY = y;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mTouchState == TOUCH_STATE_SCROLL) {
                    final VelocityTracker tracker = mVelocityTracker;
                    tracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
                    float vy = tracker.getYVelocity();
                    int scrollRange = getScrollRange();

                    if (mOverScrollState == 1) {
                        scrollRange += mOverScrollRefreshThres;
                    }

                    if (Math.abs(vy) > mMinFlingVelocity) {
                        mScroller.fling(0, getScrollY(), 0, (int) -vy, 0, 0, 0, scrollRange, 0, mOverflingDistance);
                        notifyScrollStateChangeListener(OnScrollListener.SCROLL_STATE_FLING);
                        invalidate();
                    } else {
                        if (mScroller.springBack(0, getScrollY(), 0, 0, 0, scrollRange)) {
                            invalidate();
                        }
                        notifyScrollStateChangeListener(OnScrollListener.SCROLL_STATE_IDLE);
                    }

                }
                endTouch();
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mTouchState == TOUCH_STATE_SCROLL) {
                    if (mScroller.springBack(0, getScrollY(), 0, 0, 0, getScrollRange())) {
                        invalidate();
                    }
                    notifyScrollStateChangeListener(OnScrollListener.SCROLL_STATE_IDLE);
                }
                endTouch();
                break;
        }
        return true;
    }

    private void endTouch() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
        mTouchState = TOUCH_STATE_REST;
        isUnableToDrag = false;
    }

    private void requestParentDisallowInterceptTouchEvent(boolean disallowIntercept) {
        final ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        // Treat animating scrolls differently; see #computeScroll() for why.
        if (!mScroller.isFinished()) {
            super.scrollTo(scrollX, scrollY);
            if (clampedY) {
                mScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0, getScrollRange());
            }
        } else {
            scrollTo(scrollX, scrollY);
        }
        awakenScrollBars();
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int sy = mScroller.getCurrY();

            if (sy != getScrollY()) {
                notifyScrollListener(sy < getScrollY());
                if (mOnScrollListener != null) {
                    mOnScrollListener.onScroll(sy > getScrollY() ? 2 : 1, getScrollY(), getScrollY() + getHeight());
                }
            }

            scrollTo(0, sy);

            awakenScrollBars();
            postInvalidate();
        } else if (TOUCH_STATE_REST == mTouchState) {
            notifyScrollStateChangeListener(OnScrollListener.SCROLL_STATE_IDLE);
        }
    }

    private void notifyScrollListener(boolean isUp) {
        if (mOnScrollListener != null) {
            mOnScrollListener.onScroll(isUp ? 1 : 2, getScrollY(), getScrollY() + getHeight());
        }
    }

    private void notifyScrollStateChangeListener(int state) {
        if (mOnScrollListener != null) {
            mOnScrollListener.onScrollStateChanged(state);
        }
    }

    @Override
    public void scrollTo(int x, int y) {
        super.scrollTo(x, y);
        int scrollRange = getScrollRange();
        int lastOverScrollState = mOverScrollState;
        if (y >= scrollRange + mOverScrollRefreshThres) {
            mOverScrollState = 1;
        } else if (y > scrollRange) {
            mOverScrollState = 0;
        } else {
            mOverScrollState = -1;
        }
    }

    private int getScrollRange() {
        return Math.max(mTotalChildHeight - getHeight(), 0);
    }

    public void removeViewWithoutRelayout(View child) {
        removeViewInLayout(child);
    }

    public void addViewWithoutRelayout(View child) {
        addViewInLayout(child, 0, child.getLayoutParams());
    }

    public interface OnScrollListener {
        public static int SCROLL_STATE_IDLE = 0;
        public static int SCROLL_STATE_TOUCH_SCROLL = 1;
        public static int SCROLL_STATE_FLING = 2;

        public void onScrollStateChanged(int scrollState);
        public void onScroll(int direction, int top, int bottom);
    }

    public static class LayoutParams extends MarginLayoutParams {

        public LayoutParams(int width, int height) {
            super(width, height);
        }
    }

}
