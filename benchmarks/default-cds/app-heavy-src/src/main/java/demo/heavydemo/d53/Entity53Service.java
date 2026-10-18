package demo.heavydemo.d53;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity53Service {
    private final Entity53Repository repo;
    Entity53Service(Entity53Repository repo) { this.repo = repo; }
    @Cacheable("entity53") public List<Entity53> search(String q) { return repo.search(q); }
    public List<Entity53> all() { return repo.findAll(); }
    @Transactional public Entity53 create(Entity53 e) { return repo.save(e); }
}
