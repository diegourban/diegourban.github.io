package demo.heavydemo.d26;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity26Service {
    private final Entity26Repository repo;
    Entity26Service(Entity26Repository repo) { this.repo = repo; }
    @Cacheable("entity26") public List<Entity26> search(String q) { return repo.search(q); }
    public List<Entity26> all() { return repo.findAll(); }
    @Transactional public Entity26 create(Entity26 e) { return repo.save(e); }
}
