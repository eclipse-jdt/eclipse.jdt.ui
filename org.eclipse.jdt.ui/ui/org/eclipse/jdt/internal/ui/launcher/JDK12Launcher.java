/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.launcher;

import java.io.File;import java.io.IOException;import java.util.ArrayList;import java.util.List;import org.eclipse.debug.core.DebugPlugin;import org.eclipse.debug.core.model.IProcess;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.jdt.launching.JavaRuntime;import org.eclipse.jdt.launching.VMRunnerConfiguration;import org.eclipse.jdt.launching.VMRunnerResult;

public class JDK12Launcher extends JavaLauncher {
	private static final String PREFIX= "launcher.jdk12.";
	
	private final static String ERROR_NO_JDK12_SPECIFIED= "launcher.error.noJDKspecified.";	
	
	
	public JDK12Launcher(IVMInstall vmInstance) {
		super(vmInstance);
	}
	
	public VMRunnerResult run(VMRunnerConfiguration config) {
		String location= getJDKLocation("");
		if ("".equals(location)) {
			String msg= JavaLaunchUtils.getResourceString(ERROR_NO_JDK12_SPECIFIED+"message");
			showErrorDialog(ERROR_LAUNCHING, new LauncherException(msg));
			return null;
		}
		
		String program= location+File.separator+"bin"+File.separator+"java";
		File javaw= new File(program+'w');
		
		if (javaw.isFile()) 
			program= javaw.getAbsolutePath();
		
		List arguments= new ArrayList();

		arguments.add(program);
				
		String[] bootCP= config.getBootClassPath();
		if (bootCP.length > 0) {
			arguments.add("-Xbootclasspath:"+convertClassPath(bootCP));
		} 
		
		String[] cp= config.getClassPath();
		if (cp.length > 0) {
			arguments.add("-classpath");
			arguments.add(convertClassPath(cp));
		}
		String[] vmArgs= config.getVMArguments();
		addArguments(vmArgs, arguments);
		
		arguments.add(config.getClassToLaunch());
		
		String[] programArgs= config.getProgramArguments();
		addArguments(programArgs, arguments);
				
		String[] cmdLine= new String[arguments.size()];
		arguments.toArray(cmdLine);

		try {
			Process p= Runtime.getRuntime().exec(cmdLine);
			IProcess process= DebugPlugin.getDefault().newProcess(p, renderProcessLabel(cmdLine));
			process.setAttribute(JavaRuntime.ATTR_CMDLINE, renderCommandLine(cmdLine));
			return new VMRunnerResult(null, new IProcess[] { process });
		} catch (IOException e) {
			showErrorDialog(ERROR_CREATE_PROCESS, new LauncherException(e));
		}
		return null;
		
	}

	protected String convertClassPath(String[] cp) {
		int pathCount= 0;
		StringBuffer buf= new StringBuffer();
		if (cp.length == 0)
			return "";
		for (int i= 0; i < cp.length; i++) {
			if (cp[i].endsWith("rt.jar")) {
				File f= new File(cp[i]);
				if ("rt.jar".equals(f.getName()))
					continue;
			}
			if (pathCount > 0) {
				buf.append(File.pathSeparator);
			}
			buf.append(cp[i]);
			pathCount++;
		}
		return buf.toString();
	}
	
}