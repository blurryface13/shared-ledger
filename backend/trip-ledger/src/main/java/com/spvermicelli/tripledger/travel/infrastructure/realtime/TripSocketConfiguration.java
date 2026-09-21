package com.spvermicelli.tripledger.travel.infrastructure.realtime;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.config.annotation.*;
@Configuration
@EnableWebSocket
public class TripSocketConfiguration implements WebSocketConfigurer {
    private final TripSocketHandler handler;private final String origins;
    public TripSocketConfiguration(TripSocketHandler handler,@Value("${app.travel.websocket-origins:http://127.0.0.1:4178,http://localhost:4178}") String origins){this.handler=handler;this.origins=origins;}
    @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry){registry.addHandler(handler,"/ws/trips").setAllowedOrigins(origins.split(","));}
}
