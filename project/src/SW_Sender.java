import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SW_Sender {
    // send window size
    private final int window_size;
    // last ACK received
    public int LAR;
    private int frame_num;

    // generate carrier
    private static final SinWave wave = new SinWave(0, Config.PHY_CARRIER_FREQ, Config.PHY_SAMPLING_RATE);
    private static final ArrayList<Float> carrier = wave.sample(Config.PHY_SAMPLING_RATE);
    private final ArrayList<float[]> track_list;
    private final int[] sentList;

    private final AudioHw audioHw;
    private final int millisPerFrame;

    // !!NOTE: consider DICT
    private int window_timer;

    private final int window_duration;

    private int dest;
    private int src;

    private final boolean waitChannelFree;

    SW_Sender(String filePath, int _window_size, AudioHw _audioHW, int _millsPerFrame, int _window_duration, int _dest, int _src, int type, boolean _waitChannelFree,
              String destIP, String srcIP, int destPort, int srcPort, String content){
        // get list of variable-size frames
        ArrayList<byte[]> file_data;
        if(type < Config.TYPE_COMMAND_USER) {
            file_data = Util.readTxtByBytes(filePath);
        }else{
            file_data = new ArrayList<>();
            file_data.add(content.getBytes());
        }
        // generate soundtrack for each frame
        frame_num = 0;
        track_list = new ArrayList<>();
        for (byte[] lineData: file_data){
            ArrayList<Integer> data = (ArrayList<Integer>) Arrays.stream(Util.bytesToBits(lineData)).boxed().collect(Collectors.toList());
            track_list.add(frameToTrack(data, _dest, _src, type, frame_num++, false,
                    Util.ipToLong(destIP), Util.ipToLong(srcIP), destPort, srcPort, data.size()));
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

        dest = _dest;
        src = _src;


        waitChannelFree = _waitChannelFree;
    }

    public void sendFrame(){
        int current_frame = LAR;
        window_timer = (int)System.currentTimeMillis();
        while (LAR < frame_num - 1){
//            System.out.println("current frame: " + current_frame);
//            System.out.println("LAR: " + LAR);
            if (current_frame <= LAR || (int)System.currentTimeMillis() - window_timer > window_duration){
                if ((int)System.currentTimeMillis() - window_timer > window_duration){
                    // reached retry limit
                    if (sentList[current_frame] == Config.MAC_RETRY_LIMIT){
                        System.out.println("\n"+current_frame + " reached retry limit.");
                        System.out.println("Stop sending.");
                        return;
                    }

                    // out of time, send current frame again
                    System.out.println("Out of time, send " + current_frame + " again.");
                }

                current_frame = LAR + 1;

//                while (!audioHw.isIdle()){
//                    System.out.print("\rNoisy");
//                    Thread.yield();
//                }
//                System.out.println();

                // sending
                System.out.println("SW_SENDER:\tCurrent Frame: "+(current_frame));

                if (!audioHw.PHYSend(track_list.get(current_frame))){
                    System.out.println("Send require no reply.");
                    System.out.println("Stop sending.");
                    return;
                }
                sentList[current_frame]++;

                window_timer = (int)System.currentTimeMillis();


                LAR += 1;


                try{
                    Thread.sleep(millisPerFrame);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            Thread.yield();
        }
    }

    public void sendWindowedFrame(){
        int current_frame;
        while(LAR < frame_num - 1){
            current_frame = LAR + 1;
            while ((int)System.currentTimeMillis() - window_timer < window_duration || (current_frame <= LAR + window_size && current_frame < frame_num)){
                if (current_frame <= LAR + window_size && current_frame < frame_num){
                    if (sentList[current_frame] > Config.MAC_RETRY_LIMIT){
                        System.out.println("\n"+current_frame + " reached retry limit.");
                        System.out.println("Stop sending.");
                        return;
                    }
                    if(current_frame>LAR) {
                        System.out.println("SW_SENDER:\tCurrent Frame: "+(current_frame)+" with LAR: "+ LAR);
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
//            System.out.println("LAR: " + LAR);
            window_timer = (int)System.currentTimeMillis();
        }
    }


    static float[] frameToTrack(List<Integer> frame_data, int dest, int src, int type, int idx, boolean isASK,
                                long destIP, long srcIP, int destPort, int srcPort, int validDataLen){
        // initialization
        List<Integer> frame;
        int zero_buffer_len;
        int track_size;
        float len_data;
        if(isASK){
            zero_buffer_len = Config.HW_BUFFER_SIZE - (Config.LEN_SIZE + Config.ACK_SAMPLE_SIZE) + 1;
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
        if(!isASK){
            for(int n = 0; n < Config.DEST_IP_SIZE; n++){
                bit = (int)((destIP & (1 << n)) >> n);
                frame.add(bit);
            }

            for(int n = 0; n < Config.SRC_IP_SIZE; n++){
                bit = (int)((srcIP & (1 << n)) >> n);
                frame.add(bit);
            }

            for(int n = 0; n < Config.DEST_PORT_SIZE; n++){
                bit = (destPort & (1 << n)) >> n;
                frame.add(bit);
            }

            for(int n = 0; n < Config.SRC_PORT_SIZE; n++){
                bit = (srcPort & (1 << n)) >> n;
                frame.add(bit);
            }

            for(int n = 0; n < Config.VALID_DATA_SIZE; n++){
                bit = (validDataLen & (1 << n)) >> n;
                frame.add(bit);
            }
        }
        //// part 5: add frame data
        if(frame_data != null) {
            frame.addAll(frame_data);
            for(int i=0; i<(Config.PAYLOAD_SIZE-validDataLen); i++){
                frame.add(0);
            }
        }
        //// part 6: cal CRC
        List<Integer> crc_code = crc32.get_crc(frame);
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
