package demo.heavydemo.d76;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity76Service {
    private final Entity76Repository repo;
    Entity76Service(Entity76Repository repo) { this.repo = repo; }
    @Cacheable("entity76") public List<Entity76> search(String q) { return repo.search(q); }
    public List<Entity76> all() { return repo.findAll(); }
    @Transactional public Entity76 create(Entity76 e) { return repo.save(e); }
}
