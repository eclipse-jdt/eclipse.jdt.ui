/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.launcher;import java.io.File;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Path;import org.eclipse.core.runtime.Status;import org.eclipse.jdt.launching.AbstractVMInstallType;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.jdt.launching.LibraryLocation;import org.eclipse.jdt.internal.ui.JavaPlugin;

public class StandardVMType extends AbstractVMInstallType {
	
	/**
	 * @see IVMType#validateInstallLocation(File)
	 */
	public IStatus validateInstallLocation(File installLocation) {
		File java= new File(installLocation, "bin"+File.separator+"java"); //$NON-NLS-2$ //$NON-NLS-1$
		File javaExe= new File(installLocation, "bin"+File.separator+"java.exe"); //$NON-NLS-2$ //$NON-NLS-1$
		if (!(java.isFile() || javaExe.isFile())) {
			return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, LauncherMessages.getString("standardVMType.notJDKRoot"), null); //$NON-NLS-1$
		}
		return new Status(IStatus.OK, JavaPlugin.getPluginId(), 0, LauncherMessages.getString("standardVMType.status.ok"), null); //$NON-NLS-1$
	}

	/**
	 * @see IVMType#getName()
	 */
	public String getName() {
		return LauncherMessages.getString("standardVMType.name"); //$NON-NLS-1$
	}

	
	protected IVMInstall doCreateVMInstall(String id) {
		return new StandardVM(this, id);
	}		protected boolean canDetectExecutable(File javaHome) {		File java= new File(javaHome, File.separator+"bin"+File.separator+"java"); //$NON-NLS-2$ //$NON-NLS-1$		File javaExe= new File(javaHome, File.separator+"bin"+File.separator+"java.exe"); //$NON-NLS-2$ //$NON-NLS-1$		if (!(java.isFile() || javaExe.isFile()))			return false;		return true; 	}
	
	/**
	 * @see IVMInstallType#detectInstallLocation()
	 */
	public File detectInstallLocation() {
		File javaHome= new File (System.getProperty("java.home")); //$NON-NLS-1$		File parent= new File(javaHome.getParent());
		if (!canDetectExecutable(javaHome))			return null;					if (canDetectExecutable(parent))			javaHome= parent;			
		String vendor= System.getProperty("java.vendor"); //$NON-NLS-1$
		if (!(vendor.startsWith("Sun") || vendor.startsWith("IBM"))) //$NON-NLS-2$ //$NON-NLS-1$
			return null;
		if ("J9".equals(System.getProperty("java.vm.name"))) //$NON-NLS-2$ //$NON-NLS-1$
			return null;
		return javaHome;
	}

	private IPath getDefaultSystemLibrary(File installLocation) {		IPath jreLibPath= new Path(installLocation.getPath()).append("lib").append("rt.jar"); //$NON-NLS-2$ //$NON-NLS-1$		if (jreLibPath.toFile().isFile())			return jreLibPath;		return new Path(installLocation.getPath()).append("jre").append("lib").append("rt.jar"); //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$	}

	private IPath getDefaultSystemLibrarySource(File installLocation) {		File parent= installLocation.getParentFile();		if (parent != null) {			File parentsrc= new File(parent, "src.jar"); //$NON-NLS-1$			if (parentsrc.isFile())				return new Path(parentsrc.getPath());		}
		return new Path(installLocation.getPath()).append("src.jar"); //$NON-NLS-1$	}

	public IPath getDefaultPackagRootPath() {
		return new Path("src"); //$NON-NLS-1$
	}

	/**
	 * @see IVMInstallType#getDefaultSystemLibraryDescription(File)
	 */
	public LibraryLocation getDefaultLibraryLocation(File installLocation) {
		return new LibraryLocation(getDefaultSystemLibrary(installLocation),						getDefaultSystemLibrarySource(installLocation), 						getDefaultPackagRootPath());
	}

}
