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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallChangedListener;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.jdt.launching.PropertyChangeEvent;

import org.eclipse.jdt.ui.JavaUI;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
  */
public class JavaDocVMInstallListener implements IVMInstallChangedListener {
	
	private static final String NODE_VM_ROOT= "known_vm_installs"; //$NON-NLS-1$
	private static final String NODE_VM_TYPE= "vm_type"; //$NON-NLS-1$
	private static final String NODE_VM_INSTALL= "vm_install"; //$NON-NLS-1$
	private static final String NODE_VM_ID= "id"; //$NON-NLS-1$
	private static final String NODE_VM_JAVADOCLOCATION= "javadoc_loc"; //$NON-NLS-1$
	private static final String NODE_VM_LIBRARY= "library"; //$NON-NLS-1$
	private static final String NODE_VM_LIB_PATH= "path"; //$NON-NLS-1$
	
	public static void saveVMInstallJavadocLocations(Document document, Element root) {
		Element vmRootElement = document.createElement(NODE_VM_ROOT);
		root.appendChild(vmRootElement);
		// <known_vm_installs>
		//     <vm_type id=x>
		//        <vm_install id=y javadoc_loc=url>
		//           <library path=a/>
		//           <library path=b/>
		//           <library path=c/>
		//         </vm_install>
		//      </vm_type>
		// </known_vm_installs>
		
		IVMInstallType[] types= JavaRuntime.getVMInstallTypes();
		for (int i = 0; i < types.length; i++) {
			IVMInstallType type= types[i];
			Element vmTypeElement= document.createElement(NODE_VM_TYPE);
			vmRootElement.appendChild(vmTypeElement);
			vmTypeElement.setAttribute(NODE_VM_ID, type.getId());
			IVMInstall[] installs= type.getVMInstalls();
			for (int j = 0; j < installs.length; j++) {
				IVMInstall install= installs[j];
				URL url= install.getJavadocLocation();
				LibraryLocation[] libraries= JavaRuntime.getLibraryLocations(install);
				if (url != null && libraries.length > 0) {
					Element vmInstallElement= document.createElement(NODE_VM_INSTALL);
					vmTypeElement.appendChild(vmInstallElement);
					vmInstallElement.setAttribute(NODE_VM_ID, install.getId());
					vmInstallElement.setAttribute(NODE_VM_JAVADOCLOCATION, url.toExternalForm());
					for (int k= 0; k < libraries.length; k++) {
						String path= libraries[k].getSystemLibraryPath().toPortableString();
						Element librariesElement= document.createElement(NODE_VM_LIBRARY);
						vmInstallElement.appendChild(librariesElement);
						librariesElement.setAttribute(NODE_VM_LIB_PATH, path);
					}
				}
			}
		}		
	}
		
	public static void collectChangedVMInstallJavadocLocations(Element rootElement, List resPaths, List resURLs) {
		NodeList vmRoots= rootElement.getElementsByTagName(NODE_VM_ROOT);
		Node root= (vmRoots.getLength() == 1) ? vmRoots.item(0) : null; // root == null:  no vm installs remembered
			
		IVMInstallType[] vmTypes= JavaRuntime.getVMInstallTypes();
		for (int i = 0; i < vmTypes.length; i++) {
			IVMInstallType vmType= vmTypes[i];
			Element rememberedVmType= findElement(root, NODE_VM_TYPE, NODE_VM_ID, vmType.getId()); // root can be null
			IVMInstall[] installs= vmType.getVMInstalls();
			for (int k = 0; k < installs.length; k++) {
				IVMInstall vmInstall= installs[k];
				Element rememberedVmInstall= findElement(rememberedVmType, NODE_VM_INSTALL, NODE_VM_ID, vmInstall.getId()); // rememberedVmType can be null
				processVMInstall(vmInstall, rememberedVmInstall, resPaths, resURLs); // rememberedVmInstall can be null
			}
		}
	}
	
	private static void processVMInstall(IVMInstall vmInstall, Element rememberedVmInstall, List resPaths, List resURLs) {
		URL url= vmInstall.getJavadocLocation();
		if (url == null && rememberedVmInstall == null) {
			return; // new vm install without javadoc loc: dont clear existing locations
		}
		
		boolean updateAll;
		if (rememberedVmInstall != null) {
			String loc= rememberedVmInstall.getAttribute(NODE_VM_JAVADOCLOCATION);
			updateAll= (url == null && loc != null) || (url != null && !url.toExternalForm().equals(loc));
		} else {
			updateAll= true;
		}
		// if updateAll == false: look for new library paths
		LibraryLocation[] libraryLocations= JavaRuntime.getLibraryLocations(vmInstall);
		for (int i= 0; i < libraryLocations.length; i++) {
			IPath libPath= libraryLocations[i].getSystemLibraryPath();
			if (updateAll || findElement(rememberedVmInstall, NODE_VM_LIBRARY, NODE_VM_LIB_PATH, libPath.toPortableString()) == null) {
				resPaths.add(libPath);
				resURLs.add(url);
			}
		}
	}
	
	private static Element findElement(Node root, String elemName, String attribName, String attribValue) {
		if (root != null) { // null accepted as root for convenience
			NodeList nodes= root.getChildNodes();
			for (int i= nodes.getLength() - 1; i >= 0; i--) {
				Node node= nodes.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE  && node.getNodeName().equalsIgnoreCase(elemName)) {
					if (attribValue.equals(((Element) node).getAttribute(attribName))) {
						return (Element) node;
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Constructor for VMInstallListener.
	 */
	public JavaDocVMInstallListener() {
	}
	
	public void init() {
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

	private static void updateJavadocLocations(IVMInstall install) {
		ArrayList paths= new ArrayList(), urls= new ArrayList();
		
		URL url= install.getJavadocLocation();
		if (url != null) {
			LibraryLocation[] locations= JavaRuntime.getLibraryLocations(install);
			if (locations != null) {
				for (int i = 0; i < locations.length; i++) {
					IPath path= locations[i].getSystemLibraryPath();
					if (path != null) {
						paths.add(path);
						urls.add(url);
					}
				}
			}
		}
		if (!paths.isEmpty()) {
			IPath[] allPaths= (IPath[]) paths.toArray(new IPath[paths.size()]);
			URL[] allURLs= (URL[]) urls.toArray(new URL[urls.size()]);
			JavaUI.setLibraryJavadocLocations(allPaths, allURLs); // only write once to avoid unnecessary saves
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
