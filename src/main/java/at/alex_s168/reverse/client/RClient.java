package at.alex_s168.reverse.client;

import at.alex_s168.reverse.api.universal.DEF;
import at.alex_s168.reverse.api.universal.network.RPacketBuffer;
import at.alex_s168.reverse.api.universal.network.packet.RPacket;
import at.alex_s168.reverse.api.universal.network.packet.client.RC00PacketSessionCreate;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.buffer.UnpooledDirectByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class RClient {
    private final String hostIP;
    private EventLoopGroup group;
    private Bootstrap bootstrap;
    private ChannelFuture future;
    private Channel channel;
    public final List<PacketProcessor> processors;

    public RClient(String ip) {
        this.hostIP = ip;
        processors = new ArrayList<>();
    }

    public void init() throws Exception {

        group = new NioEventLoopGroup();

        bootstrap = new Bootstrap();
        bootstrap.group(group) // Set EventLoopGroup to handle all eventsf for client.
                .channel(NioSocketChannel.class)// Use NIO to accept new connections.
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast("decoder", new ByteArrayDecoder());
                        p.addLast("encoder", new ByteArrayEncoder());

                        // This is our custom client handler which will have logic for chat.
                        p.addLast("handler", new ClientHandler(RClient.this));

                    }
                });
    }

    public void start() throws InterruptedException {
        future = bootstrap.connect(hostIP, DEF.PORT+1).sync();

        channel = future.sync().channel();
    }

    public void addProcessor(PacketProcessor processor) {
        processors.add(processor);
    }

    public void sendPacket(RPacket packet) {
        ByteBuf hbuf = Unpooled.buffer(999999);
        RPacketBuffer buf = new RPacketBuffer(hbuf);

        buf.writeString(packet.getClass().getSimpleName());

        try {
            packet.writePacketData(buf);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        channel.writeAndFlush(buf.array());
    }

    public void stop() {
        group.shutdownGracefully();
    }

}
class ClientHandler extends SimpleChannelInboundHandler<byte[]> {

    public RClient client;

    public ClientHandler(RClient client) {
        this.client = client;
    }

    @Override
    @SuppressWarnings({"unchecked","deprecation"})
    protected void channelRead0(ChannelHandlerContext ctx, byte[] in) throws Exception {
        ByteBuf hbuf = Unpooled.wrappedBuffer(in);
        RPacketBuffer buf = new RPacketBuffer(hbuf);

        String packetType = buf.readString(50);
        if(packetType.equals("")) { return; }

        Class<? extends RPacket> packetClass = (Class<? extends RPacket>) Class.forName("at.alex_s168.reverse.api.universal.network.packet.server."+packetType);
        RPacket packet = packetClass.newInstance();
        packet.readPacketData(buf);

        client.processors.forEach((p) -> p.process(packet, ctx));
        System.out.println("Received packet "+packet.getClass().getSimpleName()+" from "+ctx.channel().remoteAddress()+"!");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("Closing connection - "+cause.getMessage());
        ctx.close();
        System.exit(0);
    }

}