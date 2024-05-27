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

    /** HTML request using GET method that returns greeting message
     *
     * @return a String message to welcome the user
     */
    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }

    /** HTML request using GET method that returns the current or previous location of the user identified by the userName
     *
     * @param userName string of a User's username
     * @return a Json string of the user's VisitedLocation
     */
    @RequestMapping("/getLocation") 
    public VisitedLocation getLocation(@RequestParam String userName) throws InterruptedException, ExecutionException {
    	return tourGuideService.getUserLocation(getUser(userName));
    }

    /** HTML request using GET method that returns a CloseAttractionsInfo DTO containing a list of the five closest attractions.
     *
     * @param userName string of a User's username
     * @return a Json string of the user's CloseAttractionsInfo
     */
    @RequestMapping("/getNearbyAttractions")
    public CloseAttractionsInfo getNearbyAttractions(@RequestParam String userName) throws ExecutionException, InterruptedException {
        User user = getUser(userName);
    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(user);
    	List<NearByAttraction> nearByAttractions = tourGuideService.getNearByAttractions(visitedLocation, user);
        return new CloseAttractionsInfo(visitedLocation.location.latitude, visitedLocation.location.longitude, nearByAttractions);
    }

    /** HTML GET request that returns the list of UserRewards of a specific user
     * @param userName string of a User's username
     * @return a Json string of the list of UserRewards of the user
     */
    @RequestMapping("/getRewards") 
    public List<UserReward> getRewards(@RequestParam String userName) {
    	return tourGuideService.getUserRewards(getUser(userName));
    }

    /** HTML GET request that returns the list of Provider of a specific user
     *
     * @param userName string of the username (internalUserX)
     * @return a Json string of a user location in longitude and latitude
     */
    @RequestMapping("/getTripDeals")
    public List<Provider> getTripDeals(@RequestParam String userName) {
    	return tourGuideService.getTripDeals(getUser(userName));
    }
    
    private User getUser(String userName) {
    	return tourGuideService.getUser(userName);
    }
   

}