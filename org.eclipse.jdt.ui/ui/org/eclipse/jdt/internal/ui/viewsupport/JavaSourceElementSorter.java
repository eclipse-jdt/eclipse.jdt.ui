package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Sorts elements in compilationUnits and classfiles
 */
public class JavaSourceElementSorter extends ViewerSorter {
	
	private static final int INNER_TYPES=	0;
	private static final int CONSTRUCTORS=	1;
	private static final int STATIC_INIT= 2;
	private static final int STATIC_METHODS= 3;
	private static final int INIT= 4;
	private static final int METHODS= 5;
	private static final int STATIC_FIELDS= 6;
	private static final int FIELDS= 7;
	private static final int OTHERS= 8;
	
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
					
					case IJavaElement.TYPE: {
						if (((IType)element).getDeclaringType() == null) {
							return INNER_TYPES;
						}
						break;
					}
					
				}
			
			} catch (JavaModelException x) {
				JavaPlugin.log(x);
			}
		}
		return OTHERS;
	}
	
	/*
	 * @see ViewerSorter#compare
	 */
	public int compare(Viewer viewer, Object e1, Object e2) {	
		// do not sort elements not in a type
		// assume that both elements on the same level
		if (e1 instanceof IJavaElement) {
			if (((IJavaElement)e1).getParent().getElementType() == IJavaElement.TYPE) {
				return super.compare(viewer, e1, e2);
			}
		}
		return 0;
	}
			
	
	
	
};