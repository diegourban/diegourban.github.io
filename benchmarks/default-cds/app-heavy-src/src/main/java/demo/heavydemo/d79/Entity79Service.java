package demo.heavydemo.d79;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity79Service {
    private final Entity79Repository repo;
    Entity79Service(Entity79Repository repo) { this.repo = repo; }
    @Cacheable("entity79") public List<Entity79> search(String q) { return repo.search(q); }
    public List<Entity79> all() { return repo.findAll(); }
    @Transactional public Entity79 create(Entity79 e) { return repo.save(e); }
}
