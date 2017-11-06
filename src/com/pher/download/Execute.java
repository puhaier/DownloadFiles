package com.pher.download;

import com.pher.download.model.RemoteHostInfo;
import com.pher.download.util.SftpUtils;

import java.io.File;
import java.util.List;

public class Execute {

    private static final String CLUSTER_IP_PATH = "/opt/script/";
    private static final String CLUSTER_IP_FILE = "tsp_inst.cfg";

    private SftpUtils sftpUtils = new SftpUtils();

    public static void main(String[] args) {

    }

    public void execute() {
    }

    private List<String> getClusterIps(RemoteHostInfo hostInfo) {
        sftpUtils.setHost(hostInfo);
        sftpUtils.connect();
        String tmpdir = System.getProperty("java.io.tmpdir");
        File file = new File(tmpdir, CLUSTER_IP_FILE);
        if (file.exists()) {
            file.delete();
        }
        sftpUtils.downloadFile(CLUSTER_IP_PATH, CLUSTER_IP_FILE, tmpdir);
        return null;
    }


}
