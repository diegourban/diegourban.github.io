package demo.tx;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single-run checks. Each returns the number of pool connections checked out while the slow call runs,
 * on an otherwise idle app, so 0 means the method holds none and 1 means it holds one.
 */
@Service
public class Checks {

	private final ItemRepository repo;

	private final Downstream downstream;

	private final Inner inner;

	@PersistenceContext
	private EntityManager em;

	private final AtomicInteger downstreamCalls = new AtomicInteger();

	Checks(ItemRepository repo, Downstream downstream, Inner inner) {
		this.repo = repo;
		this.downstream = downstream;
		this.inner = inner;
	}

	public int none() {
		return downstream.probe();
	}

	@Transactional
	public int required() {
		return downstream.probe();
	}

	@Transactional(readOnly = true)
	public int readOnly() {
		return downstream.probe();
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public int notSupported() {
		return downstream.probe();
	}

	@Transactional(propagation = Propagation.NEVER)
	public int never() {
		return downstream.probe();
	}

	@Transactional(propagation = Propagation.SUPPORTS)
	public int supports() {
		return downstream.probe();
	}

	@Transactional
	public int queryThenCall() {
		repo.count();
		return downstream.probe();
	}

	@Transactional
	public int callThenQuery() {
		int active = downstream.probe();
		repo.count();
		return active;
	}

	@Transactional(propagation = Propagation.SUPPORTS)
	public int supportsQueryThenCall() {
		repo.count();
		return downstream.probe();
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public int notSupportedQueryThenCall() {
		repo.count();
		return downstream.probe();
	}

	public int noneQueryThenCall() {
		repo.count();
		return downstream.probe();
	}

	@Transactional
	public int suspended() {
		repo.count();
		return inner.notSupportedCall();
	}

	// The repository checks above go through Spring Data's SimpleJpaRepository, which is itself
	// @Transactional(readOnly = true): outside a transaction each repo call runs and commits its own.
	// These run a plain EntityManager query instead, so only the method's own propagation applies.

	@Transactional(propagation = Propagation.SUPPORTS)
	public int emSupportsQueryThenCall() {
		emCount();
		return downstream.probe();
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public int emNotSupportedQueryThenCall() {
		emCount();
		return downstream.probe();
	}

	public int emNoneQueryThenCall() {
		emCount();
		return downstream.probe();
	}

	@Transactional
	public int requiresNewInsideQueried() {
		repo.count();
		return inner.requiresNewQueryThenCall();
	}

	private void emCount() {
		em.createQuery("select count(i) from Item i").getSingleResult();
	}

	/** Holds one connection for ms milliseconds, for the contention check with a pool of 1. */
	@Transactional
	public int hold(long ms) {
		repo.count();
		try {
			Thread.sleep(ms);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		return 1;
	}

	/** The slow call first, then the write: with the pool exhausted, does the call still happen? */
	@Transactional
	public int callThenSave() {
		downstreamCalls.incrementAndGet();
		downstream.call();
		repo.save(new Item("x"));
		return 0;
	}

	public int downstreamCalls() {
		return downstreamCalls.get();
	}

	@Transactional(transactionManager = "jdbcTx")
	public int jdbcRequired() {
		return downstream.probe();
	}

	@Transactional(transactionManager = "jdbcTx", readOnly = true)
	public int jdbcReadOnly() {
		return downstream.probe();
	}

	/** A separate bean, so the call goes through the transaction proxy. */
	@Service
	static class Inner {

		private final ItemRepository repo;

		private final Downstream downstream;

		Inner(ItemRepository repo, Downstream downstream) {
			this.repo = repo;
			this.downstream = downstream;
		}

		@Transactional(propagation = Propagation.NOT_SUPPORTED)
		int notSupportedCall() {
			return downstream.probe();
		}

		@Transactional(propagation = Propagation.REQUIRES_NEW)
		int requiresNewQueryThenCall() {
			repo.count();
			return downstream.probe();
		}
	}
}
