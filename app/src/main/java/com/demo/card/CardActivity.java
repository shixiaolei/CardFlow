package com.demo.card;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;

public class CardActivity extends Activity {

    private CardFlow mCardFlow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card);
        mCardFlow = (CardFlow) findViewById(R.id.card_flow);
        BaseAdapter baseAdapter = new ArrayAdapter<>(this, R.layout.content, mLabels);
        mCardFlow.setAdapter(baseAdapter);
    }

    private String[] mLabels = new String[] {
            "111\n1\n11111\n111\n11111111111",
            "2\n2\n22\n222\n22222",
            "333 \n 33 \n333",
            "444\n444",
            "555\n555\n555\n555\n555\n555\n555\n555\n555\n555\n555\n555\n555\n555",
            "666\n666\n666\n666",
            "777\n777\n777\n777\n777\n777",
            "888",
            "999\n999999\n999\n999\n999",
            "111\n1\n11111\n111\n11111111111",
            "2\n2\n22\n222\n22222",
            "333 \n 33 \n333",
            "444\n444",
            "555\n555\n555\n555\n555\n555\n555\n555\n555\n555\n555\n555\n555\n555",
            "666\n666\n666\n666",
            "777\n777\n777\n777\n777\n777",
            "888",
            "999\n999999\n999\n999\n999",
    };

}
