/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.browsing;

import java.util.Collection;
import java.util.Comparator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.NewWizardMenu;
import org.eclipse.ui.actions.OpenPerspectiveMenu;
import org.eclipse.ui.actions.OpenWithMenu;
import org.eclipse.ui.actions.RefreshAction;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.internal.framelist.BackAction;
import org.eclipse.ui.views.internal.framelist.ForwardAction;
import org.eclipse.ui.views.internal.framelist.GoIntoAction;
import org.eclipse.ui.views.internal.framelist.UpAction;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.IWorkingCopy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementSorter;

import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GenerateGroup;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jdt.internal.ui.javaeditor.JarEntryEditorInput;
import org.eclipse.jdt.internal.ui.packageview.BuildGroup;
import org.eclipse.jdt.internal.ui.packageview.OpenResourceAction;
import org.eclipse.jdt.internal.ui.packageview.PackagesMessages;
import org.eclipse.jdt.internal.ui.packageview.ShowInNavigatorAction;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringAction;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringGroup;
import org.eclipse.jdt.internal.ui.reorg.DeleteAction;
import org.eclipse.jdt.internal.ui.reorg.ReorgGroup;
import org.eclipse.jdt.internal.ui.search.JavaSearchGroup;
import org.eclipse.jdt.internal.ui.util.OpenTypeHierarchyUtil;
import org.eclipse.jdt.internal.ui.viewsupport.BaseJavaElementContentProvider;
import org.eclipse.jdt.internal.ui.viewsupport.IProblemChangedListener;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.ProblemTableViewer;
import org.eclipse.jdt.internal.ui.viewsupport.StandardJavaUILabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.StatusBarUpdater;



abstract class JavaBrowsingPart extends ViewPart implements IMenuListener, ISelectionListener {

	private ILabelProvider fLabelProvider;
	private ILabelProvider fTitleProvider;
	private StructuredViewer fViewer;
	private IMemento fMemento;
	private JavaElementTypeComparator fTypeComparator;
	
	// Actions
	private ContextMenuGroup[] fStandardGroups;
	private Menu fContextMenu;		
	private OpenResourceAction fOpenCUAction;
	private Action fOpenToAction;
	private Action fShowNavigatorAction;
	private PropertyDialogAction fPropertyDialogAction;
 	private RefactoringAction fDeleteAction;
 	private RefreshAction fRefreshAction;
 	private BackAction fBackAction;
	private ForwardAction fForwardAction;
	private GoIntoAction fZoomInAction;
	private UpAction fUpAction;
//	private GotoTypeAction fGotoTypeAction;
//	private GotoPackageAction fGotoPackageAction;
	private IWorkbenchPart fPreviousSelectionProvider;
	private Image fOriginalTitleImage;
		
	/*
	 * Ensure selection changed events being processed only if
	 * initiated by user interaction with this part.
	 */
	private boolean fProcessSelectionEvents= true;
	
