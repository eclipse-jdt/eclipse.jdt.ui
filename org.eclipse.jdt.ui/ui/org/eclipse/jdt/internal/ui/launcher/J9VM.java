package org.eclipse.jdt.internal.ui.launcher;import org.eclipse.debug.core.ILaunchManager;import org.eclipse.jdt.launching.IVMRunner;import org.eclipse.jdt.launching.IVMInstallType;

public class J9VM extends StandardVM {

	/**
	 * Constructor for J9VM
	 */
	J9VM(IVMInstallType type, String id) {
		super(type, id);
	}

	/**
	 * @see StandardJDKVM#getVMRunner(String)
	 */
	public IVMRunner getVMRunner(String mode) {
		if (ILaunchManager.RUN_MODE.equals(mode)) {
			return new J9Launcher(this);
		} else if (ILaunchManager.DEBUG_MODE.equals(mode)) {
			return new J9DebugLauncher(this);
		}
		return null;
	}

}
