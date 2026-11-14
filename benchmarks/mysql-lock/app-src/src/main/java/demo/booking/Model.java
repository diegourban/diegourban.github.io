package demo.booking;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** The three tables of the demo: customers book seats for events, and an event has a fixed capacity. */
final class Model {
    private Model() {
    }
}

@Entity
class Customer {
    @Id
    Long id;
    String name;

    protected Customer() {
    }

    Customer(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}

@Entity
@Table(name = "event")
class Event {
    @Id
    Long id;
    int capacity;
    /** Only the counter fix uses this column; the other writers count the bookings. */
    int seatsTaken;

    protected Event() {
    }

    Event(Long id, int capacity) {
        this.id = id;
        this.capacity = capacity;
    }
}

@Entity
@Table(name = "booking", indexes = @Index(columnList = "event_id"))
class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    Event event;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    Customer customer;

    protected Booking() {
    }

    Booking(Event event, Customer customer) {
        this.event = event;
        this.customer = customer;
    }
}
