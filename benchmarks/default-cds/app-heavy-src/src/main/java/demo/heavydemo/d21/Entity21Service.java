package demo.heavydemo.d21;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity21Service {
    private final Entity21Repository repo;
    Entity21Service(Entity21Repository repo) { this.repo = repo; }
    @Cacheable("entity21") public List<Entity21> search(String q) { return repo.search(q); }
    public List<Entity21> all() { return repo.findAll(); }
    @Transactional public Entity21 create(Entity21 e) { return repo.save(e); }
}
