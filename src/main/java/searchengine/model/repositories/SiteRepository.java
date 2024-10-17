package searchengine.model.repositories;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.site;


@Repository
@Transactional
public interface SiteRepository extends JpaRepository<site, Integer> {

    boolean existsByUrl(String url);

    site getByUrl(String domain);

    void deleteByNameAndUrl(String name, String url);



}
