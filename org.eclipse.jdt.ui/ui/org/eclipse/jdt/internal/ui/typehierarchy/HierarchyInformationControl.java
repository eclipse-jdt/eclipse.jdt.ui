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
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommand;
import org.eclipse.ui.commands.ICommandManager;
import org.eclipse.ui.commands.IKeySequenceBinding;
import org.eclipse.ui.internal.keys.KeySupport;
import org.eclipse.ui.keys.KeySequence;
import org.eclipse.ui.keys.KeyStroke;
import org.eclipse.ui.keys.SpecialKey;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;

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
				try {
					// check visibility
					IPackageFragment pack= (IPackageFragment) fFocus.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
					for (int i= 0; i < methods.length; i++) {
						IMethod curr= methods[i];
						if (JavaModelUtil.isVisibleInHierarchy(curr, pack)) {
							return false;
						}
					}
				} catch (JavaModelException e) {
					// ignore
					JavaPlugin.log(e);
				}
			}
			return true;			
		}	
	}
	
	private TypeHierarchyLifeCycle fLifeCycle;
	private HierarchyInformationControlLabelProvider fLabelProvider;
	private Label fHeaderLabel;
	private Label fInfoLabel;
	private KeyAdapter fKeyAdapter;
	
	private Object[] fOtherExpandedElements;
	private TypeHierarchyContentProvider fOtherContentProvider;
	
	private IMethod fFocus; // method to filter for or null if type hierarchy
	private boolean fDoFilter;
	
	private KeySequence[] fKeySequences;

	public HierarchyInformationControl(Shell parent, int shellStyle, int treeStyle) {
		super(parent, shellStyle, treeStyle);
		fOtherExpandedElements= null;
		fKeySequences= null;
		fDoFilter= true;
	}
	
	private KeySequence[] getKeySequences() {
		if (fKeySequences == null) {
			ICommandManager commandManager = PlatformUI.getWorkbench().getCommandManager();
			ICommand command = commandManager.getCommand(IJavaEditorActionDefinitionIds.OPEN_HIERARCHY);
			if (command.isDefined()) {
				List list= command.getKeySequenceBindings();
				if (!list.isEmpty()) {
					fKeySequences= new KeySequence[list.size()];
					for (int i= 0; i < fKeySequences.length; i++) {
						fKeySequences[i]= ((IKeySequenceBinding) list.get(i)).getKeySequence();
					}
					return fKeySequences;
				}		
			}
			// default key is F12
			fKeySequences= new KeySequence[] { 
				KeySequence.getInstance(KeyStroke.getInstance(SpecialKey.F12))
			};
		}
		return fKeySequences;
	}
	
	private KeyAdapter getKeyAdapter() {
		if (fKeyAdapter == null) {
			fKeyAdapter= new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					int accelerator = KeySupport.convertEventToUnmodifiedAccelerator(e);
					KeySequence keySequence = KeySequence.getInstance(KeySupport.convertAcceleratorToKeyStroke(accelerator));
					KeySequence[] sequences= getKeySequences();
					
					for (int i= 0; i < sequences.length; i++) {
						if (sequences[i].equals(keySequence)) {
							toggleHierarchy();
							return;
						}
					}
				}
			};			
		}
		return fKeyAdapter;		
	}
	
	protected Text createFilterText(Composite parent) {
		fHeaderLabel= new Label(parent, SWT.NONE);
		// text set later
		fHeaderLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fHeaderLabel.setFont(JFaceResources.getBannerFont());
		Text text= super.createFilterText(parent);
		
		text.addKeyListener(getKeyAdapter());
		return text;
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
		
		treeViewer.getTree().addKeyListener(getKeyAdapter());	
		
		fInfoLabel= new Label(parent, SWT.NONE);
		fInfoLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fInfoLabel.setText(getInfoLabel());
		
		return treeViewer;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.AbstractInformationControl#setForegroundColor(org.eclipse.swt.graphics.Color)
	 */
	public void setForegroundColor(Color foreground) {
		super.setForegroundColor(foreground);
		fHeaderLabel.setForeground(foreground);
		fInfoLabel.setForeground(foreground);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.AbstractInformationControl#setBackgroundColor(org.eclipse.swt.graphics.Color)
	 */
	public void setBackgroundColor(Color background) {
		super.setBackgroundColor(background);
		fHeaderLabel.setBackground(background);
		fInfoLabel.setBackground(background);
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
					IMethod method= (IMethod) elem;
					if (!method.isConstructor()) {
						locked= method;				
					}
					input= method.getDeclaringType();
					break;
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
		
		fHeaderLabel.setText(getHeaderLabel(locked == null ? input : locked));
		try {
			fLifeCycle.ensureRefreshedTypeHierarchy(input, JavaPlugin.getActiveWorkbenchWindow());
		} catch (InvocationTargetException e1) {
			input= null;
		} catch (InterruptedException e1) {
			dispose();
			return;
		}
		IMember[] memberFilter= locked != null ? new IMember[] { locked } : null;
		
		TraditionalHierarchyContentProvider contentProvider= new TraditionalHierarchyContentProvider(fLifeCycle);
		contentProvider.setMemberFilter(memberFilter);
		getTreeViewer().setContentProvider(contentProvider);		
		
		fOtherContentProvider= new SuperTypeHierarchyContentProvider(fLifeCycle);
		fOtherContentProvider.setMemberFilter(memberFilter);
		
		fFocus= locked;
		
		Object[] topLevelObjects= contentProvider.getElements(fLifeCycle);
		if (topLevelObjects.length > 0 && contentProvider.getChildren(topLevelObjects[0]).length > 40) {
			fDoFilter= false;
		} else {
			getTreeViewer().addFilter(new NamePatternFilter());
		}

		Object selection= null;
		if (input instanceof IMember) {
			selection=  input;
		} else if (topLevelObjects.length > 0) {
			selection=  topLevelObjects[0];
		}
		inputChanged(fLifeCycle, selection);
	}
	
	protected void stringMatcherUpdated() {
		if (fDoFilter) {
			super.stringMatcherUpdated(); // refresh the view
		} else {
			selectFirstMatch();
		}
	}
	
	protected void toggleHierarchy() {
		TreeViewer treeViewer= getTreeViewer();
		
		Object[] expandedElements= treeViewer.getExpandedElements();
		TypeHierarchyContentProvider contentProvider= (TypeHierarchyContentProvider) treeViewer.getContentProvider();
		treeViewer.setContentProvider(fOtherContentProvider);

		treeViewer.refresh();
		if (fOtherExpandedElements != null) {
			treeViewer.setExpandedElements(fOtherExpandedElements);
		} else {
			treeViewer.expandAll();
		}
		
		fInfoLabel.setText(getInfoLabel());
		
		fOtherContentProvider= contentProvider;
		fOtherExpandedElements= expandedElements;
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
	
	private String getInfoLabel() {
		KeySequence[] sequences= getKeySequences();
		String keyName= sequences[0].format();
		
		if (fOtherContentProvider instanceof TraditionalHierarchyContentProvider) {
			return TypeHierarchyMessages.getFormattedString("HierarchyInformationControl.toggle.traditionalhierarchy.label", keyName); //$NON-NLS-1$
		} else {
			return TypeHierarchyMessages.getFormattedString("HierarchyInformationControl.toggle.superhierarchy.label", keyName); //$NON-NLS-1$
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.AbstractInformationControl#getSelectedElement()
	 */
	protected Object getSelectedElement() {
		Object selectedElement= super.getSelectedElement();
		if (selectedElement instanceof IType && fFocus != null) {
			IMethod[] methods= ((IType) selectedElement).findMethods(fFocus);
			if (methods != null && methods.length > 0) {
				return methods[0];
			}
		}
		return selectedElement;
	}

}
