package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.dto.NearByAttraction;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;

	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;
		
		Locale.setDefault(Locale.US);

		if (testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}

	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	public VisitedLocation getUserLocation(User user) throws ExecutionException, InterruptedException {
		VisitedLocation visitedLocation = (!user.getVisitedLocations().isEmpty()) ? user.getLastVisitedLocation()
				: trackUserLocation(user);
		return visitedLocation;
	}

	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}

	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}

	public void addUser(User user) {
		if (!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}

	public List<Provider> getTripDeals(User user) {
		int cumulativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(), cumulativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}

	public VisitedLocation trackUserLocation(User user) throws ExecutionException, InterruptedException {
		VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
		user.addToVisitedLocations(visitedLocation);
		rewardsService.calculateRewards(user);
		return visitedLocation;
	}

	/**
	 * This method receives a list of users and intends to track their location (see trackUserLocation(User user)) for each of them.
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
	//Function to optimize with multithreading
	public void trackUserLocationBatch(List<User> users) throws ExecutionException, InterruptedException {
		int userListSize = users.size();
		int numberOfThreads = 15;
		int subListSize = userListSize/numberOfThreads;
		List<CompletableFuture<Void>> completableFutureList = new ArrayList<>();
		for (int j = 0; j < userListSize ; j+= subListSize) {
			List<User> userSubList = new ArrayList<>(users.subList(j, Math.min(userListSize, j + subListSize)));
			CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(()-> {
				for (User user : userSubList) {
					try {
						trackUserLocation(user);
					} catch (ExecutionException e) {
						throw new RuntimeException(e);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			});
			completableFutureList.add(completableFuture);
		}

		for (CompletableFuture<Void> completableFuture : completableFutureList
		) {
			completableFuture.get();
		}
	}

	/**
	 * This method receives a user with its location and calculate the five nearest attraction.
	 * It returns a list of DTO called NearByAttraction (see NearByAttraction class for more details)
	 * The list is then sorted out by the distance between the user and the attraction locations.
	 *
	 * @param visitedLocation Location of the user
	 * @param user User of the app
	 * @return list of the five nearest attraction based off the user's location.
	 *
	 * @author Denis Siveton
	 * @version 1.0.0
	 */
	public List<NearByAttraction> getNearByAttractions(VisitedLocation visitedLocation, User user) {
		List<NearByAttraction> nearbyAttractions = new ArrayList<>();
		List<Attraction> availableAttractions = gpsUtil.getAttractions();

		for (Attraction attraction : availableAttractions) {
			if (nearbyAttractions.size()<5){
				nearbyAttractions.add(new NearByAttraction(attraction.attractionName, attraction.latitude, attraction.longitude, rewardsService.getDistanceFromVisitedLocation(visitedLocation, attraction), rewardsService.getRewardPoints(attraction, user)));
				nearbyAttractions.sort(Comparator.comparing(NearByAttraction::getAttractionDistanceFromUserLocation));
			}
			else{
				double distanceBetweenUserAndAttraction = rewardsService.getDistanceFromVisitedLocation(visitedLocation, attraction);
				int indexOfList = findIndexByDistance(nearbyAttractions, distanceBetweenUserAndAttraction);
				if(indexOfList > -1){
					nearbyAttractions.add(indexOfList, new NearByAttraction(attraction.attractionName, attraction.latitude, attraction.longitude, rewardsService.getDistanceFromVisitedLocation(visitedLocation, attraction), rewardsService.getRewardPoints(attraction, user)));
					nearbyAttractions.remove(5);
				}
			}
		}
		return nearbyAttractions;
	}

	private static int findIndexByDistance(List<NearByAttraction> nearByAttractionList, double distance) {
		int index = -1;
		for (int i = 0; i < nearByAttractionList.size(); i++) {
			if (nearByAttractionList.get(i).getAttractionDistanceFromUserLocation() > distance) {
				return i;
			}
		}
		return index;
	}

	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				tracker.stopTracking();
			}
		});
	}

	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes
	// internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();

	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);
			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i -> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
					new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}

	private double generateRandomLongitude() {
		double leftLimit = -180;
		double rightLimit = 180;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}

}
