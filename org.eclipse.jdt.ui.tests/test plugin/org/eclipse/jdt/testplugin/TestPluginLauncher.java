/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.testplugin;import java.net.URL;
public class TestPluginLauncher {
	
	public static void run(String application, String location, Class testCase, String[] args) {
		try {
			String bootLocation= getBootLocation();
			String[] newArgs= new String[args.length + 3];
			System.arraycopy(args, 0, newArgs, 2, args.length);
			newArgs[0]= "-dev";
			newArgs[1]= "bin";
			newArgs[args.length + 2]= testCase.getName();
			args= newArgs;
			NewMain newMain= new NewMain(application, location, null, bootLocation, false);
			newMain.run(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String getLocationFromProperties(String key) {
		return NewMain.getLocationFromProperties(key);
	}
	
	public static String getLocationFromProperties() {
		return NewMain.getLocationFromProperties("tests");
	}
	
	public static String getBootLocation() {
		URL url= TestPluginLauncher.class.getResource("TestPluginLauncher.class");
		String s= url.toString();
		int index= s.indexOf("/org.eclipse.jdt.ui.tests");
		if (index == -1)
			throw new IllegalArgumentException();
		s= s.substring(0, index);
		s= s + "/org.eclipse.core.boot/boot.jar";
		return s;
	}
}