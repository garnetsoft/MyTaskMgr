package com.saveyourphone.app;

import java.util.Arrays;

public class ProcInfo {
    String user;
    int pid;
    int ppid;
    int vsize;
    int rss;
    String wchan;
    String pc;
    String name;

    String info;

    public ProcInfo(String[] infoList) {

        if (infoList.length>0) {
            this.user = infoList[0];
            this.pid = Integer.parseInt(infoList[1]);
            this.ppid = Integer.parseInt(infoList[2]);
            this.vsize = Integer.parseInt(infoList[3]);
            this.rss = Integer.parseInt(infoList[4]);
            this.wchan = infoList[5];
            this.pc = infoList[6];
            this.name = infoList[7];
        }

        this.info = Arrays.toString(infoList);
    }

    public int getPid() {
        return this.pid;
    }

    public String getName() {
        return this.name;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        /*
        sb.append(user).append(" ");
        sb.append(pid).append(" ");
        sb.append(ppid).append(" ");
        sb.append(vsize).append(" ");
        sb.append(rss).append(" ");
        sb.append(wchan).append(" ");
        sb.append(pc).append(" ");
        sb.append(name).append(" ");
        */

        sb.append(info);
        return sb.toString();
    }
}
