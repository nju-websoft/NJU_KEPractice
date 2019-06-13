package cn.edu.nju.ws.SemanticSearch.service;

import cn.edu.nju.ws.SemanticSearch.structure.HittingInstance;
import cn.edu.nju.ws.SemanticSearch.structure.LabeledResource;
import cn.edu.nju.ws.SemanticSearch.structure.QueryResult;
import com.alibaba.fastjson.JSON;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

import java.util.*;

public class QueryExecutor {
    private VirtGraph connection;
    private List<String> keywords;
    private List<String> classes;

    public QueryExecutor(VirtGraph graph, List<String> keywordsFilters, List<String> classesFilters) {
        this.connection = graph;
        this.keywords = keywordsFilters;
        this.classes = classesFilters;
    }

    public QueryResult query() {
        List<HittingInstance> hittingInstances = new ArrayList<>();
        // 将输入的用户查询转换成SPARQL查询
        String sparql = String.format("SELECT ?s ?p ?l WHERE { ?s ?p ?l . ?l bif:contains \"%s\"} LIMIT 10", keywords.get(0));
        System.out.println(sparql);
        
        // 使用Virtuoso数据库进行查询
        VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, connection);
        ResultSet rs = vqe.execSelect();

        // 遍历查询结果
        while (rs.hasNext()) {
            QuerySolution solution = rs.nextSolution();
            String instanceIRI = solution.getResource("s").getURI();
            // 这里构造输出的结果，需要注意的是必须只有structure下定义的属性可以被Web页面正确显示，如果需要显示额外的内容，必须自行修改Web页面
            HittingInstance instance = new HittingInstance(instanceIRI, "label of " + instanceIRI);
            instance.addType("http://www.w3.org/2002/07/owl#Thing", "Thing");
            instance.addAttribute(solution.getResource("p").getURI(), solution.getLiteral("l").getString());
            hittingInstances.add(instance);
        }
        
        for (HittingInstance instance : hittingInstances) {
        	// 这里会给每个instance随机打分
            instance.updateScore(keywords);
        }
        
        Collections.sort(hittingInstances);

        return new QueryResult(hittingInstances, keywords);
    }
}
