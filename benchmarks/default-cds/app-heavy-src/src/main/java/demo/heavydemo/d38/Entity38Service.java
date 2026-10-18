package demo.heavydemo.d38;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity38Service {
    private final Entity38Repository repo;
    Entity38Service(Entity38Repository repo) { this.repo = repo; }
    @Cacheable("entity38") public List<Entity38> search(String q) { return repo.search(q); }
    public List<Entity38> all() { return repo.findAll(); }
    @Transactional public Entity38 create(Entity38 e) { return repo.save(e); }
}
