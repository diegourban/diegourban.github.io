package demo.heavydemo.d78;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity78Repository extends JpaRepository<Entity78, Long> {
    Optional<Entity78> findByName(String name);
    Page<Entity78> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity78> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity78 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity78> search(String q);
}
