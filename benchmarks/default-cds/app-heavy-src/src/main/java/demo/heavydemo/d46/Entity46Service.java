package demo.heavydemo.d46;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity46Service {
    private final Entity46Repository repo;
    Entity46Service(Entity46Repository repo) { this.repo = repo; }
    @Cacheable("entity46") public List<Entity46> search(String q) { return repo.search(q); }
    public List<Entity46> all() { return repo.findAll(); }
    @Transactional public Entity46 create(Entity46 e) { return repo.save(e); }
}
