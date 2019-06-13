package cn.edu.nju.ws.SemanticSearch.structure;

import java.util.*;

// 表示一个实例
public class HittingInstance implements Comparable<HittingInstance> {
	// 实例的IRI
    public String iri;
    // 实例的标签
    public String label;
    // 实例的属性信息
    public Map<String, Set<String>> attributes;
    // 实例的类型信息
    public Set<LabeledResource> types;
    // 实例的得分
    protected Double score;

    public HittingInstance(String iri, String label) {
        this.iri = iri;
        this.label = label;
        attributes = new HashMap<>();
        types = new HashSet<>();
        score = null;
    }

    public void addAttribute(String property, String literal) {
        if (!attributes.containsKey(property)) {
            attributes.put(property, new HashSet<String>());
        }
        attributes.get(property).add(literal);
    }

    public void addType(String type, String label) {
        types.add(new LabeledResource(type, label));
    }

    // 计算实例的分数，这里是随机打分
    public double computeScore(List<String> keywords) {
        return Math.random();
    }

    public void updateScore(List<String> keyword) {
        this.score = computeScore(keyword);
    }

    @Override
    public int compareTo(HittingInstance o) {
        if (score > o.score) {
            return 1;
        } else if (score.equals(o.score)) {
            return 0;
        } else {
            return -1;
        }
    }
}
