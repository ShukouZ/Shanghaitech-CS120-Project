import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Node3 {
    public static void node3_receive() throws IOException{
        DatagramSocket ds=new DatagramSocket(1234); //接收端口号的消息
        while(true){
            byte[] bys=new byte[1024];
            DatagramPacket dp=new DatagramPacket(bys,bys.length);//建立信息包
            ds.receive(dp);//将socket的信息接收到dp里

            byte[] data = dp.getData();

            ArrayList<Integer> decoded_block_data = (ArrayList<Integer>) Arrays.stream(Util.bytesToBits(data)).boxed().collect(Collectors.toList());

            int headSum = 0;

            // get destIP
            long destIP = 0;
            for (int i = 0; i < Config.DEST_IP_SIZE; i++) {
                destIP += (long) decoded_block_data.get(headSum + i) << i;
            }
            System.out.println("destIP: " + Util.longToIP(destIP));
            headSum += Config.DEST_IP_SIZE;
            // get srcIP
            long srcIP = 0;
            for (int i = 0; i < Config.SRC_IP_SIZE; i++) {
                srcIP += (long) decoded_block_data.get(headSum + i) << i;
            }
            System.out.println("srcIP: " +  Util.longToIP(srcIP));

            headSum += Config.SRC_IP_SIZE;
            // get destPort
            int destPort = 0;
            for (int i = 0; i < Config.DEST_PORT_SIZE; i++) {
                destPort += decoded_block_data.get(headSum + i) << i;
            }
            System.out.println("destPort: " + destPort);

            headSum += Config.DEST_PORT_SIZE;
            // get srcPort
            int srcPort = 0;
            for (int i = 0; i < Config.SRC_PORT_SIZE; i++) {
                srcPort += decoded_block_data.get(headSum + i) << i;
            }
            System.out.println("srcPort: " + srcPort);

            headSum += Config.SRC_PORT_SIZE;
            // get validDataLen
            int validDataLen = 0;
            for (int i = 0; i < Config.VALID_DATA_SIZE; i++) {
                validDataLen += decoded_block_data.get(headSum + i) << i;
            }
            System.out.println("validDataLen: " + validDataLen);

            headSum += Config.VALID_DATA_SIZE;

            StringBuilder output = new StringBuilder();

            for (int datum: decoded_block_data.subList(headSum, headSum + validDataLen)){
                output.append(datum);
            }

            byte[] bytes = new byte[output.length() / 8];
            for (int i = 0; i < output.length() / 8; i++) {
                // System.out.print(bitStr+"|");
                bytes[i] = Util.BitToByte(output.substring(i*8,(i+1)*8));
            }

            System.out.println("ip: "+ Util.longToIP(srcIP));
            System.out.println("port: "+ srcPort);
            System.out.println("输入数据为：\n"+new String(bytes, 0, bytes.length));
//            System.out.println("Data:");
//            byte[] buffer = dp.getData();
//            for(int i=0; i<20; i++){
//                System.out.println("\t"+buffer[i]);
//            }
            System.out.println("--------------------------------------------------------------------------");
        }
        //ds.close();
    }

    public static void node3_send() throws IOException{
        ArrayList<byte[]> data_bytes = Util.readTxtByBytes("src/INPUT1.txt");

        DatagramSocket ds=new DatagramSocket(); //建立通讯socket

        for (byte[] data: data_bytes){
            DatagramPacket dp=new DatagramPacket(data,data.length, InetAddress.getByName("10.20.170.250"),Config.node3_Port);//建立数据包，声明长度，接收端主机，端口号
            ds.send(dp);//发送数据
            System.out.println("Send:"+new String(data, 0, data.length));
            try{
                Thread.sleep(500);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        ds.close();

    }
    public static void main(String[] args) throws IOException {
        node3_send();
    }
}
