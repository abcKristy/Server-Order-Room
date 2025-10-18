package school.sorokin.reservation.reservations;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ReservationService {
    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);
    private final ReservationRerository repositiry;

    public ReservationService(ReservationRerository repositiry) {
        this.repositiry = repositiry;
    }

    public Reservation getReservationById(Long id){
        ReservationEntity reservationEntity = repositiry.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Not found reservation by id = "+id
        ));
        return toDomainReservation(reservationEntity);
    }

    public List<Reservation> findAllReservations() {
        List<ReservationEntity> allEnitities = repositiry.findAll();
        return allEnitities.stream().map
                (this::toDomainReservation).toList();
    }

    public Reservation createReservation(Reservation reservationToCreate) {
        if(reservationToCreate.status()!=null){
            throw new IllegalArgumentException("Status should be empty ");
        }
        if(!reservationToCreate.endDate().isAfter(reservationToCreate.startDate())){
            throw new IllegalArgumentException("start should to be before end");
        }
        var entityToSave = new ReservationEntity(
                null,
                reservationToCreate.userId(),
                reservationToCreate.roomId(),
                reservationToCreate.startDate(),
                reservationToCreate.endDate(),
                ReservationStatus.PENDING
        );
        var savedEntity = repositiry.save(entityToSave);
        return toDomainReservation(savedEntity);
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
        var isConflict = isReservationConflict(reservationEntity);
        if(isConflict)
            throw new IllegalArgumentException("cannot approve because of conflict ");

        var reservationToSave = new ReservationEntity(
                reservationEntity.getId(),
                reservationToUpdate.userId(),
                reservationToUpdate.roomId(),
                reservationToUpdate.startDate(),
                reservationToUpdate.endDate(),
                ReservationStatus.PENDING
        );
        var updatedReservation = repositiry.save(reservationToSave);
        return toDomainReservation(updatedReservation);
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

        var isConflict = isReservationConflict(reservationEntity);
        if(isConflict)
            throw new IllegalArgumentException("cannot approve because of conflict ");


        reservationEntity.setStatus(ReservationStatus.APPROVED);
        repositiry.save(reservationEntity);

        return toDomainReservation(reservationEntity);
    }

    private boolean isReservationConflict(
            ReservationEntity reservation
    ){
        var allReservations = repositiry.findAll();

        for (ReservationEntity existingReservation: allReservations){
            if(reservation.getId().equals(existingReservation.getId())){
                continue;
            }
            if(!reservation.getRoomId().equals(existingReservation.getRoomId())){
                continue;
            }
            if(!existingReservation.getStatus().equals(ReservationStatus.APPROVED)){
                continue;
            }
            if(reservation.getStartDate().isBefore(existingReservation.getEndDate())
                    && existingReservation.getStartDate().isBefore(reservation.getEndDate())){
                return true;
            }
        }
        return false;
    }

    private Reservation toDomainReservation(ReservationEntity reservation){
        return new Reservation(
                reservation.getId(),
                reservation.getUserId(),
                reservation.getRoomId(),
                reservation.getStartDate(),
                reservation.getEndDate(),
                reservation.getStatus());
    }
}
