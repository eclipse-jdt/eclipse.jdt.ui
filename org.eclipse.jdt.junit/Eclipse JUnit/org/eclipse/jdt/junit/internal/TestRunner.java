/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.internal;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.Platform;

/**
 * Server that runs JUnit Tests.
 */
public class TestRunner extends org.eclipse.junit.internal.TestRunner {

	public static String fgTestPluginName= null;
	public static void main(String[] args) throws InvocationTargetException{
		TestRunner testRunServer= new TestRunner();
		testRunServer.init(args);
		testRunServer.run();
	}
	
	/**
	 * Returns the ClassLoader for loading the tests
	 */
	public ClassLoader getClassLoader() throws InvocationTargetException {
		if (Platform.getPluginRegistry().getPluginDescriptor(fgTestPluginName) != null)
			return Platform.getPluginRegistry().getPluginDescriptor(fgTestPluginName).getPluginClassLoader();	
		else
			throw new InvocationTargetException(new Exception("error: no ClassLoader found for testplugin: " + fgTestPluginName));
	}
	public void init(String[] args) throws InvocationTargetException{
		defaultInit(args);
		setTestPluginName(args);
	}

	private void setTestPluginName(String[] args) throws InvocationTargetException{
		for(int i= 0; i < args.length; i++) {
			if(args[i].toLowerCase().equals("-testpluginname")) {
				fgTestPluginName= args[i + 1];
				return;
			}
		}
		throw new InvocationTargetException(new Exception("error: parameter '-testPluginName' not specified"));
	}
}	