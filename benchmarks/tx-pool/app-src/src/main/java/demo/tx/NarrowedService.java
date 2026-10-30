package demo.tx;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The reference refactor: no transaction at the top, one around the database branch only. */
@Service
public class NarrowedService {

	private final Writer writer;

	private final Downstream downstream;

	NarrowedService(Writer writer, Downstream downstream) {
		this.writer = writer;
		this.downstream = downstream;
	}

	public String handle(String route) {
		if ("db".equals(route)) {
			return "db:" + writer.write();
		}
		return "downstream:" + downstream.call();
	}

	@Service
	static class Writer {

		private final ItemRepository repo;

		Writer(ItemRepository repo) {
			this.repo = repo;
		}

		@Transactional
		Long write() {
			return repo.save(new Item("x")).getId();
		}
	}
}
