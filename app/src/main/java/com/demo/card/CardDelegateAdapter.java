package com.demo.card;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class CardDelegateAdapter extends BaseAdapter {

    private Context mContext;
    private BaseAdapter mDelegated;

    public CardDelegateAdapter(Context context, BaseAdapter adapter) {
        mContext = context;
        mDelegated = adapter;
    }

    @Override
    public int getCount() {
        return mDelegated.getCount();
    }

    @Override
    public Object getItem(int position) {
        return mDelegated.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return mDelegated.getItemId(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.card, null);
        }
        Card card = (Card) convertView;
        View content = mDelegated.getView(position, null, parent);
        card.setContent(content);
        return card;
    }
}
