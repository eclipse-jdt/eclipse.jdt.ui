package org.eclipse.jdt.internal.ui.launcher;import java.io.File;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Status;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.launching.IVM;

public class J9VMType extends StandardVMType {

	public IVM doCreateVM(String id) {
		return new J9VM(this, id);
	}
	
	public String getName() {
		return "J9 VM";
	}
	
	public IStatus validateInstallLocation(File installLocation) {
		File java= new File(installLocation, "bin"+File.separator+"j9");
		File javaExe= new File(installLocation, "bin"+File.separator+"j9.exe");
		if (!(java.isFile() || javaExe.isFile())) {
			return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, "Not a JDK Root; J9 executable was not found", null);
		}
		return new Status(IStatus.OK, JavaPlugin.getPluginId(), 0, "ok", null);
	}
}
