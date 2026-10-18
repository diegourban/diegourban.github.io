package demo.heavydemo.d28;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity28Repository extends JpaRepository<Entity28, Long> {
    Optional<Entity28> findByName(String name);
    Page<Entity28> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity28> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity28 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity28> search(String q);
}
