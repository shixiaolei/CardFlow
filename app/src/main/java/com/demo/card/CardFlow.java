package com.demo.card;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.OverScroller;

import java.util.Arrays;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * 层叠卡片视图.
 *
 * @author shixiaolei
 */
public class CardFlow extends AdapterView<ListAdapter> {

    private static final String TAG = CardFlow.class.getSimpleName();

    private static final int TOUCH_STATE_REST = 0;
    private static final int TOUCH_STATE_SCROLL = 1;
    private static final int TOUCH_STATE_SLIP_HORIZONTAL = 2;

    private OverScroller mScroller;
    private VelocityTracker mVelocityTracker;
    private int mTouchSlop;
    private int mTouchState = TOUCH_STATE_REST;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private long mLongPressTimeout;
    private int mLastY, mLastX;
    private int mDownY, mDownX;
    private int mOverScrollDistance;
    private int mOverFlingDistance;
    private int mOverScrollRange;
    private boolean mDisableScroll = false;

    private int mScrollDis;//相当于scrollView的scrollY，我们重写scrollTo并根据此值改变各卡片的高度、缩放和位置

    private int mDividerSize;
    private int mExtraBorder;//卡片流顶部预留的额外高度，用于露出“已经被叠到后面”的缩小后的卡片

    private int mTotalChildHeight = 0;
    private OnScrollListener mOnScrollListener;
    private OnClickListener mOnClickOnBlankListener;
    private long mLastClickTime;
    private Card mLastClickCard;
    private Drawable mCardBg;
    private float mCardMaxHeightRatio;

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
        mLongPressTimeout = config.getLongPressTimeout();

        mOverScrollDistance = 50;
        mOverFlingDistance = 50;

        mDividerSize = Utils.dip2px(getContext(), 4);
        mExtraBorder = Utils.dip2px(getContext(), 30);
        mCardBg = getResources().getDrawable(R.drawable.notification_card_bg);
        mCardMaxHeightRatio = 0.85f;

