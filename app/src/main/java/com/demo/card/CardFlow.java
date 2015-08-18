package com.demo.card;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ScrollView;

public class CardFlow extends ScrollView {

    private CardList mCardList;

    public CardFlow(Context context) {
        super(context);
    }

    public CardFlow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mCardList = (CardList) findViewById(R.id.card_list);
    }

    public void setAdapter(ListAdapter adapter) {
        for (int i = 0; i < adapter.getCount(); i++) {
            View content = adapter.getView(i, null, null);
            Card card = (Card) LayoutInflater.from(getContext()).inflate(R.layout.card, null);
            card.addView(content);
            mCardList.addView(card);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }
}
