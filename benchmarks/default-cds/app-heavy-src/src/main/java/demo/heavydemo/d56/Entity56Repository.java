package demo.heavydemo.d56;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity56Repository extends JpaRepository<Entity56, Long> {
    Optional<Entity56> findByName(String name);
    Page<Entity56> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity56> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity56 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity56> search(String q);
}
