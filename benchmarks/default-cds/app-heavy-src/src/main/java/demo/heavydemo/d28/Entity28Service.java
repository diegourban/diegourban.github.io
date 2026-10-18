package demo.heavydemo.d28;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity28Service {
    private final Entity28Repository repo;
    Entity28Service(Entity28Repository repo) { this.repo = repo; }
    @Cacheable("entity28") public List<Entity28> search(String q) { return repo.search(q); }
    public List<Entity28> all() { return repo.findAll(); }
    @Transactional public Entity28 create(Entity28 e) { return repo.save(e); }
}
