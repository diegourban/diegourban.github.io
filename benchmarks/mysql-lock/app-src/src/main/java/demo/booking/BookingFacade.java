package demo.booking;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** A caller that opens the transaction and reads before it calls the writer, as a service layer often does. */
@Component
public class BookingFacade {
    private final CustomerRepository customers;
    private final BookingWriter writer;

    BookingFacade(CustomerRepository customers, BookingWriter writer) {
        this.customers = customers;
        this.writer = writer;
    }

    /** The writer locks first, but it joins this transaction, whose snapshot the customer read has already fixed. */
    @Transactional
    public boolean outerRead(long eventId, long customerId) {
        customers.findById(customerId).orElseThrow();
        return writer.lockFirst(eventId, customerId);
    }

    /** The READ COMMITTED writer called from inside this transaction: does its isolation still apply? */
    @Transactional
    public boolean outerReadThenReadCommitted(long eventId, long customerId) {
        customers.findById(customerId).orElseThrow();
        return writer.readCommitted(eventId, customerId);
    }

    /** The same, but the writer runs in a new transaction with its own snapshot. */
    @Transactional
    public boolean outerReadNewTransaction(long eventId, long customerId) {
        customers.findById(customerId).orElseThrow();
        return writer.lockFirstNewTransaction(eventId, customerId);
    }
}
