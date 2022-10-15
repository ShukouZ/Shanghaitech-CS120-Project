import java.util.ArrayList;

public class CK2_1 {
    public static void main(final String[] args) {
        final AudioHw r = new AudioHw();
        r.init();


        SinWave wave1 = new SinWave(0, 1000, Config.PHY_TX_SAMPLING_RATE);
        SinWave wave2 = new SinWave(0, 10000, Config.PHY_TX_SAMPLING_RATE);

        ArrayList<Float> sample1 = wave1.sample(10 * Config.PHY_TX_SAMPLING_RATE);
        ArrayList<Float> sample2 = wave2.sample(10 * Config.PHY_TX_SAMPLING_RATE);


        ArrayList<Float> track = new ArrayList<>(10 * Config.PHY_TX_SAMPLING_RATE);

        for (int i = 0; i < 10 * Config.PHY_TX_SAMPLING_RATE; i++){
            track.add((sample1.get(i) + sample2.get(i)));
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
