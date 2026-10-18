package demo.heavydemo.d08;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity08Service {
    private final Entity08Repository repo;
    Entity08Service(Entity08Repository repo) { this.repo = repo; }
    @Cacheable("entity08") public List<Entity08> search(String q) { return repo.search(q); }
    public List<Entity08> all() { return repo.findAll(); }
    @Transactional public Entity08 create(Entity08 e) { return repo.save(e); }
}
