import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;

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
        // init the audio
        final AudioHw r = new AudioHw();
        r.init();

//////////////////////////////////////////////////////////// TRANSMITTER ////////////////////////////////////////////////////////////
        // init the frame
        ArrayList<Float> track1 = new ArrayList<>();
        // add zero buffer
        for (int j = 0; j < 48000; j++){
            track1.add(0.0f);
        }
        String filename = "src/INPUT.txt";
        ArrayList<Integer> frame_data = readTxt(filename);

        // init the carrier
        SinWave wave = new SinWave(0, Config.PHY_CARRIER_FREQ, Config.PHY_TX_SAMPLING_RATE);
        ArrayList<Float> carrier = wave.sample(Config.PHY_TX_SAMPLING_RATE);

        // compute frame buffer size
        int crc_length = Config.CHECK_SIZE;
        int frame_size = Config.FRAME_SIZE;
        int frame_num = 10000 / frame_size;
        int zero_buffer_length = 100;
        List<Integer> frame;
        List<Integer> crc_code;


        // add preamble and frame, each frame has 100 bits
        for(int i=0; i<frame_num; ++i){
            // add zero buffer1
            for (int j = 0; j < zero_buffer_length; j++){
                track1.add(0.0f);
            }
            // add preamble
            track1.addAll(Arrays.asList(Config.preamble));

            // modulation
            frame = frame_data.subList(i*frame_size, i*frame_size+frame_size);
            float[] frame_wave = new float[48*(frame_size+crc_length)];


//            if(i==1){
//                try {
//                    FileWriter writer = new FileWriter("src/1.txt");
//                    for (Integer decoded_datum : frame) {
//                        writer.write(String.valueOf(decoded_datum));
//                    }
//                    writer.close();
//                }catch (Exception e){
//                    System.out.println("Cannot read file.");
//                }
//            }

            //// calculate crc8
            crc_code = CRC8.get_crc8(frame);
            //// end of crc code calculation

            for(int j=0; j<frame.size(); ++j){
                for(int k=0; k<48; ++k){
                    frame_wave[j*48+k] = carrier.get(j*48+k) * (frame.get(j)*2-1); //  baud rate 48/48000 = 1000bps
                }
            }
            for(int j=frame_size; j<frame_size + crc_length; ++j){
                for(int k=0; k<48; ++k){
                    frame_wave[j*48+k] = carrier.get(j*48+k) * (crc_code.get(j - frame_size)*2-1); //  baud rate 48/48000 = 1000bps
                }
            }


            // add frame to track
            for (float v : frame_wave)
                track1.add(v);

            // add zero buffer2
            for (int j = 0; j < zero_buffer_length; j++){
                track1.add(0.0f);
            }

        }
        System.out.println("Size of track:"+track1.size());
        r.play(track1);


        r.start();
        try {
            Thread.sleep(15000);  // ms
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        r.stop();
    }
}
