/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.DecoratingJavaLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;
import org.eclipse.jdt.internal.ui.viewsupport.MemberFilter;

/**
 * @author dmegert
 */
public class JavaOutlineInformationControl extends AbstractInformationControl {

	public JavaOutlineInformationControl(Shell parent, int shellStyle, int treeStyle) {
		super(parent, shellStyle, treeStyle);
	}

	protected TreeViewer createTreeViewer(Composite parent, int style) {
		Tree tree= new Tree(parent, SWT.SINGLE | (style & ~SWT.MULTI));
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
	
		TreeViewer treeViewer= new TreeViewer(tree);
	
		// Hide import declartions but show the container
		treeViewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return !(element instanceof IImportDeclaration);
			}
		});
		treeViewer.addFilter(new NamePatternFilter());
		
		treeViewer.addFilter(new MemberFilter());
	
		treeViewer.setContentProvider(new StandardJavaElementContentProvider(true, true));
		treeViewer.setSorter(new JavaElementSorter());
		treeViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
		
		AppearanceAwareLabelProvider lprovider= new AppearanceAwareLabelProvider(
			AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS |  JavaElementLabels.F_APP_TYPE_SIGNATURE,
			AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS
		);
		treeViewer.setLabelProvider(new DecoratingJavaLabelProvider(lprovider));

		return treeViewer;
	}

	public void setInput(Object information) {
		if (information == null || information instanceof String) {
			inputChanged(null, null);
			return;
		}
		IJavaElement je= (IJavaElement)information;
		IJavaElement sel= null;
		ICompilationUnit cu= (ICompilationUnit)je.getAncestor(IJavaElement.COMPILATION_UNIT);
		if (cu != null)
			sel= cu;
		else
			sel= je.getAncestor(IJavaElement.CLASS_FILE);
			
		inputChanged(sel, information);
	}
}
