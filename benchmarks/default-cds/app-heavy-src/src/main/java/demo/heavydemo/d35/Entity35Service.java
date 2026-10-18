package demo.heavydemo.d35;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity35Service {
    private final Entity35Repository repo;
    Entity35Service(Entity35Repository repo) { this.repo = repo; }
    @Cacheable("entity35") public List<Entity35> search(String q) { return repo.search(q); }
    public List<Entity35> all() { return repo.findAll(); }
    @Transactional public Entity35 create(Entity35 e) { return repo.save(e); }
}
