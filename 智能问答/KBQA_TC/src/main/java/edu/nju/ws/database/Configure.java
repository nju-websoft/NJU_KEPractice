package edu.nju.ws.database;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class Configure {
	private static String VirtuosoServerUrl;
    private static String VirtuosoServerUser;
    private static String VirtuosoServerPassword;
    public static String tcqaVirtGraph;  //对应Ontology IRI
    protected static String tcqaPrefix;   //实例的prefix
    protected static String RdfsPrefix;
    protected static String OwlPrefix;
    protected static String RdfPrefix;
	private static boolean init_flag = false;
	public static void init() throws IOException {
		if(init_flag)
			return;
        Properties pro = new Properties();
        InputStreamReader in = new InputStreamReader(Configure.class.getClassLoader()
                .getResourceAsStream("data/virtuoso_config.properties"),"utf-8");
        pro.load(in);
        in.close();
        VirtuosoServerUrl = pro.getProperty("VirtuosoServerUrl");
        VirtuosoServerUser = pro.getProperty("VirtuosoServerUser");
        VirtuosoServerPassword = pro.getProperty("VirtuosoServerPassword");
        tcqaVirtGraph = pro.getProperty("tcqaVirtGraph");
        tcqaPrefix = pro.getProperty("tcqaPrefix");
        RdfsPrefix = pro.getProperty("RdfsPrefix");
        OwlPrefix = pro.getProperty("OwlPrefix");
        RdfPrefix = pro.getProperty("RdfPrefix");
        VirtGraphLoader.setUrl(VirtuosoServerUrl);
        VirtGraphLoader.setUser(VirtuosoServerUser);
        VirtGraphLoader.setPassword(VirtuosoServerPassword);
        init_flag = true;
	}
}
