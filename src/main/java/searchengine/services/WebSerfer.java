package searchengine.services;

import java.net.URL;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.site;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;

public class WebSerfer extends RecursiveAction {

    private GraphNode graphNode;
    private AtomicBoolean stopRequested;

    public WebSerfer(GraphNode graphNode, AtomicBoolean atomicBoolean){
        this.graphNode = graphNode;
        this.stopRequested = atomicBoolean;
    }


    @SneakyThrows
    @Override
    protected void compute() {
        String url = graphNode.link;

       try {

            if (stopRequested.get()) {
                Thread.currentThread().interrupt();
                return;
            }

            Document document = Jsoup.connect(url).timeout(20 * 1000).get();
            Elements links = document.select("a[href]");

            for (Element link : links) {
                if (stopRequested.get()) {
                    Thread.currentThread().interrupt();
                    break;
                }

                String linkHref = link.attr("abs:href");
                if (isValid(linkHref, url)) {
                    GraphNode childNode = new GraphNode(linkHref);
                    graphNode.addChild(childNode);
                    new WebSerfer(childNode, stopRequested).fork();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
       }
    }


    public static boolean isValid(String link, String domain){
        try {
            URL url = new URL(domain);
            domain = url.getHost();
            if(domain != null && domain.startsWith("www.")){
                domain = domain.substring(4);
            }

            return !link.contains("mailto") && !link.contains(".pdf") && link.contains(domain);
        } catch (Exception e){
            return false;
        }
    }
}
