package main.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

public interface PageRepository extends CrudRepository<Page,Integer> {
}
