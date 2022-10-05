import java.util.*;
import java.io.*;

public class test {
    public static ArrayList<Float> readTxt(String filePath) {
        ArrayList<Float> input_list =  new ArrayList<Float>();
        try {
            File file = new File(filePath);
            if(file.isFile() && file.exists()) {
                InputStreamReader isr = new InputStreamReader(new FileInputStream(file), "utf-8");
                BufferedReader br = new BufferedReader(isr);
                String lineTxt = br.readLine();
                char[] ar = lineTxt.toCharArray();
                for (char c : ar) {
                    input_list.add((float) Integer.parseInt(String.valueOf(c)));
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

        // init the frame
        ArrayList<Float> track1 = new ArrayList<>();
        String filename = "src/INPUT.txt";
        ArrayList<Float> frame_data = readTxt(filename);

        // init the carrier
        SinWave wave = new SinWave(0, Config.PHY_CARRIER_FREQ, Config.PHY_TX_SAMPLING_RATE);
        ArrayList<Float> carrier = wave.sample(Config.PHY_TX_SAMPLING_RATE);

        // add preamble and frame, each frame has 100 bits
        for(int i=0; i<100; ++i){
            // add zero buffer1
            for (int j = 0; j < 100; j++){
                track1.add(0.0f);
            }
            // add preamble
            track1.addAll(Arrays.asList(Config.preamble));

            // modulation
            List<Float> frame = frame_data.subList(i*100, i*100+100);
            float[] frame_wave = new float[48*100];

            for(int j=0; j<100-1; ++j){
                for(int k=0; k<48; ++k){
                    frame_wave[j*48+k] = carrier.get(j*48+k) * (frame.get(j+1)*2-1); //  baud rate 48/48000 = 1000bps
                }
            }
            // add frame to track
            for(int j=0; j<frame_wave.length; ++j)
                track1.add(frame_wave[j]);

            // add zero buffer2
            for (int j = 0; j < 100; j++){
                track1.add(0.0f);
            }
        }
        System.out.println("Size of track:");
        System.out.println(track1.size());
        r.play(track1);


        r.start();
        try {
            Thread.sleep(5000);  // ms
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        r.stop();


        ArrayList<Float> recorded = r.getRecorded();

        // Initialization
        float power = 0;
        int start_index = 0;
        float[] syncPower_debug = new float[recorded.size()];
        float[] power_debug = new float[recorded.size()];
        float syncPower_localMax = 0;
        ArrayList<Float> syncFIFO_list = new ArrayList<Float>();

        Arrays.fill(syncPower_debug, 0);
        Arrays.fill(power_debug, 0);
        for(int i=0; i<440; ++i){
            syncFIFO_list.add(0.0f);
        }
        syncFIFO_list.addAll(recorded);

        ArrayList<Integer> start_indexes = new ArrayList<>();

        float sum;


        for(int i = 0; i < recorded.size(); i++){
            float current_sample = recorded.get(i);
            power =  power*(1.0f - 1.0f / 64.0f) + (float)Math.pow(current_sample, 2.0f) / 64.0f;
            power_debug[i] = power;

            var syncFIFO = syncFIFO_list.subList(i, i + 440);

            sum = 0;
            for (int j = 0; j < 440; j++){
                sum += syncFIFO.get(j) * Config.preamble[j];
            }

            syncPower_debug[i] = sum / 200.0f;

            if ((syncPower_debug[i] > power * 2.0f) && (syncPower_debug[i] > syncPower_localMax) && (syncPower_debug[i] > 0.002f)) {
                syncPower_localMax = syncPower_debug[i];
                start_index = i;
            }
            else if ((i - start_index > 200) && (start_index != 0)){
                if(!start_indexes.contains(start_index)){
                    start_indexes.add(start_index);
                }
//                start_index = 0;
                syncPower_localMax = 0.0f;
            }
        }

        System.out.println();
        System.out.println("Start indexes:");
        for(int id : start_indexes){
            System.out.println(id);
        }


//
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
