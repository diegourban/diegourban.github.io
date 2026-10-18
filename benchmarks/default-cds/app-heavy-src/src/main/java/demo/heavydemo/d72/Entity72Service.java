package demo.heavydemo.d72;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity72Service {
    private final Entity72Repository repo;
    Entity72Service(Entity72Repository repo) { this.repo = repo; }
    @Cacheable("entity72") public List<Entity72> search(String q) { return repo.search(q); }
    public List<Entity72> all() { return repo.findAll(); }
    @Transactional public Entity72 create(Entity72 e) { return repo.save(e); }
}
