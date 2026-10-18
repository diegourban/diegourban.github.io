package demo.heavydemo.d53;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity53Repository extends JpaRepository<Entity53, Long> {
    Optional<Entity53> findByName(String name);
    Page<Entity53> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity53> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity53 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity53> search(String q);
}
