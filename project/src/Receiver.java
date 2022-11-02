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
        int data_size = (Config.FRAME_SIZE+Config.CHECK_SIZE+Config.ID_SIZE);
        float[] data_signal_remove_carrier = new float[Config.SAMPLE_PER_BIT * data_size];
        ArrayList<Integer> decoded_data = new ArrayList<>(10000);
        ArrayList<Integer> decoded_data_foreach = new ArrayList<>();
        float sum;

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 20000) {
            try {
                data_signal = audioHw.getFrame(frame_decoded_num);

                // remove carrier
                for (int i = 0; i < Config.SAMPLE_PER_BIT * data_size; i++) {
                    data_signal_remove_carrier[i] = data_signal[i] * carrier.get(i);
                }

                // decode
                decoded_data_foreach.clear();
                for (int i = 0; i < data_size; i++) {
                    sum = 0.0f;
                    for (int j = i * Config.SAMPLE_PER_BIT; j < (i + 1) * Config.SAMPLE_PER_BIT; j++) {
                        sum += data_signal_remove_carrier[j];
                    }

                    if (sum > 0.0f) {
                        decoded_data_foreach.add(1);
                    } else {
                        decoded_data_foreach.add(0);
                    }
                }

                // check crc code
                List<Integer> transmitted_crc = decoded_data_foreach.subList(Config.FRAME_SIZE + Config.ID_SIZE, data_size);
                List<Integer> calculated_crc = CRC8.get_crc8(decoded_data_foreach.subList(0, Config.FRAME_SIZE + Config.ID_SIZE));

                int id = 0;

                for (int n = 0; n < Config.ID_SIZE; n++)
                {
                    id += decoded_data_foreach.subList(0, Config.ID_SIZE).get(n) << n;
                }

                if(!transmitted_crc.equals(calculated_crc)){
                    System.out.println("CRC check fails at idx = " + id);
                    boolean correct = false;
                    int offset;
                    for(offset = 1; offset < 8; offset++)
                    {
                        // remove carrier
                        for (int i = 0; i < Config.SAMPLE_PER_BIT * data_size; i++) {
                            data_signal_remove_carrier[i] = data_signal[i] * carrier.get(i + offset);
                        }

                        // decode
                        decoded_data_foreach.clear();
                        for (int i = 0; i < data_size; i++) {
                            sum = 0.0f;
                            for (int j = i * Config.SAMPLE_PER_BIT; j < (i + 1) * Config.SAMPLE_PER_BIT; j++) {
                                sum += data_signal_remove_carrier[j];
                            }

                            if (sum > 0.0f) {
                                decoded_data_foreach.add(1);
                            } else {
                                decoded_data_foreach.add(0);
                            }
                        }

                        // check crc code
                        transmitted_crc = decoded_data_foreach.subList(Config.FRAME_SIZE + Config.ID_SIZE, data_size);
                        calculated_crc = CRC8.get_crc8(decoded_data_foreach.subList(0, Config.FRAME_SIZE + Config.ID_SIZE));


                        if (transmitted_crc.equals(calculated_crc)) {
                            correct = true;
                            id = 0;
                            for (int n = 0; n < Config.ID_SIZE; n++)
                            {
                                id += decoded_data_foreach.subList(0, Config.ID_SIZE).get(n) << n;
                            }
                            break;
                        }
                    }
                    if(correct){
                        System.out.println("Fixed idx = " + id + " with offset = " + offset);
                    }
                    else{
                        System.out.println("Couldn't fix idx = " + id);
                    }
                }
//                else
//                {
//                    System.out.println("CRC correct at idx = " + frame_decoded_num);
//                }



                System.out.println("Received: " + id);
                decoded_data.addAll(decoded_data_foreach.subList(Config.ID_SIZE, Config.FRAME_SIZE + Config.ID_SIZE));
                frame_decoded_num++;

            }catch (ArrayIndexOutOfBoundsException e){

            }
        }

        audioHw.stop();

        System.out.println(decoded_data.size() + " bits received.");

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
