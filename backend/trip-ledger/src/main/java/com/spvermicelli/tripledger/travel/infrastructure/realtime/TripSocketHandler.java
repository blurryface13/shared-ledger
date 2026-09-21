package com.spvermicelli.tripledger.travel.infrastructure.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spvermicelli.tripledger.identity.infrastructure.security.JwtTokenService;
import com.spvermicelli.tripledger.identity.domain.user.repository.UserRepository;
import com.spvermicelli.tripledger.travel.domain.TripRepository;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.scheduling.annotation.Scheduled;

@Component
public class TripSocketHandler extends TextWebSocketHandler {
    private final JwtTokenService tokens;
    private final UserRepository users;
    private final TripRepository trips;
    private final ObjectMapper json=new ObjectMapper();
    private final ConcurrentHashMap<String,Connection> connections=new ConcurrentHashMap<>();
    private static final class Connection {
        final WebSocketSession session;
        final Instant opened=Instant.now();
        volatile String token;
        volatile long user,trip;
        Connection(WebSocketSession s){session=new ConcurrentWebSocketSessionDecorator(s,2000,8192);}
    }
    public TripSocketHandler(JwtTokenService tokens,UserRepository users,TripRepository trips){this.tokens=tokens;this.users=users;this.trips=trips;}
    @Override public synchronized void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if(connections.size()>=256){session.close(new CloseStatus(1013,"Capacity reached"));return;}
        session.setTextMessageSizeLimit(4096);
        connections.put(session.getId(),new Connection(session));
    }
    @Override protected void handleTextMessage(WebSocketSession session,TextMessage message) {
        Connection c=connections.get(session.getId());if(c==null)return;
        try {
            if(c.token!=null)throw new IllegalArgumentException();
            var data=json.readTree(message.getPayload());
            String token=data.path("token").asText();long trip=data.path("tripId").asLong();
            if(token.length()>3500||trip<=0)throw new IllegalArgumentException();
            var login=tokens.parseToken(token);
            if(!"ACCESS".equals(login.getTokenType()))throw new IllegalArgumentException();
            long user=login.getUserId();
            if(users.findById(user).filter(u->u.isActive()).isEmpty())throw new IllegalArgumentException();
            trips.find(user,trip);
            synchronized(this){
                if(connections.values().stream().filter(x->x.token!=null&&x.user==user).count()>=4)throw new IllegalArgumentException();
                c.user=user;c.trip=trip;c.token=token;
            }
            c.session.sendMessage(new TextMessage("{\"type\":\"ready\"}"));
        }catch(Exception ex){close(c,CloseStatus.POLICY_VIOLATION);}
    }
    private boolean authorized(Connection c){
        try{return tokens.parseToken(c.token).getUserId()==c.user && users.findById(c.user).filter(u->u.isActive()).isPresent() && trips.find(c.user,c.trip)!=null;}
        catch(Exception ex){return false;}
    }
    public void changed(long trip){
        for(Connection c:connections.values())if(c.token!=null&&c.trip==trip){
            if(!authorized(c)){close(c,CloseStatus.POLICY_VIOLATION);continue;}
            try{c.session.sendMessage(new TextMessage("{\"type\":\"changed\"}"));}
            catch(Exception ex){close(c,CloseStatus.SERVER_ERROR);}
        }
    }
    @Scheduled(fixedDelay=1000)
    public void expireUnauthenticated(){for(Connection c:connections.values())if(c.token==null&&c.opened.plusSeconds(5).isBefore(Instant.now()))close(c,CloseStatus.POLICY_VIOLATION);}
    @Scheduled(fixedDelay=30000)
    public void heartbeat(){for(Connection c:connections.values())if(c.token!=null){
        if(!authorized(c)){close(c,CloseStatus.POLICY_VIOLATION);continue;}
        try{c.session.sendMessage(new TextMessage("{\"type\":\"heartbeat\"}"));}catch(Exception ex){close(c,CloseStatus.SERVER_ERROR);}
    }}
    private void close(Connection c,CloseStatus reason){connections.remove(c.session.getId(),c);try{c.session.close(reason);}catch(Exception ignored){}}
    @Override public void afterConnectionClosed(WebSocketSession session,CloseStatus status){connections.remove(session.getId());}
    @Override public void handleTransportError(WebSocketSession session,Throwable error){Connection c=connections.get(session.getId());if(c!=null)close(c,CloseStatus.SERVER_ERROR);}
}
