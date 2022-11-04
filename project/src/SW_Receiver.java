import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SW_Receiver {
    // window size
    // last ACK received
    private int LAR;
    private final ArrayList<ArrayList<Integer>> frame_list;
    private final ArrayList<Float> carrier;

    private final AudioHw audioHw;

    SW_Receiver(AudioHw _audioHW){
        audioHw = _audioHW;
        frame_list = new ArrayList<>();

        // generate carrier
        SinWave wave = new SinWave(0, Config.PHY_CARRIER_FREQ, Config.PHY_TX_SAMPLING_RATE);
        carrier = wave.sample(Config.PHY_TX_SAMPLING_RATE);

        for (int i = 0; i < Config.FILE_BYTES * 8 / Config.FRAME_SIZE; i++){
            frame_list.add(null);
        }

    }

    public void storeFrame(List<Integer> frame_data, int id){
        ArrayList<Integer> new_frame_data = new ArrayList<>(frame_data);
        frame_list.set(id, new_frame_data);
        sendACK(id);
    }

    public void sendACK(int id){
        ArrayList<Integer> frame = new ArrayList<>();

        frame = new ArrayList<>(Config.ID_SIZE);
        ArrayList<Float> track = new ArrayList<>(Arrays.asList(Config.preamble));
        for(int j=0; j<4; j++)            track.add(-1.0f);

        int _bit;
        for(int n = 0; n < Config.ID_SIZE; n++){
            _bit = (id & (1 << n)) >> n;
            frame.add(_bit);
        }
        float[] frame_wave = new float[Config.SAMPLE_PER_BIT *(frame.size()+Config.CRC_SIZE)];
        List<Integer> crc_code = CRC8.get_crc8(frame);
        for(int j=0; j<frame.size(); ++j){
            for(int k = 0; k< Config.SAMPLE_PER_BIT; ++k){
                frame_wave[j* Config.SAMPLE_PER_BIT +k] = carrier.get(j* Config.SAMPLE_PER_BIT +k) * (frame.get(j)*2-1); //  baud rate 48/48000 = 1000bps
            }
        }
        for(int j=frame.size(); j<frame.size() + Config.CRC_SIZE; ++j){
            for(int k = 0; k< Config.SAMPLE_PER_BIT; ++k){
                frame_wave[j* Config.SAMPLE_PER_BIT +k] = carrier.get(j* Config.SAMPLE_PER_BIT +k) * (crc_code.get(j-frame.size())*2-1); //  baud rate 48/48000 = 1000bps
            }
        }
        for (float v : frame_wave)
            track.add(v);

        audioHw.PHYSend(track);
    }

    public void writeFile(){
        byte[] output = new byte[Config.FILE_BYTES * 8];
        for (int i = 0; i < Config.FILE_BYTES * 8; i++){
            int frame_id = i % Config.FILE_BYTES;
            ArrayList<Integer> frame = frame_list.get(frame_id);
            if (frame != null){
                for (int datum : frame){
                    output[i] = (byte) datum;
                }
            }
        }

        Util.outputbits(output, Config.FILE_BYTES * 8);
    }

}
