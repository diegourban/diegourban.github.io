package demo.heavydemo.d44;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity44Service {
    private final Entity44Repository repo;
    Entity44Service(Entity44Repository repo) { this.repo = repo; }
    @Cacheable("entity44") public List<Entity44> search(String q) { return repo.search(q); }
    public List<Entity44> all() { return repo.findAll(); }
    @Transactional public Entity44 create(Entity44 e) { return repo.save(e); }
}
