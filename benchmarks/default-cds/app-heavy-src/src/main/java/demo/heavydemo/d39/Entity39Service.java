package demo.heavydemo.d39;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity39Service {
    private final Entity39Repository repo;
    Entity39Service(Entity39Repository repo) { this.repo = repo; }
    @Cacheable("entity39") public List<Entity39> search(String q) { return repo.search(q); }
    public List<Entity39> all() { return repo.findAll(); }
    @Transactional public Entity39 create(Entity39 e) { return repo.save(e); }
}
