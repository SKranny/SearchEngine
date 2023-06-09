package main.repository;

import main.model.Lemma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

}
