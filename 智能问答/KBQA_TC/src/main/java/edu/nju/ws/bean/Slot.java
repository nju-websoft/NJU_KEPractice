package edu.nju.ws.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;


public class Slot implements Cloneable, Comparable<Slot> {
	public String name;//槽名  ------------
	public List<String> relatetemplate; //该相关的模板
	public String value;//槽值(默认值)
	public List<String> valueList = new ArrayList<String>();//副槽值
	public List<String> IRIList = new ArrayList<String>();
	public double  score = 0;//槽值可信度

	/**
	 * 初始化填入槽名和相关模板名
	 * @param name
	 * @param relatetemplate
	 */
	public Slot(String name, List<String> relatetemplate) {
		this.name = name;
		this.relatetemplate = relatetemplate;
	}

	@Override
	public int compareTo(Slot o){
		double i = this.score - o.score;
		return (int) (i*10);	
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Slot slot = (Slot) o;
		return Double.compare(slot.score, score) == 0 &&
				Objects.equals(name, slot.name) &&
				Objects.equals(relatetemplate, slot.relatetemplate) &&
				Objects.equals(value, slot.value) &&
				Objects.equals(valueList, slot.valueList);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, relatetemplate, value, valueList, score);
	}

	public static String DisPlayList(Set<Slot> slotlist){
		StringBuilder message = new StringBuilder();
		for(Slot slot:slotlist){
			if(slot.valueList!=null&&slot.valueList.size()>0){
				StringBuilder str = new StringBuilder("    [" + slot.name);
				for(String value:slot.valueList){
					str.append(",").append(value);
				}
				str.append(" ").append(slot.score).append("]\n");
				message.append(str);
			}
		}
		return message.toString();
	}

	public boolean isEmpty(){
		if(value == null && (valueList ==null ||(valueList!=null&&valueList.size()<1))){
			return true;
		}else{
			return false;
		}
	}
}
