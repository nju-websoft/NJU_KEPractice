package edu.nju.ws.bean;

/**
 * 问句实体/属性识别的 返回结果数据格式
 */
public class slotMode {
	public Entity entity; //实体或属性
	public String mention; // 实体在问句中的提及词
	public int startindex; // 起点
	public int endindex;  // 终点
	public double score; //分数
	
	public slotMode(){
		super();
	}

	public slotMode(Entity entity, String mention, int startindex, int endindex, double score) {
		this.entity = entity;
		this.mention = mention;
		this.startindex = startindex;
		this.endindex = endindex;
		this.score = score;
	}

	@Override
	protected slotMode clone() {
		return new slotMode(this.entity,this.mention,this.startindex,this.endindex,this.score);
	}

	@Override
	public String toString() {
		return "resultMode{" +
				"entity=" + entity +
				", mention='" + mention + '\'' +
				", startindex=" + startindex +
				", endindex=" + endindex +
				", score=" + score +
				'}';
	}
}
