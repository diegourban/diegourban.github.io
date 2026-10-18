package demo.heavydemo.d23;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity23Service {
    private final Entity23Repository repo;
    Entity23Service(Entity23Repository repo) { this.repo = repo; }
    @Cacheable("entity23") public List<Entity23> search(String q) { return repo.search(q); }
    public List<Entity23> all() { return repo.findAll(); }
    @Transactional public Entity23 create(Entity23 e) { return repo.save(e); }
}
