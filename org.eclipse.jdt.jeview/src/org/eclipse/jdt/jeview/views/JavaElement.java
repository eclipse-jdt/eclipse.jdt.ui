/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.jeview.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabels;


public class JavaElement extends JEAttribute {
	
	private static final long LABEL_OPTIONS= JavaElementLabels.F_APP_TYPE_SIGNATURE | JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_APP_RETURNTYPE | JavaElementLabels.ALL_FULLY_QUALIFIED | JavaElementLabels.T_TYPE_PARAMETERS | JavaElementLabels.USE_RESOLVED;

	private final JEAttribute fParent; //can be null
	private final String fName; //can be null
	private final IJavaElement fJavaElement; //can be null

	public JavaElement(JEAttribute parent, String name, IJavaElement element) {
		fParent= parent;
		fName= name;
		fJavaElement= element;
	}
	
	public JavaElement(JEAttribute parent, IJavaElement element) {
		this(parent, null, element);
	}

	@Override
	public JEAttribute getParent() {
		return fParent;
	}
	
	public IJavaElement getJavaElement() {
		return fJavaElement;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !obj.getClass().equals(getClass())) {
			return false;
		}
		
		JavaElement other= (JavaElement) obj;
		if (fParent == null) {
			if (other.fParent != null)
				return false;
		} else if (! fParent.equals(other.fParent)) {
			return false;
		}
		
		if (fName == null) {
			if (other.fName != null)
				return false;
		} else if (! fName.equals(other.fName)) {
			return false;
		}
		
		if (fJavaElement == null) {
			if (other.fJavaElement != null)
				return false;
		} else if (! fJavaElement.equals(other.fJavaElement)) {
			return false;
		}
		
