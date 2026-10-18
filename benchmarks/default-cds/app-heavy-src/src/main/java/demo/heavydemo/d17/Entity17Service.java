package demo.heavydemo.d17;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity17Service {
    private final Entity17Repository repo;
    Entity17Service(Entity17Repository repo) { this.repo = repo; }
    @Cacheable("entity17") public List<Entity17> search(String q) { return repo.search(q); }
    public List<Entity17> all() { return repo.findAll(); }
    @Transactional public Entity17 create(Entity17 e) { return repo.save(e); }
}
