import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;

enum MACState{
    RX, TX, FRAME_DETECTION
}

class PHY_TX extends Thread {
    private AudioHw audioHw;
    private ArrayList<Float> Tx_buffer;
    public PHY_TX(AudioHw hw, ArrayList<Float> tx_buffer){
        audioHw = hw;
        Tx_buffer = tx_buffer;
    }

    @Override
    public void run(){
        while (true){
            if (!Tx_buffer.isEmpty()){
                synchronized (Tx_buffer){
                    System.out.println(Tx_buffer.size());
                    audioHw.PHYSend(Tx_buffer);
                    Tx_buffer.clear();
                    break;
                }
            }
        }
    }
}

class PHY_RX extends Thread {
    private AudioHw audioHw;
    private  ArrayList<Float> Rx_buffer;
    public PHY_RX(AudioHw hw, ArrayList<Float> buffer){
        audioHw = hw;
        Rx_buffer = buffer;
    }
    @Override
    public void run(){
        while (true){


            break;
        }
    }
}

class decoder extends Thread {
    @Override
    public void run(){
        while (true){


            break;
        }
    }
}

class MAC extends Thread {
    private AudioHw audioHw;
    private ArrayList<Float> Rx_buffer;
    private ArrayList<Float> Tx_buffer;
    private ArrayList<Float> received_FIFO;

    private MACState state;

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

    public MAC(ArrayList<Float> rx_buffer, ArrayList<Float> tx_buffer, AudioHw hw){
        audioHw = hw;
        state = MACState.FRAME_DETECTION;
        Rx_buffer = rx_buffer;
        Tx_buffer = tx_buffer;
    }

    @Override
    public void run(){
        // init the frame
        ArrayList<Float> track1 = new ArrayList<>();
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
        audioHw.PHYSend(track1);
    }
}


public class test {
    public static void main(final String[] args) {
        AudioHw audioHw = new AudioHw();
        audioHw.init();
        ArrayList<Float> Tx_buffer = new ArrayList<>();
        ArrayList<Float> Rx_buffer = new ArrayList<>();

        MAC macThread = new MAC(Rx_buffer, Tx_buffer, audioHw);
        PHY_RX RxThread = new PHY_RX(audioHw, Rx_buffer);
        PHY_TX TxThread = new PHY_TX(audioHw, Tx_buffer);

        audioHw.start();
        macThread.start();

        try {
            Thread.sleep(15000);  // ms
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        audioHw.stop();
    }
}
