package demo.heavydemo.d79;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity79Repository extends JpaRepository<Entity79, Long> {
    Optional<Entity79> findByName(String name);
    Page<Entity79> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity79> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity79 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity79> search(String q);
}
