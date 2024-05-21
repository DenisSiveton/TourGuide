package com.openclassrooms.tourguide.unit_test.controller;

import com.jayway.jsonpath.JsonPath;
import com.openclassrooms.tourguide.TourguideApplication;
import com.openclassrooms.tourguide.controller.TourGuideController;
import com.openclassrooms.tourguide.dto.NearByAttraction;
import com.openclassrooms.tourguide.model.User;
import com.openclassrooms.tourguide.model.UserReward;
import com.openclassrooms.tourguide.service.TourGuideService;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tripPricer.Provider;

import java.net.URI;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TourguideApplication.class)
@AutoConfigureMockMvc
@ComponentScan("com.nnk.springboot.controllers")
public class TourGuideControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TourGuideController tourGuideController;

    @MockBean
    private TourGuideService tourGuideService;

    static String userName;

    @BeforeEach
    public void setUpData(){
        userName = "userNameTest";
    }

    @Test
    public void index_shouldReturnTheCorrectString() throws Exception {
        //ARRANGE
        String httpMethod = "get";
        URI uri = new URI("/");
        String expectedBodyContent = "Greetings from TourGuide!";

        //ACT
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.request(httpMethod, uri))
                .andExpect(status().isOk())
                .andReturn();

        //ASSERT

        assertThat(mvcResult.getResponse().getContentAsString()).isNotEmpty();
        assertThat(mvcResult.getResponse().getContentAsString()).isEqualTo(expectedBodyContent);
    }

    @Test
    public void getLocation_ShouldCallTheCorrectMethod() throws Exception {
        //ARRANGE
        String httpMethod = "get";
        URI uri = new URI("/getLocation");

        User userTest = new User(UUID.randomUUID(), "userNameTest", "000-001", "email@test.com");

        Location location = new Location(50.252525, 122.363636);
        VisitedLocation visitedLocationTest = new VisitedLocation(UUID.randomUUID(), location, new Date());

        when(tourGuideService.getUser(userName)).thenReturn(userTest);
        when(tourGuideService.getUserLocation(userTest)).thenReturn(visitedLocationTest);
        //ACT
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.request(httpMethod, uri)
                .param("userName", userName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //ASSERT

        String response = mvcResult.getResponse().getContentAsString();
        assertThat(visitedLocationTest.location.longitude).isEqualTo(JsonPath.parse(response).read("location.longitude"));

            // Mocked calls
        verify(tourGuideService, times(1)).getUser(userName);
        verify(tourGuideService, times(1)).getUserLocation(userTest);
    }

    @Test
    public void getNearbyAttractions_ShouldCallTheCorrectMethod() throws Exception {
        //ARRANGE
        String httpMethod = "get";
        URI uri = new URI("/getNearbyAttractions");

        User userTest = new User(UUID.randomUUID(), "userNameTest", "000-001", "email@test.com");

        Location location = new Location(50.252525, 122.363636);
        VisitedLocation visitedLocationTest = new VisitedLocation(UUID.randomUUID(), location, new Date());

        List<NearByAttraction> nearByAttractionsTest = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            nearByAttractionsTest.add(new NearByAttraction("attraction #" + i,
                    -85 +30*i,
                    180 -40*i,
                    1+i,
                    (int) (new Random().nextDouble() * 1000+300)));
        }
        when(tourGuideService.getUser(userName)).thenReturn(userTest);
        when(tourGuideService.getUserLocation(userTest)).thenReturn(visitedLocationTest);
        when(tourGuideService.getNearByAttractions(visitedLocationTest, userTest)).thenReturn(nearByAttractionsTest);
        //ACT
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.request(httpMethod, uri)
                        .param("userName", userName)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //ASSERT

        String response = mvcResult.getResponse().getContentAsString();
        assertThat(visitedLocationTest.location.longitude).isEqualTo(JsonPath.parse(response).read("userLocationLongitude"));
        assertThat(nearByAttractionsTest.get(4).getAttractionName()).isEqualTo(JsonPath.parse(response).read("nearByAttractions[4].attractionName"));

        // Mocked calls
        verify(tourGuideService, times(1)).getUser(userName);
        verify(tourGuideService, times(1)).getUserLocation(userTest);
        verify(tourGuideService, times(1)).getNearByAttractions(visitedLocationTest, userTest);
    }

    @Test
    public void getRewards_ShouldCallTheCorrectMethod() throws Exception {
        //ARRANGE
        String httpMethod = "get";
        URI uri = new URI("/getRewards");

        User userTest = new User(UUID.randomUUID(), "userNameTest", "000-001", "email@test.com");

        List<Location> locationList = new ArrayList<>();
        locationList.add(new Location(-50.252525, -122.363636));
        locationList.add(new Location(10.252525, 10.363636));
        locationList.add(new Location(50.252525, 122.363636));

        List<UserReward> userRewardListTest = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            userRewardListTest.add(new UserReward(new VisitedLocation(UUID.randomUUID(), locationList.get(i), new Date()),
                    new Attraction("attraction "+i, "city "+i, "state "+i, -85+30*i, -180+40*i),
                    (int) (new Random().nextDouble() * 1000+300)));
        }

        when(tourGuideService.getUser(userName)).thenReturn(userTest);
        when(tourGuideService.getUserRewards(userTest)).thenReturn(userRewardListTest);

        //ACT
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.request(httpMethod, uri)
                        .param("userName", userName)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //ASSERT

        String response = mvcResult.getResponse().getContentAsString();
        assertThat(locationList.get(1).longitude).isEqualTo(JsonPath.parse(response).read("$[1]visitedLocation.location.longitude"));
        assertThat(userRewardListTest.get(2).attraction.city).isEqualTo(JsonPath.parse(response).read("$[2].attraction.city"));

        // Mocked calls
        verify(tourGuideService, times(1)).getUser(userName);
        verify(tourGuideService, times(1)).getUserRewards(userTest);
    }

    @Test
    public void getTripDeals_ShouldCallTheCorrectMethod() throws Exception {
        //ARRANGE
        String httpMethod = "get";
        URI uri = new URI("/getTripDeals");

        User userTest = new User(UUID.randomUUID(), "userNameTest", "000-001", "email@test.com");

        List<Provider> providerList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            double price = new Random().nextDouble() * 1000+300;
            providerList.add(new Provider(UUID.randomUUID(), "provider name "+i,Math.round(price*100)/100d));
        }

        when(tourGuideService.getUser(userName)).thenReturn(userTest);
        when(tourGuideService.getTripDeals(userTest)).thenReturn(providerList);

        //ACT
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.request(httpMethod, uri)
                        .param("userName", userName)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //ASSERT

        String response = mvcResult.getResponse().getContentAsString();
        assertThat(providerList.get(1).name).isEqualTo(JsonPath.parse(response).read("$[1].name"));
        for (Provider provider : providerList) {
            assertThat(provider.price).isBetween(300.00,1300.00);
        }


        // Mocked calls
        verify(tourGuideService, times(1)).getUser(userName);
        verify(tourGuideService, times(1)).getTripDeals(userTest);
    }
}
