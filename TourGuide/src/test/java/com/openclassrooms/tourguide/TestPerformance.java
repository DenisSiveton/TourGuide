package com.openclassrooms.tourguide;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;

public class TestPerformance {

	/*
	 * A note on performance improvements:
	 * 
	 * The number of users generated for the high volume tests can be easily
	 * adjusted via this method:
	 * 
	 * InternalTestHelper.setInternalUserNumber(100000);
	 * 
	 * 
	 * These tests can be modified to suit new solutions, just as long as the
	 * performance metrics at the end of the tests remains consistent.
	 * 
	 * These are performance metrics that we are trying to hit:
	 * 
	 * highVolumeTrackLocation: 100,000 users within 15 minutes:
	 * assertTrue(TimeUnit.MINUTES.toSeconds(15) >=
	 * TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	 *
	 * highVolumeGetRewards: 100,000 users within 20 minutes:
	 * assertTrue(TimeUnit.MINUTES.toSeconds(20) >=
	 * TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	 */

	/**
	 *  This test intends to track the location of several users using the batch method of trackUserLocation()
	 * <p>
	 *  For performance's sake, it ensures that in case of high volume users (up to 100,000 users) the duration of the test is under 15 minutes, otherwise the test fails.
	 *  The duration is calculated using the StopWatch class.
	 */
	@Test
	@Disabled
	// Users should be incremented up to 100,000, and test finishes within 15 minutes
	public void highVolumeTrackLocation() throws ExecutionException, InterruptedException {
		// ARRANGE
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

		// Data : set the amount of user to test the performance at different scale
		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		List<User> allUsers = tourGuideService.getAllUsers();

		// ACT
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		tourGuideService.trackUserLocationBatch(allUsers);
		stopWatch.stop();
		tourGuideService.tracker.stopTracking();

		System.out.println("highVolumeTrackLocation: Time Elapsed: "
				+ TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");

		// ASSERT
			//test should be completed under 15 minutes
		assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}

	/**
	 *  This test intends to the rewards for several users using the batch method of calculateRewards()
	 * <p>
	 *  For performance's sake, it ensures that in case of high volume users (up to 100,000 users) the duration of the test is under 20 minutes, otherwise the test fails.
	 *  The duration is calculated using the StopWatch class.
	 */
	@Test
	@Disabled
	// Users should be incremented up to 100,000, and test finishes within 20 minutes
	public void highVolumeGetRewards() throws ExecutionException, InterruptedException {
		// ARRANGE
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

			// Data : set the amount of users to test the performance at different scale
		InternalTestHelper.setInternalUserNumber(0);

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		Attraction attraction = gpsUtil.getAttractions().get(0);

			// Retrieve all the users in a list
		List<User> allUsers = tourGuideService.getAllUsers();

		// ACT
		allUsers.forEach(u -> u.addToVisitedLocations(new VisitedLocation(u.getUserId(), attraction, new Date())));

			//Use a new method that calculate the rewards in batch for a list of users instead of using a method for each user
		rewardsService.calculateRewardsBatch(allUsers);

		for (User user : allUsers) {
            assertFalse(user.getUserRewards().isEmpty());
		}
		stopWatch.stop();
		tourGuideService.tracker.stopTracking();

		System.out.println("highVolumeGetRewards: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime())
				+ " seconds.");
		// ASSERT
			//test should be completed under 20 minutes
		assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}

}
