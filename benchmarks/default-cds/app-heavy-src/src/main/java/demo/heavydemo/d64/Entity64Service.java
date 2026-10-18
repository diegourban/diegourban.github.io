package demo.heavydemo.d64;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity64Service {
    private final Entity64Repository repo;
    Entity64Service(Entity64Repository repo) { this.repo = repo; }
    @Cacheable("entity64") public List<Entity64> search(String q) { return repo.search(q); }
    public List<Entity64> all() { return repo.findAll(); }
    @Transactional public Entity64 create(Entity64 e) { return repo.save(e); }
}
