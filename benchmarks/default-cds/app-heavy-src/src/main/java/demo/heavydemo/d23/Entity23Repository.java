package demo.heavydemo.d23;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity23Repository extends JpaRepository<Entity23, Long> {
    Optional<Entity23> findByName(String name);
    Page<Entity23> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity23> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity23 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity23> search(String q);
}
