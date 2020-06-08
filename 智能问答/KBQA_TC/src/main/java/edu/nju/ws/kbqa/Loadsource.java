package edu.nju.ws.kbqa;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import edu.nju.ws.bean.Entity;
import edu.nju.ws.bean.Slot;
import edu.nju.ws.bean.Template;
import edu.nju.ws.database.Virtuoso_qu;
import edu.nju.ws.utils.FileUtil;
import edu.nju.ws.utils.StringUtil;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

import java.util.*;

import static edu.nju.ws.database.Virtuoso_qu.Prefix;
import static edu.nju.ws.database.Virtuoso_qu.vg;
import static edu.nju.ws.utils.FileUtil.ReadFileString;

/**
 * 该类用于初始化加载资源，主要加载对实体、属性信息，用于实体识别链接、属性识别链接（即槽识别）
 */
public class Loadsource {
	public static HashMap<String, Template> goalMap = new HashMap<String, Template>();
	public static HashMap<String, List<String>> slotMap = new HashMap<>(); //槽名到模板的映射,一个槽名可能对应多个模板

	public static Set<Entity> all_entity_pro_info = new HashSet<>(); //初始的所有实体和属性信息(typed),混杂
	public static Map<String, Map<String, Set<Entity>>> typed_entity_inverted_index = new HashMap<>(); //根据槽类型划分的实体倒排索引
	public static Map<String, Map<String, Set<Entity>>> typed_property_inverted_index = new HashMap<>(); //根据槽类型划分的属性倒排索引

	public static Map<String, Set<Entity>> typed_propertys = new HashMap<>(); //不同槽下的属性
	public static Map<String, Set<Entity>> template_propertys = new HashMap<>(); //不同模板下的属性

	public static List<String> stopWordList; //停用词列表
	public static List<String> pronounWordList; //指代词列表


	public static void InitLoad(){
		loadgoalmap();  //加载模板文件
		loadentity();  //加载实体文件 + 同义词
		loadIndex();   //建立简单索引 用于实体识别和链接
	}
	
	
	private static void loadgoalmap(){
		//加载模板文件
		String jsonIntentData = ReadFileString("data/QA/template_example.json");
		JSONArray templates = JSON.parseArray(jsonIntentData);
		for(Object tem: templates){
			JSONObject tem1 = (JSONObject) tem;
			String name = tem1.getString("name");
			Template tems = JSONObject.toJavaObject(tem1, Template.class);
			goalMap.put(name, tems); //以模板名为key,存入对应模板
		}
		//加载其他文件
		pronounWordList = FileUtil.ReadFileList("data/QA/pronounWord.txt");  //指代词
		stopWordList = FileUtil.ReadFileList("data/QA/stopword.txt");  //停用词
		//添加槽到模板的映射，一个槽可能对应多个值;
		for(String key:goalMap.keySet()){
			for(String slot:goalMap.get(key).slotList){
				if(!slotMap.containsKey(slot)) {
					slotMap.put(slot, new ArrayList<>());
					slotMap.get(slot).add(goalMap.get(key).name);
				}else{
					if(!slotMap.get(slot).contains(goalMap.get(key).name)) //去重
						slotMap.get(slot).add(goalMap.get(key).name);
				}
			}
		}
		for(String goal:goalMap.keySet()){  //将slotMap里面的槽加载进goalMap里面去
			for(String slotkey:goalMap.get(goal).slotList){
				goalMap.get(goal).slotmap.put(slotkey, new Slot(slotkey,slotMap.get(slotkey)));
			}
		}
	}

