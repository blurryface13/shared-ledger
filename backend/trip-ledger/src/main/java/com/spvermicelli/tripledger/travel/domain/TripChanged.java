package com.spvermicelli.tripledger.travel.domain;
/** A wake-up hint only; the durable change table is the source for catch-up. */
public record TripChanged(long tripId) {}
