package demo.heavydemo.d43;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity43Service {
    private final Entity43Repository repo;
    Entity43Service(Entity43Repository repo) { this.repo = repo; }
    @Cacheable("entity43") public List<Entity43> search(String q) { return repo.search(q); }
    public List<Entity43> all() { return repo.findAll(); }
    @Transactional public Entity43 create(Entity43 e) { return repo.save(e); }
}
