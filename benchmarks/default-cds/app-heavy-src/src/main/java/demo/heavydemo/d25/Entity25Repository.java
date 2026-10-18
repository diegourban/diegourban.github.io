package demo.heavydemo.d25;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity25Repository extends JpaRepository<Entity25, Long> {
    Optional<Entity25> findByName(String name);
    Page<Entity25> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity25> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity25 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity25> search(String q);
}
