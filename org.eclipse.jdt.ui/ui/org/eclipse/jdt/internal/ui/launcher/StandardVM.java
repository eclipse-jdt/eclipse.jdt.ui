/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.launcher;import java.io.File;import org.eclipse.debug.core.ILaunchManager;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jdt.launching.AbstractVMInstall;import org.eclipse.jdt.launching.IVMInstallType;import org.eclipse.jdt.launching.IVMRunner;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.launcher.JDK12DebugLauncher.IRetryQuery;import org.eclipse.jdt.internal.ui.util.SWTUtil;

public class StandardVM extends AbstractVMInstall {
	private IVMInstallType fType;
	private String fId;
	private String fName;
	private File fInstallLocation;
	
	StandardVM(IVMInstallType type, String id) {
		super(type, id);
	}
	/**
	 * @see IVM#getIVMRunner(String)
	 */
	public IVMRunner getVMRunner(String mode) {
		if (ILaunchManager.RUN_MODE.equals(mode)) {
			return new JDK12Launcher(this);
		} else if (ILaunchManager.DEBUG_MODE.equals(mode)) {
			return new JDK12DebugLauncher(this, createRetryQuery());
		}
		return null;
	}		private IRetryQuery createRetryQuery() {		return new IRetryQuery() {			public boolean queryRetry() {				final boolean[] result= new boolean[1];				SWTUtil.getStandardDisplay().syncExec(new Runnable() {					public void run() {						String title= LauncherMessages.getString("jdkLauncher.error.title");						String message= LauncherMessages.getString("jdkLauncher.error.timeout");						result[0]= (MessageDialog.openConfirm(JavaPlugin.getActiveWorkbenchShell(), title, message));					}				});				return result[0];			}		};	}	
}
