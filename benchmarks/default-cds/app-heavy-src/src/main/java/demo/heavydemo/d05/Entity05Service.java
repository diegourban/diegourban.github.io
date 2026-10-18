package demo.heavydemo.d05;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity05Service {
    private final Entity05Repository repo;
    Entity05Service(Entity05Repository repo) { this.repo = repo; }
    @Cacheable("entity05") public List<Entity05> search(String q) { return repo.search(q); }
    public List<Entity05> all() { return repo.findAll(); }
    @Transactional public Entity05 create(Entity05 e) { return repo.save(e); }
}
