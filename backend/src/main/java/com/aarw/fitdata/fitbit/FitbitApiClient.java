package com.aarw.fitdata.fitbit;

import com.aarw.fitdata.config.FitbitProps;
import com.aarw.fitdata.fitbit.dto.FitbitHeartDailyRangeResponse;
import com.aarw.fitdata.fitbit.dto.FitbitProfileResponse;
import com.aarw.fitdata.fitbit.dto.FitbitStepsSeriesResponse;
import com.aarw.fitdata.oauth.token.FitbitTokenEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class FitbitApiClient {

    private final FitbitProps props;
    private final WebClient webClient;

    public FitbitApiClient(FitbitProps props, WebClient.Builder builder) {
        this.props = props;
        this.webClient = builder.build();
    }

    /**
     * Retrieves the user profile information from the Fitbit API.
     *
     * @param token the FitbitTokenEntity containing the access token for the API request
     * @return a FitbitProfileResponse containing the user profile information
     */
    public FitbitProfileResponse getProfile(FitbitTokenEntity token) {
        return webClient.get()
                .uri(props.apiBaseUri() + "/1/user/-/profile.json")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                .retrieve()
                .bodyToMono(FitbitProfileResponse.class)
                .block();
    }

    /**
     * Retrieves the daily steps series data for a specific date range from the Fitbit API.
     *
     * @param token    the FitbitTokenEntity containing the access token for the API request
     * @param startDate the start date for the steps series data, in ISO 8601 format (yyyy-MM-dd)
     * @param endDate  the end date for the steps series data, in ISO 8601 format (yyyy-MM-dd)
     * @return a FitbitStepsSeriesResponse containing the daily steps series data
     */
    public FitbitStepsSeriesResponse getDailyStepsSeries(FitbitTokenEntity token, String startDate, String endDate) {
        return webClient.get()
                .uri(props.apiBaseUri() + "/1/user/-/activities/steps/date/" + startDate + "/" + endDate + ".json")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                .retrieve()
                .bodyToMono(FitbitStepsSeriesResponse.class)
                .block();
    }

    public FitbitHeartDailyRangeResponse getHeartByDateRange(FitbitTokenEntity token, String startDateIso, String endDateIso) {
        return webClient.get()
                .uri(props.apiBaseUri() + "/1/user/-/activities/heart/date/" + startDateIso + "/" + endDateIso + ".json")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                .retrieve()
                .bodyToMono(FitbitHeartDailyRangeResponse.class)
                .block();
    }

    public FitbitHeartDailyRangeResponse getHeartForDay(FitbitTokenEntity token, String dateIso) {
        // Uses the same start/end endpoint with start=end to avoid relying on today/1d only.
        return getHeartByDateRange(token, dateIso, dateIso);
    }

}