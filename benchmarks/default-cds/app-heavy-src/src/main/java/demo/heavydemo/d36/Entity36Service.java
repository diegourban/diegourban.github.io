package demo.heavydemo.d36;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity36Service {
    private final Entity36Repository repo;
    Entity36Service(Entity36Repository repo) { this.repo = repo; }
    @Cacheable("entity36") public List<Entity36> search(String q) { return repo.search(q); }
    public List<Entity36> all() { return repo.findAll(); }
    @Transactional public Entity36 create(Entity36 e) { return repo.save(e); }
}
