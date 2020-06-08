package edu.nju.ws.kbqa;

import edu.nju.ws.bean.Slot;
import edu.nju.ws.bean.TemplateScore;
import edu.nju.ws.bean.Template;
import edu.nju.ws.database.Virtuoso_qu;
import edu.nju.ws.utils.StringUtil;
import edu.nju.ws.utils.TextSimilarity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static edu.nju.ws.utils.StringUtil.question_handle;


/**
 * 简单的基于模板的KBQA入口
 * 过程大致分为：槽识别、填槽、转sparql查询
 */
public class Talk {

    /**
     * 问答接口
     * @param sentence
     * @return
     */
    public String SmartResponse(String sentence){
        String reply = null;
        String intent_name = null;

        HashMap<String, Integer> goallist = LU.getgoal(sentence);  //识别问句中各模板触发词个数
        Set<Slot> slotlist = LU.getslot(sentence); //得到可能的槽值对列表
        System.out.println("槽识别：\n" + Slot.DisPlayList(slotlist));

        TemplateScore templateScore;
        templateScore = TemplateManage.TempalteChoose(goallist, slotlist);
        System.out.println("模板识别" + templateScore.template_name);

        Template template = Loadsource.goalMap.get(templateScore.template_name);

        //填槽
        for(Slot slot:slotlist){
            template.fillslot(slot);
        }
        //根据已有槽，配比pattern给出答案
        List<String> replys = ServiceCall.TaskServe(sentence, template);
        reply = replys.size()==0?null:replys.get(0); //给出结果
        return reply;
    }


    public static void main(String args[]){
        try {
            Virtuoso_qu.init();     //静态初始化    (所有静态只需加载一次，无须24h调用)
		    TextSimilarity.init(); //词向量 静态资源
            Loadsource.InitLoad();
            Talk talk = new Talk();
            System.out.println("您好,请问需要什么帮助吗?");
            while (true) {
                System.out.print("::  ");
                BufferedReader input = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
                String sentence = input.readLine();
                sentence = question_handle(sentence); //问句预处理
                String reply = "";
                try {
                    reply = talk.SmartResponse(sentence);
                } catch (Exception o) {
                    o.printStackTrace();
                }
                System.out.print(">>  " + reply + "\n");
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
