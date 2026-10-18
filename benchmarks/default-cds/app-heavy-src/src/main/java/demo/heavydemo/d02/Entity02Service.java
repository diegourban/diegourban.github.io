package demo.heavydemo.d02;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity02Service {
    private final Entity02Repository repo;
    Entity02Service(Entity02Repository repo) { this.repo = repo; }
    @Cacheable("entity02") public List<Entity02> search(String q) { return repo.search(q); }
    public List<Entity02> all() { return repo.findAll(); }
    @Transactional public Entity02 create(Entity02 e) { return repo.save(e); }
}
