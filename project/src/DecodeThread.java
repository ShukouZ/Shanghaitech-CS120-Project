import java.util.ArrayList;
import java.util.List;

public class DecodeThread extends Thread {
    private boolean running;
    private final AudioHw audioHw;

    private final ArrayList<Float> carrier;
    private final float[] data_signal_remove_carrier = new float[Config.FRAME_SAMPLE_SIZE];
    private final ArrayList<Integer> decoded_data_foreach = new ArrayList<>();

    private final SW_Receiver receiver;
    private final SW_Sender sender;
    private final int node_id;


    DecodeThread(AudioHw _audioHw, SW_Receiver _receiver, SW_Sender _sender, int _src){
        running = true;
        audioHw = _audioHw;
        receiver = _receiver;
        sender = _sender;
        node_id = _src;

        // init the carrier
        SinWave wave = new SinWave(0, Config.PHY_CARRIER_FREQ, Config.PHY_SAMPLING_RATE);
        carrier = wave.sample(Config.PHY_SAMPLING_RATE);
    }

    private ArrayList<Integer> decodeFrame(ArrayList<Float> data_signal, int offset){
        float sum;
        int data_size;
        if (data_signal.size() == Config.FRAME_SAMPLE_SIZE + 8){
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

                // find best start id by crc
                decoded_block_data = decodeFrame(data_signal, 4);
                if (decoded_block_data == null) {
                    for (int offset = 0; offset < 8; offset++) {
                        decoded_block_data = decodeFrame(data_signal, offset);
                        if (decoded_block_data != null) break;
                    }
                }


                if (decoded_block_data == null) {
                    // not found
                    System.out.println(frame_decoded_num + " data block receiving ERR!!!!!!!!");
                }
                else {

                    // get dest
                    int dest = 0;
                    for (int n = 0; n < Config.DEST_SIZE; n++)
                    {
                        dest += decoded_block_data.get(n) << n;
                    }

                    if (dest != node_id) continue;

                    // get src
                    int src = 0;
                    for (int n = Config.DEST_SIZE; n < Config.DEST_SIZE + Config.SRC_SIZE; n++)
                    {
                        src += decoded_block_data.get(n) << n;
                    }

                    // get type
                    int type = 0;
                    for (int n = Config.DEST_SIZE + Config.SRC_SIZE; n < Config.DEST_SIZE + Config.SRC_SIZE + Config.TYPE_SIZE; n++)
                    {
                        type += decoded_block_data.get(n) << n;
                    }

                    // get id
                    int id = 0;
                    for (int n = Config.DEST_SIZE + Config.SRC_SIZE + Config.TYPE_SIZE; n < Config.DEST_SIZE + Config.SRC_SIZE + Config.TYPE_SIZE + Config.SEQ_SIZE; n++)
                    {
                        id += decoded_block_data.get(n) << n;
                    }

                    if (type == Config.TYPE_ACK) {
                        // ACK
                        System.out.println("Data block " + frame_decoded_num + " received ACK: " + id);
                        sender.receiveACK(id);
                    } else if (type == Config.TYPE_DATA){
                        id --;
                        System.out.println("Data block " + frame_decoded_num + " received data: " + id);
                        // write data
                        receiver.sendACK(src, node_id);
                        receiver.storeFrame(decoded_block_data.subList(Config.SEQ_SIZE, Config.PAYLOAD_SIZE + Config.SEQ_SIZE), id);
                    }
                    else if (type == Config.TYPE_PERF){

                    }else if (type == Config.TYPE_PING_REQ){

                    }
                    else if (type == Config.TYPE_PING_REPLY){

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
