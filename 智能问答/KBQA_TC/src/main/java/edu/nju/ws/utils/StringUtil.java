package edu.nju.ws.utils;

import org.apache.commons.text.StringEscapeUtils;

import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

    /**
     * Unicode转 汉字字符串
     *
     * @param str \u6728
     * @return '木' 26408
    */
    public static String unicodeToString(String str) {

        Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
        Matcher matcher = pattern.matcher(str);
        char ch;
        while (matcher.find()) {
            String group = matcher.group(2);
            ch = (char) Integer.parseInt(group, 16);
            String group1 = matcher.group(1);
            str = str.replace(group1, ch + "");
        }
        return str;
    }

    /**
     * 过滤HTML标签输出文本
     *
     * @param inputString 原字符串
     * @return 过滤后字符串
     */
    public static String Html2Text(String inputString) {
        if (inputString==null || inputString.isEmpty()) {
            return "";
        }
        inputString = inputString.replaceAll("&nbsp;&nbsp;&nbsp;&nbsp;","\t"); //对四个空格特殊处理
        inputString = StringEscapeUtils.unescapeHtml4(inputString); //反转义
        inputString = inputString.replaceAll("<span>[ ]*?</span>", "\t")
                .replaceAll("[\t]+","\t");
        String lt = "<";
        String gt = ">";

        // 含html标签的字符串
        String htmlStr = inputString.trim();
        String textStr = "";
        Pattern p_script;
        Matcher m_script;
        Pattern p_style;
        Matcher m_style;
        Pattern p_html;
        Matcher m_html;
        Pattern p_escape;
        Matcher m_escape;
        Pattern p_br;
        Matcher m_br;

        try {
            // 定义script的正则表达式{或<script[^>]*?>[\\s\\S]*?<\\/script>
            String regEx_script = lt + "[\\s]*?script[^"+ gt+"]*?" + gt + "[\\s\\S]*?" + lt + "[\\s]*?\\/[\\s]*?script[\\s]*?" + gt;

            // 定义style的正则表达式{或<style[^>]*?>[\\s\\S]*?<\\/style>
            String regEx_style = lt + "[\\s]*?style[^"+ gt+"]*?" + gt + "[\\s\\S]*?" + lt + "[\\s]*?\\/[\\s]*?style[\\s]*?" + gt;

            // 定义HTML标签的正则表达式
            String regEx_html = lt + "[^>]+" + gt;

            // 定义转义字符
            String regEx_escape = "&.{2,6}?;";

            //换行符
            String regEx_br = lt + "br[\\s]*?[/]?" + gt;

            // 过滤script标签
            p_script = Pattern.compile(regEx_script, Pattern.CASE_INSENSITIVE);
            m_script = p_script.matcher(htmlStr);
            htmlStr = m_script.replaceAll("");

            // 过滤style标签
            p_style = Pattern.compile(regEx_style, Pattern.CASE_INSENSITIVE);
            m_style = p_style.matcher(htmlStr);
            htmlStr = m_style.replaceAll("");

            //转换换行符
            p_br = Pattern.compile(regEx_br, Pattern.CASE_INSENSITIVE);
            m_br = p_br.matcher(htmlStr);
            htmlStr = m_br.replaceAll("\n");
//            System.out.println(htmlStr);

            // 过滤html标签
            p_html = Pattern.compile(regEx_html, Pattern.CASE_INSENSITIVE);
            m_html = p_html.matcher(htmlStr);

            htmlStr = m_html.replaceAll("");

            // 过滤转义字符
            p_escape = Pattern.compile(regEx_escape, Pattern.CASE_INSENSITIVE);
            m_escape = p_escape.matcher(htmlStr);
            htmlStr = m_escape.replaceAll("");

            textStr = htmlStr;

            textStr = textStr.replaceAll("nbsp;"," ")
                    .replaceAll("[\n]+","\n")
                    .replaceAll("\n$",""); //去除多余换行符
        } catch (Exception e) {
            System.out.println("Html2Text:" + e.getMessage());
        }
        // 返回文本字符串
        return textStr;
    }

    /**
     * 从mysql读取模板时，将html标签反转义
     * @param input
     * @return
     */
    public static String HtmlZ(String input){
        if(input==null)
            return "";
        return StringEscapeUtils.unescapeHtml4(input);
    }

    /**
     *  获取question中所有的token，结果用大到小排序
     * @param question
     * @return
     */
    public static Vector<String> get_ngram(String question){
        //以字为单位
        String[] tokens = question.toLowerCase().replaceAll("([\u4e00-\u9fa5])", " $1 ")
                .replaceAll("[ ]{2,}"," ").split(" ");//去除多余空格，然后以空格切词
//        List<String> tokens = segmentByStanford(question); //以分词结果为单位进行索引建立

        Vector<String> ngram = new Vector<String>();
        for(int i=0; i<tokens.length; i++){
            for(int j=0; j<=i; j++){
                if(i-j<=8){  //将最长的token设为8吧
                    StringBuffer sb = new StringBuffer();
                    for(int k=j; k<=i; k++){
                        if(k==i)
                            sb.append(tokens[k]);
                        else
                            sb.append(tokens[k]);
                    }
                    if(!ngram.contains(sb.toString()))
                        ngram.add(sb.toString());
                }
            }
        }
        //将结果以元素长度进行排序
        ngram.sort((left, right) -> (right.length() - left.length()));
        return ngram;
    }

    /**
     * 问句的预处理
     * 每次得到问句就调用
     * @param question
     * @return
     */
    public static String question_handle(String question){
        return question .replaceAll("[,，()（）\"‘“”、\\-/：:?？_]", "")  // 替换标点
                .replaceAll("[ ]",""); //移除空格
    }

    public static String trim(String input){
        if(input==null)
            return null;
        else
            return input.trim();
    }

}
