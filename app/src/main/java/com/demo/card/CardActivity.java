package com.demo.card;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

public class CardActivity extends Activity implements View.OnClickListener {

    private CardFlow mCardFlow;
    private Button mBtn;
    private String[] mLabels;
    private BaseAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card);
        mCardFlow = (CardFlow) findViewById(R.id.card_flow);
        mBtn = (Button) findViewById(R.id.btn_refresh);
        mBtn.setOnClickListener(this);
        mLabels = genLabels();
        mAdapter = new MyCardAdapter();
        mCardFlow.setAdapter(mAdapter);
    }

    @Override
    public void onClick(View v) {
        if (v == mBtn) {
            refresh();
        }
    }

    private void refresh() {
        mLabels = genLabels();
        mAdapter.notifyDataSetChanged();
    }

    private String[] genLabels() {
        int cardCount = Utils.random(1, 20);
        String[] result = new String[cardCount];
        for (int i = 0; i < cardCount; i++) {
            char c = (char) ('a' + i);
            int lineCount = Utils.random(1, 20);
            String s = "";
            for (int j = 0; j < lineCount; j++) {
                int charCount = Utils.random(1, 20);
                for (int k = 0; k < charCount; k++) {
                    s += c;
                }
                if (j != lineCount - 1) {
                    s += "\n";
                }
            }
            result[i] = s;
        }
        return result;
    }

    private class MyCardAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mLabels.length;
        }

        @Override
        public Object getItem(int position) {
            return mLabels[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView = (TextView) LayoutInflater.from(CardActivity.this).inflate(R.layout.content, null);
            textView.setText(mLabels[position]);
            return textView;
        }
    };

}
