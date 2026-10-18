package demo.heavydemo.d58;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity58Service {
    private final Entity58Repository repo;
    Entity58Service(Entity58Repository repo) { this.repo = repo; }
    @Cacheable("entity58") public List<Entity58> search(String q) { return repo.search(q); }
    public List<Entity58> all() { return repo.findAll(); }
    @Transactional public Entity58 create(Entity58 e) { return repo.save(e); }
}
