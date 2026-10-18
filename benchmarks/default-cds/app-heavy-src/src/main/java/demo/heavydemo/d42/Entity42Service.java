package demo.heavydemo.d42;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity42Service {
    private final Entity42Repository repo;
    Entity42Service(Entity42Repository repo) { this.repo = repo; }
    @Cacheable("entity42") public List<Entity42> search(String q) { return repo.search(q); }
    public List<Entity42> all() { return repo.findAll(); }
    @Transactional public Entity42 create(Entity42 e) { return repo.save(e); }
}
