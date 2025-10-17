package school.sorokin.reservation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRerository extends JpaRepository<ReservationEntity,Long> {

    List<ReservationEntity> findAllByStatusIs(ReservationStatus status);

}
