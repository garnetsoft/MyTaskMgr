package com.saveyourphone.app;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by George on 12/20/14.
 */
public class LoadProcData extends AsyncTask<Void, Void, ArrayList<String>> {

    private final String procCmd = "/system/bin/ps";  // or search through /proc file sys
    private final CompleteListAdapter mAdapter;

    public LoadProcData(CompleteListAdapter adapter) {
        this.mAdapter = adapter;
    }

    private List<String> getRunningProcs() {
        List<String> procs = new ArrayList<String>();

        try {
            Process process = Runtime.getRuntime().exec(procCmd);
            InputStreamReader reader = new InputStreamReader(process.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(reader);

            String line = null; //bufferedReader.readLine();  // skip header
            while ( (line = bufferedReader.readLine()) != null) {
                if (line.endsWith("/system/bin/ps"))
                    line += "(DO NOT KILL)";

                if (!line.startsWith("root") && !line.startsWith("system")) {
                    procs.add(line);
                }
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

    @Override
    protected ArrayList<String> doInBackground(Void... params) {
        List<String> runningProcs = getRunningProcs();

        return (ArrayList) runningProcs;
    }

    @Override
    protected void onPostExecute(ArrayList<String> runningProcs) {
        mAdapter.updateProcListView(runningProcs);
    }
}
