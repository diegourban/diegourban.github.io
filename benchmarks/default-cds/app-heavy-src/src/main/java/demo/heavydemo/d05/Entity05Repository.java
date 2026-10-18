package demo.heavydemo.d05;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity05Repository extends JpaRepository<Entity05, Long> {
    Optional<Entity05> findByName(String name);
    Page<Entity05> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity05> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity05 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity05> search(String q);
}
