package demo.heavydemo.d78;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity78Service {
    private final Entity78Repository repo;
    Entity78Service(Entity78Repository repo) { this.repo = repo; }
    @Cacheable("entity78") public List<Entity78> search(String q) { return repo.search(q); }
    public List<Entity78> all() { return repo.findAll(); }
    @Transactional public Entity78 create(Entity78 e) { return repo.save(e); }
}
