package com.demo.card;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;

public class CardActivity extends Activity {

    private CardFlow mCardFlow;
    private CardDelegateAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card);
        mCardFlow = (CardFlow) findViewById(R.id.card_flow);
        BaseAdapter baseAdapter = new ArrayAdapter<>(this, R.layout.content, mLabels);
        mAdapter = new CardDelegateAdapter(this, baseAdapter);
        mCardFlow.setAdapter(mAdapter);
    }

    private String[] mLabels = new String[] {
            "111",
            "2\n2\n22\n222\n22222",
            "333 \n 33 \n333",
            "444\n444",
            "555\n555\n555\n555\n555\n555\n555\n555\n555\n555\n555\n555\n555\n555\n",
            "666\n666\n666\n666\n",
            "777\n777\n777\n777\n777\n777\n",
            "888",
            "999\n999999\n999\n999\n999\n",
            "111",
            "2\n2\n22\n222\n22222",
            "333 \n 33 \n333",
            "444\n444",
            "555\n555\n555\n555\n555\n555\n555\n555\n555\n555\n555\n555\n555\n555\n",
            "666\n666\n666\n666\n",
            "777\n777\n777\n777\n777\n777\n",
            "888",
            "999\n999999\n999\n999\n999\n",
    };

}