	private IPartListener fPartListener= new IPartListener() {
		public void partActivated(IWorkbenchPart part) {
			setSelectionFromEditor(part);
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
	
	/*
	 * Implements method from IViewPart.
	 */
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		fMemento= memento;
	}

	/*
	 * Implements method from IViewPart.
	 */
	public void saveState(IMemento memento) {
		if (fViewer == null) {
			// part has not been created
			if (fMemento != null) //Keep the old state;
				memento.putMemento(fMemento);
			return;
		}
		// XXX: need to save state here
		// fViewer.saveState(memento);
	}	

	/**
	 * Creates the search list inner viewer.
	 */
	public void createPartControl(Composite parent) {
		Assert.isTrue(fViewer == null);
		if (fMemento != null)
			// XXX: Restore state here
			// fViewer.restoreState(fMemento);
		fMemento= null;

		fTypeComparator= new JavaElementTypeComparator();

		// Setup viewer
		fViewer= createViewer(parent);

		fLabelProvider= createLabelProvider();
		ILabelDecorator decorationMgr= getViewSite().getDecoratorManager();
		fViewer.setLabelProvider(new DecoratingLabelProvider(fLabelProvider, decorationMgr));
		
		fViewer.setSorter(new JavaElementSorter());
		fViewer.setUseHashlookup(true);
		JavaPlugin.getDefault().getProblemMarkerManager().addListener((IProblemChangedListener)fViewer);
		fTitleProvider= createTitleProvider();
		
		MenuManager menuMgr= new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(this);
		fContextMenu= menuMgr.createContextMenu(fViewer.getControl());
		fViewer.getControl().setMenu(fContextMenu);
		getSite().registerContextMenu(menuMgr, fViewer);

		createActions(); // call before registering for selection changes
		addKeyListener();		

		getSite().setSelectionProvider(fViewer);
		
		// Status line
		IStatusLineManager slManager= getViewSite().getActionBars().getStatusLineManager();
		fViewer.addSelectionChangedListener(new StatusBarUpdater(slManager));
	
		
		hookViewerListeners();

		// Initialize viewer input
		fViewer.setContentProvider(createContentProvider());
		setInitialInput();
		
		// Initialize selecton
		setInitialSelection();
		
		// Filters
		addFilters();

		fillToolBar(getViewSite().getActionBars().getToolBarManager());	

		// Listen to workbench window changes
		getViewSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
		getViewSite().getPage().addPartListener(fPartListener);
		
		setHelp();
	}
	
	//---- IWorkbenchPart ------------------------------------------------------


	public void setFocus() {
		fViewer.getControl().setFocus();
	}
	
	public void dispose() {
		if (fViewer != null) {
			JavaPlugin.getDefault().getProblemMarkerManager().removeListener((IProblemChangedListener)fViewer);
			getViewSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
			getViewSite().getPage().removePartListener(fPartListener);
			fViewer= null;
		}
		super.dispose();
	}
	
	/**
	 * Adds the KeyListener
	 */
	protected void addKeyListener() {
		fViewer.getControl().addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent event) {
				handleKeyReleased(event);
			}
		});
	}

	protected void handleKeyReleased(KeyEvent event) {
		if (event.stateMask != 0) 
			return;		
		
		int key= event.keyCode;
		if (key == SWT.F5) {
			fRefreshAction.selectionChanged(
				(IStructuredSelection) fViewer.getSelection());
			if (fRefreshAction.isEnabled())
				fRefreshAction.run();
		} else if (key == SWT.F4) {
			OpenTypeHierarchyUtil.open(getSelectionProvider().getSelection(), getSite().getWorkbenchWindow());
		} else if (key == SWT.F3) {
			fOpenCUAction.update();
			if (fOpenCUAction.isEnabled())
				fOpenCUAction.run();
		}
		else if (event.character == SWT.DEL) {
			fDeleteAction.update();
			if (fDeleteAction.isEnabled())
				fDeleteAction.run();
		}
	}
	
	//---- Adding Action to Toolbar -------------------------------------------
	
	protected void fillToolBar(IToolBarManager tbm) {
//		fViewer.fillToolBar(tbm);
	}	

