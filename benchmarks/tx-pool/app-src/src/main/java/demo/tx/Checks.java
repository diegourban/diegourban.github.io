package demo.tx;

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

		private final Downstream downstream;

		Inner(Downstream downstream) {
			this.downstream = downstream;
		}

		@Transactional(propagation = Propagation.NOT_SUPPORTED)
		int notSupportedCall() {
			return downstream.probe();
		}
	}
}
