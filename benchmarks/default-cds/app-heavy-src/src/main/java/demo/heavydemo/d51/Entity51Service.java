package demo.heavydemo.d51;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity51Service {
    private final Entity51Repository repo;
    Entity51Service(Entity51Repository repo) { this.repo = repo; }
    @Cacheable("entity51") public List<Entity51> search(String q) { return repo.search(q); }
    public List<Entity51> all() { return repo.findAll(); }
    @Transactional public Entity51 create(Entity51 e) { return repo.save(e); }
}
