package edu.nju.ws.database;
import virtuoso.jena.driver.*;
import java.io.IOException;


/**
 * 执行Virtuoso上面的查询、插入操作
 */
public class Virtuoso_qu {
    public static VirtGraph vg;
    private static boolean flag = true;
    public static String Prefix;

    public static VirtGraph getVg() {
        return vg;
    }
    public static void init() throws IOException {
        if(flag) {
            Configure.init();
            Prefix = Configure.tcqaPrefix + " " + Configure.OwlPrefix + " " + Configure.RdfsPrefix + " " + Configure.RdfPrefix;
            String url = VirtGraphLoader.getUrl();
            String usr = VirtGraphLoader.getUser();
            String psd = VirtGraphLoader.getPassword();
            vg = new VirtGraph(Configure.tcqaVirtGraph,url,usr,psd);
            flag = false;
        }
    }
    public static void main(String[] args){
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
