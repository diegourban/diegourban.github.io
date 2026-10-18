package demo.heavydemo.d37;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity37Service {
    private final Entity37Repository repo;
    Entity37Service(Entity37Repository repo) { this.repo = repo; }
    @Cacheable("entity37") public List<Entity37> search(String q) { return repo.search(q); }
    public List<Entity37> all() { return repo.findAll(); }
    @Transactional public Entity37 create(Entity37 e) { return repo.save(e); }
}
