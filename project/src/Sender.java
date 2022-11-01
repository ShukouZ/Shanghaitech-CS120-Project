import com.synthbot.jasiohost.AsioDriverState;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Sender {
    public static ArrayList<Integer> readTxt(String filePath) {
        ArrayList<Integer> input_list =  new ArrayList<>();
        try {
            File file = new File(filePath);
            if(file.isFile() && file.exists()) {
                InputStreamReader isr = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                String lineTxt = br.readLine();
                char[] ar = lineTxt.toCharArray();
                for (char c : ar) {
                    input_list.add(Integer.parseInt(String.valueOf(c)));
                }
                br.close();
            } else {
                System.out.println("文件不存在!");
            }
        } catch (Exception e) {
            System.out.println("错误!");
        }
        return input_list;
    }
    public static void main(final String[] args) {
        AudioHw audioHw = new AudioHw();
        audioHw.init();
        audioHw.start();

        // init the frame
        ArrayList<Float> track1 = new ArrayList<>();
        String filename = "src/INPUT.txt";
        ArrayList<Integer> frame_data = readTxt(filename);

        // init the carrier
        SinWave wave = new SinWave(0, Config.PHY_CARRIER_FREQ, Config.PHY_TX_SAMPLING_RATE);
        ArrayList<Float> carrier = wave.sample(Config.PHY_TX_SAMPLING_RATE);

        // compute frame buffer size
        int data_size = 10000;
        int crc_length = Config.CHECK_SIZE;
        int frame_size = Config.FRAME_SIZE;
        int frame_num = data_size / frame_size;
        List<Integer> frame;
        List<Integer> crc_code;


        // add preamble and frame, each frame has 100 bits
        for(int i=0; i<frame_num; ++i){
            track1.clear();
            // add preamble
            track1.addAll(Arrays.asList(Config.preamble));

            // modulation
            frame = new ArrayList<>(Config.FRAME_SIZE + Config.ID_SIZE);

            int bit;
            for(int n = 0; n < Config.ID_SIZE; n++){
                bit = (i & (1 << n)) >> n;
                frame.add(bit);
            }
            frame.addAll(frame_data.subList(i * frame_size, i * frame_size + frame_size));
            float[] frame_wave = new float[Config.SAMPLE_PER_BIT *(Config.ID_SIZE+frame_size+crc_length)];

            //// calculate crc8
            crc_code = CRC8.get_crc8(frame);
            //// end of crc code calculation

            for(int j=0; j<frame.size(); ++j){
                for(int k = 0; k< Config.SAMPLE_PER_BIT; ++k){
                    frame_wave[j* Config.SAMPLE_PER_BIT +k] = carrier.get(j* Config.SAMPLE_PER_BIT +k) * (frame.get(j)*2-1); //  baud rate 48/48000 = 1000bps
                }
            }
            for(int j=frame_size+Config.ID_SIZE; j<frame_size + Config.ID_SIZE + crc_length; ++j){
                for(int k = 0; k< Config.SAMPLE_PER_BIT; ++k){
                    frame_wave[j* Config.SAMPLE_PER_BIT +k] = carrier.get(j* Config.SAMPLE_PER_BIT +k) * (crc_code.get(j-frame_size-Config.ID_SIZE)*2-1); //  baud rate 48/48000 = 1000bps
                }
            }

            // add frame to track
            for (float v : frame_wave)
                track1.add(v);

            System.out.println("Send Idx:"+i+"\twith size:"+track1.size());
            System.out.println();
            audioHw.PHYSend(track1);

            try {
                Thread.sleep(400);  // ms
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }

            new Thread() {
                @Override
                public void run() {

                }
            }.start();

        }


        audioHw.stop();
    }
}
