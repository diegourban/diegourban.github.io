package demo.heavydemo.d31;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity31Service {
    private final Entity31Repository repo;
    Entity31Service(Entity31Repository repo) { this.repo = repo; }
    @Cacheable("entity31") public List<Entity31> search(String q) { return repo.search(q); }
    public List<Entity31> all() { return repo.findAll(); }
    @Transactional public Entity31 create(Entity31 e) { return repo.save(e); }
}
