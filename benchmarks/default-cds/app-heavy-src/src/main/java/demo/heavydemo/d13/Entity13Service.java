package demo.heavydemo.d13;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity13Service {
    private final Entity13Repository repo;
    Entity13Service(Entity13Repository repo) { this.repo = repo; }
    @Cacheable("entity13") public List<Entity13> search(String q) { return repo.search(q); }
    public List<Entity13> all() { return repo.findAll(); }
    @Transactional public Entity13 create(Entity13 e) { return repo.save(e); }
}
