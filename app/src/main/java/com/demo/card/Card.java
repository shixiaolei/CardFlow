package com.demo.card;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.demo.card.CardFlow.CardParams;

/**
 * 单张卡片.
 */
public class Card extends ViewGroup {

    private View mContent;

    private int mContentHeight;

    public Card(Context context) {
        super(context);
        setBackgroundResource(R.drawable.card_bg);
    }

    public void setContent(View content) {
        mContent = content;
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
    public void addView(View child, ViewGroup.LayoutParams params) {
        ensureChildrenCount();
        super.addView(child, params);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
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

        measureChild(mContent, widthMeasureSpec, heightMeasureSpec);
        mContentHeight = mContent.getMeasuredHeight();

        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        CardParams lp = (CardParams) getLayoutParams();
        if (lp.shrinkHeight > 0) {
            setMeasuredDimension(width, lp.shrinkHeight);
            int contentHeightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.AT_MOST);
            measureChild(mContent, widthMeasureSpec, contentHeightSpec);
        } else {
            setMeasuredDimension(width, getContentHeight());
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mContent == null) {
            return;
        }

        int parentLeft = getPaddingLeft();
        int parentRight = r - l - getPaddingRight();
        int parentTop = getPaddingTop();
        int parentBottom = b - t - getPaddingBottom();
        int childWidth = mContent.getMeasuredWidth();
        int childHeight = mContent.getMeasuredHeight();
        int childTop = parentTop + (parentBottom - parentTop - childHeight) / 2;
        int childLeft = parentLeft + (parentRight - parentLeft - childWidth) / 2;
        mContent.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
    }

    public int getContentHeight() {
        return mContentHeight;
    }
}
