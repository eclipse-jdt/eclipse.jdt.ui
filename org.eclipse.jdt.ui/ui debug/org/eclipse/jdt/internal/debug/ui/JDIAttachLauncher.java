package org.eclipse.jdt.internal.debug.ui;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2001
 */

import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ILauncherDelegate;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdi.Bootstrap;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.launching.WorkspaceSourceLocator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class JDIAttachLauncher implements ILauncherDelegate {

	private static final String PREFIX= "jdi_attach_launcher.";
	private static final String LABEL= PREFIX + "label";
	private static final String ERROR= PREFIX + "error.";
	private static final String NO_CONNECTOR= ERROR + "no_connector.";
	private static final String CONNECTION_REFUSED= ERROR + "connection_refused.";

	protected String fPort;
	protected String fHost;
	protected boolean fAllowTerminate;

	/**
	 * Perform the attach launch.
	 */
	protected boolean doLaunch(Object element, ILauncher launcher) {
		AttachingConnector connector= null;
		Iterator iter= Bootstrap.virtualMachineManager().attachingConnectors().iterator();
		while (iter.hasNext()) {
			AttachingConnector lc= (AttachingConnector) iter.next();
			if (lc.name().equals("com.sun.jdi.SocketAttach")) {
				connector= lc;
				break;
			}
		}

		// determine the launched project from the element
		IResource res= null;
		if (element instanceof IAdaptable) {
			res= (IResource) ((IAdaptable) element).getAdapter(IResource.class);
		}
		if (res != null) {
			element= res.getProject();
		}

		if (connector != null) {
			Map map= connector.defaultArguments();
			Connector.Argument param= (Connector.Argument) map.get("hostname");
			param.setValue(fHost);
			param= (Connector.Argument) map.get("port");
			param.setValue(fPort);
			try {
				VirtualMachine vm= connector.attach(map);
				StringBuffer vmLabel= new StringBuffer(vm.name());
				vmLabel.append('[');
				vmLabel.append(fHost);
				vmLabel.append(':');
				vmLabel.append(fPort);
				vmLabel.append(']');
				IDebugTarget target= JDIDebugModel.newDebugTarget(vm, vmLabel.toString(), null, fAllowTerminate, true);
				ISourceLocator sl= new WorkspaceSourceLocator(ResourcesPlugin.getWorkspace());
				ILaunch launch= new Launch(launcher, ILaunchManager.DEBUG_MODE, element, sl, null, target);
				DebugPlugin.getDefault().getLaunchManager().registerLaunch(launch);
				return true;
			} catch (IOException e) {
				errorDialog(CONNECTION_REFUSED, IJDIStatusConstants.CODE_CONNECTION_FAILED, e);
			} catch (IllegalConnectorArgumentsException e) {
				DebugUIUtils.logError(e);
			}

		} else {
			errorDialog(NO_CONNECTOR, IJDIStatusConstants.CODE_CONNECTION_FAILED, null);
		}

		return false;
	}

	protected boolean doLaunchUsingWizard(Object element, ILauncher launcher) {
		IStructuredSelection ss= new StructuredSelection(element);
		JDIAttachLauncherWizard w= new JDIAttachLauncherWizard();
		w.init(launcher, ILaunchManager.DEBUG_MODE, ss);
		WizardDialog wd= new WizardDialog(JavaPlugin.getActiveWorkbenchWindow().getShell(), w);
		return wd.open() == wd.OK;
	}

	protected void setPort(String port) {
		fPort= port;
	}

	protected void setHost(String host) {
		fHost= host;
	}

	protected void setAllowTerminate(boolean allow) {
		fAllowTerminate= allow;
	}

	protected void errorDialog(String prefix, int code, Throwable exception) {
		Status s= new Status(IStatus.ERROR, "org.eclipse.jdt.ui", IJDIStatusConstants.CODE_CONNECTION_FAILED, DebugUIUtils.getResourceString(prefix + "message"), exception);
		DebugUIUtils.errorDialog(JavaPlugin.getActiveWorkbenchWindow().getShell(), prefix, s);
	}

	/**
	 * @see ILauncher#launch
	 */
	public boolean launch(Object[] objects, String mode, ILauncher launcher) {
		Object element= null;
		if (objects.length > 0) {
			element= objects[0];
		}
		return doLaunchUsingWizard(element, launcher);
	}

}
