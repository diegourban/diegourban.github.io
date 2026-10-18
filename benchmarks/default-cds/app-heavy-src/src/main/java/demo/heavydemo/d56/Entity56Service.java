package demo.heavydemo.d56;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity56Service {
    private final Entity56Repository repo;
    Entity56Service(Entity56Repository repo) { this.repo = repo; }
    @Cacheable("entity56") public List<Entity56> search(String q) { return repo.search(q); }
    public List<Entity56> all() { return repo.findAll(); }
    @Transactional public Entity56 create(Entity56 e) { return repo.save(e); }
}
