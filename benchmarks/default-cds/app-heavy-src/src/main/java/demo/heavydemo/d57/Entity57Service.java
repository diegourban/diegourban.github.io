package demo.heavydemo.d57;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity57Service {
    private final Entity57Repository repo;
    Entity57Service(Entity57Repository repo) { this.repo = repo; }
    @Cacheable("entity57") public List<Entity57> search(String q) { return repo.search(q); }
    public List<Entity57> all() { return repo.findAll(); }
    @Transactional public Entity57 create(Entity57 e) { return repo.save(e); }
}
