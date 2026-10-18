package demo.heavydemo.d73;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity73Service {
    private final Entity73Repository repo;
    Entity73Service(Entity73Repository repo) { this.repo = repo; }
    @Cacheable("entity73") public List<Entity73> search(String q) { return repo.search(q); }
    public List<Entity73> all() { return repo.findAll(); }
    @Transactional public Entity73 create(Entity73 e) { return repo.save(e); }
}
