package demo.tx;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** The slow downstream service (WireMock with a fixed delay), plus a probe of the pool for the single-run checks. */
@Component
public class Downstream {

	private final RestClient client;

	private final DataSource dataSource;

	Downstream(RestClient.Builder builder, @Value("${downstream.url}") String url, DataSource dataSource) {
		this.client = builder.baseUrl(url).build();
		this.dataSource = dataSource;
	}

	public String call() {
		return client.get().retrieve().body(String.class);
	}

	/** Connections checked out of the pool right now, read just before the slow call. */
	public int probe() {
		int active = activeConnections();
		call();
		return active;
	}

	public int activeConnections() {
		try {
			// Unwrap works through the lazy proxy, which is a DelegatingDataSource.
			return dataSource.unwrap(HikariDataSource.class).getHikariPoolMXBean().getActiveConnections();
		}
		catch (java.sql.SQLException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
