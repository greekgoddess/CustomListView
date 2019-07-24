package com.ding.view;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by jindingwei on 2019/7/23.
 */

public class CustomAdapter extends BaseAdapter {
    private Context mContext;
    private List<String> mDataList;

    public CustomAdapter(Context context, List<String> list) {
        if (list == null || list.size() <= 0) {
            return;
        }
        mContext = context;
        mDataList = list;
    }

    @Override
    public int getCount() {
        return mDataList.size();
    }

    @Override
    public Object getItem(int position) {
        return mDataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_view_layout, null);
            viewHolder = new ViewHolder();
            viewHolder.textView = convertView.findViewById(R.id.text);
            viewHolder.btn = convertView.findViewById(R.id.btn);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        if (position % 2 == 0) {
            viewHolder.textView.setBackgroundResource(R.color.colorAccent);
        } else {
            viewHolder.textView.setBackgroundResource(R.color.colorPrimary);
        }
        String str = (String) getItem(position);
        viewHolder.textView.setText(str);
        viewHolder.btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("ding", "viewHolder.btn.setOnClickListener" + position);
            }
        });
        return convertView;
    }

    public static class ViewHolder {
        private TextView textView;
        private Button btn;
    }
}
