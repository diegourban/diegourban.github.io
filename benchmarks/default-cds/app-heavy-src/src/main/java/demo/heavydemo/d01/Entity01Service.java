package demo.heavydemo.d01;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity01Service {
    private final Entity01Repository repo;
    Entity01Service(Entity01Repository repo) { this.repo = repo; }
    @Cacheable("entity01") public List<Entity01> search(String q) { return repo.search(q); }
    public List<Entity01> all() { return repo.findAll(); }
    @Transactional public Entity01 create(Entity01 e) { return repo.save(e); }
}
