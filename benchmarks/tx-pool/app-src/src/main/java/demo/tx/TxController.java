package demo.tx;

import java.util.Map;
import java.util.function.IntSupplier;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TxController {

	private final MixedService mixed;

	private final NarrowedService narrowed;

	private final Checks c;

	private final Map<String, IntSupplier> checks;

	TxController(MixedService mixed, NarrowedService narrowed, Checks c) {
		this.mixed = mixed;
		this.narrowed = narrowed;
		this.checks = Map.ofEntries(
				Map.entry("none", c::none), Map.entry("required", c::required), Map.entry("readOnly", c::readOnly),
				Map.entry("notSupported", c::notSupported), Map.entry("never", c::never), Map.entry("supports", c::supports),
				Map.entry("queryThenCall", c::queryThenCall), Map.entry("callThenQuery", c::callThenQuery),
				Map.entry("supportsQueryThenCall", c::supportsQueryThenCall),
				Map.entry("notSupportedQueryThenCall", c::notSupportedQueryThenCall),
				Map.entry("noneQueryThenCall", c::noneQueryThenCall), Map.entry("suspended", c::suspended),
				Map.entry("jdbcRequired", c::jdbcRequired), Map.entry("jdbcReadOnly", c::jdbcReadOnly),
				Map.entry("emSupportsQueryThenCall", c::emSupportsQueryThenCall),
				Map.entry("emNotSupportedQueryThenCall", c::emNotSupportedQueryThenCall),
				Map.entry("emNoneQueryThenCall", c::emNoneQueryThenCall),
				Map.entry("requiresNewInsideQueried", c::requiresNewInsideQueried));
		this.c = c;
	}

	@GetMapping("/hold")
	int hold(@RequestParam long ms) {
		return c.hold(ms);
	}

	/** Runs callThenSave and reports where it failed: before or after the downstream call. */
	@GetMapping("/callThenSave")
	Map<String, Object> callThenSave() {
		int before = c.downstreamCalls();
		String outcome;
		try {
			c.callThenSave();
			outcome = "ok";
		}
		catch (RuntimeException ex) {
			outcome = ex.getClass().getSimpleName();
		}
		return Map.of("outcome", outcome, "downstreamCalled", c.downstreamCalls() > before);
	}

	@PostMapping("/mixed")
	String mixed(@RequestParam String route) {
		return mixed.handle(route);
	}

	@PostMapping("/narrowed")
	String narrowed(@RequestParam String route) {
		return narrowed.handle(route);
	}

	@GetMapping("/check/{name}")
	Map<String, Object> check(@PathVariable String name) {
		return Map.of("check", name, "activeDuringCall", checks.get(name).getAsInt());
	}
}
