package com.aarw.fitdata.fitbit;

import com.aarw.fitdata.config.FitbitProps;
import com.aarw.fitdata.fitbit.dto.*;
import com.aarw.fitdata.oauth.token.FitbitTokenEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
public class FitbitApiClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FitbitApiClient.class);

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
        String url = props.apiBaseUri() + "/1/user/-/profile.json";
        try {
            return webClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                    .retrieve()
                    .onStatus(status -> status.value() == 429, resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .flatMap(body -> {
                                        String retryAfter = resp.headers().asHttpHeaders().getFirst(HttpHeaders.RETRY_AFTER);
                                        return Mono.error(new FitbitRateLimitException("Fitbit API rate limit exceeded. Retry after: " + retryAfter, retryAfter, body));
                                    })
                    )
                    .bodyToMono(FitbitProfileResponse.class)
                    .block();
        } catch (FitbitRateLimitException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Fitbit profile call failed: " + url, e);
        }
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
        String url = props.apiBaseUri() + "/1/user/-/activities/steps/date/" + startDate + "/" + endDate + ".json";
        try {
            return webClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                    .retrieve()
                    .onStatus(status -> status.value() == 429, resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .flatMap(body -> {
                                        String retryAfter = resp.headers().asHttpHeaders().getFirst(HttpHeaders.RETRY_AFTER);
                                        return Mono.error(new FitbitRateLimitException("Fitbit API rate limit exceeded. Retry after: " + retryAfter, retryAfter, body));
                                    })
                    )
                    .bodyToMono(FitbitStepsSeriesResponse.class)
                    .block();
        } catch (FitbitRateLimitException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Fitbit steps series call failed: " + url, e);
        }
    }

    public FitbitHeartDailyRangeResponse getHeartByDateRange(FitbitTokenEntity token, String startDateIso, String endDateIso) {
        String url = props.apiBaseUri() + "/1/user/-/activities/heart/date/" + startDateIso + "/" + endDateIso + ".json";
        try {
            return webClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                    .retrieve()
                    .onStatus(status -> status.value() == 429, resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .flatMap(body -> {
                                        String retryAfter = resp.headers().asHttpHeaders().getFirst(HttpHeaders.RETRY_AFTER);
                                        return Mono.error(new FitbitRateLimitException("Fitbit API rate limit exceeded. Retry after: " + retryAfter, retryAfter, body));
                                    })
                    )
                    .bodyToMono(FitbitHeartDailyRangeResponse.class)
                    .block();
        } catch (FitbitRateLimitException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Fitbit heart range call failed: " + url, e);
        }
    }


    public FitbitHeartIntradayResponse getHeartIntraday(FitbitTokenEntity token, String dateIso, String detailLevel) {
        String url = props.apiBaseUri() + "/1/user/-/activities/heart/date/" + dateIso + "/1d/" + detailLevel + ".json";

        try {
            return webClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                    .retrieve()
                    .onStatus(status -> status.value() == 429, resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .flatMap(body -> {
                                        String retryAfter = resp.headers().asHttpHeaders().getFirst(HttpHeaders.RETRY_AFTER);
                                        return Mono.error(new FitbitRateLimitException("Fitbit API rate limit exceeded. Retry after: " + retryAfter, retryAfter, body));
                                    })
                    )
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .map(body -> new RuntimeException("Fitbit intraday HR API error: HTTP " + resp.statusCode() + " body=" + body))
                    )
                    .bodyToMono(FitbitHeartIntradayResponse.class)
                    .block();
        } catch (FitbitRateLimitException e) {
            throw e;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                String retryAfter = e.getHeaders().getFirst(HttpHeaders.RETRY_AFTER);
                throw new FitbitRateLimitException("Fitbit API rate limit exceeded. Retry after: " + retryAfter, retryAfter, e.getResponseBodyAsString());
            }
            throw new RuntimeException("Fitbit intraday HR call failed: HTTP " + e.getStatusCode() + " body=" + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Fitbit intraday HR call failed: " + url, e);
        }
    }

    public FitbitHeartDailyRangeResponse getHeartForDay(FitbitTokenEntity token, String dateIso) {
        String url = props.apiBaseUri() + "/1/user/-/activities/heart/date/" + dateIso + "/" + dateIso + ".json";
        try {
            return webClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                    .retrieve()
                    .onStatus(status -> status.value() == 429, resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .flatMap(body -> {
                                        String retryAfter = resp.headers().asHttpHeaders().getFirst(HttpHeaders.RETRY_AFTER);
                                        return Mono.error(new FitbitRateLimitException("Fitbit API rate limit exceeded. Retry after: " + retryAfter, retryAfter, body));
                                    })
                    )
                    .bodyToMono(FitbitHeartDailyRangeResponse.class)
                    .block();
        } catch (FitbitRateLimitException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Fitbit daily heart call failed: " + url, e);
        }
    }

    public FitbitActivitiesSummaryResponse getActivitiesSummaryForDay(FitbitTokenEntity token, String dateIso) {
        String url = props.apiBaseUri() + "/1/user/-/activities/date/" + dateIso + ".json";
        try {
            return webClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                    .retrieve()
                    .onStatus(status -> status.value() == 429, resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .flatMap(body -> {
                                        String retryAfter = resp.headers().asHttpHeaders().getFirst(HttpHeaders.RETRY_AFTER);
                                        return Mono.error(new FitbitRateLimitException("Fitbit API rate limit exceeded. Retry after: " + retryAfter, retryAfter, body));
                                    })
                    )
                    .bodyToMono(FitbitActivitiesSummaryResponse.class)
                    .block();
        } catch (FitbitRateLimitException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Fitbit activity summary call failed: " + url, e);
        }
    }

    public FitbitWeightResponse getWeightSeries(FitbitTokenEntity token, String startDate, String endDate) {
        String url = props.apiBaseUri() + "/1/user/-/body/log/weight/date/" + startDate + "/" + endDate + ".json";
        try {
            return webClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                    .retrieve()
                    .onStatus(status -> status.value() == 429, resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .flatMap(body -> {
                                        String retryAfter = resp.headers().asHttpHeaders().getFirst(HttpHeaders.RETRY_AFTER);
                                        return Mono.error(new FitbitRateLimitException("Fitbit API rate limit exceeded. Retry after: " + retryAfter, retryAfter, body));
                                    })
                    )
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .map(body -> new RuntimeException("Fitbit Weight API error: HTTP " + resp.statusCode() + " body=" + body))
                    )
                    .bodyToMono(FitbitWeightResponse.class)
                    .block();
        } catch (FitbitRateLimitException e) {
            throw e;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                String retryAfter = e.getHeaders().getFirst(HttpHeaders.RETRY_AFTER);
                throw new FitbitRateLimitException("Fitbit API rate limit exceeded. Retry after: " + retryAfter, retryAfter, e.getResponseBodyAsString());
            }
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
                    .onStatus(status -> status.value() == 429, resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .flatMap(body -> {
                                        String retryAfter = resp.headers().asHttpHeaders().getFirst(HttpHeaders.RETRY_AFTER);
                                        return Mono.error(new FitbitRateLimitException("Fitbit API rate limit exceeded. Retry after: " + retryAfter, retryAfter, body));
                                    })
                    )
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .map(body -> new RuntimeException("Fitbit Sleep API error: HTTP " + resp.statusCode() + " body=" + body))
                    )
                    .bodyToMono(FitbitSleepResponse.class)
                    .block();
        } catch (FitbitRateLimitException e) {
            throw e;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                String retryAfter = e.getHeaders().getFirst(HttpHeaders.RETRY_AFTER);
                throw new FitbitRateLimitException("Fitbit API rate limit exceeded. Retry after: " + retryAfter, retryAfter, e.getResponseBodyAsString());
            }
            throw new RuntimeException("Fitbit Sleep call failed: HTTP " + e.getStatusCode() + " body=" + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Fitbit Sleep call failed: " + url, e);
        }
    }

    public FitbitVo2MaxResponse getVo2Max(FitbitTokenEntity token, String date) {
        String url = props.apiBaseUri() + "/1/user/-/cardioscore/date/" + date + ".json";
        try {
            return webClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                    .retrieve()
                    .onStatus(status -> status.value() == 429, resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .flatMap(body -> {
                                        String retryAfter = resp.headers().asHttpHeaders().getFirst(HttpHeaders.RETRY_AFTER);
                                        return Mono.error(new FitbitRateLimitException("Fitbit API rate limit exceeded. Retry after: " + retryAfter, retryAfter, body));
                                    })
                    )
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .flatMap(body -> {
                                        if (resp.statusCode().value() == 404 || resp.statusCode().value() == 403) {
                                            return Mono.empty();
                                        }
                                        log.error("Fitbit VO2 Max API error for date {}: HTTP {} body={}", date, resp.statusCode(), body);
                                        return Mono.error(new RuntimeException("Fitbit VO2 Max API error: HTTP " + resp.statusCode() + " body=" + body));
                                    })
                    )
                    .bodyToMono(FitbitVo2MaxResponse.class)
                    .defaultIfEmpty(new FitbitVo2MaxResponse(java.util.Collections.emptyList()))
                    .block();
        } catch (FitbitRateLimitException e) {
            throw e;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                String retryAfter = e.getHeaders().getFirst(HttpHeaders.RETRY_AFTER);
                throw new FitbitRateLimitException("Fitbit API rate limit exceeded. Retry after: " + retryAfter, retryAfter, e.getResponseBodyAsString());
            }
            throw new RuntimeException("Fitbit VO2 Max call failed: HTTP " + e.getStatusCode() + " body=" + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Fitbit VO2 Max call failed: " + url, e);
        }
    }

    public FitbitHrvResponse getHrv(FitbitTokenEntity token, String date) {
        String url = props.apiBaseUri() + "/1/user/-/hrv/date/" + date + ".json";
        try {
            return webClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                    .retrieve()
                    .onStatus(status -> status.value() == 429, resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .flatMap(body -> {
                                        String retryAfter = resp.headers().asHttpHeaders().getFirst(HttpHeaders.RETRY_AFTER);
                                        return Mono.error(new FitbitRateLimitException("Fitbit API rate limit exceeded. Retry after: " + retryAfter, retryAfter, body));
                                    })
                    )
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .flatMap(body -> {
                                        if (resp.statusCode().value() == 404) {
                                            return Mono.empty();
                                        }
                                        log.error("Fitbit HRV API error for date {}: HTTP {} body={}", date, resp.statusCode(), body);
                                        return Mono.error(new RuntimeException("Fitbit HRV API error: HTTP " + resp.statusCode() + " body=" + body));
                                    })
                    )
                    .bodyToMono(FitbitHrvResponse.class)
                    .defaultIfEmpty(new FitbitHrvResponse(java.util.Collections.emptyList()))
                    .block();
        } catch (FitbitRateLimitException e) {
            throw e;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                String retryAfter = e.getHeaders().getFirst(HttpHeaders.RETRY_AFTER);
                throw new FitbitRateLimitException("Fitbit API rate limit exceeded. Retry after: " + retryAfter, retryAfter, e.getResponseBodyAsString());
            }
            throw new RuntimeException("Fitbit HRV call failed: HTTP " + e.getStatusCode() + " body=" + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Fitbit HRV call failed: " + url, e);
        }
    }

    public FitbitHrvResponse getHrvRange(FitbitTokenEntity token, String startDate, String endDate) {
        String url = props.apiBaseUri() + "/1/user/-/hrv/date/" + startDate + "/" + endDate + ".json";
        try {
            return webClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                    .retrieve()
                    .onStatus(status -> status.value() == 429, resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .flatMap(body -> {
                                        String retryAfter = resp.headers().asHttpHeaders().getFirst(HttpHeaders.RETRY_AFTER);
                                        return Mono.error(new FitbitRateLimitException("Fitbit API rate limit exceeded. Retry after: " + retryAfter, retryAfter, body));
                                    })
                    )
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .flatMap(body -> {
                                        if (resp.statusCode().value() == 404) {
                                            return Mono.empty();
                                        }
                                        log.error("Fitbit HRV Range API error for {} to {}: HTTP {} body={}", startDate, endDate, resp.statusCode(), body);
                                        return Mono.error(new RuntimeException("Fitbit HRV Range API error: HTTP " + resp.statusCode() + " body=" + body));
                                    })
                    )
                    .bodyToMono(FitbitHrvResponse.class)
                    .defaultIfEmpty(new FitbitHrvResponse(java.util.Collections.emptyList()))
                    .block();
        } catch (FitbitRateLimitException e) {
            throw e;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                String retryAfter = e.getHeaders().getFirst(HttpHeaders.RETRY_AFTER);
                throw new FitbitRateLimitException("Fitbit API rate limit exceeded. Retry after: " + retryAfter, retryAfter, e.getResponseBodyAsString());
            }
            throw new RuntimeException("Fitbit HRV Range call failed: HTTP " + e.getStatusCode() + " body=" + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Fitbit HRV Range call failed: " + url, e);
        }
    }

}