package demo.heavydemo.d41;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity41Service {
    private final Entity41Repository repo;
    Entity41Service(Entity41Repository repo) { this.repo = repo; }
    @Cacheable("entity41") public List<Entity41> search(String q) { return repo.search(q); }
    public List<Entity41> all() { return repo.findAll(); }
    @Transactional public Entity41 create(Entity41 e) { return repo.save(e); }
}
