package com.openclassrooms.tourguide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
    private static final int DEFAULT_PROXIMITY_BUFFER = 10;
	private int proximityBuffer = DEFAULT_PROXIMITY_BUFFER;
	private static final int ATTRACTION_PROXIMITY_RANGE = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;
	
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
				// explication ...
				if(!isAttractionAlreadyRewarded(user, attraction) && (nearAttraction(visitedLocation, attraction))) {
						user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
				}
			}
		}
	}

	/**
	 * This method receives a list of users and intends to calculate the rewards (see calculateRewards(User user)) for each of them.
	 * For performance's sake, the ExecutorService class is used to optimize the process time.
	 * A dynamic pool of threads is instantiated from the start and will grow as needed to achieve the method role.
	 *
	 * @param users the list of users whose rewards will be calculated
	 * @throws ExecutionException Exception when a task can not compute as it should.
	 * @throws InterruptedException Exception when a task from a thread is interrupted and cannot be completed.
	 *
	 * @author Denis Siveton
	 * @version 1.0.0
	 */
	public void calculateRewardsBatch(List<User> users) throws ExecutionException, InterruptedException {
		ExecutorService executorService = Executors.newCachedThreadPool();
		List<Future<String>> futures = new ArrayList<>();
		for(final User user : users) {

			Future<String> future = executorService.submit(new Callable<String>() {
				@Override
				public String call() throws Exception {
					calculateRewards(user);
					return user.getUserName() + "updated.";
				}
			});

			futures.add(future);
		}

		executorService.shutdown();

		for(Future<String> future : futures) {
			String message = future.get();
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
