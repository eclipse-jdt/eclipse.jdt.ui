package org.eclipse.jdt.internal.ui.launcher;import java.io.File;import org.eclipse.debug.core.ILaunchManager;import org.eclipse.jdt.launching.AbstractVM;import org.eclipse.jdt.launching.IVMRunner;import org.eclipse.jdt.launching.IVMType;

public class StandardVM extends AbstractVM {
	private IVMType fType;
	private String fId;
	private String fName;
	private File fInstallLocation;
	
	StandardVM(IVMType type, String id) {
		super(type, id);
	}
	/**
	 * @see IVM#getIVMRunner(String)
	 */
	public IVMRunner getVMRunner(String mode) {
		if (ILaunchManager.RUN_MODE.equals(mode)) {
			return new JDK12Launcher(this);
		} else if (ILaunchManager.DEBUG_MODE.equals(mode)) {
			return new JDK12DebugLauncher(this);
		}
		return null;
	}
}
