package demo.booking;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CustomerRepository extends JpaRepository<Customer, Long> {
}

interface EventRepository extends JpaRepository<Event, Long> {

    /** SELECT ... FOR UPDATE on the event row. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Event lockById(@Param("id") Long id);

    /** The capacity check folded into the locking statement: the event row is locked, the subquery is not. */
    @Query(nativeQuery = true, value = """
            select * from event e
             where e.id = :id
               and (select count(*) from booking b where b.event_id = e.id) < e.capacity
               for update""")
    Optional<Event> lockIfSeatsLeft(@Param("id") Long id);
}

interface BookingRepository extends JpaRepository<Booking, Long> {
    long countByEventId(Long eventId);
}
