package com.demo.card;

import android.content.Context;
import android.graphics.Canvas;
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
    private int mExtraBorder;//卡片流顶部预留的额外高度，用于露出“已经被叠到后面”的缩小后的卡片

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
        mExtraBorder = Utils.dp2px(30);

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
        mTotalChildHeight = getPaddingTop() + mExtraBorder; //各卡片完整高度(不考虑shrink)的叠加，作为滑动距离的依据
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
            if (lp.scaleX != 1) {
                int scaleSize = (int) (card.getMeasuredWidth() * (1 - lp.scaleX) * 0.5f);
                childLeft += scaleSize;
                childRight -= scaleSize;
            }
            int childTop = lp.displayTop;
            int childBottom = lp.displayTop + lp.displayHeight;
            child.layout(childLeft, childTop, childRight, childBottom);
        }
        resetChildDrawingOrder();
    }

    private void prepareLayout(Card card) {
        CardParams lp = (CardParams) card.getLayoutParams();
        int top = lp.scrollTop - mScrollDis;
        int bottom = lp.scrollBottom - mScrollDis;
        int parentHeight = getMeasuredHeight();

        if (top >= mExtraBorder && bottom <= parentHeight - mExtraBorder) { //完全在边界内部的卡片
            lp.state = CardParams.STATE_FULL_IN;
            lp.displayHeight = card.getContentHeight();
            lp.displayTop = top;
            lp.scaleX = 1;

        } else if (top < mExtraBorder) {
            if (bottom >= mExtraBorder) { //上面滑动到一半的卡片，一半在边界内一半在边界外
                lp.state = CardParams.STATE_HALF_IN;
                int start = mExtraBorder + card.getContentHeight();
                int end = mExtraBorder;
                lp.displayTop = (int) Utils.linearValue(start, mExtraBorder, end, 0, bottom);
                lp.displayHeight = (int) Utils.linearValue(start, card.getContentHeight(), end, mExtraBorder, bottom);
                lp.scaleX = Utils.linearValue(start, 1f, end, 0.9f, bottom);

            } else {//已经完全划上去的卡片，露出一个边
                lp.state = CardParams.STATE_FULL_OUT;
                lp.displayTop = 0;
                lp.displayHeight = mExtraBorder;
                lp.scaleX = 0.9f;
            }

        } else if (bottom > parentHeight - mExtraBorder) {
            if (top < parentHeight - mExtraBorder) { //下面滑动到一半的卡片，一半在边界内一半在边界外
                lp.state = CardParams.STATE_HALF_IN;
                int start = parentHeight - mExtraBorder - card.getContentHeight();
                int end = parentHeight - mExtraBorder;
                lp.displayTop = top;
                lp.displayHeight = (int) Utils.linearValue(start, card.getContentHeight(), end, mExtraBorder, top);
                lp.scaleX = Utils.linearValue(start, 1f, end, 0.9f, top);

            } else {//已经完全划下去的卡片，露出一个边
                lp.state = CardParams.STATE_FULL_OUT;
                lp.displayTop = parentHeight - mExtraBorder;
                lp.displayHeight = mExtraBorder;
                lp.scaleX = 0.9f;
            }
        }
    }

    private int[] mDrawingOrder;

    private void resetChildDrawingOrder() {
        int childCount = getChildCount();

        int[] temp = new int[childCount];
        int first = -1;
        int last = -1;
        int index = 0;

        for (int i = 0; i < childCount; i++) {
            CardParams lp = (CardParams) getChildAt(i).getLayoutParams();
            if (lp.state != CardParams.STATE_FULL_OUT) {
                lp.willDraw = true;
                if (first == -1) {
                    first = i;
                }
                last = i;
                temp[index++] = i;
            }
        }

        first--;
        if (first >= 0) {
            temp[index++] = first;
            CardParams lp = (CardParams) getChildAt(first).getLayoutParams();
            lp.willDraw = true;
            first--;
        }
        last++;
        if (last < childCount - 1) {
            temp[index++] = last;
            CardParams lp = (CardParams) getChildAt(last).getLayoutParams();
            lp.willDraw = true;
            last++;
        }

        if (first > 0) {
            for (int i = first; i >=0; i--) {
                temp[index++] = i;
            }
        }
        if (last < childCount - 1) {
            for (int i = last; i <= childCount - 1; i++) {
                temp[index++] = i;
            }
        }

        mDrawingOrder = new int[childCount];
        for (int i = 0; i < childCount; i++) {
            mDrawingOrder[childCount - 1 - i] = temp[i];
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        CardParams lp = (CardParams) child.getLayoutParams();
        if (lp.willDraw || true) {
            return super.drawChild(canvas, child, drawingTime);
        }
        return false;
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        return mDrawingOrder != null ? mDrawingOrder[i] : super.getChildDrawingOrder(childCount, i);
    }

    private boolean ensureCard(View child) {
        return child.getVisibility() != GONE && (child instanceof Card) && (child.getLayoutParams() instanceof CardParams);
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


    @Override
    public void addView(View child) {
        ensureChild(child);
        super.addView(child);
    }

    @Override
    public void addView(View child, int index) {
        ensureChild(child);
        super.addView(child, index);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        ensureChild(child);
        super.addView(child, params);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        ensureChild(child);
        super.addView(child, index, params);
    }

    @Override
    protected boolean addViewInLayout(View child, int index, LayoutParams params) {
        ensureChild(child);
        return super.addViewInLayout(child, index, params);
    }

    @Override
    protected boolean addViewInLayout(View child, int index, LayoutParams params, boolean preventRequestLayout) {
        ensureChild(child);
        return super.addViewInLayout(child, index, params, preventRequestLayout);
    }

    public void addCard(View body) {
        Card card = new Card(getContext());
        card.setContent(body);
        addView(card);
    }

    public void addCardInLayout(View body, int index) {
        Card card = new Card(getContext());
        card.setContent(body);
        addViewInLayout(card, index, generateDefaultLayoutParams());
    }

    private void ensureChild(View child) {
        if (!(child instanceof Card)) {
            throw new IllegalStateException("CardFlow only accept Card in addView(), use addCard() or addCardInLayout() instead.");
        }
    }

    public interface OnScrollListener {
        int SCROLL_STATE_IDLE = 0;
        int SCROLL_STATE_TOUCH_SCROLL = 1;
        int SCROLL_STATE_FLING = 2;

        void onScrollStateChanged(int scrollState);
        void onScroll(int direction, int top, int bottom);
    }

    public static class CardParams extends MarginLayoutParams {

        private static final int STATE_FULL_IN = 0; //相当于listview里完全在边界内部的卡片
        private static final int STATE_HALF_IN = 1; //相当于listview里firstvisible的卡片，一半在边界里一半在边界外
        private static final int STATE_FULL_OUT = 2; //相当于listview里完全滑出边界的卡片

        private int state = STATE_FULL_IN;

        private int scrollTop;
        private int scrollBottom;
        private int displayHeight;
        private int displayTop;
        private float scaleX = 1;
        private boolean willDraw = true;

        public CardParams(int width, int height) {
            super(width, height);
        }
    }

}
