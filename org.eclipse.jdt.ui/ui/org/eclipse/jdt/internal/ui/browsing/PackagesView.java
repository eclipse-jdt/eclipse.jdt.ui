/*
 * (c) Copyright IBM Corp. 2000, 2002. All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.browsing;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.JavaElementSorter;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.MultiActionGroup;
import org.eclipse.jdt.internal.ui.actions.SelectAllAction;
import org.eclipse.jdt.internal.ui.filters.NonJavaElementFilter;
import org.eclipse.jdt.internal.ui.viewsupport.DecoratingJavaLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;
import org.eclipse.jdt.internal.ui.viewsupport.JavaUILabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.LibraryFilter;
import org.eclipse.jdt.internal.ui.viewsupport.ProblemTableViewer;
import org.eclipse.jdt.internal.ui.viewsupport.ProblemTreeViewer;
import org.eclipse.jdt.internal.ui.viewsupport.StatusBarUpdater;
import org.eclipse.jdt.internal.ui.viewsupport.TreeHierarchyLayoutProblemsDecorator;


public class PackagesView extends JavaBrowsingPart{

	private static final String TAG_VIEW_STATE= ".viewState"; //$NON-NLS-1$
	private static final int LIST_VIEW_STATE= 0;
	private static final int TREE_VIEW_STATE= 1;


	private static class StatusBarUpdater4LogicalPackage extends StatusBarUpdater {

		private StatusBarUpdater4LogicalPackage(IStatusLineManager statusLineManager) {
			super(statusLineManager);
		}

		protected String formatMessage(ISelection sel) {
			if (sel instanceof IStructuredSelection) {
				IStructuredSelection selection= (IStructuredSelection)sel;
				int nElements= selection.size();
				Object elem= selection.getFirstElement();
				if (nElements == 1 && (elem instanceof LogicalPackage))
					return formatLogicalPackageMessage((LogicalPackage) elem);
			}
			return super.formatMessage(sel);
		}

		private String formatLogicalPackageMessage(LogicalPackage logicalPackage) {
			IPackageFragment[] fragments= logicalPackage.getFragments();
			StringBuffer buf= new StringBuffer(logicalPackage.getElementName());
			buf.append(JavaElementLabels.CONCAT_STRING);
			String message= ""; //$NON-NLS-1$
			boolean firstTime= true;
			for (int i= 0; i < fragments.length; i++) {
				IPackageFragment fragment= fragments[i];
				IJavaElement element= fragment.getParent();
				if (element instanceof IPackageFragmentRoot) {
					IPackageFragmentRoot root= (IPackageFragmentRoot) element;
					String label= JavaElementLabels.getElementLabel(root, JavaElementLabels.DEFAULT_QUALIFIED | JavaElementLabels.ROOT_QUALIFIED);
					if (firstTime) {
						buf.append(label);
						firstTime= false;
					}
					else
						message= JavaBrowsingMessages.getFormattedString("StatusBar.concat", new String[] {message, label}); //$NON-NLS-1$
				}
			}
			buf.append(message);
			return buf.toString();
		}
	};


	private SelectAllAction fSelectAllAction;
	
	private int fCurrViewState;
	
	private PackageViewerWrapper fWrappedViewer;
	
	private MultiActionGroup fSwitchActionGroup;
	private IJavaElement fLastInput;
	private boolean fLastInputWasProject;
	
	/**
	 * Adds filters the viewer of this part.
	 */
	protected void addFilters() {
		super.addFilters();
		getViewer().addFilter(createNonJavaElementFilter());
		getViewer().addFilter(new LibraryFilter());
	}

	
	/**
	 * Creates new NonJavaElementFilter and overides method select to allow for
	 * LogicalPackages.
	 * @return NonJavaElementFilter
	 */
	protected NonJavaElementFilter createNonJavaElementFilter() {
		return new NonJavaElementFilter(){
			public boolean select(Viewer viewer, Object parent, Object element){
				return ((element instanceof IJavaElement) || (element instanceof LogicalPackage));	
			}
		};
	}
	
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);

		//this must be created before all actions and filters
		fWrappedViewer= new PackageViewerWrapper();
		restoreLayoutState(memento);
	}

	private void restoreLayoutState(IMemento memento) {
		if (memento == null) {
			//read state from the preference store
			IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
			fCurrViewState= store.getInt(this.getViewSite().getId() + TAG_VIEW_STATE);
		} else {
			//restore from memento
			Integer integer= memento.getInteger(this.getViewSite().getId() + TAG_VIEW_STATE);
			if ((integer == null) || !isValidState(integer.intValue())) {
				fCurrViewState= LIST_VIEW_STATE;
			} else fCurrViewState= integer.intValue(); 
		}
	}
	
	private boolean isValidState(int state) {
		return (state==LIST_VIEW_STATE) || (state==TREE_VIEW_STATE);
	}
	


	/*
	 * @see org.eclipse.ui.IViewPart#saveState(org.eclipse.ui.IMemento)
	 */
	public void saveState(IMemento memento) {
		super.saveState(memento);
		memento.putInteger(this.getViewSite().getId()+TAG_VIEW_STATE,fCurrViewState);
	}	
	
	/**
	 * Creates the viewer of this part dependent on the current
	 * layout.
	 * 
	 * @param parent the parent for the viewer
	 */
	protected StructuredViewer createViewer(Composite parent) {
		StructuredViewer viewer;
		if(isInListState())
			viewer= createTableViewer(parent);
		else
			viewer= createTreeViewer(parent);
	
		((PackageViewerWrapper)fWrappedViewer).setViewer(viewer);
		return fWrappedViewer;
	}

	protected boolean isInListState() {
		return fCurrViewState== LIST_VIEW_STATE;
	}
	
	private ProblemTableViewer createTableViewer(Composite parent) {
		return new PackagesViewTableViewer(parent, SWT.MULTI);
	}
	
	private ProblemTreeViewer createTreeViewer(Composite parent) {
		return new PackagesViewTreeViewer(parent, SWT.MULTI);
	}
	
	/**
	 * Overrides the createContentProvider from JavaBrowsingPart
	 * Creates the the content provider of this part.
	 */
	protected IContentProvider createContentProvider() {
		if(isInListState())
			return new PackagesViewFlatContentProvider(fWrappedViewer.getViewer());
		else return new PackagesViewHierarchicalContentProvider(fWrappedViewer.getViewer());
	}

	protected JavaUILabelProvider createLabelProvider() {	
		if(isInListState())
			return createListLabelProvider();
		else return createTreeLabelProvider();
	}
	
	private JavaUILabelProvider createTreeLabelProvider() {
		PackagesViewLabelProvider lprovider= new PackagesViewLabelProvider(PackagesViewLabelProvider.HIERARCHICAL_VIEW_STATE);
		lprovider.addLabelDecorator(new TreeHierarchyLayoutProblemsDecorator(null));
		return lprovider;
	}

	private JavaUILabelProvider createListLabelProvider() {
		return new PackagesViewLabelProvider(PackagesViewLabelProvider.FLAT_VIEW_STATE);
	}
	
	private ILabelDecorator[] concat(ILabelDecorator[] d1, ILabelDecorator[] d2) {
		int d1Len= d1.length;
		int d2Len= d2.length;
		ILabelDecorator[] decorators= new ILabelDecorator[d1Len + d2Len];
		System.arraycopy(d1, 0, decorators, 0, d1Len);
		System.arraycopy(d2, 0, decorators, d1Len, d2Len); 
		return decorators;
	}

	/**
	 * Returns the context ID for the Help system
	 * 
	 * @return	the string used as ID for the Help context
	 */
	protected String getHelpContextId() {
		return IJavaHelpContextIds.PACKAGES_BROWSING_VIEW;
	}

	/**
	 * Answers if the given <code>element</code> is a valid
	 * input for this part.
	 * 
	 * @param 	element	the object to test
	 * @return	<true> if the given element is a valid input
	 */
	protected boolean isValidInput(Object element) {
		if (element instanceof IJavaProject || (element instanceof IPackageFragmentRoot && ((IJavaElement)element).getElementName() != IPackageFragmentRoot.DEFAULT_PACKAGEROOT_PATH))
			try {
				IJavaProject jProject= ((IJavaElement)element).getJavaProject();
				if (jProject != null)
					return jProject.getProject().hasNature(JavaCore.NATURE_ID);
			} catch (CoreException ex) {
				return false;
			}
		return false;
	}
	
	/**
	 * Answers if the given <code>element</code> is a valid
	 * element for this part.
	 * 
	 * @param 	element	the object to test
	 * @return	<true> if the given element is a valid element
	 */
	protected boolean isValidElement(Object element) {
		if (element instanceof IPackageFragment) {
			IJavaElement parent= ((IPackageFragment)element).getParent();
			if (parent != null)
				return super.isValidElement(parent) || super.isValidElement(parent.getJavaProject());
		}
		return false;
	}
	
	/**
	 * Finds the element which has to be selected in this part.
	 * 
	 * @param je	the Java element which has the focus
	 */
	protected IJavaElement findElementToSelect(IJavaElement je) {
		if (je == null)
			return null;

		switch (je.getElementType()) {
			case IJavaElement.PACKAGE_FRAGMENT:
				return je;
			case IJavaElement.COMPILATION_UNIT:
				return ((ICompilationUnit)je).getParent();
			case IJavaElement.CLASS_FILE:
				return ((IClassFile)je).getParent();
			case IJavaElement.TYPE:
				return ((IType)je).getPackageFragment();
			default:
				return findElementToSelect(je.getParent());
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.browsing.JavaBrowsingPart#setInput(java.lang.Object)
	 */
	protected void setInput(Object input) {
		setViewerWrapperInput(input);
		super.updateTitle();
	}
	
	private void setViewerWrapperInput(Object input) {
		fWrappedViewer.setViewerInput(input);
	}
	
	/**
	 * @see org.eclipse.jdt.internal.ui.browsing.JavaBrowsingPart#fillActionBars(org.eclipse.ui.IActionBars)
	 */
	protected void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		fSwitchActionGroup.fillActionBars(actionBars);
	}
	

	
	private void setUpViewer(StructuredViewer viewer){
		Assert.isTrue(viewer != null);

		JavaUILabelProvider labelProvider= createLabelProvider();	
		viewer.setLabelProvider(createDecoratingLabelProvider(labelProvider));
		
		viewer.setSorter(createJavaElementSorter());
		viewer.setUseHashlookup(true);
		
		createContextMenu();
		
		//disapears when control disposed
		addKeyListener();

		//this methods only adds listeners to the viewer,
		//these listenters disapear when the viewer is disposed
		hookViewerListeners();

		// Set content provider
		viewer.setContentProvider(createContentProvider());
		//Disposed when viewer's Control is disposed
		initDragAndDrop();
		
	}

	//alter sorter to include LogicalPackages
	protected JavaElementSorter createJavaElementSorter() {
		return new JavaElementSorter(){
			public int category(Object element) {
				if (element instanceof LogicalPackage) {
					LogicalPackage cp= (LogicalPackage) element;
					return super.category(cp.getFragments()[0]);
				} else return super.category(element);
			}
			public int compare(Viewer viewer, Object e1, Object e2){
				if (e1 instanceof LogicalPackage) {
					LogicalPackage cp= (LogicalPackage) e1;
					e1= cp.getFragments()[0];
				}
				if (e2 instanceof LogicalPackage) {
					LogicalPackage cp= (LogicalPackage) e2;
					e2= cp.getFragments()[0];
				}
				return super.compare(viewer, e1, e2);
			}
		};
	}
	
	protected StatusBarUpdater createStatusBarUpdater(IStatusLineManager slManager) {
		return new StatusBarUpdater4LogicalPackage(slManager);
	}
	
	protected void setSiteSelectionProvider(){
		getSite().setSelectionProvider(fWrappedViewer);
	}
	
	//do the same thing as the JavaBrowsingPart but with wrapper
	protected void createActions() {
		super.createActions();

		createSelectAllAction();
		
		//create the switch action group
		fSwitchActionGroup= createSwitchActionGroup();
	}
	
	private MultiActionGroup createSwitchActionGroup(){
		
		LayoutAction switchToFlatViewAction= new LayoutAction(JavaBrowsingMessages.getString("PackagesView.flatLayoutAction.label"),LIST_VIEW_STATE); //$NON-NLS-1$
		LayoutAction switchToHierarchicalViewAction= new LayoutAction(JavaBrowsingMessages.getString("PackagesView.HierarchicalLayoutAction.label"), TREE_VIEW_STATE); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(switchToFlatViewAction, "flatLayout.gif"); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(switchToHierarchicalViewAction, "hierarchicalLayout.gif"); //$NON-NLS-1$
			
		return new LayoutActionGroup(new IAction[]{switchToFlatViewAction,switchToHierarchicalViewAction}, fCurrViewState);
	}
	
	private static class LayoutActionGroup extends MultiActionGroup {

		LayoutActionGroup(IAction[] actions, int index) {
			super(actions, index);
		}

		public void fillActionBars(IActionBars actionBars) {
			//create new layout group
			IMenuManager manager= actionBars.getMenuManager();
			IContributionItem groupMarker= new GroupMarker("Layout"); //$NON-NLS-1$
			manager.add(groupMarker);
			IMenuManager newManager= new MenuManager(JavaBrowsingMessages.getString("PackagesView.LayoutActionGroup.layout.label")); //$NON-NLS-1$
			manager.appendToGroup("Layout", newManager); //$NON-NLS-1$
			super.addActions(newManager);
		}
	}

	
	/**
	 * Switches between flat and hierarchical state.
	 */
	private class LayoutAction extends Action {

		private int fState;
		private Runnable fRunnable;

		public LayoutAction(String text, int state) {
			super(text);
			fState= state;
		}

		public int getState() {
			return fState;
		}

		public void setRunnable(Runnable runnable) {
			Assert.isNotNull(runnable);
			fRunnable= runnable;
		}

		/*
		 * @see org.eclipse.jface.action.IAction#run()
		 */
		public void run() {
			switchViewer(fState);
		}
	}

	private void switchViewer(int state) {
		//Indicate which viewer is to be used
		if (fCurrViewState == state)
			return;
		else {
			fCurrViewState= state;
			IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
			store.setValue(getViewSite().getId() + TAG_VIEW_STATE, state);
		}

		//get the information from the existing viewer
		StructuredViewer viewer= fWrappedViewer.getViewer();
		Object object= viewer.getInput();
		ISelection selection= viewer.getSelection();

		// create and set up the new viewer
		Control control= createViewer(fWrappedViewer.getControl().getParent()).getControl();

		setUpViewer(fWrappedViewer);

		// FIXME: does not work
		createSelectAllAction();

		// add the selection information from old viewer
		fWrappedViewer.setViewerInput(object);
		fWrappedViewer.setSelection(selection, true);

		// dispose old viewer
		viewer.getContentProvider().dispose();
		viewer.getControl().dispose();

		// layout the new viewer
		if (control != null && !control.isDisposed()) {
			control.setVisible(true);
			control.getParent().layout(true);
		}
	}

	private void createSelectAllAction() {
		IActionBars actionBars= getViewSite().getActionBars();
		if (isInListState()) {
			fSelectAllAction= new SelectAllAction((TableViewer)fWrappedViewer.getViewer());
			actionBars.setGlobalActionHandler(IWorkbenchActionConstants.SELECT_ALL, fSelectAllAction);
		} else {
			actionBars.setGlobalActionHandler(IWorkbenchActionConstants.SELECT_ALL, null);
			fSelectAllAction= null;
		}
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.browsing.JavaBrowsingPart#findInputForJavaElement(org.eclipse.jdt.core.IJavaElement)
	 */
	protected IJavaElement findInputForJavaElement(IJavaElement je) {
		if (isValidInput(je)){
			fLastInputWasProject= je.getElementType() == IJavaElement.JAVA_PROJECT;
			return je;
		} else if (fLastInputWasProject && je != null) {
			return je.getJavaProject();	
		} else
			return super.findInputForJavaElement(je);
	}
	
	/**
	 * Override the getText and getImage methods for the DecoratingLabelProvider
	 * to handel the decoration of logical packages.
	 * 
	 * @see org.eclipse.jdt.internal.ui.browsing.JavaBrowsingPart#createDecoratingLabelProvider(org.eclipse.jface.viewers.ILabelDecorator)
	 */
	protected DecoratingLabelProvider createDecoratingLabelProvider(JavaUILabelProvider provider) {
		return new DecoratingJavaLabelProvider(provider) {
			
			public String getText(Object element){
				if (element instanceof LogicalPackage) {
					LogicalPackage el= (LogicalPackage) element;
					return super.getText(el.getFragments()[0]);
				} else return super.getText(element);	
			}
			
			public Image getImage(Object element) {
				if(element instanceof LogicalPackage){
					LogicalPackage el= (LogicalPackage) element;
					ILabelDecorator decorator= getLabelDecorator();
					IPackageFragment[] fragments= el.getFragments();
				
					Image image= super.getImage(el);
					for (int i= 0; i < fragments.length; i++) {
						IPackageFragment fragment= fragments[i];
						Image decoratedImage= decorator.decorateImage(image, fragment);
						if(decoratedImage != null)
							image= decoratedImage;
			}
					return image;
				} else return super.getImage(element);
			}
			
		};
	}
	
	/*
	 * Overridden from JavaBrowsingPart to handel LogicalPackages and tree
	 * structure.
	 * @see org.eclipse.jdt.internal.ui.browsing.JavaBrowsingPart#adjustInputAndSetSelection(org.eclipse.jdt.core.IJavaElement)
	 */
	void adjustInputAndSetSelection(IJavaElement je) {

		IJavaElement jElementToSelect= getSuitableJavaElement(findElementToSelect(je));
		LogicalPackagesProvider p= (LogicalPackagesProvider) fWrappedViewer.getContentProvider();

		Object elementToSelect= jElementToSelect;
		if (jElementToSelect != null && jElementToSelect.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
			IPackageFragment pkgFragment= (IPackageFragment)jElementToSelect;
			elementToSelect= p.findLogicalPackage(pkgFragment);
			if (elementToSelect == null)
				elementToSelect= pkgFragment;
		}

		IJavaElement newInput= findInputForJavaElement(je);
		if (elementToSelect == null && !isValidInput(newInput))
			setInput(null);
		// XXX: to use testFindItem needs to be reworked
		else if (elementToSelect == null || getViewer().testFindItem(elementToSelect) == null) {

			//optimization, if you are in the same project but expansion hasn't happened
			Object input= getViewer().getInput();
			if (elementToSelect != null && newInput != null) {
				if (newInput.equals(input)) {
					getViewer().reveal(elementToSelect);
				// Adjust input to selection
				} else {
					setInput(newInput);
					getViewer().reveal(elementToSelect);
				}
			} else
				setInput(newInput);

			// Recompute suitable element since it depends on the viewer's input
			jElementToSelect= getSuitableJavaElement(elementToSelect);
			if (jElementToSelect != null && jElementToSelect.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
				IPackageFragment pkgFragment= (IPackageFragment)jElementToSelect;
				elementToSelect= p.findLogicalPackage(pkgFragment);
				if (elementToSelect == null)
					elementToSelect= pkgFragment;
			}
		}

		ISelection selection;
		if (elementToSelect != null)
			selection= new StructuredSelection(elementToSelect);
		else
			selection= StructuredSelection.EMPTY;
		setSelection(selection, true);
	}

}
