import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SW_Sender {
    // send window size
    private final int window_size;
    // last ACK received
    private int LAR;
    private final int frame_num;

    // generate carrier
    private static SinWave wave = new SinWave(0, Config.PHY_CARRIER_FREQ, Config.PHY_TX_SAMPLING_RATE);
    private static ArrayList<Float> carrier = wave.sample(Config.PHY_TX_SAMPLING_RATE);
    private final ArrayList<float[]> track_list;
    public boolean[] ACKList;
    private final int[] sendedList;

    private final AudioHw audioHw;

    private final int millisPerFrame;

    // !!NOTE: consider DICT
    private int window_timer;


    SW_Sender(String filePath, int _window_size, AudioHw _audioHW, int _millsPerFrame){
        // get 6250 bytes of data
        byte[] byte_data = Util.readFileByBytes(filePath, Config.FILE_BYTES);
        // get 6250*8 bits of data
        ArrayList<Integer> data = (ArrayList<Integer>) Arrays.stream(Util.bytesToBits(byte_data)).boxed().collect(Collectors.toList());

        // generate soundtrack for each frame
        frame_num = data.size() / Config.FRAME_SIZE;
        track_list = new ArrayList<>();
        for (int i = 0; i< frame_num; i++){
            track_list.add(frameToTrack(data.subList(i*Config.FRAME_SIZE, (i+1)*Config.FRAME_SIZE), i+1, false));
        }

        // init the audio driver
        audioHw = _audioHW;

        // init window
        window_size = _window_size;
        LAR = -1;
        window_timer = 0;

        // init ACK list and sended list
        ACKList = new boolean[frame_num];
        Arrays.fill(ACKList, false);
        sendedList = new int[frame_num];
        Arrays.fill(sendedList, 0);

        // set time for each frame
        millisPerFrame = _millsPerFrame;
    }

    public void sendWindowedFrame(){
        int current_frame = -1;
        while(current_frame < frame_num - 1){
            current_frame = LAR + 1;
            while ((int)System.currentTimeMillis() - window_timer < 1000){
                if (current_frame <= LAR + window_size){
                    if (sendedList[current_frame] > Config.MAC_RETRY_LIMIT){
                        System.out.println(current_frame + " reached retry limit.");
                        System.out.println("Stop sending.");
                    }
                    audioHw.PHYSend(track_list.get(current_frame));
                    sendedList[current_frame] ++;
                    current_frame ++;
                }
            }
            window_timer = (int)System.currentTimeMillis();
        }
    }

    public boolean[] getACKList(){
        return ACKList;
    }

    public void receiveACK(int id){
        if(id > LAR && id <= LAR+window_size) {
            LAR = id;
            window_timer = (int)System.currentTimeMillis();
        }
    }

    public void setACKList(boolean[] _ACKList){
        ACKList = _ACKList;
    }

    public void updateLAR(){
        for (int i=LAR+window_size; i>LAR; i--){
            if (ACKList[i]){
                LAR = i;
                break;
            }
        }
    }



    static float[] frameToTrack(List<Integer> frame_data, int idx, boolean isASK){
        // initialization
        List<Integer> frame;
        int zero_buffer_len = 10;
        // add preamble
        float[] track = new float[Config.preamble.length + 4 + Config.FRAME_SAMPLE_SIZE + zero_buffer_len];
        System.arraycopy(Config.preamble, 0, track, 0, Config.preamble.length);
        // add length flag for frame data
        if(isASK){
            for(int j=0; j<4; j++)
                track[Config.preamble.length+j] = -1.0f;
        }else{
            for(int j=0; j<4; j++)
                track[Config.preamble.length+j] = 1.0f;
        }

        // add frame data
        //// modulation
        frame = new ArrayList<>(Config.FRAME_SIZE + Config.ID_SIZE);
        //// part1: add idx
        int bit;
        for(int n = 0; n < Config.ID_SIZE; n++){
            bit = (idx & (1 << n)) >> n;
            frame.add(bit);
        }
        //// part2: add frame data
        if(frame_data != null) {
            frame.addAll(frame_data);
        }
        //// part3: cal CRC
        List<Integer> crc_code = CRC8.get_crc8(frame);
        //// part4: modulate
        float[] frame_wave = new float[Config.SAMPLE_PER_BIT *(frame.size()+ Config.CRC_SIZE)];
        modulate(frame, crc_code, frame_wave, carrier);

        // add frame to track
        System.arraycopy(frame_wave, 0, track, Config.preamble.length+4, frame_wave.length);
        // zero buffer
        for (int j=0; j<zero_buffer_len; j++)   track[Config.preamble.length+4+frame_wave.length+j] = 0.0f;

        return track;
    }

    static void modulate(List<Integer> frame, List<Integer> crc_code, float[] frame_wave, ArrayList<Float> carrier) {
        for(int j=0; j<frame.size(); ++j){
            for(int k = 0; k< Config.SAMPLE_PER_BIT; ++k){
                frame_wave[j* Config.SAMPLE_PER_BIT +k] = carrier.get(j* Config.SAMPLE_PER_BIT +k) * (frame.get(j)*2-1);
            }
        }
        for(int j=frame.size(); j<frame.size() + Config.CRC_SIZE; ++j){
            for(int k = 0; k< Config.SAMPLE_PER_BIT; ++k){
                frame_wave[j* Config.SAMPLE_PER_BIT +k] = carrier.get(j* Config.SAMPLE_PER_BIT +k) * (crc_code.get(j-frame.size())*2-1);
            }
        }
    }


}
