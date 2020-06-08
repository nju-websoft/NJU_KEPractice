package edu.nju.ws.nlp;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;

import java.util.ArrayList;
import java.util.List;

public class Segmenter {

    /**
     * 使用HanLP分词工具，返回分词结果
     * @param text
     */
    public static List<String> segmentByHanLP(String text){
        List<String> result = new ArrayList<String>();
        List<Term> terms = HanLP.segment(text);
        for(Term t: terms)
            result.add(t.word);
        return result;
    }

}

