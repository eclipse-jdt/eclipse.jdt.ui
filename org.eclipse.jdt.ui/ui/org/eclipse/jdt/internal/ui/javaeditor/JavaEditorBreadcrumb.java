/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IWorkingCopyProvider;
import org.eclipse.jdt.ui.JavaElementComparator;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
import org.eclipse.jdt.ui.ProblemsLabelDecorator.ProblemsLabelChangedEvent;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.BreadcrumbViewer;
import org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.EditorBreadcrumb;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.DecoratingJavaLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.ResourceToItemsMapper;


/**
 * The breadcrumb for the Java editor. Shows Java elements. Requires
 * a Java editor.
 * 
 * @since 3.4
 */
public class JavaEditorBreadcrumb extends EditorBreadcrumb {

	private static class ProblemBreadcrumbViewer extends BreadcrumbViewer implements ResourceToItemsMapper.IContentViewerAccessor {

		private ResourceToItemsMapper fResourceToItemsMapper;

		public ProblemBreadcrumbViewer(Composite parent, int style) {
			super(parent, style);
			fResourceToItemsMapper= new ResourceToItemsMapper(this);
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.viewsupport.ResourceToItemsMapper.IContentViewerAccessor#doUpdateItem(org.eclipse.swt.widgets.Widget)
		 */
		public void doUpdateItem(Widget item) {
			doUpdateItem(item, item.getData(), true);
		}

		/*
		 * @see StructuredViewer#mapElement(Object, Widget)
		 */
		protected void mapElement(Object element, Widget item) {
			super.mapElement(element, item);
			if (item instanceof Item) {
				fResourceToItemsMapper.addToMap(element, (Item) item);
			}
		}

		/*
		 * @see StructuredViewer#unmapElement(Object, Widget)
		 */
		protected void unmapElement(Object element, Widget item) {
			if (item instanceof Item) {
				fResourceToItemsMapper.removeFromMap(element, (Item) item);
			}
			super.unmapElement(element, item);
		}

		/*
		 * @see StructuredViewer#unmapAllElements()
		 */
		protected void unmapAllElements() {
			fResourceToItemsMapper.clearMap();
			super.unmapAllElements();
		}

		/*
		 * @see org.eclipse.jface.viewers.StructuredViewer#handleLabelProviderChanged(org.eclipse.jface.viewers.LabelProviderChangedEvent)
		 */
		protected void handleLabelProviderChanged(LabelProviderChangedEvent event) {
			if (event instanceof ProblemsLabelChangedEvent) {
				ProblemsLabelChangedEvent e= (ProblemsLabelChangedEvent) event;
				if (!e.isMarkerChange() && canIgnoreChangesFromAnnotionModel()) {
					return;
				}
			}

			Object[] changed= event.getElements();
			if (changed != null && !fResourceToItemsMapper.isEmpty()) {
				ArrayList others= new ArrayList(changed.length);
				for (int i= 0; i < changed.length; i++) {
					Object curr= changed[i];
					if (curr instanceof IResource) {
						fResourceToItemsMapper.resourceChanged((IResource) curr);
					} else {
						others.add(curr);
					}
				}
				if (others.isEmpty()) {
					return;
				}
				event= new LabelProviderChangedEvent((IBaseLabelProvider) event.getSource(), others.toArray());
			}
			super.handleLabelProviderChanged(event);
		}

		/**
		 * Answers whether this viewer can ignore label provider changes resulting from
		 * marker changes in annotation models
		 * @return returns <code>true</code> if annotation model changes can be ignored
		 */
		private boolean canIgnoreChangesFromAnnotionModel() {
			Object contentProvider= getContentProvider();
			return contentProvider instanceof IWorkingCopyProvider && !((IWorkingCopyProvider) contentProvider).providesWorkingCopies();
		}

		/* 
		 * @see org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.BreadcrumbViewer#configureDropDownViewer(org.eclipse.jface.viewers.TreeViewer, java.lang.Object)
		 */
		public void configureDropDownViewer(TreeViewer viewer, Object input) {
			viewer.setContentProvider(createContentProvider());
			viewer.setLabelProvider(createDropDownLabelProvider());
			viewer.setComparator(new JavaElementComparator());
			viewer.addFilter(new ViewerFilter() {
				public boolean select(Viewer viewer1, Object parentElement, Object element) {
					if (element instanceof IMember) {
						if (((IMember) element).getElementName().startsWith("<")) { //$NON-NLS-1$
							// filter out <clinit>
							return false;
						}
					}

					return true;
				}
			});
		}

		private ILabelProvider createDropDownLabelProvider() {
			final AppearanceAwareLabelProvider result= new AppearanceAwareLabelProvider(AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS | JavaElementLabels.F_APP_TYPE_SIGNATURE
					| JavaElementLabels.ALL_CATEGORY, JavaElementImageProvider.SMALL_ICONS | AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS);

			return new DecoratingJavaLabelProvider(result);
		}
	}

