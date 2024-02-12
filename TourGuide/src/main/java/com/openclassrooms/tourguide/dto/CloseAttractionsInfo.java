package com.openclassrooms.tourguide.dto;

import java.util.List;

public class CloseAttractionsInfo {

    private final double userLocationLatitude;

    private final double userLocationLongitude;

    private List<NearByAttraction> nearByAttractions;

    public double getUserLocationLatitude() {
        return userLocationLatitude;
    }

    public double getUserLocationLongitude() {
        return userLocationLongitude;
    }

    public List<NearByAttraction> getNearByAttractions() {
        return nearByAttractions;
    }

    public CloseAttractionsInfo(double userLocationLatitude, double userLocationLongitude, List<NearByAttraction> nearByAttractions) {
        this.userLocationLatitude = userLocationLatitude;
        this.userLocationLongitude = userLocationLongitude;
        this.nearByAttractions = nearByAttractions;
    }
}
