package demo.heavydemo.d18;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity18Service {
    private final Entity18Repository repo;
    Entity18Service(Entity18Repository repo) { this.repo = repo; }
    @Cacheable("entity18") public List<Entity18> search(String q) { return repo.search(q); }
    public List<Entity18> all() { return repo.findAll(); }
    @Transactional public Entity18 create(Entity18 e) { return repo.save(e); }
}
