
package com.codeabovelab.dm.agent.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.DomainSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
class Backend implements InitializingBean, DisposableBean {

    private Bootstrap bootstrap;
    private EpollEventLoopGroup group;

    @Override
    public void destroy() throws Exception {
        group.shutdownGracefully();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.bootstrap = new Bootstrap();
        this.group = new EpollEventLoopGroup();
        bootstrap.group(group)
          .channel(EpollDomainSocketChannel.class)
          .handler(new ChannelInitializer<DomainSocketChannel>() {
                       @Override
                       protected void initChannel(DomainSocketChannel channel) throws Exception {
                           channel.pipeline().addLast(new HttpClientCodec());
                       }
                   }
          );
    }

    ChannelFuture connect() {
        return bootstrap.connect(new DomainSocketAddress("/var/run/docker.sock"));
    }
}
