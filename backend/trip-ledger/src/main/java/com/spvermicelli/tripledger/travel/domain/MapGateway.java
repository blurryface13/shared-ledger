package com.spvermicelli.tripledger.travel.domain;
import java.util.List;
public interface MapGateway {
    record Place(String id,String name,String address,double longitude,double latitude) {}
    record Leg(List<List<Double>> coordinates,long distanceMeters,long durationSeconds) {}
    List<Place> search(String city,String keyword);
    Leg walk(Trip.Activity from,Trip.Activity to);
}
