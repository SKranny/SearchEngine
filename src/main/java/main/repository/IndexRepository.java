package main.repository;

import main.model.Search_index;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IndexRepository extends JpaRepository<Search_index, Integer> {
}
