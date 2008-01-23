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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.IViewerLabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerLabel;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IWorkingCopyProvider;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
import org.eclipse.jdt.ui.ProblemsLabelDecorator.ProblemsLabelChangedEvent;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.BreadcrumbViewer;
import org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.EditorBreadcrumb;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.ColoredString;
import org.eclipse.jdt.internal.ui.viewsupport.DecoratingJavaLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.IRichLabelProvider;
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
	}

	private static final class JavaBreadcrumbLabelProvider implements ILabelProvider, IRichLabelProvider, IViewerLabelProvider {

		private final DecoratingJavaLabelProvider fParent;

		public JavaBreadcrumbLabelProvider(DecoratingJavaLabelProvider parent) {
			fParent= parent;
		}

		/*
		 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			return fParent.getImage(element);
		}

		/*
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			return fParent.getText(element);
		}

		/*
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
		 */
		public void addListener(ILabelProviderListener listener) {
			fParent.addListener(listener);
		}

		/*
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
		 */
		public void dispose() {
			//Ignore: some viewers dispose the label provider
			//we create it, and we dispose it in #internalDispose
		}

		/*
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
		 */
		public boolean isLabelProperty(Object element, String property) {
			return fParent.isLabelProperty(element, property);
		}

		/*
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
		 */
		public void removeListener(ILabelProviderListener listener) {
			fParent.removeListener(listener);
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.viewsupport.IRichLabelProvider#getRichTextLabel(java.lang.Object)
		 */
		public ColoredString getRichTextLabel(Object object) {
			return fParent.getRichTextLabel(object);
		}

		/*
		 * @see org.eclipse.jface.viewers.IViewerLabelProvider#updateLabel(org.eclipse.jface.viewers.ViewerLabel, java.lang.Object)
		 */
		public void updateLabel(ViewerLabel label, Object element) {
			fParent.updateLabel(label, element);
		}

		private void internalDispose() {
			fParent.dispose();
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
				fElements= getTypes((IPackageFragment) inputElement);
			} else if (inputElement instanceof IProject) {
				try {
					fElements= ((IProject) inputElement).members();
				} catch (CoreException e) {
					JavaPlugin.log(e);
				}
			} else {
				fElements= fParent.getChildren(inputElement);
			}

			return fElements;
		}

		/*
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
		 */
		public Object getParent(Object element) {
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
				try {
					return ((IProject) element).members().length > 0;
				} catch (CoreException e) {
					JavaPlugin.log(e);
				}
				return false;
			} else {
				return fParent.hasChildren(element);
			}
		}

		private IType[] getTypes(IPackageFragment pack) {
			ArrayList result= new ArrayList();
			try {
				ICompilationUnit[] units= pack.getCompilationUnits();
				for (int i= 0; i < units.length; i++) {
					IType[] types= units[i].getTypes();
					for (int j= 0; j < types.length; j++) {
						result.add(types[j]);
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}

			try {
				IClassFile[] classFiles= pack.getClassFiles();
				for (int i= 0; i < classFiles.length; i++) {
					result.add(classFiles[i].getType());
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}

			return (IType[]) result.toArray(new IType[result.size()]);
		}

		/*
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		public void dispose() {
			fElements= null;
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

	private ActionGroup fBreadcrumbActionGroup;
	private BreadcrumbViewer fViewer;
	private JavaBreadcrumbLabelProvider fLabelProvider;


	public JavaEditorBreadcrumb(JavaEditor javaEditor) {
		super(javaEditor);
		setTextViewer(javaEditor.getViewer());
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.EditorBreadcrumb#activateBreadcrumb()
	 */
	protected void activateBreadcrumb() {
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
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.EditorBreadcrumb#createViewer(org.eclipse.swt.widgets.Composite)
	 */
	protected BreadcrumbViewer createViewer(Composite composite) {
		fViewer= new ProblemBreadcrumbViewer(composite, SWT.HORIZONTAL);

		AppearanceAwareLabelProvider parentLabelProvider= new AppearanceAwareLabelProvider(AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS | JavaElementLabels.F_APP_TYPE_SIGNATURE
				| JavaElementLabels.ALL_CATEGORY, JavaElementImageProvider.SMALL_ICONS | AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS);
		DecoratingJavaLabelProvider decoratingJavaLabelProvider= new DecoratingJavaLabelProvider(parentLabelProvider, true);

		fLabelProvider= new JavaBreadcrumbLabelProvider(decoratingJavaLabelProvider);
		fViewer.setLabelProvider(fLabelProvider);

		StandardJavaElementContentProvider parentContentProvider= new StandardJavaElementContentProvider(true);
		JavaEditorBreadcrumbContentProvider contentProvider= new JavaEditorBreadcrumbContentProvider(parentContentProvider);
		fViewer.setContentProvider(contentProvider);
		fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				fBreadcrumbActionGroup.setContext(new ActionContext(fViewer.getSelection()));
			}
		});

		fBreadcrumbActionGroup= new JavaEditorBreadcrumbActionGroup(getJavaEditor(), fViewer);

		return fViewer;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.EditorBreadcrumb#dispose()
	 */
	public void dispose() {
		super.dispose();

		if (fViewer != null) {
			fBreadcrumbActionGroup.dispose();
			fLabelProvider.internalDispose();
			fViewer= null;
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.EditorBreadcrumb#fillContextMenu(org.eclipse.jface.action.MenuManager)
	 */
	protected void fillContextMenu(MenuManager manager) {
		JavaPlugin.createStandardGroups(manager);

		fBreadcrumbActionGroup.setContext(new ActionContext(fViewer.getSelection()));
		fBreadcrumbActionGroup.fillContextMenu(manager);

		getJavaEditor().getEditorSite().registerContextMenu(manager, fViewer, false);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.EditorBreadcrumb#getCurrentInput()
	 */
	protected Object getCurrentInput() {
		try {
			IJavaElement result= SelectionConverter.getElementAtOffset(getJavaEditor());
			if (result instanceof IImportDeclaration)
				result= result.getParent();

			if (result instanceof IImportContainer)
				result= result.getParent();

			if (result instanceof IPackageDeclaration)
				result= result.getParent();

			if (result instanceof ICompilationUnit) {
				IType[] types= ((ICompilationUnit) result).getTypes();
				if (types.length > 0)
					result= types[0];
			}

			if (result instanceof IClassFile)
				result= ((IClassFile) result).getType();

			return result;
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return null;
		}
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.EditorBreadcrumb#open(java.lang.Object)
	 */
	protected boolean open(Object element) {
		if (element instanceof IFile) {
			return openInNewEditor(element);
		}
		
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