//	protected void setContextMenuContributor(final IContextMenuContributor contributor) {
//		// Make sure we are doing it in the right thread.
//		getDisplay().syncExec(new Runnable() {
//			public void run() {
////				getViewer().setContextMenuTarget(contributor);
//			}
//		});
//	}

	/**
	 * Called when the context menu is about to open.
	 * Override to add your own context dependent menu contributions.
	 */
	public void menuAboutToShow(IMenuManager menu) {
		JavaPlugin.createStandardGroups(menu);
		IStructuredSelection selection= (IStructuredSelection) fViewer.getSelection();
		
		fPropertyDialogAction.selectionChanged(selection);

		MenuManager newMenu= new MenuManager(PackagesMessages.getString("PackageExplorer.new")); //$NON-NLS-1$
		menu.appendToGroup(IContextMenuConstants.GROUP_NEW, newMenu);
		new NewWizardMenu(newMenu, getSite().getWorkbenchWindow(), false);

		// updateActions(selection);
//		Object element= selection.getFirstElement();
//		if (selection.size() == 1 && fViewer.isExpandable(element)) 
//			menu.appendToGroup(IContextMenuConstants.GROUP_GOTO, fZoomInAction);
//		addGotoMenu(menu);
//

		// Open menus
		fOpenCUAction.update();
		if (fOpenCUAction.isEnabled())
			menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fOpenCUAction);
		addOpenWithMenu(menu, selection);
		addOpenToMenu(menu, selection);

		addRefactoring(menu);

		ContextMenuGroup.add(menu, fStandardGroups, fViewer);
		
		menu.appendToGroup(IContextMenuConstants.GROUP_BUILD, fRefreshAction);
		fRefreshAction.selectionChanged(selection);

		menu.add(new Separator());
		if (fPropertyDialogAction.isApplicableForSelection())
			menu.appendToGroup(IContextMenuConstants.GROUP_PROPERTIES, fPropertyDialogAction);	
	}

	private void addRefactoring(IMenuManager menu){
		MenuManager refactoring= new MenuManager(PackagesMessages.getString("PackageExplorer.refactoringTitle"));  //$NON-NLS-1$
		ContextMenuGroup.add(refactoring, new ContextMenuGroup[] { new RefactoringGroup() }, fViewer);
		if (!refactoring.isEmpty())
			menu.appendToGroup(IContextMenuConstants.GROUP_REORGANIZE, refactoring);
	}

	private void createActions() {
		ISelectionProvider provider= getSelectionProvider();
		fOpenCUAction= new OpenResourceAction(provider);
		fPropertyDialogAction= new PropertyDialogAction(getShell(), provider);
		fShowNavigatorAction= new ShowInNavigatorAction(provider);
		
		fStandardGroups= new ContextMenuGroup[] {
			new BuildGroup(),
			new ReorgGroup(),
			new GenerateGroup(),
			new JavaSearchGroup()
		};
		
		fDeleteAction= new DeleteAction(provider);
		fRefreshAction= new RefreshAction(getShell());
//		fFilterAction = new FilterSelectionAction(getShell(), this, PackagesMessages.getString("PackageExplorer.filters")); //$NON-NLS-1$
//		fShowLibrariesAction = new ShowLibrariesAction(this, PackagesMessages.getString("PackageExplorer.referencedLibs")); //$NON-NLS-1$
//		fShowBinariesAction = new ShowBinariesAction(getShell(), this, PackagesMessages.getString("PackageExplorer.binaryProjects")); //$NON-NLS-1$
//		fFilterWorkingSetAction = new FilterWorkingSetAction(getShell(), this, "Filter Working Set..."); //$NON-NLS-1$
//		fRemoveWorkingSetAction = new RemoveWorkingSetFilterAction(getShell(), this, "Remove Working Set Filter"); //$NON-NLS-1$
		
//		fBackAction= new BackAction(fFrameList);
//		fForwardAction= new ForwardAction(fFrameList);
//		fZoomInAction= new GoIntoAction(fFrameList);
//		fUpAction= new UpAction(fFrameList);

//		fGotoTypeAction= new GotoTypeAction(this);
//		fGotoPackageAction= new GotoPackageAction(this);
		IActionBars actionService= getViewSite().getActionBars();
		actionService.setGlobalActionHandler(IWorkbenchActionConstants.DELETE, fDeleteAction);
		ReorgGroup.addGlobalReorgActions(actionService, provider);
	}

	private void addOpenToMenu(IMenuManager menu, IStructuredSelection selection) {
		if (selection.size() != 1)
			return;
		IAdaptable element= (IAdaptable) selection.getFirstElement();
		IResource resource= (IResource)element.getAdapter(IResource.class);

		if ((resource instanceof IContainer)) {			
			// Create a menu flyout.
			MenuManager submenu = new MenuManager(PackagesMessages.getString("PackageExplorer.openPerspective")); //$NON-NLS-1$
			submenu.add(new OpenPerspectiveMenu(getSite().getWorkbenchWindow(), resource));
			menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, submenu);
		}
		OpenTypeHierarchyUtil.addToMenu(getSite().getWorkbenchWindow(), menu, element);
	}
	
	private void addOpenWithMenu(IMenuManager menu, IStructuredSelection selection) {
		// If one file is selected get it.
		// Otherwise, do not show the "open with" menu.
		if (selection.size() != 1)
			return;

		IAdaptable element= (IAdaptable)selection.getFirstElement();
		Object resource= element.getAdapter(IResource.class);
		if (!(resource instanceof IFile))
			return; 

		// Create a menu flyout.
		MenuManager submenu= new MenuManager(PackagesMessages.getString("PackageExplorer.openWith")); //$NON-NLS-1$
		submenu.add(new OpenWithMenu(getSite().getPage(), (IFile) resource));

		// Add the submenu.
		menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, submenu);

	}

	/**
	 * Returns the shell to use for opening dialogs.
	 * Used in this class, and in the actions.
	 */
	private Shell getShell() {
		return fViewer.getControl().getShell();
	}

	protected final Display getDisplay() {
		return fViewer.getControl().getDisplay();
	}	

	/**
	 * Returns the selection provider.
	 */
	private ISelectionProvider getSelectionProvider() {
		return fViewer;
	}

	/**
	 * Answers if the given <code>element</code> is a valid
	 * input for this part.
	 * 
	 * @param 	element	the object to test
	 * @return	<true> if the given element is a valid input
	 */
	abstract protected boolean isValidInput(Object element);

	/**
	 * Answers if the given <code>element</code> is a valid
	 * element for this part.
	 * 
	 * @param 	element	the object to test
	 * @return	<true> if the given element is a valid element
	 */
	protected boolean isValidElement(Object element) {
		if (element == null)
			return false;
		element= getSuitableJavaElement(element);
		if (element == null)
			return false;
		Object input= getViewer().getInput();
		if (input == null)
			return false;
		if (input instanceof Collection)
			return ((Collection)input).contains(element);
		else
			return input.equals(element);

	}

	private boolean isInputResetBy(Object newInput, Object input, IWorkbenchPart part) {
		if (newInput == null)
			return part == fPreviousSelectionProvider;
			
		if (input instanceof IJavaElement && newInput instanceof IJavaElement)
			return getTypeComparator().compare(newInput, input)  > 0;
		else
			return false;
	}

	protected boolean isAncestorOf(Object ancestor, Object element) {
		if (element instanceof IJavaElement && ancestor instanceof IJavaElement)
			return !element.equals(ancestor) && internalIsAncestorOf((IJavaElement)ancestor, (IJavaElement)element);
		return false;
	}
	
	private boolean internalIsAncestorOf(IJavaElement ancestor, IJavaElement element) {
		if (element != null)
			return element.equals(ancestor) || internalIsAncestorOf(ancestor, element.getParent());
		else
			return false;
	}

	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		
		if (!fProcessSelectionEvents || part == this || !(selection instanceof IStructuredSelection))
			return;
			
		if (selection.isEmpty() && part instanceof JavaBrowsingPart && !(part instanceof ProjectsView))
			return;
		
		// Set input
		Object newInput= getElementFromSingleSelection(selection);
		Object currentInput= (IJavaElement)getViewer().getInput();
		if (newInput != null && newInput.equals(currentInput)) {
			IJavaElement elementToSelect= findElementToSelect(getElementFromSingleSelection(selection));
			if (elementToSelect != null && getTypeComparator().compare(newInput, elementToSelect) < 0)
				setSelection(new StructuredSelection(elementToSelect), true);
			fPreviousSelectionProvider= part;
			return;
		}

		// Clear input if needed
		if (part != fPreviousSelectionProvider && newInput != null && !newInput.equals(currentInput) && isInputResetBy(newInput, currentInput, part)) {
			if (!isAncestorOf(newInput, currentInput))
				setInput(null);
			fPreviousSelectionProvider= part;
			return;
		} else if (newInput == null && part == fPreviousSelectionProvider) {
			setInput(null);
			fPreviousSelectionProvider= part;
			return;
		}
		fPreviousSelectionProvider= part;
		
		// Set selection
		if (newInput instanceof IJavaElement)
			adjustInputAndSetSelection((IJavaElement)newInput);
		else
			setSelection(StructuredSelection.EMPTY, true);
	}


	protected void setInput(Object input) {
		if (input == null)
			setTitleImage(fOriginalTitleImage);
		else if (input instanceof Collection) {
			if (((Collection)input).isEmpty())
				setTitleImage(fOriginalTitleImage);
			else {
				Object firstElement= ((Collection)input).iterator().next();
				setTitleImage(fTitleProvider.getImage(firstElement));
			}
		} else
			setTitleImage(fTitleProvider.getImage(input));
		setViewerInput(input);
	}

	private void setViewerInput(Object input) {
		fProcessSelectionEvents= false;
		fViewer.setInput(input);
		fProcessSelectionEvents= true;
	}

	/**
	 * Sets or clears the title image of this part and
	 * store the orignal image on the first call.
	 */
	protected void setTitleImage(Image titleImage) {
		if (fOriginalTitleImage == null)
			fOriginalTitleImage= getTitleImage();
		if (titleImage == null)
			titleImage= fOriginalTitleImage;
		super.setTitleImage(titleImage);
	}

	protected final StructuredViewer getViewer() {
		return fViewer;
	}

	protected ILabelProvider createLabelProvider() {
		return new StandardJavaUILabelProvider(
						StandardJavaUILabelProvider.DEFAULT_TEXTFLAGS,
						StandardJavaUILabelProvider.DEFAULT_IMAGEFLAGS | JavaElementImageProvider.SMALL_ICONS,
						StandardJavaUILabelProvider.getAdornmentProviders(true, null)
						);
	}

	protected ILabelProvider createTitleProvider() {
		return new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_BASICS | JavaElementLabelProvider.SHOW_SMALL_ICONS);
	}

	protected final ILabelProvider getLabelProvider() {
		return fLabelProvider;
	}

	protected final ILabelProvider getTitleProvider() {
		return fTitleProvider;
	}

	/**
	 * Creates the the viewer of this part.
	 * 
	 * @param parent	the parent for the viewer
	 */
	protected StructuredViewer createViewer(Composite parent) {
		return new ProblemTableViewer(parent, SWT.SINGLE);
	}
	
	protected int getLabelProviderFlags() {
		return JavaElementLabelProvider.SHOW_BASICS | JavaElementLabelProvider.SHOW_OVERLAY_ICONS |
				JavaElementLabelProvider.SHOW_SMALL_ICONS | JavaElementLabelProvider.SHOW_VARIABLE | JavaElementLabelProvider.SHOW_PARAMETERS;
	}

	/**
	 * Adds filters the viewer of this part.
	 */
	protected void addFilters() {
		// default is to have no filters
	}

	/**
	 * Creates the the content provider of this part.
	 */
	protected BaseJavaElementContentProvider createContentProvider() {
		return new JavaElementContentProvider(true, this); //
	}

	protected void setInitialInput() {
		// Use the selection, if any
		ISelection selection= getSite().getPage().getSelection();
		Object input= getElementFromSingleSelection(selection);
		if (!(input instanceof IJavaElement)) {
			// Use the input of the page
			input= getSite().getPage().getInput();
			if (!(input instanceof IJavaElement) && input instanceof IAdaptable)
				input= ((IAdaptable)input).getAdapter(IJavaElement.class);
		}
		setInput(findInputForJavaElement((JavaElement)input));		
	}

	protected void setInitialSelection() {
		// Use the selection, if any
		Object input;
		ISelection selection= getSite().getPage().getSelection();
		if (selection != null && !selection.isEmpty())
			input= getElementFromSingleSelection(selection);
		else {
			// Use the input of the page
			input= getSite().getPage().getInput();
			if (!(input instanceof IJavaElement) && input instanceof IAdaptable)
				input= ((IAdaptable)input).getAdapter(IJavaElement.class);
			
			else
				return;
		}
		if (findElementToSelect((IJavaElement)input) != null)
			adjustInputAndSetSelection((IJavaElement)input);
	}

	final protected void setHelp() {
		WorkbenchHelp.setHelp(fViewer.getControl(), getHelpContextId());
	}

	/**
	 * Returns the context ID for the Help system
	 * 
	 * @return	the string used as ID for the Help context
	 */
	abstract protected String getHelpContextId();

	/**
	 * Adds additional listeners to this view.
	 */
	protected void hookViewerListeners() {
		fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (!fProcessSelectionEvents)
					return;

				if (JavaBrowsingPreferencePage.openEditorOnSingleClick())
					new ShowInEditorAction().run(event.getSelection(), getSite().getPage());
				else
					linkToEditor((IStructuredSelection)event.getSelection());
			}
		});

		fViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				if (fProcessSelectionEvents && !JavaBrowsingPreferencePage.openEditorOnSingleClick())
					new ShowInEditorAction().run(event.getSelection(), getSite().getPage());
			}
		});
	}

	void adjustInputAndSetSelection(IJavaElement je) {
		je= getSuitableJavaElement(je);
		IJavaElement elementToSelect= findElementToSelect(je);
		IJavaElement newInput= findInputForJavaElement(je);
		if (elementToSelect == null && !isValidInput(newInput))
			// Clear input
			setInput(null);
		else if (elementToSelect == null || getViewer().testFindItem(elementToSelect) == null)
			// Adjust input to selection
			setInput(findInputForJavaElement(je));
		
		if (elementToSelect != null)
			setSelection(new StructuredSelection(elementToSelect), true);
		else
			setSelection(StructuredSelection.EMPTY, true);
	}

	/**
	 * Finds the closest Java element which can be used as input for
	 * this part and has the given Java element as child
	 * 
	 * @param 	je 	the Java element for which to search the closest input
	 * @return	the closest Java element used as input for this part
	 */
	protected IJavaElement findInputForJavaElement(IJavaElement je) {
		if (je == null)
			return null;
		if (isValidInput(je))
			return je;
		return findInputForJavaElement(je.getParent());
	}
	
	final protected IJavaElement findElementToSelect(Object obj) {
		if (obj instanceof IJavaElement)
			return findElementToSelect((IJavaElement)obj);
		return null;
	}
	
	/**
	 * Finds the element which has to be selected in this part.
	 * 
	 * @param je	the Java element which has the focus
	 */
	abstract protected IJavaElement findElementToSelect(IJavaElement je);
	

	private Object getElementFromSingleSelection(ISelection selection) {
		if (selection instanceof StructuredSelection
			&& ((StructuredSelection)selection).size() == 1)
				return ((StructuredSelection)selection).getFirstElement();

		return null;
	}

	/**
	 * Gets the typeComparator.
	 * @return Returns a JavaElementTypeComparator
	 */
	protected Comparator getTypeComparator() {
		return fTypeComparator;
	}

	/**
	 * Links to editor (if option enabled)
	 */
	private void linkToEditor(IStructuredSelection selection) {
		if (selection == null || selection.isEmpty())
			return;

		Object obj= selection.getFirstElement();
		Object element= null;

		if (selection.size() == 1) {
			if (obj instanceof IJavaElement) {
				IJavaElement cu= JavaModelUtil.findElementOfKind((IJavaElement)obj, IJavaElement.COMPILATION_UNIT);
				if (cu != null)
					element= getResourceFor(cu);
				if (element == null)
					element= JavaModelUtil.findElementOfKind((IJavaElement)obj, IJavaElement.CLASS_FILE);
			}
			else if (obj instanceof IFile)
				element= obj;
				
			if (element == null)
				return;

			IWorkbenchPage page= getSite().getPage();
			IEditorPart editorArray[]= page.getEditors();
			for (int i= 0; i < editorArray.length; ++i) {
				IEditorPart editor= editorArray[i];
				Object input= getElementOfInput(editor.getEditorInput());					
				if (input != null && input.equals(element)) {
					page.bringToTop(editor);
					if (obj instanceof IJavaElement) 
						EditorUtility.revealInEditor(editor, (IJavaElement) obj);
					return;
				}
			}
		}
	}

	private void setSelectionFromEditor(IWorkbenchPart part) {
		if (part == null)
			return;
		IWorkbenchPartSite site= part.getSite();
		if (site == null)
			return;
		ISelectionProvider provider= site.getSelectionProvider();
		if (provider != null)
			setSelectionFromEditor(part, provider.getSelection());
	}

	private void setSelectionFromEditor(IWorkbenchPart part, ISelection selection) {
		if (part instanceof IEditorPart && JavaBrowsingPreferencePage.linkViewSelectionToEditor()) {
			IEditorInput ei= ((IEditorPart)part).getEditorInput();
			if (selection instanceof ITextSelection) {
				int offset= ((ITextSelection)selection).getOffset();
				IJavaElement element= getElementForInputAt(ei, offset);
				if (element != null) {
					adjustInputAndSetSelection(element);
					return;
				}
			}
			if (ei instanceof IFileEditorInput) {
				IFile file= ((IFileEditorInput)ei).getFile();
				IJavaElement je= (IJavaElement)file.getAdapter(IJavaElement.class);
				if (je == null) {
					setSelection(null, false);
					return;
				}
				adjustInputAndSetSelection(je);

			} else if (ei instanceof IClassFileEditorInput) {
				IClassFile cf= ((IClassFileEditorInput)ei).getClassFile();
				adjustInputAndSetSelection(cf);
			}
			return;
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
	
	private IResource getResourceFor(Object element) {
		if (element instanceof IJavaElement) {
			if (element instanceof IWorkingCopy) {
				IWorkingCopy wc= (IWorkingCopy)element;
				IJavaElement original= wc.getOriginalElement();
				if (original != null)
					element= original;
			}
			try {
				element= ((IJavaElement)element).getUnderlyingResource();
			} catch (JavaModelException e) {
				return null;
			}
		}
		if (!(element instanceof IResource) || ((IResource)element).isPhantom()) {
			return null;
		}
		return (IResource)element;
	}

	private void setSelection(ISelection selection, boolean reveal) {
		if (selection != null && selection.equals(fViewer.getSelection()))
			return;
		fProcessSelectionEvents= false;
		fViewer.setSelection(selection, reveal);
		fProcessSelectionEvents= true;
	}

	/**
	 * Tries to find the given element in a workingcopy.
	 */
	protected static IJavaElement getWorkingCopy(IJavaElement input) {
		try {
			if (input instanceof ICompilationUnit)
				return EditorUtility.getWorkingCopy((ICompilationUnit)input);
			else
				return EditorUtility.getWorkingCopy(input, true);
		} catch (JavaModelException ex) {
		}
		return null;
	}

	/**
	 * Returns the original element from which the specified working copy
	 * element was created from. This is a handle only method, the
	 * returned element may or may not exist.
	 * 
	 * @param	workingCopy the element for which to get the original
	 * @return the original Java element or <code>null</code> if this is not a working copy element
	 */
	protected static IJavaElement getOriginal(IJavaElement workingCopy) {
		ICompilationUnit cu= getCompilationUnit(workingCopy);
		if (cu != null)
			return ((IWorkingCopy)cu).getOriginal(workingCopy);
		return null;
	}

	/**
	 * Returns the compilation unit for the given java element.
	 * 
	 * @param	element the java element whose compilation unit is searched for
	 * @return	the compilation unit of the given java element
	 */
	protected static ICompilationUnit getCompilationUnit(IJavaElement element) {
		if (element == null)
			return null;
			
		if (element instanceof IMember)
			return ((IMember) element).getCompilationUnit();
		
		int type= element.getElementType();
		if (IJavaElement.COMPILATION_UNIT == type)
			return (ICompilationUnit) element;
		if (IJavaElement.CLASS_FILE == type)
			return null;
			
		return getCompilationUnit(element.getParent());
	}

	/**
	 * Converts the given Java element to one which is suitable for this
	 * view. It takes into account wether the view shows working copies or not.
	 *
	 * @param	element the Java element to be converted
	 * @return	an element suitable for this view
	 */
	protected IJavaElement getSuitableJavaElement(Object obj) {
		if (!(obj instanceof IJavaElement))
			return null;
		IJavaElement element= (IJavaElement)obj;
		if (fTypeComparator.compare(element, IJavaElement.COMPILATION_UNIT) > 0)
			return element;
		if (element.getElementType() == IJavaElement.CLASS_FILE)
			return element;
		if (((BaseJavaElementContentProvider)getViewer().getContentProvider()).getProvideWorkingCopy()) {
			IJavaElement wc= getWorkingCopy(element);
			if (wc != null)
				element= wc;
			return element;
		}
		else {
			ICompilationUnit cu= getCompilationUnit(element);
			if (cu != null && ((IWorkingCopy)cu).isWorkingCopy())
				return ((IWorkingCopy)cu).getOriginal(element);
			else
				return element;
		}
	}

	/**
	 * @see JavaEditor#getElementAt(int)
	 */
	protected IJavaElement getElementForInputAt(IEditorInput input, int offset) {
		if (input instanceof IClassFileEditorInput) {
			try {
				return ((IClassFileEditorInput)input).getClassFile().getElementAt(offset);
			} catch (JavaModelException ex) {
				return null;
			}
		}

		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
		ICompilationUnit unit= manager.getWorkingCopy(input);
		if (unit != null)
			synchronized (unit) {
				try {
					unit.reconcile(null);
					return unit.getElementAt(offset);
				} catch (JavaModelException x) {
				}
			}
		return null;
	}
	
	protected IType getTypeForCU(ICompilationUnit cu) {
		cu= (ICompilationUnit)getSuitableJavaElement(cu);
		
		// Use primary type if possible
		IType primaryType= JavaModelUtil.findPrimaryType(cu);
		if (primaryType != null)
			return primaryType;

		// Use first top-level type
		try {
			IType[] types= cu.getTypes();
			if (types.length > 0)
				return types[0];
			else
				return null;
		} catch (JavaModelException ex) {
			return null;
		}
	}	
}
