import java.util.*;
import java.io.*;

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
            Thread.sleep(20000);  // ms
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        r.stop();


        //////////////////////////////////////////////////////////// RECEIVER ////////////////////////////////////////////////////////////
        ArrayList<Float> recorded = r.getRecorded();
        int crc_length = Config.CHECK_SIZE;
        int frame_size = Config.FRAME_SIZE;

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

            if ((syncPower_debug[i] > power * 2.0f) && (syncPower_debug[i] > syncPower_localMax) && (syncPower_debug[i] > 0.05f)) {
                syncPower_localMax = syncPower_debug[i];
                start_index = i;
            }
            else if ((i - start_index > 200) && (start_index != 0)){
                if(!start_indexes.contains(start_index)){
                    start_indexes.add(start_index);
                    // re init the local variables, prepare for next index decode
                    syncPower_localMax = 0.0f;
                    syncFIFO.clear();
                    for(int j=0; j<preamble_size; ++j){
                        syncFIFO.add(0.0f);
                    }
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
        ArrayList<Integer> decoded_data_foreach = new ArrayList<>();

        // decode & check crc
        for (int start_id : start_indexes){
            // Try index nearby the given id: [id-2, id+2]
            int[] potential_idx = new int[]{start_id-2, start_id-1, start_id+2, start_id+1, start_id};
            boolean correct = false;
            for(int id: potential_idx) {
                // find data signal
                data_signal = recorded.subList(id + 1, id + 1 + 48 * (frame_size + crc_length));

                // remove carrier
                for (int i = 0; i < 48 * (frame_size + crc_length); i++) {
                    data_signal_remove_carrier[i] = data_signal.get(i) * carrier.get(i);
                }

                // decode
                decoded_data_foreach.clear();
                for (int i = 0; i < frame_size + crc_length; i++) {
                    sum = 0.0f;
                    for (int j = 10 + i * 48; j < 30 + i * 48; j++) {
                        sum += data_signal_remove_carrier[j];
                    }

                    if (sum > 0.0f) {
                        decoded_data_foreach.add(1);
                    } else {
                        decoded_data_foreach.add(0);
                    }
                }

                // check crc code
                List<Integer> transmitted_crc = decoded_data_foreach.subList(frame_size, frame_size + crc_length);
                List<Integer> calculated_crc = CRC8.get_crc8(decoded_data_foreach.subList(0, frame_size));
                if (transmitted_crc.equals(calculated_crc)) {
                    correct = true;
                    break;
                }

            }
            if(!correct){
                System.out.println("CRC check fails at idx = " + start_id);
            }

            decoded_data.addAll(decoded_data_foreach.subList(0, frame_size));

            if (start_id == start_indexes.get(1)){
                try {
                    FileWriter writer = new FileWriter("src/2.txt");
                    for (Integer decoded_datum : decoded_data_foreach) {
                        writer.write(String.valueOf(decoded_datum));
                    }
                    writer.close();
                }catch (Exception e){
                    System.out.println("Cannot read file.");
                }
            }

        }

        System.out.println(decoded_data.size());

        try {
            FileWriter writer = new FileWriter("src/OUTPUT.txt");
            for (Integer decoded_datum : decoded_data) {
                writer.write(String.valueOf(decoded_datum));
            }
            writer.close();
        }catch (Exception e){
            System.out.println("Cannot read file.");
        }
    }
}
