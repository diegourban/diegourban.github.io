package demo.heavydemo.d40;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity40Service {
    private final Entity40Repository repo;
    Entity40Service(Entity40Repository repo) { this.repo = repo; }
    @Cacheable("entity40") public List<Entity40> search(String q) { return repo.search(q); }
    public List<Entity40> all() { return repo.findAll(); }
    @Transactional public Entity40 create(Entity40 e) { return repo.save(e); }
}
