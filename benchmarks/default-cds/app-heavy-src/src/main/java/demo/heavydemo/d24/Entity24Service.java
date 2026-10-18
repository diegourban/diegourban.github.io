package demo.heavydemo.d24;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity24Service {
    private final Entity24Repository repo;
    Entity24Service(Entity24Repository repo) { this.repo = repo; }
    @Cacheable("entity24") public List<Entity24> search(String q) { return repo.search(q); }
    public List<Entity24> all() { return repo.findAll(); }
    @Transactional public Entity24 create(Entity24 e) { return repo.save(e); }
}
