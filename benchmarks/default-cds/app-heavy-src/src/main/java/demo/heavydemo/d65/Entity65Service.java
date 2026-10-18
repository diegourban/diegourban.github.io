package demo.heavydemo.d65;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class Entity65Service {
    private final Entity65Repository repo;
    Entity65Service(Entity65Repository repo) { this.repo = repo; }
    @Cacheable("entity65") public List<Entity65> search(String q) { return repo.search(q); }
    public List<Entity65> all() { return repo.findAll(); }
    @Transactional public Entity65 create(Entity65 e) { return repo.save(e); }
}
