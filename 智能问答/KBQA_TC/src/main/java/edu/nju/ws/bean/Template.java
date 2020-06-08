package edu.nju.ws.bean;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Template {
	public String name;  //模板name
	public Map<String, Slot> slotmap = new HashMap<String,Slot>(); //该模板有哪些槽
	public List<String> Triggers = new ArrayList<String>();  //该模板相关的触发词
	public List<String> slotList = new ArrayList<String>();
	public String regex; //该模板对应的正则表达式
	public String sparql; //该模板调用的sparql语句
	public List<String> param = new ArrayList<>(); //该sparql中可获取的参数
	public String info;
	public String reply; //该pattern返回的结果
		
	/**
	 * 
	 * @return 找到的还没有填入值的槽名，如果填满则返回空
	 */
	public String getEmptyslot(){
		try{
			for(String key:slotmap.keySet()){
				if(slotmap.get(key).isEmpty()){
					return key;
				}
			}
		}catch(Exception e){
			System.out.println("slotmap is null");
		}
		return null;	
	}
	
	/**
	 * 填槽
	 * @param slot
	 */
	public void fillslot(Slot slot){
		try{
			slotmap.put(slot.name, slot);
		}catch(Exception e){
			System.out.println(e.getMessage());
		}
	}

}

























