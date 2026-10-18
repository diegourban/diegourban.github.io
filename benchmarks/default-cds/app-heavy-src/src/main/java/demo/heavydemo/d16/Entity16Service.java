package demo.heavydemo.d16;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity16Service {
    private final Entity16Repository repo;
    Entity16Service(Entity16Repository repo) { this.repo = repo; }
    @Cacheable("entity16") public List<Entity16> search(String q) { return repo.search(q); }
    public List<Entity16> all() { return repo.findAll(); }
    @Transactional public Entity16 create(Entity16 e) { return repo.save(e); }
}
