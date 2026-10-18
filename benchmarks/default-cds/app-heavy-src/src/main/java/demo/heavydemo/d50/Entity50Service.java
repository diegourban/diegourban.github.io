package demo.heavydemo.d50;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity50Service {
    private final Entity50Repository repo;
    Entity50Service(Entity50Repository repo) { this.repo = repo; }
    @Cacheable("entity50") public List<Entity50> search(String q) { return repo.search(q); }
    public List<Entity50> all() { return repo.findAll(); }
    @Transactional public Entity50 create(Entity50 e) { return repo.save(e); }
}
