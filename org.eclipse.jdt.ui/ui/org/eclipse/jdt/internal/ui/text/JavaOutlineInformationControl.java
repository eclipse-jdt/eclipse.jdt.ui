/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.keys.KeySequence;
import org.eclipse.ui.keys.SWTKeySupport;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jdt.ui.OverrideIndicatorLabelDecorator;
import org.eclipse.jdt.ui.ProblemsLabelDecorator;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.SuperTypeHierarchyCache;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
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

	private OutlineSorter fOutlineSorter;
	
	private OutlineLabelProvider fInnerLabelProvider;
	protected Color fForegroundColor;
	
	private boolean fShowOnlyMainType;
	private LexicalSortingAction fLexicalSortingAction;
	private SortByDefiningTypeAction fSortByDefiningTypeAction;
	private ShowOnlyMainTypeAction fShowOnlyMainTypeAction;
	private Map fTypeHierarchies= new HashMap();

	
	private class OutlineLabelProvider extends AppearanceAwareLabelProvider {

		private boolean fShowDefiningType;

		private OutlineLabelProvider() {
			super(AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS |  JavaElementLabels.F_APP_TYPE_SIGNATURE, AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS);
		}
		
		/*
		 * @see ILabelProvider#getText
		 */ 	
		public String getText(Object element) {
			String text= super.getText(element);
			if (fShowDefiningType) {
				try {
					IType type= getDefiningType(element);
					if (type != null) {
						StringBuffer buf= new StringBuffer(super.getText(type));
						buf.append(JavaElementLabels.CONCAT_STRING);
						buf.append(text);
						return buf.toString();			
					}
				} catch (JavaModelException e) {
				}
			}
			return text;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.viewsupport.JavaUILabelProvider#getForeground(java.lang.Object)
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
						return null;
					}
				}
				return fForegroundColor;
			}
			return null;
		}
		
		public void setShowDefiningType(boolean showDefiningType) {
			fShowDefiningType= showDefiningType;
		}
		
		public boolean isShowDefiningType() {
			return fShowDefiningType;
		}	
		
		private IType getDefiningType(Object element) throws JavaModelException {
			int kind= ((IJavaElement) element).getElementType();
		
			if (kind != IJavaElement.METHOD && kind != IJavaElement.FIELD && kind != IJavaElement.INITIALIZER) {
				return null;
			}
			IType declaringType= ((IMember) element).getDeclaringType();
			if (kind != IJavaElement.METHOD) {
				return declaringType;
			}
			ITypeHierarchy hierarchy= getSuperTypeHierarchy(declaringType);
			if (hierarchy == null) {
				return declaringType;
			}
			IMethod method= (IMethod) element;
			int flags= method.getFlags();
			if (Flags.isPrivate(flags) || Flags.isStatic(flags) || method.isConstructor()) {
				return declaringType;
			}
			IMethod res= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, declaringType, method.getElementName(), method.getParameterTypes(), false);
			if (res == null || method.equals(res)) {
				return declaringType;
			}
			return res.getDeclaringType();
		}
	}

	
	private class OutlineTreeViewer extends TreeViewer {
		
		private boolean fIsFiltering= false;

		private OutlineTreeViewer(Tree tree) {
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

		private boolean fShowInheritedMembers;

		/**
		 * Creates a new Outline content provider.
		 * 
		 * @param showInheritedMembers <code>true</code> iff inherited members are shown
		 */
		private OutlineContentProvider(boolean showInheritedMembers) {
			super(true);
			fShowInheritedMembers= showInheritedMembers;
		}
		
		public boolean isShowingInheritedMembers() {
			return fShowInheritedMembers;
		}
		
		public void toggleShowInheritedMembers() {
			Tree tree= getTreeViewer().getTree();
			
			tree.setRedraw(false);
			fShowInheritedMembers= !fShowInheritedMembers;
			getTreeViewer().refresh();
			getTreeViewer().expandToLevel(2);
			
			// reveal selection
			Object selectedElement= getSelectedElement();
			if (selectedElement != null)
				getTreeViewer().reveal(selectedElement);

			tree.setRedraw(true);
		}
		
		/**
		 * {@inheritDoc}
		 */
		public Object[] getChildren(Object element) {
			if (fShowOnlyMainType) {
				if (element instanceof ICompilationUnit) {
					element= getMainType((ICompilationUnit)element);
				} else if (element instanceof IClassFile) {
					element= getMainType((IClassFile)element);
				}

				if (element == null)
					return NO_CHILDREN;
			}
			
			if (fShowInheritedMembers && element instanceof IType) {
				IType type= (IType)element;
				if (type.getDeclaringType() == null) {
					ITypeHierarchy th= getSuperTypeHierarchy(type);
					if (th != null) {
						List children= new ArrayList();
						IType[] superClasses= th.getAllSupertypes(type);
						children.addAll(Arrays.asList(super.getChildren(type)));
						for (int i= 0, scLength= superClasses.length; i < scLength; i++)
							children.addAll(Arrays.asList(super.getChildren(superClasses[i])));
						return children.toArray();
					}
				}
			}
			return super.getChildren(element);
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
		
		/**
		 * Returns the primary type of a compilation unit (has the same
		 * name as the compilation unit).
		 * 
		 * @param compilationUnit the compilation unit
		 * @return returns the primary type of the compilation unit, or
		 * <code>null</code> if is does not have one
		 */
		private IType getMainType(ICompilationUnit compilationUnit) {
			
			if (compilationUnit == null)
				return null;
			
			String name= compilationUnit.getElementName();
			int index= name.indexOf('.');
			if (index != -1)
				name= name.substring(0, index);
			IType type= compilationUnit.getType(name);
			return type.exists() ? type : null;
		}

		/**
		 * Returns the primary type of a class file.
		 * 
		 * @param classFile the class file
		 * @return returns the primary type of the class file, or <code>null</code>
		 * if is does not have one
		 */
		private IType getMainType(IClassFile classFile) {
			try {
				IType type= classFile.getType();
				return type != null && type.exists() ? type : null;
			} catch (JavaModelException e) {
				return null;	
			}
		}
	}
	
	
	private class ShowOnlyMainTypeAction extends Action {
		
		private static final String STORE_GO_INTO_TOP_LEVEL_TYPE_CHECKED= "GoIntoTopLevelTypeAction.isChecked"; //$NON-NLS-1$
		
		private TreeViewer fOutlineViewer;

		private ShowOnlyMainTypeAction(TreeViewer outlineViewer) {
			super(TextMessages.getString("JavaOutlineInformationControl.GoIntoTopLevelType.label"), IAction.AS_CHECK_BOX); //$NON-NLS-1$
			setToolTipText(TextMessages.getString("JavaOutlineInformationControl.GoIntoTopLevelType.tooltip")); //$NON-NLS-1$
			setDescription(TextMessages.getString("JavaOutlineInformationControl.GoIntoTopLevelType.description")); //$NON-NLS-1$
			
			JavaPluginImages.setLocalImageDescriptors(this, "gointo_toplevel_type.gif"); //$NON-NLS-1$
			
			WorkbenchHelp.setHelp(this, IJavaHelpContextIds.GO_INTO_TOP_LEVEL_TYPE_ACTION);
			
			fOutlineViewer= outlineViewer;

			boolean showclass= getDialogSettings().getBoolean(STORE_GO_INTO_TOP_LEVEL_TYPE_CHECKED);
			setTopLevelTypeOnly(showclass);
		}

		/*
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			setTopLevelTypeOnly(!fShowOnlyMainType);
		}

		private void setTopLevelTypeOnly(boolean show) {
			fShowOnlyMainType= show;
			setChecked(show);
			
			Tree tree= fOutlineViewer.getTree();
			tree.setRedraw(false);
			
			fOutlineViewer.refresh(false);
			if (!fShowOnlyMainType)
				fOutlineViewer.expandToLevel(2);
			
			
			// reveal selection
			Object selectedElement= getSelectedElement();
			if (selectedElement != null)
				fOutlineViewer.reveal(selectedElement);
			
			tree.setRedraw(true);
					
			getDialogSettings().put(STORE_GO_INTO_TOP_LEVEL_TYPE_CHECKED, show);
		}
	}
	
	private class OutlineSorter extends ViewerSorter {

		private static final int OTHER= 1;
		private static final int TYPE= 2;
		private static final int ANONYM= 3;

		private JavaElementSorter fJavaElementSorter= new JavaElementSorter();
		
		/*
		 * @see org.eclipse.jface.viewers.ViewerSorter#sort(org.eclipse.jface.viewers.Viewer, java.lang.Object[])
		 */
		public void sort(Viewer viewer, Object[] elements) {
			if (!fLexicalSortingAction.isChecked() && !fSortByDefiningTypeAction.isChecked())
				return;
			
			super.sort(viewer, elements);
		}
		
		/*
		 * @see org.eclipse.jface.viewers.ViewerSorter#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public int compare(Viewer viewer, Object e1, Object e2) {
			int cat1= category(e1);
			int cat2= category(e2);

			if (cat1 != cat2)
				return cat1 - cat2;
			
			if (cat1 == OTHER) { // method or field
				if (fSortByDefiningTypeAction.isChecked()) {
					try {
						IType def1= (e1 instanceof IMethod) ? getDefiningType((IMethod) e1) : null;
						IType def2= (e2 instanceof IMethod) ? getDefiningType((IMethod) e2) : null;
						if (def1 != null) {
							if (def2 != null) {
								if (!def2.equals(def1)) {
									return compareInHierarchy(getSuperTypeHierarchy(def1), def1, def2);
								}
							} else {
								return -1;						
							}					
						} else {
							if (def2 != null) {
								return 1;
							}	
						}
					} catch (JavaModelException e) {
						// ignore, default to normal comparison
					}
				}
			} else if (cat1 == ANONYM) {
				return 0;
			}
			if (fLexicalSortingAction.isChecked())
				return fJavaElementSorter.compare(viewer, e1, e2); // use appearance preference page settings
			else
				return 0;
		}
		
		private IType getDefiningType(IMethod method) throws JavaModelException {
			IType declaringType= method.getDeclaringType();
			int flags= method.getFlags();
			if (Flags.isPrivate(flags) || Flags.isStatic(flags) || method.isConstructor()) {
				return null;
			}
		
			IMethod res= JavaModelUtil.findMethodDeclarationInHierarchy(getSuperTypeHierarchy(declaringType), declaringType, method.getElementName(), method.getParameterTypes(), false);
			if (res == null || method.equals(res)) {
				return null;
			}
			return res.getDeclaringType();
		}

		/*
		 * @see org.eclipse.jface.viewers.ViewerSorter#category(java.lang.Object)
		 */
		public int category(Object element) {
			if (element instanceof IType) {
				IType type= (IType) element;
				if (type.getElementName().length() == 0) {
					return ANONYM;
				}
				return TYPE;
			}
			return OTHER;
		}

		private int compareInHierarchy(ITypeHierarchy hierarchy, IType def1, IType def2) {
			if (isSuperType(hierarchy, def1, def2)) {
				return 1;
			} else if (isSuperType(hierarchy, def2, def1)) {
				return -1;
			}
			// interfaces after classes
			int flags1= hierarchy.getCachedFlags(def1);
			int flags2= hierarchy.getCachedFlags(def2);
			if (Flags.isInterface(flags1)) {
				if (!Flags.isInterface(flags2)) {
					return 1;
				}
			} else if (Flags.isInterface(flags2)) {
				return -1;
			}
			String name1= def1.getElementName();
			String name2= def2.getElementName();
			
			return getCollator().compare(name1, name2);
		}

		private boolean isSuperType(ITypeHierarchy hierarchy, IType def1, IType def2) {
			IType superType= hierarchy.getSuperclass(def1);
			if (superType != null) {
				if (superType.equals(def2) || isSuperType(hierarchy, superType, def2)) {
					return true;
				}
			}
			IType[] superInterfaces= hierarchy.getAllSuperInterfaces(def1);
			for (int i= 0; i < superInterfaces.length; i++) {
				IType curr= superInterfaces[i];
				if (curr.equals(def2) || isSuperType(hierarchy, curr, def2)) {
					return true;
				}		
			}
			return false;
		}
	}
	
	
	private class LexicalSortingAction extends Action {
		
		private static final String STORE_LEXICAL_SORTING_CHECKED= "LexicalSortingAction.isChecked"; //$NON-NLS-1$
		
		private TreeViewer fOutlineViewer;

		private LexicalSortingAction(TreeViewer outlineViewer) {
			super(TextMessages.getString("JavaOutlineInformationControl.LexicalSortingAction.label"), IAction.AS_CHECK_BOX); //$NON-NLS-1$
			setToolTipText(TextMessages.getString("JavaOutlineInformationControl.LexicalSortingAction.tooltip")); //$NON-NLS-1$
			setDescription(TextMessages.getString("JavaOutlineInformationControl.LexicalSortingAction.description")); //$NON-NLS-1$
			
			JavaPluginImages.setLocalImageDescriptors(this, "alphab_sort_co.gif"); //$NON-NLS-1$
			
			fOutlineViewer= outlineViewer;
			
			boolean checked=getDialogSettings().getBoolean(STORE_LEXICAL_SORTING_CHECKED);
			setChecked(checked);
			WorkbenchHelp.setHelp(this, IJavaHelpContextIds.LEXICAL_SORTING_BROWSING_ACTION);
		}

		public void run() {
			valueChanged(isChecked(), true);
		}

		private void valueChanged(final boolean on, boolean store) {
			setChecked(on);
			BusyIndicator.showWhile(fOutlineViewer.getControl().getDisplay(), new Runnable() {
				public void run() {
					fOutlineViewer.refresh(false);
				}
			});
			
			if (store)
				getDialogSettings().put(STORE_LEXICAL_SORTING_CHECKED, on);
		}
	}


	private class SortByDefiningTypeAction extends Action {
		
		private static final String STORE_SORT_BY_DEFINING_TYPE_CHECKED= "SortByDefiningType.isChecked"; //$NON-NLS-1$
		
		private TreeViewer fOutlineViewer;
		
		/** 
		 * Creates the action.
		 * 
		 * @param outlineViewer the outline viewer
		 */
		private SortByDefiningTypeAction(TreeViewer outlineViewer) {
			super(TextMessages.getString("JavaOutlineInformationControl.SortByDefiningTypeAction.label")); //$NON-NLS-1$
			setDescription(TextMessages.getString("JavaOutlineInformationControl.SortByDefiningTypeAction.description")); //$NON-NLS-1$
			setToolTipText(TextMessages.getString("JavaOutlineInformationControl.SortByDefiningTypeAction.tooltip")); //$NON-NLS-1$
			
			JavaPluginImages.setLocalImageDescriptors(this, "definingtype_sort_co.gif"); //$NON-NLS-1$
			
			fOutlineViewer= outlineViewer;
			
			WorkbenchHelp.setHelp(this, IJavaHelpContextIds.SORT_BY_DEFINING_TYPE_ACTION);
	 
			boolean state= getDialogSettings().getBoolean(STORE_SORT_BY_DEFINING_TYPE_CHECKED);
			setChecked(state);
			fInnerLabelProvider.setShowDefiningType(state);
		}
		
		/*
		 * @see Action#actionPerformed
		 */	
		public void run() {
			BusyIndicator.showWhile(fOutlineViewer.getControl().getDisplay(), new Runnable() {
				public void run() {
					fInnerLabelProvider.setShowDefiningType(isChecked());
					getDialogSettings().put(STORE_SORT_BY_DEFINING_TYPE_CHECKED, isChecked());
					
					fOutlineViewer.refresh(true);
					
					// reveal selection
					Object selectedElement= getSelectedElement();
					if (selectedElement != null)
						fOutlineViewer.reveal(selectedElement);
				}
			});		
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
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= tree.getItemHeight() * 12;
		tree.setLayoutData(gd);
	
		final TreeViewer treeViewer= new OutlineTreeViewer(tree);
	
		// Hard-coded filters
		treeViewer.addFilter(new NamePatternFilter());
		treeViewer.addFilter(new MemberFilter());

		fForegroundColor= parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);

		fInnerLabelProvider= new OutlineLabelProvider();
		fInnerLabelProvider.addLabelDecorator(new ProblemsLabelDecorator(null));
		IDecoratorManager decoratorMgr= PlatformUI.getWorkbench().getDecoratorManager();
		if (decoratorMgr.getEnabled("org.eclipse.jdt.ui.override.decorator")) //$NON-NLS-1$
			fInnerLabelProvider.addLabelDecorator(new OverrideIndicatorLabelDecorator(null));
		
		treeViewer.setLabelProvider(fInnerLabelProvider);
		
		fLexicalSortingAction= new LexicalSortingAction(treeViewer);
		fSortByDefiningTypeAction= new SortByDefiningTypeAction(treeViewer);
		fShowOnlyMainTypeAction= new ShowOnlyMainTypeAction(treeViewer);
		
		fOutlineContentProvider= new OutlineContentProvider(false);
		treeViewer.setContentProvider(fOutlineContentProvider);
		fOutlineSorter= new OutlineSorter();
		treeViewer.setSorter(fOutlineSorter);
		treeViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
		
		
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
	
	/*
	 * @see org.eclipse.jdt.internal.ui.text.AbstractInformationControl#getId()
	 * @since 3.0
	 */
	protected String getId() {
		return "org.eclipse.jdt.internal.ui.text.QuickOutline"; //$NON-NLS-1$
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
		long flags= AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS |  JavaElementLabels.F_APP_TYPE_SIGNATURE;
		if (!fOutlineContentProvider.isShowingInheritedMembers())
			flags |= JavaElementLabels.ALL_POST_QUALIFIED;
		fInnerLabelProvider.setTextFlags(flags);
		fOutlineContentProvider.toggleShowInheritedMembers();
		updateStatusFieldText();
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.text.AbstractInformationControl#fillViewMenu(org.eclipse.jface.action.IMenuManager)
	 */
	protected void fillViewMenu(IMenuManager viewMenu) {
		super.fillViewMenu(viewMenu);
		viewMenu.add(fShowOnlyMainTypeAction); //$NON-NLS-1$

		viewMenu.add(new Separator("Sorters")); //$NON-NLS-1$
		viewMenu.add(fLexicalSortingAction);
		
		viewMenu.add(fSortByDefiningTypeAction);
	}
	
	private ITypeHierarchy getSuperTypeHierarchy(IType type) {
		ITypeHierarchy th= (ITypeHierarchy)fTypeHierarchies.get(type);
		if (th == null) {
			try {
				th= SuperTypeHierarchyCache.getTypeHierarchy(type, getProgressMonitor());
			} catch (JavaModelException e) {
				return null;
			}
			fTypeHierarchies.put(type, th);
		}
		return th;
	}
	
	private IProgressMonitor getProgressMonitor() {
		IWorkbenchPage wbPage= JavaPlugin.getActivePage();
		if (wbPage == null)
			return null;
		
		IEditorPart editor= wbPage.getActiveEditor();
		if (editor == null)
			return null;
		
		return editor.getEditorSite().getActionBars().getStatusLineManager().getProgressMonitor();			
	}
}
