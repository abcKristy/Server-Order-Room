package school.sorokin.reservation.reservations.availability;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import school.sorokin.reservation.reservations.ReservationController;

@RestController
@RequestMapping("/reservation/availability")
public class ReservationAvailabilityController {
    private final ReservationAvailabilityService service;

    private static final Logger log = LoggerFactory.getLogger(ReservationController.class);

    public ReservationAvailabilityController(ReservationAvailabilityService service) {
        this.service = service;
    }

    @PostMapping("/check")
    public ResponseEntity<CheckAvailabilityResponse> checkAvailability(
            @Valid CheckAvailabilityRequest request
    ){
        log.info("called method checkAvailability request = {}",request);
        boolean isAvailable = service.isReservationAvailable(
                request.roomId(),
                request.startDate(),
                request.endDate()
        );
        var massage = isAvailable
                ? "room available to reservation"
                : "room not available to reservation";
        var status = isAvailable
                ? AvailabilityStatus.AVAILABLE
                : AvailabilityStatus.RESERVED;
        return  ResponseEntity.status(200)
                .body(new CheckAvailabilityResponse(massage,status));

    }

}
