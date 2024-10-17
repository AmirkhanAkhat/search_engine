package searchengine.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.lemma;
import searchengine.model.site;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<lemma, Integer> {
    boolean existsByLemma(String lemma);

    lemma getByLemma(String lemma);

    lemma getAllByLemma(lemma lemma);

    List<lemma> getAllBySite(site site);

    int countBySiteId(int site_id);

    long countByLemma(String lemma);

    lemma findByLemma(String newLemma);
}
