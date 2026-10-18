package demo.heavydemo.d12;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity12Service {
    private final Entity12Repository repo;
    Entity12Service(Entity12Repository repo) { this.repo = repo; }
    @Cacheable("entity12") public List<Entity12> search(String q) { return repo.search(q); }
    public List<Entity12> all() { return repo.findAll(); }
    @Transactional public Entity12 create(Entity12 e) { return repo.save(e); }
}
