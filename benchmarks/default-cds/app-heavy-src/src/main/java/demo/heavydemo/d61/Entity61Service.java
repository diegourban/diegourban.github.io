package demo.heavydemo.d61;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity61Service {
    private final Entity61Repository repo;
    Entity61Service(Entity61Repository repo) { this.repo = repo; }
    @Cacheable("entity61") public List<Entity61> search(String q) { return repo.search(q); }
    public List<Entity61> all() { return repo.findAll(); }
    @Transactional public Entity61 create(Entity61 e) { return repo.save(e); }
}
