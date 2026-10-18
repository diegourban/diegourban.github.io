package demo.heavydemo.d71;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity71Service {
    private final Entity71Repository repo;
    Entity71Service(Entity71Repository repo) { this.repo = repo; }
    @Cacheable("entity71") public List<Entity71> search(String q) { return repo.search(q); }
    public List<Entity71> all() { return repo.findAll(); }
    @Transactional public Entity71 create(Entity71 e) { return repo.save(e); }
}
