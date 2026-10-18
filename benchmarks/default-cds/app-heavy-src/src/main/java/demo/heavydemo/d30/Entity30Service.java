package demo.heavydemo.d30;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity30Service {
    private final Entity30Repository repo;
    Entity30Service(Entity30Repository repo) { this.repo = repo; }
    @Cacheable("entity30") public List<Entity30> search(String q) { return repo.search(q); }
    public List<Entity30> all() { return repo.findAll(); }
    @Transactional public Entity30 create(Entity30 e) { return repo.save(e); }
}
