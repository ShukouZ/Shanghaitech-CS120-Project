import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SW_Sender {
    // send window size
    private int window_size;
    // last ACK received
    private int LAR;
    private int frame_size;
    private final ArrayList<Float> carrier;
    private ArrayList<ArrayList<Float>> track_list;
    public boolean[] ACKList;
    private final int[] sendedList;

    private final AudioHw audioHw;

    private int millsPerFrame;

    SW_Sender(String filePath, int _window_size, AudioHw _audioHW, int _millsPerFrame){
        // get 6250 bytes of data
        byte[] byte_data = Util.readFileByBytes(filePath, 6250);
        // get 6250*8 bits of data
        ArrayList<Integer> data = (ArrayList<Integer>) Arrays.stream(Util.bytesToBits(byte_data)).boxed().collect(Collectors.toList());

        // generate carrier
        SinWave wave = new SinWave(0, Config.PHY_CARRIER_FREQ, Config.PHY_TX_SAMPLING_RATE);
        carrier = wave.sample(Config.PHY_TX_SAMPLING_RATE);

        // generate soundtrack for each frame
        frame_size = data.size() / Config.FRAME_SIZE;
        track_list = new ArrayList<>();
        for (int i=0; i<frame_size; i++){
            track_list.add(frameToTrack(data.subList(i*Config.FRAME_SIZE, (i+1)*Config.FRAME_SIZE), i+1));
        }

        // init the audio driver
        audioHw = _audioHW;

        // init window size
        window_size = _window_size;
        LAR = 0;
        //      xx  xx  xx  ............ xx
        //       |  |--------------------|
        //      LAR       window

        // init ACK list and sended list
        ACKList = new boolean[frame_size];
        Arrays.fill(ACKList, false);
        sendedList = new int[frame_size];
        Arrays.fill(sendedList, 0);

        // set time for each frame
        millsPerFrame = _millsPerFrame;
    }

    public void sendWindowedFrame(){
        if (LAR == frame_size){
            return;
        }
        for(int i=LAR; i<LAR+window_size && i < frame_size; i++){
            audioHw.PHYSend(track_list.get(i));
            try {
                Thread.sleep(millsPerFrame);  // ms
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Track "+(i+1)+" with size: "+track_list.get(i).size());
            LAR+=1;
        }
        sendWindowedFrame();
    }

    public boolean[] getACKList(){
        return ACKList;
    }

    public void setACKList(boolean[] _ACKList){
        ACKList = _ACKList;
    }

    public void updateLAR(){
        for(int i=LAR; i<frame_size; i++){
            if(ACKList[i]){
                LAR = i;
            }
            else{
                break;
            }
        }
    }



    private ArrayList<Float> frameToTrack(List<Integer> frame_data, int idx){
        // initialization
        List<Integer> frame;
        int zero_buffer_len = 10;
        // add preamble
        ArrayList<Float> track = new ArrayList<>(Arrays.asList(Config.preamble));
        // add length flag for frame data
        for(int j=0; j<4; j++)
            track.add(1.0f);
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
        frame.addAll(frame_data);
        //// part3: cal CRC
        List<Integer> crc_code = CRC8.get_crc8(frame);
        //// part4: modulate
        float[] frame_wave = new float[Config.SAMPLE_PER_BIT *(frame.size()+ Config.CRC_SIZE)];
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

        // add frame to track
        for (float v : frame_wave)
            track.add(v);

        // zero buffer
        for (int j=0; j<zero_buffer_len; j++)   track.add(0.0f);

        return track;
    }



}
