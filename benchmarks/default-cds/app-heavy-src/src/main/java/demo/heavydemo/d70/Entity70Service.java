package demo.heavydemo.d70;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity70Service {
    private final Entity70Repository repo;
    Entity70Service(Entity70Repository repo) { this.repo = repo; }
    @Cacheable("entity70") public List<Entity70> search(String q) { return repo.search(q); }
    public List<Entity70> all() { return repo.findAll(); }
    @Transactional public Entity70 create(Entity70 e) { return repo.save(e); }
}
