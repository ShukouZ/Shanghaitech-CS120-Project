import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

public class crc32 {
    public static List<Integer> get_crc(List<Integer> frame) {
        CRC32 crc = new CRC32();
        byte[] data = Util.frameToBytes(frame);
        crc.update(data, 0, data.length);
        int fcs = (int) crc.getValue();

        String crc_code = Integer.toString((char)(fcs),2);
        char[] sum_char = crc_code.toCharArray();
        int zero_buffer_num = 32-sum_char.length;
        List<Integer> res = new ArrayList<>();
        for(int i=0; i<zero_buffer_num; ++i){
            res.add(0);
        }
        for(char c: sum_char){
            res.add(Integer.parseInt((String.valueOf(c))));
        }
        return res;
    }
    public static void main(String[] args){
        ArrayList<Integer> a = new ArrayList<>();
        a.add(0);
        a.add(0);
        a.add(0);
        a.add(0);

        a.add(0);
        a.add(0);
        a.add(1);
        a.add(0);

        a.add(0);
        a.add(0);
        a.add(0);
        a.add(0);

        a.add(0);
        a.add(1);
        a.add(0);
        a.add(1);

//        byte[] buffer = new byte[] {0x02, 0x05};
        List<Integer> crc32 = get_crc(a);
        System.out.println(crc32);
    }
}
