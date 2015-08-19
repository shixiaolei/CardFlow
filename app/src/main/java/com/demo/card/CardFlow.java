package com.demo.card;

import android.content.Context;
import android.util.AttributeSet;
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
import static com.demo.card.CardFlow.OnScrollListener.SCROLL_STATE_FLING;
import static com.demo.card.CardFlow.OnScrollListener.SCROLL_STATE_IDLE;
import static com.demo.card.CardFlow.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL;

public class CardFlow extends ViewGroup {

    private static final int TOUCH_STATE_REST = 0;
    private static final int TOUCH_STATE_SCROLL = 1;

    private OverScroller mScroller;
    private VelocityTracker mVelocityTracker;
    private int mTouchSlop;
    private int mTouchState = TOUCH_STATE_REST;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private int mLastY, mLastX;
    private int mOverScrollDistance;
    private int mOverFlingDistance;
    private int mOverScrollRange;
    private boolean isUnableToDrag = false;

    private int mDividerSize;
    private int mTotalChildHeight = 0;
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

        mOverScrollDistance = 50;
        mOverFlingDistance = 50;

        mDividerSize = Utils.dp2px(15);

        setOverScrollMode(OVER_SCROLL_ALWAYS);
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

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        int top = getPaddingTop() - mScrollDistance;
        mTotalChildHeight = 0;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            if (!(child instanceof Card)) {
                return;
            }
            if (!(child.getLayoutParams() instanceof CardLayoutParams)) {
                return;
            }
            Card card = (Card) child;
            CardLayoutParams lp = (CardLayoutParams) child.getLayoutParams();
            int childLeft = getPaddingLeft();
            int childRight = childLeft + card.getMeasuredWidth();
            int childTop = top;
            int childBottom = top + card.getContentHeight();
            child.layout(childLeft, childTop, childRight, childBottom);
            top = childBottom + mDividerSize;
            mTotalChildHeight += card.getContentHeight() + mDividerSize;
            lp.scrollTop = childTop;
            lp.scrollBottom = childBottom;
        }
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new CardLayoutParams(MATCH_PARENT, WRAP_CONTENT);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof CardLayoutParams;
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
                    notifyScrollStateChangeListener(SCROLL_STATE_IDLE);
                } else {
                    mScroller.abortAnimation();
                    mTouchState = TOUCH_STATE_SCROLL;
                    notifyScrollStateChangeListener(SCROLL_STATE_TOUCH_SCROLL);
                    requestParentDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int diffY = (int) Math.abs(ev.getY() - mLastY);
                int diffX = (int) Math.abs(ev.getX() - mLastX);
                if (diffY > mTouchSlop && diffY > diffX) {
                    mTouchState = TOUCH_STATE_SCROLL;
                    notifyScrollStateChangeListener(SCROLL_STATE_TOUCH_SCROLL);
                    mLastY = (int) ev.getY();
                    requestParentDisallowInterceptTouchEvent(true);
                } else if (diffX > mTouchSlop) {
                    isUnableToDrag = true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                mTouchState = TOUCH_STATE_REST;
                notifyScrollStateChangeListener(SCROLL_STATE_IDLE);
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
                        notifyScrollStateChangeListener(SCROLL_STATE_TOUCH_SCROLL);
                        mLastY = y;
                        requestParentDisallowInterceptTouchEvent(true);
                    }
                }

                if (mTouchState == TOUCH_STATE_SCROLL) {
                    int diff = y - mLastY;
                    int scrollY = mScrollDistance;
                    if (diff > 0) {
                        notifyScrollListener(true);
                        if (overScrollBy(0, -diff, 0, mScrollDistance, 0, getScrollRange(), 0, scrollY > diff ? mOverScrollRange : mOverScrollDistance, true)) {
                            mVelocityTracker.clear();
                        }
                    } else if (diff < 0) {
                        notifyScrollListener(false);
                        if (overScrollBy(0, -diff, 0, mScrollDistance, 0, getScrollRange(), 0, mOverScrollRange, true)) {
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

                    if (Math.abs(vy) > mMinFlingVelocity) {
                        mScroller.fling(0, mScrollDistance, 0, (int) -vy, 0, 0, 0, scrollRange, 0, mOverFlingDistance);
                        notifyScrollStateChangeListener(SCROLL_STATE_FLING);
                        invalidate();
                    } else {
                        if (mScroller.springBack(0, mScrollDistance, 0, 0, 0, scrollRange)) {
                            invalidate();
                        }
                        notifyScrollStateChangeListener(SCROLL_STATE_IDLE);
                    }
                }
                endTouch();
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mTouchState == TOUCH_STATE_SCROLL) {
                    if (mScroller.springBack(0, mScrollDistance, 0, 0, 0, getScrollRange())) {
                        invalidate();
                    }
                    notifyScrollStateChangeListener(SCROLL_STATE_IDLE);
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
            scrollTo(scrollX, scrollY);
            if (clampedY) {
                mScroller.springBack(getScrollX(), mScrollDistance, 0, 0, 0, getScrollRange());
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
            if (sy != mScrollDistance) {
                notifyScrollListener(sy < mScrollDistance);
            }
            scrollTo(0, sy);

            awakenScrollBars();
            postInvalidate();
        } else if (TOUCH_STATE_REST == mTouchState) {
            notifyScrollStateChangeListener(SCROLL_STATE_IDLE);
        }
    }

    private void notifyScrollListener(boolean isUp) {
        if (mOnScrollListener != null) {
            int state = isUp ? SCROLL_STATE_TOUCH_SCROLL : SCROLL_STATE_FLING;
            mOnScrollListener.onScroll(state, mScrollDistance, mScrollDistance + getHeight());
        }
    }

    private void notifyScrollStateChangeListener(int state) {
        if (mOnScrollListener != null) {
            mOnScrollListener.onScrollStateChanged(state);
        }
    }

    private int mScrollDistance;

    @Override
    public void scrollTo(int x, int y) {
        if (mScrollDistance == y) {
            return;
        }
        mScrollDistance = y;
        requestLayout();
    }

    private int getScrollRange() {
        return Math.max(mTotalChildHeight - getHeight(), 0);
    }

    public void setAdapter(ListAdapter adapter) {
        for (int i = 0; i < adapter.getCount(); i++) {
            View content = adapter.getView(i, null, null);
            Card card = new Card(getContext());
            card.setContent(content);
            addViewInLayout(card, i, generateDefaultLayoutParams());
        }
        requestLayout();
    }

    public interface OnScrollListener {
        int SCROLL_STATE_IDLE = 0;
        int SCROLL_STATE_TOUCH_SCROLL = 1;
        int SCROLL_STATE_FLING = 2;

        void onScrollStateChanged(int scrollState);
        void onScroll(int direction, int top, int bottom);
    }

    public static class CardLayoutParams extends MarginLayoutParams {

        public int scrollTop;
        public int scrollBottom;
        public int shrinkHeight;

        public CardLayoutParams(int width, int height) {
            super(width, height);
        }
    }

}
