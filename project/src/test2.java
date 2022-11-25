import java.net.InetAddress;
import java.util.Scanner;

import jpcap.*;
import jpcap.packet.EthernetPacket;
import jpcap.packet.ICMPPacket;
import jpcap.packet.IPPacket;
import jpcap.NetworkInterface;

class SendICMP
{
    public static void main(String[] args) throws java.io.IOException{
        NetworkInterface[] devices = JpcapCaptor.getDeviceList();
        int k = -1;
        for (NetworkInterface n : devices) {
            k++;
            System.out.println("序号 " + k + "   " + n.name + "     |     " + n.description);
        }
        System.out.println("请输入你想要使用的网卡序号: ");
        Scanner sc = new Scanner(System.in);
        int index = sc.nextInt();
        JpcapSender sender=JpcapSender.openDevice(devices[index]);

        ICMPPacket p=new ICMPPacket();
        p.type=ICMPPacket.ICMP_TSTAMP;
        p.seq=1000;
        p.id=999;
        p.orig_timestamp=123;
        p.trans_timestamp=456;
        p.recv_timestamp=789;
//        p.setIPv4Parameter(0,false,false,false,0,false,false,false,0,1010101,100,IPPacket.IPPROTO_ICMP,
//                InetAddress.getByName("www.yahoo.com"),InetAddress.getByName("www.amazon.com"));
        p.setIPv4Parameter(0,false,false,false,0,false,false,false,0,1010101,100,IPPacket.IPPROTO_ICMP,
                InetAddress.getByName("10.19.74.124"),InetAddress.getByName("www.baidu.com"));
        p.data="data".getBytes();

        EthernetPacket ether=new EthernetPacket();
        ether.frametype=EthernetPacket.ETHERTYPE_IP;
        ether.src_mac=new byte[]{(byte)0,(byte)1,(byte)2,(byte)3,(byte)4,(byte)5};
        ether.dst_mac=new byte[]{(byte)0,(byte)6,(byte)7,(byte)8,(byte)9,(byte)10};
        p.datalink=ether;

        for(int i=0;i<1000;i++) {
            System.out.println("Sent ICMP packet.");
            sender.sendPacket(p);
        }
    }
}