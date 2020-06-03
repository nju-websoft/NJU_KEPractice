package edu.nju.ws.kbqa;

import edu.nju.ws.bean.Entity;
import edu.nju.ws.bean.slotMode;
import edu.nju.ws.utils.StringUtil;
import edu.nju.ws.utils.TextSimilarity;

import java.util.*;

import static edu.nju.ws.kbqa.Loadsource.typed_entity_inverted_index;
import static edu.nju.ws.kbqa.Loadsource.typed_propertys;
import static edu.nju.ws.nlp.Segmenter.segmentByHanLP;

/**
 * 在该类中主要识别问句中的实体以及属性，并将结果返回
 */
public class SentenceRec {
    private static int top_num = 10;  //初步实体链接返回最多个数
    private static double entityyu = 0.51; //实体链接的阈值(当多个时)
    private static double entityfirsatyu = 0.34;//第一个实体的阈值
    private static double propertyyu = 0.222; //属性连接阈值
    private static double propertyadd = 0.2;

    /**
     * 实体识别+链接
     * @param question
     * @param top_num
     * @return
     */
    private static List<slotMode> entityRec(String question, int top_num){
        Map<String, Set<Entity>> index = new HashMap<>();
        for(String key:typed_entity_inverted_index.keySet()){
            for(String token:typed_entity_inverted_index.get(key).keySet()){
                if(index.containsKey(token)){
                    index.get(token).addAll(typed_entity_inverted_index.get(key).get(token));
                }else{
                    Set<Entity> temp = new HashSet<>(typed_entity_inverted_index.get(key).get(token));
                    index.put(token,temp);
                }
            }
        }

        Vector<slotMode> entitys = new Vector<>();
        Vector<String> tokens = StringUtil.get_ngram(question);  //按token长度从大到小排序
        for(String item :tokens){
            if(index.containsKey(item) && index.get(item).size() > 0){
                for(Entity a:index.get(item)){
                    slotMode temp = new slotMode();
                    temp.entity = a.clone();
                    if(question.contains(a.label))
                        temp.mention = a.label;
                    else
                        temp.mention = item;
                    temp.startindex = question.indexOf(temp.mention);
                    temp.endindex = temp.startindex + temp.mention.length();
                    entitys.add(temp);
                }
            }
        }

        //对C中找到的实体，基于实体label和mention之间的编辑距离进行打分排序
        for(slotMode item:entitys){
            item.score = TextSimilarity.getLCS(item.entity.label, item.mention);
        }
        //将结果以元素score进行排序
        entitys.sort((left, right) -> {
            if (right.score - left.score > 0)
                return 1;
            else if (right.score - left.score == 0)
                return 0;
            else
                return -1;
        });

        int i = 1;
        if(entitys.size()>0 && entitys.get(0).score < entityfirsatyu){  //如果第一个实体的得分低于阈值（.034）则认为该句话中没有实体存在
            return new Vector<>();
        }
        for(; i<entitys.size(); i++){ //从第2个实体开始，进行基于实体阈值的识别
            if(entitys.get(i).score < entityyu)
                break;
        }

        if (entitys.size() > Math.min(i, top_num)) {
            entitys.subList(Math.min(i, top_num), entitys.size()).clear();
        }

        return entitys;
    }

