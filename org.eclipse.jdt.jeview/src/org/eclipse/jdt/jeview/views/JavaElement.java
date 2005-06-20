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

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabels;


public class JavaElement extends JEAttribute {
	
	private static final long LABEL_OPTIONS= JavaElementLabels.F_APP_TYPE_SIGNATURE | JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_APP_RETURNTYPE | JavaElementLabels.ALL_FULLY_QUALIFIED | JavaElementLabels.T_TYPE_PARAMETERS | JavaElementLabels.USE_RESOLVED;

	private final IJavaElement fJavaElement;
	private final JEAttribute fParent;
	
	public JavaElement(JEAttribute parent, IJavaElement element) {
		fParent= parent;
		fJavaElement= element;
	}

	@Override
	public JEAttribute getParent() {
		return fParent;
	}

	@Override
	public String getLabel() {
		if (fJavaElement == null) {
			return "java element: null"; //$NON-NLS-1$
		} else {
			String classname= fJavaElement.getClass().getName();
			return classname.substring(classname.lastIndexOf('.') + 1) + ": " //$NON-NLS-1$ //$NON-NLS-2$
					+ JavaElementLabels.getElementLabel(fJavaElement, LABEL_OPTIONS)
					+ (fJavaElement.exists() ? "" : " (does not exist)");  //$NON-NLS-1$//$NON-NLS-2$
		}
	}

	@Override
	public Image getImage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JEAttribute[] getChildren() {
		ArrayList<JEAttribute> result= new ArrayList<JEAttribute>();
		
		addJavaElementChildren(result, fJavaElement);
		
		if (fJavaElement instanceof IParent) {
			addParentChildren(result, (IParent) fJavaElement);
		}
		
		if (fJavaElement instanceof IJavaModel) {
			addJavaModelChildren(result, (IJavaModel) fJavaElement);
		} else if (fJavaElement instanceof IJavaProject) {
			addJavaProjectChildren(result, (IJavaProject) fJavaElement);
			
		}
		
		return result.toArray(new JEAttribute[result.size()]);
		
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
		result.add(new JavaElementProperty(this, "ElementName", javaElement.getElementName()));
		result.add(new JavaElementProperty(this, "ElementType", getElementTypeString(javaElement.getElementType())));
		result.add(new JavaElementProperty(this, "HandleIdentifier", javaElement.getHandleIdentifier()));
		result.add(new JavaElementProperty(this, "exists", javaElement.exists()));
		result.add(new JavaElementProperty(this, "isReadOnly", javaElement.isReadOnly()));
//		result.add(new JavaElementProperty(this, "isStructureKnown", javaElement.isStructureKnown()));
	}

	private String getElementTypeString(int elementType) {
		String name;
		switch (elementType) {
			case IJavaElement.JAVA_MODEL :
				name= "IJavaModel";
			case IJavaElement.JAVA_PROJECT :
				name= "IJavaProject";
			case IJavaElement.PACKAGE_FRAGMENT_ROOT :
				name= "IPackageFragmentRoot";
			case IJavaElement.PACKAGE_FRAGMENT :
				name= "IPackageFragment";
			case IJavaElement.COMPILATION_UNIT :
				name= "ICompilationUnit";
			case IJavaElement.CLASS_FILE :
				name= "IClassFile";
			case IJavaElement.TYPE :
				name= "IType";
			case IJavaElement.FIELD :
				name= "IField";
			case IJavaElement.METHOD :
				name= "IMethod";
			case IJavaElement.INITIALIZER :
				name= "IInitializer";
			case IJavaElement.PACKAGE_DECLARATION :
				name= "IPackageDeclaration";
			case IJavaElement.IMPORT_CONTAINER :
				name= "IImportContainer";
			case IJavaElement.IMPORT_DECLARATION :
				name= "IImportDeclaration";
			case IJavaElement.LOCAL_VARIABLE :
				name= "ILocalVariable";
			case IJavaElement.TYPE_PARAMETER :
				name= "ITypeParameter";
			default :
				name= "UNKNOWN";
		}
		return elementType + " (" + name + ")";
	}
	
	private void addJavaModelChildren(ArrayList<JEAttribute> result, final IJavaModel javaModel) {
		result.add(new JavaElementChildrenProperty(this, "JavaProjects") {
			@Override
			public JEAttribute[] computeChildren() throws JavaModelException {
				return createJavaElements(this, javaModel.getJavaProjects());
			}
		});
		result.add(new JavaElementChildrenProperty(this, "NonJavaResources") {
			@Override
			public JEAttribute[] computeChildren() throws JavaModelException {
				return createResources(this, javaModel.getNonJavaResources());
			}
		});
	}

	private void addJavaProjectChildren(ArrayList<JEAttribute> result, final IJavaProject project) {
		result.add(new JavaElementChildrenProperty(this, "AllPackageFragmentRoots") {
			@Override
			protected JEAttribute[] computeChildren() throws Exception {
				return createJavaElements(this, project.getAllPackageFragmentRoots());
			}
		});
		result.add(new JavaElementChildrenProperty(this, "NonJavaResources") {
			@Override
			protected JEAttribute[] computeChildren() throws Exception {
				return createResources(this, project.getNonJavaResources());
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
		JavaElementProperty[] resourceChildren= new JavaElementProperty[resources.length];
		for (int i= 0; i < resources.length; i++) {
			resourceChildren[i]= new JavaElementProperty(parent, "", resources[i]);
		}
		return resourceChildren;
	}

}
