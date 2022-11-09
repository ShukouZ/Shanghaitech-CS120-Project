import java.util.ArrayList;
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

    DecodeThread(AudioHw _audioHw, SW_Receiver _receiver, SW_Sender _sender, int _src){
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
    }

    public static void sendACK(int dest, int src, int type, int id){
        float[] track = SW_Sender.frameToTrack(null, dest, src, type, id, true);
        audioHw.PHYSend(track, false);
//        System.out.println("Send ACK: " + id);
    }




    private ArrayList<Integer> decodeFrame(ArrayList<Float> data_signal, int offset){
        float sum;
        int data_size;
        if (data_signal.size() == Config.FRAME_SAMPLE_SIZE + Config.MAX_OFFSET){
            data_size = Config.FRAME_SIZE + Config.CRC_SIZE;
//            System.out.println("Frame");
        }
        else {
            data_size = Config.ACK_SIZE + Config.CRC_SIZE;
//            System.out.println("ACK");
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
        List<Integer> calculated_crc = CRC16.get_crc16(decoded_data_foreach.subList(0, data_size - Config.CRC_SIZE));

        if(!transmitted_crc.equals(calculated_crc)){
            return null;
        }

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

                    // get src
                    int src = 0;
                    for (int i = 0; i < Config.SRC_SIZE; i++)
                    {
                        src += decoded_block_data.get(Config.DEST_SIZE + i) << i;
                    }
//                    System.out.println("src: " + src);

                    // get type
                    int type = 0;
                    for (int i = 0; i < Config.TYPE_SIZE; i++)
                    {
                        type += decoded_block_data.get(Config.DEST_SIZE + Config.SRC_SIZE + i) << i;
                    }
//                    System.out.println("type: " + type);

                    // get id
                    int id = 0;
                    for (int i = 0; i < Config.SEQ_SIZE; i++)
                    {
                        id += decoded_block_data.get(Config.DEST_SIZE + Config.SRC_SIZE + Config.TYPE_SIZE + i) << i;
                    }
//                    System.out.println("id: " + id);

                    if (type == Config.TYPE_ACK) {
                        // ACK
                        sender.receiveACK(id);
                        System.out.println("Data block " + frame_decoded_num + " received ACK: " + id);

                    } else if (type == Config.TYPE_DATA){
                        System.out.println("Data block " + frame_decoded_num + " received data: " + id);
                        // write data
                        receiver.storeFrame(decoded_block_data.subList(Config.DEST_SIZE + Config.SRC_SIZE + Config.TYPE_SIZE + Config.SEQ_SIZE, Config.PAYLOAD_SIZE + Config.DEST_SIZE + Config.SRC_SIZE + Config.TYPE_SIZE + Config.SEQ_SIZE), id);
                        sendACK(src, node_id, Config.TYPE_ACK, receiver.getReceivedSize());
                    }
                    else if (type == Config.TYPE_PERF){
//                        System.out.println("Data block " + frame_decoded_num + " received perf: " + id);
//                        if (id == perf_id){
//                            perf_id++;
//                        }
//                        sendACK(src, node_id, Config.TYPE_ACK, perf_id);
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
                    }
                    else if (type == Config.TYPE_SEND_REPLY){
                        audioHw.channelFree = true;
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
