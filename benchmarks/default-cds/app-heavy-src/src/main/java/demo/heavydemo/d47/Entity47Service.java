package demo.heavydemo.d47;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity47Service {
    private final Entity47Repository repo;
    Entity47Service(Entity47Repository repo) { this.repo = repo; }
    @Cacheable("entity47") public List<Entity47> search(String q) { return repo.search(q); }
    public List<Entity47> all() { return repo.findAll(); }
    @Transactional public Entity47 create(Entity47 e) { return repo.save(e); }
}
