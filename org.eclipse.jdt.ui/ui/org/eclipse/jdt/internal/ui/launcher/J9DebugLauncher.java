/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.launcher;

import java.io.File;import java.io.IOException;import java.util.ArrayList;import java.util.List;import java.util.Map;import org.eclipse.debug.core.DebugPlugin;import org.eclipse.debug.core.model.IDebugTarget;import org.eclipse.debug.core.model.IProcess;import org.eclipse.jdi.Bootstrap;import org.eclipse.jdt.debug.core.JDIDebugModel;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.util.SocketUtil;import org.eclipse.jdt.launching.VMRunnerConfiguration;import org.eclipse.jdt.launching.VMRunnerResult;import com.sun.jdi.VirtualMachine;import com.sun.jdi.connect.AttachingConnector;import com.sun.jdi.connect.Connector;import com.sun.jdi.connect.IllegalConnectorArgumentsException;import com.sun.jdi.connect.Connector.IntegerArgument;

public class J9DebugLauncher extends J9Launcher {
	private static final String PREFIX= "launcher.j9.debug.";
	
	protected static final String ERROR_NO_PORT=PREFIX+"error.no_port.";
	protected static final String ERROR_NO_CONNECTOR=PREFIX+"error.no_connector.";
	protected static final String ERROR_CONNECT= PREFIX+"error.connect.";
	
	public VMRunnerResult run(VMRunnerConfiguration config) {
		int port= SocketUtil.findUnusedLocalPort("localhost", 5000, 15000);
		int proxyPort= SocketUtil.findUnusedLocalPort("localhost", 5000, 15000);

		if (port == -1 || proxyPort == -1) {
			String message= JavaLaunchUtils.getResourceString(ERROR_NO_PORT+"message");
			showErrorDialog(ERROR_LAUNCHING, new LauncherException(message));
			return null;
		}
		String location= getJDKLocation("");
		if ("".equals(location)) {
			String message= JavaLaunchUtils.getResourceString(ERROR_NO_J9_SPECIFIED+"message");
			showErrorDialog(ERROR_LAUNCHING, new LauncherException(message));
			return null;
		}		
		String program= location+File.separator+"bin"+File.separator+"j9";
		String proxy= location+File.separator+"bin"+File.separator+"j9proxy";
		
		List arguments= new ArrayList();
		
		arguments.add(program);
		
		String[] bootCP= config.getBootClassPath();
		if (bootCP.length > 0) {
			arguments.add("-bp:"+convertClassPath(bootCP));
		} 
		
		String[] cp= config.getClassPath();
		if (cp.length > 0) {
			arguments.add("-classpath");
			arguments.add(convertClassPath(cp));
		}
		addArguments(config.getVMArguments(), arguments);
		
		arguments.add("-debug:"+port);
		arguments.add(config.getClassToLaunch());
		addArguments(config.getProgramArguments(), arguments);
		String[] cmdLine= new String[arguments.size()];
		arguments.toArray(cmdLine);
		
		String proxyCmd= proxy+" localhost:"+port+" "+ proxyPort;

		Process p= null;
		Process p2= null;
		try {
			p= Runtime.getRuntime().exec(cmdLine);
			p2= Runtime.getRuntime().exec(proxyCmd);
			
		} catch (IOException e) {
			if (p != null)
				p.destroy();
			if (p2 != null)
				p2.destroy();
			JavaLaunchUtils.errorDialog(JavaPlugin.getActiveWorkbenchShell(), ERROR_CREATE_PROCESS, new LauncherException(e));
			return null;
		}
		IProcess process1= DebugPlugin.getDefault().newProcess(p, renderProcessLabel(cmdLine));
		IProcess process2= DebugPlugin.getDefault().newProcess(p2, renderProcessLabel(new String[] { "j9Proxy" }));
		process1.setAttribute(ATTR_CMDLINE, renderCommandLine(cmdLine));
		process2.setAttribute(ATTR_CMDLINE, proxyCmd);
				
		AttachingConnector connector= getConnector();
		if (connector == null) {
			p.destroy();
			p2.destroy();
			String message= JavaLaunchUtils.getResourceString(ERROR_NO_CONNECTOR+"message");
			showErrorDialog(ERROR_LAUNCHING, new LauncherException(message));
			return null;;
		} 
		Map map= connector.defaultArguments();
		specifyArguments(map, proxyPort);
		boolean retry= false;
		do {
			try {
				VirtualMachine vm= connector.attach(map);
				setTimeout(vm);
				IDebugTarget debugTarget= JDIDebugModel.newDebugTarget(vm, renderDebugTarget(config.getClassToLaunch(), port), process1, true);
				return new VMRunnerResult(debugTarget, new IProcess[] { process1, process2 });
			} catch (IOException e) {
				String title= JavaLaunchUtils.getResourceString(ERROR_CONNECT+"title");
				String msg= JavaLaunchUtils.getResourceString(ERROR_CONNECT+"message");
				retry= askRetry(ERROR_CONNECT);
			} catch (IllegalConnectorArgumentsException e) {
				retry= false;
				JavaLaunchUtils.errorDialog(JavaPlugin.getActiveWorkbenchShell(), ERROR_CONNECT, new LauncherException(e));
			}
		} while (retry);
		p.destroy();
		p2.destroy();
		return null;
	}
	
	protected AttachingConnector getConnector() {
		List connectors= Bootstrap.virtualMachineManager().attachingConnectors();
		for (int i= 0; i < connectors.size(); i++) {
			AttachingConnector c= (AttachingConnector)connectors.get(i);
			if ("com.sun.jdi.SocketAttach".equals(c.name()))
				return c;
		}
		return null;
	}
		
	protected void specifyArguments(Map map, int portNumber)  {
		// this is a hack to allow us to put a quote (") around the classpath
		Connector.IntegerArgument port= (Connector.IntegerArgument) map.get("port");
		port.setValue(portNumber);
	}
	
	



}