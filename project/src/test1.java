
import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;

import java.util.Arrays;

public class test1 {
    public static void main(String[] args) {
        /*-------第一步,显示网络设备列表-------- */
        // 获取网络接口列表，返回你所有的网络设备数组,一般就是网卡;
        NetworkInterface[] devices = JpcapCaptor.getDeviceList();
        int k = -1;
        // 显示所有网络设备的名称和描述信息;
        // 要注意的是,显示出来的网络设备在不同网络环境下是不同的,可以在控制台使用 ipconfig /all命令查看;
        System.out.println("?"+ Arrays.toString(devices));
        for (NetworkInterface n : devices) {
            k++;
            System.out.println("序号 " + k + "   " + n.name + "     |     " + n.description);
            System.out.println("------------------------------------------------");
        }
    }
}
