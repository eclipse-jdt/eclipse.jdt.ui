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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.keys.KeySequence;
import org.eclipse.ui.keys.SWTKeySupport;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.DecoratingJavaLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;
import org.eclipse.jdt.internal.ui.viewsupport.MemberFilter;

/**
 * Show outline in light-weight control.
 * 
 * @since 2.1
 */
public class JavaOutlineInformationControl extends AbstractInformationControl {
	
	private KeyAdapter fKeyAdapter;
	private OutlineContentProvider fOutlineContentProvider;
	private IJavaElement fInput= null;

	private AppearanceAwareLabelProvider fInnerLabelProvider;
	protected Color fForegroundColor;
	

	private class OutlineLabelProvider extends AppearanceAwareLabelProvider {

		private OutlineLabelProvider() {
			super(AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS |  JavaElementLabels.F_APP_TYPE_SIGNATURE, AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS);
		}
		
		/**
		 * {@inheritDoc}
		 */
		public Color getForeground(Object element) {
			if (fOutlineContentProvider.isShowingInheritedMembers()) {
				if (element instanceof IJavaElement) {
					IJavaElement je= (IJavaElement)element;
					if (fInput.getElementType() == IJavaElement.CLASS_FILE)
						je= je.getAncestor(IJavaElement.CLASS_FILE);
					else
						je= je.getAncestor(IJavaElement.COMPILATION_UNIT);
					if (fInput.equals(je)) {
						if (fForegroundColor == null)
							fForegroundColor= getTreeViewer().getControl().getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE);
						return fForegroundColor;
					}
				}
			}
			return null;
		}
	}

	private class OutlineTreeViewer extends TreeViewer {
		
		private boolean fIsFiltering= false;

		public OutlineTreeViewer(Tree tree) {
			super(tree);
		}
		
		/**
		 * {@inheritDoc}
		 */
		protected Object[] getFilteredChildren(Object parent) {
			Object[] result = getRawChildren(parent);
			int unfilteredChildren= result.length;
			ViewerFilter[] filters = getFilters();
			if (filters != null) {
				for (int i= 0; i < filters.length; i++)
					result = filters[i].filter(this, parent, result);
			}
			fIsFiltering= unfilteredChildren != result.length;
			return result;
		}
		
		/**
		 * {@inheritDoc}
		 */
		protected void internalExpandToLevel(Widget node, int level) {
			if (!fIsFiltering && node instanceof Item) {
				Item i= (Item) node;
				if (i.getData() instanceof IJavaElement) {
					IJavaElement je= (IJavaElement) i.getData();
					if (je.getElementType() == IJavaElement.IMPORT_CONTAINER || isInnerType(je)) {
						setExpanded(i, false);
						return;
					}
				}
			}
			super.internalExpandToLevel(node, level);
		}
		
		private boolean isInnerType(IJavaElement element) {
			if (element != null && element.getElementType() == IJavaElement.TYPE) {
				IType type= (IType)element;
				try {
					return type.isMember();
				} catch (JavaModelException e) {
					IJavaElement parent= type.getParent();
					if (parent != null) {
						int parentElementType= parent.getElementType();
						return (parentElementType != IJavaElement.COMPILATION_UNIT && parentElementType != IJavaElement.CLASS_FILE);
					}
				}
			}
			return false;		
		}
	}
	
	private class OutlineContentProvider extends StandardJavaElementContentProvider {

		private Map fTypeHierarchies= new HashMap();
		private boolean fShowInheritedMembers;

		/**
		 * Creates a new Outline content provider.
		 * 
		 * @param showInheritedMembers <code>true</code> iff inherited members are shown
		 */
		public OutlineContentProvider(boolean showInheritedMembers) {
			super(true, true);
			fShowInheritedMembers= showInheritedMembers;
		}
		
		public boolean isShowingInheritedMembers() {
			return fShowInheritedMembers;
		}
		
		public void toggleShowInheritedMembers() {
			fShowInheritedMembers= !fShowInheritedMembers;
			getTreeViewer().refresh();
		}
		
		/**
		 * {@inheritDoc}
		 */
		public Object[] getChildren(Object element) {
			if (fShowInheritedMembers && element instanceof IType) {
				IType type= (IType)element;
				if (type.getDeclaringType() == null) {
					ITypeHierarchy th= getSuperTypeHierarchy(type);
					if (th != null) {
						List children= new ArrayList();
						IType[] superClasses= th.getAllSuperclasses(type);
						children.addAll(Arrays.asList(super.getChildren(type)));
						for (int i= 0, scLength= superClasses.length; i < scLength; i++)
							children.addAll(Arrays.asList(super.getChildren(superClasses[i])));
						return children.toArray();
					}
				}
			}
			return super.getChildren(element);
		}
		
		private ITypeHierarchy getSuperTypeHierarchy(IType type) {
			ITypeHierarchy th= (ITypeHierarchy)fTypeHierarchies.get(type);
			if (th == null) {
				try {
					th= type.newSupertypeHierarchy(JavaPlugin.getActiveWorkbenchWindow().getActivePage().getActiveEditor().getEditorSite().getActionBars().getStatusLineManager().getProgressMonitor());
				} catch (JavaModelException e) {
					return null;
				}
				fTypeHierarchies.put(type, th);
			}
			return th;
		}
		
		/**
		 * {@inheritDoc}
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			super.inputChanged(viewer, oldInput, newInput);
			fTypeHierarchies.clear();
		}
		
		/**
		 * {@inheritDoc}
		 */
		public void dispose() {
			super.dispose();
			fTypeHierarchies.clear();
		}
	}

	/**
	 * Creates a new Java outline information control.
	 * 
	 * @param parent
	 * @param shellStyle
	 * @param treeStyle
	 * @param commandId
	 */
	public JavaOutlineInformationControl(Shell parent, int shellStyle, int treeStyle, String commandId) {
		super(parent, shellStyle, treeStyle, commandId, true);
	}

	/**
	 * {@inheritDoc}
	 */
	protected Text createFilterText(Composite parent) {
		Text text= super.createFilterText(parent);
		text.addKeyListener(getKeyAdapter());
		return text;
	}

	/**
	 * {@inheritDoc}
	 */
	protected TreeViewer createTreeViewer(Composite parent, int style) {
		Tree tree= new Tree(parent, SWT.SINGLE | (style & ~SWT.MULTI));
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
	
		final TreeViewer treeViewer= new OutlineTreeViewer(tree);
	
		// Hide import declartions but show the container
		treeViewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return !(element instanceof IImportDeclaration);
			}
		});
		treeViewer.addFilter(new NamePatternFilter());
		
		treeViewer.addFilter(new MemberFilter());
		
		fOutlineContentProvider= new OutlineContentProvider(false);
		treeViewer.setContentProvider(fOutlineContentProvider);
		treeViewer.setSorter(new JavaElementSorter());
		treeViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
		
		fInnerLabelProvider= new OutlineLabelProvider();
		
		treeViewer.setLabelProvider(new DecoratingJavaLabelProvider(fInnerLabelProvider));
		treeViewer.getTree().addKeyListener(getKeyAdapter());
		
		return treeViewer;
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getStatusFieldText() {
		KeySequence[] sequences= getInvokingCommandKeySequences();
		if (sequences == null || sequences.length == 0)
			return ""; //$NON-NLS-1$
		
		String keySequence= sequences[0].format();
		
		if (fOutlineContentProvider.isShowingInheritedMembers())
			return JavaUIMessages.getFormattedString("JavaOutlineControl.statusFieldText.hideInheritedMembers", keySequence); //$NON-NLS-1$
		else
			return JavaUIMessages.getFormattedString("JavaOutlineControl.statusFieldText.showInheritedMembers", keySequence); //$NON-NLS-1$
	}

	/**
	 * {@inheritDoc}
	 */
	public void setInput(Object information) {
		if (information == null || information instanceof String) {
			inputChanged(null, null);
			return;
		}
		IJavaElement je= (IJavaElement)information;
		ICompilationUnit cu= (ICompilationUnit)je.getAncestor(IJavaElement.COMPILATION_UNIT);
		if (cu != null)
			fInput= cu;
		else
			fInput= je.getAncestor(IJavaElement.CLASS_FILE);
			
		inputChanged(fInput, information);
	}
	
	private KeyAdapter getKeyAdapter() {
		if (fKeyAdapter == null) {
			fKeyAdapter= new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					int accelerator = SWTKeySupport.convertEventToUnmodifiedAccelerator(e);
					KeySequence keySequence = KeySequence.getInstance(SWTKeySupport.convertAcceleratorToKeyStroke(accelerator));
					KeySequence[] sequences= getInvokingCommandKeySequences();
					if (sequences == null)
						return;
					for (int i= 0; i < sequences.length; i++) {
						if (sequences[i].equals(keySequence)) {
							e.doit= false;
							toggleShowInheritedMembers();
							return;
						}
					}
				}
			};			
		}
		return fKeyAdapter;		
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected void handleStatusFieldClicked() {
		toggleShowInheritedMembers();
	}

	protected void toggleShowInheritedMembers() {
		int flags= AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS |  JavaElementLabels.F_APP_TYPE_SIGNATURE;
		if (!fOutlineContentProvider.isShowingInheritedMembers())
			flags |= JavaElementLabels.ALL_POST_QUALIFIED;
		fInnerLabelProvider.setTextFlags(flags);	
		fOutlineContentProvider.toggleShowInheritedMembers();
		updateStatusFieldText();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		super.dispose();
		fOutlineContentProvider.dispose();
		fInnerLabelProvider.dispose();
	}
}
