import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class Receiver {
    public static void main(final String[] args) {
        AudioHw audioHw = new AudioHw();
        audioHw.init();
        audioHw.start();

        // init the carrier
        SinWave wave = new SinWave(0, Config.PHY_CARRIER_FREQ, Config.PHY_TX_SAMPLING_RATE);
        ArrayList<Float> carrier = wave.sample(Config.PHY_TX_SAMPLING_RATE);

        int frame_decoded_num = 0;
        float[] data_signal;
        int data_size = (Config.FRAME_SIZE+Config.CHECK_SIZE);
        float[] data_signal_remove_carrier = new float[48 * data_size];
        ArrayList<Integer> decoded_data = new ArrayList<>(10000);
        ArrayList<Integer> decoded_data_foreach = new ArrayList<>();
        float sum;
        boolean correct = false;


        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 18000) {
            try {
                data_signal = audioHw.getFrame(frame_decoded_num);

                // remove carrier
                for (int i = 0; i < 48 * (Config.FRAME_SIZE+Config.CHECK_SIZE); i++) {
                    data_signal_remove_carrier[i] = data_signal[i] * carrier.get(i);
                }

                // decode
                decoded_data_foreach.clear();
                for (int i = 0; i < data_size; i++) {
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
                List<Integer> transmitted_crc = decoded_data_foreach.subList(Config.FRAME_SIZE, data_size);
                List<Integer> calculated_crc = CRC8.get_crc8(decoded_data_foreach.subList(0, Config.FRAME_SIZE));
                if (transmitted_crc.equals(calculated_crc)) {
                    correct = true;
                }


                if(!correct){
                    System.out.println("CRC check fails at idx = " + frame_decoded_num);
                }

                decoded_data.addAll(decoded_data_foreach.subList(0, Config.FRAME_SIZE));

            }catch (ArrayIndexOutOfBoundsException e){

            }
        }

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
