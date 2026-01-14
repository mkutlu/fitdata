package com.aarw.fitdata.fitbit;

import com.aarw.fitdata.config.FitbitProps;
import com.aarw.fitdata.fitbit.dto.*;
import com.aarw.fitdata.oauth.token.FitbitTokenEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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


    public FitbitHeartIntradayResponse getHeartIntraday(FitbitTokenEntity token, String dateIso, String detailLevel) {
        String url = props.apiBaseUri() + "/1/user/-/activities/heart/date/" + dateIso + "/1d/" + detailLevel + ".json";

        try {
            return webClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .map(body -> new RuntimeException("Fitbit intraday HR API error: HTTP " + resp.statusCode() + " body=" + body))
                    )
                    .bodyToMono(FitbitHeartIntradayResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new RuntimeException("Fitbit intraday HR call failed: HTTP " + e.getStatusCode() + " body=" + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Fitbit intraday HR call failed: " + url, e);
        }
    }

    public FitbitHeartDailyRangeResponse getHeartForDay(FitbitTokenEntity token, String dateIso) {
        return webClient.get()
                .uri(props.apiBaseUri() + "/1/user/-/activities/heart/date/" + dateIso + "/" + dateIso + ".json")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                .retrieve()
                .bodyToMono(FitbitHeartDailyRangeResponse.class)
                .block();
    }

    public FitbitActivitiesSummaryResponse getActivitiesSummaryForDay(FitbitTokenEntity token, String dateIso) {
        return webClient.get()
                .uri(props.apiBaseUri() + "/1/user/-/activities/date/" + dateIso + ".json")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                .retrieve()
                .bodyToMono(FitbitActivitiesSummaryResponse.class)
                .block();
    }

    public FitbitWeightResponse getWeightSeries(FitbitTokenEntity token, String startDate, String endDate) {
        String url = props.apiBaseUri() + "/1/user/-/body/log/weight/date/" + startDate + "/" + endDate + ".json";
        try {
            return webClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .map(body -> new RuntimeException("Fitbit Weight API error: HTTP " + resp.statusCode() + " body=" + body))
                    )
                    .bodyToMono(FitbitWeightResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new RuntimeException("Fitbit Weight call failed: HTTP " + e.getStatusCode() + " body=" + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Fitbit Weight call failed: " + url, e);
        }
    }

    public FitbitSleepResponse getSleep(FitbitTokenEntity token, String date) {
        String url = props.apiBaseUri() + "/1.2/user/-/sleep/date/" + date + ".json";
        try {
            return webClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .map(body -> new RuntimeException("Fitbit Sleep API error: HTTP " + resp.statusCode() + " body=" + body))
                    )
                    .bodyToMono(FitbitSleepResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new RuntimeException("Fitbit Sleep call failed: HTTP " + e.getStatusCode() + " body=" + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Fitbit Sleep call failed: " + url, e);
        }
    }

}