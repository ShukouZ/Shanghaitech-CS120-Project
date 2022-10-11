import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;

public class test {
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

        // add preamble and frame, each frame has 100 bits
        for(int i=0; i<frame_num; ++i){
            // add zero buffer1
            for (int j = 0; j < zero_buffer_length; j++){
                track1.add(0.0f);
            }
            // add preamble
            track1.addAll(Arrays.asList(Config.preamble));

            // modulation
            List<Integer> frame = frame_data.subList(i*frame_size, i*frame_size+frame_size);
            float[] frame_wave = new float[48*(frame_size+crc_length)];

            //// calculate crc8
            List<Integer> crc_code = CRC8.get_crc8(frame);
            frame.addAll(crc_code);
            //// end of crc code calculation

            for(int j=0; j<frame.size(); ++j){
                for(int k=0; k<48; ++k){
                    frame_wave[j*48+k] = carrier.get(j*48+k) * (frame.get(j)*2-1); //  baud rate 48/48000 = 1000bps
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

//////////////////////////////////////////////////////////// RECEIVER ////////////////////////////////////////////////////////////
        ArrayList<Float> recorded = r.getRecorded();

////////// Task 1: Find all start indexes
        // Initialization
        float power = 0;
        int start_index = 0;
        float[] syncPower_debug = new float[recorded.size()];
        float syncPower_localMax = 0;
        int preamble_size = Config.preamble.length;
        List<Float> syncFIFO = new ArrayList<>();

        Arrays.fill(syncPower_debug, 0);
        for(int i=0; i<preamble_size; ++i){
            syncFIFO.add(0.0f);
        }

        List<Integer> start_indexes = new ArrayList<>();

        float sum; // temp

        for(int i = 0; i < recorded.size(); i++){
            float current_sample = recorded.get(i);
            power =  power*(1.0f - 1.0f / 64.0f) + (float)Math.pow(current_sample, 2.0f) / 64.0f;

            syncFIFO.remove(0);
            syncFIFO.add(current_sample);

            sum = 0.0f;
            for (int j = 0; j < preamble_size; j++){
                sum += syncFIFO.get(j) * Config.preamble[j];
            }

            syncPower_debug[i] = sum / 200.0f;

            if ((syncPower_debug[i] > power * 2.0f) && (syncPower_debug[i] > syncPower_localMax) && (syncPower_debug[i] > 0.015f)) {
                syncPower_localMax = syncPower_debug[i];
                start_index = i;
            }
            else if ((i - start_index > 200) && (start_index != 0)){
                if(!start_indexes.contains(start_index)){
                    start_indexes.add(start_index);
                }
                // re init the local variables, prepare for next decode
                syncPower_localMax = 0.0f;
                syncFIFO.clear();
                for(int j=0; j<preamble_size; ++j){
                    syncFIFO.add(0.0f);
                }
            }
        }

        System.out.println();
        System.out.println("Start indexes:");
        for(int id : start_indexes){
            System.out.println(id);
        }

////////// Task 2: decode all frames
        List<Float> data_signal;
        float[] data_signal_remove_carrier = new float[48 * (frame_size+crc_length)];
        ArrayList<Integer> decoded_data = new ArrayList<>(10000);

        // decode & check crc
        for (int start_id : start_indexes){
            // Try index nearby the given id: [id-2, id+2]
            int[] potential_idx = new int[]{start_id-2, start_id-1, start_id, start_id+1, start_id+2};
            for(int id: potential_idx) {
                // find data signal
                data_signal = recorded.subList(id + 1, id + 1 + 48 * (frame_size + crc_length));

                // remove carrier
                for (int i = 0; i < 48 * (frame_size + crc_length); i++) {
                    data_signal_remove_carrier[i] = data_signal.get(i) * carrier.get(i);
                }

                // decode
                for (int i = 0; i < frame_size + crc_length; i++) {
                    sum = 0.0f;
                    for (int j = 10 + i * 48; j < 30 + i * 48; j++) {
                        sum += data_signal_remove_carrier[j];
                    }

                    if (sum > 0.0f) {
                        decoded_data.add(1);
                    } else {
                        decoded_data.add(0);
                    }
                }

                // check crc code
                List<Integer> transmitted_crc = decoded_data.subList(frame_size, frame_size + crc_length);
                List<Integer> calculated_crc = CRC8.get_crc8(decoded_data.subList(0, frame_size));
                if (!transmitted_crc.equals(calculated_crc)) {
                    System.out.println("CRC check fails at idx = " + id);
                }else{
                    // Since we only need decoded_data, break
                    break;
                }
            }
        }

        System.out.println(decoded_data.size());

        try {
            FileWriter writer = new FileWriter("src/OUTPUT.txt");
            for (Integer decoded_datum : decoded_data.subList(0, frame_size)) {
                writer.write(String.valueOf(decoded_datum));
            }
            writer.close();
        }catch (Exception e){
            System.out.println("Cannot read file.");
        }

//        try {
//            PrintWriter track_writer = new PrintWriter("track.txt");
//            for (float p : track1){
//                track_writer.println(p);
//            }
//            PrintWriter recorded_writer = new PrintWriter("recorded.txt");
//            for (float p : recorded){
//                recorded_writer.println(p);
//            }
//            PrintWriter power_writer = new PrintWriter("power.txt");
//            for (float p : power_debug){
//                power_writer.println(p);
//            }
//            PrintWriter sync_writer = new PrintWriter("sync.txt");
//            for (float p : syncPower_debug){
//                sync_writer.println(p);
//            }
//        }catch (Exception e){
//            System.out.println("Cannot read file.");
//        }

//        float max = 0.0f;
//        for (float m : syncPower_debug){
//            if (max < m){
//                max = m;
//            }
//        }
//
//
//        for (int i = 0; i < syncPower_debug.length; i++){
//            if (syncPower_debug[i] > 0.07f){
//                System.out.println(i);
//                System.out.println(syncPower_debug[i]);
//                System.out.println(power_debug[i]);
//            }
//        }

//        System.out.println();
//        System.out.println(max);
//        System.out.println(recorded.size());
//        System.out.println(start_index);

//        ArrayList<Float> track = new ArrayList<>();
//        track.addAll(recorded.subList(0, recorded.size()));
//
//
//        final AudioHw speaker = new AudioHw();
//        speaker.init();
//        speaker.play(track);
//        speaker.start();
//        try {
//            Thread.sleep(5000);  // ms
//        } catch (final InterruptedException e) {
//            e.printStackTrace();
//        }
//        speaker.stop();
    }
}
