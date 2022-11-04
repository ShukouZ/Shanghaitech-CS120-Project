import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class DecodeThread extends Thread {
    private boolean running;
    private final AudioHw audioHw;

    private boolean[] ACKList;
    private final int data_size = (Config.FRAME_SIZE+Config.CRC_SIZE +Config.ID_SIZE);

    private final ArrayList<Float> carrier;
    private final float[] data_signal_remove_carrier = new float[Config.SAMPLE_PER_BIT * data_size];
    private final ArrayList<Integer> decoded_data_foreach = new ArrayList<>();

    DecodeThread(AudioHw _audioHw){
        running = true;
        audioHw = _audioHw;

        // init the carrier
        SinWave wave = new SinWave(0, Config.PHY_CARRIER_FREQ, Config.PHY_TX_SAMPLING_RATE);
        carrier = wave.sample(Config.PHY_TX_SAMPLING_RATE);
    }

    private ArrayList<Integer> decodeFrame(ArrayList<Float> data_signal, int offset){

        float sum;
        int size;
        if (data_signal.size() == Config.FRAME_SAMPLE_SIZE + 8){
            size = data_size;
        }
        else {
            size = (Config.CRC_SIZE + Config.ID_SIZE);
        }

        // decode frame
        // remove carrier
        for (int i = 0; i < Config.SAMPLE_PER_BIT * size; i++) {
            data_signal_remove_carrier[i] = data_signal.get(i + offset) * carrier.get(i);
        }

        // decode
        decoded_data_foreach.clear();
        for (int i = 0; i < size; i++) {
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
        List<Integer> transmitted_crc = decoded_data_foreach.subList(size - Config.CRC_SIZE, size);
        List<Integer> calculated_crc = CRC8.get_crc8(decoded_data_foreach.subList(0, size - Config.CRC_SIZE));

        if(!transmitted_crc.equals(calculated_crc)){
            return null;
        }

        return new ArrayList<>(decoded_data_foreach.subList(0, size - Config.CRC_SIZE));
    }

    private int get_block_id(ArrayList<Integer> block){
        int id = 0;
        for (int n = 0; n < Config.ID_SIZE; n++)
        {
            id += block.get(n) << n;
        }
        return id;
    }


    @Override
    public void run(){



        int frame_decoded_num = 0;
        ArrayList<Float> data_signal;
        ArrayList<Integer> decoded_data = new ArrayList<>();
        ArrayList<Integer> decoded_block_data;

        while (running) {
            data_signal = audioHw.getFrame(frame_decoded_num);

            if(data_signal != null){
                decoded_block_data = decodeFrame(data_signal, 4);
                if (decoded_block_data == null) {
                    for (int offset = 0; offset < 8; offset++) {
                        decoded_block_data = decodeFrame(data_signal, offset);
                        if (decoded_block_data != null) break;
                    }
                }
                if (decoded_block_data == null) {
                    System.out.println(frame_decoded_num + " data block receiving ERR!!!!!!!!");
                }
                else {
                    if (decoded_block_data.size() == Config.ID_SIZE + Config.CRC_SIZE) {
                        // ACK
                        System.out.println("Data block " + frame_decoded_num + " received ACK: " + get_block_id(decoded_block_data));
                        ACKList[get_block_id(decoded_block_data)] = true;
                    } else {
                        System.out.println("Data block " + frame_decoded_num + " received data: " + get_block_id(decoded_block_data));
                        // TODO: write data
                        decoded_data.addAll(decoded_block_data.subList(Config.ID_SIZE, Config.FRAME_SIZE + Config.ID_SIZE));
                    }
                }

                frame_decoded_num++;
            }

            try {
                Thread.sleep(0);  // ms
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println(decoded_data.size() + " bits received.");

        try {
            FileWriter writer = new FileWriter("src/OUTPUT.txt");
            for (Integer decoded_datum : decoded_data) {
                writer.write(String.valueOf(decoded_datum));
            }
            writer.close();
        }catch (Exception e){
            System.out.println("Cannot read file.");
        }
    }

    public void setACKList(boolean[] ackList){
        ACKList = ackList;
    }

    public void stopDecoding(){
        running = false;
    }
}
