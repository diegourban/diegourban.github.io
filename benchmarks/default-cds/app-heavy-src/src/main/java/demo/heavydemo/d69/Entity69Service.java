package demo.heavydemo.d69;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity69Service {
    private final Entity69Repository repo;
    Entity69Service(Entity69Repository repo) { this.repo = repo; }
    @Cacheable("entity69") public List<Entity69> search(String q) { return repo.search(q); }
    public List<Entity69> all() { return repo.findAll(); }
    @Transactional public Entity69 create(Entity69 e) { return repo.save(e); }
}
