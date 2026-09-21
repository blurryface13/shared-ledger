package com.spvermicelli.tripledger.travel.infrastructure.realtime;
import com.spvermicelli.tripledger.identity.infrastructure.security.JwtTokenService;
import com.spvermicelli.tripledger.identity.domain.user.repository.UserRepository;
import com.spvermicelli.tripledger.identity.domain.user.model.User;
import com.spvermicelli.tripledger.shared.common.context.LoginUser;
import com.spvermicelli.tripledger.travel.domain.TripRepository;
import org.junit.jupiter.api.*;
import org.springframework.web.socket.*;
import java.util.Optional;
import static org.mockito.Mockito.*;
class TripSocketHandlerTest {
    JwtTokenService tokens=mock(JwtTokenService.class);UserRepository users=mock(UserRepository.class);TripRepository trips=mock(TripRepository.class);
    TripSocketHandler handler=new TripSocketHandler(tokens,users,trips);WebSocketSession session=mock(WebSocketSession.class);
    @BeforeEach void setup()throws Exception{when(session.getId()).thenReturn("socket");when(session.isOpen()).thenReturn(true);handler.afterConnectionEstablished(session);}
    void auth(String type)throws Exception{
        when(tokens.parseToken("valid")).thenReturn(LoginUser.builder().userId(1L).tokenType(type).build());
        User user=mock(User.class);when(user.isActive()).thenReturn(true);when(users.findById(1L)).thenReturn(Optional.of(user));
        when(trips.find(1L,7L)).thenReturn(mock(com.spvermicelli.tripledger.travel.domain.Trip.class));
        handler.handleMessage(session,new TextMessage("{\"token\":\"valid\",\"tripId\":7}"));
    }
    @Test void noUnauthenticatedPush()throws Exception{handler.changed(7);verify(session,never()).sendMessage(any());}
    @Test void rejectsBindToken()throws Exception{auth("BIND_MOBILE");verify(session).close(CloseStatus.POLICY_VIOLATION);verify(session,never()).sendMessage(any());}
    @Test void authenticatesAndSendsOnlyHints()throws Exception{auth("ACCESS");handler.changed(7);verify(session).sendMessage(argThat(m->m.getPayload().equals("{\"type\":\"changed\"}")));}
    @Test void revocationClosesExistingConnection()throws Exception{auth("ACCESS");clearInvocations(session);when(trips.find(1L,7L)).thenThrow(new IllegalArgumentException());handler.changed(7);verify(session).close(CloseStatus.POLICY_VIOLATION);verify(session,never()).sendMessage(any());}
    @Test void expiredTokenClosesOnHeartbeat()throws Exception{auth("ACCESS");clearInvocations(session);when(tokens.parseToken("valid")).thenThrow(new IllegalArgumentException());handler.heartbeat();verify(session).close(CloseStatus.POLICY_VIOLATION);}
    @Test void cannotSwitchSubscriptionOnAuthenticatedConnection()throws Exception{auth("ACCESS");clearInvocations(session);handler.handleMessage(session,new TextMessage("{\"token\":\"valid\",\"tripId\":8}"));verify(session).close(CloseStatus.POLICY_VIOLATION);}
}
