package demo.booking;

import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ways to book one seat for an event with a fixed capacity. Each method returns true when it booked the seat and false
 * when the event was full. They differ only in the order of their reads and in how they check the capacity.
 */
@Component
public class BookingWriter {
    private final CustomerRepository customers;
    private final EventRepository events;
    private final BookingRepository bookings;

    BookingWriter(CustomerRepository customers, EventRepository events, BookingRepository bookings) {
        this.customers = customers;
        this.events = events;
        this.bookings = bookings;
    }

    /** The bug: a plain read of the customer comes first, so the snapshot is fixed before the lock is taken. */
    @Transactional
    public boolean plainReadFirst(long eventId, long customerId) {
        Customer customer = customers.findById(customerId).orElseThrow();
        Hooks.at("beforeLock");
        Event event = events.lockById(eventId);
        if (bookings.countByEventId(eventId) >= event.capacity) {
            return false;
        }
        bookings.save(new Booking(event, customer));
        Hooks.at("beforeCommit");
        return true;
    }

    /** Fix 1: the locking read is the transaction's first read. */
    @Transactional
    public boolean lockFirst(long eventId, long customerId) {
        return lockThenCount(eventId, customerId);
    }

    /** Fix 1 in its own transaction, for a caller that has already read inside an outer one. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean lockFirstNewTransaction(long eventId, long customerId) {
        return lockThenCount(eventId, customerId);
    }

    private boolean lockThenCount(long eventId, long customerId) {
        Hooks.at("beforeLock");
        Event event = events.lockById(eventId);
        Customer customer = customers.findById(customerId).orElseThrow();
        if (bookings.countByEventId(eventId) >= event.capacity) {
            return false;
        }
        bookings.save(new Booking(event, customer));
        Hooks.at("beforeCommit");
        return true;
    }

    /** Fix 2: the same code as the bug, but every statement reads the latest committed data. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean readCommitted(long eventId, long customerId) {
        Customer customer = customers.findById(customerId).orElseThrow();
        Hooks.at("beforeLock");
        Event event = events.lockById(eventId);
        if (bookings.countByEventId(eventId) >= event.capacity) {
            return false;
        }
        bookings.save(new Booking(event, customer));
        Hooks.at("beforeCommit");
        return true;
    }

    /** Fix 3: the count lives on the locked row, and a locking read always returns the row's latest version. */
    @Transactional
    public boolean counter(long eventId, long customerId) {
        Customer customer = customers.findById(customerId).orElseThrow();
        Hooks.at("beforeLock");
        Event event = events.lockById(eventId);
        if (event.seatsTaken >= event.capacity) {
            return false;
        }
        event.seatsTaken++;
        bookings.save(new Booking(event, customer));
        Hooks.at("beforeCommit");
        return true;
    }

    /** The second trap: the capacity check moved into the locking statement, after a plain read. */
    @Transactional
    public boolean subquery(long eventId, long customerId) {
        Customer customer = customers.findById(customerId).orElseThrow();
        Hooks.at("beforeLock");
        return subqueryBooking(eventId, customer);
    }

    /** The same locking statement as the transaction's first read. */
    @Transactional
    public boolean subqueryFirst(long eventId, long customerId) {
        Hooks.at("beforeLock");
        return subqueryBooking(eventId, customers.getReferenceById(customerId));
    }

    private boolean subqueryBooking(long eventId, Customer customer) {
        Optional<Event> event = events.lockIfSeatsLeft(eventId);
        if (event.isEmpty()) {
            return false;
        }
        bookings.save(new Booking(event.get(), customer));
        Hooks.at("beforeCommit");
        return true;
    }

    /** Reference: no lock at all. */
    @Transactional
    public boolean noLock(long eventId, long customerId) {
        Customer customer = customers.findById(customerId).orElseThrow();
        Hooks.at("beforeLock");
        Event event = events.findById(eventId).orElseThrow();
        if (bookings.countByEventId(eventId) >= event.capacity) {
            return false;
        }
        bookings.save(new Booking(event, customer));
        Hooks.at("beforeCommit");
        return true;
    }

    /** Hidden read candidate: existsById before the lock. */
    @Transactional
    public boolean existsFirst(long eventId, long customerId) {
        if (!customers.existsById(customerId)) {
            return false;
        }
        Hooks.at("beforeLock");
        Event event = events.lockById(eventId);
        if (bookings.countByEventId(eventId) >= event.capacity) {
            return false;
        }
        bookings.save(new Booking(event, customers.getReferenceById(customerId)));
        Hooks.at("beforeCommit");
        return true;
    }

    /** Hidden read candidate: getReferenceById before the lock (a proxy, so no SELECT is expected). */
    @Transactional
    public boolean referenceFirst(long eventId, long customerId) {
        Customer customer = customers.getReferenceById(customerId);
        Hooks.at("beforeLock");
        Event event = events.lockById(eventId);
        if (bookings.countByEventId(eventId) >= event.capacity) {
            return false;
        }
        bookings.save(new Booking(event, customer));
        Hooks.at("beforeCommit");
        return true;
    }

    /** Hidden read candidate: the counter fix, but the event was loaded with a plain read earlier in the transaction. */
    @Transactional
    public boolean counterEventLoadedFirst(long eventId, long customerId) {
        events.findById(eventId).orElseThrow();
        Customer customer = customers.findById(customerId).orElseThrow();
        Hooks.at("beforeLock");
        Event event = events.lockById(eventId);
        if (event.seatsTaken >= event.capacity) {
            return false;
        }
        event.seatsTaken++;
        bookings.save(new Booking(event, customer));
        Hooks.at("beforeCommit");
        return true;
    }
}
