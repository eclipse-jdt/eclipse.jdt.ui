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
package org.eclipse.jdt.internal.ui.typehierarchy;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.AbstractInformationControl;
import org.eclipse.jdt.internal.ui.typehierarchy.SuperTypeHierarchyViewer.SuperTypeHierarchyContentProvider;
import org.eclipse.jdt.internal.ui.typehierarchy.TraditionalHierarchyViewer.TraditionalHierarchyContentProvider;
import org.eclipse.jdt.internal.ui.viewsupport.DecoratingJavaLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

/**
 *
 */
public class HierarchyInformationControl extends AbstractInformationControl {

	private KeyAdapter fKeyAdapter;
	private TypeHierarchyLifeCycle fLifeCycle;

	public HierarchyInformationControl(Shell parent, int shellStyle, int treeStyle) {
		super(parent, shellStyle, treeStyle);
	}
	
	private KeyAdapter getKeyAdapter() {
		if (fKeyAdapter == null) {
			fKeyAdapter= new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					if (e.keyCode == SWT.F12) {
						toggleHierarchy();					
					}
				}
			};			
		}
		return fKeyAdapter;		
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.JavaOutlineInformationControl#createTreeViewer(org.eclipse.swt.widgets.Composite, int)
	 */
	protected TreeViewer createTreeViewer(Composite parent, int style) {
		Tree tree= new Tree(parent, SWT.SINGLE | (style & ~SWT.MULTI));

		TreeViewer treeViewer= new TreeViewer(tree);
		
		fLifeCycle= new TypeHierarchyLifeCycle(false);

		treeViewer.setSorter(new HierarchyViewerSorter(fLifeCycle));
		treeViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);

		HierarchyLabelProvider labelProvider= new HierarchyLabelProvider(fLifeCycle);
		labelProvider.setTextFlags(JavaElementLabels.ALL_DEFAULT | JavaElementLabels.T_POST_QUALIFIED);
		treeViewer.setLabelProvider(new DecoratingJavaLabelProvider(labelProvider, true, false));
		
		treeViewer.getTree().addKeyListener(getKeyAdapter());		
		
		return treeViewer;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.JavaOutlineInformationControl#createFilterText(org.eclipse.swt.widgets.Composite)
	 */
	protected Text createFilterText(Composite parent) {
		Text text= super.createFilterText(parent);
		text.addKeyListener(getKeyAdapter());
		return text;
	}
	
	protected void toggleHierarchy() {
		TreeViewer treeViewer= getTreeViewer();
		TypeHierarchyContentProvider contentProvider= (TypeHierarchyContentProvider) treeViewer.getContentProvider();
		if (contentProvider instanceof TraditionalHierarchyContentProvider) {
			treeViewer.setContentProvider(new SuperTypeHierarchyContentProvider(fLifeCycle));
		} else {
			treeViewer.setContentProvider(new TraditionalHierarchyContentProvider(fLifeCycle));
		}
		treeViewer.refresh();
		treeViewer.expandAll();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.JavaOutlineInformationControl#setInput(java.lang.Object)
	 */
	public void setInput(Object information) {
		if (!(information instanceof IJavaElement)) {
			inputChanged(null, null);
			return;
		}
		IJavaElement input= null;
		IMember[] locked= null;
		try {
			IJavaElement elem= (IJavaElement) information;
			switch (elem.getElementType()) {
				case IJavaElement.JAVA_PROJECT :
				case IJavaElement.PACKAGE_FRAGMENT_ROOT :
				case IJavaElement.PACKAGE_FRAGMENT :
				case IJavaElement.TYPE :
					input= elem;
					break;
				case IJavaElement.COMPILATION_UNIT :
					input= ((ICompilationUnit) elem).findPrimaryType();
					break;
				case IJavaElement.CLASS_FILE :
					input= ((IClassFile) elem).getType();
					break;
				case IJavaElement.METHOD :
				case IJavaElement.FIELD :
					locked= new IMember[] { (IMember) elem };
				case IJavaElement.INITIALIZER :
					input= ((IMember) elem).getDeclaringType();
					break;
				case IJavaElement.PACKAGE_DECLARATION :
					input= elem.getParent().getParent();
					break;
				case IJavaElement.IMPORT_DECLARATION :
					IImportDeclaration decl= (IImportDeclaration) elem;
					if (decl.isOnDemand()) {
						input= JavaModelUtil.findTypeContainer(decl.getJavaProject(), Signature.getQualifier(decl.getElementName()));
					} else {
						input= decl.getJavaProject().findType(decl.getElementName());
					}
					break;
				default :
					input= null;
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
			
		try {
			fLifeCycle.ensureRefreshedTypeHierarchy(input, JavaPlugin.getActiveWorkbenchWindow());
		} catch (InvocationTargetException e1) {
			input= null;
		} catch (InterruptedException e1) {
			input= null;
		}
		TraditionalHierarchyContentProvider contentProvider= new TraditionalHierarchyContentProvider(fLifeCycle);
		contentProvider.setMemberFilter(locked);
		getTreeViewer().setContentProvider(contentProvider);		
		
		Object selection= null;
		if (locked != null) {
			selection= locked[0];
		} else if (input instanceof IType) {
			selection=  input;
		} else {
			Object[] objects= contentProvider.getElements(fLifeCycle);
			if (objects.length > 0) {
				selection=  objects[0];
			}
		}
		inputChanged(fLifeCycle, selection);
	}



}