	/**
	 * 加载预定义的模板所需槽实体
	 */
	private static void loadentity() {
		//加载实体/属性同义词
		load_synonym();

		String slot2class = ReadFileString("data/QA/slot_class_map.json"); //槽值类型和数据库类型对应文件
		JSONObject slot2class_json = JSONObject.parseObject(slot2class);
		for(String key:slot2class_json.keySet()) { //遍历各槽值
			if (slot2class_json.getJSONObject(key).getJSONArray("class") != null){
				for (Object classname_o : slot2class_json.getJSONObject(key).getJSONArray("class")) { //从知识库加载该类所有实体
					String classname = (String) classname_o;
					String sparql = Prefix + " select distinct ?sub ?o0 where { ?sub rdf:type " + classname + " ." +
							" ?sub rdfs:label ?o0. }";
					VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, vg);
					ResultSet results = vqe.execSelect();
					while (results.hasNext()) {
						QuerySolution result = results.nextSolution();
						RDFNode sub = result.get("sub");
						RDFNode o0 = result.get("o0");
						Entity e1 = new Entity("<" + sub.toString() + ">", o0.toString(), key + "_name");
						all_entity_pro_info.add(e1);
					}
					vqe.close();
				}
			}
			if(slot2class_json.getJSONObject(key).getJSONArray("property")!=null) { //该槽值相关属性
				for (Object property_name_o : slot2class_json.getJSONObject(key).getJSONArray("property")){
					if(property_name_o==null ||property_name_o.toString().split("#").length <= 1)
						continue;
					Entity e2 = new Entity(property_name_o.toString(), property_name_o.toString().split("#")[1]
							.replace(">",""), key + "_property");
					all_entity_pro_info.add(e2);
					if(!typed_propertys.containsKey(key + "_property"))
						typed_propertys.put(key + "_property", new HashSet<>());
					typed_propertys.get(key + "_property").add(e2);
					if(slotMap.containsKey(key + "_property")){
						for (String template : slotMap.get(key + "_property")) { //遍历该槽值拥有的模板
							if (!template_propertys.containsKey(template))
								template_propertys.put(template, new HashSet<>());
							template_propertys.get(template).add(e2);
						}
					}
				}
			}
		}
	}

	/**
	 * 对所有实体和属性建立索引
	 */
	private static void loadIndex(){
		Map<String, Map<String, Set<Entity>>> inverted_index = null;
		for(Entity item: all_entity_pro_info){
//			mylogger.info(item.toString());
			if(item.type.endsWith("_name")) {
				inverted_index = typed_entity_inverted_index;
			}else {
				inverted_index = typed_property_inverted_index;
			}

			Map<String, Set<Entity>> temp = null;
			if(inverted_index.containsKey(item.type)){
					temp = inverted_index.get(item.type);
			}else{
					temp = new HashMap<>();
			}
			Vector<String> tokens = StringUtil.get_ngram(item.label); //获取实体label的tokens
			for (String token : tokens) {  //tokens 中已过滤标点符号
				if (token.trim().isEmpty()) //跳过空字符
					continue;
				Set<Entity> t = null;
				if (!temp.containsKey(token))
					t = new HashSet<>();
				else
					t = temp.get(token);
				t.add(item);
				temp.put(token, t);
			}
			inverted_index.put(item.type,temp);
		}
	}

	/**
	 * 加载实体/属性同义词
	 */
	private static void load_synonym(){
		try{
			List<String> lines = FileUtil.ReadFileList("data/QA/entity_Synonym.txt");
			for(String line:lines){
				String[] items = line.split("\t");
				if(items[0].endsWith("_name") && items.length > 3){
					for(int i=3; i<items.length; i++) {
						Entity e1 = new Entity(items[1], items[i], items[0]);
						all_entity_pro_info.add(e1);
					}
				}
			}
			lines = FileUtil.ReadFileList("data/QA/property_Synonym.txt");
			for(String line:lines){
				String[] items = line.split("\t");
				if(items[0].endsWith("_property") && items.length >= 3){
					for(int i=2; i<items.length; i++) {
						Entity e1 = new Entity(items[1], items[i], items[0]);
						all_entity_pro_info.add(e1);
						//将属性同义词加入Intent_propertys中
						if (!typed_propertys.containsKey(e1.type))
							typed_propertys.put(e1.type, new HashSet<>());
						typed_propertys.get(e1.type).add(e1);
						if (slotMap.containsKey(e1.type)) {
							for (String template : slotMap.get(e1.type)) { //遍历该槽值拥有的模板
								if (!template_propertys.containsKey(template))
									template_propertys.put(template, new HashSet<>());
								template_propertys.get(template).add(e1);
							}
						}
					}
				}
			}
		}catch (Exception e){
			e.printStackTrace();
		}
	}


	public static void main(String[] args){
		try {
			Virtuoso_qu.init();
			InitLoad();
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	

		
}











