package com.team2.userservice.region.dto;

import java.util.List;

public record VWorldAddressResponse(
        Response response
) {

    public record Response(
            String status,
            List<Result> result
    ) {
    }

    public record Result(
            Structure structure
    ) {
    }

    public record Structure(
            String level1,
            String level2,
            String level4A,
            String level4AC
    ) {
    }
}