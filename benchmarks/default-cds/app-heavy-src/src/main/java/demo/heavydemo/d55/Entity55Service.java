package demo.heavydemo.d55;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity55Service {
    private final Entity55Repository repo;
    Entity55Service(Entity55Repository repo) { this.repo = repo; }
    @Cacheable("entity55") public List<Entity55> search(String q) { return repo.search(q); }
    public List<Entity55> all() { return repo.findAll(); }
    @Transactional public Entity55 create(Entity55 e) { return repo.save(e); }
}
