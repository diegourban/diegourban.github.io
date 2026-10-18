package demo.heavydemo.d68;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity68Service {
    private final Entity68Repository repo;
    Entity68Service(Entity68Repository repo) { this.repo = repo; }
    @Cacheable("entity68") public List<Entity68> search(String q) { return repo.search(q); }
    public List<Entity68> all() { return repo.findAll(); }
    @Transactional public Entity68 create(Entity68 e) { return repo.save(e); }
}
