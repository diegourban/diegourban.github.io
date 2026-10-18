package demo.heavydemo.d02;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity02Repository extends JpaRepository<Entity02, Long> {
    Optional<Entity02> findByName(String name);
    Page<Entity02> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity02> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity02 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity02> search(String q);
}
