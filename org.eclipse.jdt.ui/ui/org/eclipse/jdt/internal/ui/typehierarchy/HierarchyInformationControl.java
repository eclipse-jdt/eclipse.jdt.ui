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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.AbstractInformationControl;
import org.eclipse.jdt.internal.ui.typehierarchy.TraditionalHierarchyViewer.TraditionalHierarchyContentProvider;
import org.eclipse.jdt.internal.ui.viewsupport.DecoratingJavaLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

/**
 *
 */
public class HierarchyInformationControl extends AbstractInformationControl {
	
	private class HierarchyInformationControlLabelProvider extends HierarchyLabelProvider {

		public HierarchyInformationControlLabelProvider(TypeHierarchyLifeCycle lifeCycle) {
			super(lifeCycle);
		}
				
		protected boolean isDifferentScope(IType type) {
			if (fFocus == null) {
				return super.isDifferentScope(type);
			}
			IMethod[] methods= type.findMethods(fFocus);
			if (methods != null && methods.length > 0) {
				return false;
			}
			return true;			
		}
			
	}
	
	private TypeHierarchyLifeCycle fLifeCycle;
	private HierarchyInformationControlLabelProvider fLabelProvider;
	private Label fLabel;
	
	private IMethod fFocus;

	public HierarchyInformationControl(Shell parent, int shellStyle, int treeStyle) {
		super(parent, shellStyle, treeStyle);
	}
	
	protected Text createFilterText(Composite parent) {
		fLabel= new Label(parent, SWT.NONE);
		// text set later
		fLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fLabel.setFont(JFaceResources.getBannerFont());
		return super.createFilterText(parent);
	}	
		
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.JavaOutlineInformationControl#createTreeViewer(org.eclipse.swt.widgets.Composite, int)
	 */
	protected TreeViewer createTreeViewer(Composite parent, int style) {
		Tree tree= new Tree(parent, SWT.SINGLE | (style & ~SWT.MULTI));
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));

		TreeViewer treeViewer= new TreeViewer(tree);
		treeViewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return element instanceof IType;
			}
		});		
		
		fLifeCycle= new TypeHierarchyLifeCycle(false);

		treeViewer.setSorter(new HierarchyViewerSorter(fLifeCycle));
		treeViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);

		fLabelProvider= new HierarchyInformationControlLabelProvider(fLifeCycle);

		fLabelProvider.setTextFlags(JavaElementLabels.ALL_DEFAULT | JavaElementLabels.T_POST_QUALIFIED);
		treeViewer.setLabelProvider(new DecoratingJavaLabelProvider(fLabelProvider, true, false));	
		
		return treeViewer;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.AbstractInformationControl#setForegroundColor(org.eclipse.swt.graphics.Color)
	 */
	public void setForegroundColor(Color foreground) {
		super.setForegroundColor(foreground);
		fLabel.setForeground(foreground);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.AbstractInformationControl#setBackgroundColor(org.eclipse.swt.graphics.Color)
	 */
	public void setBackgroundColor(Color background) {
		super.setBackgroundColor(background);
		fLabel.setBackground(background);
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
		IMethod locked= null;
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
					IMethod curr= (IMethod) elem;
					if (!curr.isConstructor()) {
						IType declaring= curr.getDeclaringType();
						ITypeHierarchy hierarchy= declaring.newSupertypeHierarchy(null);
						IMethod method= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, declaring, curr.getElementName(), curr.getParameterTypes(), false);
						if (method != null) {
							input= method.getDeclaringType();
							locked= method;
						} else {
							locked= curr;
							input= declaring;
						}
					}
				case IJavaElement.FIELD :
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
		
		fLabel.setText(getHeaderLabel(locked == null ? input : locked));
		try {
			fLifeCycle.ensureRefreshedTypeHierarchy(input, JavaPlugin.getActiveWorkbenchWindow());
		} catch (InvocationTargetException e1) {
			input= null;
		} catch (InterruptedException e1) {
			input= null;
		}
		TraditionalHierarchyContentProvider contentProvider= new TraditionalHierarchyContentProvider(fLifeCycle);
		contentProvider.setMemberFilter(locked != null ? new IMember[] { locked } : null);
		getTreeViewer().setContentProvider(contentProvider);		
		
		fFocus= locked;
		
		Object selection= null;
		if (input instanceof IMember) {
			selection=  input;
		} else {
			Object[] objects= contentProvider.getElements(fLifeCycle);
			if (objects.length > 0) {
				selection=  objects[0];
			}
		}
		inputChanged(fLifeCycle, selection);
	}
	
	private String getHeaderLabel(IJavaElement input) {
		if (input instanceof IMethod) {
			String[] args= { input.getParent().getElementName(), JavaElementLabels.getElementLabel(input, JavaElementLabels.ALL_DEFAULT) };
			return TypeHierarchyMessages.getFormattedString("HierarchyInformationControl.methodhierarchy.label", args); //$NON-NLS-1$
		} else {
			String arg= JavaElementLabels.getElementLabel(input, JavaElementLabels.DEFAULT_QUALIFIED);
			return TypeHierarchyMessages.getFormattedString("HierarchyInformationControl.hierarchy.label", arg);	 //$NON-NLS-1$
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.AbstractInformationControl#getSelectedElement()
	 */
	protected Object getSelectedElement() {
		Object selectedElement= ((IStructuredSelection)getTreeViewer().getSelection()).getFirstElement();
		if (selectedElement instanceof IType && fFocus != null) {
			IMethod[] methods= ((IType) selectedElement).findMethods(fFocus);
			if (methods != null && methods.length > 0) {
				return methods[0];
			}
		}
		return selectedElement;
	}

}
