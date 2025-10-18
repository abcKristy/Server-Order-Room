package school.sorokin.reservation.reservations;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import school.sorokin.reservation.reservations.availability.ReservationAvailabilityService;

import java.util.*;

@Service
public class ReservationService {
    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);
    private final ReservationRepository repository;
    private final ReservationMapper mapper;
    private final ReservationAvailabilityService availabilityService;

    public ReservationService(ReservationRepository repositiry, ReservationMapper mapper, ReservationAvailabilityService availabilityService) {
        this.repository = repositiry;
        this.mapper = mapper;
        this.availabilityService = availabilityService;
    }

    public Reservation getReservationById(Long id){
        ReservationEntity reservationEntity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Not found reservation by id = "+id
        ));
        return mapper.toDomain(reservationEntity);
    }

    public List<Reservation> searchAllByFilter(
            ReservationSearchFilter filter
    ) {
        int pageSize = filter.pageSize() != null
                ? filter.pageSize() : 10;
        int pageNumber = filter.pageNumber() != null
                ? filter.pageNumber() : 0;
        var pageable = Pageable
                .ofSize(pageSize)
                .withPage(pageNumber);

        List<ReservationEntity> allEntities = repository.searchAllByFilter(
                filter.roomId(),
                filter.userId(),
                pageable
        );
        return allEntities.stream().map
                (mapper::toDomain).toList();
    }

    public Reservation createReservation(Reservation reservationToCreate) {
        if(reservationToCreate.status()!=null){
            throw new IllegalArgumentException("Status should be empty ");
        }
        if(!reservationToCreate.endDate().isAfter(reservationToCreate.startDate())){
            throw new IllegalArgumentException("start should to be before end");
        }

        var entityToSave = mapper.toEntity(reservationToCreate);
        entityToSave.setStatus(ReservationStatus.PENDING);

                var savedEntity = repository.save(entityToSave);
        return mapper.toDomain(savedEntity);
    }

    public Reservation updateReservation(Long id, Reservation reservationToUpdate) {
        var reservationEntity = repository.findById(id)
                .orElseThrow(()->new NoSuchElementException("Not found reservation by id = "+id));
        if(reservationEntity.getStatus()!=ReservationStatus.PENDING){
            throw new IllegalArgumentException("cannot modify reservation status " + reservationEntity.getStatus());
        }
        if(!reservationToUpdate.endDate().isAfter(reservationToUpdate.startDate())){
            throw new IllegalArgumentException("start should to be before end");
        }
        var isAvailableToApprove = availabilityService.isReservationAvailable(
                reservationEntity.getRoomId(),
                reservationEntity.getStartDate(),
                reservationEntity.getEndDate()
        );
        if(!isAvailableToApprove)
            throw new IllegalArgumentException("cannot approve because of conflict ");

        var reservationToSave = mapper.toEntity(reservationToUpdate);
        reservationToSave.setId(reservationEntity.getId());
        reservationToSave.setStatus(ReservationStatus.PENDING);

        var updatedReservation = repository.save(reservationToSave);
        return mapper.toDomain(updatedReservation);
    }

    @Transactional
    public void cancelReservation(Long id) {
        var reservation = repository.findById(id)
                        .orElseThrow(()-> new EntityNotFoundException("Not found reservation by id = "+id));
        if(reservation.getStatus().equals(ReservationStatus.APPROVED)){
            throw new IllegalStateException("can not canceled reservation without manager");
        }
        if(reservation.getStatus().equals(ReservationStatus.CANCELED)){
            throw new IllegalStateException("can not cancel reservation, it was already cancelled");
        }
        repository.setStatus(id, ReservationStatus.CANCELED);
        log.info("successfully canceled reservation by id "+ id);
    }

    public Reservation approveReservation(Long id) {
        var reservationEntity = repository.findById(id)
                .orElseThrow(()->new NoSuchElementException("Not found reservation by id = "+id));

        if(reservationEntity.getStatus()!=ReservationStatus.PENDING){
            throw new IllegalArgumentException("cannot approve reservation status " + reservationEntity.getStatus());
        }

        var isAvailableToApprove = availabilityService.isReservationAvailable(
                reservationEntity.getRoomId(),
                reservationEntity.getStartDate(),
                reservationEntity.getEndDate()
        );
        if(!isAvailableToApprove)
            throw new IllegalArgumentException("cannot approve because of conflict ");


        reservationEntity.setStatus(ReservationStatus.APPROVED);
        repository.save(reservationEntity);

        return mapper.toDomain(reservationEntity);
    }
}
