package demo.heavydemo.d22;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity22Service {
    private final Entity22Repository repo;
    Entity22Service(Entity22Repository repo) { this.repo = repo; }
    @Cacheable("entity22") public List<Entity22> search(String q) { return repo.search(q); }
    public List<Entity22> all() { return repo.findAll(); }
    @Transactional public Entity22 create(Entity22 e) { return repo.save(e); }
}
