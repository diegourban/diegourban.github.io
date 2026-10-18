package demo.heavydemo.d40;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity40Repository extends JpaRepository<Entity40, Long> {
    Optional<Entity40> findByName(String name);
    Page<Entity40> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity40> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity40 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity40> search(String q);
}
