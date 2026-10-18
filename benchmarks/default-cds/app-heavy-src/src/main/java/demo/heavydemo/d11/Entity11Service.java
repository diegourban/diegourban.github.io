package demo.heavydemo.d11;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity11Service {
    private final Entity11Repository repo;
    Entity11Service(Entity11Repository repo) { this.repo = repo; }
    @Cacheable("entity11") public List<Entity11> search(String q) { return repo.search(q); }
    public List<Entity11> all() { return repo.findAll(); }
    @Transactional public Entity11 create(Entity11 e) { return repo.save(e); }
}
