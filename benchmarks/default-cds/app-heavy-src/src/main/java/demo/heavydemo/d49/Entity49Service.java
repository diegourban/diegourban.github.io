package demo.heavydemo.d49;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity49Service {
    private final Entity49Repository repo;
    Entity49Service(Entity49Repository repo) { this.repo = repo; }
    @Cacheable("entity49") public List<Entity49> search(String q) { return repo.search(q); }
    public List<Entity49> all() { return repo.findAll(); }
    @Transactional public Entity49 create(Entity49 e) { return repo.save(e); }
}
