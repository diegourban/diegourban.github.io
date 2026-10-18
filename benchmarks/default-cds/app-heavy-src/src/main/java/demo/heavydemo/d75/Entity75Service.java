package demo.heavydemo.d75;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity75Service {
    private final Entity75Repository repo;
    Entity75Service(Entity75Repository repo) { this.repo = repo; }
    @Cacheable("entity75") public List<Entity75> search(String q) { return repo.search(q); }
    public List<Entity75> all() { return repo.findAll(); }
    @Transactional public Entity75 create(Entity75 e) { return repo.save(e); }
}
