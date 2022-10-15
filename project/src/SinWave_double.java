import java.util.ArrayList;

public class SinWave_double {

    private double phase;
    private double freq;  // Hz
    private double sampleRate;
    private final double dphase;

    SinWave_double(double _phase, double _freq, double _sampleRate){
        phase = _phase;
        freq = _freq;
        sampleRate = _sampleRate;
        dphase = (2 * Math.PI * freq) / sampleRate;
    }


    public ArrayList<Double> sample(int num){
        ArrayList<Double> samples = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            phase = phase + dphase;
            samples.add((Math.sin(phase)));
        }
        return samples;
    }
}
