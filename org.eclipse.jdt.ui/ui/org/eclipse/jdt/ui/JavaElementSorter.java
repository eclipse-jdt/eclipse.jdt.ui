/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

/**
 * Sorter for Java elements. Ordered by element category, then by element name. 
 * Package fragment roots are sorted as ordered on the classpath.
 * @since 2.0
 */
public class JavaElementSorter extends ViewerSorter {
	
	private static final int JAVAPROJECTS= 1;
	private static final int PACKAGEFRAGMENTROOTS= 2;
	private static final int PACKAGEFRAGMENT= 3;
	private static final int RESOURCEPACKAGES= 6;
	private static final int COMPILATIONUNITS= 4;
	private static final int CLASSFILES= 5;
	
	private static final int RESOURCEFOLDERS= 7;
	private static final int RESOURCES= 8;
	private static final int STORAGE= 9;	
	
	private static final int PACKAGE_DECL=	10;
	private static final int IMPORT_CONTAINER= 11;
	private static final int IMPORT_DECLARATION= 12;
	private static final int TYPES= 13;
	private static final int STATIC_INIT= 14;
	private static final int STATIC_FIELDS= 15;	
	private static final int STATIC_METHODS= 16;	

	private static final int FIELDS= 17;	
	private static final int CONSTRUCTORS=	18;
	private static final int INIT= 19;
	private static final int METHODS= 20;
	
	private static final int JAVAELEMENTS= 21;	
	private static final int OTHERS= 22;	
	
	public JavaElementSorter() {
	}
	
	/*
	 * @see ViewerSorter#isSorterProperty
	 */		
	public boolean isSorterProperty(Object element, Object property) {
		return true;
	}

	/*
	 * @see ViewerSorter#category
	 */	
	public int category(Object element) {
		if (element instanceof IJavaElement) {
			try {
				IJavaElement je= (IJavaElement) element;
				
				switch (je.getElementType()) {
					case IJavaElement.METHOD: {
						IMethod method= (IMethod) je;
						if (method.isConstructor())
							return CONSTRUCTORS;
							
						int flags= method.getFlags();
						return Flags.isStatic(flags) ? STATIC_METHODS : METHODS;
					}
					
					case IJavaElement.FIELD: {
						int flags= ((IField) je).getFlags();
						return Flags.isStatic(flags) ? STATIC_FIELDS : FIELDS;
					}
					
					case IJavaElement.INITIALIZER: {
						int flags= ((IInitializer) je).getFlags();
						return Flags.isStatic(flags) ? STATIC_INIT : INIT;
					}
					
					case IJavaElement.TYPE:
						return TYPES;
					case IJavaElement.PACKAGE_DECLARATION:
						return PACKAGE_DECL;
					case IJavaElement.IMPORT_CONTAINER:
						return IMPORT_CONTAINER;
					case IJavaElement.IMPORT_DECLARATION:
						return IMPORT_DECLARATION;						
					case IJavaElement.PACKAGE_FRAGMENT:
						IPackageFragment pack= (IPackageFragment) je;
						if (pack.getParent().getUnderlyingResource() instanceof IProject) {
							return PACKAGEFRAGMENTROOTS;
						}
						if (!pack.hasChildren() && pack.getNonJavaResources().length > 0) {
							return RESOURCEPACKAGES;
						}
						return PACKAGEFRAGMENT;
					case IJavaElement.PACKAGE_FRAGMENT_ROOT:
						return PACKAGEFRAGMENTROOTS;
					case IJavaElement.JAVA_PROJECT:
						return JAVAPROJECTS;
					case IJavaElement.CLASS_FILE:
						return CLASSFILES;
					case IJavaElement.COMPILATION_UNIT:
						return COMPILATIONUNITS;				
				}
			
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
			return JAVAELEMENTS;
		} else if (element instanceof IFile) {
			return RESOURCES;
		} else if (element instanceof IContainer) {
			return RESOURCEFOLDERS;	
		} else if (element instanceof IStorage) {
			return STORAGE;
		}			
		return OTHERS;
	}
	
	/*
	 * @see ViewerSorter#compare
	 */
	public int compare(Viewer viewer, Object e1, Object e2) {
		int cat1= category(e1);
		int cat2= category(e2);

		if (cat1 != cat2)
			return cat1 - cat2;	
		
		if (cat1 == PACKAGEFRAGMENTROOTS) {
			IPackageFragmentRoot root1= JavaModelUtil.getPackageFragmentRoot((IJavaElement)e1);
			IPackageFragmentRoot root2= JavaModelUtil.getPackageFragmentRoot((IJavaElement)e2);
			if (!root1.getPath().equals(root2.getPath())) {
				int p1= getClassPathIndex(root1);
				int p2= getClassPathIndex(root2);
				if (p1 != p2) {
					return p1 - p2;
				}
			}
		}
		// non - java resources are sorted using the label from the viewers label provider
		if (cat1 == RESOURCES || cat1 == RESOURCEFOLDERS || cat1 == STORAGE || cat1 == OTHERS) {
			return compareWithLabelProvider(viewer, e1, e2);
		}
		
		if (cat1 == METHODS || cat1 == STATIC_METHODS || cat1 == CONSTRUCTORS) {
			IMethod m1= (IMethod) e1;
			IMethod m2= (IMethod) e2;
			int nParamDiff= m1.getParameterTypes().length - m2.getParameterTypes().length;
			if (nParamDiff != 0) {
				return nParamDiff;
			}
		}
		
		// java element are sorted by name (and parameter types)
		String name1= JavaElementLabels.getTextLabel(e1, JavaElementLabels.M_PARAMETER_TYPES);
		String name2= JavaElementLabels.getTextLabel(e2, JavaElementLabels.M_PARAMETER_TYPES);
		return getCollator().compare(name1, name2);	
	}
	
	private int compareWithLabelProvider(Viewer viewer, Object e1, Object e2) {
		if (viewer == null || !(viewer instanceof ContentViewer)) {
			IBaseLabelProvider prov = ((ContentViewer) viewer).getLabelProvider();
			if (prov instanceof ILabelProvider) {
				ILabelProvider lprov= (ILabelProvider) prov;
				String name1 = lprov.getText(e1);
				String name2 = lprov.getText(e2);
				if (name1 != null && name2 != null) {
					return getCollator().compare(name1, name2);
				}
			}
		}
		return 0; // can't compare
	}
			
	private int getClassPathIndex(IPackageFragmentRoot root) {
		try {
			IPath rootPath= root.getPath();
			IPackageFragmentRoot[] roots= root.getJavaProject().getPackageFragmentRoots();
			for (int i= 0; i < roots.length; i++) {
				if (roots[i].getPath().equals(rootPath)) {
					return i;
				}
			}
		} catch (JavaModelException e) {
		}

		return Integer.MAX_VALUE;
	}	
}