	private static final class JavaEditorBreadcrumbContentProvider implements ITreeContentProvider {

		private final StandardJavaElementContentProvider fParent;
		private Object[] fElements;
		private Object fLastInputElement;

		public JavaEditorBreadcrumbContentProvider(StandardJavaElementContentProvider parent) {
			fParent= parent;
		}

		/*
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}

		/*
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
		 */
		public Object[] getChildren(Object inputElement) {
			if (inputElement == fLastInputElement)
				return fElements;

			fLastInputElement= inputElement;
			if (inputElement instanceof IPackageFragment) {
				fElements= getPackageContent((IPackageFragment) inputElement);
			} else if (inputElement instanceof IProject) {
				IProject project= (IProject) inputElement;
				if (project.isAccessible()) {
					try {
						fElements= ((IProject) inputElement).members();
					} catch (CoreException e) {
						JavaPlugin.log(e);
					}
				} else {
					fElements= new Object[0];
				}
			} else if (inputElement instanceof IPackageFragmentRoot) {
				Object[] fragments= fParent.getChildren(inputElement);

				ArrayList packages= new ArrayList();
				for (int i= 0; i < fragments.length; i++) {
					Object object= fragments[i];
					if (object instanceof IPackageFragment) {
						try {
							if (((IPackageFragment) object).hasChildren())
								packages.add(object);
						} catch (JavaModelException e) {
							JavaPlugin.log(e);
							packages.add(object);
						}
					} else {
						packages.add(object);
					}
				}
				fElements= packages.toArray();
			} else {
				fElements= fParent.getChildren(inputElement);
			}

			return fElements;
		}

		/*
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
		 */
		public Object getParent(Object element) {
			if (element instanceof IType && ((IType) element).isBinary()) {
				IType declaringType= ((IType) element).getDeclaringType();
				if (declaringType != null)
					return declaringType;
			}
			
			Object result= fParent.getParent(element);

			if (result instanceof ITypeRoot) {
				if (ActionUtil.isOnBuildPath((IJavaElement) result)) {
					result= fParent.getParent(result);
				} else {
					result= ((ITypeRoot) result).getResource();
					if (result instanceof IFile)
						result= fParent.getParent(result);
				}
			}

			return result;
		}

		/*
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
		 */
		public boolean hasChildren(Object element) {
			if (element instanceof IProject) {
				IProject project= (IProject) element;
				if (!project.isAccessible()) {
					return false;
				}
				try {
					return project.members().length > 0;
				} catch (CoreException e) {
					JavaPlugin.log(e);
				}
				return false;
			} else {
				return fParent.hasChildren(element);
			}
		}

