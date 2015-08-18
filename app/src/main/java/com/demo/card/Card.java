package com.demo.card;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

/**
 * 单张卡片.
 */
public class Card extends FrameLayout {

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
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mContent != null) {
            mContentHeight = mContent.getMeasuredHeight();
        }
    }

    public int getContentHeight() {
        return mContentHeight;
    }
}
