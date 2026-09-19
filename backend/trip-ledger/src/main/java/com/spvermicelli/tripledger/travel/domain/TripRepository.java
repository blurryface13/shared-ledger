package com.spvermicelli.tripledger.travel.domain;
import java.util.List;
public interface TripRepository {
    List<Trip> list(long userId);
    Trip find(long userId,long id);
    Trip create(long userId,Trip trip);
    Trip update(long userId,Trip trip);
    long storePlan(long userId,long tripId,long version,String payload);
    String getPlan(long userId,long tripId,long planId,long version);
    void applied(long planId);
}
