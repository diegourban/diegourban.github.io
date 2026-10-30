package demo.tx;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The method under test: transactional, but only one branch touches the database. */
@Service
public class MixedService {

	private final ItemRepository repo;

	private final Downstream downstream;

	MixedService(ItemRepository repo, Downstream downstream) {
		this.repo = repo;
		this.downstream = downstream;
	}

	@Transactional
	public String handle(String route) {
		if ("db".equals(route)) {
			return "db:" + repo.save(new Item("x")).getId();
		}
		return "downstream:" + downstream.call();
	}
}
