/*******************************************************************************
 * Copyright (c) 2023 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.ui;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.bcoview.BytecodeOutlinePlugin;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;

/**
 * Replacement for TypeInfoFactory which is internal in 3.2 and removed or partly not more
 * accessible in 3.3. The class contains a limited features subset from original one
 */
public class TypeFactory {
	static final char SEPARATOR = '/';

	static final char EXTENSION_SEPARATOR = '.';

	static final char PACKAGE_PART_SEPARATOR = '.';

	private final String[] fProjects;

	private char[][] enclosingNames;

	private String simpleTypeName;

	public TypeFactory() {
		super();
		fProjects = getProjectList();
	}

	private IType getType(IJavaElement container) {
		try {
			if (container instanceof ICompilationUnit) {
				return findTypeInCompilationUnit((ICompilationUnit) container, getTypeQualifiedName());
			} else if (container instanceof IOrdinaryClassFile) {
				return ((IOrdinaryClassFile) container).getType();
			}
			return null;
		} catch (JavaModelException e) {
			BytecodeOutlinePlugin.error("getType() fails for: " + simpleTypeName, e); //$NON-NLS-1$
		}
		return null;
	}

	public String getTypeQualifiedName() {
		if (enclosingNames != null && enclosingNames.length > 0) {
			StringBuffer buf = new StringBuffer();
			for (char[] enclosingName : enclosingNames) {
				buf.append(enclosingName);
				buf.append('.');
			}
			buf.append(simpleTypeName);
			return buf.toString();
		}
		return simpleTypeName;
	}

	/**
	 * Will be called at most two times if the return value was not null.
	 *
	 * @param packageName non null
	 * @param simpleTypeName1 non null
	 * @param enclosingName non null
	 * @param path non null
	 * @param searchScope non null
	 * @return any non-null value.
	 */
	public IType create(char[] packageName, char[] simpleTypeName1, char[][] enclosingName, String path, IJavaSearchScope searchScope) {
		this.enclosingNames = enclosingName;
		this.simpleTypeName = new String(simpleTypeName1);
		String pn = new String(packageName);

		int index = path.indexOf(IJavaSearchScope.JAR_FILE_ENTRY_SEPARATOR);
		IType result = null;
		if (index != -1) {
			result = createJarFileEntryTypeInfo(pn, simpleTypeName, path, index, searchScope);
		} else {
			String project = getProject(path);
			if (project != null) {
				result = createIFileTypeInfo(pn, simpleTypeName, path, project);
			}
		}
		return result;
	}

	private IType createIFileTypeInfo(String packageName, String typeName, String path, String project) {
		String rest = path.substring(project.length() + 1); // the first slashes.
		int index = rest.lastIndexOf(SEPARATOR);
		if (index == -1) {
			return null;
		}
		String middle = rest.substring(0, index);
		rest = rest.substring(index + 1);
		index = rest.lastIndexOf(EXTENSION_SEPARATOR);
		String file = null;
		String extension = null;
		if (index != -1) {
			file = rest.substring(0, index);
			extension = rest.substring(index + 1);
		} else {
			return null;
		}
		String src = null;
		int ml = middle.length();
		int pl = packageName.length();
		// if we have a source or package then we have to substract the leading '/'
		if (ml > 0 && ml - 1 > pl) {
			// If we have a package then we have to substract the '/' between src and package
			src = middle.substring(1, ml - pl - (pl > 0 ? 1 : 0));
		}

		if (typeName.equals(file)) {
			file = typeName;
		}

		IFileTypeInfo fileTypeInfo = new IFileTypeInfo(packageName, project, src, file, extension);
		IJavaElement container = fileTypeInfo.getContainer();
		return getType(container);
	}

	protected IType createJarFileEntryTypeInfo(String packageName, String typeName, String path, int index, IJavaSearchScope searchScope) {
		String jar = path.substring(0, index);
		String rest = path.substring(index + 1);
		index = rest.lastIndexOf(SEPARATOR);
		if (index != -1) {
			rest = rest.substring(index + 1);
		}
		String file = null;
		String extension = null;
		index = rest.lastIndexOf(EXTENSION_SEPARATOR);
		if (index != -1) {
			file = rest.substring(0, index);
			extension = rest.substring(index + 1);
		} else {
			return null;
		}

		if (typeName.equals(file)) {
			file = typeName;
		}
		JarFileEntryTypeInfo info = new JarFileEntryTypeInfo(packageName, jar, file, extension);
		IJavaElement container;
		try {
			container = info.getContainer(searchScope);
			return getType(container);
		} catch (JavaModelException e) {
			BytecodeOutlinePlugin.error("createJarFileEntryTypeInfo() fails for: " + simpleTypeName, e); //$NON-NLS-1$
		}
		return null;
	}

	private String getProject(String path) {
		for (String project : fProjects) {
			if (path.startsWith(project, 1)) {
				return project;
			}
		}
		return null;
	}

