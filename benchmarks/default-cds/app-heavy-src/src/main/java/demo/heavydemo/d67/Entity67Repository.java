package demo.heavydemo.d67;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity67Repository extends JpaRepository<Entity67, Long> {
    Optional<Entity67> findByName(String name);
    Page<Entity67> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity67> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity67 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity67> search(String q);
}
