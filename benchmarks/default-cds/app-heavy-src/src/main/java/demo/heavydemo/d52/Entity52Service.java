package demo.heavydemo.d52;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity52Service {
    private final Entity52Repository repo;
    Entity52Service(Entity52Repository repo) { this.repo = repo; }
    @Cacheable("entity52") public List<Entity52> search(String q) { return repo.search(q); }
    public List<Entity52> all() { return repo.findAll(); }
    @Transactional public Entity52 create(Entity52 e) { return repo.save(e); }
}
