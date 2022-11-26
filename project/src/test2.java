import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

import jpcap.*;
import jpcap.packet.EthernetPacket;
import jpcap.packet.ICMPPacket;
import jpcap.packet.IPPacket;
import jpcap.NetworkInterface;
import jpcap.packet.Packet;
class SendICMP
{
    public static void main(String[] args) throws java.io.IOException{
        NetworkInterface[] devices = JpcapCaptor.getDeviceList();
        int k = -1;
        for (NetworkInterface n : devices) {
            k++;
            System.out.println("序号 " + k + "   " + n.name + "     |     " + n.description);
        }
        /*--------第二步,选择网卡并打开网卡连接--------*/
        // 选择网卡序号;
        // 注意!每台设备连接网络的网卡不同,选择正确的网卡才能捕获到数据包;
        System.out.println("请输入你想要监听的网卡序号: ");
        Scanner sc = new Scanner(System.in);
        int index = sc.nextInt();

        JpcapCaptor jpcap = null;
        //第二步,监听选中的网卡;
        try {
            // 参数一:选择一个网卡，调用 JpcapCaptor.openDevice()连接，返回一个 JpcapCaptor类的对象 jpcap;
            // 参数二:限制每一次收到一个数据包，只提取该数据包中前1512个字节;
            // 参数三:设置为非混杂模式,才可以使用下面的捕获过滤器方法;
            // 参数四:指定超时的时间;

            jpcap = JpcapCaptor.openDevice(devices[index], 1512, true, 20);
            jpcap.setFilter("proto ICMP", true);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("抓取数据包时出现异常!!");
        }



        JpcapSender sender=JpcapSender.openDevice(devices[index]);

        ICMPPacket p=new ICMPPacket();
        p.type=ICMPPacket.ICMP_ECHO;
        p.seq=1000;
        p.id=999;
        p.orig_timestamp=123;
        p.trans_timestamp=456;
        p.recv_timestamp=789;
//        p.setIPv4Parameter(0,false,false,false,0,false,false,false,0,1010101,100,IPPacket.IPPROTO_ICMP,
//                InetAddress.getByName("www.yahoo.com"),InetAddress.getByName("www.amazon.com"));
        p.setIPv4Parameter(0,false,false,false,0,false,false,false,0,1010101,100,IPPacket.IPPROTO_ICMP,
                InetAddress.getByName("10.20.160.157"),InetAddress.getByName("182.61.200.6"));


        EthernetPacket ether=new EthernetPacket();
        ether.frametype=EthernetPacket.ETHERTYPE_IP;
        ether.src_mac=new byte[]{(byte)0xd8,(byte)0x9c,(byte)0x67,(byte)0x7b,(byte)0xf1,(byte)0xdb};
        ether.dst_mac=new byte[]{(byte)0x00,(byte)0x00,(byte)0x5e,(byte)0x00,(byte)0x01,(byte)0x01};
        p.datalink=ether;

        for(int i=0;i<65536;i++) {
            System.out.println("i: " + (i & 0xFFFF));
            p.data = new byte[]{(byte)(i & 0xFF),(byte)((i & 0xFF00) >> 8)};

            System.out.println("Sent ICMP packet.");
            sender.sendPacket(p);

            IPPacket packet = null;

            while (true){
                try {
                    assert jpcap != null;
                    packet = (IPPacket) jpcap.getPacket();
                    if (packet!=null)
                    {
                        System.out.println(packet);
                        if (Objects.equals(packet.src_ip.getHostAddress(), "182.61.200.6")){
                            break;
                        }
                    }


                }catch (Exception e){

                }
            }

//            System.out.println(packet);
            byte a = packet.data[0];
            byte b = packet.data[1];

//            System.out.println("a: " + packet.data[0]);
//            System.out.println("b: " + packet.data[1]);
//            System.out.println("aa: " + (a > 0 ? (int)a : -(int)a));
//            System.out.println("bb: " + ((b > 0 ? (int)b : -(int)b)));
            int I = (a >= 0 ? (int)a : -(int)a) + ((b >= 0 ? (int)b : 256 + (int)b) << 8);
            System.out.println("I: " + I);
            System.out.println();
//            try {
//                Thread.sleep(500);  // ms
//            } catch (final InterruptedException e) {
//                e.printStackTrace();
//            }
//            System.out.println((Arrays.toString(packet.data)));
//            System.out.println((System.currentTimeMillis() & 0xFFFF) - t);
        }
//
//        try {
//            Thread.sleep(30000);  // ms
//        } catch (final InterruptedException e) {
//            e.printStackTrace();
//        }
    }
}