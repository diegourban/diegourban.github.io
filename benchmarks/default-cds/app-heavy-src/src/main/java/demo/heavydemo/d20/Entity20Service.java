package demo.heavydemo.d20;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity20Service {
    private final Entity20Repository repo;
    Entity20Service(Entity20Repository repo) { this.repo = repo; }
    @Cacheable("entity20") public List<Entity20> search(String q) { return repo.search(q); }
    public List<Entity20> all() { return repo.findAll(); }
    @Transactional public Entity20 create(Entity20 e) { return repo.save(e); }
}
