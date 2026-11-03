package demo.tx;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;

@SpringBootApplication
public class TxdemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(TxdemoApplication.class, args);
	}

	// Defining any transaction manager switches off Boot's JPA one, so both are declared here:
	// JPA for everything, plain JDBC only for the one check that compares the two.
	@Bean
	@Primary
	JpaTransactionManager transactionManager(EntityManagerFactory emf) {
		return new JpaTransactionManager(emf);
	}

	@Bean
	JdbcTransactionManager jdbcTx(DataSource dataSource) {
		return new JdbcTransactionManager(dataSource);
	}
}