        setOverScrollMode(OVER_SCROLL_ALWAYS);
        setChildrenDrawingOrderEnabled(true);
    }

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.mOnScrollListener = onScrollListener;
    }

    public void setDividerSize(int dividerSize) {
        mDividerSize = dividerSize;
    }

    public void setOnClickOnBlankListener(OnClickListener listener) {
        mOnClickOnBlankListener = listener;
    }

    public void setCardBg(Drawable bg) {
        mCardBg = bg;
    }

    public void setCardMaxHeightRatio(float ratio) {
        mCardMaxHeightRatio = ratio;
    }

    /***************************层叠滑动效果BEGIN******************************************/

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (getVisibility() != View.VISIBLE) {
            return false;
        }
        if ((ev.getAction() & MotionEvent.ACTION_MASK) != MotionEvent.ACTION_DOWN) {
            if (mDisableScroll) {
                return false;
            }
        }
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mLastX = (int) ev.getX();
                mLastY = (int) ev.getY();
                mDownX = mLastX;
                mDownY = mLastY;
                mDisableScroll = false;
                if (mScroller.isFinished()) {
                    mTouchState = TOUCH_STATE_REST;
                    notifyScrollStateChangeListener(OnScrollListener.SCROLL_STATE_IDLE);
                    mLastClickCard = findMotionCard(mLastY);
                    mLastClickTime = System.currentTimeMillis();

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

                } else if (diffX > mTouchSlop && diffX > diffY && mLastClickCard != null) {
                    mTouchState = TOUCH_STATE_SLIP_HORIZONTAL;
                    mLastY = (int) ev.getY();
                    requestParentDisallowInterceptTouchEvent(true);

                } else if (diffX > mTouchSlop) {
                    mDisableScroll = true;
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
                mLastX = (int) event.getX();
                mLastY = (int) event.getY();
                mDownX = mLastX;
                mDownY = mLastY;
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int x = (int) event.getX();
                int y = (int) event.getY();
                if (mTouchState == TOUCH_STATE_REST) {
                    int diffY = Math.abs(y - mLastY);
                    int diffX = (int) Math.abs(event.getX() - mLastX);
                    if (diffY > mTouchSlop && diffY > diffX) {
                        mTouchState = TOUCH_STATE_SCROLL;
                        notifyScrollStateChangeListener(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                        mLastY = y;
                        requestParentDisallowInterceptTouchEvent(true);

                    } else if (diffX > mTouchSlop && diffX > diffY && mLastClickCard != null) {
                        mTouchState = TOUCH_STATE_SLIP_HORIZONTAL;
                        mLastX = x;
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

                } else if (mTouchState == TOUCH_STATE_SLIP_HORIZONTAL) {
                    if (isCardSlipDeletable()) {
                        int diffX = x - mDownX;
                        onSlipMove(diffX);
                        mLastX = x;
                    }
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
                        notifyScrollStateChangeListener(OnScrollListener.SCROLL_STATE_FLING);
                        invalidate();
                    } else {
                        if (mScroller.springBack(0, mScrollDis, 0, 0, 0, scrollRange)) {
                            invalidate();
                        }
                        notifyScrollStateChangeListener(OnScrollListener.SCROLL_STATE_IDLE);
                    }

                } else if (mTouchState == TOUCH_STATE_SLIP_HORIZONTAL) {
                    if (isCardSlipDeletable()) {
                        final VelocityTracker tracker = mVelocityTracker;
                        tracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
                        float vx = tracker.getXVelocity();
                        mLastX = (int) event.getX();
                        if (vx > mMinFlingVelocity) {
                            onSlipDelete(false);
                        } else if (vx < -mMinFlingVelocity) {
                            onSlipDelete(true);
                        } else {
                            int diffX = mLastX - mDownX;
                            int minDeleteDistance = (int) (getMeasuredWidth() * 0.15f);
                            if (diffX > minDeleteDistance) {
                                onSlipDelete(false);
                            } else if (minDeleteDistance < -minDeleteDistance) {
                                onSlipDelete(true);
                            } else {
                                onSlipReset();
                            }
                        }
                    }

                } else if (mTouchState == TOUCH_STATE_REST) {
                    boolean isClick = (System.currentTimeMillis() - mLastClickTime) < mLongPressTimeout;
                    if (mLastClickCard != null) {
                        CardParams lp = (CardParams) mLastClickCard.getLayoutParams();
                        if (isClick && mOnItemClickListener != null) {
                            mOnItemClickListener.onItemClick(this, mLastClickCard, lp.position, lp.itemId);
                        } else if (!isClick && mOnItemClickLongClickListener != null) {
                            mOnItemClickLongClickListener.onItemLongClick(this, mLastClickCard, lp.position, lp.itemId);
                        }

                    } else {
                        if (isClick && mOnClickOnBlankListener != null) {
                            mOnClickOnBlankListener.onClick(this);
                        }
                    }
                }
                endTouch();
                break;

            case MotionEvent.ACTION_CANCEL:
                if (mTouchState == TOUCH_STATE_SCROLL) {
                    if (mScroller.springBack(0, mScrollDis, 0, 0, 0, getScrollRange())) {
                        invalidate();
                    }
                    notifyScrollStateChangeListener(OnScrollListener.SCROLL_STATE_IDLE);
                } else if (mTouchState == TOUCH_STATE_SLIP_HORIZONTAL) {
                    onSlipReset();
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
        mDisableScroll = false;
        mLastClickCard = null;
        mLastClickTime = -1;
    }

    public void reset() {
        mScrollDis = 0;
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
            notifyScrollStateChangeListener(OnScrollListener.SCROLL_STATE_IDLE);
        }
    }

    private void notifyScrollListener(boolean isUp) {
        if (mOnScrollListener != null) {
            int state = isUp ? OnScrollListener.SCROLL_STATE_TOUCH_SCROLL : OnScrollListener.SCROLL_STATE_FLING;
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
        mMinCardHeight = Integer.MAX_VALUE;
        for (int i = 0; i < getChildCount(); i++) {
            Card card = (Card) getChildAt(i);
            CardParams lp = (CardParams) card.getLayoutParams();
            measureChild(card, widthMeasureSpec, heightMeasureSpec);

            int cardHeight = card.getMeasuredHeight();
            lp.scrollTop = mTotalChildHeight;
            lp.scrollBottom = mTotalChildHeight + cardHeight;
            mTotalChildHeight = lp.scrollBottom + mDividerSize;

            if (cardHeight > 0 && cardHeight < mMinCardHeight) {
                mMinCardHeight = cardHeight;
            }
            card.postInvalidate();
        }
        mTotalChildHeight += mExtraBorder;
    }

    private int mMinCardHeight;

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            Card card = (Card) getChildAt(i);
            CardParams lp = (CardParams) card.getLayoutParams();
            prepareLayout(card);

            int childLeft = getPaddingLeft();
            int childRight = childLeft + card.getMeasuredWidth();
            int childTop = lp.displayTop;
            int childBottom = lp.displayTop + lp.displayHeight;
            card.layout(childLeft, childTop, childRight, childBottom);
        }
        resetChildDrawingOrder();
        debugLayout();
    }

    private void prepareLayout(Card card) {
        CardParams lp = (CardParams) card.getLayoutParams();
        int top = lp.scrollTop - mScrollDis;
        int bottom = lp.scrollBottom - mScrollDis;
        int parentHeight = getMeasuredHeight();

        if (top >= mExtraBorder && bottom <= parentHeight - mExtraBorder) { //完全在边界内部的卡片
            lp.state = CardParams.STATE_FULL_IN;
            lp.displayHeight = card.getMeasuredHeight();
            lp.displayTop = top;

        } else if (top < mExtraBorder) {
            if (bottom >= mExtraBorder) { //上面滑动到一半的卡片，一半在边界内一半在边界外
                lp.state = CardParams.STATE_HALF_IN;
                int start = mExtraBorder + card.getMeasuredHeight();
                int end = mExtraBorder;
                lp.displayTop = (int) Utils.linearValue(start, mExtraBorder, end, 0, bottom);
                lp.displayHeight = (int) Utils.linearValue(start, card.getMeasuredHeight(), end, mExtraBorder, bottom);

            } else { //已经完全划上去的卡片，露出一个边
                lp.state = CardParams.STATE_FULL_OUT;
                lp.displayTop = 0;
                lp.displayHeight = mExtraBorder;
            }

        } else if (bottom > parentHeight - mExtraBorder) {
            if (top < parentHeight - mExtraBorder) { //下面滑动到一半的卡片，一半在边界内一半在边界外
                lp.state = CardParams.STATE_HALF_IN;
                int start = parentHeight - mExtraBorder - card.getMeasuredHeight();
                int end = parentHeight - mExtraBorder;
                lp.displayTop = top;
                lp.displayHeight = (int) Utils.linearValue(start, card.getMeasuredHeight(), end, mExtraBorder, top);

            } else {//已经完全划下去的卡片，露出一个边
                lp.state = CardParams.STATE_FULL_OUT;
                lp.displayTop = parentHeight - mExtraBorder;
                lp.displayHeight = mExtraBorder;
            }
        }
        card.invalidate();
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
            lp.willDraw = false;
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

    private void debugLayout() {
        if (!BuildConfig.DEBUG) {
            return;
        }
        Log.e(TAG, "debug begin========");
        for (int i = 0; i < getChildCount(); i++) {
            Card card = (Card) getChildAt(i);
            CardParams lp = (CardParams) card.getLayoutParams();
            Log.d(TAG, "i = " + i + ", displayTop =" + lp.displayTop
                    + ", displayHeight = " + lp.displayHeight + ", status = " + lp.state
                    + ", scrollTop = " + lp.scrollTop + ", scrollBottom = " + lp.scrollBottom
                    + ", height = " + card.getMeasuredHeight() + ", parentHeight = " + getMeasuredHeight());
        }
        Log.i(TAG, "drawing order: " + Arrays.toString(mDrawingOrder));
        Log.e(TAG, "debug end========");

    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        CardParams lp = (CardParams) child.getLayoutParams();
        if (lp.willDraw) {
            return super.drawChild(canvas, child, drawingTime);
        }
        return false;
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        return mDrawingOrder != null ? mDrawingOrder[i] : super.getChildDrawingOrder(childCount, i);
    }

    @Override
    protected CardParams generateDefaultLayoutParams() {
        return new CardParams(MATCH_PARENT, WRAP_CONTENT);
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
        return p instanceof CardParams;
    }

    private int getScrollRange() {
        return Math.max(mTotalChildHeight - getHeight(), 0);
    }

    public interface OnScrollListener {
        int SCROLL_STATE_IDLE = 0;
        int SCROLL_STATE_TOUCH_SCROLL = 1;
        int SCROLL_STATE_FLING = 2;

        void onScrollStateChanged(int scrollState);
        void onScroll(int direction, int top, int bottom);
    }

    /***************************层叠滑动效果END******************************************/

    /***************************AdapterView相关实现BEGIN******************************************/

    private ListAdapter mAdapter;
    private CardDataSetObserver mDataSetObserver;
    private OnItemClickListener mOnItemClickListener;
    private OnItemLongClickListener mOnItemClickLongClickListener;

    @Override
    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    @Override
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        mOnItemClickLongClickListener = listener;
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        if (mAdapter != null && mDataSetObserver != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
        }
        mAdapter = adapter;

        if (mAdapter != null) {
            mDataSetObserver = new CardDataSetObserver();
            mAdapter.registerDataSetObserver(mDataSetObserver);
        }
        reloadData();
    }

    @Override
    public ListAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public View getSelectedView() {
        return null;
    }

    @Override
    public void setSelection(int position) {
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mAdapter != null && mDataSetObserver == null) {
            mDataSetObserver = new CardDataSetObserver();
            mAdapter.registerDataSetObserver(mDataSetObserver);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAdapter != null && mDataSetObserver != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
            mDataSetObserver = null;
        }
    }

    private void reloadData() {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "reloadData(), size = " + mAdapter.getCount());
        }
        removeAllViewsInLayout();
        for (int i = 0; i < mAdapter.getCount(); i++) {
            View body = mAdapter.getView(i, null, null);
            final Card card = new Card(getContext());
            card.setContent(body);
            final CardParams lp = generateDefaultLayoutParams();
            lp.position = i;
            lp.itemId = mAdapter.getItemId(i);
            addViewInLayout(card, i, lp);
        }
        updateEmptyStatus(mAdapter.isEmpty());
        requestLayout();
        invalidate();
    }

    private void updateEmptyStatus(boolean empty) {
        if (empty) {
            if (getEmptyView() != null) {
                getEmptyView().setVisibility(View.VISIBLE);
                setVisibility(View.GONE);
            } else {
                // If the caller just removed our empty view, make sure the list view is visible
                setVisibility(View.VISIBLE);
            }

        } else {
            if (getEmptyView() != null) {
                getEmptyView().setVisibility(View.GONE);
            }
            setVisibility(View.VISIBLE);
        }
    }

    private class CardDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            reloadData();
        }
    }

    /***************************AdapterView相关实现END******************************************/


    /***************************左右滑动删除BEGIN******************************************/

    private boolean mIsSlipDeleteEnabled;
    private SlipDeleteCallback mSlipDeleteCallBack;

    public void setSlipDeleteEnabled(boolean enable) {
        mIsSlipDeleteEnabled = enable;
    }

    public void setSlipDeleteCallBack(SlipDeleteCallback callback) {
        mSlipDeleteCallBack = callback;
    }

    private Card findMotionCard(int y) {
        for (int i = 0; i < getChildCount(); i++) {
            Card card = (Card) getChildAt(i);
            CardParams lp = (CardParams) card.getLayoutParams();
            if (lp.state == CardParams.STATE_FULL_OUT) {
                continue;
            }
            if ((lp.displayTop < y) && (lp.displayTop + lp.displayHeight > y)) {
                return card;
            }
        }
        return null;
    }

    // 左右滑动手势中
    private void onSlipMove(int deltaX) {
        if (mLastClickCard == null) {
            return;
        }
        mLastClickCard.setTranslationX(deltaX);
        float alpha = 1f - 2f * Math.abs(deltaX) / mLastClickCard.getMeasuredWidth();
        mLastClickCard.setAlpha(Utils.constrain(alpha, 0.15f, 1f));
    }

    // 左右滑动手势松手，触发删除
    private void onSlipDelete(boolean isToLeftSide) {
        if (mLastClickCard == null) {
            return;
        }
        final Card card = mLastClickCard;
        int distance = card.getMeasuredWidth();
        int targetX = isToLeftSide ? -distance : distance;
        ViewPropertyAnimator animator = card.animate()
                .setDuration(120).translationX(targetX).alpha(0.15f)
                .setInterpolator(new AccelerateInterpolator(2f));
        Utils.setAnimatorEndAction(animator, new Runnable() {
            @Override
            public void run() {
                post(new Runnable() {
                    @Override
                    public void run() {
                        CardParams lp = (CardParams) card.getLayoutParams();
                        mSlipDeleteCallBack.onDelete(CardFlow.this, lp.position);
                    }
                });
            }
        });
    }

    // 左右滑动手势松手，回到原来的位置
    private void onSlipReset() {
        if (mLastClickCard == null) {
            return;
        }
        ViewPropertyAnimator animator = mLastClickCard.animate()
                .setDuration(120).translationX(0).alpha(1f)
                .setInterpolator(new AccelerateInterpolator(2f));
        Utils.setAnimatorEndAction(animator, null);
    }

    private boolean isCardSlipDeletable() {
        if (mLastClickCard == null) {
            return false;
        }
        CardParams lp = (CardParams) mLastClickCard.getLayoutParams();
        return mIsSlipDeleteEnabled && mSlipDeleteCallBack != null && mSlipDeleteCallBack.canDelete(lp.position);
    }

    /**
     * 滑动删除回调.
     */
    public interface SlipDeleteCallback {

        /** 第position张卡片能否被删除. */
        boolean canDelete(int position);

        /** 删除回调. */
        void onDelete(CardFlow listView, int position);
    }

    /***************************左右滑动删除END******************************************/

    /**
     * LayoutParams数据结构.
     */
    static class CardParams extends MarginLayoutParams {

        private static final int STATE_FULL_IN = 0; //相当于listview里完全在边界内部的卡片
        private static final int STATE_HALF_IN = 1; //相当于listview里firstvisible的卡片，一半在边界里一半在边界外
        private static final int STATE_FULL_OUT = 2; //相当于listview里完全滑出边界的卡片

        private int state = STATE_FULL_IN;

        private int position;
        private long itemId;
        private int scrollTop; //不考虑折叠效果时的top，也就卡片顶部碰到屏幕上沿需要滚的距离
        private int scrollBottom; //不考虑折叠效果时的top，也就底部完全滚过屏幕时需要的距离
        private int displayTop; //考虑折叠，当前所处的top
        private int displayHeight;//考虑折叠，当前所处的底部
        private boolean willDraw = true;

        public CardParams(int width, int height) {
            super(width, height);
        }
    }

    /**
     * 单张卡片视图.
     */
    class Card extends ViewGroup {

        private View mContent;

        public Card(Context context) {
            super(context);
        }

        private void setContent(View content) {
            mContent = content;
//            setBackgroundDrawable(mCardBg);
            setBackgroundColor(0x8000ff00);
            Log.w("sxl", "##########  " + content);
            removeAllViewsInLayout();
            addView(content);
        }

        @Override
        public void addView(View child) {
            ensureChildrenCount();
            super.addView(child);
        }

        @Override
        public void addView(View child, int index) {
            ensureChildrenCount();
            super.addView(child, index);
        }

        @Override
        public void addView(View child, LayoutParams params) {
            ensureChildrenCount();
            super.addView(child, params);
        }

        @Override
        public void addView(View child, int index, LayoutParams params) {
            ensureChildrenCount();
            super.addView(child, index, params);
        }

        private void ensureChildrenCount() {
            if (getChildCount() > 0) {
                throw new IllegalStateException("Card can host only one direct child");
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            if (mContent == null) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }

            int parentHeightSize = MeasureSpec.getSize(heightMeasureSpec);
            int parentHeightMode = MeasureSpec.getMode(heightMeasureSpec);
            if (parentHeightMode != MeasureSpec.AT_MOST && parentHeightMode != MeasureSpec.EXACTLY) {
                throw new IllegalStateException("parentHeightMode should not be UNSPECIFIED.");
            }
            int childMaxHeight = (int) (parentHeightSize * mCardMaxHeightRatio);
            int childHeightSpec = MeasureSpec.makeMeasureSpec(childMaxHeight, MeasureSpec.AT_MOST);
            measureChild(mContent, widthMeasureSpec, childHeightSpec);

            int contentHeight = mContent.getMeasuredHeight();
            int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
            setMeasuredDimension(width, contentHeight);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            if (mContent == null) {
                return;
            }

            int parentLeft = getPaddingLeft();
            int parentRight = r - l - getPaddingRight();
            int parentTop = getPaddingTop();
            int childWidth = mContent.getMeasuredWidth();
            int childHeight = mContent.getMeasuredHeight();
            int childTop = parentTop;
            int childLeft = parentLeft + (parentRight - parentLeft - childWidth) / 2;
            mContent.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        }
    }
}
