package org.eclipse.jdt.internal.ui.viewsupport;

import java.text.Collator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Sorts Java elements:
 * Package fragment roots are sorted as ordered in the classpath.
 */
public class JavaElementSorter extends ViewerSorter {
	
	private static final int PACKAGE_DECL=	1;
	private static final int IMPORT_CONTAINER=	2;
	private static final int TYPES= 3;
	private static final int CONSTRUCTORS=	4;
	private static final int STATIC_INIT= 5;
	private static final int STATIC_METHODS= 6;
	private static final int INIT= 7;
	private static final int METHODS= 8;
	private static final int STATIC_FIELDS= 9;
	private static final int FIELDS= 10;
	private static final int JAVAELEMENTS= 11;
	private static final int PACKAGEFRAGMENTROOTS= 12;
	private static final int PACKAGEFRAGMENT= 13;
	private static final int JAVAPROJECTS= 14;
	private static final int RESOURCEPACKAGES= 15;
	private static final int RESOURCEFOLDERS= 16;
	private static final int RESOURCES= 17;
	private static final int STORAGE= 18;
	
	private static final int OTHERS= 20;	

	private IClasspathEntry[] fClassPath;

	/*
	 * @see ViewerSorter#sort
	 */
	public void sort(Viewer v, Object[] property) {
		fClassPath= null;
		try {
			super.sort(v, property);
		} finally {
			fClassPath= null;
		}
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
					case IJavaElement.PACKAGE_FRAGMENT:
						IPackageFragment pack= (IPackageFragment) je;
						if (!pack.hasChildren() && pack.getNonJavaResources().length > 0) {
							return RESOURCEPACKAGES;
						}
						if (pack.getParent().getUnderlyingResource() instanceof IProject) {
							return PACKAGEFRAGMENTROOTS;
						}
						return PACKAGEFRAGMENT;
					case IJavaElement.PACKAGE_FRAGMENT_ROOT:
						return PACKAGEFRAGMENTROOTS;
					case IJavaElement.JAVA_PROJECT:
						return JAVAPROJECTS;
				}
			
			} catch (JavaModelException x) {
				JavaPlugin.log(x);
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
		
		Collator collator= getCollator();
		
		switch (cat1) {
			case OTHERS:
				// unknown element type
				return 0;
			case PACKAGEFRAGMENTROOTS:
				int p1= getClassPathIndex(JavaModelUtil.getPackageFragmentRoot((IJavaElement)e1));
				int p2= getClassPathIndex(JavaModelUtil.getPackageFragmentRoot((IJavaElement)e2));
				if (p1 == p2) {
					return collator.compare(((IJavaElement)e1).getElementName(), ((IJavaElement)e2).getElementName());
				}
				return p1 - p2;
			case STORAGE:
				return collator.compare(((IStorage)e1).getName(), ((IStorage)e2).getName());
			case RESOURCES:
			case RESOURCEFOLDERS:
				return collator.compare(((IResource)e1).getName(), ((IResource)e2).getName());
			default:
				return collator.compare(((IJavaElement)e1).getElementName(), ((IJavaElement)e2).getElementName());
		}
	}
			
	private int getClassPathIndex(IPackageFragmentRoot root) {
		try {
			if (fClassPath == null)
				fClassPath= root.getJavaProject().getResolvedClasspath(true);
		} catch (JavaModelException e) {
			return Integer.MAX_VALUE;
		}
		for (int i= 0; i < fClassPath.length; i++) {
			if (fClassPath[i].getPath().equals(root.getPath()))
				return i;
		}
		return Integer.MAX_VALUE;
	}	
	
	
};