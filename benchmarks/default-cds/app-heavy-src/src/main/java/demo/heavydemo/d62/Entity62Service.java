package demo.heavydemo.d62;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity62Service {
    private final Entity62Repository repo;
    Entity62Service(Entity62Repository repo) { this.repo = repo; }
    @Cacheable("entity62") public List<Entity62> search(String q) { return repo.search(q); }
    public List<Entity62> all() { return repo.findAll(); }
    @Transactional public Entity62 create(Entity62 e) { return repo.save(e); }
}
