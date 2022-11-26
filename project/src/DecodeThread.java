import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DecodeThread extends Thread {
    private boolean running;
    private static AudioHw audioHw;

    private final ArrayList<Float> carrier;
    private final float[] data_signal_remove_carrier = new float[Config.FRAME_SAMPLE_SIZE];
    private final ArrayList<Integer> decoded_data_foreach = new ArrayList<>();

    private final SW_Receiver receiver;
    private final SW_Sender sender;
    private final int node_id;
    private int perf_id;

    public boolean receivedPing;

    private ICMP_Sender icmp_sender=null;

    DecodeThread(AudioHw _audioHw, SW_Receiver _receiver, SW_Sender _sender, int _src) throws IOException {
        running = true;
        audioHw = _audioHw;
        receiver = _receiver;
        sender = _sender;
        node_id = _src;
        perf_id = 0;
        receivedPing = false;

        // init the carrier
        SinWave wave = new SinWave(0, Config.PHY_CARRIER_FREQ, Config.PHY_SAMPLING_RATE);
        carrier = wave.sample(Config.PHY_SAMPLING_RATE);

        // init the icmp sender
        icmp_sender = new ICMP_Sender();
    }

    public static void sendACK(int dest, int src, int type, int id){
        float[] track = SW_Sender.frameToTrack(null, dest, src, type, id, true, 0, 0, 0, 0, 0, 0);
        audioHw.PHYSend(track, false);
        if (type == Config.TYPE_ACK){
//            System.out.println("Send ACK: " + id);
        } else if (type == Config.TYPE_SEND_REQ) {
//            System.out.println("Send require...");
        }else if (type == Config.TYPE_SEND_REPLY) {
//            System.out.println("Reply send require...");
        }else if (type == Config.TYPE_PING_REQ) {
//            System.out.println("Ping require...");
        }else if (type == Config.TYPE_PING_REPLY) {
//            System.out.println("Reply ping require...");
        }
    }




    private ArrayList<Integer> decodeFrame(ArrayList<Float> data_signal, int offset){
        float sum;
        int data_size;
//        System.out.println(data_signal.size());
        if (data_signal.size() < Config.FRAME_SAMPLE_SIZE + Config.MAX_OFFSET){
            data_size = Config.ACK_SIZE + Config.CRC_SIZE;
//            System.out.println("ACK");
        }
        else {
            data_size = Config.FRAME_SIZE + Config.CRC_SIZE;
//            System.out.println("Frame");
        }

        // decode frame
        // remove carrier
        for (int i = 0; i < Config.SAMPLE_PER_BIT * data_size; i++) {
            data_signal_remove_carrier[i] = data_signal.get(i + offset) * carrier.get(i);
        }

        // decode
        decoded_data_foreach.clear();
        for (int i = 0; i < data_size; i++) {
            sum = 0.0f;
            for (int j = i * Config.SAMPLE_PER_BIT; j < (i + 1) * Config.SAMPLE_PER_BIT; j++) {
                sum += data_signal_remove_carrier[j];
            }

            if (sum > 0.0f) {
                decoded_data_foreach.add(1);
            } else {
                decoded_data_foreach.add(0);
            }
        }

        // check crc code
        List<Integer> transmitted_crc = decoded_data_foreach.subList(data_size - Config.CRC_SIZE, data_size);
        List<Integer> calculated_crc = crc32.get_crc(decoded_data_foreach.subList(0, data_size - Config.CRC_SIZE));

        if(!transmitted_crc.equals(calculated_crc)){
            return null;
        }

//        System.out.println(offset);
        return new ArrayList<>(decoded_data_foreach.subList(0, data_size - Config.CRC_SIZE));
    }

    private int get_block_id(ArrayList<Integer> block){
        int id = 0;
        for (int n = 0; n < Config.SEQ_SIZE; n++)
        {
            id += block.get(n) << n;
        }
        return id;
    }


    @Override
    public void run(){
        int frame_decoded_num = 0;
        ArrayList<Float> data_signal;
        ArrayList<Integer> decoded_block_data;

        DatagramSocket ds= null; //建立通讯socket
        try {
            ds = new DatagramSocket();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        while (running) {
            data_signal = audioHw.getFrame(frame_decoded_num);

            if(data_signal != null){
                frame_decoded_num++;

                decoded_block_data = decodeFrame(data_signal, 3);

                if (decoded_block_data == null) {
                    // find best start id by crc
                    for (int offset = 4; offset < Config.MAX_OFFSET; offset++) {
                        decoded_block_data = decodeFrame(data_signal, offset);
                        if (decoded_block_data != null) break;
                    }
                }

                if (decoded_block_data == null) {
                    // find best start id by crc
                    for (int offset = 0; offset < 2; offset++) {
                        decoded_block_data = decodeFrame(data_signal, offset);
                        if (decoded_block_data != null) break;
                    }
                }


                if (decoded_block_data == null) {
                    // not found
//                    System.out.println(frame_decoded_num + " data block receiving ERR!!!!!!!!");
                }
                else {
                    // set the headSum
                    int headSum = 0;
                    // get dest
                    int dest = 0;
                    for (int i = 0; i < Config.DEST_SIZE; i++)
                    {
                        dest += decoded_block_data.get(i) << i;
                    }

                    if (dest != node_id) {
//                        System.out.println("dest: " + dest);
                        continue;
                    }
                    headSum += Config.DEST_SIZE;

                    // get src
                    int src = 0;
                    for (int i = 0; i < Config.SRC_SIZE; i++)
                    {
                        src += decoded_block_data.get(headSum + i) << i;
                    }
                    headSum += Config.SRC_SIZE;
//                    System.out.println("src: " + src);

                    // get type
                    int type = 0;
                    for (int i = 0; i < Config.TYPE_SIZE; i++)
                    {
                        type += decoded_block_data.get(headSum + i) << i;
                    }
                    headSum += Config.TYPE_SIZE;
//                    System.out.println("type: " + type);

                    // get id
                    int id = 0;
                    for (int i = 0; i < Config.SEQ_SIZE; i++)
                    {
                        id += decoded_block_data.get(headSum + i) << i;
                    }
                    headSum += Config.SEQ_SIZE;
                    int UDPStart = headSum;
//                    System.out.println("id: " + id);

                    if (type == Config.TYPE_ACK) {
                        // ACK
                        sender.receiveACK(id);
//                        System.out.println("Data block " + frame_decoded_num + " received ACK: " + id);
                        continue;
                    }

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
                    // get ICMP echo start time
                    int icmp_time = 0;
                    for (int i = 0; i < Config.ICMP_TIME_SIZE; i++) {
                        icmp_time += decoded_block_data.get(headSum + i) << i;
                    }
                    System.out.println("icmp_time: " + icmp_time);

                    if (type == Config.TYPE_DATA ){
                        System.out.println("Data block " + frame_decoded_num + " received data: " + id);
                        // write data
                        receiver.storeFrame(decoded_block_data.subList(headSum, validDataLen + headSum), id);
                        sendACK(src, node_id, Config.TYPE_ACK, receiver.getReceivedSize());
                        audioHw.state = Config.STATE_FRAME_DETECTION;

                        // transfer data(int) to data(bytes)
                        StringBuilder output = new StringBuilder();
                        for (int datum: decoded_block_data.subList(UDPStart, headSum+validDataLen)){
                            output.append(datum);
                        }
                        byte[] bytes = new byte[output.length() / 8];
                        for (int i = 0; i < output.length() / 8; i++) {
                            // System.out.print(bitStr+"|");
                            bytes[i] = Util.BitToByte(output.substring(i*8,(i+1)*8));
                        }

                        System.out.println(new String(bytes, 14, bytes.length - 14));
                        System.out.println("--------------------------------------------------------------");
                        System.out.println();

                        // UDP send packet
                        try {
//                            System.out.println(Arrays.toString(bytes));
                            DatagramPacket dp=new DatagramPacket(bytes,bytes.length, InetAddress.getByName(Util.longToIP(destIP)), destPort);//建立数据包，声明长度，接收端主机，端口号
                            ds.send(dp);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    }
                    else if (type == Config.TYPE_PERF){
//                        System.out.println("Data block " + frame_decoded_num + " received perf: " + id);
                        if (id == perf_id){
                            perf_id++;
                        }
                        sendACK(src, node_id, Config.TYPE_ACK, perf_id);
                        audioHw.state = Config.STATE_FRAME_DETECTION;
                    }else if (type == Config.TYPE_PING_REQ){
                        sendACK(src, node_id, Config.TYPE_PING_REPLY, id);
                    }
                    else if (type == Config.TYPE_PING_REPLY){
//                        int end_time = (int)System.currentTimeMillis() % 256;
//                        System.out.println("End: " + end_time);
//                        end_time = end_time > id ?
//                                end_time :
//                                end_time + 256;
//                        int duration = end_time - id;
                        receivedPing = true;
                        System.out.println("Ping: "+((int)System.currentTimeMillis() - audioHw.end_time)+" ms.");
                    }
                    else if (type == Config.TYPE_SEND_REQ){
                        sendACK(src, node_id, Config.TYPE_SEND_REPLY, id);
                        audioHw.state = Config.STATE_FRAME_RX;
                    }
                    else if (type == Config.TYPE_SEND_REPLY){
                        System.out.println("Reply received. Sending...");
                        audioHw.state = Config.STATE_FRAME_TX;
                    } else if (type == Config.TYPE_ICMP_ECHO) {
                        // transmit the message from node1 to node3: send an ICMP echo packet to node3
                        StringBuilder output = new StringBuilder();
                        for (int datum: decoded_block_data.subList(UDPStart, headSum+validDataLen)){
                            output.append(datum);
                        }
                        byte[] bytes = new byte[output.length() / 8];
                        for (int i = 0; i < output.length() / 8; i++) {
                            // System.out.print(bitStr+"|");
                            bytes[i] = Util.BitToByte(output.substring(i*8,(i+1)*8));
                        }

                        try {
                            icmp_sender.send(Config.node1_IP, Config.node3_IP, bytes, 1);
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    else if (type == Config.TYPE_ICMP_ECHO_REPLY) {
                        // output IP, payload and latency of the received ICMP packets
                        StringBuilder output = new StringBuilder();
                        for (int datum: decoded_block_data.subList(UDPStart, headSum+validDataLen)){
                            output.append(datum);
                        }
                        byte[] bytes = new byte[output.length() / 8];
                        for (int i = 0; i < output.length() / 8; i++) {
                            // System.out.print(bitStr+"|");
                            bytes[i] = Util.BitToByte(output.substring(i*8,(i+1)*8));
                        }

                        int current_time = (int)(System.currentTimeMillis() % Math.pow(2.0f, Config.ICMP_TIME_SIZE));
                        if(current_time<icmp_time){
                            current_time += (int)Math.pow(2.0f, Config.ICMP_TIME_SIZE);
                        }
                        System.out.println("IP:"+srcIP);
                        System.out.println("Payload:"+ Arrays.toString(bytes));
                        System.out.println("Latency:"+(current_time-icmp_time));
                    }
                }

            }

            Thread.yield();
        }
    }


    public void stopDecoding(){
        running = false;
    }
}
