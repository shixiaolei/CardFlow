package com.demo.card;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListAdapter;

public class AdapterCardFlow extends CardFlow {

    private ListAdapter mAdapter;
    private CardDataSetObserver mDataSetObserver;

    public AdapterCardFlow(Context context) {
        super(context);
    }

    public AdapterCardFlow(Context context, AttributeSet attrs) {
        super(context, attrs);
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

    private void reloadData() {
        removeAllViewsInLayout();
        for (int i = 0; i < mAdapter.getCount(); i++) {
            View content = mAdapter.getView(i, null, null);
            addCardInLayout(content, i);
        }
        requestLayout();
    }

    private class CardDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            reloadData();
        }
    }
}
