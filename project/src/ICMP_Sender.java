import jpcap.JpcapCaptor;
import jpcap.JpcapSender;
import jpcap.NetworkInterface;
import jpcap.packet.EthernetPacket;
import jpcap.packet.ICMPPacket;
import jpcap.packet.IPPacket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ICMP_Sender {
    private static ICMPPacket p = null;
    static JpcapSender sender = null;

    ICMP_Sender() throws IOException {
        NetworkInterface[] devices = JpcapCaptor.getDeviceList();
        int index = 5;
        sender=JpcapSender.openDevice(devices[index]);

        p=new ICMPPacket();
        p.type=ICMPPacket.ICMP_ECHO;
        p.seq=1000;
        p.id=999;
        p.orig_timestamp=0;
        p.trans_timestamp=0;
        p.recv_timestamp=0;
    }

    public void send(String src_IP, String dst_IP, byte[] data, int repeat_times) throws UnknownHostException {
        p.setIPv4Parameter(0,false,false,false,0,false,false,false,0,1010101,100, IPPacket.IPPROTO_ICMP,
                InetAddress.getByName(src_IP),InetAddress.getByName(dst_IP));
        p.data=data;

        EthernetPacket ether=new EthernetPacket();
        ether.frametype=EthernetPacket.ETHERTYPE_IP;
        ether.src_mac=new byte[]{(byte)0x24,(byte)0xee,(byte)0x9a,(byte)0x24,(byte)0x10,(byte)0xda};
        ether.dst_mac=new byte[]{(byte)0x0,(byte)0x0,(byte)0x5e,(byte)0,(byte)0x1,(byte)0x1};
        p.datalink=ether;

        if(repeat_times <= 0){
            repeat_times = 1;
        }
        for(int i=0; i<repeat_times; i++){
            sender.sendPacket(p);
        }
    }
}
