package edu.nju.ws.kbqa;

import edu.nju.ws.bean.Slot;
import edu.nju.ws.bean.slotMode;
import java.util.*;

import static edu.nju.ws.kbqa.Loadsource.slotMap;


public class LU {
	static double THRESHOLD = 0.41;
    
	 /**
	 * 识别句子中出现的典型某一个模板才会出现的词，用字符串匹配
	  * 返回出现关键字的次数
	 * @param sentence
	 * @return list
	 */
	public static HashMap<String,Integer> getgoal(String sentence){
		HashMap<String,Integer> goallist = new HashMap<>();
		
		for(String key: Loadsource.goalMap.keySet()){
			for(String word: Loadsource.goalMap.get(key).Triggers){
				if(sentence.contains(word)){
					goallist.putIfAbsent(key,0);
					goallist.put(key, goallist.get(key)+1);
				}
			}
		}
		return goallist;	
	}

	
	/**
	 * 识别句子中出现的槽有哪些  粗识别 按大类识别
	 * @param sentence
	 * @return 返回识别出来的槽列表
	 */
	public static Set<Slot> getslot(String sentence) {
		List<slotMode> pairList = null;
		Set<Slot> list = new HashSet<>();
		pairList = SentenceRec.SlotRec(sentence);

		if(pairList!=null&&pairList.size()>0){
			for(slotMode pair:pairList){
				if(pair.score >THRESHOLD){ //保留大于阈值的槽
					list.add(new Slot(pair.entity.type, slotMap.get(pair.entity.type))); //加入槽名和相关意图
				}
			}
			
			for(slotMode pair:pairList){
				if(pair.score >THRESHOLD){
					for(Slot slot:list){
						if(slot.name.equals(pair.entity.type)) {
							slot.valueList.add(pair.mention);
							slot.IRIList.add(pair.entity.IRI);
							if (pair.score > slot.score) {
								slot.score = pair.score;  // 将值填入槽中的值的最大置信度分数作为该槽的置信度
							}
						}
					}
				}
			}
		}
		return list;
    }
	
}




















