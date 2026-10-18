package demo.heavydemo.d63;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity63Service {
    private final Entity63Repository repo;
    Entity63Service(Entity63Repository repo) { this.repo = repo; }
    @Cacheable("entity63") public List<Entity63> search(String q) { return repo.search(q); }
    public List<Entity63> all() { return repo.findAll(); }
    @Transactional public Entity63 create(Entity63 e) { return repo.save(e); }
}
