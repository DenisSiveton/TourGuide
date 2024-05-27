package com.openclassrooms.tourguide.integrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.openclassrooms.tourguide.dto.CloseAttractionsInfo;
import com.openclassrooms.tourguide.dto.NearByAttraction;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.model.User;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import gpsUtil.location.VisitedLocation;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
public class TourGuideControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TourGuideService tourGuideService;

    @Autowired
    private RewardsService rewardsService;

    @Autowired
    private WebApplicationContext webContext;

    @Before
    public void setupMockmvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webContext).build();
    }

    @Test
    public void getLocation_EachLogicLayerShouldBeOperational_ITest() throws Exception {
        //ARRANGE
        String httpMethod = "get";
        URI uri = new URI("/getLocation");

        InternalTestHelper internalTestHelper = new InternalTestHelper();
        internalTestHelper.setInternalUserNumber(1);
        String userName = "internalUser0";

        VisitedLocation expectedVisitedLocation = tourGuideService.getUser(userName).getLastVisitedLocation();

        //ACT
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.request(httpMethod, uri)
                        .param("userName", userName)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //ASSERT
        assertThat(mvcResult.getRequest().getParameter("userName").contains(userName));

        String responseBody = mvcResult.getResponse().getContentAsString();

        assertThat(responseBody).contains("location\":{\"longitude\"");
        assertThat(expectedVisitedLocation.userId.toString()).isEqualTo(JsonPath.parse(responseBody).read("userId"));

        // Retrieve Date from VisitedLocation and compare both Date values
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.FRANCE);
        formatter.setTimeZone(TimeZone.getTimeZone("Z"));
        Date actualTimeVisited = formatter.parse(JsonPath.parse(responseBody).read("timeVisited"));

        assertThat(expectedVisitedLocation.timeVisited).isEqualTo(actualTimeVisited);
    }

   @Test
    public void getNearbyAttractions_EachLogicLayerShouldBeOperational_ITest() throws Exception {
        //ARRANGE
        String httpMethod = "get";
        URI uri = new URI("/getNearbyAttractions");

        InternalTestHelper internalTestHelper = new InternalTestHelper();
        internalTestHelper.setInternalUserNumber(1);
        String userName = "internalUser0";

        User expectedUser = tourGuideService.getUser(userName);
        VisitedLocation expectedVisitedLocation = expectedUser.getLastVisitedLocation();
        List<NearByAttraction> nearByAttractions = tourGuideService.getNearByAttractions(expectedVisitedLocation, expectedUser);
        CloseAttractionsInfo expectedCloseAttractionsInfo =  new CloseAttractionsInfo(expectedVisitedLocation.location.latitude, expectedVisitedLocation.location.longitude, nearByAttractions);

        //ACT
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.request(httpMethod, uri)
                        .param("userName", userName)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //ASSERT
        assertThat(mvcResult.getRequest().getParameter("userName").contains(userName));

        String responseBody = mvcResult.getResponse().getContentAsString();

        assertThat(responseBody).contains("userLocationLatitude");
        assertThat(responseBody).contains("userLocationLongitude");
        assertThat(responseBody).contains("attractionDistanceFromUserLocation");
        for (int i = 0; i < 4; i++) {
            double distanceAttractionOne = JsonPath.parse(responseBody).read("nearByAttractions[" + (i + 1) +"].attractionDistanceFromUserLocation");
            double distanceAttractionTwo = JsonPath.parse(responseBody).read("nearByAttractions[" + i +"].attractionDistanceFromUserLocation");
            assertThat(distanceAttractionOne).isGreaterThanOrEqualTo(distanceAttractionTwo);
            System.out.println("Attraction number "+ i + " is closer to the user than Attraction number " + (i + 1)
                    + " by "+ (distanceAttractionOne - distanceAttractionTwo) + " miles.");
        }
    }
    @Test
    public void getRewards_EachLogicLayerShouldBeOperational_ITest() throws Exception {
        //ARRANGE
        String httpMethod = "get";
        URI uri = new URI("/getRewards");

        InternalTestHelper internalTestHelper = new InternalTestHelper();
        internalTestHelper.setInternalUserNumber(1);
        String userName = "internalUser0";

        rewardsService.setProximityBuffer(Integer.MAX_VALUE);
        rewardsService.calculateRewards(tourGuideService.getUser(userName));
        //ACT
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.request(httpMethod, uri)
                        .param("userName", userName)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //ASSERT
        assertThat(mvcResult.getRequest().getParameter("userName").contains(userName));

        String responseBody = mvcResult.getResponse().getContentAsString();

        //Makes sure that all attractions (26) are taken into account since ProximityBuffer was set to Max.
        assertThat((int) JsonPath.parse(responseBody).read("$[25]rewardPoints")).isBetween(1,1000);
    }

    @Test
    public void getTripDeals_EachLogicLayerShouldBeOperational_ITest() throws Exception {
        //ARRANGE
        String httpMethod = "get";
        URI uri = new URI("/getTripDeals");

        InternalTestHelper internalTestHelper = new InternalTestHelper();
        internalTestHelper.setInternalUserNumber(1);
        String userName = "internalUser0";

        rewardsService.setProximityBuffer(Integer.MAX_VALUE);
        rewardsService.calculateRewards(tourGuideService.getUser(userName));

        //ACT
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.request(httpMethod, uri)
                        .param("userName", userName)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //ASSERT
        assertThat(mvcResult.getRequest().getParameter("userName").contains(userName));

        String responseBody = mvcResult.getResponse().getContentAsString();

        //Makes sure that all attractions (26) are taken into account since ProximityBuffer was set to Max.
        assertThat(responseBody).contains("tripId");
        assertThat((double) JsonPath.parse(responseBody).read("$[4]price")).isGreaterThanOrEqualTo(0);
    }
}