	private static String[] getProjectList() {
		IJavaModel model = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
		String[] result;
		try {
			IJavaProject[] projects = model.getJavaProjects();
			result = new String[projects.length];
			for (int i = 0; i < projects.length; i++) {
				result[i] = projects[i].getElementName();
			}
		} catch (JavaModelException e) {
			result = new String[0];
		}
		// We have to sort the list of project names to make sure that we cut of the longest
		// project from the path, if two projects with the same prefix exist. For example
		// org.eclipse.jdt.ui and org.eclipse.jdt.ui.tests.
		Arrays.sort(result, (o1, o2) -> {
			int l1 = o1.length();
			int l2 = o2.length();
			if (l1 < l2) {
				return 1;
			}
			if (l2 < l1) {
				return -1;
			}
			return 0;
		});
		return result;
	}

	/**
	 * Returns the qualified type name of the given type using '.' as separators. This is a replace
	 * for IType.getTypeQualifiedName() which uses '$' as separators. As '$' is also a valid
	 * character in an id this is ambiguous. JavaCore PR: 1GCFUNT
	 *
	 * @param type non null
	 * @return FQN
	 */
	private static String getTypeQualifiedName(IType type) {
		try {
			if (type.isBinary() && !type.isAnonymous()) {
				IType declaringType = type.getDeclaringType();
				if (declaringType != null) {
					return getTypeQualifiedName(declaringType) + '.' + type.getElementName();
				}
			}
		} catch (JavaModelException e) {
			// ignore
		}
		return type.getTypeQualifiedName('.');
	}

	/**
	 * Finds a type in a compilation unit. Typical usage is to find the corresponding type in a
	 * working copy.
	 *
	 * @param cu the compilation unit to search in
	 * @param typeQualifiedName the type qualified name (type name with enclosing type names
	 *            (separated by dots))
	 * @return the type found, or null if not existing
	 * @throws JavaModelException on error
	 */
	private static IType findTypeInCompilationUnit(ICompilationUnit cu, String typeQualifiedName) throws JavaModelException {
		IType[] types = cu.getAllTypes();
		for (IType type : types) {
			String currName = getTypeQualifiedName(type);
			if (typeQualifiedName.equals(currName)) {
				return type;
			}
		}
		return null;
	}

	private static final class JarFileEntryTypeInfo {

		private final String fJar;

		private final String fFileName;

		private final String fExtension;

		private final String pkg;

		public JarFileEntryTypeInfo(String pkg, String jar, String fileName, String extension) {
			this.pkg = pkg;
			fJar = jar;
			fFileName = fileName;
			fExtension = extension;
		}

		private IJavaElement getContainer(IJavaSearchScope scope) throws JavaModelException {
			IJavaModel jmodel = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
			IPath[] enclosedPaths = scope.enclosingProjectsAndJars();

			for (IPath curr : enclosedPaths) {
				if (curr.segmentCount() == 1) {
					IJavaProject jproject = jmodel.getJavaProject(curr.segment(0));
					IPackageFragmentRoot root = jproject.getPackageFragmentRoot(fJar);
					if (root.exists()) {
						return findElementInRoot(root);
					}
				}
			}
			List<IPath> paths = Arrays.asList(enclosedPaths);
			IJavaProject[] projects = jmodel.getJavaProjects();
			for (IJavaProject jproject : projects) {
				if (!paths.contains(jproject.getPath())) {
					IPackageFragmentRoot root = jproject.getPackageFragmentRoot(fJar);
					if (root.exists()) {
						return findElementInRoot(root);
					}
				}
			}
			return null;
		}

		private IJavaElement findElementInRoot(IPackageFragmentRoot root) {
			IJavaElement res;
			IPackageFragment frag = root.getPackageFragment(pkg);
			String extension = fExtension;
			String fullName = fFileName + '.' + extension;

			if ("class".equals(extension)) { //$NON-NLS-1$
				res = frag.getClassFile(fullName);
			} else if (JavaCore.isJavaLikeFileName(fullName)) {
				res = frag.getCompilationUnit(fullName);
			} else {
				return null;
			}
			if (res.exists()) {
				return res;
			}
			return null;
		}
	}

	private static final class IFileTypeInfo {

		private final String fProject;

		private final String fFolder;

		private final String fFile;

		private final String fExtension;

		private final String pkg;

		public IFileTypeInfo(String pkg, String project, String sourceFolder, String file, String extension) {
			this.pkg = pkg;
			fProject = project;
			fFolder = sourceFolder;
			fFile = file;
			fExtension = extension;
		}

		private IJavaElement getContainer() {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IPath path = new Path(getPath());
			IResource resource = root.findMember(path);
			if (resource != null) {
				IJavaElement elem = JavaCore.create(resource);
				if (elem != null && elem.exists()) {
					return elem;
				}
			}
			return null;
		}

		private String getPath() {
			StringBuffer result = new StringBuffer();
			result.append(SEPARATOR);
			result.append(fProject);
			result.append(SEPARATOR);
			if (fFolder != null && fFolder.length() > 0) {
				result.append(fFolder);
				result.append(SEPARATOR);
			}
			if (pkg != null && pkg.length() > 0) {
				result.append(pkg.replace(PACKAGE_PART_SEPARATOR, SEPARATOR));
				result.append(SEPARATOR);
			}
			result.append(fFile);
			result.append('.');
			result.append(fExtension);
			return result.toString();
		}
	}
}
