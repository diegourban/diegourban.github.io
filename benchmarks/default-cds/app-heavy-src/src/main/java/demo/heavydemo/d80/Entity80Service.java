package demo.heavydemo.d80;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity80Service {
    private final Entity80Repository repo;
    Entity80Service(Entity80Repository repo) { this.repo = repo; }
    @Cacheable("entity80") public List<Entity80> search(String q) { return repo.search(q); }
    public List<Entity80> all() { return repo.findAll(); }
    @Transactional public Entity80 create(Entity80 e) { return repo.save(e); }
}
