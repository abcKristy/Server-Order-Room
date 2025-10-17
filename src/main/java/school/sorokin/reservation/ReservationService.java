package school.sorokin.reservation;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ReservationService {

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
        if(reservationToCreate.id()!=null){
            throw new IllegalArgumentException("Id should be empty ");
        }
        if(reservationToCreate.status()!=null){
            throw new IllegalArgumentException("Status should be empty ");
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

    public void deleteReservation(Long id) {
        if(!repositiry.existsById(id))
            throw new NoSuchElementException("Not found reservation by id = "+id);
        repositiry.deleteById(id);
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
