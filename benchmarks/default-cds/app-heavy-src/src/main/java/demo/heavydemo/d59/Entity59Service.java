package demo.heavydemo.d59;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity59Service {
    private final Entity59Repository repo;
    Entity59Service(Entity59Repository repo) { this.repo = repo; }
    @Cacheable("entity59") public List<Entity59> search(String q) { return repo.search(q); }
    public List<Entity59> all() { return repo.findAll(); }
    @Transactional public Entity59 create(Entity59 e) { return repo.save(e); }
}
