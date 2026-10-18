package demo.heavydemo.d29;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity29Service {
    private final Entity29Repository repo;
    Entity29Service(Entity29Repository repo) { this.repo = repo; }
    @Cacheable("entity29") public List<Entity29> search(String q) { return repo.search(q); }
    public List<Entity29> all() { return repo.findAll(); }
    @Transactional public Entity29 create(Entity29 e) { return repo.save(e); }
}
