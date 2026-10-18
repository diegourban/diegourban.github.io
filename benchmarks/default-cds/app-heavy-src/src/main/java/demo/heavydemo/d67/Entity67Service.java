package demo.heavydemo.d67;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity67Service {
    private final Entity67Repository repo;
    Entity67Service(Entity67Repository repo) { this.repo = repo; }
    @Cacheable("entity67") public List<Entity67> search(String q) { return repo.search(q); }
    public List<Entity67> all() { return repo.findAll(); }
    @Transactional public Entity67 create(Entity67 e) { return repo.save(e); }
}
