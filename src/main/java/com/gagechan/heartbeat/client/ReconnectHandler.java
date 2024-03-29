package com.gagechan.heartbeat.client;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoop;

import java.util.concurrent.TimeUnit;

@ChannelHandler.Sharable
public class ReconnectHandler extends ChannelInboundHandlerAdapter {
 
    private int retries = 0;
    private RetryPolicy retryPolicy;
 
    private HeartBeatClient heartBeatClient;
 
    public ReconnectHandler(HeartBeatClient heartBeatClient) {
        this.heartBeatClient = heartBeatClient;
    }
 
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Successfully established a connection to the server.");
        retries = 0;
        ctx.fireChannelActive();
    }
 
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (retries == 0) {
            System.err.println("Lost the TCP connection with the server.");
            ctx.close();
        }
 
        boolean allowRetry = getRetryPolicy().allowRetry(retries);
        if (allowRetry) {
 
            long sleepTimeMs = getRetryPolicy().getSleepTimeMs(retries);
 
            System.out.println(String.format("Try to reconnect to the server after %dms. Retry count: %d.", sleepTimeMs, ++retries));
 
            final EventLoop eventLoop = ctx.channel().eventLoop();
            eventLoop.schedule(() -> {
                System.out.println("Reconnecting ...");
                heartBeatClient.connect();
            }, sleepTimeMs, TimeUnit.MILLISECONDS);
        }
        ctx.fireChannelInactive();
    }
 
 
    private RetryPolicy getRetryPolicy() {
        if (this.retryPolicy == null) {
            this.retryPolicy = heartBeatClient.getRetryPolicy();
        }
        return this.retryPolicy;
    }
}