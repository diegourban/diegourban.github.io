package demo.heavydemo.d29;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity29Repository extends JpaRepository<Entity29, Long> {
    Optional<Entity29> findByName(String name);
    Page<Entity29> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity29> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity29 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity29> search(String q);
}
