package searchengine.dto.statistics;

import lombok.Data;

import java.util.List;


@Data
public class SearchResponse {
    private boolean result;
    private int count;
    private String error;
    private List<SearchData> data;

    public SearchResponse(int count, List<SearchData> data){
        this.result = true;
        this.count = count;
        this.data = data;
    }

    public SearchResponse(String error){
        this.result = false;
        this.error = error;
    }

}
