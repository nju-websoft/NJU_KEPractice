package cn.edu.nju.ws.SemanticSearch.structure;

// 表示单个带有标签的实体
public class LabeledResource {
    public LabeledResource(String iri, String label) {
        this.iri = iri;
        this.label = label;
    }

    public String iri;
    public String label;
}
