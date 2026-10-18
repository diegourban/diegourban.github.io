package demo.heavydemo.d60;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity60Service {
    private final Entity60Repository repo;
    Entity60Service(Entity60Repository repo) { this.repo = repo; }
    @Cacheable("entity60") public List<Entity60> search(String q) { return repo.search(q); }
    public List<Entity60> all() { return repo.findAll(); }
    @Transactional public Entity60 create(Entity60 e) { return repo.save(e); }
}
