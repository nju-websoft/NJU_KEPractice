package cn.edu.nju.ws.SemanticSearch.structure;

import java.util.List;

public class QueryResult {
    public QueryResult(List<HittingInstance> hittingInstances, List<String> keywords) {
        this.hittingInstances = hittingInstances;
        this.keywords = keywords;
    }

    public List<HittingInstance> hittingInstances;
    public List<String> keywords;
}
