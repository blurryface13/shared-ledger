package com.spvermicelli.tripledger.travel.domain;
import java.util.List;
public interface MapGateway {
    record Place(String id,String name,String address,double longitude,double latitude,String coordinateSystem) {
        public Place(String id,String name,String address,double longitude,double latitude){this(id,name,address,longitude,latitude,"WGS84");}
    }
    record Center(double longitude,double latitude,String coordinateSystem) {
        public Center(double longitude,double latitude){this(longitude,latitude,"WGS84");}
    }
    record Leg(List<List<Double>> coordinates,long distanceMeters,long durationSeconds) {}
    Center center(String city);
    List<Place> search(String city,String keyword);
    List<Place> nearby(double longitude,double latitude,String category);
    Leg walk(Trip.Activity from,Trip.Activity to);
}
