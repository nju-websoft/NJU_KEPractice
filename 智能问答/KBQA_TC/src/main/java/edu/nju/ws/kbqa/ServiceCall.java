package edu.nju.ws.kbqa;

import com.alibaba.fastjson.JSONObject;
import edu.nju.ws.bean.Template;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import static edu.nju.ws.database.Virtuoso_qu.Prefix;
import static edu.nju.ws.database.Virtuoso_qu.vg;


public class ServiceCall {


	//填完槽值后匹配pattern查询结果
	public static List<String> TaskServe(String sentence, Template template){
		List<String> replys = new ArrayList<>();

		//若该模板有regex验证
		String sen = sentence;
		if(!template.regex.isEmpty()){
			//将问句中相应的mention覆盖掉
			for(String slotname: template.slotList) { //该pattern相关的槽
				for(String mention:template.slotmap.get(slotname).valueList){
					sen = sen.replace(mention,"<" + slotname + ">");
				}
			}
			Pattern regex = Pattern.compile(template.regex);
			if(!regex.matcher(sen).matches()){ //若不匹配
				return replys;
			}
		}

		//若regex为空，或者匹配regex，调用sparql
		//填充sparql需要的参数
		int[] len = new  int[template.slotList.size()];
		for(int i=0; i< template.slotList.size(); i++){
			String slotname = template.slotList.get(i); //槽名
			len[i] = template.slotmap.get(slotname).valueList.size(); //各槽值个数
		}

		//获取排列组合个数， 正常情况下 只有一种组合
		List<List<Integer>> middle = new ArrayList<>();
		for(int i=0; i< template.slotList.size(); i++) {
			if (middle.size() == 0){
				for (int k = 0; k < len[i]; k++) {
					List<Integer> t = new ArrayList<>();
					t.add(k);
					middle.add(t);
				}
			}else {
				List<List<Integer>> middle_temp = new ArrayList<>();
				for (List<Integer> aMiddle : middle) {
					for (int k = 0; k < len[i]; k++) {
						List<Integer> temp = new ArrayList<>(aMiddle);
						temp.add(k);
						middle_temp.add(temp);
					}
				}
				middle = middle_temp;
			}
		}

		for(List<Integer> zuhe:middle){
			//对于合法的组合情况，填槽，查询
			String temp_sparql = Prefix + " " + template.sparql;
			String temp_reply = template.reply;
			for(int j=0; j<template.slotList.size(); j++){
				String slotname = template.slotList.get(j);
				String slotvalue = template.slotmap.get(slotname).valueList.get(zuhe.get(j)); //展示的值
				String slotIRI = template.slotmap.get(slotname).IRIList.get(zuhe.get(j)); //搜索的值
				temp_sparql = temp_sparql.replaceAll("<" + slotname + ">",slotIRI);
				temp_reply = temp_reply.replaceAll("<" + slotname + ">",slotvalue);
			}
			System.out.println(temp_sparql);
			//查询
			VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(temp_sparql, vg);
			ResultSet results = vqe.execSelect();
			List<String> temp_replys = new ArrayList<>();
			HashSet<String> temp_mysqlid = new HashSet<>(); //一句回答，可能综合了多个mysqlid知识
			while (results.hasNext()) {
				String t_temp_reply = temp_reply;
				QuerySolution result = results.nextSolution();
				for(String pa: template.param) {
					RDFNode sub = result.get(pa);
					if(sub==null)
						break;
					String value = sub.toString().split("\\^\\^")[0].replaceAll("\"","");
					t_temp_reply = t_temp_reply.replaceAll("#"+pa,value);
				}
				if(!t_temp_reply.isEmpty())
					temp_replys.add(t_temp_reply);
			}
			if(temp_replys.size()>0) {
				JSONObject t = new JSONObject();
				t.put("answer", String.join("\n", temp_replys));
				replys.add(t.toJSONString());
			}
			vqe.close();
		}
		return replys;
	}

}
