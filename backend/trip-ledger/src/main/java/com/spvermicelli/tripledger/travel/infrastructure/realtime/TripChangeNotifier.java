package com.spvermicelli.tripledger.travel.infrastructure.realtime;
import com.spvermicelli.tripledger.travel.domain.TripChanged;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import java.util.concurrent.*;
import jakarta.annotation.PreDestroy;
@Component
public class TripChangeNotifier {
    private final TripSocketHandler sockets;
    // Hints may be dropped under overload: durable HTTP catch-up remains authoritative.
    private final ThreadPoolExecutor executor=new ThreadPoolExecutor(1,2,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(128),new ThreadPoolExecutor.DiscardPolicy());
    public TripChangeNotifier(TripSocketHandler sockets){this.sockets=sockets;}
    @TransactionalEventListener(phase=org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
    public void committed(TripChanged event){executor.execute(()->sockets.changed(event.tripId()));}
    @PreDestroy public void shutdown(){executor.shutdownNow();}
}
