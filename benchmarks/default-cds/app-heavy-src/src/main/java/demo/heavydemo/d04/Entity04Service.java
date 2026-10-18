package demo.heavydemo.d04;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity04Service {
    private final Entity04Repository repo;
    Entity04Service(Entity04Repository repo) { this.repo = repo; }
    @Cacheable("entity04") public List<Entity04> search(String q) { return repo.search(q); }
    public List<Entity04> all() { return repo.findAll(); }
    @Transactional public Entity04 create(Entity04 e) { return repo.save(e); }
}
