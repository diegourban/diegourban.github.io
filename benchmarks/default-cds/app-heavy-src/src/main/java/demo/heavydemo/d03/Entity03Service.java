package demo.heavydemo.d03;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity03Service {
    private final Entity03Repository repo;
    Entity03Service(Entity03Repository repo) { this.repo = repo; }
    @Cacheable("entity03") public List<Entity03> search(String q) { return repo.search(q); }
    public List<Entity03> all() { return repo.findAll(); }
    @Transactional public Entity03 create(Entity03 e) { return repo.save(e); }
}
