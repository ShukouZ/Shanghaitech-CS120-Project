import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Part1_Receiver {
    public static void main(String[] args) throws IOException {
        DatagramSocket ds=new DatagramSocket(12345); //接收端口号的消息
        while(true){
            byte[] bys=new byte[1024];
            DatagramPacket dp=new DatagramPacket(bys,bys.length);//建立信息包
            ds.receive(dp);//将socket的信息接收到dp里
            System.out.println("ip: "+ dp.getAddress());
            System.out.println("port: "+ dp.getPort());
            System.out.println("输入数据为：\n"+new String(dp.getData(),0,dp.getLength()));
//            System.out.println("Data:");
//            byte[] buffer = dp.getData();
//            for(int i=0; i<20; i++){
//                System.out.println("\t"+buffer[i]);
//            }
            System.out.println("--------------------------------------------------------------------------");
        }
        //ds.close();
    }
}