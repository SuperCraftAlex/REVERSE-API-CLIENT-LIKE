package at.alex_s168.reverse.client;

import at.alex_s168.reverse.api.universal.network.packet.client.*;

public class Main {

    public static void main(String[] args) {
        RClient c = new RClient("127.0.0.1");
        try {
            c.init();
            c.start();

            Runtime.getRuntime().addShutdownHook(new Thread(c::stop));

            c.sendPacket(new RC00PacketSessionCreate(0,"","","",""));
        } catch (Exception e) {
            c.stop();
            throw new RuntimeException(e);
        }
    }

}
