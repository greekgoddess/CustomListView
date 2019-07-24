package com.ding.view;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private CustomListView mListView;
    private TextView mText;
    private List<String> list;
    private CustomAdapter adapter;
    private ViewGroup mRootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRootView = findViewById(R.id.rootview);
        mText = findViewById(R.id.text);
        mText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("ding", "mText");
                list.add(0, "新添加的数据");
                adapter.notifyDataSetChanged();
//                mListView.requestLayout();
            }
        });
        mListView = findViewById(R.id.custom_list_view);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.e("ding", "position" + position);
            }
        });

        list = new LinkedList<>();
        list.add("1111111111");
        list.add("2222222222");
        list.add("3333333333");
        list.add("4444444444");
        list.add("5555555555");
        list.add("6666666666");
        list.add("7777777777");
        list.add("8888888888");
        list.add("9999999999");
        list.add("1qqqqqqqqqqq");
        list.add("2wwwwwwwwwwww");
        list.add("3gggggggg");
        list.add("4hhhhhhhh");
        list.add("5ssssssss");
        list.add("6cccccccc");
        list.add("7vbbbbbbbb");
        list.add("8jjjj");
        list.add("9,,,,,");
        list.add("10;;;;;");
        list.add("11nnnnnn");
        list.add("12bbbbbb");
        list.add("13bbbbbbb");
        list.add("14mmmmmmm");
        list.add("15bbbbbbb");
        list.add("16vvvvvvvv");
        list.add("17ccccccccc");
        adapter = new CustomAdapter(MainActivity.this, list);

        mListView.setAdapter(adapter);

    }
}
