import java.util.ArrayList;

public class CK2_1 {
    public static void main(final String[] args) {
        final AudioHw r = new AudioHw();
        r.init();


        SinWave_double wave1 = new SinWave_double(0, 1000, Config.PHY_TX_SAMPLING_RATE);
        SinWave_double wave2 = new SinWave_double(0, 10000, Config.PHY_TX_SAMPLING_RATE);

        ArrayList<Double> sample1 = wave1.sample(10 * Config.PHY_TX_SAMPLING_RATE);
        ArrayList<Double> sample2 = wave2.sample(10 * Config.PHY_TX_SAMPLING_RATE);


        ArrayList<Float> track = new ArrayList<>(10 * Config.PHY_TX_SAMPLING_RATE);

        for (int i = 0; i < 10 * Config.PHY_TX_SAMPLING_RATE; i++){
            track.add((float)(sample1.get(i) + sample2.get(i)));
        }

//        ArrayList<Float> track = new ArrayList<>(Arrays.asList(Config.preamble));


        r.play(track);

        r.start();
        try {
            Thread.sleep(10000);  // ms
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        r.stop();
    }
}
