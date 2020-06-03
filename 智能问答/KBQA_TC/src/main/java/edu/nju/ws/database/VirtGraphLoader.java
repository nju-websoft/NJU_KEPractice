package edu.nju.ws.database;

public class VirtGraphLoader {
	private static String url;
	private static String user;
	private static String password;
	public static String getUrl() {
		return url;
	}
	public static String getUser() {
		return user;
	}
	public static String getPassword() {
		return password;
	}
	public static void setUrl(String url) {
		VirtGraphLoader.url = url;
	}
	public static void setUser(String user) {
		VirtGraphLoader.user = user;
	}
	public static void setPassword(String password) {
		VirtGraphLoader.password = password;
	}
}
