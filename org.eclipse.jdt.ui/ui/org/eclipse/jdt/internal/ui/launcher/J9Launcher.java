/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.launcher;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IProcess;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jdt.launching.VMRunnerResult;

public class J9Launcher extends JavaLauncher {
	
	private static final String PREFIX= "launcher.j9.";
	
	protected final static String ERROR_NO_J9_SPECIFIED= "launcher.error.noJ9specified.";

	public VMRunnerResult run(VMRunnerConfiguration config) {
		String location= getJDKLocation("");
		if ("".equals(location)) {
			String message= JavaLaunchUtils.getResourceString(ERROR_NO_J9_SPECIFIED+"message");
			showErrorDialog(ERROR_LAUNCHING, new LauncherException(message));
			return null;
		}		
		String program= location+File.separator+"bin"+File.separator+"j9";
		
		Vector arguments= new Vector();

		arguments.addElement(program);
				
		String[] bootCP= config.getBootClassPath();
		if (bootCP.length > 0) {
			arguments.add("-bp:"+convertClassPath(bootCP));
		} 
		
		String[] cp= config.getClassPath();
		if (cp.length > 0) {
			arguments.add("-classpath");
			arguments.add(convertClassPath(cp));
		}
		String[] vmArgs= config.getVMArguments();
		addArguments(vmArgs, arguments);
		
		arguments.addElement(config.getClassToLaunch());
		
		String[] programArgs= config.getProgramArguments();
		addArguments(programArgs, arguments);
				
		String[] cmdLine= new String[arguments.size()];
		arguments.copyInto(cmdLine);
		try {
			Process p= Runtime.getRuntime().exec(cmdLine);
			IProcess[] processes= new IProcess[] { DebugPlugin.getDefault().newProcess(p, cmdLine[0]) };
			return new VMRunnerResult(null, processes);
		} catch (IOException e) {
			JavaLaunchUtils.errorDialog(JavaPlugin.getActiveWorkbenchShell(), ERROR_CREATE_PROCESS, new LauncherException(e));
		}
		return null;
	}

	protected boolean shouldIncludeInPath(String path) {
		return true;
	}
	
	protected String getJDKLocation(String dflt) {
		String location= JavaPlugin.getDefault().getPreferenceStore().getString(J9PreferencePage.PREF_LOCATION);
		if (location == null)
			return dflt;
		return location;
	}
	
	protected String convertClassPath(String[] cp) {
		int pathCount= 0;
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < cp.length; i++) {
			if (pathCount > 0) {
				buf.append(File.pathSeparator);
			}
			buf.append(cp[i]);
			pathCount++;
		}
		return buf.toString();
	}
}