package searchengine.services;
import lombok.SneakyThrows;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import searchengine.model.index;
import searchengine.model.lemma;
import searchengine.model.page;
import searchengine.model.site;
import searchengine.model.repositories.IndexRepository;
import searchengine.model.repositories.LemmaRepository;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;
import java.net.URL;
import java.util.*;


public class LemmasIndexing{
    private  PageRepository pageRepository;
    private SiteRepository siteRepository;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;
    private final LuceneMorphology luceneMorphology;
    private final String regex = "[^\\sа-я]";
    private static final String WORD_TYPE_REGEX = "\\W\\w&&[^а-яА-Я\\s]";
    private static final String[] particlesNames = new String[]{"ЧАСТ", "МЕЖД", "ПРЕДЛ", "СОЮЗ", "МС"};



    public LemmasIndexing(LuceneMorphology luceneMorphology, PageRepository pageRepository, SiteRepository siteRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.luceneMorphology = luceneMorphology;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    @SneakyThrows
    public LemmasIndexing(LuceneMorphology luceneMorphology){
        luceneMorphology = new RussianLuceneMorphology();
        this.luceneMorphology = luceneMorphology;
    }



    public HashMap<String, Integer> getLemmaMap(String text){
        HashMap<String, Integer> map = new HashMap<>();
        text = text.toLowerCase().replaceAll(regex, " ");
        String[] words = text.split("\\s+");
        for (String word : words) {

            if(word.isBlank()){
                continue;
            }

            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) {
                continue;
            }

            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }

            String normalForm = normalForms.get(0);

            if (map.containsKey(normalForm)) {
                map.put(word, map.get(normalForm) + 1);
            } else {
                map.put(word, 1);
            }
        }

        return map;
    }


    public Set<String> getLemmaSet(String text) {
        String[] textArray = text.split("\\s+");
        Set<String> lemmaSet = new HashSet<>();
        for (String word : textArray) {
            if (!word.isEmpty() && isCorrectWordForm(word)) {
                List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
                if (anyWordBaseBelongToParticle(wordBaseForms)) {
                    continue;
                }
                lemmaSet.addAll(luceneMorphology.getNormalForms(word));
            }
        }
        return lemmaSet;
    }



    public boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    public boolean hasParticleProperty(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }



    private String htmlTagsCleaning(String responseBody){
        return Jsoup.parse(responseBody).text();
    }


    @SneakyThrows
    public String extractProtocolAndDomain(String urlString) {
        URL url = new URL(urlString);
        String protocol = url.getProtocol();
        String domain = url.getHost();

        if(!domain.startsWith("www.")){
            domain = "www." + domain;
        }

        return protocol + "://" + domain;

    }


    @SneakyThrows
    public void getLemmasAndIndex(String url, site site){

        Connection.Response response = Jsoup.connect(url).timeout(20 * 1000).execute();

        page page = new page();
        page.setPath(url);
        page.setSite(site);
        page.setCode(response.statusCode());
        page.setContent(response.body());
        pageRepository.save(page);

        String text = htmlTagsCleaning(response.body());
        HashMap<String, Integer> lemmasForIndex = getLemmaMap(text);
        for(String key : lemmasForIndex.keySet()){
            lemma newLemma;
            if(lemmaRepository.existsByLemma(key)){
                newLemma = lemmaRepository.getByLemma(key);
                newLemma.setFrequency(newLemma.getFrequency() + 1);
            } else {
                newLemma = new lemma();
                newLemma.setSite(site);
                newLemma.setLemma(key);
                newLemma.setFrequency(1);
                lemmaRepository.save(newLemma);
            }


            index index = new index();
            index.setLemma(newLemma);
            index.setPage(page);
            index.setRank(lemmasForIndex.get(key));

            indexRepository.save(index);

        }



    }


    private boolean isCorrectWordForm(String word) {
        List<String> wordInfo = luceneMorphology.getMorphInfo(word);
        for (String morphInfo : wordInfo) {
            if (morphInfo.matches(WORD_TYPE_REGEX)) {
                return false;
            }
        }
        return true;
    }






}
