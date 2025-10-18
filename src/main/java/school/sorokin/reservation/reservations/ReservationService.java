package school.sorokin.reservation.reservations;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class ReservationService {
    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);
    private final ReservationRerository repositiry;
    private final ReservationMapper mapper;

    public ReservationService(ReservationRerository repositiry, ReservationMapper mapper) {
        this.repositiry = repositiry;
        this.mapper = mapper;
    }

    public Reservation getReservationById(Long id){
        ReservationEntity reservationEntity = repositiry.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Not found reservation by id = "+id
        ));
        return mapper.toDomain(reservationEntity);
    }

    public List<Reservation> findAllReservations() {
        List<ReservationEntity> allEnitities = repositiry.findAll();
        return allEnitities.stream().map
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

                var savedEntity = repositiry.save(entityToSave);
        return mapper.toDomain(savedEntity);
    }

    public Reservation updateReservation(Long id, Reservation reservationToUpdate) {
        var reservationEntity = repositiry.findById(id)
                .orElseThrow(()->new NoSuchElementException("Not found reservation by id = "+id));
        if(reservationEntity.getStatus()!=ReservationStatus.PENDING){
            throw new IllegalArgumentException("cannot modify reservation status " + reservationEntity.getStatus());
        }
        if(!reservationToUpdate.endDate().isAfter(reservationToUpdate.startDate())){
            throw new IllegalArgumentException("start should to be before end");
        }
        var isConflict = isReservationConflict(
                reservationEntity.getRoomId(),
                reservationEntity.getStartDate(),
                reservationEntity.getEndDate()
        );
        if(isConflict)
            throw new IllegalArgumentException("cannot approve because of conflict ");

        var reservationToSave = mapper.toEntity(reservationToUpdate);
        reservationToSave.setId(reservationEntity.getId());
        reservationToSave.setStatus(ReservationStatus.PENDING);

        var updatedReservation = repositiry.save(reservationToSave);
        return mapper.toDomain(updatedReservation);
    }

    @Transactional
    public void cancelReservation(Long id) {
        var reservation = repositiry.findById(id)
                        .orElseThrow(()-> new EntityNotFoundException("Not found reservation by id = "+id));
        if(reservation.getStatus().equals(ReservationStatus.APPROVED)){
            throw new IllegalStateException("can not canceled reservation without manager");
        }
        if(reservation.getStatus().equals(ReservationStatus.CANCELED)){
            throw new IllegalStateException("can not cancel reservation, it was already cancelled");
        }
        repositiry.setStatus(id, ReservationStatus.CANCELED);
        log.info("successfully canceled reservation by id "+ id);
    }

    public Reservation approveReservation(Long id) {
        var reservationEntity = repositiry.findById(id)
                .orElseThrow(()->new NoSuchElementException("Not found reservation by id = "+id));

        if(reservationEntity.getStatus()!=ReservationStatus.PENDING){
            throw new IllegalArgumentException("cannot approve reservation status " + reservationEntity.getStatus());
        }

        var isConflict = isReservationConflict(
                reservationEntity.getRoomId(),
                reservationEntity.getStartDate(),
                reservationEntity.getEndDate()
        );
        if(isConflict)
            throw new IllegalArgumentException("cannot approve because of conflict ");


        reservationEntity.setStatus(ReservationStatus.APPROVED);
        repositiry.save(reservationEntity);

        return mapper.toDomain(reservationEntity);
    }

    private boolean isReservationConflict(
            Long roomId,
            LocalDate startDate,
            LocalDate endDate
    ){
        List<Long> conflictingIds = repositiry.findConflictReservationIds(
                roomId,
                startDate,
                endDate,
                ReservationStatus.APPROVED
        );
        if(conflictingIds.isEmpty()) return false;
        log.info("conflict with ids = ",conflictingIds);
        return true;
    }


}
