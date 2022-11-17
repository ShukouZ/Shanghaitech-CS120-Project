import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class SendDemo {
    public static void main(String[] args) throws IOException {
        DatagramSocket ds=new DatagramSocket(); //建立通讯socket

        BufferedReader br=new BufferedReader(new InputStreamReader(System.in));//读取键盘输入流
        String line;
        while((line=br.readLine())!=null){
            if("886".equals(line))
                break;
            byte[] bys=line.getBytes();
            DatagramPacket dp=new DatagramPacket(bys,bys.length, InetAddress.getByName("10.20.170.250"),12345);//建立数据包，声明长度，接收端主机，端口号
            ds.send(dp);//发送数据
        }

        ds.close();

    }
}
//        版权声明：本文为CSDN博主「鸿蒙小白」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
//        原文链接：https://blog.csdn.net/qq_40662086/article/details/115165744