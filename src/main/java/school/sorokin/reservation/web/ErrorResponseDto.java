package school.sorokin.reservation.web;

import java.time.LocalDate;

public record ErrorResponseDto(
        String message,
        String detailedMassage,
        LocalDate errortime
) {
}

