import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Random;

public class Receiver {

    public static void main(final String[] args) throws IOException {
        AudioHw audioHw = new AudioHw();
        audioHw.init();
        audioHw.start();

        SW_Receiver receiver = new SW_Receiver(audioHw);
//        for (int i = 0; i < 100; i++) {
//            receiver.sendACK(i);
//        }

        DecodeThread decodeThread  = new DecodeThread(audioHw, receiver, null, Config.NODE_2_CODE);
        decodeThread.start();
        try {
            Thread.sleep(20000);  // ms
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        decodeThread.stopDecoding();
        byte[] bytes = receiver.writeFile("src/OUTPUT.txt");
        audioHw.stop();

//        ////////////////////////////////////////////////////////
        DatagramSocket ds=new DatagramSocket(8818); //建立通讯socket

        DatagramPacket dp=new DatagramPacket(bytes,bytes.length, InetAddress.getByName("10.20.170.250"),12345);//建立数据包，声明长度，接收端主机，端口号
        ds.send(dp);//发送数据
    }
}
