package demo.heavydemo.d15;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity15Service {
    private final Entity15Repository repo;
    Entity15Service(Entity15Repository repo) { this.repo = repo; }
    @Cacheable("entity15") public List<Entity15> search(String q) { return repo.search(q); }
    public List<Entity15> all() { return repo.findAll(); }
    @Transactional public Entity15 create(Entity15 e) { return repo.save(e); }
}
