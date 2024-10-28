package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.SearchData;
import searchengine.dto.statistics.SearchResponse;
import searchengine.model.index;
import searchengine.model.lemma;
import searchengine.model.page;
import searchengine.model.repositories.IndexRepository;
import searchengine.model.repositories.LemmaRepository;
import searchengine.model.repositories.PageRepository;


import java.sql.SQLOutput;
import java.util.*;



@Service
@RequiredArgsConstructor
public class Search implements SearchService {

    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    private final String regex = "[^\\sА-Яа-я]";
    private LuceneMorphology luceneMorphology;
    private LemmasIndexing lemmasIndexing = new LemmasIndexing(luceneMorphology);


    @Override
    @SneakyThrows
    public SearchResponse search(String query, String site, int offset, int limit) {

        if(query == null || query.isEmpty()){
            return new SearchResponse("Задан пустой поисковый запрос");
        }


        luceneMorphology = new RussianLuceneMorphology();
        query = query.toLowerCase().replaceAll(regex, " ");
        List<lemma> filteredLemmas = new ArrayList<>();
        Set<String> lemmaSet = lemmasIndexing.getLemmaSet(query);
        double percentage = 0.9;
        long pagesCount = site == null ? pageRepository.count() : pageRepository.countBySiteUrl(site);
        for(String lemma : lemmaSet) {
            long lemmaPageCount = lemmaRepository.countByLemma(lemma);
            double frequency = (double) lemmaPageCount / pagesCount;
            if (frequency < percentage) {
                filteredLemmas.add(lemmaRepository.getByLemma(lemma));
            }

        }

        if (filteredLemmas.isEmpty()) return new SearchResponse(0, Collections.emptyList());


        filteredLemmas.sort(Comparator.comparing(lemma -> lemmaRepository.countByLemma(lemma.getLemma())));


        List<index> indexList = indexRepository.findAllByLemma(filteredLemmas.get(0));
        List<page> pagesList = new ArrayList<>();

        if(!indexList.isEmpty()) {
            for (index index : indexList) {
                pagesList.add(index.getPage());
            }
        } else {
            return new SearchResponse(0, Collections.emptyList());
        }



        for (int i = 1; i < filteredLemmas.size(); i++) {
            List<page> nextPages = new ArrayList<>();
            List<index> nextIndexList = indexRepository.findAllByLemma(filteredLemmas.get(i));
            for (index index : nextIndexList) {
                nextPages.add(index.getPage());
            }
            pagesList.retainAll(nextPages);
            if (pagesList.isEmpty()) {
                return new SearchResponse(0, Collections.emptyList());
            }
        }

        Map<page, Double> pageRelevanceMap = new HashMap<>();
        double maxAbsRelevance = 0;
        for (page page : pagesList) {

            double absRelevance = 0;

            for(lemma lemma : filteredLemmas) {
                Optional<index> indexOptional = indexRepository.findByPageAndLemma(page, lemma);
                if (indexOptional.isPresent()) {
                    absRelevance += indexOptional.get().getRank();
                }
            }
            pageRelevanceMap.put(page, absRelevance);

            if(absRelevance > maxAbsRelevance){
                maxAbsRelevance = absRelevance;
            }

        }




        List<SearchData> searchDataList = new ArrayList<>();
        for(Map.Entry<page, Double> entry : pageRelevanceMap.entrySet()){
            page page = entry.getKey();
            double absRel = entry.getValue();
            double relRelevance = absRel / maxAbsRelevance;

            SearchData searchData = new SearchData();
            searchData.setSite(page.getSite().getUrl());
            searchData.setSiteName(page.getSite().getName());
            searchData.setUri(getUri(page.getPath()));
            searchData.setTitle(extractTitleFromHtml(page.getContent()));
            searchData.setSnippet(createSnippet(page.getContent(), lemmaSet));
            searchData.setRelevance((float) relRelevance);
            searchDataList.add(searchData);
        }




        searchDataList.sort(Comparator.comparing(SearchData::getRelevance).reversed());

        return new SearchResponse(searchDataList.size(), searchDataList);
    }





    private String createSnippet(String content, Set<String> lemmaSet) {
        int snippetLength = 150;
        StringBuilder snippet = new StringBuilder();
        String lowerContent = content.toLowerCase();


        List<Integer> matchPositions = new ArrayList<>();
        for (String lemma : lemmaSet) {
            int index = lowerContent.indexOf(lemma);
            while (index >= 0) {
                matchPositions.add(index);
                index = lowerContent.indexOf(lemma, index + 1);
            }
        }


        if (matchPositions.isEmpty()) {
            return content.length() > snippetLength ? content.substring(0, snippetLength) + "..." : content;
        }


        int startPos = Math.max(0, matchPositions.get(0) - snippetLength / 2);
        int endPos = Math.min(content.length(), startPos + snippetLength);
        String snippetContent = content.substring(startPos, endPos);


        String[] words = snippetContent.split("\\s+");
        for (String word : words) {
            String cleanWord = word.replaceAll("[^a-zA-Zа-яА-Я0-9]", "").toLowerCase();
            if (lemmaSet.contains(cleanWord)) {
                snippet.append("<b>").append(word).append("</b> ");
            } else {
                snippet.append(word).append(" ");
            }
        }


        if (content.length() > endPos) {
            snippet.append("...");
        }

        return snippet.toString().trim();
    }



    private String extractTitleFromHtml(String htmlContent) {
        Document document = Jsoup.parse(htmlContent);
        return document.title();
    }


    private String getUri(String url){
        int index = url.indexOf("://");
        if (index != -1) {
            url = url.substring(index + 3);
        }

        index = url.indexOf("/");
        if (index != -1) {
            return url.substring(index);
        }


        return "";
    }


}
