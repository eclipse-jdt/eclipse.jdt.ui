/*******************************************************************************
 * Copyright (c) 2017 GK Software AG, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.provisional.JavaModelAccess;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Node in the tree of CPListElement et al, representing a module directive like add-exports ...
 */
public abstract class ModuleEncapsulationDetail {

	protected CPListElementAttribute fAttribElem;

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
						return JavaModelAccess.getUnfilteredPackageFragmentRoots(currentProject, classpathEntry);
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		return null;
	}

	public static String encodeFiltered(ModuleEncapsulationDetail[] details, Class<?> detailClass) {
		return Arrays.stream(details)
				.filter(detailClass::isInstance)
				.map(ModuleEncapsulationDetail::toString)
				.collect(Collectors.joining(":")); //$NON-NLS-1$
	}

	/**
	 * Node in the tree of CPListElement et al, representing a patch-module directive.
	 */
	static class ModulePatch extends ModuleEncapsulationDetail {

		public static ModulePatch fromString(CPListElementAttribute attribElem, String value) {
			return new ModulePatch(value, attribElem);
		}

		public final String fModule;
		
		public ModulePatch(String module, CPListElementAttribute attribElem) {
			fModule= module;
			fAttribElem= attribElem;
		}

		@Override
		public String toString() {
			return fModule;
		}
	}

	/**
	 * Node in the tree of CPListElement et al, representing an add-exports module directive.
	 */
	static class ModuleAddExport extends ModuleEncapsulationDetail {

		public static ModuleAddExport fromString(CPListElementAttribute attribElem, String value) {
			int slash= value.indexOf('/');
			int equals= value.indexOf('=');
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

		public final String fSourceModule;
		public final String fPackage;
		public final String fTargetModules;

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
	}

	/**
	 * Node in the tree of CPListElement et al, representing an add-reads module directive.
	 */
	static class ModuleAddReads extends ModuleEncapsulationDetail {

		public static ModuleAddReads fromString(CPListElementAttribute attribElem, String value) {
			int equals= value.indexOf('=');
			if (equals != -1) {
				return new ModuleAddReads(value.substring(0, equals),
											value.substring(equals+1),
											attribElem);
			}
			return null;
		}

		public static Collection<ModuleAddReads> fromMultiString(CPListElementAttribute attribElem, String values) {
			List<ModuleAddReads> readss= new ArrayList<>();
			for (String value : values.split(":")) { //$NON-NLS-1$
				ModuleAddReads reads= fromString(attribElem, value);
				if (reads != null)
					readss.add(reads);
			}
			return readss;
		}

		public final String fSourceModule;
		public final String fTargetModule;

		public ModuleAddReads(String sourceModule, String targetModule, CPListElementAttribute attribElem) {
			fSourceModule= sourceModule;
			fTargetModule= targetModule;
			fAttribElem= attribElem;
		}

		@Override
		public String toString() {
			return fSourceModule+'='+fTargetModule;
		}
	}

	/**
	 * Node in the tree of CPListElement et al, representing a limit-modules directive.
	 */
	static class LimitModules extends ModuleEncapsulationDetail {
		
		public static LimitModules fromString(CPListElementAttribute attribElem, String value) {
			String[] modules= value.split(","); //$NON-NLS-1$
			for (int i= 0; i < modules.length; i++) {
				modules[i]= modules[i].trim();
			}
			return new LimitModules(Arrays.asList(modules), attribElem);
		}
		
		public final List<String> fExplicitlyIncludedModules;

		public LimitModules(List<String> explicitlyIncludedModules, CPListElementAttribute attribElem) {
			fExplicitlyIncludedModules= explicitlyIncludedModules;
			fAttribElem= attribElem;
		}
		@Override
		public String toString() {
			return String.join(",", fExplicitlyIncludedModules); //$NON-NLS-1$
		}
	}
}
