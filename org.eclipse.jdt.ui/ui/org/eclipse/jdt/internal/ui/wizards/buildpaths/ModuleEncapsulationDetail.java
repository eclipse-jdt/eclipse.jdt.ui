/*******************************************************************************
 * Copyright (c) 2017, 2019 GK Software SE, and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

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
						return currentProject.findUnfilteredPackageFragmentRoots(classpathEntry);
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		return null;
	}

	public static String encodeFiltered(ModuleEncapsulationDetail[] details, Class<?> detailClass) {
		// ModulePatch uses File.pathSeparator internally, which conflicts with single ":" on unix.
		String separator= detailClass == ModulePatch.class ? "::" : ":";  //$NON-NLS-1$//$NON-NLS-2$
		return Arrays.stream(details)
				.filter(detailClass::isInstance)
				.map(ModuleEncapsulationDetail::toString)
				.collect(Collectors.joining(separator));
	}

	/**
	 * Node in the tree of CPListElement et al, representing a patch-module directive.
	 */
	static class ModulePatch extends ModuleEncapsulationDetail {

		public static Collection<ModulePatch> fromMultiString(CPListElementAttribute attribElem, String values) {
			List<ModulePatch> patches= new ArrayList<>();
			for (String value : values.split("::")) { // see comment in #encodeFiltered(..) //$NON-NLS-1$
				ModulePatch patch= fromString(attribElem, value);
				if (patch != null)
					patches.add(patch);
			}
			return patches;
		}

		public static ModulePatch fromString(CPListElementAttribute attribElem, String value) {
			return new ModulePatch(value, attribElem);
		}

		public final String fModule;
		public final String fPaths;

		public ModulePatch(String value, CPListElementAttribute attribElem) {
			int eqIdx= value.indexOf('=');
			if (eqIdx == -1) {
				fModule= value;
				fPaths= attribElem.getParent().getJavaProject().getPath().toString();
			} else {
				fModule= value.substring(0, eqIdx);
				fPaths= value.substring(eqIdx + 1);
			}
			fAttribElem= attribElem;
		}

		public ModulePatch(String moduleName, String paths, CPListElementAttribute attribElem) {
			fModule= moduleName;
			fPaths= paths;
			fAttribElem= attribElem;
		}

		public ModulePatch addLocations(String newLocations) {
			String mergedPaths= fPaths + File.pathSeparatorChar + newLocations;
			return new ModulePatch(fModule, mergedPaths, fAttribElem);
		}

		public ModulePatch removeLocations(String locations) {
			Set<String> toRemove= new HashSet<>(Arrays.asList(locations.split(File.pathSeparator)));
			List<String> current= new ArrayList<>(Arrays.asList(fPaths.split(File.pathSeparator)));
			current.removeAll(toRemove);
			String newPaths= String.join(File.pathSeparator, current);
			if (newPaths.isEmpty()) {
				return null;
			}
			return new ModulePatch(fModule, newPaths, fAttribElem);
		}

		@Override
		public boolean affects(String module) {
			return module.equals(fModule);
		}

		public String[] getPathArray() {
			return fPaths.split(File.pathSeparator);
		}

		@Override
		public int hashCode() {
			return Objects.hash(fModule, fPaths);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ModulePatch other= (ModulePatch) obj;
			if (!Objects.equals(fModule, other.fModule)) {
				return false;
			}
			if (!Objects.equals(fPaths, other.fPaths)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			if (fPaths != null) {
				return fModule + '=' + fPaths;
			}
			return fModule;
		}

		@Override
		public String getAttributeName() {
			return IClasspathAttribute.PATCH_MODULE;
		}

		public String toAbsolutePathsString(IJavaProject focusProject) {
			String[] paths= fPaths.split(File.pathSeparator);
			String[] absPaths= new String[paths.length];
			IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
			for (int i= 0; i < paths.length; i++) {
				IResource resource= root.findMember(new Path(paths[i]));
				try {
					absPaths[i]= toAbsolutePath(resource, focusProject, root);
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
				if (absPaths[i] == null) {
					absPaths[i]= paths[i];
				}
			}
			String allPaths= String.join(File.pathSeparator, absPaths);
			return fModule + '=' + allPaths;
		}

		private String toAbsolutePath(IResource resource, IJavaProject focusProject, IWorkspaceRoot root) throws JavaModelException {
			if (resource instanceof IProject) {
				if (resource.equals(focusProject.getProject())) {
					// focus project: collect all source locations (pre-joined into one string):
					StringBuilder allSources= new StringBuilder();
					for (IClasspathEntry classpathEntry : focusProject.getRawClasspath()) {
						if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
							if (allSources.length() > 0) {
								allSources.append(File.pathSeparator);
							}
							allSources.append(absPath(root, classpathEntry.getPath()));
						}
					}
					return allSources.toString();
				} else {
					// other projects: use the default output locations:
					return absPath(root, JavaCore.create((IProject) resource).getOutputLocation());
				}
			} else if (resource != null) {
				if (isSourceFolderOf(resource, focusProject)) {
					// within current project use source path:
					return resource.getLocation().toString();
				} else {
					IJavaProject otherJProj= JavaCore.create(resource.getProject());
					if (otherJProj.exists()) {
						IClasspathEntry cpEntry= otherJProj.getClasspathEntryFor(resource.getFullPath());
						if (cpEntry != null && cpEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
							// source of other project -> map to its output location:
							IPath outputLocation= cpEntry.getOutputLocation();
							if (outputLocation == null) {
								outputLocation= otherJProj.getOutputLocation();
							}
							return absPath(root, outputLocation);
						}
					}
					// non-source location as-is:
					return resource.getLocation().toString();
				}
			}
			return null;
		}

		private String absPath(IWorkspaceRoot root, IPath path) {
			return root.findMember(path).getLocation().toString();
		}

		private boolean isSourceFolderOf(IResource resource, IJavaProject javaProject) throws JavaModelException {
			IPackageFragmentRoot root= javaProject.findPackageFragmentRoot(resource.getFullPath());
			if (root != null) {
				return root.getKind() == IPackageFragmentRoot.K_SOURCE;
			}
			return false;
		}
	}

	/** Shared implementation for ModuleAddExports & ModuleAddOpens (same structure). */
	abstract static class ModuleAddExpose extends ModuleEncapsulationDetail {

		public static ModuleAddExpose fromString(CPListElementAttribute attribElem, String value, boolean isExports) {
			int slash= value.indexOf('/');
			int equals= value.indexOf('=');
			if (slash != -1 && equals != -1 && equals > slash) {
				if (isExports)
					return new ModuleAddExport(value.substring(0, slash),
											value.substring(slash+1, equals),
											value.substring(equals+1),
											attribElem);
				else
					return new ModuleAddOpens(value.substring(0, slash),
							value.substring(slash+1, equals),
							value.substring(equals+1),
							attribElem);
			}
			return null;
		}

		public static Collection<ModuleAddExpose> fromMultiString(CPListElementAttribute attribElem, String values, boolean isExports) {
			List<ModuleAddExpose> exports= new ArrayList<>();
			for (String value : values.split(":")) { //$NON-NLS-1$
				ModuleAddExpose export= fromString(attribElem, value, isExports);
				if (export != null)
					exports.add(export);
			}
			return exports;
		}

		public final String fSourceModule;
		public final String fPackage;
		public final String fTargetModules;

		public ModuleAddExpose(String sourceModule, String aPackage, String targetModules, CPListElementAttribute attribElem) {
			fSourceModule= sourceModule;
			fPackage= aPackage;
			fTargetModules= targetModules;
			fAttribElem= attribElem;
		}

		@Override
		public boolean affects(String module) {
			return module.equals(fSourceModule);
		}

		@Override
		public int hashCode() {
			return Objects.hash(fPackage, fSourceModule, fTargetModules);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ModuleAddExpose other= (ModuleAddExpose) obj;
			if (!Objects.equals(fPackage, other.fPackage)) {
				return false;
			}
			if (!Objects.equals(fSourceModule, other.fSourceModule)) {
				return false;
			}
			if (!Objects.equals(fTargetModules, other.fTargetModules)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return fSourceModule+'/'+fPackage+'='+fTargetModules;
		}
	}

	/**
	 * Node in the tree of CPListElement et al, representing an add-exports module directive.
	 */
	static class ModuleAddExport extends ModuleAddExpose {
		public ModuleAddExport(String sourceModule, String aPackage, String targetModules, CPListElementAttribute attribElem) {
			super(sourceModule, aPackage, targetModules, attribElem);
		}

		@Override
		public String getAttributeName() {
			return IClasspathAttribute.ADD_EXPORTS;
		}
	}

	/**
	 * Node in the tree of CPListElement et al, representing an add-opens module directive.
	 */
	static class ModuleAddOpens extends ModuleAddExpose {
		public ModuleAddOpens(String sourceModule, String aPackage, String targetModules, CPListElementAttribute attribElem) {
			super(sourceModule, aPackage, targetModules, attribElem);
		}
		@Override
		public String getAttributeName() {
			return IClasspathAttribute.ADD_OPENS;
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
		public boolean affects(String module) {
			return module.equals(fSourceModule);
		}

		@Override
		public int hashCode() {
			return Objects.hash(fSourceModule, fTargetModule);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ModuleAddReads other= (ModuleAddReads) obj;
			if (!Objects.equals(fSourceModule, other.fSourceModule)) {
				return false;
			}
			if (!Objects.equals(fTargetModule, other.fTargetModule)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return fSourceModule+'='+fTargetModule;
		}

		@Override
		public String getAttributeName() {
			return IClasspathAttribute.ADD_READS;
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

		public final Collection<String> fExplicitlyIncludedModules;

		public LimitModules(Collection<String> explicitlyIncludedModules, CPListElementAttribute attribElem) {
			fExplicitlyIncludedModules= explicitlyIncludedModules;
			fAttribElem= attribElem;
		}
		@Override
		public boolean affects(String module) {
			return false; // no change on the module, just on the module graph / set of root modules
		}
		@Override
		public String toString() {
			return String.join(",", fExplicitlyIncludedModules); //$NON-NLS-1$
		}
		@Override
		public String getAttributeName() {
			return IClasspathAttribute.LIMIT_MODULES;
		}
	}

	public abstract boolean affects(String module);

	public abstract String getAttributeName();

	/**
	 * Searches the given list of details for a {@link ModulePatch} element affecting the given {@code module}.
	 * If found, replaces the found ModulePatch with a new value where given {@code newLocations} have been added.
	 * If no matching {@link ModulePatch} if found, a new one will be created and added to the {@code details} list.
	 * This operation modifies the given list of details.
	 * @param details list representation of a module attribute value
	 * @param module name of the module, for which a {@link ModulePatch} should be modified
	 * @param newLocations paths (separated by {@link File#pathSeparator}) to be added to the {@link ModulePatch}.
	 * @param attribElem parent attribute, to hold the resulting details (not updated during this operation)
	 */
	public static void addPatchLocations(List<ModuleEncapsulationDetail> details, String module, String newLocations, CPListElementAttribute attribElem) {
		for (int i= 0; i < details.size(); i++) {
			ModuleEncapsulationDetail detail= details.get(i);
			if (detail instanceof ModulePatch) {
				ModulePatch oldPatch= (ModulePatch) detail;
				if (oldPatch.affects(module)) {
					details.set(i, oldPatch.addLocations(newLocations));
					return;
				}
			}
		}
		details.add(new ModulePatch(module, newLocations, attribElem));
	}

	/**
	 * Searches the given list of details for a {@link ModulePatch} element affecting the given {@code module}.
	 * If found, replaces the found {@link ModulePatch} with a new value where the given {@code paths} have been removed.
	 * This operation modifies the given list of details.
	 * @param details list representation of a module attribute value
	 * @param module name of the module, for which a {@link ModulePatch} should be modified
	 * @param paths paths (separated by {@link File#pathSeparator}) to be removed from the {@link ModulePatch}.
	 */
	public static void removePatchLocation(List<ModuleEncapsulationDetail> details, String module, String paths) {
		for (int i= 0; i < details.size(); i++) {
			ModuleEncapsulationDetail detail= details.get(i);
			if (detail instanceof ModulePatch) {
				ModulePatch oldPatch= (ModulePatch) detail;
				if (oldPatch.affects(module)) {
					ModulePatch updatedPatch= oldPatch.removeLocations(paths);
					if (updatedPatch == null) {
						details.remove(i);
					} else {
						details.set(i, updatedPatch);
					}
					break;
				}
			}
		}
	}
}
