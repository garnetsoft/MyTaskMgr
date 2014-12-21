package com.saveyourphone.app;

import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class CompleteListActivity extends Activity implements OnClickListener {
    private ListView mCompleteListView;
    private Button mAddItemToList;
    private Button mClearBtn;
    private List<String> mItems;
    private CompleteListAdapter mListAdapter;
    private static final int MIN = 0, MAX = 10000;

    private TextView runnProcsCount;
    private LoadProcData loadProcData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_list);

        initViews();

        mItems = new ArrayList<String>();
        mListAdapter = new CompleteListAdapter(this, mItems);
        mCompleteListView.setAdapter(mListAdapter);

        this.loadProcData = new LoadProcData(mListAdapter);
        //loadProcData.execute();  // run in background

    }

    private void initViews() {
        mCompleteListView = (ListView) findViewById(R.id.completeList);
        mAddItemToList = (Button) findViewById(R.id.addItemToList);
        mAddItemToList.setOnClickListener(this);

        mClearBtn = (Button) findViewById(R.id.buttonClear);
        mClearBtn.setOnClickListener(this);

        runnProcsCount = (TextView) findViewById(R.id.procCountText);
    }

    private void addItemsToList() {
        int randomVal = MIN + (int) (Math.random() * ((MAX - MIN) + 1));
        //mItems.add(String.valueOf(randomVal));
        loadProcData.execute();
        mListAdapter.notifyDataSetChanged();
        runnProcsCount.setText(runnProcsCount.getText().toString()+mItems.size());
    }

    private void clearItemsList() {
        //mItems.clear();
        mListAdapter.updateProcListView(new ArrayList<String>());
        mListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.addItemToList:
                addItemsToList();
                break;
            case R.id.buttonClear:
                clearItemsList();
                break;
        }
    }
}