package edu.nju.ws.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {
	public static String ReadFileString(String path){
		 StringBuilder laststr= new StringBuilder();
		 BufferedReader reader=null;
		 InputStream in = null;
		 try{
		 	in = FileUtil.class.getClassLoader().getResourceAsStream(path);
		 	reader=new BufferedReader(new InputStreamReader(in,"UTF-8"));// 读取文件
			 String tempString=null;
			 while((tempString=reader.readLine())!=null){
			 	laststr.append(tempString.trim()); //每行去除首尾空格
			 }
			 reader.close();
		 }catch(IOException e){
		 	e.printStackTrace();
		 }finally{
		 	if(reader!=null){
		 		try{
		 			reader.close();
		 		}catch(IOException el){
		 		}
		 	}
		 }
		 return laststr.toString();
	}
	
	public static List<String> ReadFileList(String path){
        List<String> strList = new ArrayList<String>();
        BufferedReader reader=null;
        try{
            reader=new BufferedReader(new InputStreamReader(FileUtil.class.getClassLoader().getResourceAsStream(path),"UTF-8"));// 读取文件
            String tempString=null;
            while((tempString=reader.readLine())!=null){
            	tempString.replace("\r\n","");
                strList.add(tempString);
            }
            reader.close();
        }catch(IOException e){
            e.printStackTrace();
        }finally{
            if(reader!=null){
                try{
                    reader.close();
                }catch(IOException el){
                }  
            }  
        }
        return strList;
	}
	
	public static void writeFile(String fileName, String fileContent) {
	    try {
	      File f = new File(fileName);
	      if (!f.exists()) {
	        f.createNewFile();
	      }
	      OutputStreamWriter write = new OutputStreamWriter(new FileOutputStream(f), "utf-8");
	      BufferedWriter writer = new BufferedWriter(write);
	      writer.write(fileContent);
	      writer.close();
	    } catch (Exception e) {
	      e.printStackTrace();
	    }
	  }
	
	
}
