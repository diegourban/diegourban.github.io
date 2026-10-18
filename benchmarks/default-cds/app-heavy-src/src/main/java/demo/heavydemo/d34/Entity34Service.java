package demo.heavydemo.d34;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity34Service {
    private final Entity34Repository repo;
    Entity34Service(Entity34Repository repo) { this.repo = repo; }
    @Cacheable("entity34") public List<Entity34> search(String q) { return repo.search(q); }
    public List<Entity34> all() { return repo.findAll(); }
    @Transactional public Entity34 create(Entity34 e) { return repo.save(e); }
}
