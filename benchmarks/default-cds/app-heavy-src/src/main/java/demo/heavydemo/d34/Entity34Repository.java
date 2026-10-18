package demo.heavydemo.d34;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity34Repository extends JpaRepository<Entity34, Long> {
    Optional<Entity34> findByName(String name);
    Page<Entity34> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity34> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity34 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity34> search(String q);
}
