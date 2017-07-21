package com.tracking;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.tracking.preview.PreviewActivity;

/**
 * Created by jerrypxiao on 2016/3/18.
 */
public class DemoListActivity extends ListActivity {

    String tests[] = { "MainActivity",
                        "preview.PreviewActivity",
                        "preview.PreviewFilterActivity",
                        "sdkexample.MainActivity",
                      };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, tests));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        String testName = tests[position];
        Class clazz = null;
        try{
            clazz = Class.forName("com.tracking." + testName);
            Intent intent = new Intent(this, clazz);
            startActivity(intent);
        }catch(ClassNotFoundException e){
            e.printStackTrace();
        }
    }
}