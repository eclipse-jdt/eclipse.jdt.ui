package org.eclipse.jdt.testplugin.ui;

import org.eclipse.jdt.testplugin.TestPluginLauncher;

public class TestPluginUILauncher extends TestPluginLauncher {
	
	public static void run(String location, Class testCase, String[] args) {
		TestPluginLauncher.run(JavaTestUIApplication.APP_NAME, location, testCase, args);
	}
}