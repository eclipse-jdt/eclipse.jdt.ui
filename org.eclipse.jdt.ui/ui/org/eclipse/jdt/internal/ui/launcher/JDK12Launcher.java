/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.launcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IProcess;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jdt.launching.VMRunnerResult;

public class JDK12Launcher extends JavaLauncher {
	
	public JDK12Launcher(IVMInstall vmInstance) {
		super(vmInstance);
	}
	
	public VMRunnerResult run(VMRunnerConfiguration config) throws CoreException {
		String location= getJDKLocation(""); //$NON-NLS-1$
		if ("".equals(location)) { //$NON-NLS-1$
			throw new CoreException(createStatus(LauncherMessages.getString("jdkLauncher.noJDKHome"), null));
		}
		
		String program= location+File.separator+"bin"+File.separator+"java"; //$NON-NLS-2$ //$NON-NLS-1$
		File javawexe= new File(program+"w.exe"); //$NON-NLS-1$
		File javaw= new File(program+"w"); //$NON-NLS-1$
		
		if (javaw.isFile()) 
			program= javaw.getAbsolutePath();
		else if (javawexe.isFile())
			program= javawexe.getAbsolutePath();
		
		List arguments= new ArrayList();

		arguments.add(program);
				
		String[] bootCP= config.getBootClassPath();
		if (bootCP.length > 0) {
			arguments.add("-Xbootclasspath:"+convertClassPath(bootCP)); //$NON-NLS-1$
		} 
		
		String[] cp= config.getClassPath();
		if (cp.length > 0) {
			arguments.add("-classpath"); //$NON-NLS-1$
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
			throw new CoreException(createStatus(LauncherMessages.getString("jdkLauncher.error.startMV"), e));
		}
	}

	protected String convertClassPath(String[] cp) {
		int pathCount= 0;
		StringBuffer buf= new StringBuffer();
		if (cp.length == 0)
			return ""; //$NON-NLS-1$
		for (int i= 0; i < cp.length; i++) {
			if (cp[i].endsWith("rt.jar")) { //$NON-NLS-1$
				File f= new File(cp[i]);
				if ("rt.jar".equals(f.getName())) //$NON-NLS-1$
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