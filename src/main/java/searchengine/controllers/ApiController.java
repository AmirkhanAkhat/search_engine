package searchengine.controllers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.dto.statistics.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;
    private CompletableFuture<IndexingResponse> indexingResponse;
    private SearchResponse searchResponse;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;

    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public CompletableFuture<IndexingResponse> startIndexing(){
        indexingResponse = indexingService.startIndexing();
        return indexingResponse;
    }

    @GetMapping("/stopIndexing")
    public CompletableFuture<IndexingResponse> stopIndexing(){
        indexingResponse = indexingService.stopIndexing();
        return indexingResponse;
    }

    @PostMapping("/indexPage")
    public CompletableFuture<IndexingResponse> indexPage(@RequestParam String url){
        indexingResponse = indexingService.indexPage(url);
        return indexingResponse;
    }


    @GetMapping("/search")
    public SearchResponse search(@RequestParam(value = "query") String query,
                                                      @RequestParam(value = "site", required = false) String site,
                                                      @RequestParam(value = "offset", defaultValue = "0") int offset,
                                                      @RequestParam(value = "limit", defaultValue = "20") int limit
                                                      ){
        searchResponse = searchService.search(query, site, offset, limit);
        return searchResponse;
    }
}
