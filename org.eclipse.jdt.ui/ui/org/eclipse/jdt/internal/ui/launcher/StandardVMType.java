package org.eclipse.jdt.internal.ui.launcher;import java.io.File;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Status;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.launching.AbstractVMInstallType;import org.eclipse.jdt.launching.IVMInstall;

public class StandardVMType extends AbstractVMInstallType {
	
	/**
	 * @see IVMType#validateInstallLocation(File)
	 */
	public IStatus validateInstallLocation(File installLocation) {
		File java= new File(installLocation, "bin"+File.separator+"java");
		File javaExe= new File(installLocation, "bin"+File.separator+"java.exe");
		if (!(java.isFile() || javaExe.isFile())) {
			return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, "Not a JDK Root; Java executable was not found", null);
		}
		return new Status(IStatus.OK, JavaPlugin.getPluginId(), 0, "ok", null);
	}

	/**
	 * @see IVMType#getName()
	 */
	public String getName() {
		return "Standard VM";
	}

	
	protected IVMInstall doCreateVM(String id) {
		return new StandardVM(this, id);
	}
}
