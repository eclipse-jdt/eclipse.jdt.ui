/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.launcher;

import java.io.File;import java.io.IOException;import java.io.InterruptedIOException;import java.util.List;import java.util.Map;import java.util.Vector;import org.eclipse.debug.core.DebugPlugin;import org.eclipse.debug.core.model.IDebugTarget;import org.eclipse.debug.core.model.IProcess;import org.eclipse.jdi.Bootstrap;import org.eclipse.jdt.debug.core.JDIDebugModel;import org.eclipse.jdt.internal.ui.util.SocketUtil;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.jdt.launching.JavaRuntime;import org.eclipse.jdt.launching.VMRunnerConfiguration;import org.eclipse.jdt.launching.VMRunnerResult;import org.eclipse.jdt.ui.JavaUI;import com.sun.jdi.VirtualMachine;import com.sun.jdi.connect.Connector;import com.sun.jdi.connect.IllegalConnectorArgumentsException;import com.sun.jdi.connect.ListeningConnector;import com.sun.jdi.connect.Connector.IntegerArgument;

/**
 * A launcher for running java main classes. Uses JDI to launch a vm in debug 
 * mode.
 */
public class JDK12DebugLauncher extends JDK12Launcher {


	/**
	 * Creates a new lauchner
	 */
	public JDK12DebugLauncher(IVMInstall vmInstance) {
		super(vmInstance);
	}

	public VMRunnerResult run(VMRunnerConfiguration config) {
		int port= SocketUtil.findUnusedLocalPort(null, 5000, 15000);
		if (port == -1) {
			String msg= LauncherMessages.getString("jdkLauncher.noPort"); //$NON-NLS-1$
			showErrorDialog(LauncherMessages.getString("jdkLauncher.error.title"), msg, new LauncherException(msg)); //$NON-NLS-1$
			return null;
		}
		String location= getJDKLocation(""); //$NON-NLS-1$
		if ("".equals(location)) { //$NON-NLS-1$
			String msg= LauncherMessages.getString("jdkLauncher.error.noJDKHome"); //$NON-NLS-1$
			String title= LauncherMessages.getString("jdkLauncher.error.title"); //$NON-NLS-1$
			showErrorDialog(title, msg, new LauncherException(msg));
			return null;
		}
		String program= location+File.separator+"bin"+File.separator+"java"; //$NON-NLS-2$ //$NON-NLS-1$
		File javawexe= new File(program+"w.exe"); //$NON-NLS-1$
		File javaw= new File(program+"w"); //$NON-NLS-1$
		
		if (javaw.isFile()) 
			program= javaw.getAbsolutePath();
		else if (javawexe.isFile())
			program= javawexe.getAbsolutePath();

		Vector arguments= new Vector();

		arguments.addElement(program);

		String[] bootCP= config.getBootClassPath();
		if (bootCP.length > 0) {
			arguments.add("-Xbootclasspath:"+convertClassPath(bootCP)); //$NON-NLS-1$
		} 
		
		String[] cp= config.getClassPath();
		if (cp.length > 0) {
			arguments.add("-classpath"); //$NON-NLS-1$
			arguments.add(convertClassPath(cp));
		}
		addArguments(config.getVMArguments(), arguments);

		arguments.add("-Xdebug"); //$NON-NLS-1$
		arguments.add("-Xnoagent"); //$NON-NLS-1$
		arguments.add("-Djava.compiler=NONE"); //$NON-NLS-1$
		arguments.add("-Xrunjdwp:transport=dt_socket,address=localhost:" + port); //$NON-NLS-1$

		arguments.add(config.getClassToLaunch());
		addArguments(config.getProgramArguments(), arguments);
		String[] cmdLine= new String[arguments.size()];
		arguments.copyInto(cmdLine);

		ListeningConnector connector= getConnector();
		if (connector == null) {
			String msg= LauncherMessages.getString("jdkLauncher.error.noConnector"); //$NON-NLS-1$
			showErrorDialog(LauncherMessages.getString("jdkLauncher.error.title"), msg, new LauncherException(msg)); //$NON-NLS-1$
			return null;
		}
		Map map= connector.defaultArguments();
		specifyArguments(map, port);
			Process p= null;
		try {
			try {
				connector.startListening(map);

				try {
					p= Runtime.getRuntime().exec(cmdLine);
		
				} catch (IOException e) {
					if (p != null)
						p.destroy();
					showErrorDialog(LauncherMessages.getString("jdkLauncher.error.title"), LauncherMessages.getString("jdkLauncher.error.startVM"), new LauncherException(e)); //$NON-NLS-1$ //$NON-NLS-2$
					return null;
				}
		
				IProcess process= DebugPlugin.getDefault().newProcess(p, renderProcessLabel(cmdLine));
				process.setAttribute(JavaRuntime.ATTR_CMDLINE, renderCommandLine(cmdLine));
				try {
					Thread.currentThread().sleep(5000);
				} catch (InterruptedException e) {
				}
				boolean retry= false;
				do  {
					try {
						VirtualMachine vm= connector.accept(map);
						setTimeout(vm);
						IDebugTarget debugTarget= JDIDebugModel.newDebugTarget(vm, renderDebugTarget(config.getClassToLaunch(), port), process, true, false);
						return new VMRunnerResult(debugTarget, new IProcess[] { process });
					} catch (InterruptedIOException e) {
						retry= askRetry(LauncherMessages.getString("jdkLauncher.error.title"), LauncherMessages.getString("jdkLauncher.error.timeout")); //$NON-NLS-1$ //$NON-NLS-2$
					}
				} while (retry);
			} finally {
				connector.stopListening(map);
			}
		} catch (IOException e) {
			showErrorDialog(LauncherMessages.getString("jdkLauncher.error.title"), LauncherMessages.getString("jdkLauncher.error.connect"), new LauncherException(e)); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (IllegalConnectorArgumentsException e) {
			showErrorDialog(LauncherMessages.getString("jdkLauncher.error.title"), LauncherMessages.getString("jdkLauncher.error.connect"), new LauncherException(e)); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (p != null)
			p.destroy();
		return null;
	}
	
	protected void specifyArguments(Map map, int portNumber) {
		// XXX: Revisit - allows us to put a quote (") around the classpath
		Connector.IntegerArgument port= (Connector.IntegerArgument) map.get("port"); //$NON-NLS-1$
		port.setValue(portNumber);
		
		Connector.IntegerArgument timeout= (Connector.IntegerArgument) map.get("timeout"); //$NON-NLS-1$
		timeout.setValue(3000);
		
		
	}

	protected ListeningConnector getConnector() {
		List connectors= Bootstrap.virtualMachineManager().listeningConnectors();
		for (int i= 0; i < connectors.size(); i++) {
			ListeningConnector c= (ListeningConnector) connectors.get(i);
			if ("com.sun.jdi.SocketListen".equals(c.name())) //$NON-NLS-1$
				return c;
		}
		return null;
	}

}
