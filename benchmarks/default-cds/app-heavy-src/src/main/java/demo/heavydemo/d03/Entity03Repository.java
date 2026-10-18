package demo.heavydemo.d03;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity03Repository extends JpaRepository<Entity03, Long> {
    Optional<Entity03> findByName(String name);
    Page<Entity03> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity03> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity03 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity03> search(String q);
}
