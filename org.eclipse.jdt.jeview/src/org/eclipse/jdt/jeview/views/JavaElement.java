/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabels;


public class JavaElement extends JEAttribute {
	
	private static final long LABEL_OPTIONS= JavaElementLabels.F_APP_TYPE_SIGNATURE | JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_APP_RETURNTYPE | JavaElementLabels.ALL_FULLY_QUALIFIED | JavaElementLabels.T_TYPE_PARAMETERS | JavaElementLabels.USE_RESOLVED;

	private final JEAttribute fParent; //can be null
	private final String fName; //can be null
	private final IJavaElement fJavaElement; //can be null

	private JEAttribute[] fChildren;
	
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
		if (fChildren != null)
			return fChildren;
		
		if (fJavaElement == null) {
			fChildren= EMPTY;
			return fChildren;
		}
		
		ArrayList<JEAttribute> result= new ArrayList<JEAttribute>();
		
		addJavaElementChildren(result, fJavaElement);
		
		if (fJavaElement instanceof IParent) {
			addParentChildren(result, (IParent) fJavaElement);
		}
		
		if (fJavaElement instanceof IJavaModel) {
			addJavaModelChildren(result, (IJavaModel) fJavaElement);
		} else if (fJavaElement instanceof IJavaProject) {
			addJavaProjectChildren(result, (IJavaProject) fJavaElement);
			
		} else if (fJavaElement instanceof IType) {
			addTypeChildren(result, (IType) fJavaElement);
			addMemberChildren(result, (IMember) fJavaElement);
			
		} else if (fJavaElement instanceof IMethod) {
			addMethodChildren(result, (IMethod) fJavaElement);
			addMemberChildren(result, (IMember) fJavaElement);
			
		} else if (fJavaElement instanceof IField) {
			addFieldChildren(result, (IField) fJavaElement);
			addMemberChildren(result, (IMember) fJavaElement);
			
		} else if (fJavaElement instanceof IInitializer) {
			addMemberChildren(result, (IMember) fJavaElement);
		}
		
		fChildren= result.toArray(new JEAttribute[result.size()]);
		return fChildren;
		
	}

	private void addParentChildren(ArrayList<JEAttribute> result, final IParent parent) {
		result.add(new JavaElementProperty(this, "hasChildren") {
			@Override
			public Object computeValue() throws JavaModelException {
				return parent.hasChildren();
			}
		});
		result.add(new JavaElementChildrenProperty(this, "children") {
			@Override
			public JEAttribute[] computeChildren() throws JavaModelException {
				return createJavaElements(this, parent.getChildren());
			}
		});
	}

	private void addJavaElementChildren(ArrayList<JEAttribute> result, final IJavaElement javaElement) {
		result.add(new JavaElementProperty(this, "elementName", javaElement.getElementName()));
		result.add(new JavaElementProperty(this, "elementType", getElementTypeString(javaElement.getElementType())));
		result.add(new JavaElementProperty(this, "handleIdentifier", javaElement.getHandleIdentifier()));
		result.add(new JavaElementProperty(this, "exists", javaElement.exists()));
		result.add(new JavaElementProperty(this, "isReadOnly", javaElement.isReadOnly()));
		result.add(new JavaElementProperty(this, "isStructureKnown") {
			@Override protected Object computeValue() throws JavaModelException {
				return javaElement.isStructureKnown();
			}
		});
		result.add(new JEResource(this, "resource", javaElement.getResource()));
		result.add(new JavaElement(this, "parent", javaElement.getParent()));
		result.add(new JEResource(this, "correspondingResource") {
			@Override protected IResource computeResource() throws JavaModelException {
				return javaElement.getCorrespondingResource();
			}
		});
	}

	private String getElementTypeString(int elementType) {
		String name;
		switch (elementType) {
			case IJavaElement.JAVA_MODEL :
				name= "IJavaModel";
				break;
			case IJavaElement.JAVA_PROJECT :
				name= "IJavaProject";
				break;
			case IJavaElement.PACKAGE_FRAGMENT_ROOT :
				name= "IPackageFragmentRoot";
				break;
			case IJavaElement.PACKAGE_FRAGMENT :
				name= "IPackageFragment";
				break;
			case IJavaElement.COMPILATION_UNIT :
				name= "ICompilationUnit";
				break;
			case IJavaElement.CLASS_FILE :
				name= "IClassFile";
				break;
			case IJavaElement.TYPE :
				name= "IType";
				break;
			case IJavaElement.FIELD :
				name= "IField";
				break;
			case IJavaElement.METHOD :
				name= "IMethod";
				break;
			case IJavaElement.INITIALIZER :
				name= "IInitializer";
				break;
			case IJavaElement.PACKAGE_DECLARATION :
				name= "IPackageDeclaration";
				break;
			case IJavaElement.IMPORT_CONTAINER :
				name= "IImportContainer";
				break;
			case IJavaElement.IMPORT_DECLARATION :
				name= "IImportDeclaration";
				break;
			case IJavaElement.LOCAL_VARIABLE :
				name= "ILocalVariable";
				break;
			case IJavaElement.TYPE_PARAMETER :
				name= "ITypeParameter";
				break;
			default :
				name= "UNKNOWN";
				break;
		}
		return elementType + " (" + name + ")";
	}
	
	private void addJavaModelChildren(ArrayList<JEAttribute> result, final IJavaModel javaModel) {
		result.add(new JavaElementChildrenProperty(this, "javaProjects") {
			@Override
			public JEAttribute[] computeChildren() throws JavaModelException {
				return createJavaElements(this, javaModel.getJavaProjects());
			}
		});
		result.add(new JavaElementChildrenProperty(this, "nonJavaResources") {
			@Override
			public JEAttribute[] computeChildren() throws JavaModelException {
				return createResources(this, javaModel.getNonJavaResources());
			}
		});
	}

	private void addJavaProjectChildren(ArrayList<JEAttribute> result, final IJavaProject project) {
		result.add(new JavaElementChildrenProperty(this, "allPackageFragmentRoots") {
			@Override
			protected JEAttribute[] computeChildren() throws Exception {
				return createJavaElements(this, project.getAllPackageFragmentRoots());
			}
		});
		result.add(new JavaElementChildrenProperty(this, "nonJavaResources") {
			@Override
			protected JEAttribute[] computeChildren() throws Exception {
				return createResources(this, project.getNonJavaResources());
			}
		});
	}

	private void addMemberChildren(ArrayList<JEAttribute> result, final IMember member) {
		result.add(new JavaElement(this, "classFile", member.getClassFile()));
		result.add(new JavaElement(this, "compilationUnit", member.getCompilationUnit()));
	}
	
	private void addTypeChildren(ArrayList<JEAttribute> result, final IType type) {
		result.add(new JavaElementProperty(this, "isResolved", type.isResolved()));
		result.add(new JavaElementProperty(this, "key", type.getKey()));
	}

	private void addFieldChildren(ArrayList<JEAttribute> result, final IField field) {
		result.add(new JavaElementProperty(this, "isResolved", field.isResolved()));
		result.add(new JavaElementProperty(this, "key", field.getKey()));
	}

	private void addMethodChildren(ArrayList<JEAttribute> result, final IMethod method) {
		result.add(new JavaElementProperty(this, "isResolved", method.isResolved()));
		result.add(new JavaElementProperty(this, "key", method.getKey()));
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
				resourceChildren[i]= new JEResource(parent, "", (IResource) resource);
			else
				resourceChildren[i]= new JavaElementProperty(parent, "", resource);
		}
		return resourceChildren;
	}

}
