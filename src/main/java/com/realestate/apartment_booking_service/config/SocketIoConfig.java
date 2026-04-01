package com.realestate.apartment_booking_service.config;

import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "socketio.enabled", havingValue = "true", matchIfMissing = true)
public class SocketIoConfig {

    @Value("${socketio.host:127.0.0.1}")
    private String host;

    @Value("${socketio.port:9092}")
    private Integer port;

    @Bean(destroyMethod = "stop")
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration socketConfiguration = new com.corundumstudio.socketio.Configuration();
        socketConfiguration.setHostname(host);
        socketConfiguration.setPort(port);
        return new SocketIOServer(socketConfiguration);
    }

    @Bean
    public CommandLineRunner socketServerRunner(SocketIOServer socketIOServer) {
        return args -> socketIOServer.start();
    }
}
