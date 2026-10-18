package demo.heavydemo.d25;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity25Service {
    private final Entity25Repository repo;
    Entity25Service(Entity25Repository repo) { this.repo = repo; }
    @Cacheable("entity25") public List<Entity25> search(String q) { return repo.search(q); }
    public List<Entity25> all() { return repo.findAll(); }
    @Transactional public Entity25 create(Entity25 e) { return repo.save(e); }
}
