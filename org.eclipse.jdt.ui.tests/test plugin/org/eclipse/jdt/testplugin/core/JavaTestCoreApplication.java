package org.eclipse.jdt.testplugin.core;

import junit.textui.TestRunner;

import org.eclipse.core.boot.IPlatformRunnable;

public class JavaTestCoreApplication implements IPlatformRunnable {

	public static final String APP_NAME= "org.eclipse.jdt.testplugin.test";

	public Object run(Object arguments) throws Exception {
		if (arguments instanceof String[]) {
			TestRunner.main((String[])arguments);
		}
		return null;
	}
}