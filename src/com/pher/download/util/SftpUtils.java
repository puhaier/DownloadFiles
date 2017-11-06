package com.pher.download.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.pher.download.model.RemoteHostInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SftpUtils {
    private static Logger log = LogManager.getLogger(SftpUtils.class.getName());

    private RemoteHostInfo host;
    private ChannelSftp sftp = null;
    private Session sshSession = null;

    public RemoteHostInfo getHost() {
        return host;
    }

    public void setHost(RemoteHostInfo host) {
        this.host = host;
    }

    public SftpUtils() {
    }

    public SftpUtils(RemoteHostInfo host) {
        this.host = host;
    }

    /**
     * 通过SFTP连接服务器
     */
    public void connect() {
        try {
            JSch jsch = new JSch();
            jsch.getSession(host.getUsername(), host.getHost(), host.getPort());
            sshSession = jsch.getSession(host.getUsername(), host.getHost(), host.getPort());
            if (log.isInfoEnabled()) {
                log.info("Session created.");
            }
            sshSession.setPassword(host.getPassword());
            Properties sshConfig = new Properties();
            sshConfig.put("StrictHostKeyChecking", "no");
            sshSession.setConfig(sshConfig);
            sshSession.connect();
            if (log.isInfoEnabled()) {
                log.info("Session connected.");
            }
            Channel channel = sshSession.openChannel("sftp");
            channel.connect();
            if (log.isInfoEnabled()) {
                log.info("Opening Channel.");
            }
            sftp = (ChannelSftp) channel;
            if (log.isInfoEnabled()) {
                log.info("Connected to " + host + ".");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭连接
     */
    public void disconnect() {
        if (this.sftp != null) {
            if (this.sftp.isConnected()) {
                this.sftp.disconnect();
                if (log.isInfoEnabled()) {
                    log.info("sftp is closed already");
                }
            }
        }
        if (this.sshSession != null) {
            if (this.sshSession.isConnected()) {
                this.sshSession.disconnect();
                if (log.isInfoEnabled()) {
                    log.info("sshSession is closed already");
                }
            }
        }
    }


    /**
     * 批量下载远程目录
     *
     * @param remotePath
     * @param localPath
     * @param msg
     */
    public void batchDownLoadFile(String remotePath, String localPath, StringBuffer msg) throws SftpException {


        List<String> remoteDirs = new ArrayList<>();
        remoteDirs.add(remotePath);
        File localPathRoot = createLocalPath(remotePath, localPath);
        String rootName = new File(remotePath).getName();
        do {
            List<String> temp = new ArrayList<>();
            for (String path : remoteDirs) {
                int index = path.indexOf(rootName);
                File tempLocalPath = new File(localPathRoot, path.substring(index + rootName.length()));
                if (!tempLocalPath.exists()) {
                    tempLocalPath.mkdirs();
                }
                batchDownLoadFile(path, tempLocalPath.getPath(), temp, msg);
            }
            remoteDirs.clear();
            remoteDirs.addAll(temp);
        } while (remoteDirs.size() > 0);


    }

    /**
     * 批量下载远程目录
     *
     * @param remotePath
     * @param localPath
     * @param remoteDirs
     * @param msg
     */
    private void batchDownLoadFile(String remotePath, String localPath, List<String> remoteDirs, StringBuffer msg) throws SftpException {
        Vector vector = listFiles(remotePath);
        if (vector.size() == 0) {
            return;
        }
        Iterator it = vector.iterator();
        while (it.hasNext()) {
            LsEntry entry = (LsEntry) it.next();
            String fileName = entry.getFilename();
            SftpATTRS attrs = entry.getAttrs();
            if (attrs.isDir()) {
                if (fileName.equals(".") || fileName.equals("..")) {
                    continue;
                }
                remoteDirs.add(MessageFormat.format("{0}{1}/", remotePath, fileName));
            } else {
                downloadFile(remotePath, fileName, localPath);
            }
        }

    }

    /**
     * 下载单个文件
     * @param remotePath
     * @param fileName
     * @param localPath
     */
    public void downloadFile(String remotePath, String fileName, String localPath) {
        FileOutputStream fileOutput = null;
        try {
            File file = new File(localPath, fileName);
            fileOutput = new FileOutputStream(file);
            sftp.get(remotePath + fileName, fileOutput);
            if (log.isInfoEnabled()) {
                log.info("===DownloadFile:" + fileName + " success from sftp.");
            }
        } catch (FileNotFoundException | SftpException e) {
            log.error(e);
        } finally {
            if (null != fileOutput) {
                try {
                    fileOutput.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 创建本地目录
     *
     * @param remotePath
     * @param localPath
     */
    private File createLocalPath(String remotePath, String localPath) {
        File remoteDir = new File(remotePath);
        String localDirName = remoteDir.getName();
        File localFile = new File(localPath, localDirName);
        if (!localFile.exists()) {
            localFile.mkdirs();
        }
        return localFile;
    }


//    /**
//     * 批量下载文件
//     *
//     * @param remotePath：远程下载目录(以路径符号结束,可以为相对路径eg:/assess/sftp/jiesuan_2/2014/)
//     * @param localPath：本地保存目录(以路径符号结束,D:\Duansha\sftp\)
//     * @param fileFormat：下载文件格式(以特定字符开头,为空不做检验)
//     * @param fileEndFormat：下载文件格式(文件格式)
//     * @param del：下载后是否删除sftp文件
//     * @return
//     */
//    public List<String> batchDownLoadFile(String remotePath, String localPath,
//                                          String fileFormat, String fileEndFormat, boolean del) {
//        List<String> filenames = new ArrayList<>();
//        try {
//            // connect();
//            Vector v = listFiles(remotePath);
//            // sftp.cd(remotePath);
//            if (v.size() > 0) {
//                System.out.println("本次处理文件个数不为零,开始下载...fileSize=" + v.size());
//                Iterator it = v.iterator();
//                while (it.hasNext()) {
//                    LsEntry entry = (LsEntry) it.next();
//                    String filename = entry.getFilename();
//                    SftpATTRS attrs = entry.getAttrs();
//                    if (attrs.isDir()) {
//
//                    }
//                        boolean flag = false;
//                        String localFileName = localPath + filename;

//                    downloadFile(remotePath, filename, localPath, filename);

//
//                        fileFormat = fileFormat == null ? "" : fileFormat
//                                .trim();
//                        fileEndFormat = fileEndFormat == null ? ""
//                                : fileEndFormat.trim();
//                        // 三种情况
//                        if (fileFormat.length() > 0 && fileEndFormat.length() > 0) {
//                            if (filename.startsWith(fileFormat) && filename.endsWith(fileEndFormat)) {
//                                flag = downloadFile(remotePath, filename, localPath, filename);
//                                if (flag) {
//                                    filenames.add(localFileName);
//                                    if (flag && del) {
//                                        deleteSFTP(remotePath, filename);
//                                    }
//                                }
//                            }
//                        } else if (fileFormat.length() > 0 && "".equals(fileEndFormat)) {
//                            if (filename.startsWith(fileFormat)) {
//                                flag = downloadFile(remotePath, filename, localPath, filename);
//                                if (flag) {
//                                    filenames.add(localFileName);
//                                    if (flag && del) {
//                                        deleteSFTP(remotePath, filename);
//                                    }
//                                }
//                            }
//                        } else if (fileEndFormat.length() > 0 && "".equals(fileFormat)) {
//                            if (filename.endsWith(fileEndFormat)) {
//                                flag = downloadFile(remotePath, filename, localPath, filename);
//                                if (flag) {
//                                    filenames.add(localFileName);
//                                    if (flag && del) {
//                                        deleteSFTP(remotePath, filename);
//                                    }
//                                }
//                            }
//                        } else {
//                            flag = downloadFile(remotePath, filename, localPath, filename);
//                            if (flag) {
//                                filenames.add(localFileName);
//                                if (flag && del) {
//                                    deleteSFTP(remotePath, filename);
//                                }
//                            }
//                        }
//                }
//            }
//        }
//        if (log.isInfoEnabled()) {
//            log.info("download file is success:remotePath=" + remotePath
//                    + "and localPath=" + localPath + ",file size is"
//                    + v.size());
//        }
//    } catch(
//    SftpException e)
//
//    {
//        e.printStackTrace();
//    } finally
//
//    {
//        // this.disconnect();
//    }
//        return filenames;
//}

    /**
     * 上传单个文件
     *
     * @param remotePath：远程保存目录
     * @param remoteFileName：保存文件名
     * @param localPath：本地上传目录(以路径符号结束)
     * @param localFileName：上传的文件名
     * @return
     */
    public boolean uploadFile(String remotePath, String remoteFileName, String localPath, String localFileName) {
        FileInputStream in = null;
        try {
            createDir(remotePath);
            File file = new File(localPath + localFileName);
            in = new FileInputStream(file);
            sftp.put(in, remoteFileName);
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (SftpException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * 批量上传文件
     *
     * @param remotePath：远程保存目录
     * @param localPath：本地上传目录(以路径符号结束)
     * @param del：上传后是否删除本地文件
     * @return
     */
    public boolean bacthUploadFile(String remotePath, String localPath,
                                   boolean del) {
        try {
            connect();
            File file = new File(localPath);
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()
                        && files[i].getName().indexOf("bak") == -1) {
                    if (this.uploadFile(remotePath, files[i].getName(),
                            localPath, files[i].getName())
                            && del) {
                        deleteFile(localPath + files[i].getName());
                    }
                }
            }
            if (log.isInfoEnabled()) {
                log.info("upload file is success:remotePath=" + remotePath
                        + "and localPath=" + localPath + ",file size is "
                        + files.length);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.disconnect();
        }
        return false;

    }

    /**
     * 删除本地文件
     *
     * @param filePath
     * @return
     */
    public boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return false;
        }

        if (!file.isFile()) {
            return false;
        }
        boolean rs = file.delete();
        if (rs && log.isInfoEnabled()) {
            log.info("delete file success from local.");
        }
        return rs;
    }

    /**
     * 创建目录
     *
     * @param createpath
     * @return
     */
    public boolean createDir(String createpath) {
        try {
            if (isDirExist(createpath)) {
                this.sftp.cd(createpath);
                return true;
            }
            String pathArry[] = createpath.split("/");
            StringBuffer filePath = new StringBuffer("/");
            for (String path : pathArry) {
                if (path.equals("")) {
                    continue;
                }
                filePath.append(path + "/");
                if (isDirExist(filePath.toString())) {
                    sftp.cd(filePath.toString());
                } else {
                    // 建立目录
                    sftp.mkdir(filePath.toString());
                    // 进入并设置为当前目录
                    sftp.cd(filePath.toString());
                }

            }
            this.sftp.cd(createpath);
            return true;
        } catch (SftpException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 判断目录是否存在
     *
     * @param directory
     * @return
     */
    public boolean isDirExist(String directory) {
        boolean isDirExistFlag = false;
        try {
            SftpATTRS sftpATTRS = sftp.lstat(directory);
            isDirExistFlag = true;
            return sftpATTRS.isDir();
        } catch (Exception e) {
            if (e.getMessage().toLowerCase().equals("no such file")) {
                isDirExistFlag = false;
            }
        }
        return isDirExistFlag;
    }

    /**
     * 删除stfp文件
     *
     * @param directory：要删除文件所在目录
     * @param deleteFile：要删除的文件
     */
    public void deleteSFTP(String directory, String deleteFile) {
        try {
            // sftp.cd(directory);
            sftp.rm(directory + deleteFile);
            if (log.isInfoEnabled()) {
                log.info("delete file success from sftp.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 如果目录不存在就创建目录
     *
     * @param path
     */
    public void mkdirs(String path) {
        File f = new File(path);

        String fs = f.getParent();

        f = new File(fs);

        if (!f.exists()) {
            f.mkdirs();
        }
    }

    /**
     * 列出目录下的文件
     *
     * @param directory：要列出的目录
     * @return
     * @throws SftpException
     */
    public Vector listFiles(String directory) throws SftpException {
        return sftp.ls(directory);
    }


    public ChannelSftp getSftp() {
        return sftp;
    }

    public void setSftp(ChannelSftp sftp) {
        this.sftp = sftp;
    }

    /**
     * 测试
     */
    public static void main(String[] args) {
        SftpUtils sftp = null;
        // 本地存放地址
        String localPath = "D:\\";
        // Sftp下载路径
        String sftpPath = "/usr/local/tools/";
        List<String> filePathList = new ArrayList<String>();
        try {
//            sftp = new SftpUtils("192.168.218.131", "root", "root");
            sftp.connect();
            // 下载
//            sftp.batchDownLoadFile(sftpPath, localPath, null);
            sftp.downloadFile("/usr/local/tools/","libaio-0.3.93-4.i386.rpm","D:/");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sftp.disconnect();
        }
    }
}

