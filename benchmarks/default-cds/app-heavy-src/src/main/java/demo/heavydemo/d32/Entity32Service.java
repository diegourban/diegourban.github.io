package demo.heavydemo.d32;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity32Service {
    private final Entity32Repository repo;
    Entity32Service(Entity32Repository repo) { this.repo = repo; }
    @Cacheable("entity32") public List<Entity32> search(String q) { return repo.search(q); }
    public List<Entity32> all() { return repo.findAll(); }
    @Transactional public Entity32 create(Entity32 e) { return repo.save(e); }
}
