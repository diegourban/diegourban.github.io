package demo.heavydemo.d27;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity27Service {
    private final Entity27Repository repo;
    Entity27Service(Entity27Repository repo) { this.repo = repo; }
    @Cacheable("entity27") public List<Entity27> search(String q) { return repo.search(q); }
    public List<Entity27> all() { return repo.findAll(); }
    @Transactional public Entity27 create(Entity27 e) { return repo.save(e); }
}
