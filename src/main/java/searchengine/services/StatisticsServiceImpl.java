package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.repositories.LemmaRepository;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;
import searchengine.model.site;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final Random random = new Random();
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexingServiceImpl indexingServiceImpl;

    @Override
    public StatisticsResponse getStatistics() {

        if(siteRepository.count() == 0){
            return getDefaultStatistics();
        }

        StatisticsData data = new StatisticsData();
        TotalStatistics total = getTotalStatistics();
        List<DetailedStatisticsItem> detailed = getDetailedStatistics();
        data.setTotal(total);
        data.setDetailed(detailed);
        StatisticsResponse response = new StatisticsResponse();
        response.setStatistics(data);
        response.setResult(true);
        return response;

    }


    private TotalStatistics getTotalStatistics() {
        TotalStatistics total = new TotalStatistics();
        int totalPages = (int) pageRepository.count();
        int totalLemmas = (int) lemmaRepository.count();
        int totalSites = sites.getSites().size();
        total.setPages(totalPages);
        total.setLemmas(totalLemmas);
        total.setSites(totalSites);
        total.setIndexing(indexingServiceImpl.isIndexingInProgress());
        return total;
    }



    private StatisticsResponse getDefaultStatistics() {
        StatisticsData data = new StatisticsData();
        TotalStatistics total = new TotalStatistics();
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        data.setTotal(total);
        data.setDetailed(detailed);
        StatisticsResponse response = new StatisticsResponse();
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }


    private List<DetailedStatisticsItem> getDetailedStatistics() {
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for(int i = 0; i < sitesList.size(); i++) {
            Site siteConfig = sitesList.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            site site = siteRepository.getByUrl(siteConfig.getUrl());
            int siteId = site.getId();
            int pages = pageRepository.countPagesBySiteId(siteId);
            int lemmas = lemmaRepository.countBySiteId(siteId);
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setName(siteConfig.getName());
            item.setUrl(siteConfig.getUrl());
            item.setStatus(site.getStatus().toString());
            item.setError(site.getLastError());
            Instant instant = site.getStatusTime().atZone(ZoneId.of("Asia/Almaty")).toInstant();
            long millis = instant.toEpochMilli();
            item.setStatusTime(millis);
            detailed.add(item);
        }
        return detailed;
    }
}
