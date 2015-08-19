package com.demo.card;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
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

    private int mScrollDis;//相当于scrollView的scrollY，我们重写scrollTo并根据此值改变各卡片的高度、缩放和位置

    private int mDividerSize;
    private int mShrinkHeight;//当卡片本身高度小于此高度、或者高度压缩到此高度后；开始scale（层叠效果）
    private int mExtraTop;//卡片流顶部预留的额外高度，用于露出“已经被叠到后面”的缩小后的卡片

    private int mTotalChildHeight = 0;
    private OnScrollListener mOnScrollListener;

    public CardFlow(Context context) {
        this(context, null);
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
        mShrinkHeight = Utils.dp2px(70);

        setOverScrollMode(OVER_SCROLL_ALWAYS);
        setChildrenDrawingOrderEnabled(true);
    }

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.mOnScrollListener = onScrollListener;
    }

    public void setDividerSize(int dividerSize) {
        mDividerSize = dividerSize;
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
                    int scrollY = mScrollDis;
                    if (diff > 0) {
                        notifyScrollListener(true);
                        if (overScrollBy(0, -diff, 0, mScrollDis, 0, getScrollRange(), 0, scrollY > diff ? mOverScrollRange : mOverScrollDistance, true)) {
                            mVelocityTracker.clear();
                        }
                    } else if (diff < 0) {
                        notifyScrollListener(false);
                        if (overScrollBy(0, -diff, 0, mScrollDis, 0, getScrollRange(), 0, mOverScrollRange, true)) {
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
                        mScroller.fling(0, mScrollDis, 0, (int) -vy, 0, 0, 0, scrollRange, 0, mOverFlingDistance);
                        notifyScrollStateChangeListener(SCROLL_STATE_FLING);
                        invalidate();
                    } else {
                        if (mScroller.springBack(0, mScrollDis, 0, 0, 0, scrollRange)) {
                            invalidate();
                        }
                        notifyScrollStateChangeListener(SCROLL_STATE_IDLE);
                    }
                }
                endTouch();
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mTouchState == TOUCH_STATE_SCROLL) {
                    if (mScroller.springBack(0, mScrollDis, 0, 0, 0, getScrollRange())) {
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
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        // Treat animating scrolls differently; see #computeScroll() for why.
        if (!mScroller.isFinished()) {
            scrollTo(scrollX, scrollY);
            if (clampedY) {
                mScroller.springBack(getScrollX(), mScrollDis, 0, 0, 0, getScrollRange());
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
            if (sy != mScrollDis) {
                notifyScrollListener(sy < mScrollDis);
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
            mOnScrollListener.onScroll(state, mScrollDis, mScrollDis + getHeight());
        }
    }

    private void notifyScrollStateChangeListener(int state) {
        if (mOnScrollListener != null) {
            mOnScrollListener.onScrollStateChanged(state);
        }
    }

    @Override
    public void scrollTo(int x, int y) {
        if (mScrollDis == y) {
            return;
        }
        mScrollDis = y;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mTotalChildHeight = getPaddingTop() + mExtraTop; //各卡片完整高度(不考虑shrink)的叠加，作为滑动距离的依据
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (!ensureCard(child)) {
                continue;
            }
            Card card = (Card) child;
            CardParams lp = (CardParams) child.getLayoutParams();
            measureChild(card, widthMeasureSpec, heightMeasureSpec);

            lp.scrollTop = mTotalChildHeight;
            lp.scrollBottom = mTotalChildHeight + card.getContentHeight();
            mTotalChildHeight = lp.scrollBottom + mDividerSize;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (!ensureCard(child)) {
                continue;
            }
            Card card = (Card) child;
            CardParams lp = (CardParams) child.getLayoutParams();
            prepareLayout(card);

            int childLeft = getPaddingLeft();
            int childRight = childLeft + card.getMeasuredWidth();
            int childTop = lp.realTop;
            int childBottom = lp.realTop + lp.shrinkHeight;
            child.layout(childLeft, childTop, childRight, childBottom);
        }
    }

    private void prepareLayout(Card card) {
        CardParams lp = (CardParams) card.getLayoutParams();
        int top = lp.scrollTop - mScrollDis;
        int bottom = lp.scrollBottom - mScrollDis;

        if (top >= 0) { //完全在边界内部的卡片
            lp.state = CardParams.STATE_FULL;
            lp.shrinkHeight = card.getContentHeight();
            lp.realTop = lp.scrollTop - mScrollDis + mExtraTop;

        } else if (bottom > mShrinkHeight) { //上面的卡片，滑动过程中缩小高度直到mShrinkHeight（相当于listview里firstvisible的卡片，一半在边界里一半在边界外）
            lp.state = CardParams.STATE_SHRINKING_HEIGHT;
            lp.realTop = mExtraTop;
            lp.shrinkHeight = bottom;

        } else { //已经划上去的卡片，露出一个边（相当于listview里完全滑出边界的卡片）
            lp.state = CardParams.STATE_MOVE_BEHIND;
            float ratio = (float) bottom / mShrinkHeight;
            lp.realTop = (int) (mExtraTop * ratio);
            lp.shrinkHeight = mShrinkHeight;
            card.setScaleX(0.8f + 0.2f * ratio);
        }
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        return super.getChildDrawingOrder(childCount, i);
    }

    private boolean ensureCard(View child) {
        return  child.getVisibility() != GONE && (child instanceof Card) && (child.getLayoutParams() instanceof CardParams);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new CardParams(MATCH_PARENT, WRAP_CONTENT);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof CardParams;
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

    public static class CardParams extends MarginLayoutParams {

        static final int STATE_MOVE_BEHIND = 0; // 折叠收起到后面阶段
        static final int STATE_SHRINKING_HEIGHT = 1; // 压缩高度阶段
        static final int STATE_FULL = 2; //完全展开的状态

        public int scrollTop;
        public int scrollBottom;
        public int shrinkHeight;
        public int realTop;
        public int state = STATE_FULL;

        public CardParams(int width, int height) {
            super(width, height);
        }
    }

}
