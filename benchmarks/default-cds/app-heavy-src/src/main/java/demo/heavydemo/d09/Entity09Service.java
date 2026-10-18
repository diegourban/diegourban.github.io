package demo.heavydemo.d09;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity09Service {
    private final Entity09Repository repo;
    Entity09Service(Entity09Repository repo) { this.repo = repo; }
    @Cacheable("entity09") public List<Entity09> search(String q) { return repo.search(q); }
    public List<Entity09> all() { return repo.findAll(); }
    @Transactional public Entity09 create(Entity09 e) { return repo.save(e); }
}
