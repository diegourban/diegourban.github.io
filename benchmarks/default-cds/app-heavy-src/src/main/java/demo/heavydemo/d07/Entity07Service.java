package demo.heavydemo.d07;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity07Service {
    private final Entity07Repository repo;
    Entity07Service(Entity07Repository repo) { this.repo = repo; }
    @Cacheable("entity07") public List<Entity07> search(String q) { return repo.search(q); }
    public List<Entity07> all() { return repo.findAll(); }
    @Transactional public Entity07 create(Entity07 e) { return repo.save(e); }
}
