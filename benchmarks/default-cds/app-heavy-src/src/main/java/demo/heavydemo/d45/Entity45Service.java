package demo.heavydemo.d45;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity45Service {
    private final Entity45Repository repo;
    Entity45Service(Entity45Repository repo) { this.repo = repo; }
    @Cacheable("entity45") public List<Entity45> search(String q) { return repo.search(q); }
    public List<Entity45> all() { return repo.findAll(); }
    @Transactional public Entity45 create(Entity45 e) { return repo.save(e); }
}
