import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

public class DecodeThread extends Thread {
    private boolean running;
    private static AudioHw audioHw;

    private final ArrayList<Float> carrier;
    private final float[] data_signal_remove_carrier = new float[Config.FRAME_SAMPLE_SIZE];
    private final ArrayList<Integer> decoded_data_foreach = new ArrayList<>();

    private final SW_Receiver receiver;
    private SW_Sender sender;

    private final FTPClient ftpClient;
    private final int node_id;

    public boolean receivedPing;

    DecodeThread(AudioHw _audioHw, SW_Receiver _receiver, SW_Sender _sender, FTPClient _ftpClient, int _src) throws IOException {
        running = true;
        audioHw = _audioHw;
        receiver = _receiver;
        sender = _sender;
        ftpClient = _ftpClient;
        node_id = _src;
        receivedPing = false;

        // init the carrier
        SinWave wave = new SinWave(0, Config.PHY_CARRIER_FREQ, Config.PHY_SAMPLING_RATE);
        carrier = wave.sample(Config.PHY_SAMPLING_RATE);

    }

    public void updateSender(SW_Sender new_sender){
        sender = new_sender;
    }
    public static void sendACK(int dest, int src, int type, int id){
        float[] track = SW_Sender.frameToTrack(null, dest, src, type, id, true, 0, 0, 0, 0, 0);
        audioHw.PHYSend(track);
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
//                    System.out.println("destIP: " + Util.longToIP(destIP));
                    headSum += Config.DEST_IP_SIZE;
                    // get srcIP
                    long srcIP = 0;
                    for (int i = 0; i < Config.SRC_IP_SIZE; i++) {
                        srcIP += (long) decoded_block_data.get(headSum + i) << i;
                    }
//                    System.out.println("srcIP: " +  Util.longToIP(srcIP));

                    headSum += Config.SRC_IP_SIZE;
                    // get destPort
                    int destPort = 0;
                    for (int i = 0; i < Config.DEST_PORT_SIZE; i++) {
                        destPort += decoded_block_data.get(headSum + i) << i;
                    }
//                    System.out.println("destPort: " + destPort);

                    headSum += Config.DEST_PORT_SIZE;
                    // get srcPort
                    int srcPort = 0;
                    for (int i = 0; i < Config.SRC_PORT_SIZE; i++) {
                        srcPort += decoded_block_data.get(headSum + i) << i;
                    }
//                    System.out.println("srcPort: " + srcPort);

                    headSum += Config.SRC_PORT_SIZE;
                    // get validDataLen
                    int validDataLen = 0;
                    for (int i = 0; i < Config.VALID_DATA_SIZE; i++) {
                        validDataLen += decoded_block_data.get(headSum + i) << i;
                    }
//                    System.out.println("validDataLen: " + validDataLen);

                    headSum += Config.VALID_DATA_SIZE;

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


                    } else{
//                        System.out.println(type);
//                        System.out.println();

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

                        String content = new String(bytes, 14, bytes.length - 14);

                        sendACK(src, node_id, Config.TYPE_ACK, 1);

                        if (type == Config.TYPE_COMMAND_USER) {
                            try {
                                ftpClient.user(content);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            sendString(ftpClient.getReplyString(), src, Config.TYPE_COMMAND_REPLY);
                        } else if (type == Config.TYPE_COMMAND_PASS) {
                            try {
                                ftpClient.pass(content);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            sendString(ftpClient.getReplyString(), src, Config.TYPE_COMMAND_REPLY);
                        } else if (type == Config.TYPE_COMMAND_PWD) {
                            try {
                                ftpClient.pwd();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            sendString(ftpClient.getReplyString(), src, Config.TYPE_COMMAND_REPLY);
                        } else if (type == Config.TYPE_COMMAND_CWD) {
                            try {
                                ftpClient.cwd(content);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            sendString(ftpClient.getReplyString(), src, Config.TYPE_COMMAND_REPLY);
                        } else if (type == Config.TYPE_COMMAND_PASV) {
                            try {
                                ftpClient.pasv();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            sendString(ftpClient.getReplyString(), src, Config.TYPE_COMMAND_REPLY);
                        } else if (type == Config.TYPE_COMMAND_LIST) {
                            String[] list;
                            try {
                                list = ftpClient.listNames();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            sendStringList(list, src, Config.TYPE_COMMAND_REPLY);
                        } else if (type == Config.TYPE_COMMAND_RETR) {
                            InputStream remoteInput;
                            try {
                                remoteInput = ftpClient.retrieveFileStream(content);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            sendString(ftpClient.getReplyString(), src, Config.TYPE_COMMAND_REPLY);
                            var byteArrayOutputStream = new ByteArrayOutputStream();
                            int _byte;
                            while (true) {
                                try {
                                    if ((_byte = remoteInput.read()) == -1) break;
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                byteArrayOutputStream.write(_byte);
                            }

                            byte[] bytes_ = byteArrayOutputStream.toByteArray();
                            String filePath = "./src/file.bin";
                            Util.writeFileByBytes(bytes_, filePath);
                            sendFile(filePath, src, Config.TYPE_DATA);

                        } else if (type == Config.TYPE_COMMAND_REPLY) {
                            System.out.println(content);
                        }

                    }
                }

            }

            Thread.yield();
        }
    }


    public void stopDecoding(){
        running = false;
    }

    private void sendString(String str, int dest, int type){
        // send commands
        sender = new SW_Sender(null,
                10,
                audioHw,
                50,
                300,
                dest,
                node_id,
                type,
                false,
                Config.node3_IP,
                Config.node1_IP,
                Config.node3_Port,
                Config.node1_Port,
                Collections.singletonList(str));

        sender.sendFrame();
    }

    private void sendStringList(String[] str, int dest, int type){
        List<String> strList = Arrays.asList(str);
        strList.add("\n");
        // send commands
        sender = new SW_Sender(null,
                10,
                audioHw,
                50,
                300,
                dest,
                node_id,
                type,
                false,
                Config.node3_IP,
                Config.node1_IP,
                Config.node3_Port,
                Config.node1_Port,
                strList);

        sender.sendFrame();
    }

    private void sendFile(String filePath, int dest, int type){
        // send commands
        sender = new SW_Sender(filePath,
                10,
                audioHw,
                50,
                300,
                dest,
                node_id,
                type,
                false,
                Config.node3_IP,
                Config.node1_IP,
                Config.node3_Port,
                Config.node1_Port,
                null);

        sender.sendFrame();
    }
}
