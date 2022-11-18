import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Part2_Receiver {
    public static void main(String[] args) throws IOException {
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
}
