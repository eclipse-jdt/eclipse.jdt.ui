/*******************************************************************************
 * Copyright (c) 2017 GK Software AG, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Node in the tree of CPListElement et al, representing an add-exports module directive.
 */
public class ModuleAddExport {

	public static ModuleAddExport fromString(CPListElementAttribute attribElem, String value) {
		int slash = value.indexOf('/');
		int equals = value.indexOf('=');
		if (slash != -1 && equals != -1 && equals > slash) {
			return new ModuleAddExport(value.substring(0, slash),
										value.substring(slash+1, equals),
										value.substring(equals+1),
										attribElem);
		}
		return null;
	}

	public static Collection<ModuleAddExport> fromMultiString(CPListElementAttribute attribElem, String values) {
		List<ModuleAddExport> exports= new ArrayList<>();
		for (String value : values.split(":")) { //$NON-NLS-1$
			ModuleAddExport export= fromString(attribElem, value);
			if (export != null)
				exports.add(export);
		}
		return exports;
	}

	public static String encode(ModuleAddExport[] exports) {
		StringBuilder buf= new StringBuilder();
		for (ModuleAddExport export : exports) {
			if (buf.length() > 0)
				buf.append(':');
			buf.append(export.toString());
		}
		return buf.toString();
	}

	public final String fSourceModule;
	public final String fPackage;
	public final String fTargetModules;

	private CPListElementAttribute fAttribElem;

	public ModuleAddExport(String sourceModule, String aPackage, String targetModules, CPListElementAttribute attribElem) {
		fSourceModule= sourceModule;
		fPackage= aPackage;
		fTargetModules= targetModules;
		fAttribElem= attribElem;
	}

	@Override
	public String toString() {
		return fSourceModule+'/'+fPackage+'='+fTargetModules;
	}

	public CPListElementAttribute getParent() {
		return fAttribElem;
	}

	/**
	 * Retrieve the java element(s) targeted by a given classpath entry.
	 * @param currentProject the Java project holding the classpath entry 
	 * @param path the path value of the classpath entry
	 * @return either an array of {@link IPackageFragmentRoot} or a singleton array of {@link IJavaProject} 
	 * 	targeted by the given classpath entry, or {@code null} if no not found
	 */
	public static IJavaElement[] getTargetJavaElements(IJavaProject currentProject, IPath path) {
		IResource member= ResourcesPlugin.getWorkspace().getRoot().findMember(path);
		if (member != null) {
			IJavaElement element= JavaCore.create(member);
			if (element != null)
				return new IJavaElement[] {element};
		} else if (path != null && path.isAbsolute()) {
			try {
				for (IClasspathEntry classpathEntry : currentProject.getRawClasspath()) {
					if (classpathEntry.getPath().equals(path)) {
						switch (classpathEntry.getEntryKind()) {
							case IClasspathEntry.CPE_LIBRARY:
								return new IJavaElement[] {currentProject.getPackageFragmentRoot(path.toString())};
							default:
								// keep looking
						}
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		} else {
			try {
				for (IClasspathEntry classpathEntry : currentProject.getRawClasspath()) {
					if (classpathEntry.getPath().equals(path)) {
						return currentProject.findPackageFragmentRoots(classpathEntry);
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		return null;
	}
}
