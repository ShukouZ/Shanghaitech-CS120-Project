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
        sendACK(id);
        if (frame_list.get(id) == null){
            ArrayList<Integer> new_frame_data = new ArrayList<>(frame_data.size());
            new_frame_data.addAll(frame_data);
            frame_list.set(id, new_frame_data);
        }
    }

    public void sendACK(int id){
        float[] track = SW_Sender.frameToTrack(null, id, true);
        audioHw.PHYSend(track);
        System.out.println("Send ACK: " + id);
    }

    public void writeFile(){
        byte[] output = new byte[Config.FILE_BYTES * 8];
        for (int i = 0; i < Config.FILE_BYTES * 8; i++){
            int frame_id = i / Config.FILE_BYTES;
            ArrayList<Integer> frame = frame_list.get(frame_id);
            if (frame != null){
                for (int datum : frame){
                    output[i] = (byte) datum;
                }
            }
        }

        Util.outputbits(output, output.length);
    }

}
