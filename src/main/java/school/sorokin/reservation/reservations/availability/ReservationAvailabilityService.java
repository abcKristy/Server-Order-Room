package school.sorokin.reservation.reservations.availability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import school.sorokin.reservation.reservations.ReservationController;
import school.sorokin.reservation.reservations.ReservationRepository;
import school.sorokin.reservation.reservations.ReservationStatus;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReservationAvailabilityService {
    private final ReservationRepository repository;
    private static final Logger log = LoggerFactory.getLogger(ReservationController.class);
    public ReservationAvailabilityService(ReservationRepository repository) {
        this.repository = repository;
    }
    public boolean isReservationAvailable(
            Long roomId,
            LocalDate startDate,
            LocalDate endDate
    ){
        if(!endDate.isAfter(startDate)){
            throw new IllegalArgumentException("start should to be before end");
        }
        List<Long> conflictingIds = repository.findConflictReservationIds(
                roomId,
                startDate,
                endDate,
                ReservationStatus.APPROVED
        );
        if(conflictingIds.isEmpty()) return true;
        log.info("conflict with ids = {}",conflictingIds);
        return false;
    }
}