		private Object[] getPackageContent(IPackageFragment pack) {
			ArrayList result= new ArrayList();
			try {
				ICompilationUnit[] units= pack.getCompilationUnits();
				for (int i= 0; i < units.length; i++) {
					IType[] types= units[i].getTypes();
					for (int j= 0; j < types.length; j++) {
						if (isValidType(types[j]))
							result.add(types[j]);
					}
				}
				
				IClassFile[] classFiles= pack.getClassFiles();
				for (int i= 0; i < classFiles.length; i++) {
					if (isValidType(classFiles[i].getType()))
						result.add(classFiles[i].getType());
				}
				
				Object[] nonJavaResources= pack.getNonJavaResources();
				for (int i= 0; i < nonJavaResources.length; i++) {
					result.add(nonJavaResources[i]);
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}

			return result.toArray();
		}

		private boolean isValidType(IType type) {
			if (type.getDeclaringType() != null)
				return false;

			try {
				return !type.isAnonymous();
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
				return false;
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		public void dispose() {
			fParent.dispose();
		}

		/*
		 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			fElements= null;
			fLastInputElement= null;
			fParent.inputChanged(viewer, oldInput, newInput);
		}
	}

	private class ElementChangeListener implements IElementChangedListener {
		
		private Runnable fRunnable;

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.IElementChangedListener#elementChanged(org.eclipse.jdt.core.ElementChangedEvent)
		 */
		public void elementChanged(ElementChangedEvent event) {
			if (fViewer == null)
				return;
			
			Object input= fViewer.getInput();
			if (!(input instanceof IJavaElement))
				return;
			
			if (fRunnable != null)
				return;

			ITypeRoot root= (ITypeRoot) ((IJavaElement) input).getAncestor(IJavaElement.COMPILATION_UNIT);
			if (root == null)
				root= (ITypeRoot) ((IJavaElement) input).getAncestor(IJavaElement.CLASS_FILE);

			if (!isParentElementChanged(root, event.getDelta()))
				return;

			fRunnable= new Runnable() {
				public void run() {
					if (fViewer == null)
						return;
					
					Object newInput= getCurrentInput();
					if (newInput instanceof IJavaElement)
						newInput= getInput((IJavaElement) newInput);
					
					fViewer.setInput(newInput);
					fRunnable= null;
				}
			};
			fViewer.getControl().getDisplay().asyncExec(fRunnable);
		}
		
		/**
		 * Does the given delta describe a change to the given root element or any
		 * of its parents?
		 * 
		 * @param root the root to check for changes
		 * @param delta the Java element delta to inspect
		 * @return true if delta describes a change to parent of root
		 */
		private boolean isParentElementChanged(ITypeRoot root, IJavaElementDelta delta) {
			if ((delta.getFlags() & IJavaElementDelta.F_CHILDREN) != 0) {
				IJavaElementDelta[] children= delta.getAffectedChildren();
				for (int i= 0; i < children.length; i++) {
					if (isParentElementChanged(root, children[i]))
						return true;
				}
				
				return false;
			} else {
				return hasParent(root, delta.getElement());
			}
		}

		/**
		 * Does <code>element</code> has an element equals to <code>parent</code>
		 * in its parent chain?
		 * 
		 * @param element the element to resolve the parent chain for
		 * @param parent the potential parent
		 * @return <code>true</code> if <code>parent</code> is a parent of <code>element</code>
		 */
		private boolean hasParent(IJavaElement element, IJavaElement parent) {
			if (element == null)
				return false;

			if (element == parent || element.equals(parent))
				return true;

			return hasParent(element.getParent(), parent);
		}
	}

	private ActionGroup fBreadcrumbActionGroup;
	private BreadcrumbViewer fViewer;
	private ISelection fEditorSelection;
	private ElementChangeListener fElementChangeListener;

	public JavaEditorBreadcrumb(JavaEditor javaEditor) {
		super(javaEditor);
		setTextViewer(javaEditor.getViewer());
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.EditorBreadcrumb#activateBreadcrumb()
	 */
	protected void activateBreadcrumb() {
		fEditorSelection= getJavaEditor().getSelectionProvider().getSelection();
		IEditorSite editorSite= getJavaEditor().getEditorSite();
		editorSite.getKeyBindingService().setScopes(new String[] { "org.eclipse.jdt.ui.breadcrumbEditorScope" }); //$NON-NLS-1$
		getJavaEditor().setActionsActivated(false);
		fBreadcrumbActionGroup.fillActionBars(editorSite.getActionBars());
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.EditorBreadcrumb#deactivateBreadcrumb()
	 */
	protected void deactivateBreadcrumb() {
		IEditorSite editorSite= getJavaEditor().getEditorSite();
		editorSite.getKeyBindingService().setScopes(new String[] { "org.eclipse.jdt.ui.javaEditorScope" }); //$NON-NLS-1$
		getJavaEditor().getActionGroup().fillActionBars(editorSite.getActionBars());
		getJavaEditor().setActionsActivated(true);
		fEditorSelection= null;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.EditorBreadcrumb#createViewer(org.eclipse.swt.widgets.Composite)
	 */
	protected BreadcrumbViewer createViewer(Composite composite) {
		fViewer= new ProblemBreadcrumbViewer(composite, SWT.HORIZONTAL);

		fViewer.setLabelProvider(createLabelProvider());
		fViewer.setToolTipLabelProvider(createToolTipLabelProvider());

		fViewer.setContentProvider(createContentProvider());
		fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				fBreadcrumbActionGroup.setContext(new ActionContext(fViewer.getSelection()));
			}
		});

		fBreadcrumbActionGroup= new JavaEditorBreadcrumbActionGroup(getJavaEditor(), fViewer);

		fElementChangeListener= new ElementChangeListener();
		JavaCore.addElementChangedListener(fElementChangeListener, ElementChangedEvent.POST_CHANGE);

		return fViewer;
	}

	/**
	 * Create a new instance of the content provider to use for the Java editor breadcrumb.
	 * 
	 * @return a new content provider
	 */
	private static JavaEditorBreadcrumbContentProvider createContentProvider() {
		StandardJavaElementContentProvider parentContentProvider= new StandardJavaElementContentProvider(true);
		return new JavaEditorBreadcrumbContentProvider(parentContentProvider);
	}

	/**
	 * Create a new instance of the label provider to use for the Java editor breadcrumb
	 * 
	 * @return a new label provider
	 */
	private static ILabelProvider createLabelProvider() {
		final AppearanceAwareLabelProvider result= new AppearanceAwareLabelProvider(JavaElementLabels.ROOT_VARIABLE | JavaElementLabels.T_TYPE_PARAMETERS | JavaElementLabels.M_PARAMETER_TYPES
				| JavaElementLabels.M_APP_TYPE_PARAMETERS | JavaElementLabels.M_APP_RETURNTYPE | JavaElementLabels.F_APP_TYPE_SIGNATURE
				| JavaElementLabels.ALL_CATEGORY, JavaElementImageProvider.SMALL_ICONS | AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS);

		return new DecoratingJavaLabelProvider(result) {
			/* (non-Javadoc)
			 * @see org.eclipse.jdt.internal.ui.viewsupport.ColoringLabelProvider#getText(java.lang.Object)
			 */
			public String getText(Object element) {
				if (element instanceof IPackageFragmentRoot) {
					IPackageFragmentRoot root= (IPackageFragmentRoot) element;
					if (root.isArchive() && root.isExternal()) {
						return JavaElementLabels.getElementLabel(root, JavaElementLabels.ALL_DEFAULT);
					}
				}
				
				return result.getText(element);
			}
			
			/* (non-Javadoc)
			 * @see org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider#getStyledText(java.lang.Object)
			 */
			protected StyledString getStyledText(Object element) {
				return new StyledString(getText(element));
			}
		};
	}
	
	/**
	 * Label provider to use for the tool tips.
	 * 
	 * @return a label provider for the tool tip
	 */
	private ILabelProvider createToolTipLabelProvider() {
		final AppearanceAwareLabelProvider result= new AppearanceAwareLabelProvider(AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS | JavaElementLabels.F_APP_TYPE_SIGNATURE
				| JavaElementLabels.ALL_CATEGORY, JavaElementImageProvider.SMALL_ICONS | AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS);

		return new DecoratingJavaLabelProvider(result);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.EditorBreadcrumb#dispose()
	 */
	public void dispose() {
		super.dispose();

		if (fViewer != null) {
			fBreadcrumbActionGroup.dispose();
			JavaCore.removeElementChangedListener(fElementChangeListener);
			fViewer= null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.EditorBreadcrumb#createContextMenuActionGroup(org.eclipse.jface.viewers.ISelectionProvider)
	 */
	protected ActionGroup createContextMenuActionGroup(ISelectionProvider selectionProvider) {
		return new JavaEditorBreadcrumbActionGroup(getJavaEditor(), selectionProvider);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.EditorBreadcrumb#setInput(java.lang.Object)
	 */
	public void setInput(Object element) {
		if (element == null) {
			element= getCurrentInput();
			if (element instanceof IType) {
				element= ((IType) element).getDeclaringType();
			}
		}
		
		if (element instanceof IJavaElement) {
			super.setInput(getInput((IJavaElement) element));
		} else {
			super.setInput(element);
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.EditorBreadcrumb#getCurrentInput()
	 */
	protected Object getCurrentInput() {
		try {
			ITypeRoot input= SelectionConverter.getInput(getJavaEditor());
			if (input == null)
				return null;
			
			ITextSelection selection;
			if (fEditorSelection instanceof ITextSelection) {
				selection= (ITextSelection) fEditorSelection;
			} else {
				selection= (ITextSelection) getJavaEditor().getSelectionProvider().getSelection();
			}
			return getInput(SelectionConverter.getElementAtOffset(input, selection));
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return null;
		}
	}

	/**
	 * Returns the input for the given element. The Java breadcrumb
	 * does not show some elements of the model:
	 * ITypeRoots
	 * IPackageDeclaration
	 * IImportContainer
	 * IImportDeclaration
	 * 
	 * @param element the potential input element
	 * @return the element to use as input
	 */
	private IJavaElement getInput(IJavaElement element) {
		try {
			if (element instanceof IImportDeclaration)
				element= element.getParent();

			if (element instanceof IImportContainer)
				element= element.getParent();

			if (element instanceof IPackageDeclaration)
				element= element.getParent();

			if (element instanceof ICompilationUnit) {
				IType[] types= ((ICompilationUnit) element).getTypes();
				if (types.length > 0)
					element= types[0];
			}

			if (element instanceof IClassFile)
				element= ((IClassFile) element).getType();
			
			return element;
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return null;
		}
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.EditorBreadcrumb#open(java.lang.Object)
	 */
	protected boolean open(Object element) {
		if (element instanceof IFile)
			return openInNewEditor(element);
		
		if (element instanceof IJarEntryResource)
			return openInNewEditor(element);
		
		if (!(element instanceof IJavaElement))
			return false;
		
		IJavaElement javaElement= (IJavaElement) element;

		ITypeRoot root= (ITypeRoot) javaElement.getAncestor(IJavaElement.COMPILATION_UNIT);
		if (root == null)
			root= (ITypeRoot) javaElement.getAncestor(IJavaElement.CLASS_FILE);

		if (root == null)
			return false;

		return openInNewEditor(element);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.EditorBreadcrumb#reveal(java.lang.Object)
	 */
	protected boolean reveal(Object element) {
		if (!(element instanceof IJavaElement))
			return false;

		IJavaElement javaElement= (IJavaElement) element;

		ITypeRoot inputElement= EditorUtility.getEditorInputJavaElement(getJavaEditor(), false);

		ITypeRoot root= (ITypeRoot) javaElement.getAncestor(IJavaElement.COMPILATION_UNIT);
		if (root == null)
			root= (ITypeRoot) javaElement.getAncestor(IJavaElement.CLASS_FILE);
		
		if (root == null)
			return false;

		if (!root.equals(inputElement))
			return false;
			
		return revealInEditor(javaElement);
	}

	private boolean openInNewEditor(Object element) {
		try {
			IEditorPart newEditor= EditorUtility.openInEditor(element);
			if (newEditor != null && element instanceof IJavaElement)
				EditorUtility.revealInEditor(newEditor, (IJavaElement) element);

			return true;
		} catch (PartInitException e) {
			JavaPlugin.log(e);
			return false;
		}
	}

	private boolean revealInEditor(IJavaElement element) {
		EditorUtility.revealInEditor(getJavaEditor(), element);
		return true;
	}

	private JavaEditor getJavaEditor() {
		return (JavaEditor)getTextEditor();
	}

}
