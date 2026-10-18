package demo.heavydemo.d74;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity74Service {
    private final Entity74Repository repo;
    Entity74Service(Entity74Repository repo) { this.repo = repo; }
    @Cacheable("entity74") public List<Entity74> search(String q) { return repo.search(q); }
    public List<Entity74> all() { return repo.findAll(); }
    @Transactional public Entity74 create(Entity74 e) { return repo.save(e); }
}
