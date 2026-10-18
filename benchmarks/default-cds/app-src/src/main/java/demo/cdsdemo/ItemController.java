package demo.cdsdemo;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/items")
public class ItemController {
    private final ItemRepository repo;
    ItemController(ItemRepository repo) { this.repo = repo; }
    @GetMapping List<Item> all() { return repo.findAll(); }
    @PostMapping Item create(@Valid @RequestBody Item item) { return repo.save(item); }
}
