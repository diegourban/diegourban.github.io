package demo.heavydemo.d41;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity41Repository extends JpaRepository<Entity41, Long> {
    Optional<Entity41> findByName(String name);
    Page<Entity41> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity41> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity41 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity41> search(String q);
}
