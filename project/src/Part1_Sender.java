import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

public class Part1_Sender {
    public static void main(String[] args) throws IOException {
        DatagramSocket ds=new DatagramSocket(8808); //建立通讯socket

        Random rd = new Random();
//        byte[] arr = new byte[7];
//        rd.nextBytes(arr);
//        System.out.println(arr);
        System.out.println(ds.getPort());

        for (int i = 0; i < 10; i++){
            byte[] bys=new byte[20];
            rd.nextBytes(bys);
            DatagramPacket dp=new DatagramPacket(bys,bys.length, InetAddress.getByName("10.20.170.250"),12345);//建立数据包，声明长度，接收端主机，端口号
            ds.send(dp);//发送数据

            System.out.println("Send: ");
            for (byte b: bys){
                System.out.println(b);
            }
        }


//        BufferedReader br=new BufferedReader(new InputStreamReader(System.in));//读取键盘输入流
//        String line;
//        while((line=br.readLine())!=null){
//            if("886".equals(line))
//                break;
//            byte[] bys=line.getBytes();
//            DatagramPacket dp=new DatagramPacket(bys,bys.length, InetAddress.getByName("10.20.170.250"),12345);//建立数据包，声明长度，接收端主机，端口号
//            ds.send(dp);//发送数据
//        }

        ds.close();

    }
}
