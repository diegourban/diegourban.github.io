package demo.heavydemo.d19;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity19Service {
    private final Entity19Repository repo;
    Entity19Service(Entity19Repository repo) { this.repo = repo; }
    @Cacheable("entity19") public List<Entity19> search(String q) { return repo.search(q); }
    public List<Entity19> all() { return repo.findAll(); }
    @Transactional public Entity19 create(Entity19 e) { return repo.save(e); }
}
