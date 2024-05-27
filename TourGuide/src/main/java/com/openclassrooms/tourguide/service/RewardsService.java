package com.openclassrooms.tourguide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.model.User;
import com.openclassrooms.tourguide.model.UserReward;


@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
    private static final int DEFAULT_PROXIMITY_BUFFER = 10;
	private int proximityBuffer = DEFAULT_PROXIMITY_BUFFER;
	private static final int ATTRACTION_PROXIMITY_RANGE = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);

	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}
	
	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}
	
	public void setDefaultProximityBuffer() {
		proximityBuffer = DEFAULT_PROXIMITY_BUFFER;
	}
	
	public void calculateRewards(User user) {
		//The CopyOnWriteArrayList of the visited locations of the user ensure that the concurrentModificationException will not occur.
		List<VisitedLocation> userLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());
		List<Attraction> attractions = gpsUtil.getAttractions();

		searchForNewRewards(user, userLocations, attractions);
	}

	private void searchForNewRewards(User user, List<VisitedLocation> userLocations, List<Attraction> attractions) {
		for(VisitedLocation visitedLocation : userLocations) {
			for(Attraction attraction : attractions) {
				// For each attraction, the code checks if the user isn't already rewarded and close enough.
				//    If both criteria are met, the User gets a new reward for that attraction
				if(!isAttractionAlreadyRewarded(user, attraction) && (nearAttraction(visitedLocation, attraction))) {
						user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
				}
			}
		}
	}

	/**
	 * This method receives a list of users and intends to calculate the rewards (see calculateRewards(User user)) for each of them.
	 * For performance's sake, the ExecutorService class is used to optimize the process time.
	 * A dynamic pool of threads is instantiated from the start and will grow as needed to achieve the method's role.
	 *
	 * @param users the list of users whose rewards will be calculated
	 * @author Denis Siveton
	 * @version 1.0.0
	 */
	public void calculateRewardsBatch(List<User> users) throws RuntimeException {
		try {
			ExecutorService executorService = Executors.newCachedThreadPool();
			for (User user : users) {
				Runnable runnableTask = () -> {
					calculateRewards(user);
				};
				executorService.execute(runnableTask);
			}
			executorService.shutdown();
			executorService.awaitTermination(20, TimeUnit.MINUTES);
		} catch (InterruptedException interruptedException) {
			logger.debug(interruptedException.getMessage());
		}
	}

	/**
	 * This method checks if an attraction is already among the reward list of the user by comparing their names
	 * @param user user of the app
	 * @param attraction attraction that is analysed for reward
	 *
	 * @return a boolean which is true if the user has already received rewards for that attraction specifically.
	 * @author Denis Siveton
	 * @version 1.0.0
	 */
	private static boolean isAttractionAlreadyRewarded(User user, Attraction attraction) {
		boolean isAttractionAlreadyRewarded = false;
		for (UserReward userReward : user.getUserRewards()) {
			if(userReward.attraction.attractionName.equals(attraction.attractionName)){
				isAttractionAlreadyRewarded = true;
				break;
			}
		}
		return isAttractionAlreadyRewarded;
	}

	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) <= ATTRACTION_PROXIMITY_RANGE;
	}
	
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) <= proximityBuffer;
	}
	
	int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}

	public double getDistanceFromVisitedLocation(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location);
	}
	public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        return STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
	}

}
