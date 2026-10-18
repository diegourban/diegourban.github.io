package demo.heavydemo.d60;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity60Repository extends JpaRepository<Entity60, Long> {
    Optional<Entity60> findByName(String name);
    Page<Entity60> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity60> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity60 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity60> search(String q);
}
