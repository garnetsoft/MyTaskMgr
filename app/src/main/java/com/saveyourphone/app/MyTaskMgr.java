package com.saveyourphone.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import static android.os.Build.VERSION_CODES.*;

public class MyTaskMgr extends Activity {

    private static ProgressDialog dialog;

    private static Handler handler;
    private Thread downloadThread;

    private static final String procCmd = "/system/bin/ps";  // or search through /proc file sys
    private static List<String> mProcsList = new ArrayList<String>();
    public static Button mProcsCount;
    private ListView mCompleteListView;
    private ArrayAdapter<String> mListAdapter;

    @TargetApi(HONEYCOMB)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mCompleteListView = (ListView) findViewById(R.id.completeList);
        mListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mProcsList);
        mCompleteListView.setAdapter(mListAdapter);
        mCompleteListView.setFastScrollEnabled(true);
        mCompleteListView.setFocusable(true);

        Context context = mCompleteListView.getContext();
        System.out.println(context);

        mProcsCount = (Button) findViewById(R.id.procs_num);

        mCompleteListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
                String selectedProc = ((TextView)view).getText().toString();
                String[] procInfo = selectedProc.split("\\s+");
                int pid = Integer.parseInt(procInfo[1]);

                String status = killProcs(pid);

                Toast.makeText(MyTaskMgr.this,
                        "You killed " + status,
                        Toast.LENGTH_LONG).show();

                //killAll(null);
                getRunningProcesses(null);  // refresh
            }
        });

        // create a handler to get running process list
        handler = new Handler() {
            @TargetApi(HONEYCOMB)
            @Override
            public void handleMessage(Message msg) {
                mListAdapter.clear();
                mListAdapter.addAll(mProcsList.toArray(new String[0]));
                mProcsCount.setText(": " + mProcsList.size());

                dialog.dismiss();
            }
        };

        // Did we already loaded the procs?
        if (mProcsList != null) {
            mListAdapter.clear();
            mListAdapter.addAll(mProcsList.toArray(new String[0]));
            mProcsCount.setText(": " + mProcsList.size());
        }

        // check if the thread is already running
        downloadThread = (Thread) getLastNonConfigurationInstance();
        if (downloadThread != null && downloadThread.isAlive()) {
            dialog = ProgressDialog.show(this, "/system/bin/ps", "loading...");
        }
    }

    // call this from a background thread to do automatic clean up
    public void killAll(View view) {
        if (mProcsList != null) {
            // iterate through mProcsList and kill each of the proc
            //for (String selectedProc : mProcsList) {
            int size = mProcsList.size();
            for (int i = 1; i < size; i++) {
                String[] procInfo = mProcsList.get(i).split("\\s+");
                //System.out.println("xxxx killAll: " + Arrays.toString(procInfo));
                //Log.i("xxxx HAAHA: ", Arrays.toString(procInfo));
                int pid = Integer.parseInt(procInfo[1]);

                String status = killProcs(pid);
            }

            mProcsList.clear();
            mProcsList = null;
        }

        mListAdapter.clear();
        mProcsCount.setText(":");
    }

    public void getRunningProcesses(View view) {
        dialog = ProgressDialog.show(this, "/system/bin/ps", "Loading");
        downloadThread = new MyThread();
        downloadThread.start();
    }

    // save the thread
    @Override
    public Object onRetainNonConfigurationInstance() {
        return downloadThread;
    }

    // dismiss dialog if activity is destroyed
    @Override
    protected void onDestroy() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            dialog = null;
        }
        super.onDestroy();
    }

    private static List<String> getRunningProcs() {
        List<String> procs = new ArrayList<String>();

        try {
            Process process = Runtime.getRuntime().exec(procCmd);
            InputStreamReader reader = new InputStreamReader(process.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(reader);

            String line = null; //bufferedReader.readLine();  // skip header
            while ( (line = bufferedReader.readLine()) != null) {
                if (line.contains("com.saveyourphone.app"))
                    line += "(DO NOT KILL)";

                if (!line.startsWith("root") && !line.startsWith("system"))
                    procs.add(line);
            }
            bufferedReader.close();
            process.waitFor();
        }
        catch (IOException io) {
            io.printStackTrace();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        return procs;
    }

    private static String killProcs(int pid) {
        StringBuilder commandOutput = new StringBuilder();
        commandOutput.append(pid);

        try {
            Process process = Runtime.getRuntime().exec("/system/bin/kill -9 "+ pid);
            InputStreamReader reader = new InputStreamReader(process.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(reader);

            String line;
            while ( (line = bufferedReader.readLine()) != null) {
                commandOutput.append(line);
            }
            bufferedReader.close();
            process.waitFor();
        }
        catch (IOException io) {
            io.printStackTrace();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        return commandOutput.toString();
    }

    static public class MyThread extends Thread {
        @Override
        public void run() {
            try {
                mProcsList = getRunningProcs();

                // Updates the user interface
                handler.sendEmptyMessage(0);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
            }
        }
    }

}