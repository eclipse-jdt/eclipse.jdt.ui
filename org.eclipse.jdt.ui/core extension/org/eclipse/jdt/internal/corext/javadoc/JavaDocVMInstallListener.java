/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.javadoc;

import java.net.URL;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallChangedListener;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.jdt.launching.PropertyChangeEvent;

import org.eclipse.jdt.ui.JavaUI;

/**
  */
public class JavaDocVMInstallListener implements IVMInstallChangedListener {

	/**
	 * Constructor for VMInstallListener.
	 */
	public JavaDocVMInstallListener() {
	}
	
	public void init() {
		IVMInstallType[] types= JavaRuntime.getVMInstallTypes();
		for (int i = 0; i < types.length; i++) {
			IVMInstall[] installs= types[i].getVMInstalls();
			for (int j = 0; j < installs.length; j++) {
				updateJavadocLocations(installs[j]);
			}
		}
		
		JavaRuntime.addVMInstallChangedListener(this);
	}
	
	public void remove() {
		JavaRuntime.removeVMInstallChangedListener(this);
	}
	
	

	/* (non-Javadoc)
	 * @see IVMInstallChangedListener#defaultVMInstallChanged(IVMInstall, IVMInstall)
	 */
	public void defaultVMInstallChanged(IVMInstall previous, IVMInstall current) {
	}

	private void updateJavadocLocations(IVMInstall install) {
		URL url= install.getJavadocLocation();
		if (url != null) {
			LibraryLocation[] locations= JavaRuntime.getLibraryLocations(install);
			if (locations != null) {
				for (int i = 0; i < locations.length; i++) {
					IPath path= locations[i].getSystemLibraryPath();
					if (path != null) {
						JavaUI.setLibraryJavadocLocation(path, url);
					}
				}
			}
		}
	}
		

	/* (non-Javadoc)
	 * @see IVMInstallChangedListener#vmChanged(PropertyChangeEvent)
	 */
	public void vmChanged(PropertyChangeEvent event) {
		String property= event.getProperty();
		if (PROPERTY_INSTALL_LOCATION.equals(property)
			|| PROPERTY_JAVADOC_LOCATION.equals(property) 
			|| PROPERTY_LIBRARY_LOCATIONS.equals(property)) {	
					updateJavadocLocations((IVMInstall) event.getSource());
		}
	}

	/* (non-Javadoc)
	 * @see IVMInstallChangedListener#vmAdded(IVMInstall)
	 */
	public void vmAdded(IVMInstall vm) {
		updateJavadocLocations(vm);
	}

	/* (non-Javadoc)
	 * @see IVMInstallChangedListener#vmRemoved(IVMInstall)
	 */
	public void vmRemoved(IVMInstall vm) {
	}

}
