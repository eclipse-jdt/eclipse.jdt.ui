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
package org.eclipse.jdt.internal.ui.packageview;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Tree;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.framelist.FrameAction;
import org.eclipse.ui.views.framelist.FrameList;
import org.eclipse.ui.views.framelist.TreeFrame;
import org.eclipse.ui.views.navigator.LocalSelectionTransfer;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dnd.DelegatingDropAdapter;
import org.eclipse.jdt.internal.ui.dnd.JdtViewerDragAdapter;
import org.eclipse.jdt.internal.ui.dnd.ResourceTransferDragAdapter;
import org.eclipse.jdt.internal.ui.dnd.TransferDragSourceListener;
import org.eclipse.jdt.internal.ui.dnd.TransferDropTargetListener;
import org.eclipse.jdt.internal.ui.filters.OutputFolderFilter;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jdt.internal.ui.javaeditor.JarEntryEditorInput;
import org.eclipse.jdt.internal.ui.preferences.MembersOrderPreferenceCache;
import org.eclipse.jdt.internal.ui.util.JavaUIHelp;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.DecoratingJavaLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.FilterUpdater;
import org.eclipse.jdt.internal.ui.viewsupport.IViewPartInputProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;
import org.eclipse.jdt.internal.ui.viewsupport.ProblemTreeViewer;
import org.eclipse.jdt.internal.ui.viewsupport.StatusBarUpdater;
import org.eclipse.jdt.internal.ui.workingsets.WorkingSetFilterActionGroup;

import org.eclipse.jdt.ui.IPackagesViewPart;
import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
import org.eclipse.jdt.ui.actions.CustomFiltersActionGroup;
 
/**
 * The ViewPart for the ProjectExplorer. It listens to part activation events.
 * When selection linking with the editor is enabled the view selection tracks
 * the active editor page. Similarly when a resource is selected in the packages
 * view the corresponding editor is activated. 
 */

