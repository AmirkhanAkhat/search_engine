package searchengine.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.page;

@Repository
public interface PageRepository extends JpaRepository<page, Integer> {
    boolean existsByPath(String path);

    int countPagesBySiteId(int site_id);

    long countBySiteUrl(String site);

    page findByPath(String url);
}
