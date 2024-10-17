package searchengine.model.repositories;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.index;
import searchengine.model.lemma;
import searchengine.model.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface IndexRepository extends JpaRepository<index, Integer> {


    List<index> findAllByPage(page page);

    void deleteAllByLemmaIn(List<lemma> lemmas);

    List<index> findAllByLemma(lemma lemma);


    ArrayList<page> findAllPagesByLemma(lemma lemma);


    Optional<index> findByPageAndLemma(page page, lemma lemma);
}
