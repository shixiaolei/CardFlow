package com.demo.card;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListAdapter;

public class AdapterCardFlow extends CardFlow {

    public AdapterCardFlow(Context context) {
        super(context);
    }

    public AdapterCardFlow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setAdapter(ListAdapter adapter) {
        for (int i = 0; i < adapter.getCount(); i++) {
            View content = adapter.getView(i, null, null);
            addCardInLayout(content, i);
        }
        requestLayout();
    }
}
