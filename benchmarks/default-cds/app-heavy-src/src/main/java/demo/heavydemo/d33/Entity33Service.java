package demo.heavydemo.d33;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity33Service {
    private final Entity33Repository repo;
    Entity33Service(Entity33Repository repo) { this.repo = repo; }
    @Cacheable("entity33") public List<Entity33> search(String q) { return repo.search(q); }
    public List<Entity33> all() { return repo.findAll(); }
    @Transactional public Entity33 create(Entity33 e) { return repo.save(e); }
}
