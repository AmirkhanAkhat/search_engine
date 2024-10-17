package searchengine.services;

import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;

public class GraphNode {

    String link;
    Set<GraphNode> children;

    public GraphNode(String link){
        this.link = link;
        this.children = new HashSet<>();
    }

    public void addChild(GraphNode child){
        children.add(child);
    }



}
