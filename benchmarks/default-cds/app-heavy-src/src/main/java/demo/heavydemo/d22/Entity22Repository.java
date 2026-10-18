package demo.heavydemo.d22;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Entity22Repository extends JpaRepository<Entity22, Long> {
    Optional<Entity22> findByName(String name);
    Page<Entity22> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Entity22> findByAmountGreaterThanAndEffectiveDateIsNotNull(java.math.BigDecimal amount);
    @Query("select e from Entity22 e where lower(e.name) like lower(concat('%', :q, '%'))")
    List<Entity22> search(String q);
}
