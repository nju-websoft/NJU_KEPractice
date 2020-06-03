package edu.nju.ws.kbqa;

import edu.nju.ws.bean.Slot;
import edu.nju.ws.bean.TemplateScore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class TemplateManage {
	
	/**
	 * @param goallist 对一句话的意图进行打分得到的意图分数列表
	 * @param slotlist 一句话中的槽列表
	 * @return  返回经过判断后的对应模板名
	 */
	public static TemplateScore TempalteChoose(HashMap<String, Integer> goallist, Set<Slot> slotlist) {
		String intent = null;
		TemplateScore intentscore = new TemplateScore();
		double lamda2 = 0.45;
		double lamda3 = 0.46; //关键字，关键字数量要少且精
		double discount = 0.1;
		
		Map<String, Double> goalScoreMap = new HashMap<String,Double>();

		if(slotlist != null&&slotlist.size()>0){ //基础识别出的槽
			  
			for(Slot s:slotlist){
				if(s.relatetemplate==null||s.relatetemplate.size()==0) { //该槽无相关模板，一般不可能出现
					System.out.println(s.name + "槽没有相关模板");
					continue;
				}
				if(s.relatetemplate.size() == 1){//专有槽
					//在模板意图分数表中加上模板意图，初始化分数为0
					String intenttype = s.relatetemplate.get(0);
					goalScoreMap.putIfAbsent(intenttype, 0.0);
					double templateScore = goalScoreMap.get(intenttype);
					templateScore += s.score*lamda2; //专有槽 将   槽分数*参数lamda1
					goalScoreMap.put(intenttype, templateScore);
				}else {//公共槽
					for(String intentname:s.relatetemplate){
						//在意图分数表中加上意图，初始化分数为0
						goalScoreMap.putIfAbsent(intentname, 0.0);
						double templateScore = goalScoreMap.get(intentname);
						templateScore += s.score*discount*lamda2;
						goalScoreMap.put(intentname, templateScore);
					}
				}
			}
		}

		if(goallist != null && goallist.size()>0){  // 基于识别出来的关键字
			for(String intentname:goallist.keySet()){
				//在意图分数表中加上意图，初始化分数为0
				goalScoreMap.putIfAbsent(intentname, 0.0);
				double templateScore = goalScoreMap.get(intentname);
				templateScore += lamda3 * goallist.get(intentname); //专有槽 将   槽分数*参数lamda3
				goalScoreMap.put(intentname, templateScore);
			}
		}

		ArrayList<Map.Entry<String,Double>> sortedKeys = new ArrayList<>(goalScoreMap.entrySet());//排序决定选哪个模板意图
		sortedKeys.sort((p1, p2) -> {
			if (p1.getValue() > p2.getValue()) {
				return -1;
			} else if (Math.abs(p1.getValue() - p2.getValue()) < 0.0001) { //若相同优先返回槽个数少者
				return Integer.compare(Loadsource.goalMap.get(p1.getKey()).slotList.size(), Loadsource.goalMap.get(p2.getKey()).slotList.size());
			} else {
				return 1;
			}
		});
		  
	  if(sortedKeys.size()>0){  //选择分数最高的意图作为意图
	  	intentscore.template_name = sortedKeys.get(0).getKey();
	  	intentscore.score = sortedKeys.get(0).getValue();
	  }
	  return intentscore;
	}
	
}

