/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.launcher;import org.eclipse.jdt.launching.AbstractVMInstall;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.jdt.launching.IVMInstallType;/** * A VMInstall class that used for manipulating the set of vm installs * withouth committing early. */
public class VMStandin extends AbstractVMInstall {

	/**
	 * Constructor for VMStandin
	 */
	public VMStandin(IVMInstallType type, String id) {
		super(type, id);
	}		public VMStandin(IVMInstall realVM) {		this (realVM.getVMInstallType(), realVM.getId());		setName(realVM.getName());		setInstallLocation(realVM.getInstallLocation());		setDebuggerTimeout(realVM.getDebuggerTimeout());		setLibraryLocation(realVM.getLibraryLocation());	}		public IVMInstall convertToRealVM() {		IVMInstallType vmType= getVMInstallType();		IVMInstall realVM= vmType.findVMInstall(getId());				if (realVM == null) {			realVM= vmType.createVMInstall(getId());		}				realVM.setName(getName());		realVM.setInstallLocation(getInstallLocation());		realVM.setDebuggerTimeout(getDebuggerTimeout());		realVM.setLibraryLocation(getLibraryLocation());		return realVM;	}
}
