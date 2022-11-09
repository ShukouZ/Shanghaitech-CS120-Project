import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SW_Sender {
    // send window size
    private final int window_size;
    // last ACK received
    public int LAR;
    private final int frame_num;

    // generate carrier
    private static final SinWave wave = new SinWave(0, Config.PHY_CARRIER_FREQ, Config.PHY_SAMPLING_RATE);
    private static final ArrayList<Float> carrier = wave.sample(Config.PHY_SAMPLING_RATE);
    private final ArrayList<float[]> track_list;
    private final int[] sentList;

    private final AudioHw audioHw;
    private final int millisPerFrame;

    // !!NOTE: consider DICT
    private int window_timer;

    private int window_duration;

    SW_Sender(String filePath, int _window_size, AudioHw _audioHW, int _millsPerFrame, int _window_duration, int dest, int src, int type){
        // get 6250 bytes of data
        byte[] byte_data = Util.readFileByBytes(filePath, Config.FILE_BYTES);
        // get 6250*8 bits of data
        ArrayList<Integer> data = (ArrayList<Integer>) Arrays.stream(Util.bytesToBits(byte_data)).boxed().collect(Collectors.toList());

        // generate soundtrack for each frame
        frame_num = data.size() / Config.PAYLOAD_SIZE;
        track_list = new ArrayList<>();
        for (int i = 0; i< frame_num; i++){
            track_list.add(frameToTrack(data.subList(i*Config.PAYLOAD_SIZE, (i+1)*Config.PAYLOAD_SIZE), dest, src, type, i, false));
        }

        // init the audio driver
        audioHw = _audioHW;

        // init window
        window_size = _window_size;
        window_duration = _window_duration;
        LAR = -1;
        window_timer = 0;

        // init sent list
        sentList = new int[frame_num];
        Arrays.fill(sentList, 0);

        // set time for each frame
        millisPerFrame = _millsPerFrame;
    }

    public void sendWindowedFrame(){
        int current_frame;
        while(LAR < frame_num - 1){
            current_frame = LAR + 1;
            while ((int)System.currentTimeMillis() - window_timer < window_duration){
                if (current_frame <= LAR + window_size && current_frame < frame_num){
                    if (sentList[current_frame] > Config.MAC_RETRY_LIMIT){
                        System.out.println("\n"+current_frame + " reached retry limit.");
                        System.out.println("Stop sending.");
                        return;
                    }
                    if(current_frame>LAR) {
//                        System.out.println("SW_SENDER:\tCurrent Frame: "+(current_frame)+" with LAR: "+ LAR);
                        audioHw.PHYSend(track_list.get(current_frame));
                        sentList[current_frame]++;
                        try{
                            Thread.sleep(millisPerFrame);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    current_frame ++;


                }
            }
            window_timer = (int)System.currentTimeMillis();
        }

    }

    public void receiveACK(int id){
        id--;
        if(id > LAR && id <= LAR+window_size) {
            LAR = id;
            window_timer = (int)System.currentTimeMillis();
        }
    }



    static float[] frameToTrack(List<Integer> frame_data, int dest, int src, int type, int idx, boolean isASK){
        // initialization
        List<Integer> frame;
        int zero_buffer_len;
        int track_size;
        float len_data;
        if(isASK){
            zero_buffer_len = Config.HW_BUFFER_SIZE;
            track_size = Config.preamble.length + Config.LEN_SIZE + Config.ACK_SAMPLE_SIZE;
            len_data = -1.0f;
        }else{
            zero_buffer_len = 10;
            track_size = Config.preamble.length + Config.LEN_SIZE + Config.FRAME_SAMPLE_SIZE;
            len_data = 1.0f;
        }
        // add preamble
        float[] track = new float[track_size+zero_buffer_len];
        System.arraycopy(Config.preamble, 0, track, 0, Config.preamble.length);
        // add length flag for frame data
        for(int j=0; j<Config.LEN_SIZE; j++)
            track[Config.preamble.length+j] = len_data;

        // add frame data
        //// modulation
        frame = new ArrayList<>();
        //// part 1: add dest
        int bit;
        for(int n = 0; n < Config.DEST_SIZE; n++){
            bit = (dest & (1 << n)) >> n;
            frame.add(bit);
        }
        //// part 2: add src
        for(int n = 0; n < Config.SRC_SIZE; n++){
            bit = (src & (1 << n)) >> n;
            frame.add(bit);
        }
        //// part 3: add type
        for(int n = 0; n < Config.TYPE_SIZE; n++){
            bit = (type & (1 << n)) >> n;
            frame.add(bit);
        }
        //// part 4: add seq
        for(int n = 0; n < Config.SEQ_SIZE; n++){
            bit = (idx & (1 << n)) >> n;
            frame.add(bit);
        }
        //// part 5: add frame data
        if(frame_data != null) {
            frame.addAll(frame_data);
        }
        //// part 6: cal CRC
        List<Integer> crc_code = CRC16.get_crc16(frame);
        //// part 7: modulate
        float[] frame_wave = new float[Config.SAMPLE_PER_BIT *(frame.size()+ Config.CRC_SIZE)];
        // modulate
        for(int j=0; j<frame.size(); ++j){
            for(int k = 0; k< Config.SAMPLE_PER_BIT; ++k){
                frame_wave[j* Config.SAMPLE_PER_BIT +k] = SW_Sender.carrier.get(j* Config.SAMPLE_PER_BIT +k) * (frame.get(j)*2-1);
            }
        }
        for(int j=frame.size(); j<frame.size() + Config.CRC_SIZE; ++j){
            for(int k = 0; k< Config.SAMPLE_PER_BIT; ++k){
                frame_wave[j* Config.SAMPLE_PER_BIT +k] = SW_Sender.carrier.get(j* Config.SAMPLE_PER_BIT +k) * (crc_code.get(j-frame.size())*2-1);
            }
        }
        // add frame to track
        System.arraycopy(frame_wave, 0, track, Config.preamble.length + Config.LEN_SIZE, frame_wave.length);
        // zero buffer
        for (int j=0; j<zero_buffer_len; j++)   track[track_size+j] = 0.0f;

        return track;
    }


}
