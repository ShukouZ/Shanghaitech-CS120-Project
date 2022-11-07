import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SW_Receiver {
    // window size
    // last ACK received
    private int LAR;
    private final ArrayList<ArrayList<Integer>> frame_list;

    private final AudioHw audioHw;

    SW_Receiver(AudioHw _audioHW){
        audioHw = _audioHW;
        frame_list = new ArrayList<>();
    }

    public void storeFrame(List<Integer> frame_data, int id){
        sendACK();
        if (id == frame_list.size()){
            ArrayList<Integer> new_frame_data = new ArrayList<>(frame_data.size());
            new_frame_data.addAll(frame_data);
            frame_list.add(new_frame_data);
        }
    }

    public void sendACK(){
        int id = frame_list.size() - 1;
        float[] track = SW_Sender.frameToTrack(null, id, true);
        audioHw.PHYSend(track);
        System.out.println("Send ACK: " + id);
    }

    public void writeFile(){
        StringBuilder output = new StringBuilder();
        for(ArrayList<Integer> frame: frame_list){
            for (int datum: frame){
                output.append(datum);
            }
        }
        byte[] bytes = new byte[output.length() / 8];
        for (int i = 0; i < output.length() / 8; i++) {
            // System.out.print(bitStr+"|");
            bytes[i] = Util.BitToByte(output.substring(i*8,(i+1)*8));
        }
        Util.writeFileByBytes(bytes, "src/OUTPUT.bin");
    }

}
