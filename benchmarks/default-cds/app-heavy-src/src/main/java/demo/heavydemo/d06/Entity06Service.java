package demo.heavydemo.d06;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity06Service {
    private final Entity06Repository repo;
    Entity06Service(Entity06Repository repo) { this.repo = repo; }
    @Cacheable("entity06") public List<Entity06> search(String q) { return repo.search(q); }
    public List<Entity06> all() { return repo.findAll(); }
    @Transactional public Entity06 create(Entity06 e) { return repo.save(e); }
}
