package edu.nju.ws.utils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

public class TextSimilarity {
	
	private static Map<String,Vector<Double>> wordvector = null; //词向量文件
	private static HashMap<String,Integer> proper_Synonym = new HashMap<>(); //用于属性链接的同义词
	
	public static void init(){
		loadVectorMap("data/QA/hanlp-wiki-vec-zh.txt");  //加载词向量文件
		InputStream is = null;
		Reader r = null;
		BufferedReader br = null;
		wordvector = new HashMap<String,Vector<Double>>(); //最终词向量
		try {
			is = TextSimilarity.class.getClassLoader().getResourceAsStream("data/QA/word_Synonym.txt");
			r = new InputStreamReader(is, "utf-8");
			br = new BufferedReader(r);

			String line = br.readLine();
			int id = 0;
			while((line = br.readLine())!=null){
				for(String item:line.split("##")){
					proper_Synonym.put(item, id);
				}
				id++;
			}
		}catch (IOException E){
			E.printStackTrace();
		}
	}

	/**
	 * 将文本中的标点符号进行省略
	 * @param obj
	 * @return
	 */
	public static String texthandle(String obj){
		char punct[] = {',','.','!','?','。','，','、','！',';','；',':','：','《','》','<','>','？','‘','’','/',' '};
		String result="";
		int startt = 0;
		for( int j=0;j<obj.length();j++){
			boolean need_filter = false;
			for(int k=0; k <punct.length;k++){
				if(punct[k] == obj.charAt(j)){
					need_filter = true;
					break;
				}
			}
			if(need_filter){
				String tempstr = obj.substring(startt,j);
				startt = j+1;
				result+=tempstr;
			}
		}
		String tempstr = obj.substring(startt);
		result+=tempstr;
		return result;
	}
	
	/**
	 * 默认增加、删除 代价为1，替换代价都为2，编辑距离
	 * @param target
	 * @param source
	 * @return
	 */
	public static double cmpDistance(String target, String source){
		int n = target.trim().length();
		int m = source.trim().length();
		int[][] dis = new int[n+1][m+1];//dis[i][j]标识target[1-i]与source[1-j]之间所需最小编辑距离
		dis[0][0] = 0;
		for(int i = 1;i <= m;i++)
			dis[0][i] = i;
		for(int i = 1;i <= n; i++)
			dis[i][0] = i;
		for(int i = 1;i <= n ;i++){
			for(int j =1; j <= m ;j++){
				int d;
				int temp = Math.min(dis[i-1][j]+1, dis[i][j-1]+1);
				if(target.charAt(i-1) == source.charAt(j-1)){
					d = 0;
				}else{
					d = 2;
				}
				dis[i][j] = Math.min(temp,dis[i-1][j-1] + d);
			}
		}
		int distance =  dis[n][m];
		double simaliar = 1-distance*1.0/(n+ m);
		if(source.length() == 1 ||target.length() == 1 //对于单个字的相似度计算，进行惩戒
				){ //对于坦克进行惩戒
			if(simaliar < 1.0)
				simaliar = Math.min(0.4, simaliar);
		}
		return simaliar;
	}
	
	/**
	 * 加载词向量文件
	 * @param modelFileName
	 */
	private static void loadVectorMap(String modelFileName){
        InputStream is = null;
        Reader r = null;
        BufferedReader br = null;
        wordvector = new HashMap<String,Vector<Double>>(); //最终词向量
        try{

			is = TextSimilarity.class.getClassLoader().getResourceAsStream(modelFileName);
            r = new InputStreamReader(is, "utf-8");
            br = new BufferedReader(r);

            String line = br.readLine();
            int words = Integer.parseInt(line.split("\\s+")[0].trim()); //词汇数目
            int size = Integer.parseInt(line.split("\\s+")[1].trim()); //词向量维度

            String[] vocab = new String[words];
            double[][] matrix = new double[words][];

            for(int i = 0; i < words; i++){
                line = br.readLine().trim();
                String[] params = line.split("\\s+");
                if (params.length != size + 1){
                    System.out.println("词向量有一行格式不规范（可能是单词含有空格）：" + line);
                    --words;
                    --i;
                    continue;
                }
                vocab[i] = params[0];
                matrix[i] = new double[size];
                double len = 0;
                for (int j = 0; j < size; j++){
                    matrix[i][j] = Double.parseDouble(params[j + 1]);
                    len += matrix[i][j] * matrix[i][j];
                }
                len = Math.sqrt(len);
                for (int j = 0; j < size; j++){
                    matrix[i][j] /= len;  //对词向量进行归一化，便于余弦相似度计算
                }
            }
            for(int i=0; i<words; i++){
            	Vector<Double> vec  = new Vector<Double>();
            	for(double a : matrix[i])
            		vec.add(a);
            	wordvector.put(vocab[i], vec);
            }
        }catch(Exception e){
        	e.printStackTrace();
        }
    }
	
	/**
	 * 基于语义vec，返回两个词语之间语义相似度
	 */
	public static double pharseSimilarity(String text1, String text2){
		if(proper_Synonym.containsKey(text1) && proper_Synonym.containsKey(text2) &&
				proper_Synonym.get(text1).equals(proper_Synonym.get(text2))){
			return 1.0;
		}
		if(wordvector == null){
			loadVectorMap("data/QA/hanlp-wiki-vec-zh.txt");  //加载词向量文件
		}
		Vector<Double> vectortext1 = wordvector.get(text1);
        if (vectortext1 == null){
            return -1.0;
        }
        Vector<Double> vectortext2 = wordvector.get(text2);
        if (vectortext2 == null){
            return -1.0;
        }
        double ret = 0.0;
        for(int i=0; i<vectortext1.size(); ++i){
            ret += vectortext1.get(i) * vectortext2.get(i); //向量已经归一化
        }
        return ret;
	}
	
	/**
	 * 根据最长公共子序列求相似度
	 */
	public static double getLCS(String str1, String str2) {
		str1 = str1.replaceAll(" ","").toLowerCase();
		str2 = str2.replaceAll(" ","").toLowerCase();
		char[] c1 = str1.toCharArray();
		char[] c2 = str2.toCharArray();
		if(str1.length()==0||str2.length()==0)
			return 0.0;
		int[][] length = new int[c1.length][c2.length];
		for (int i = 0; i < c1.length; i++) {
			for (int j = 0; j < c2.length; j++) {
				if (c1[i] == c2[j]) {
					if (i == 0 || j == 0) {
						length[i][j] = 1;
					} else {
						length[i][j] = length[i - 1][j - 1] + 1;
					}
				} else {
					if (i == 0 && j == 0) {
						length[i][j] = 0;
					} else if (i == 0 && j != 0) {
						length[i][j] = length[i][j - 1];
					} else if (i != 0 && j == 0) {
						length[i][j] = length[i - 1][j];
					} else {
						length[i][j] = Math.max(length[i - 1][j], length[i][j - 1]);
					}
				} 
			}
		}
		
		double score = length[c1.length-1][c2.length-1]*1.0 / Math.max(c1.length, c2.length);

		if(str1.length() == 1 ||str2.length() == 1 //对于单个字的相似度计算，进行惩戒
				||isNUM(str1)||isNUM(str2)){
			if(score < 1.0)
				score = Math.min(0.33, score);
		}
		return score;
	}

	private static boolean isNUM(String str){
		Pattern pattern = Pattern.compile("[0-9]*");
		return pattern.matcher(str).matches();
	}
	public static void main(String[] args){
		String a="驻军", b = "驻军";
		System.out.println(pharseSimilarity(a,b));
	}
}
