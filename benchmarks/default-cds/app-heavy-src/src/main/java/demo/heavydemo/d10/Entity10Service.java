package demo.heavydemo.d10;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity10Service {
    private final Entity10Repository repo;
    Entity10Service(Entity10Repository repo) { this.repo = repo; }
    @Cacheable("entity10") public List<Entity10> search(String q) { return repo.search(q); }
    public List<Entity10> all() { return repo.findAll(); }
    @Transactional public Entity10 create(Entity10 e) { return repo.save(e); }
}
