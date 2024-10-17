package searchengine.services;

import searchengine.dto.statistics.IndexingResponse;
import searchengine.dto.statistics.SearchResponse;

import java.util.concurrent.CompletableFuture;

public interface SearchService {
    SearchResponse search(String query, String site, int offset, int limit);
}
