package demo.heavydemo.d15;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity15Repository extends JpaRepository<Entity15, Long> {
    Optional<Entity15> findByName(String name);
    Page<Entity15> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity15> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity15 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity15> search(String q);
}
