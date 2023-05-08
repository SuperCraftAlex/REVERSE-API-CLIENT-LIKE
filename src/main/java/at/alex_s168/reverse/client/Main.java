package at.alex_s168.reverse.client;

import at.alex_s168.reverse.api.universal.network.packet.client.*;
import at.alex_s168.reverse.api.universal.network.packet.server.RS00PacketSessionResult;
import at.alex_s168.reverse.api.universal.network.packet.server.RS09PacketSessionEnd;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

public class Main {

    public static void main(String[] args) {
        RClient c = new RClient("127.0.0.1");
        try {
            c.init();
            c.start();
            AtomicReference<String> sid = new AtomicReference<>("");

            Runtime.getRuntime().addShutdownHook(new Thread(c::stop));

            c.sendPacket(new RC00PacketSessionCreate(0,"","","",""));

            c.addProcessor((packet, ctx) -> {if(packet instanceof RS00PacketSessionResult) {sid.set(((RS00PacketSessionResult) packet).sid);}});
            c.addProcessor((packet, ctx) -> {if(packet instanceof RS09PacketSessionEnd) {c.stop();System.exit(0);}});

            Timer ticker = new Timer(); ticker.schedule(new TimerTask() { @Override public void run() { c.sendPacket(new RC04PacketKeepAlive(sid.get(), 0,"","")); }}, 0, 3000);
        } catch (Exception e) {
            c.stop();
            throw new RuntimeException(e);
        }
    }

}
