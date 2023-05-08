package at.alex_s168.reverse.client;

import at.alex_s168.reverse.api.universal.network.packet.RPacket;
import io.netty.channel.ChannelHandlerContext;

public interface PacketProcessor {

    void process(RPacket p, ChannelHandlerContext ctx);

}
