/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.internal;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.boot.IPlatformRunnable;
import org.eclipse.jdt.junit.internal.TestRunner;

public class TestApplication implements IPlatformRunnable {

	/** true if workspace tests should log their deltas */
	private static boolean deltas = false;
	
	/**
	 * Runs a set of tests as defined by the given command line args.
	 * This is the platform application entry point.
	 * @see IPlatformRunnable
	 */
	public Object run(Object arguments) throws Exception {
		String[] args= processCommandLine((String[]) arguments);
		try {
			TestRunner.main(args);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean deltasEnabled() {
		return deltas;
	}
		
	protected String[] processCommandLine(String[] args) {
		int[] configArgs = new int[100];
		configArgs[0] = -1; // need to initialize the first element to something that could not be an index.
		int configArgIndex = 0;
		for (int i = 0; i < args.length; i++) {
			boolean found = false;
			// check for args without parameters (i.e., a flag arg)
			// see if we should be logging deltas
			if (args[i].equalsIgnoreCase("-deltas")) {
				found = true;
				deltas = true;
			}
			if (found) {
				configArgs[configArgIndex++] = i;
				continue;
			}
	
			// check for args with parameters
			if (i == args.length - 1 || args[i + 1].startsWith("-")) {
				continue;
			}
			String arg = args[++i];

			// done checking for args.  Remember where an arg was found 
			if (found) {
				configArgs[configArgIndex++] = i - 1;
				configArgs[configArgIndex++] = i;
			}
		}
	
		//remove all the arguments consumed by this argument parsing
		if (configArgIndex == 0)
			return args;
		String[] passThruArgs = new String[args.length - configArgIndex];
		configArgIndex = 0;
		int j = 0;
		for (int i = 0; i < args.length; i++) {
			if (i == configArgs[configArgIndex])
				configArgIndex++;
			else
				passThruArgs[j++] = args[i];
		}
		return passThruArgs;
	}
}
