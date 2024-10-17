package searchengine.services;

import jakarta.transaction.Transactional;
import searchengine.dto.statistics.IndexingResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Transactional
public interface IndexingService {

    CompletableFuture<IndexingResponse> startIndexing();

    CompletableFuture<IndexingResponse> stopIndexing();

    CompletableFuture<IndexingResponse> indexPage(String url);




}
