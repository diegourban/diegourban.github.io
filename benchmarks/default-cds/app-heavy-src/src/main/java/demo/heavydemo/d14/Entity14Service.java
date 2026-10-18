package demo.heavydemo.d14;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity14Service {
    private final Entity14Repository repo;
    Entity14Service(Entity14Repository repo) { this.repo = repo; }
    @Cacheable("entity14") public List<Entity14> search(String q) { return repo.search(q); }
    public List<Entity14> all() { return repo.findAll(); }
    @Transactional public Entity14 create(Entity14 e) { return repo.save(e); }
}
