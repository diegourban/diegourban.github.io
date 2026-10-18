package demo.heavydemo.d66;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity66Service {
    private final Entity66Repository repo;
    Entity66Service(Entity66Repository repo) { this.repo = repo; }
    @Cacheable("entity66") public List<Entity66> search(String q) { return repo.search(q); }
    public List<Entity66> all() { return repo.findAll(); }
    @Transactional public Entity66 create(Entity66 e) { return repo.save(e); }
}
