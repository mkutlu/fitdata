package com.aarw.fitdata.fitbit.dto;

public record FitbitProfileResponse(
        FitbitUser user
) {
    public record FitbitUser(
            String encodedId,
            String fullName,
            int age,
            String gender
    ) {}
}