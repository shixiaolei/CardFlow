package com.demo.card;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class CardDelegateAdapter extends BaseAdapter {

    private Context mContext;
    private BaseAdapter mAdapter;

    public CardDelegateAdapter(Context context, BaseAdapter adapter) {
        mContext = context;
        mAdapter = adapter;
    }

    @Override
    public int getCount() {
        return mAdapter.getCount();
    }

    @Override
    public Object getItem(int position) {
        return mAdapter.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return mAdapter.getItemId(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.card, null);
        }
        Card card = (Card) convertView;
        View content = mAdapter.getView(position, convertView, parent);
        card.setContent(content);
        return card;
    }
}
