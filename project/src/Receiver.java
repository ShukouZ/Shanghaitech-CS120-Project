import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Receiver {
    public static void main(final String[] args) {
        // init the audio
        final AudioHw r = new AudioHw();
        r.init();

        // init the carrier
        SinWave wave = new SinWave(0, Config.PHY_CARRIER_FREQ, Config.PHY_TX_SAMPLING_RATE);
        ArrayList<Float> carrier = wave.sample(Config.PHY_TX_SAMPLING_RATE);

        r.start();
        try {
            Thread.sleep(15000);  // ms
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
        for(int i=0; i<480; ++i){
            syncFIFO_list.add(0.0f);
        }
        syncFIFO_list.addAll(recorded);

        ArrayList<Integer> start_indexes = new ArrayList<>();

        float sum; // temp


        for(int i = 0; i < recorded.size(); i++){
            float current_sample = recorded.get(i);
            power =  power*(1.0f - 1.0f / 64.0f) + (float)Math.pow(current_sample, 2.0f) / 64.0f;
            power_debug[i] = power;

            var syncFIFO = syncFIFO_list.subList(i, i + 480);

            sum = 0;
            for (int j = 0; j < 480; j++){
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
//                start_index = 0;
                syncPower_localMax = 0.0f;
            }
        }

        System.out.println();
        System.out.println("Start indexes:");
        for(int id : start_indexes){
            System.out.println(id);
        }

        List<Float> data_signal;
        float[] data_signal_remove_carrier = new float[48 * 100];
        ArrayList<Integer> decoded_data = new ArrayList<>(10000);

        // decode
        for (int id : start_indexes){
            // find data signal
            data_signal = recorded.subList(id + 1, id + 48 * 100 + 1);

            // remove carrier
            for (int i = 0; i < 48 * 100; i++){
                data_signal_remove_carrier[i] = data_signal.get(i) * carrier.get(i);
            }

            for (int i = 0; i < 100; i++){
                sum = 0;
                for (int j = 10 + i * 48; j < 30 + i * 48; j++){
                    sum += data_signal_remove_carrier[j];
                }

                if (sum > 0){
                    decoded_data.add(1);
                }
                else{
                    decoded_data.add(0);
                }
            }



        }

        System.out.println(decoded_data.size());

        try {
            FileWriter writer = new FileWriter("src/OUTPUT.txt");
            for (int i = 0; i < decoded_data.size(); i++) {
                writer.write(String.valueOf(decoded_data.get(i)));
            }
            writer.close();
        }catch (Exception e){
            System.out.println("Cannot read file.");
        }
    }
}
