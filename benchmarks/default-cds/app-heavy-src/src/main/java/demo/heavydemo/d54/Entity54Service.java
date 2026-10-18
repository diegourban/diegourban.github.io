package demo.heavydemo.d54;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity54Service {
    private final Entity54Repository repo;
    Entity54Service(Entity54Repository repo) { this.repo = repo; }
    @Cacheable("entity54") public List<Entity54> search(String q) { return repo.search(q); }
    public List<Entity54> all() { return repo.findAll(); }
    @Transactional public Entity54 create(Entity54 e) { return repo.save(e); }
}
