/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui;

import java.text.Collator;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * Sorter for Java elements. Ordered by element category, then by element name. 
 * Package fragment roots are sorted as ordered on the classpath.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class JavaElementSorter extends ViewerSorter {
	
	private static final int JAVAPROJECTS= 1;
	private static final int PACKAGEFRAGMENTROOTS= 2;
	private static final int PACKAGEFRAGMENT= 3;

	private static final int COMPILATIONUNITS= 4;
	private static final int CLASSFILES= 5;
	
	private static final int RESOURCEFOLDERS= 7;
	private static final int RESOURCES= 8;
	private static final int STORAGE= 9;	
	
	private static final int PACKAGE_DECL=	10;
	private static final int IMPORT_CONTAINER= 11;
	private static final int IMPORT_DECLARATION= 12;
	
	// Includes all categories ordered using the OutlineSortOrderPage:
	// types, initializers, methods & fields
	private static final int MEMBERSOFFSET= 15;
	
	private static final int JAVAELEMENTS= 50;
	private static final int OTHERS= 51;
	
	// cache for configurable member sort order
	
	// index to cache array
	private static final int TYPE_INDEX= 0;
	private static final int CONSTRUCTORS_INDEX= 1;
	private static final int METHOD_INDEX= 2;
	private static final int FIELDS_INDEX= 3;
	private static final int INIT_INDEX= 4;
	private static final int STATIC_FIELDS_INDEX= 5;
	private static final int STATIC_INIT_INDEX= 6;
	private static final int STATIC_METHODS_INDEX= 7;
	private static final int N_ENTRIES= STATIC_METHODS_INDEX + 1;

	private static Cache fgCache= null;
	
	private static int getMemberCategory(int kind) {
		if (fgCache == null) {
			fgCache= new Cache();
			PreferenceConstants.getPreferenceStore().addPropertyChangeListener(fgCache);
		}			
		return fgCache.getIndex(kind) + MEMBERSOFFSET;
	}		

	private static class Cache implements IPropertyChangeListener {
		private int[] fOffsets= null;

		public void propertyChange(PropertyChangeEvent event) {
			fOffsets= null;
		}

		public int getIndex(int kind) {
			if (fOffsets == null) {
				fOffsets= getOffsets();
			}
			return fOffsets[kind];
		}
		
		private int[] getOffsets() {
			int[] offsets= new int[N_ENTRIES];
			IPreferenceStore store= PreferenceConstants.getPreferenceStore();
			String key= PreferenceConstants.APPEARANCE_MEMBER_SORT_ORDER;
			boolean success= fillOffsetsFromPreferenceString(store.getString(key), offsets);
			if (!success) {
				store.setValue(key, null);
				fillOffsetsFromPreferenceString(store.getDefaultString(key), offsets);	
			}
			return offsets;
		}		
		
		private boolean fillOffsetsFromPreferenceString(String str, int[] offsets) {
			StringTokenizer tokenizer= new StringTokenizer(str, ","); //$NON-NLS-1$
			int i= 0;
			while (tokenizer.hasMoreTokens()) {
				String token= tokenizer.nextToken().trim();
				if ("T".equals(token)) {
					offsets[TYPE_INDEX]= i++;
				} else if ("M".equals(token)) {
					offsets[METHOD_INDEX]= i++;
				} else if ("F".equals(token)) {
					offsets[FIELDS_INDEX]= i++;
				} else if ("I".equals(token)) {
					offsets[INIT_INDEX]= i++;
				} else if ("SF".equals(token)) {
					offsets[STATIC_FIELDS_INDEX]= i++;
				} else if ("SI".equals(token)) {
					offsets[STATIC_INIT_INDEX]= i++;
				} else if ("SM".equals(token)) {
					offsets[STATIC_METHODS_INDEX]= i++;
				} else if ("C".equals(token)) {
					offsets[CONSTRUCTORS_INDEX]= i++;
				}
			}
			return i == N_ENTRIES;
		}
	}
	
	public JavaElementSorter() {
		super(null); // delay initialization of collator
	}
	
	/**
	 * @deprecated Bug 22518. Method never used: does not override ViewerSorter#isSorterProperty(Object, String).
	 * Method could be removed, but kept for API compatibility.
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
					case IJavaElement.METHOD:
						{
							IMethod method= (IMethod) je;
							if (method.isConstructor()) {
								return getMemberCategory(CONSTRUCTORS_INDEX);
							}
							int flags= method.getFlags();
							if (Flags.isStatic(flags))
								return getMemberCategory(STATIC_METHODS_INDEX);
							else
								return getMemberCategory(METHOD_INDEX);
						}
					case IJavaElement.FIELD :
						{
							int flags= ((IField) je).getFlags();
							if (Flags.isStatic(flags))
								return getMemberCategory(STATIC_FIELDS_INDEX);
							else
								return getMemberCategory(FIELDS_INDEX);
						}
					case IJavaElement.INITIALIZER :
						{
							int flags= ((IInitializer) je).getFlags();
							if (Flags.isStatic(flags))
								return getMemberCategory(STATIC_INIT_INDEX);
							else
								return getMemberCategory(INIT_INDEX);
						}
					case IJavaElement.TYPE :
						return getMemberCategory(TYPE_INDEX);
					case IJavaElement.PACKAGE_DECLARATION :
						return PACKAGE_DECL;
					case IJavaElement.IMPORT_CONTAINER :
						return IMPORT_CONTAINER;
					case IJavaElement.IMPORT_DECLARATION :
						return IMPORT_DECLARATION;
					case IJavaElement.PACKAGE_FRAGMENT :
						IPackageFragment pack= (IPackageFragment) je;
						if (pack.getParent().getResource() instanceof IProject) {
							return PACKAGEFRAGMENTROOTS;
						}
						return PACKAGEFRAGMENT;
					case IJavaElement.PACKAGE_FRAGMENT_ROOT :
						return PACKAGEFRAGMENTROOTS;
					case IJavaElement.JAVA_PROJECT :
						return JAVAPROJECTS;
					case IJavaElement.CLASS_FILE :
						return CLASSFILES;
					case IJavaElement.COMPILATION_UNIT :
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
		
		String name1= ((IJavaElement) e1).getElementName();
		String name2= ((IJavaElement) e2).getElementName();
		
		// java element are sorted by name
		int cmp= getCollator().compare(name1, name2);
		if (cmp != 0) {
			return cmp;
		}
		
		if (e1 instanceof IMethod) {
			String[] params1= ((IMethod) e1).getParameterTypes();
			String[] params2= ((IMethod) e2).getParameterTypes();
			int len= Math.min(params1.length, params2.length);
			for (int i = 0; i < len; i++) {
				cmp= getCollator().compare(Signature.toString(params1[i]), Signature.toString(params2[i]));
				if (cmp != 0) {
					return cmp;
				}
			}
			return params1.length - params2.length;
		}
		return 0;
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerSorter#getCollator()
	 */
	public final Collator getCollator() {
		if (collator == null) {
			collator= Collator.getInstance();
		}
		return collator;
	}

}