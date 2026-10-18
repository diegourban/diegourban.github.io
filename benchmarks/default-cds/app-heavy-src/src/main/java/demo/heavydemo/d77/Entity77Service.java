package demo.heavydemo.d77;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity77Service {
    private final Entity77Repository repo;
    Entity77Service(Entity77Repository repo) { this.repo = repo; }
    @Cacheable("entity77") public List<Entity77> search(String q) { return repo.search(q); }
    public List<Entity77> all() { return repo.findAll(); }
    @Transactional public Entity77 create(Entity77 e) { return repo.save(e); }
}
