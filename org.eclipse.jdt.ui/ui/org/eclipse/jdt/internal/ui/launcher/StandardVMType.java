/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.launcher;import java.io.File;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Path;import org.eclipse.core.runtime.Status;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.launching.AbstractVMInstallType;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.jdt.launching.LibraryLocation;

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

	
	protected IVMInstall doCreateVMInstall(String id) {
		return new StandardVM(this, id);
	}		protected boolean canDetectExecutable(File javaHome) {		File java= new File(javaHome, File.separator+"bin"+File.separator+"java");		File javaExe= new File(javaHome, File.separator+"bin"+File.separator+"java.exe");		if (!(java.isFile() || javaExe.isFile()))			return false;		return true; 	}
	
	/**
	 * @see IVMInstallType#detectInstallLocation()
	 */
	public File detectInstallLocation() {
		File javaHome= new File (System.getProperty("java.home"));		File parent= new File(javaHome.getParent());
		if (!canDetectExecutable(javaHome))			return null;					if (canDetectExecutable(parent))			javaHome= parent;			
		String vendor= System.getProperty("java.vendor");
		if (!(vendor.startsWith("Sun") || vendor.startsWith("IBM")))
			return null;
		if ("J9".equals(System.getProperty("java.vm.name")))
			return null;
		return javaHome;
	}

	private File getDefaultSystemLibrary(File installLocation) {
		File jreLib= new File(installLocation, "lib"+File.separator+"rt.jar");		if (jreLib.isFile())			return jreLib;		return new File(installLocation, "jre"+File.separator+"lib"+File.separator+"rt.jar");
	}

	private File getDefaultSystemLibrarySource(File installLocation) {		File parent= installLocation.getParentFile();		if (parent != null) {			File parentsrc= new File(parent, "src.jar");			if (parentsrc.isFile())				return parentsrc;		}
		return new File(installLocation, "src.jar");	}

	public IPath getDefaultPackagRootPath() {
		return new Path("src");
	}

	/**
	 * @see IVMInstallType#getDefaultSystemLibraryDescription(File)
	 */
	public LibraryLocation getDefaultLibraryLocation(File installLocation) {
		return new LibraryLocation(getDefaultSystemLibrary(installLocation),						getDefaultSystemLibrarySource(installLocation), 						getDefaultPackagRootPath());
	}

}
