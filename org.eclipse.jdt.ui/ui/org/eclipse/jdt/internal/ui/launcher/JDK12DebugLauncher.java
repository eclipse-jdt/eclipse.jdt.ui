/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.launcher;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.ListeningConnector;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;

import org.eclipse.jdi.Bootstrap;
import org.eclipse.jdt.debug.core.JDIDebugModel;

import org.eclipse.jdt.internal.ui.util.SocketUtil;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jdt.launching.VMRunnerResult;

/**
 * A launcher for running java main classes. Uses JDI to launch a vm in debug 
 * mode.
 */
public class JDK12DebugLauncher extends JDK12Launcher {

	protected final static String PREFIX= "launcher.jdk12.debug.";
	protected static final String ERROR_NO_PORT= PREFIX + "error.no_port.";
	protected static final String ERROR_NO_CONNECTOR= PREFIX + "error.no_connector.";
	protected static final String ERROR_CONNECT= PREFIX + "error.connect.";
	protected static final String CONNECT_TIMEOUT= PREFIX + "timeout.";

	private final static String ERROR_NO_JDK12_SPECIFIED= "launcher.error.noJDKspecified.";

	/**
	 * Creates a new lauchner
	 */
	public JDK12DebugLauncher() {
	}

	public VMRunnerResult run(VMRunnerConfiguration config) {
		int port= SocketUtil.findUnusedLocalPort(null, 5000, 15000);
		if (port == -1) {
			String msg= JavaLaunchUtils.getResourceString(ERROR_NO_PORT+"message");
			showErrorDialog(ERROR_LAUNCHING, new LauncherException(msg));
			return null;
		}
		String location= getJDKLocation("");
		if ("".equals(location)) {
			String msg= JavaLaunchUtils.getResourceString(ERROR_NO_JDK12_SPECIFIED+"message");
			showErrorDialog(ERROR_LAUNCHING, new LauncherException(msg));
			return null;
		}
		String program= location + File.separator + "bin" + File.separator + "java";

		Vector arguments= new Vector();

		arguments.addElement(program);

		String[] bootCP= config.getBootClassPath();
		if (bootCP.length > 0) {
			arguments.add("-Xbootclasspath:"+convertClassPath(bootCP));
		} 
		
		String[] cp= config.getClassPath();
		if (cp.length > 0) {
			arguments.add("-classpath");
			arguments.add(convertClassPath(cp));
		}
		addArguments(config.getVMArguments(), arguments);

		arguments.add("-Xdebug");
		arguments.add("-Xnoagent");
		arguments.add("-Djava.compiler=NONE");
		arguments.add("-Xrunjdwp:transport=dt_socket,address=localhost:" + port);

		arguments.add(config.getClassToLaunch());
		addArguments(config.getProgramArguments(), arguments);
		String[] cmdLine= new String[arguments.size()];
		arguments.copyInto(cmdLine);

		ListeningConnector connector= getConnector();
		if (connector == null) {
			String msg= JavaLaunchUtils.getResourceString(ERROR_NO_CONNECTOR+"message");
			showErrorDialog(ERROR_LAUNCHING, new LauncherException(msg));
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
					showErrorDialog(ERROR_CREATE_PROCESS, new LauncherException(e));
					return null;
				}
		
				IProcess[] processes= new IProcess[] {DebugPlugin.getDefault().newProcess(p, renderCommandLine(cmdLine))};
				try {
					Thread.currentThread().sleep(5000);
				} catch (InterruptedException e) {
				}
				boolean retry= false;
				do  {
					try {
						VirtualMachine vm= connector.accept(map);
						IDebugTarget debugTarget= JDIDebugModel.newDebugTarget(vm, renderDebugTarget(config.getClassToLaunch(), port), processes[0], true);
						return new VMRunnerResult(debugTarget, processes);
					} catch (InterruptedIOException e) {
						retry= askRetry(CONNECT_TIMEOUT);
					}
				} while (retry);
			} finally {
				connector.stopListening(map);
			}
		} catch (IOException e) {
			showErrorDialog(ERROR_CONNECT, new LauncherException(e));
		} catch (IllegalConnectorArgumentsException e) {
			showErrorDialog(ERROR_CONNECT, new LauncherException(e));
		}
		if (p != null)
			p.destroy();
		return null;
	}
	
	protected void specifyArguments(Map map, int portNumber) {
		// this is a hack to allow us to put a quote (") around the classpath
		Connector.IntegerArgument port= (Connector.IntegerArgument) map.get("port");
		port.setValue(portNumber);
		
		Connector.IntegerArgument timeout= (Connector.IntegerArgument) map.get("timeout");
		timeout.setValue(3000);
		
		
	}

	protected ListeningConnector getConnector() {
		List connectors= Bootstrap.virtualMachineManager().listeningConnectors();
		for (int i= 0; i < connectors.size(); i++) {
			ListeningConnector c= (ListeningConnector) connectors.get(i);
			if ("com.sun.jdi.SocketListen".equals(c.name()))
				return c;
		}
		return null;
	}

}