public class PackageExplorerPart extends ViewPart 
	implements ISetSelectionTarget, IMenuListener,
		IShowInTarget,
		IPackagesViewPart,  IPropertyChangeListener, 
		IViewPartInputProvider {
	
	private boolean fIsCurrentLayoutFlat; // true means flat, false means hierachical

	private static final int HIERARCHICAL_LAYOUT= 0x1;
	private static final int FLAT_LAYOUT= 0x2;
	
	public final static String VIEW_ID= JavaUI.ID_PACKAGES;
				
	// Persistance tags.
	static final String TAG_SELECTION= "selection"; //$NON-NLS-1$
	static final String TAG_EXPANDED= "expanded"; //$NON-NLS-1$
	static final String TAG_ELEMENT= "element"; //$NON-NLS-1$
	static final String TAG_PATH= "path"; //$NON-NLS-1$
	static final String TAG_VERTICAL_POSITION= "verticalPosition"; //$NON-NLS-1$
	static final String TAG_HORIZONTAL_POSITION= "horizontalPosition"; //$NON-NLS-1$
	static final String TAG_FILTERS = "filters"; //$NON-NLS-1$
	static final String TAG_FILTER = "filter"; //$NON-NLS-1$
	static final String TAG_LAYOUT= "layout"; //$NON-NLS-1$
	static final String TAG_CURRENT_FRAME= "currentFramge"; //$NON-NLS-1$
	

	private PackageExplorerContentProvider fContentProvider;
	private FilterUpdater fFilterUpdater;
	
	private PackageExplorerActionGroup fActionSet;
	private ProblemTreeViewer fViewer; 
	private Menu fContextMenu;		
	
	private IMemento fMemento;	
	private ISelectionChangedListener fSelectionListener;
	
	private String fWorkingSetName;
	
	private IPartListener fPartListener= new IPartListener() {
		public void partActivated(IWorkbenchPart part) {
			if (part instanceof IEditorPart)
				editorActivated((IEditorPart) part);
		}
		public void partBroughtToTop(IWorkbenchPart part) {
		}
		public void partClosed(IWorkbenchPart part) {
		}
		public void partDeactivated(IWorkbenchPart part) {
		}
		public void partOpened(IWorkbenchPart part) {
		}
	};
	
	private ITreeViewerListener fExpansionListener= new ITreeViewerListener() {
		public void treeCollapsed(TreeExpansionEvent event) {
		}
		
		public void treeExpanded(TreeExpansionEvent event) {
			Object element= event.getElement();
			if (element instanceof ICompilationUnit || 
				element instanceof IClassFile)
				expandMainType(element);
		}
	};

	private class PackageExplorerProblemTreeViewer extends ProblemTreeViewer {
		boolean fInChangeInput;
		
		public PackageExplorerProblemTreeViewer(Composite parent, int style) {
			super(parent, style);
		}
		
		protected void inputChanged(Object input, Object oldInput) {
			fInChangeInput= true;
			try {
				super.inputChanged(input, oldInput);
			} finally {
				fInChangeInput= false;
			}
		}
		
		public void add(Object parentElement, Object[] childElements) {
			if (!fInChangeInput)
				super.add(parentElement, childElements);
		}
		
		public void remove(Object[] elements) {
			if (!fInChangeInput)
				super.remove(elements);
		}
		
		public void refresh(Object element, boolean updateLabels) {
			if (!fInChangeInput)
				super.refresh(element, updateLabels);
		}
		/*
		 * @see org.eclipse.jface.viewers.StructuredViewer#filter(java.lang.Object)
		 */
		protected Object[] getFilteredChildren(Object parent) {
			List list = new ArrayList();
			ViewerFilter[] filters = fViewer.getFilters();
			Object[] children = ((ITreeContentProvider) fViewer.getContentProvider()).getChildren(parent);
			for (int i = 0; i < children.length; i++) {
				Object object = children[i];
				if (!isEssential(object)) {
					object = filter(object, parent, filters);
					if (object != null) {
						list.add(object);
					}
				} else
					list.add(object);
			}
			return list.toArray();
		}
		/*
		 * @see AbstractTreeViewer#isExpandable(java.lang.Object)
		 */
		public boolean isExpandable(Object parent) {
			if (isFlatLayout())
				return super.isExpandable(parent);
			
			ViewerFilter[] filters= fViewer.getFilters();
			Object[] children= ((ITreeContentProvider) fViewer.getContentProvider()).getChildren(parent);
			for (int i = 0; i < children.length; i++) {
				Object object= children[i];
				
				if (isEssential(object))
					return true;
				
				object= filter(object, parent, filters);
				if (object != null)
					return true;
			}
			return false;
		}
		
		// Sends the object through the given filters
		private Object filter(Object object, Object parent, ViewerFilter[] filters) {
			for (int i = 0; i < filters.length; i++) {
				ViewerFilter filter = filters[i];
				if (!filter.select(fViewer, parent, object))
					return null;
			}
			return object;
		}
	
		/* Checks if a filtered object in essential (ie. is a parent that
		 * should not be removed).
		 */ 
		private boolean isEssential(Object object) {
			try {
				if (!isFlatLayout() && object instanceof IPackageFragment) {
					IPackageFragment fragment = (IPackageFragment) object;
					return !fragment.isDefaultPackage() && fragment.hasSubpackages();
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
			return false;
		}
		
		protected void handleInvalidSelection(ISelection invalidSelection, ISelection newSelection) {
			IStructuredSelection is= (IStructuredSelection)invalidSelection;
			List ns= null;
			if (newSelection instanceof IStructuredSelection) {
				ns= new ArrayList(((IStructuredSelection)newSelection).toList());
			} else {
				ns= new ArrayList();
			}
			boolean changed= false;
			for (Iterator iter= is.iterator(); iter.hasNext();) {
				Object element= iter.next();
				if (element instanceof IJavaProject) {
					IProject project= ((IJavaProject)element).getProject();
					if (!project.isOpen()) {
						ns.add(project);
						changed= true;
					}
				} else if (element instanceof IProject) {
					IProject project= (IProject)element;
					if (project.isOpen()) {
						IJavaProject jProject= JavaCore.create(project);
						if (jProject != null && jProject.exists())
							ns.add(jProject);
							changed= true;
					}
				}
			}
			if (changed) {
				newSelection= new StructuredSelection(ns);
				setSelection(newSelection);
			}
			super.handleInvalidSelection(invalidSelection, newSelection);
		}
	}
 
	private PackageExplorerLabelProvider fLabelProvider;	
	
	/* (non-Javadoc)
	 * Method declared on IViewPart.
	 */
	private boolean fLinkingEnabled;

    public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		fMemento= memento;
		restoreLayoutState(memento);
	}

	private void restoreLayoutState(IMemento memento) {
		Integer state= null;
		if (memento != null)
			state= memento.getInteger(TAG_LAYOUT);

		// If no memento try an restore from preference store
		if(state == null) {
			IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
			state= new Integer(store.getInt(TAG_LAYOUT));
		}

		if (state.intValue() == FLAT_LAYOUT)
			fIsCurrentLayoutFlat= true;
		else if (state.intValue() == HIERARCHICAL_LAYOUT)
			fIsCurrentLayoutFlat= false;
		else
			fIsCurrentLayoutFlat= true;
	}
	
	/**
	 * Returns the package explorer part of the active perspective. If 
	 * there isn't any package explorer part <code>null</code> is returned.
	 */
	public static PackageExplorerPart getFromActivePerspective() {
		IWorkbenchPage activePage= JavaPlugin.getActivePage();
		if (activePage == null)
			return null;
		IViewPart view= activePage.findView(VIEW_ID);
		if (view instanceof PackageExplorerPart)
			return (PackageExplorerPart)view;
		return null;	
	}
	
	/**
	 * Makes the package explorer part visible in the active perspective. If there
	 * isn't a package explorer part registered <code>null</code> is returned.
	 * Otherwise the opened view part is returned.
	 */
	public static PackageExplorerPart openInActivePerspective() {
		try {
			return (PackageExplorerPart)JavaPlugin.getActivePage().showView(VIEW_ID);
		} catch(PartInitException pe) {
			return null;
		}
	} 
		
	 public void dispose() {
		if (fContextMenu != null && !fContextMenu.isDisposed())
			fContextMenu.dispose();
		getSite().getPage().removePartListener(fPartListener);
		JavaPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
		if (fViewer != null)
			fViewer.removeTreeListener(fExpansionListener);
		
		if (fActionSet != null)	
			fActionSet.dispose();
		if (fFilterUpdater != null)
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(fFilterUpdater);
		super.dispose();	
	}

	/**
	 * Implementation of IWorkbenchPart.createPartControl(Composite)
	 */
	public void createPartControl(Composite parent) {
		
		fViewer= createViewer(parent);
		fViewer.setUseHashlookup(true);
		if (!JavaPlugin.USE_WORKING_COPY_OWNERS) {
			fViewer.setComparer(new PackageExplorerElementComparer());
		}
		initDragAndDrop();
		
		setProviders();
		
		JavaPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
	
		
		MenuManager menuMgr= new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(this);
		fContextMenu= menuMgr.createContextMenu(fViewer.getTree());
		fViewer.getTree().setMenu(fContextMenu);
		
		// Register viewer with site. This must be done before making the actions.
		IWorkbenchPartSite site= getSite();
		site.registerContextMenu(menuMgr, fViewer);
		site.setSelectionProvider(fViewer);
		site.getPage().addPartListener(fPartListener);
		
		if (fMemento != null) {
			restoreLinkingEnabled(fMemento);
		}
		
		makeActions(); // call before registering for selection changes
		
		// Set input after filter and sorter has been set. This avoids resorting and refiltering.
		restoreFilterAndSorter();
		fViewer.setInput(findInputElement());
		initFrameActions();
		initKeyListener();
			

		fViewer.addPostSelectionChangedListener(fSelectionListener);
		
		fViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				fActionSet.handleDoubleClick(event);
			}
		});
		
		fViewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				fActionSet.handleOpen(event);
			}
		});

		IStatusLineManager slManager= getViewSite().getActionBars().getStatusLineManager();
		fViewer.addSelectionChangedListener(new StatusBarUpdater(slManager));
		fViewer.addTreeListener(fExpansionListener);
	
		if (fMemento != null)
			restoreUIState(fMemento);
		fMemento= null;
	
		// Set help for the view 
		JavaUIHelp.setHelp(fViewer, IJavaHelpContextIds.PACKAGES_VIEW);
		
		fillActionBars();

		updateTitle();
		
		fFilterUpdater= new FilterUpdater(fViewer);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(fFilterUpdater);
	}

	private void initFrameActions() {
		fActionSet.getUpAction().update();
		fActionSet.getBackAction().update();
		fActionSet.getForwardAction().update();
	}

	/**
	 * This viewer ensures that non-leaves in the hierarchical
	 * layout are not removed by any filters.
	 * 
	 * @since 2.1
	 */
	private ProblemTreeViewer createViewer(Composite composite) {
		return  new PackageExplorerProblemTreeViewer(composite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
	}

	/**
	 * Answers whether this part shows the packages flat or hierarchical.
	 * 
	 * @since 2.1
	 */
	boolean isFlatLayout() {
		return fIsCurrentLayoutFlat;
	}
	
	private void setProviders() {
		//content provider must be set before the label provider
		fContentProvider= createContentProvider();
		fContentProvider.setIsFlatLayout(fIsCurrentLayoutFlat);
		fViewer.setContentProvider(fContentProvider);
	
		fLabelProvider= createLabelProvider();
		fLabelProvider.setIsFlatLayout(fIsCurrentLayoutFlat);
		fViewer.setLabelProvider(new DecoratingJavaLabelProvider(fLabelProvider, false));
		// problem decoration provided by PackageLabelProvider
	}
	
	void toggleLayout() {

		// Update current state and inform content and label providers
		fIsCurrentLayoutFlat= !fIsCurrentLayoutFlat;
		saveLayoutState(null);
		
		fContentProvider.setIsFlatLayout(isFlatLayout());
		fLabelProvider.setIsFlatLayout(isFlatLayout());
		
		fViewer.getControl().setRedraw(false);
		fViewer.refresh();
		fViewer.getControl().setRedraw(true);
	}
	
	/**
	 * This method should only be called inside this class
	 * and from test cases.
	 */
	public PackageExplorerContentProvider createContentProvider() {
		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		boolean showCUChildren= store.getBoolean(PreferenceConstants.SHOW_CU_CHILDREN);
		return new PackageExplorerContentProvider(this, showCUChildren);
	}
	
	private PackageExplorerLabelProvider createLabelProvider() {
		return new PackageExplorerLabelProvider(AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS | JavaElementLabels.P_COMPRESSED,
				AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS | JavaElementImageProvider.SMALL_ICONS,
				fContentProvider);			
	}
	
	private void fillActionBars() {
		IActionBars actionBars= getViewSite().getActionBars();
		fActionSet.fillActionBars(actionBars);
	}
	
	private Object findInputElement() {
		Object input= getSite().getPage().getInput();
		if (input instanceof IWorkspace) { 
			return JavaCore.create(((IWorkspace)input).getRoot());
		} else if (input instanceof IContainer) {
			IJavaElement element= JavaCore.create((IContainer)input);
			if (element != null && element.exists())
				return element;
			return input;
		}
		//1GERPRT: ITPJUI:ALL - Packages View is empty when shown in Type Hierarchy Perspective
		// we can't handle the input
		// fall back to show the workspace
		return JavaCore.create(JavaPlugin.getWorkspace().getRoot());	
	}
	
	/**
	 * Answer the property defined by key.
	 */
	public Object getAdapter(Class key) {
		if (key.equals(ISelectionProvider.class))
			return fViewer;
		if (key == IShowInSource.class) {
			return getShowInSource();
		}
		if (key == IShowInTargetList.class) {
			return new IShowInTargetList() {
				public String[] getShowInTargetIds() {
					return new String[] { IPageLayout.ID_RES_NAV };
				}

			};
		}
		return super.getAdapter(key);
	}

	/**
	 * Returns the tool tip text for the given element.
	 */
	String getToolTipText(Object element) {
		String result;
		if (!(element instanceof IResource)) {
			if (element instanceof IJavaModel) 
				result= PackagesMessages.getString("PackageExplorerPart.workspace"); //$NON-NLS-1$
			else
				result= JavaElementLabels.getTextLabel(element, AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS);		
		} else {
			IPath path= ((IResource) element).getFullPath();
			if (path.isRoot()) {
				result= PackagesMessages.getString("PackageExplorer.title"); //$NON-NLS-1$
			} else {
				result= path.makeRelative().toString();
			}
		}
		
		if (fWorkingSetName == null)
			return result;

		String wsstr= PackagesMessages.getFormattedString("PackageExplorer.toolTip", new String[] { fWorkingSetName }); //$NON-NLS-1$
		if (result.length() == 0)
			return wsstr;
		return PackagesMessages.getFormattedString("PackageExplorer.toolTip2", new String[] { result, fWorkingSetName }); //$NON-NLS-1$
	}
	
	public String getTitleToolTip() {
		if (fViewer == null)
			return super.getTitleToolTip();
		return getToolTipText(fViewer.getInput());
	}
	
	/**
	 * @see IWorkbenchPart#setFocus()
	 */
	public void setFocus() {
		fViewer.getTree().setFocus();
	}

	/**
	 * Returns the current selection.
	 */
	private ISelection getSelection() {
		return fViewer.getSelection();
	}
	  
	//---- Action handling ----------------------------------------------------------
	
	/**
	 * Called when the context menu is about to open. Override
	 * to add your own context dependent menu contributions.
	 */
	public void menuAboutToShow(IMenuManager menu) {
		JavaPlugin.createStandardGroups(menu);
		
		fActionSet.setContext(new ActionContext(getSelection()));
		fActionSet.fillContextMenu(menu);
		fActionSet.setContext(null);
	}

	private void makeActions() {
		fActionSet= new PackageExplorerActionGroup(this);
	}
	
	//---- Event handling ----------------------------------------------------------
	
	private void initDragAndDrop() {
		initDrag();
		initDrop();
	}

	private void initDrag() {
		int ops= DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK;
		Transfer[] transfers= new Transfer[] {
			LocalSelectionTransfer.getInstance(), 
			ResourceTransfer.getInstance(),
			FileTransfer.getInstance()};
		TransferDragSourceListener[] dragListeners= new TransferDragSourceListener[] {
			new SelectionTransferDragAdapter(fViewer),
			new ResourceTransferDragAdapter(fViewer),
			new FileTransferDragAdapter(fViewer)
		};
		fViewer.addDragSupport(ops, transfers, new JdtViewerDragAdapter(fViewer, dragListeners));
				}

	private void initDrop() {
		int ops= DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK | DND.DROP_DEFAULT;
		Transfer[] transfers= new Transfer[] {
			LocalSelectionTransfer.getInstance(), 
			FileTransfer.getInstance()};
		TransferDropTargetListener[] dropListeners= new TransferDropTargetListener[] {
			new SelectionTransferDropAdapter(fViewer),
			new FileTransferDropAdapter(fViewer)
		};
		fViewer.addDropSupport(ops, transfers, new DelegatingDropAdapter(dropListeners));
					}

	/**
	 * Handles selection changed in viewer.
	 * Updates global actions.
	 * Links to editor (if option enabled)
	 */
	private void handleSelectionChanged(SelectionChangedEvent event) {
		IStructuredSelection selection= (IStructuredSelection) event.getSelection();
		fActionSet.handleSelectionChanged(event);
		if (isLinkingEnabled())
			linkToEditor(selection);
	}

	public void selectReveal(ISelection selection) {
		selectReveal(selection, 0);
	}
	
	private void selectReveal(final ISelection selection, final int count) {
		Control ctrl= getViewer().getControl();
		if (ctrl == null || ctrl.isDisposed())
			return;
		ISelection javaSelection= convertSelection(selection);
		fViewer.setSelection(javaSelection, true);
		PackageExplorerContentProvider provider= (PackageExplorerContentProvider)getViewer().getContentProvider();
		ISelection cs= fViewer.getSelection();
		// If we have Pending changes and the element could not be selected then
		// we try it again on more time by posting the select and reveal asynchronuoulsy
		// to the event queue. See PR http://bugs.eclipse.org/bugs/show_bug.cgi?id=30700
		// for a discussion of the underlying problem.
		if (count == 0 && provider.hasPendingChanges() && !javaSelection.equals(cs)) {
			ctrl.getDisplay().asyncExec(new Runnable() {
				public void run() {
					selectReveal(selection, count + 1);
				}
			});
		}
	}

	private ISelection convertSelection(ISelection s) {
		if (!(s instanceof IStructuredSelection))
			return s;
			
		Object[] elements= ((StructuredSelection)s).toArray();
		if (!containsResources(elements))
			return s;
				
		for (int i= 0; i < elements.length; i++) {
			Object o= elements[i];
			if (!(o instanceof IJavaElement)) {
				if (o instanceof IResource) {
					IJavaElement jElement= JavaCore.create((IResource)o);
					if (jElement != null && jElement.exists()) 
						elements[i]= jElement;
				}
				else if (o instanceof IAdaptable) {
					IResource r= (IResource)((IAdaptable)o).getAdapter(IResource.class);
					if (r != null) {
						IJavaElement jElement= JavaCore.create(r);
						if (jElement != null && jElement.exists()) 
							elements[i]= jElement;
						else
							elements[i]= r;
					}
				}
			}
		}
		
		return new StructuredSelection(elements);
	}
	
	private boolean containsResources(Object[] elements) {
		for (int i = 0; i < elements.length; i++) {
			Object o= elements[i];
			if (!(o instanceof IJavaElement)) {
				if (o instanceof IResource)
					return true;
				if ((o instanceof IAdaptable) && ((IAdaptable)o).getAdapter(IResource.class) != null)
					return true;
				}
		}
		return false;
	}
	
	public void selectAndReveal(Object element) {
		selectReveal(new StructuredSelection(element));
	}
	
	boolean isLinkingEnabled() {
		return fLinkingEnabled;
	}
	
	/**
	 * Initializes the linking enabled setting from the preference store.
	 */
	private void initLinkingEnabled() {
		fLinkingEnabled= PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.LINK_PACKAGES_TO_EDITOR);
	}


	/**
	 * Links to editor (if option enabled)
	 */
	private void linkToEditor(IStructuredSelection selection) {
		// ignore selection changes if the package explorer is not the active part.
		// In this case the selection change isn't triggered by a user.
		if (!isActivePart())
			return;
		Object obj= selection.getFirstElement();

		if (selection.size() == 1) {
			IEditorPart part= EditorUtility.isOpenInEditor(obj);
			if (part != null) {
				IWorkbenchPage page= getSite().getPage();
				page.bringToTop(part);
				if (obj instanceof IJavaElement)
					EditorUtility.revealInEditor(part, (IJavaElement)obj);
			}
		}
	}

	private boolean isActivePart() {
		return this == getSite().getPage().getActivePart();
	}


	
	public void saveState(IMemento memento) {
		if (fViewer == null) {
			// part has not been created
			if (fMemento != null) //Keep the old state;
				memento.putMemento(fMemento);
			return;
		}
// disable the persisting of state which can trigger expensive operations as
// a side effect: see bug 52474 and 53958
		saveCurrentFrame(memento);
//		saveExpansionState(memento);
//		saveSelectionState(memento);
		saveLayoutState(memento);
		saveLinkingEnabled(memento);
		// commented out because of http://bugs.eclipse.org/bugs/show_bug.cgi?id=4676
		//saveScrollState(memento, fViewer.getTree());
		fActionSet.saveFilterAndSorterState(memento);
	}
	
	private void saveCurrentFrame(IMemento memento) {
        FrameAction action = fActionSet.getUpAction();
        FrameList frameList= action.getFrameList();

		if (frameList.getCurrentIndex() > 0) {
			TreeFrame currentFrame = (TreeFrame) frameList.getCurrentFrame();
			IMemento frameMemento = memento.createChild(TAG_CURRENT_FRAME);
			currentFrame.saveState(frameMemento);
		}
	}

	private void saveLinkingEnabled(IMemento memento) {
		memento.putInteger(PreferenceConstants.LINK_PACKAGES_TO_EDITOR, fLinkingEnabled ? 1 : 0);
	}

	/**
	 * Saves the current layout state.
	 * 
	 * @param memento	the memento to save the state into or
	 * 					<code>null</code> to store the state in the preferences
	 * @since 2.1
	 */
	private void saveLayoutState(IMemento memento) {
		if (memento != null) {	
			memento.putInteger(TAG_LAYOUT, getLayoutAsInt());
		} else {
		//if memento is null save in preference store
			IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
			store.setValue(TAG_LAYOUT, getLayoutAsInt());
		}
	}

	private int getLayoutAsInt() {
		if (fIsCurrentLayoutFlat)
			return FLAT_LAYOUT;
		else
			return HIERARCHICAL_LAYOUT;
	}

	protected void saveScrollState(IMemento memento, Tree tree) {
		ScrollBar bar= tree.getVerticalBar();
		int position= bar != null ? bar.getSelection() : 0;
		memento.putString(TAG_VERTICAL_POSITION, String.valueOf(position));
		//save horizontal position
		bar= tree.getHorizontalBar();
		position= bar != null ? bar.getSelection() : 0;
		memento.putString(TAG_HORIZONTAL_POSITION, String.valueOf(position));
	}

	protected void saveSelectionState(IMemento memento) {
		Object elements[]= ((IStructuredSelection) fViewer.getSelection()).toArray();
		if (elements.length > 0) {
			IMemento selectionMem= memento.createChild(TAG_SELECTION);
			for (int i= 0; i < elements.length; i++) {
				IMemento elementMem= selectionMem.createChild(TAG_ELEMENT);
				// we can only persist JavaElements for now
				Object o= elements[i];
				if (o instanceof IJavaElement)
					elementMem.putString(TAG_PATH, ((IJavaElement) elements[i]).getHandleIdentifier());
			}
		}
	}

	protected void saveExpansionState(IMemento memento) {
		Object expandedElements[]= fViewer.getVisibleExpandedElements();
		if (expandedElements.length > 0) {
			IMemento expandedMem= memento.createChild(TAG_EXPANDED);
			for (int i= 0; i < expandedElements.length; i++) {
				IMemento elementMem= expandedMem.createChild(TAG_ELEMENT);
				// we can only persist JavaElements for now
				Object o= expandedElements[i];
				if (o instanceof IJavaElement)
					elementMem.putString(TAG_PATH, ((IJavaElement) expandedElements[i]).getHandleIdentifier());
			}
		}
	}

	private void restoreFilterAndSorter() {
		fViewer.addFilter(new OutputFolderFilter());
		fViewer.setSorter(new JavaElementSorter());
		if (fMemento != null)	
			fActionSet.restoreFilterAndSorterState(fMemento);
	}

	private void restoreUIState(IMemento memento) {
		// see comment in save state
		restoreCurrentFrame(memento);
		//restoreExpansionState(memento);
		//restoreSelectionState(memento);
		// commented out because of http://bugs.eclipse.org/bugs/show_bug.cgi?id=4676
		//restoreScrollState(memento, fViewer.getTree());
	}

	private void restoreCurrentFrame(IMemento memento) {
		IMemento frameMemento = memento.getChild(TAG_CURRENT_FRAME);
		
		if (frameMemento != null) {
	        FrameAction action = fActionSet.getUpAction();
	        FrameList frameList= action.getFrameList();
			TreeFrame frame = new TreeFrame(fViewer);
			frame.restoreState(frameMemento);
			frame.setName(getFrameName(frame.getInput()));
			frame.setToolTipText(getToolTipText(frame.getInput()));
			frameList.gotoFrame(frame);
		}
	}

	private void restoreLinkingEnabled(IMemento memento) {
		Integer val= memento.getInteger(PreferenceConstants.LINK_PACKAGES_TO_EDITOR);
		if (val != null) {
			fLinkingEnabled= val.intValue() != 0;
		}
	}

	protected void restoreScrollState(IMemento memento, Tree tree) {
		ScrollBar bar= tree.getVerticalBar();
		if (bar != null) {
			try {
				String posStr= memento.getString(TAG_VERTICAL_POSITION);
				int position;
				position= new Integer(posStr).intValue();
				bar.setSelection(position);
			} catch (NumberFormatException e) {
				// ignore, don't set scrollposition
			}
		}
		bar= tree.getHorizontalBar();
		if (bar != null) {
			try {
				String posStr= memento.getString(TAG_HORIZONTAL_POSITION);
				int position;
				position= new Integer(posStr).intValue();
				bar.setSelection(position);
			} catch (NumberFormatException e) {
				// ignore don't set scroll position
			}
		}
	}

	protected void restoreSelectionState(IMemento memento) {
		IMemento childMem;
		childMem= memento.getChild(TAG_SELECTION);
		if (childMem != null) {
			ArrayList list= new ArrayList();
			IMemento[] elementMem= childMem.getChildren(TAG_ELEMENT);
			for (int i= 0; i < elementMem.length; i++) {
				Object element= JavaCore.create(elementMem[i].getString(TAG_PATH));
				if (element != null)
					list.add(element);
			}
			fViewer.setSelection(new StructuredSelection(list));
		}
	}

	protected void restoreExpansionState(IMemento memento) {
		IMemento childMem= memento.getChild(TAG_EXPANDED);
		if (childMem != null) {
			ArrayList elements= new ArrayList();
			IMemento[] elementMem= childMem.getChildren(TAG_ELEMENT);
			for (int i= 0; i < elementMem.length; i++) {
				Object element= JavaCore.create(elementMem[i].getString(TAG_PATH));
				if (element != null)
					elements.add(element);
			}
			fViewer.setExpandedElements(elements.toArray());
		}
	}
	
	/**
	 * Create the KeyListener for doing the refresh on the viewer.
	 */
	private void initKeyListener() {
		fViewer.getControl().addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent event) {
				fActionSet.handleKeyEvent(event);
			}
		});
	}

	/**
	 * An editor has been activated.  Set the selection in this Packages Viewer
	 * to be the editor's input, if linking is enabled.
	 */
	void editorActivated(IEditorPart editor) {
		if (!isLinkingEnabled())  
			return;
		Object input= getElementOfInput(editor.getEditorInput());
		if (input == null) 
			return;
		if (!inputIsSelected(editor.getEditorInput()))
			showInput(input);
	}

	private boolean inputIsSelected(IEditorInput input) {
		IStructuredSelection selection= (IStructuredSelection)fViewer.getSelection();
		if (selection.size() != 1) 
			return false;
		IEditorInput selectionAsInput= null;
		try {
			selectionAsInput= EditorUtility.getEditorInput(selection.getFirstElement());
		} catch (JavaModelException e1) {
			return false;
		}
		return input.equals(selectionAsInput);
	}

	boolean showInput(Object input) {
		Object element= null;
			
		if (input instanceof IFile && isOnClassPath((IFile)input)) {
			element= JavaCore.create((IFile)input);
		}
				
		if (element == null) // try a non Java resource
			element= input;
				
		if (element != null) {
			ISelection newSelection= new StructuredSelection(element);
			if (fViewer.getSelection().equals(newSelection)) {
				fViewer.reveal(element);
			} else {
				try {
					fViewer.removeSelectionChangedListener(fSelectionListener);						
					fViewer.setSelection(newSelection, true);
	
					while (element != null && fViewer.getSelection().isEmpty()) {
						// Try to select parent in case element is filtered
						element= getParent(element);
						if (element != null) {
							newSelection= new StructuredSelection(element);
							fViewer.setSelection(newSelection, true);
						}
					}
				} finally {
					fViewer.addSelectionChangedListener(fSelectionListener);
				}
			}
			return true;
		}
		return false;
	}
	
	private boolean isOnClassPath(IFile file) {
		IJavaProject jproject= JavaCore.create(file.getProject());
		return jproject.isOnClasspath(file);
	}

	/**
	 * Returns the element's parent.
	 * 
	 * @return the parent or <code>null</code> if there's no parent
	 */
	private Object getParent(Object element) {
		if (element instanceof IJavaElement)
			return ((IJavaElement)element).getParent();
		else if (element instanceof IResource)
			return ((IResource)element).getParent();
//		else if (element instanceof IStorage) {
			// can't get parent - see bug 22376
//		}
		return null;
	}
	
	/**
	 * A compilation unit or class was expanded, expand
	 * the main type.  
	 */
	void expandMainType(Object element) {
		try {
			IType type= null;
			if (element instanceof ICompilationUnit) {
				ICompilationUnit cu= (ICompilationUnit)element;
				IType[] types= cu.getTypes();
				if (types.length > 0)
					type= types[0];
			}
			else if (element instanceof IClassFile) {
				IClassFile cf= (IClassFile)element;
				type= cf.getType();
			}			
			if (type != null) {
				final IType type2= type;
				Control ctrl= fViewer.getControl();
				if (ctrl != null && !ctrl.isDisposed()) {
					ctrl.getDisplay().asyncExec(new Runnable() {
						public void run() {
							Control ctrl2= fViewer.getControl();
							if (ctrl2 != null && !ctrl2.isDisposed()) 
								fViewer.expandToLevel(type2, 1);
						}
					}); 
				}
			}
		} catch(JavaModelException e) {
			// no reveal
		}
	}
	
	/**
	 * Returns the element contained in the EditorInput
	 */
	Object getElementOfInput(IEditorInput input) {
		if (input instanceof IClassFileEditorInput)
			return ((IClassFileEditorInput)input).getClassFile();
		else if (input instanceof IFileEditorInput)
			return ((IFileEditorInput)input).getFile();
		else if (input instanceof JarEntryEditorInput)
			return ((JarEntryEditorInput)input).getStorage();
		return null;
	}
	
	/**
 	 * Returns the Viewer.
 	 */
	TreeViewer getViewer() {
		return fViewer;
	}
	
	/**
 	 * Returns the TreeViewer.
 	 */
	public TreeViewer getTreeViewer() {
		return fViewer;
	}
	
	boolean isExpandable(Object element) {
		if (fViewer == null)
			return false;
		return fViewer.isExpandable(element);
	}

	void setWorkingSetName(String workingSetName) {
		fWorkingSetName= workingSetName;
	}
	
	/**
	 * Updates the title text and title tool tip.
	 * Called whenever the input of the viewer changes.
	 */ 
	void updateTitle() {		
		Object input= fViewer.getInput();
		String viewName= getConfigurationElement().getAttribute("name"); //$NON-NLS-1$
		if (input == null
			|| (input instanceof IJavaModel)) {
			setTitle(viewName);
			setTitleToolTip(""); //$NON-NLS-1$
		} else {
			String inputText= JavaElementLabels.getTextLabel(input, AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS);
			String title= PackagesMessages.getFormattedString("PackageExplorer.argTitle", new String[] { viewName, inputText }); //$NON-NLS-1$
			setTitle(title);
			setTitleToolTip(getToolTipText(input));
		} 
	}
	
	/**
	 * Sets the decorator for the package explorer.
	 *
	 * @param decorator a label decorator or <code>null</code> for no decorations.
	 * @deprecated To be removed
	 */
	public void setLabelDecorator(ILabelDecorator decorator) {
	}
	
	/*
	 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (fViewer == null)
			return;
		
		boolean refreshViewer= false;
	
		if (PreferenceConstants.SHOW_CU_CHILDREN.equals(event.getProperty())) {
			fActionSet.updateActionBars(getViewSite().getActionBars());
			
			boolean showCUChildren= PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.SHOW_CU_CHILDREN);
			((StandardJavaElementContentProvider)fViewer.getContentProvider()).setProvideMembers(showCUChildren);
			
			refreshViewer= true;
		} else if (MembersOrderPreferenceCache.isMemberOrderProperty(event.getProperty())) {
			refreshViewer= true;
		}

		if (refreshViewer)
			fViewer.refresh();
	}
	
	/* (non-Javadoc)
	 * @see IViewPartInputProvider#getViewPartInput()
	 */
	public Object getViewPartInput() {
		if (fViewer != null) {
			return fViewer.getInput();
		}
		return null;
	}

	public void collapseAll() {
		try {
			fViewer.getControl().setRedraw(false);		
			fViewer.collapseToLevel(getViewPartInput(), AbstractTreeViewer.ALL_LEVELS);
		} finally {
			fViewer.getControl().setRedraw(true);
		}
	}
	
	public PackageExplorerPart() { 
		initLinkingEnabled();
		fSelectionListener= new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleSelectionChanged(event);
			}
		};
	}

	public boolean show(ShowInContext context) {
		Object input= context.getInput();
		if (input instanceof IEditorInput) {
			Object elementOfInput= getElementOfInput((IEditorInput)context.getInput());
			if (elementOfInput == null) 
				return false; 
			return tryToReveal(elementOfInput);
		}

		ISelection selection= context.getSelection();
		if (selection != null) {
			selectReveal(selection);
			return true;
		}
		return false;
	}

	/**
	 * Returns the <code>IShowInSource</code> for this view.
	 */
	protected IShowInSource getShowInSource() {
		return new IShowInSource() {
			public ShowInContext getShowInContext() {
				return new ShowInContext(
					getViewer().getInput(),
					getViewer().getSelection());
			}
		};
	}

	/*
	 * @see org.eclipse.ui.views.navigator.IResourceNavigator#setLinkingEnabled(boolean)
	 * @since 2.1
	 */
	public void setLinkingEnabled(boolean enabled) {
		fLinkingEnabled= enabled;
		PreferenceConstants.getPreferenceStore().setValue(PreferenceConstants.LINK_PACKAGES_TO_EDITOR, enabled);

		if (enabled) {
			IEditorPart editor = getSite().getPage().getActiveEditor();
			if (editor != null) {
				editorActivated(editor);
			}
		}
	}

	/**
	 * Returns the name for the given element.
	 * Used as the name for the current frame. 
	 */
	String getFrameName(Object element) {
		if (element instanceof IJavaElement) {
			return ((IJavaElement) element).getElementName();
		} else {
			return ((ILabelProvider) getTreeViewer().getLabelProvider()).getText(element);
		}
	}
	
	void projectStateChanged(Object root) {
		Control ctrl= fViewer.getControl();
		if (ctrl != null && !ctrl.isDisposed()) {
			fViewer.refresh(root, true);
			// trigger a syntetic selection change so that action refresh their
			// enable state.
			fViewer.setSelection(fViewer.getSelection());
		}
	}

    public boolean tryToReveal(Object element) {
        if (revealElementOrParent(element))
            return true;
        
        WorkingSetFilterActionGroup workingSetGroup = fActionSet.getWorkingSetActionGroup();
        IWorkingSet workingSet = workingSetGroup.getWorkingSet();  	    
        if (workingSetGroup.isFiltered(getVisibleParent(element), element)) {
            String message= PackagesMessages.getFormattedString("PackageExplorer.notFound", workingSet.getName());  //$NON-NLS-1$
            if (MessageDialog.openQuestion(getSite().getShell(), PackagesMessages.getString("PackageExplorer.filteredDialog.title"), message)) { //$NON-NLS-1$
                workingSetGroup.setWorkingSet(null, true);		
                if (revealElementOrParent(element))
                    return true;
            }
        }
        // try to remove filters
        CustomFiltersActionGroup filterGroup = fActionSet.getCustomFilterActionGroup();
        String[] filters= filterGroup.removeFiltersFor(getVisibleParent(element), element, getTreeViewer().getContentProvider()); 
        if (filters.length > 0) {
            String message= PackagesMessages.getString("PackageExplorer.removeFilters"); //$NON-NLS-1$
            if (MessageDialog.openQuestion(getSite().getShell(), PackagesMessages.getString("PackageExplorer.filteredDialog.title"), message)) { //$NON-NLS-1$
                filterGroup.setFilters(filters);		
                if (revealElementOrParent(element))
                    return true;
            }
        }
        FrameAction action = fActionSet.getUpAction();
        while (action.getFrameList().getCurrentIndex() > 0) {
            action.run();
            if (revealElementOrParent(element))
                return true;
        }
        return false;
    }
    
    private boolean revealElementOrParent(Object element) {
        if (this != null) { 
            if (revealAndVerify(element))
                return true;
            element= getVisibleParent(element);
            if (element != null) {
                if (revealAndVerify(element))
                    return true;
                if (element instanceof IJavaElement) {
                    IResource resource= ((IJavaElement)element).getResource();
                    if (resource != null) {
                        if (revealAndVerify(resource))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    private Object getVisibleParent(Object object) {
    	// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=19104
    	if (object == null)
    		return null;
    	if (!(object instanceof IJavaElement))
    	    return object;
    	IJavaElement element2= (IJavaElement) object;
    	switch (element2.getElementType()) {
    		case IJavaElement.IMPORT_DECLARATION:
    		case IJavaElement.PACKAGE_DECLARATION:
    		case IJavaElement.IMPORT_CONTAINER:
    		case IJavaElement.TYPE:
    		case IJavaElement.METHOD:
    		case IJavaElement.FIELD:
    		case IJavaElement.INITIALIZER:
    			// select parent cu/classfile
    			element2= (IJavaElement)element2.getOpenable();
    			break;
    		case IJavaElement.JAVA_MODEL:
    			element2= null;
    			break;
    	}
    	if (element2.getElementType() == IJavaElement.COMPILATION_UNIT) {
    		element2= JavaModelUtil.toOriginal((ICompilationUnit)element2);
    	}
    	return element2;
    }

    private boolean revealAndVerify(Object element) {
    	if (element == null)
    		return false;
    	selectReveal(new StructuredSelection(element));
    	IElementComparer comparer= getTreeViewer().getComparer();
    	Object selected= ((IStructuredSelection)getSite().getSelectionProvider().getSelection()).getFirstElement();
    	if (comparer != null ? comparer.equals(element, selected) : element.equals(selected))
    		return true;
    	return false;
    }
}
