package searchengine.services;


import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.model.*;
import searchengine.model.repositories.IndexRepository;
import searchengine.model.repositories.LemmaRepository;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final AtomicBoolean indexingInProgress = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    ForkJoinPool pool;
    CompletableFuture<Void> future;
    List<CompletableFuture<Void>> futures;
    LuceneMorphology luceneMorphology;


    @Override
    public CompletableFuture<IndexingResponse> startIndexing() {

        if(indexingInProgress.get()){
            return CompletableFuture.completedFuture(new IndexingResponse(false, "Индексация уже запущена"));
        }

        indexingInProgress.set(true);
        stopRequested.set(false);

        List<Site> sites = sitesList.getSites();

        futures = new ArrayList<>();


        List<site> siteList = siteRepository.findAll();
        List<page> pageList = pageRepository.findAll();


        if (!pageList.isEmpty()) {
            indexRepository.deleteAll();
            indexRepository.flush();
            lemmaRepository.deleteAll();
            lemmaRepository.flush();
            pageRepository.deleteAll();
            pageRepository.flush();
        }

        if (!siteList.isEmpty()) {
            siteRepository.deleteAll();
            pageRepository.flush();
        }

        for (Site site : sites) {
             future = CompletableFuture.runAsync( () -> {
                String name = site.getName();
                String siteUrl = site.getUrl();

                System.out.println(name + " - " + siteUrl);
                site siteForDb = new site();
                siteForDb.setUrl(siteUrl);
                siteForDb.setStatus(status.INDEXING);
                siteForDb.setName(name);
                siteForDb.setStatusTime(LocalDateTime.now());
                siteRepository.save(siteForDb);
                indexPages(siteUrl, siteForDb);
            });
            futures.add(future);
        }

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.thenRun(() -> indexingInProgress.set(false));
        return CompletableFuture.completedFuture(new IndexingResponse(true));

    }

    @Override
    public CompletableFuture<IndexingResponse> stopIndexing() {

        if(!indexingInProgress.get()){
            return CompletableFuture.completedFuture(new IndexingResponse(false, "Индексация не запущена"));
        }

        stopRequested.set(true);
        if(pool != null && future != null){
            pool.shutdownNow();
            future.cancel(true);
            futures.add(future);
        }

        indexingInProgress.set(false);

        return CompletableFuture.completedFuture(new IndexingResponse(true));
    }




    @SneakyThrows
    @Override
    public CompletableFuture<IndexingResponse> indexPage(String url) {

        luceneMorphology = new RussianLuceneMorphology();
        LemmasIndexing lemmasIndexing = new LemmasIndexing(luceneMorphology, pageRepository, siteRepository, lemmaRepository, indexRepository);

        site site;

        if(!siteRepository.existsByUrl(lemmasIndexing.extractProtocolAndDomain(url))) {
            return CompletableFuture.completedFuture(new IndexingResponse(false, "Данная страница находится за пределами сайтов, указанных в конфиге"));
        }


        if (pageRepository.findByPath(url) != null) {

            page newpage = pageRepository.findByPath(url);
            List<lemma> lemmas = new ArrayList<>();

            site = siteRepository.getByUrl(newpage.getSite().getUrl());

            List<index> indexList = indexRepository.findAllByPage(newpage);
            if (!indexList.isEmpty()) {
                for(index index : indexList){
                    lemmas.add(index.getLemma());
                }
                indexRepository.deleteAll(indexList);
                indexRepository.deleteAllByLemmaIn(lemmas);
                lemmaRepository.deleteAll(lemmas);
            }

            pageRepository.delete(newpage);
        } else {
            site = siteRepository.getByUrl(lemmasIndexing.extractProtocolAndDomain(url));
        }


        lemmasIndexing.getLemmasAndIndex(url, site);

        return  CompletableFuture.completedFuture(new IndexingResponse(true));
    }


    public void indexPages(String url, site site) {
        site newSite = siteRepository.findById(site.getId()).get();
        try {
            GraphNode root = new GraphNode(url);
            pool = new ForkJoinPool();
            pool.invoke(new WebSerfer(root, stopRequested));
            createPages(root, site);
            newSite.setStatus(status.INDEXED);
            siteRepository.save(newSite);
        } catch (RuntimeException e) {
            newSite.setStatus(status.FAILED);
            newSite.setLastError("Индексация прервана пользователем");
            siteRepository.save(newSite);
        } catch (Exception e){
            newSite.setStatus(status.FAILED);
            newSite.setLastError(e.getLocalizedMessage());
            siteRepository.save(newSite);
        }

    }


    @SneakyThrows
    public void createPages(GraphNode graphNode, site site) throws RuntimeException{
        site newSite = siteRepository.findById(site.getId()).get();
        luceneMorphology = new RussianLuceneMorphology();
        LemmasIndexing lemmasIndexing = new LemmasIndexing(luceneMorphology, pageRepository, siteRepository, lemmaRepository, indexRepository);
        try {
            for (GraphNode child : graphNode.children) {
                if(stopRequested.get()){
                    throw new InterruptedException("Индексация прервана пользователем");
                }
                if (!pageRepository.existsByPath(child.link)) {
                    lemmasIndexing.getLemmasAndIndex(child.link, site);
                    System.out.println("Добавляем: " + child.link);
                    newSite.setStatusTime(LocalDateTime.now());
                    siteRepository.save(newSite);
                    createPages(child, site);
                }
            }
        } catch (InterruptedException e){
            e.printStackTrace();
            throw new RuntimeException();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public boolean isIndexingInProgress() {
        return indexingInProgress.get();
    }

}
