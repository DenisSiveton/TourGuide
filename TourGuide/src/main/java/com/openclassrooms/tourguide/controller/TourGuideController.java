package com.openclassrooms.tourguide.controller;

import com.openclassrooms.tourguide.dto.CloseAttractionsInfo;
import com.openclassrooms.tourguide.dto.NearByAttraction;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.model.User;
import com.openclassrooms.tourguide.model.UserReward;
import gpsUtil.location.VisitedLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tripPricer.Provider;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
public class TourGuideController {

	@Autowired
	TourGuideService tourGuideService;
	
    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }
    
    @RequestMapping("/getLocation") 
    public VisitedLocation getLocation(@RequestParam String userName) throws InterruptedException, ExecutionException {
    	return tourGuideService.getUserLocation(getUser(userName));
    }

    @RequestMapping("/getNearbyAttractions")
    public CloseAttractionsInfo getNearbyAttractions(@RequestParam String userName) throws ExecutionException, InterruptedException {
        User user = getUser(userName);
    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(user);
    	List<NearByAttraction> nearByAttractions = tourGuideService.getNearByAttractions(visitedLocation, user);
        return new CloseAttractionsInfo(visitedLocation.location.latitude, visitedLocation.location.longitude, nearByAttractions);
    }
    
    @RequestMapping("/getRewards") 
    public List<UserReward> getRewards(@RequestParam String userName) {
    	return tourGuideService.getUserRewards(getUser(userName));
    }
       
    @RequestMapping("/getTripDeals")
    public List<Provider> getTripDeals(@RequestParam String userName) {
    	return tourGuideService.getTripDeals(getUser(userName));
    }
    
    private User getUser(String userName) {
    	return tourGuideService.getUser(userName);
    }
   

}