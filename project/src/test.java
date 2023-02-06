import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;

import java.util.Arrays;
import java.util.Scanner;

public class test {
    public static void main(final String[] args) throws IOException {
        FTPClient ftpClient = null;
        try {
            //创建一个ftp客户端
            ftpClient = new FTPClient();
            // 连接FTP服务器
            ftpClient.connect("ftp.ncnu.edu.tw");
            // 登陆FTP服务器
//            ftpClient.login("anonymous", "");
            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                //未连接到FTP，用户名或密码错误
                ftpClient.disconnect();
            } else {
                //FTP连接成功
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        assert ftpClient != null;

        ftpClient.user("anonymous");
        System.out.println(ftpClient.getReplyString());

        ftpClient.pass("");
        System.out.println(ftpClient.getReplyString());

        ftpClient.cwd("/pub");
        System.out.println(ftpClient.getReplyString());

        ftpClient.pasv();
        System.out.println(ftpClient.getReplyString());

        InputStream remoteInput = ftpClient.retrieveFileStream("/pub/robots.txt");
        System.out.println(ftpClient.getReplyString());
//        BufferedReader in = new BufferedReader(new InputStreamReader(remoteInput));
//        String line = null;
//        while((line = in.readLine()) != null) {
//            System.out.println(line);
//        }

        var byteArrayOutputStream = new ByteArrayOutputStream();
        int _byte;
        while ((_byte = remoteInput.read()) != -1)
            byteArrayOutputStream.write(_byte);

        byte[] bytes = byteArrayOutputStream.toByteArray();

        System.out.println(new String(bytes));
    }

}
