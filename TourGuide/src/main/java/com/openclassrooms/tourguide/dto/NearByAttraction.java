package com.openclassrooms.tourguide.dto;

public class NearByAttraction {

    private String attractionName;

    private final double attractionLatitude;

    private final double attractionLongitude;

    private double attractionDistanceFromUserLocation;

    private int attractionRewardPoints;

    public NearByAttraction(String attractionName, double attractionLatitude, double attractionLongitude, double attractionDistanceFromUserLocation, int attractionRewardPoints) {
        this.attractionName = attractionName;
        this.attractionLatitude = attractionLatitude;
        this.attractionLongitude = attractionLongitude;
        this.attractionDistanceFromUserLocation = attractionDistanceFromUserLocation;
        this.attractionRewardPoints = attractionRewardPoints;
    }

    public double getAttractionDistanceFromUserLocation() {
        return attractionDistanceFromUserLocation;
    }

    public String getAttractionName() {
        return attractionName;
    }

    public double getAttractionLatitude() {
        return attractionLatitude;
    }

    public double getAttractionLongitude() {
        return attractionLongitude;
    }

    public int getAttractionRewardPoints() {
        return attractionRewardPoints;
    }
}