    /**
     * 属性识别+链接
     * @return
     */
    private static slotMode propertyRec(String question, Set<Entity> propertys, double threshold){
        List<String> questionList = segmentByHanLP(question);
        //对属性进行分词
        double property_Score = -100;
        Entity property = null; //最终返回的属性
        int startindex = 0;
        int endindex = 0;
        for(Entity pro:propertys){
            String proper = pro.label;
            List<String> pl = segmentByHanLP(proper);
            double score1 = -100; //记录每一个属性和问句最高分
            int ii = 0,jj = 0;
            int leni = 0; //起始长度
            for(int i=0; i<questionList.size(); i++){
                double score = 0;  //窗口中每一个词得分和
                int len = 0; //属性长度
                int cnt = 0;
                for(int j=0; j<pl.size(); j++){ //顺序比较
                    if(i+j < questionList.size()){
                        double semanticsimilarity = TextSimilarity.pharseSimilarity(questionList.get(i+j), pl.get(j));
                        if((semanticsimilarity+1)<1e-7){ //表示两个词中存在未登录词语
                            semanticsimilarity = 0;
                        }else{
                            score += semanticsimilarity;
                            cnt++;
                        }
                    }
                }
                if(cnt > 0)
                    score = score *1.0 / cnt;
                //逆序比较
                double ni_score = 0;
                cnt = 0;
                for(int j=0; j<pl.size(); j++){
                    if(i-j >= 0){
                        double semanticsimilarity = TextSimilarity.pharseSimilarity(questionList.get(i-j), pl.get(j));
                        if((semanticsimilarity+1)<1e-7){ //表示两个词中存在未登录词语
                            semanticsimilarity = 0;
                        }else{
                            ni_score += semanticsimilarity;
                            cnt++;
                        }
                    }
                }
                if(cnt > 0)
                    ni_score = ni_score *1.0 / cnt;
                if(ni_score > score)
                    score = ni_score;
                String mention = question.substring(leni, Math.min(leni+proper.length(),question.length()));
                if(cnt > 0)
                    score = 0.3*score +  0.7*TextSimilarity.getLCS(proper, mention); //属性的最终得分由0.5的语义得分 + 0.5的编辑距离得分给出
                else
                    score = TextSimilarity.getLCS(proper, mention);
                if(score > score1){
                    score1 = score;
                    ii = leni; //窗口开始
                    len = Math.min(mention.length(),proper.length());
                    jj = leni + len; //窗口结束
                }
                leni +=questionList.get(i).length();
            }
            if(score1 >= property_Score){
                property_Score = score1;
                property = pro;
                startindex = ii;
                endindex = jj;
            }
        }

        String mention = question.substring(startindex, Math.min(endindex,question.length()));
        slotMode result =  new slotMode(property, mention, startindex, endindex, property_Score);
        if(result.score < threshold)
            return null;
        else
            return result;
    }

    /**
     * 对实体识别结果进行冲突消解，并基于实体识别结果对问句进行处理
     * @param entities
     * @return
     */
    private static String ConflictRes(List<slotMode> entities, String question){
        int i=0;
        //删除有重复覆盖区域的实体
        int[] visit = new int[question.length()]; //初始化问句区域
        for(i=0; i<visit.length; i++){
            visit[i] = 0;
        }
        for(i=0; i<entities.size(); i++){ //遍历找到的实体以及实体属性
            slotMode temp = entities.get(i);
            boolean flag = true;
            for(int j=temp.startindex; j<temp.endindex; j++){
                if(visit[j] == 1)
                    flag = false;
            }
            if(flag){
                for(int j=temp.startindex; j<temp.endindex; j++){
                    visit[j] = 1;
                }
            }else{
                entities.remove(temp);
                i--;
            }
        }
        //去掉实体后的问句
        StringBuilder iquestion = new StringBuilder();
        for(i=0; i<question.length(); i++){
            if(visit[i]==0)
                iquestion.append(question.charAt(i));
            else
                iquestion.append("#"); //使用特殊字符覆盖
        }
        return iquestion.toString();
    }

    /**
     * 输入问题，返回槽（实体/属性）结果
     * @return
     */
    public static List<slotMode> SlotRec(String question){
        List<slotMode> entities = entityRec(question, 4);

        //对问句其余部分进一步进行属性映射,属性映射依据包含的实体类型来限制范围
        Set<Entity> property = new HashSet<>();
        Set<String> ptype = new HashSet<String>();
        //根据意图限定属性链接范围
        for (slotMode item : entities) {
            String entitytype = item.entity.type;
            ptype.add(entitytype.replace("_name", "_property"));
        }
        if(ptype.size()>0){
            for(String type: ptype) { //加载各type下相关属性
                if(typed_propertys.containsKey(type))
                    property.addAll(typed_propertys.get(type));
            }
        }else{
            for(String key: typed_propertys.keySet()){
                property.addAll(typed_propertys.get(key));
            }
        }
        String iquestion = ConflictRes(entities, question);

        //属性链接
        double threshold = propertyyu;
        int i;
        while(true){
            slotMode re = propertyRec(iquestion, property, threshold);
            if(re == null){ //属性映射失败
                break;
            }
            //覆盖已识别出的属性
            StringBuilder padding = new StringBuilder();
            for(i=re.startindex;i<re.endindex;i++)
                padding.append("#");

            iquestion = iquestion.substring(0, re.startindex) + padding + iquestion.substring(re.endindex);
            entities.add(re);
            threshold += propertyadd;
            for(String key: typed_propertys.keySet()){
                property.addAll(typed_propertys.get(key));
            }
        }
        return entities;
    }
}
