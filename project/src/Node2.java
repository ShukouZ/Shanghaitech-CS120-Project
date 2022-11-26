import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;
import jpcap.packet.ICMPPacket;
import jpcap.packet.IPPacket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;
import java.util.stream.Collectors;

public class Node2 {
    public static void node2_123() throws IOException{
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

        DatagramPacket dp=new DatagramPacket(bytes,bytes.length, InetAddress.getByName("10.19.74.124"),12345);//建立数据包，声明长度，接收端主机，端口号
        ds.send(dp);//发送数据
    }

    public static void node2_321() throws IOException {
        AudioHw audioHw = new AudioHw();
        audioHw.init();
        audioHw.start();

        DatagramSocket ds=new DatagramSocket(1234); //接收端口号的消息
        int idx = 0;

//        ArrayList<float[]> track_list = new ArrayList<>();
        while(true){
            byte[] bys=new byte[1024];
            DatagramPacket dp=new DatagramPacket(bys,bys.length);//建立信息包
            ds.receive(dp);//将socket的信息接收到dp里
            byte[] data = Arrays.copyOfRange(dp.getData(), 0, dp.getLength());
            String s = new String(data);
            if (s.equals("over")){
                break;
            }

            ArrayList<Integer> decoded_data = (ArrayList<Integer>) Arrays.stream(Util.bytesToBits(data)).boxed().collect(Collectors.toList());
            float[] track = SW_Sender.frameToTrack(decoded_data,
                    Config.NODE_1_CODE,
                    Config.NODE_3_CODE,
                    Config.TYPE_DATA,
                    idx++,
                    false,
                    Util.ipToLong(Config.node1_IP),
                    Util.ipToLong(Config.node3_IP),
                    Config.node1_Port,
                    Config.node3_Port,
                    decoded_data.size()
            );
//            track_list.add(track);
            ////////////////////////
            audioHw.PHYSend(track);
            ///////////////////////
            System.out.println("输入数据为：\n"+s);
            System.out.println("--------------------------------------------------------------------------");
        }
        ds.close();
        audioHw.stop();
    }

    public static void node2_ICMP() throws IOException{
        AudioHw audioHw = new AudioHw();
        audioHw.init();
        audioHw.start();

        DecodeThread decodeThread  = new DecodeThread(audioHw, null, null, Config.NODE_2_CODE);
        decodeThread.start();

        NetworkInterface[] devices = JpcapCaptor.getDeviceList();
        int index = 5;

        JpcapCaptor jpcap = null;

        // 打开网卡连接,此时还未开始捕获数据包;
        try {
            jpcap = JpcapCaptor.openDevice(devices[index], 1512, true,6000);
            jpcap.setFilter("proto ICMP", true);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("抓取数据包时出现异常!!");
        }

        long start_time = System.currentTimeMillis();

        while (System.currentTimeMillis() - start_time < 30000) {
            assert jpcap != null;
            try {
                ICMPPacket packet = (ICMPPacket) jpcap.getPacket();
                if (packet!=null)
                {
//                    System.out.println(packet);
                    if (packet.type == ICMPPacket.ICMP_ECHOREPLY){
                        // TODO: send reply to node1
                        ArrayList<Integer> data = (ArrayList<Integer>) Arrays.stream(Util.bytesToBits(packet.data)).boxed().collect(Collectors.toList());
                        long src_ip = Util.ipToLong(packet.src_ip.getHostAddress());
                        long dst_ip = Util.ipToLong(Config.node1_IP);

                        float[] track = SW_Sender.frameToTrack(data, Config.NODE_1_CODE, Config.NODE_2_CODE, Config.TYPE_ICMP_ECHO_REPLY, packet.id, false,
                                dst_ip, src_ip, 0, 0, data.size());
                        audioHw.PHYSend(track);



                    }
                }


            }catch (Exception e){

            }
        }


        decodeThread.stopDecoding();
        audioHw.stop();
    }

    public static void main(final String[] args) throws IOException {
//        node2_321();
        node2_123();
    }
}
