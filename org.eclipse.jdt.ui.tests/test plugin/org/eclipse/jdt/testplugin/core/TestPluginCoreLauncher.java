package org.eclipse.jdt.testplugin.core;

import java.net.URL;import org.eclipse.jdt.testplugin.NewMain;import org.eclipse.jdt.testplugin.TestPluginLauncher;

public class TestPluginCoreLauncher extends TestPluginLauncher {
	
	public static void run(String location, Class testCase, String[] args) {
		TestPluginLauncher.run(JavaTestCoreApplication.APP_NAME, location, testCase, args);
	}	
}