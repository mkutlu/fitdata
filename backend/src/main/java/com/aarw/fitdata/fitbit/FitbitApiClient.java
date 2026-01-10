package com.aarw.fitdata.fitbit;

import com.aarw.fitdata.config.FitbitProps;
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

    public FitbitProfileResponse getProfile(FitbitTokenEntity token) {
        return webClient.get()
                .uri(props.apiBaseUri() + "/1/user/-/profile.json")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                .retrieve()
                .bodyToMono(FitbitProfileResponse.class)
                .block();
    }

    public FitbitStepsSeriesResponse getDailyStepsSeries(FitbitTokenEntity token, String startDate, String endDate) {
        return webClient.get()
                .uri(props.apiBaseUri() + "/1/user/-/activities/steps/date/" + startDate + "/" + endDate + ".json")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                .retrieve()
                .bodyToMono(FitbitStepsSeriesResponse.class)
                .block();
    }

}