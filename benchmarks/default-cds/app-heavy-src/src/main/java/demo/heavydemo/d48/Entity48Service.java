package demo.heavydemo.d48;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity48Service {
    private final Entity48Repository repo;
    Entity48Service(Entity48Repository repo) { this.repo = repo; }
    @Cacheable("entity48") public List<Entity48> search(String q) { return repo.search(q); }
    public List<Entity48> all() { return repo.findAll(); }
    @Transactional public Entity48 create(Entity48 e) { return repo.save(e); }
}