		return true;
	}
	
	@Override
	public int hashCode() {
		return (fParent != null ? fParent.hashCode() : 0)
				+ (fName != null ? fName.hashCode() : 0)
				+ (fJavaElement != null ? fJavaElement.hashCode() : 0);
	}

	@Override
	public Object getWrappedObject() {
		return fJavaElement;
	}
	
	@Override
	public String getLabel() {
		StringBuffer sb= new StringBuffer();
		if (fName != null)
			sb.append(fName).append(": ");
		
		if (fJavaElement == null) {
			sb.append("java element: null");
		} else {
			String classname= fJavaElement.getClass().getName();
			sb.append(classname.substring(classname.lastIndexOf('.') + 1)).append(": ");
			sb.append(JavaElementLabels.getElementLabel(fJavaElement, LABEL_OPTIONS));
			if (! fJavaElement.exists())
				sb.append(" (does not exist)");
		}
		return sb.toString();
	}

	@Override
	public JEAttribute[] getChildren() {
		if (fJavaElement == null)
			return EMPTY;
		
		ArrayList<JEAttribute> result= new ArrayList<JEAttribute>();
		
		if (fJavaElement instanceof IParent) {
			addParentChildren(result, (IParent) fJavaElement);
		}
		
		addJavaElementChildren(result, fJavaElement);
		
		if (fJavaElement instanceof IJavaModel)
			addJavaModelChildren(result, (IJavaModel) fJavaElement);
		if (fJavaElement instanceof IJavaProject)
			addJavaProjectChildren(result, (IJavaProject) fJavaElement);
		if (fJavaElement instanceof IPackageFragmentRoot)
			addPackageFragmentRootChildren(result, (IPackageFragmentRoot) fJavaElement);
		if (fJavaElement instanceof IPackageFragment)
			addPackageFragmentChildren(result, (IPackageFragment) fJavaElement);
		
		if (fJavaElement instanceof ITypeRoot)
			addTypeRootChildren(result, (ITypeRoot) fJavaElement);
		if (fJavaElement instanceof IClassFile)
			addClassFileChildren(result, (IClassFile) fJavaElement);
		if (fJavaElement instanceof ICompilationUnit)
			addCompilationUnitChildren(result, (ICompilationUnit) fJavaElement);
		
		if (fJavaElement instanceof IType)
			addTypeChildren(result, (IType) fJavaElement);
		if (fJavaElement instanceof IMethod)
			addMethodChildren(result, (IMethod) fJavaElement);
		if (fJavaElement instanceof IMember)
			addMemberChildren(result, (IMember) fJavaElement);
		
		return result.toArray(new JEAttribute[result.size()]);
		
	}

	private void addParentChildren(ArrayList<JEAttribute> result, final IParent parent) {
		result.add(new JavaElementChildrenProperty(this, "CHILDREN") {
			@Override
			public JEAttribute[] computeChildren() throws JavaModelException {
				return createJavaElements(this, parent.getChildren());
			}
		});
	}

	private void addJavaElementChildren(ArrayList<JEAttribute> result, final IJavaElement javaElement) {
		result.add(new JavaElement(this, "PARENT", javaElement.getParent()));
		result.add(new JavaElement(this, "PRIMARY ELEMENT", javaElement.getPrimaryElement()));
		result.add(new JavaElement(this, "JAVA MODEL", javaElement.getJavaModel()));
		result.add(new JavaElement(this, "JAVA PROJECT", javaElement.getJavaProject()));
		result.add(JEResource.create(this, "RESOURCE", javaElement.getResource()));
		result.add(JEResource.compute(this, "CORRESPONDING RESOURCE", new Callable<IResource>() {
			public IResource call() throws JavaModelException {
				return javaElement.getCorrespondingResource();
			}
		}));
		result.add(JEResource.compute(this, "UNDERLYING RESOURCE", new Callable<IResource>() {
			public IResource call() throws JavaModelException {
				return javaElement.getUnderlyingResource();
			}
		}));
	}

	private void addJavaModelChildren(ArrayList<JEAttribute> result, final IJavaModel javaModel) {
		result.add(new JavaElementChildrenProperty(this, "JAVA PROJECTS") {
			@Override
			public JEAttribute[] computeChildren() throws JavaModelException {
				return createJavaElements(this, javaModel.getJavaProjects());
			}
		});
		result.add(new JavaElementChildrenProperty(this, "NON JAVA RESOURCES") {
			@Override
			public JEAttribute[] computeChildren() throws JavaModelException {
				return createResources(this, javaModel.getNonJavaResources());
			}
		});
	}

	private void addJavaProjectChildren(ArrayList<JEAttribute> result, final IJavaProject project) {
		//TODO: IClassPathEntry
		result.add(new JavaElementChildrenProperty(this, "ALL PACKAGE FRAGMENT ROOTS") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createJavaElements(this, project.getAllPackageFragmentRoots());
			}
		});
		result.add(new JavaElementChildrenProperty(this, "PACKAGE FRAGMENT ROOTS") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createJavaElements(this, project.getPackageFragmentRoots());
			}
		});
		result.add(new JavaElementChildrenProperty(this, "PACKAGE FRAGMENTS") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createJavaElements(this, project.getPackageFragments());
			}
		});
		result.add(new JavaElementChildrenProperty(this, "NON JAVA RESOURCES") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createResources(this, project.getNonJavaResources());
			}
		});
		result.add(JEResource.create(this, "PROJECT", project.getProject()));
		result.add(new JavaElementChildrenProperty(this, "REQUIRED PROJECT NAMES") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createStrings(this, project.getRequiredProjectNames());
			}
		});
		result.add(new JavaElementChildrenProperty(this, "OPTIONS(FALSE)") {
			@SuppressWarnings("unchecked")
			@Override
			protected JEAttribute[] computeChildren() {
				return createOptions(this, project.getOptions(false));
			}
		});
		result.add(new JavaElementChildrenProperty(this, "OPTIONS(TRUE)") {
			@SuppressWarnings("unchecked")
			@Override
			protected JEAttribute[] computeChildren() {
				return createOptions(this, project.getOptions(true));
			}
		});
		result.add(new JavaElementChildrenProperty(this, "RAW CLASSPATH") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createCPEntries(this, project.getRawClasspath());
			}
		});
		result.add(new JavaElementChildrenProperty(this, "RESOLVED CLASSPATH") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createCPEntries(this, project.getResolvedClasspath(false));
			}
		});
	}
	
	private void addPackageFragmentRootChildren(ArrayList<JEAttribute> result, final IPackageFragmentRoot packageFragmentRoot) {
		result.add(new JavaElementChildrenProperty(this, "NON JAVA RESOURCES") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createResources(this, packageFragmentRoot.getNonJavaResources());
			}
		});
	}

	private void addPackageFragmentChildren(ArrayList<JEAttribute> result, final IPackageFragment packageFragment) {
		result.add(new JavaElementChildrenProperty(this, "COMPILATION UNITS") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createJavaElements(this, packageFragment.getCompilationUnits());
			}
		});
		result.add(new JavaElementChildrenProperty(this, "CLASS FILES") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createJavaElements(this, packageFragment.getClassFiles());
			}
		});
		result.add(new JavaElementChildrenProperty(this, "NON JAVA RESOURCES") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createResources(this, packageFragment.getNonJavaResources());
			}
		});
	}
	
	private void addTypeRootChildren(ArrayList<JEAttribute> result, final ITypeRoot typeRoot) {
		result.add(JavaElement.compute(this, "FIND PRIMARY TYPE", new Callable<IJavaElement>() {
			public IJavaElement call() {
				return typeRoot.findPrimaryType();
			}
		}));
	}
	
	private void addClassFileChildren(ArrayList<JEAttribute> result, final IClassFile classFile) {
		result.add(JavaElement.compute(this, "TYPE", new Callable<IJavaElement>() {
			public IJavaElement call() throws JavaModelException {
				return classFile.getType();
			}
		}));
	}

	private void addCompilationUnitChildren(ArrayList<JEAttribute> result, final ICompilationUnit compilationUnit) {
		//TODO: WorkingCopyOwner
		result.add(new JavaElement(this, "PRIMARY", compilationUnit.getPrimary()));
		result.add(new JavaElementChildrenProperty(this, "TYPES") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createJavaElements(this, compilationUnit.getTypes());
			}
		});
		result.add(new JavaElementChildrenProperty(this, "ALL TYPES") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createJavaElements(this, compilationUnit.getAllTypes());
			}
		});
		result.add(new JavaElement(this, "IMPORT CONTAINER", compilationUnit.getImportContainer()));
		result.add(new JavaElementChildrenProperty(this, "IMPORTS") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createJavaElements(this, compilationUnit.getImports());
			}
		});
		result.add(new JavaElementChildrenProperty(this, "PACKAGE DECLARATIONS") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createJavaElements(this, compilationUnit.getPackageDeclarations());
			}
		});
	}
	
	private void addMemberChildren(ArrayList<JEAttribute> result, final IMember member) {
		result.add(new JavaElement(this, "CLASS FILE", member.getClassFile()));
		result.add(new JavaElement(this, "COMPILATION UNIT", member.getCompilationUnit()));
		result.add(new JavaElement(this, "TYPE ROOT", member.getTypeRoot()));
		result.add(new JavaElement(this, "DECLARING TYPE", member.getDeclaringType()));
		result.add(new JavaElementChildrenProperty(this, "CATEGORIES") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createStrings(this, member.getCategories());
			}
		});
	}
	
	private void addTypeChildren(ArrayList<JEAttribute> result, final IType type) {
		result.add(new JavaElementProperty(this, "IS RESOLVED", type.isResolved()));
		result.add(new JavaElementProperty(this, "KEY", type.getKey()));
		result.add(new JavaElement(this, "PACKAGE FRAGMENT", type.getPackageFragment()));
		result.add(new JavaElementChildrenProperty(this, "TYPE PARAMETERS") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createJavaElements(this, type.getTypeParameters());
			}
		});
		result.add(new JavaElementChildrenProperty(this, "TYPE PARAMETER SIGNATURES") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createStrings(this, type.getTypeParameterSignatures());
			}
		});
		
		result.add(new JavaElementProperty(this, "SUPERCLASS NAME") {
			@Override
			protected Object computeValue() throws Exception {
				return type.getSuperclassName();
			}
		});
		
		result.add(new JavaElementProperty(this, "SUPERCLASS TYPE SIGNATURE") {
			@Override
			protected Object computeValue() throws Exception {
				return type.getSuperclassTypeSignature();
			}
		});
		result.add(new JavaElementChildrenProperty(this, "SUPER INTERFACE NAMES") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createStrings(this, type.getSuperInterfaceNames());
			}
		});
		result.add(new JavaElementChildrenProperty(this, "SUPER INTERFACE TYPE SIGNATURES") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createStrings(this, type.getSuperInterfaceTypeSignatures());
			}
		});
		
		result.add(new JavaElementChildrenProperty(this, "FIELDS") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createJavaElements(this, type.getFields());
			}
		});
		result.add(new JavaElementChildrenProperty(this, "INITIALIZERS") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createJavaElements(this, type.getInitializers());
			}
		});
		result.add(new JavaElementChildrenProperty(this, "METHODS") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createJavaElements(this, type.getMethods());
			}
		});
		result.add(new JavaElementChildrenProperty(this, "TYPES") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createJavaElements(this, type.getTypes());
			}
		});
	}

	private void addMethodChildren(ArrayList<JEAttribute> result, final IMethod method) {
		result.add(new JavaElementChildrenProperty(this, "EXCEPTION TYPES") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createStrings(this, method.getExceptionTypes());
			}
		});
		result.add(new JavaElementChildrenProperty(this, "PARAMETER NAMES") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createStrings(this, method.getParameterNames());
			}
		});
		result.add(new JavaElementChildrenProperty(this, "PARAMETER TYPES") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createStrings(this, method.getParameterTypes());
			}
		});
		result.add(new JavaElementChildrenProperty(this, "TYPE PARAMETERS") {
			@Override
			protected JEAttribute[] computeChildren() throws JavaModelException {
				return createJavaElements(this, method.getTypeParameters());
			}
		});
	}
	
	static JavaElement[] createJavaElements(JEAttribute parent, IJavaElement[] javaElements) {
		JavaElement[] jeChildren= new JavaElement[javaElements.length];
		for (int i= 0; i < javaElements.length; i++) {
			jeChildren[i]= new JavaElement(parent, javaElements[i]);
		}
		return jeChildren;
	}
	
	static JEAttribute[] createResources(JEAttribute parent, Object[] resources) {
		JEAttribute[] resourceChildren= new JEAttribute[resources.length];
		for (int i= 0; i < resources.length; i++) {
			Object resource= resources[i];
			if (resource instanceof IResource)
				resourceChildren[i]= new JEResource(parent, null, (IResource) resource);
			else if (resource instanceof IJarEntryResource)
				resourceChildren[i]= new JEJarEntryResource(parent, null, (IJarEntryResource) resource);
			else
				resourceChildren[i]= new JavaElementProperty(parent, null, resource);
		}
		return resourceChildren;
	}
	
	static JEAttribute[] createCPEntries(JEAttribute parent, IClasspathEntry[] entries) {
		JEAttribute[] entryChildren= new JEAttribute[entries.length];
		for (int i= 0; i < entries.length; i++) {
			IClasspathEntry entry= entries[i];
			entryChildren[i]= new JavaElementProperty(parent, null, entry);
		}
		return entryChildren;
	}

	static JEAttribute[] createOptions(JEAttribute parent, Map<String, String> options) {
		ArrayList<Entry<String, String>> entries= new ArrayList<Entry<String, String>>(options.entrySet());
		Collections.sort(entries, new Comparator<Entry<String, String>>() {
			public int compare(Entry<String, String> o1, Entry<String, String> o2) {
				return o1.getKey().compareTo(o2.getKey());
			}
		});
		
		JEAttribute[] children= new JEAttribute[entries.size()];
		for (int i= 0; i < entries.size(); i++) {
			Entry<String, String> entry= entries.get(i);
			children[i]= new JavaElementProperty(parent, entry.getKey(), entry.getValue());
		}
		return children;
	}
	
	static JEAttribute[] createStrings(JEAttribute parent, String[] strings) {
		JEAttribute[] children= new JEAttribute[strings.length];
		for (int i= 0; i < strings.length; i++) {
			children[i]= new JavaElementProperty(parent, null, strings[i]);
		}
		return children;
	}

	public static JEAttribute compute(JEAttribute parent, String name, Callable<IJavaElement> computer) {
		try {
			IJavaElement javaElement= computer.call();
			return create(parent, name, javaElement);
		} catch (Exception e) {
			return new Error(parent, name, e);
		}
	}
	
	public static JEAttribute create(JEAttribute parent, String name, IJavaElement javaElement) {
		if (javaElement == null) {
			return new Null(parent, name);
		} else {
			return new JavaElement(parent, name, javaElement);
		}
	}

}
