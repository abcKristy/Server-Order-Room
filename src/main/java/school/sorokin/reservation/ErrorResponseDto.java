package school.sorokin.reservation;

import java.time.LocalDate;

public record ErrorResponseDto(
        String message,
        String detailedMassage,
        LocalDate errortime
) {
}

