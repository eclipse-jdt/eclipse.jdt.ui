/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.ToolBar;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IInputSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.OpenWithMenu;
import org.eclipse.ui.help.ViewContextComputer;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.ITypeHierarchyViewPart;
import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.AddMethodStubAction;
import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.compare.JavaReplaceWithEditionAction;
import org.eclipse.jdt.internal.ui.dnd.BasicSelectionTransferDragAdapter;
import org.eclipse.jdt.internal.ui.dnd.LocalSelectionTransfer;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.packageview.BuildGroup;
import org.eclipse.jdt.internal.ui.preferences.JavaBasePreferencePage;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringGroup;
import org.eclipse.jdt.internal.ui.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.util.OpenTypeHierarchyHelper;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;
import org.eclipse.jdt.internal.ui.viewsupport.IProblemChangedListener;
import org.eclipse.jdt.internal.ui.viewsupport.MarkerErrorTickProvider;
import org.eclipse.jdt.internal.ui.viewsupport.SelectionProviderMediator;
import org.eclipse.jdt.internal.ui.viewsupport.StatusBarUpdater;

/**
 * view showing the supertypes/subtypes of its input.
 */
public class TypeHierarchyViewPart extends ViewPart implements ITypeHierarchyViewPart {
	
	public static final int VIEW_ID_TYPE= 2;
	public static final int VIEW_ID_SUPER= 0;
	public static final int VIEW_ID_SUB= 1;
	
	private static final String DIALOGSTORE_HIERARCHYVIEW= "TypeHierarchyViewPart.hierarchyview";	 //$NON-NLS-1$
	private static final String DIALOGSTORE_VIEWORIENTATION= "TypeHierarchyViewPart.orientation";	 //$NON-NLS-1$


	private static final String TAG_INPUT= "input"; //$NON-NLS-1$
	private static final String TAG_VIEW= "view"; //$NON-NLS-1$
	private static final String TAG_ORIENTATION= "orientation"; //$NON-NLS-1$
	private static final String TAG_RATIO= "ratio"; //$NON-NLS-1$
	private static final String TAG_SELECTION= "selection"; //$NON-NLS-1$
	private static final String TAG_VERTICAL_SCROLL= "vertical_scroll"; //$NON-NLS-1$

	private IType fInput;
	
	private IMemento fMemento;
	
	private ArrayList fInputHistory;
	private int fCurrHistoryIndex;
	
	private IProblemChangedListener fHierarchyProblemListener;
	
	private TypeHierarchyLifeCycle fHierarchyLifeCycle;
	private ITypeHierarchyLifeCycleListener fTypeHierarchyLifeCycleListener;
		
	private MethodsViewer fMethodsViewer;
			
	private int fCurrentViewerIndex;
	private TypeHierarchyViewer[] fAllViewers;
	
	private boolean fIsEnableMemberFilter;
	
	private SashForm fTypeMethodsSplitter;
	private PageBook fViewerbook;
	private PageBook fPagebook;
	private Label fNoHierarchyShownLabel;
	private Label fEmptyTypesViewer;
	
	private CLabel fMethodViewerPaneLabel;
	private JavaElementLabelProvider fPaneLabelProvider;
	
	private IDialogSettings fDialogSettings;
	
	private ToggleViewAction[] fViewActions;
	
	private HistoryAction fForwardAction;
	private HistoryAction fBackwardAction;
	
	private ToggleOrientationAction fToggleOrientationAction;
	
	private EnableMemberFilterAction fEnableMemberFilterAction;
	private AddMethodStubAction fAddStubAction;
	private FocusOnTypeAction fFocusOnTypeAction;
	private FocusOnSelectionAction fFocusOnSelectionAction;
	
	private IPartListener fPartListener;
	
	public TypeHierarchyViewPart() {
		
		fHierarchyLifeCycle= new TypeHierarchyLifeCycle();
		fTypeHierarchyLifeCycleListener= new ITypeHierarchyLifeCycleListener() {
			public void typeHierarchyChanged(TypeHierarchyLifeCycle typeHierarchy) {
				doTypeHierarchyChanged(typeHierarchy);
			}
		};
		fHierarchyLifeCycle.addChangedListener(fTypeHierarchyLifeCycleListener);
		
		fHierarchyProblemListener= null;
		
		fIsEnableMemberFilter= false;
		
		fInputHistory= new ArrayList();
		fCurrHistoryIndex= -1;
		
		String title= TypeHierarchyMessages.getString("TypeHierarchyViewPart.toggleaction.supertypes.label"); //$NON-NLS-1$
		ToggleViewAction superViewAction= new ToggleViewAction(this, VIEW_ID_SUPER, title, true);
		superViewAction.setDescription(TypeHierarchyMessages.getString("TypeHierarchyViewPart.toggleaction.supertypes.description")); //$NON-NLS-1$
		superViewAction.setToolTipText(TypeHierarchyMessages.getString("TypeHierarchyViewPart.toggleaction.supertypes.tooltip")); //$NON-NLS-1$
		JavaPluginImages.setImageDescriptors(superViewAction, "lcl16", "super_co.gif"); //$NON-NLS-2$ //$NON-NLS-1$

		title= TypeHierarchyMessages.getString("TypeHierarchyViewPart.toggleaction.subtypes.label"); //$NON-NLS-1$
		ToggleViewAction subViewAction= new ToggleViewAction(this, VIEW_ID_SUB, title, false);
		subViewAction.setDescription(TypeHierarchyMessages.getString("TypeHierarchyViewPart.toggleaction.subtypes.description")); //$NON-NLS-1$
		subViewAction.setToolTipText(TypeHierarchyMessages.getString("TypeHierarchyViewPart.toggleaction.subtypes.tooltip")); //$NON-NLS-1$
		JavaPluginImages.setImageDescriptors(subViewAction, "lcl16", "sub_co.gif"); //$NON-NLS-2$ //$NON-NLS-1$

		title= TypeHierarchyMessages.getString("TypeHierarchyViewPart.toggleaction.vajhierarchy.label"); //$NON-NLS-1$
		ToggleViewAction vajViewAction= new ToggleViewAction(this, VIEW_ID_TYPE, title, false);
		vajViewAction.setDescription(TypeHierarchyMessages.getString("TypeHierarchyViewPart.toggleaction.vajhierarchy.description")); //$NON-NLS-1$
		vajViewAction.setToolTipText(TypeHierarchyMessages.getString("TypeHierarchyViewPart.toggleaction.vajhierarchy.tooltip")); //$NON-NLS-1$
		JavaPluginImages.setImageDescriptors(vajViewAction, "lcl16", "hierarchy_co.gif"); //$NON-NLS-1$ //$NON-NLS-2$
		
		fViewActions= new ToggleViewAction[] { vajViewAction, superViewAction, subViewAction };
		
		fDialogSettings= JavaPlugin.getDefault().getDialogSettings();
		
		fForwardAction= new HistoryAction(this, true);
		fBackwardAction= new HistoryAction(this, false);
		
		fToggleOrientationAction= new ToggleOrientationAction(this, fDialogSettings.getBoolean(DIALOGSTORE_VIEWORIENTATION));
		
		fEnableMemberFilterAction= new EnableMemberFilterAction(this, false);
		
		fFocusOnTypeAction= new FocusOnTypeAction(this);
		
		fPaneLabelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_BASICS);
		fPaneLabelProvider.setErrorTickManager(new MarkerErrorTickProvider());
		
		fAllViewers= new TypeHierarchyViewer[3];
	
		fPartListener= new IPartListener() {
			public void partActivated(IWorkbenchPart part) {
				if (part instanceof IEditorPart)
					editorActivated((IEditorPart) part);
			}
			public void partBroughtToTop(IWorkbenchPart part) {}
			public void partClosed(IWorkbenchPart part) {}
			public void partDeactivated(IWorkbenchPart part) {}
			public void partOpened(IWorkbenchPart part) {}
		};	
	}	
		
	/**
	 * Selects an member in the methods list
	 */	
	public void selectMember(IMember member) {
		fMethodsViewer.setSelection(new StructuredSelection(member), true);
	}	


	/**
	 * Gets the current input (IType)
	 */	
	public IType getInput() {
		return fInput;
	}	
	
	private void addHistoryEntry(IType entry) {
		for (int i= fInputHistory.size() - 1; i > fCurrHistoryIndex; i--) {
			fInputHistory.remove(i);
		}
		fInputHistory.add(entry);
		fCurrHistoryIndex++;
		fForwardAction.update();
		fBackwardAction.update();
	}
	
	/**
	 * Gets the next or previous entry from the history
	 * @param forward If true, the next entry is returned, if
	 * if false, the previous
	 * @return The entry from the history or null if no entry available
	 */
	public IType getHistoryEntry(boolean forward) {
		if (forward) {
			int index= fCurrHistoryIndex + 1;
			if (index < fInputHistory.size()) {
				return (IType) fInputHistory.get(index);
			}
		} else {
			int index= fCurrHistoryIndex - 1;
			if (index >= 0) {
				return (IType) fInputHistory.get(index);
			}
		}
		return null;
	}

	/**
	 * Goes to the next or previous entry from the history
	 * @param forward If true, the next entry is returned, if
	 * if false, the previous
	 */	
	public void gotoHistoryEntry(boolean forward) {
		IType elem= getHistoryEntry(forward);
		if (elem != null) {
			if (forward) {
				fCurrHistoryIndex++;
			} else {
				fCurrHistoryIndex--;
			}
			updateInput(elem);
			
			fForwardAction.update();
			fBackwardAction.update();
		}
	}
	
	/**
	 * Sets the input to a new type
	 */
	public void setInput(IType type) {
		if (type != null) {
			ICompilationUnit cu= type.getCompilationUnit();
			if (cu != null && cu.isWorkingCopy()) {
				type= (IType)cu.getOriginal(type);
			}
		} 
		
		if (type != null && !type.equals(fInput)) {
			addHistoryEntry(type);
		}
			
		updateInput(type);
	}		
			
	/**
	 * Changes the input to a new type
	 */
	public void updateInput(IType type) {
		boolean typeChanged= (type != fInput);
		
		fInput= type;
		if (fInput == null) {	
			clearInput();
		} else {
			// turn off member filtering
			enableMemberFilter(false);			
			
			try {
				fHierarchyLifeCycle.ensureRefreshedTypeHierarchy(fInput);
			} catch (JavaModelException e) {
				JavaPlugin.log(e.getStatus());
				clearInput();
				return;
			}
				
			fPagebook.showPage(fTypeMethodsSplitter);						
			
			if (typeChanged) {
				updateTypesViewer();
			}
			
			getCurrentViewer().setSelection(new StructuredSelection(fInput));
			
			updateMethodViewer(fInput);
			updateTitle();
		}
	}
	
	private void clearInput() {
		fInput= null;
		fHierarchyLifeCycle.freeHierarchy();
		
		updateTypesViewer();
	}

	/**
	 * @see IWorbenchPart#setFocus
	 */	
	public void setFocus() {
		fPagebook.setFocus();
	}

	/**
	 * @see IWorkbenchPart#dispose
	 */	
	public void dispose() {
		fHierarchyLifeCycle.freeHierarchy();
		fHierarchyLifeCycle.removeChangedListener(fTypeHierarchyLifeCycleListener);
		fPaneLabelProvider.dispose();
		getSite().getPage().removePartListener(fPartListener);

		if (fHierarchyProblemListener != null) {
			JavaPlugin.getDefault().getProblemMarkerManager().removeListener(fHierarchyProblemListener);
		}
		JavaPlugin.getDefault().getProblemMarkerManager().removeListener(fMethodsViewer);
		super.dispose();
	}
		
	private Control createTypeViewerControl(Composite parent) {
		
		fViewerbook= new PageBook(parent, SWT.NULL);
		
		ISelectionChangedListener selectionChangedListener= new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (!fIsEnableMemberFilter) {
					typeSelectionChanged(event.getSelection());
				}
			}
		};
		
		KeyListener keyListener= new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if (event.stateMask == SWT.NONE) {
					if (event.keyCode == SWT.F4) {
						Object elem= SelectionUtil.getSingleElement(getCurrentViewer().getSelection());
						if (elem instanceof IType) {
							IType[] arr= new IType[] { (IType) elem };
							new OpenTypeHierarchyHelper().open(arr, getSite().getWorkbenchWindow());			
						} else {
							getCurrentViewer().getControl().getDisplay().beep();
						}
						return;
					} else if (event.keyCode == SWT.F5) {
						updateTypesViewer();
						return;
					}
				}
				viewPartKeyShortcuts(event);					
			}
		};
				
		// Create the viewers
		final TypeHierarchyViewer superTypesViewer= new SuperTypeHierarchyViewer(fViewerbook, fHierarchyLifeCycle, this);
		superTypesViewer.getControl().setVisible(false);
		superTypesViewer.getControl().addKeyListener(keyListener);
		superTypesViewer.initContextMenu(new IMenuListener() {
			public void menuAboutToShow(IMenuManager menu) {
				fillTypesViewerContextMenu(superTypesViewer, menu);
			}
		}, IContextMenuConstants.TARGET_ID_SUPERTYPES_VIEW,	getSite());
		superTypesViewer.addSelectionChangedListener(selectionChangedListener);
		
		
		final TypeHierarchyViewer subTypesViewer= new SubTypeHierarchyViewer(fViewerbook, fHierarchyLifeCycle, this);
		subTypesViewer.getControl().setVisible(false);
		subTypesViewer.getControl().addKeyListener(keyListener);
		subTypesViewer.initContextMenu(new IMenuListener() {
			public void menuAboutToShow(IMenuManager menu) {
				fillTypesViewerContextMenu(subTypesViewer, menu);
			}
		}, IContextMenuConstants.TARGET_ID_SUBTYPES_VIEW, getSite());
		subTypesViewer.addSelectionChangedListener(selectionChangedListener);
	
		final TypeHierarchyViewer vajViewer= new TraditionalHierarchyViewer(fViewerbook, fHierarchyLifeCycle, this);
		vajViewer.getControl().setVisible(false);
		vajViewer.getControl().addKeyListener(keyListener);
		vajViewer.initContextMenu(new IMenuListener() {
			public void menuAboutToShow(IMenuManager menu) {
				fillTypesViewerContextMenu(vajViewer, menu);
			}
		}, IContextMenuConstants.TARGET_ID_HIERARCHY_VIEW,	getSite());
		vajViewer.addSelectionChangedListener(selectionChangedListener);

		fAllViewers= new TypeHierarchyViewer[3];
		fAllViewers[VIEW_ID_SUPER]= superTypesViewer;
		fAllViewers[VIEW_ID_SUB]= subTypesViewer;
		fAllViewers[VIEW_ID_TYPE]= vajViewer;
		
		int currViewerIndex;
		try {
			currViewerIndex= fDialogSettings.getInt(DIALOGSTORE_HIERARCHYVIEW);
			if (currViewerIndex < 0 || currViewerIndex > 2) {
				currViewerIndex= VIEW_ID_TYPE;
			}
		} catch (NumberFormatException e) {
			currViewerIndex= VIEW_ID_TYPE;
		}
			
		fEmptyTypesViewer= new Label(fViewerbook, SWT.LEFT);
		
		for (int i= 0; i < fAllViewers.length; i++) {
			fAllViewers[i].setInput(fAllViewers[i]);
		}
		
		// force the update
		fCurrentViewerIndex= -1;
		setView(currViewerIndex);
				
		return fViewerbook;
	}
	
	private Control createMethodViewerControl(Composite parent) {
		fMethodsViewer= new MethodsViewer(parent, this);
		fMethodsViewer.initContextMenu(new IMenuListener() {
			public void menuAboutToShow(IMenuManager menu) {
				fillMethodsViewerContextMenu(menu);
			}
		}, IContextMenuConstants.TARGET_ID_MEMBERS_VIEW, getSite());
		fMethodsViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				methodSelectionChanged(event.getSelection());
			}
		});
		Control control= fMethodsViewer.getTable();
		control.addKeyListener(new KeyAdapter() { 
			public void keyPressed(KeyEvent event) {
				viewPartKeyShortcuts(event);
			}
		});
		
		JavaPlugin.getDefault().getProblemMarkerManager().addListener(fMethodsViewer);
		return control;
	}
	
	private void initDragAndDrop() {
		Transfer[] transfers= new Transfer[] { LocalSelectionTransfer.getInstance() };
		int ops= DND.DROP_COPY;

		DragSource source= new DragSource(fMethodsViewer.getControl(), ops);
		source.setTransfer(transfers);
		source.addDragListener(new BasicSelectionTransferDragAdapter(fMethodsViewer));
		
		for (int i= 0; i < fAllViewers.length; i++) {
			TypeHierarchyViewer curr= fAllViewers[i];
			curr.addDropSupport(ops, transfers, new TypeHierarchyTransferDropAdapter(curr));
		}	
	}
	
	
	private void viewPartKeyShortcuts(KeyEvent event) {
		if (event.stateMask == SWT.CTRL) {
			if (event.character == '1') {
				setView(VIEW_ID_TYPE);
			} else if (event.character == '2') {
				setView(VIEW_ID_SUPER);
			} else if (event.character == '3') {
				setView(VIEW_ID_SUB);
			} else if (event.keyCode == SWT.ARROW_RIGHT) {
				gotoHistoryEntry(true);
			} else if (event.keyCode == SWT.ARROW_LEFT) {
				gotoHistoryEntry(false);
			}
		}	
	}
	
		
	/**
	 * Returns the inner component in a workbench part.
	 * @see IWorkbenchPart#createPartControl
	 */
	public void createPartControl(Composite container) {
						
		fPagebook= new PageBook(container, SWT.NONE);
						
		// page 1 of pagebook (viewers)

		fTypeMethodsSplitter= new SashForm(fPagebook, SWT.VERTICAL);
		fTypeMethodsSplitter.setVisible(false);

		ViewForm typeViewerViewForm= new ViewForm(fTypeMethodsSplitter, SWT.NONE);
		
		Control typeViewerControl= createTypeViewerControl(typeViewerViewForm);
		typeViewerViewForm.setContent(typeViewerControl);
				
		ViewForm methodViewerViewForm= new ViewForm(fTypeMethodsSplitter, SWT.NONE);
		fTypeMethodsSplitter.setWeights(new int[] {35, 65});
		setOrientation(fToggleOrientationAction.isChecked());	
		
		Control methodViewerPart= createMethodViewerControl(methodViewerViewForm);
		methodViewerViewForm.setContent(methodViewerPart);
		
		fMethodViewerPaneLabel= new CLabel(methodViewerViewForm, SWT.NONE);
		methodViewerViewForm.setTopLeft(fMethodViewerPaneLabel);
		
		initDragAndDrop();		
		
		ToolBar methodViewerToolBar= new ToolBar(methodViewerViewForm, SWT.FLAT | SWT.WRAP);
		methodViewerViewForm.setTopCenter(methodViewerToolBar);
		
		// page 2 of pagebook (no hierarchy label)
		fNoHierarchyShownLabel= new Label(fPagebook, SWT.TOP + SWT.LEFT + SWT.WRAP);
		fNoHierarchyShownLabel.setText(TypeHierarchyMessages.getString("TypeHierarchyViewPart.empty")); //$NON-NLS-1$	
		
		MenuManager menu= new MenuManager();
		menu.add(fFocusOnTypeAction);
		fNoHierarchyShownLabel.setMenu(menu.createContextMenu(fNoHierarchyShownLabel));
		
		fPagebook.showPage(fNoHierarchyShownLabel);
		
		// toolbar actions
		IActionBars actionBars= getViewSite().getActionBars();
		IToolBarManager tbmanager= actionBars.getToolBarManager();
		tbmanager.add(fBackwardAction);
		tbmanager.add(fForwardAction);
		IMenuManager viewMenu= actionBars.getMenuManager();
		viewMenu.add(fToggleOrientationAction);  		
	
		
		ToolBarManager lowertbmanager= new ToolBarManager(methodViewerToolBar);
		lowertbmanager.add(fEnableMemberFilterAction);			
		lowertbmanager.add(new Separator());
		fMethodsViewer.contributeToToolBar(lowertbmanager);
		lowertbmanager.update(true);
						
		fAddStubAction= new AddMethodStubAction();
		updateViewerVisibility(false);
		
		for (int i= 0; i < fViewActions.length; i++) {
			tbmanager.add(fViewActions[i]);
		}
		
		// selection provider
		int nHierarchyViewers= fAllViewers.length; 
		Viewer[] trackedViewers= new Viewer[nHierarchyViewers + 1];
		for (int i= 0; i < nHierarchyViewers; i++) {
			trackedViewers[i]= fAllViewers[i];
		}
		trackedViewers[nHierarchyViewers]= fMethodsViewer;
		ISelectionProvider selProvider= new SelectionProviderMediator(trackedViewers);
		IStatusLineManager slManager= getViewSite().getActionBars().getStatusLineManager();
		selProvider.addSelectionChangedListener(new StatusBarUpdater(slManager));
		
		getSite().setSelectionProvider(selProvider);
		getSite().getPage().addPartListener(fPartListener);
				
		fFocusOnSelectionAction= new FocusOnSelectionAction(this, selProvider);		
		
		IType input= determineInputElement();
		if (fMemento != null) {
			restoreState(fMemento, input);
		} else if (input != null) {
			setInput(input);
		}
		
		// fixed for 1GETAYN: ITPJUI:WIN - F1 help does nothing
		WorkbenchHelp.setHelp(fPagebook, new ViewContextComputer(this, IJavaHelpContextIds.TYPE_HIERARCHY_VIEW));
	}
	
	/**
	 * Creates the context menu for the hierarchy viewers
	 */
	private void fillTypesViewerContextMenu(TypeHierarchyViewer viewer, IMenuManager menu) {
		JavaPlugin.createStandardGroups(menu);
		// viewer entries
		viewer.contributeToContextMenu(menu);
		IStructuredSelection selection= (IStructuredSelection)viewer.getSelection();
		if (JavaBasePreferencePage.openTypeHierarchyInPerspective()) {
			addOpenPerspectiveItem(menu, selection);
		}
		addOpenWithMenu(menu, selection);
		
		menu.appendToGroup(IContextMenuConstants.GROUP_SHOW, fFocusOnTypeAction);
		if (fFocusOnSelectionAction.canActionBeAdded())
			menu.appendToGroup(IContextMenuConstants.GROUP_SHOW, fFocusOnSelectionAction);
			
		addRefactoring(menu, viewer);
		ContextMenuGroup.add(menu, new ContextMenuGroup[] { new BuildGroup() }, viewer);
	}

	/**
	 * Creates the context menu for the method viewer
	 */	
	private void fillMethodsViewerContextMenu(IMenuManager menu) {
		JavaPlugin.createStandardGroups(menu);
		// viewer entries
		fMethodsViewer.contributeToContextMenu(menu);
		if (fAddStubAction != null) {
			if (fAddStubAction.init(fInput, fMethodsViewer.getSelection())) {
				menu.appendToGroup(IContextMenuConstants.GROUP_REORGANIZE, fAddStubAction);
			}
		}
		menu.appendToGroup(IContextMenuConstants.GROUP_REORGANIZE, new JavaReplaceWithEditionAction(fMethodsViewer));	
		addOpenWithMenu(menu, (IStructuredSelection)fMethodsViewer.getSelection());
		addRefactoring(menu, fMethodsViewer);
		ContextMenuGroup.add(menu, new ContextMenuGroup[] { new BuildGroup() }, fMethodsViewer);
	}
	
	private void addRefactoring(IMenuManager menu, IInputSelectionProvider viewer){
		MenuManager refactoring= new MenuManager(TypeHierarchyMessages.getString("TypeHierarchyViewPart.menu.refactor")); //$NON-NLS-1$
		ContextMenuGroup.add(refactoring, new ContextMenuGroup[] { new RefactoringGroup() }, viewer);
		if (!refactoring.isEmpty())
			menu.appendToGroup(IContextMenuConstants.GROUP_REORGANIZE, refactoring);
	}
	
	private void addOpenWithMenu(IMenuManager menu, IStructuredSelection selection) {
		// If one file is selected get it.
		// Otherwise, do not show the "open with" menu.
		if (selection.size() != 1)
			return;

		Object element= selection.getFirstElement();
		if (!(element instanceof IJavaElement))
			return;
		IResource resource= null;
		try {
			resource= ((IJavaElement)element).getUnderlyingResource();	
		} catch(JavaModelException e) {
			// ignore
		}
		if (!(resource instanceof IFile))
			return; 

		// Create a menu flyout.
		MenuManager submenu= new MenuManager(TypeHierarchyMessages.getString("TypeHierarchyViewPart.menu.open")); //$NON-NLS-1$
		submenu.add(new OpenWithMenu(getSite().getPage(), (IFile) resource));

		// Add the submenu.
		menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, submenu);
	}

	private void addOpenPerspectiveItem(IMenuManager menu, IStructuredSelection selection) {
		OpenTypeHierarchyHelper.addToMenu(getSite().getWorkbenchWindow(), menu, selection);
	}

	/**
	 * Toggles between the empty viewer page and the hierarchy
	 */
	private void updateViewerVisibility(boolean showEmptyViewer) {
		if (!showEmptyViewer) {
			fViewerbook.showPage(getCurrentViewer().getControl());
		} else {
			fViewerbook.showPage(fEmptyTypesViewer);
		}
	}		
	
	/**
	 * When the input changed or the hierarchy pane becomes visible,
	 * <code>updateTypesViewer<code> brings up the correct view and refreshes
	 * the current tree
	 */
	public void updateTypesViewer() {
		if (fInput == null) {
			fPagebook.showPage(fNoHierarchyShownLabel);
		} else {
			if (getCurrentViewer().containsElements()) {
				Runnable runnable= new Runnable() {
					public void run() {
						getCurrentViewer().updateContent();
					}
				};
				Display display= getDisplay();
				if (display != null) {
					BusyIndicator.showWhile(display, runnable);
				} else {
					runnable.run();
				}
				if (!isChildVisible(fViewerbook, getCurrentViewer().getControl())) {
					updateViewerVisibility(false);
				}	
			} else {							
				fEmptyTypesViewer.setText(TypeHierarchyMessages.getFormattedString("TypeHierarchyViewPart.nodecl", fInput.getElementName()));				 //$NON-NLS-1$
				updateViewerVisibility(true);
			}
		}
	}
			
	private void setMemberFilter(IMember[] memberFilter) {
		for (int i= 0; i < fAllViewers.length; i++) {
			fAllViewers[i].setMemberFilter(memberFilter);
		}
		updateTypesViewer();
		updateTitle();
	}
	
	
	private void methodSelectionChanged(ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			List selected= ((IStructuredSelection)sel).toList();
			int nSelected= selected.size();
			if (fIsEnableMemberFilter) {
				IMember[] memberFilter= null;
				if (nSelected > 0) {
					memberFilter= new IMember[nSelected];
					selected.toArray(memberFilter);
				}
				setMemberFilter(memberFilter);
			}
			if (nSelected == 1) {
				revealElementInEditor(selected.get(0));
			}
		}
	}
	
	private void typeSelectionChanged(ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			List selected= ((IStructuredSelection)sel).toList();
			int nSelected= selected.size();
			if (nSelected != 0) {
				List types= new ArrayList(nSelected);
				for (int i= nSelected-1; i >= 0; i--) {
					Object elem= selected.get(i);
					if (elem instanceof IType && !types.contains(elem)) {
						types.add(elem);
					}
				}
				if (types.size() == 1) {
					IType selectedType= (IType)types.get(0);
					if (!selectedType.equals(fMethodsViewer.getInput())) {
						updateMethodViewer(selectedType);
					}
				} else if (types.size() == 0) {
					// method selected, no change
				}
				if (nSelected == 1) {
					revealElementInEditor(selected.get(0));
				}
			} else {
				if (fMethodsViewer.getInput() != null) {
					updateMethodViewer(null);
				}
			}
		}
	}
	
	private void revealElementInEditor(Object elem) {
		// only allow revealing when the type hierarchy is the active pagae
		// no revealing after selection events due to model changes
		if (getSite().getPage().getActivePart() != this) {
			return;
		}
		
		IEditorPart editorPart= EditorUtility.isOpenInEditor(elem);
		if (editorPart != null && (elem instanceof ISourceReference)) {
			try {
				EditorUtility.openInEditor(elem, false);
				EditorUtility.revealInEditor(editorPart, (ISourceReference)elem);
			} catch (PartInitException e) {
				JavaPlugin.log(e);
			} catch (JavaModelException e) {
				JavaPlugin.log(e.getStatus());
			}
		}
	}
	
	private void updateMethodViewer(IType input) {
		if (input != fMethodsViewer.getInput()) {
			if (input != null) {
				fMethodViewerPaneLabel.setText(fPaneLabelProvider.getText(input));
				fMethodViewerPaneLabel.setImage(fPaneLabelProvider.getImage(input));
			} else {
				fMethodViewerPaneLabel.setText(""); //$NON-NLS-1$
				fMethodViewerPaneLabel.setImage(null);
			}
			fMethodsViewer.setInput(input);
		}
	}
	
	/**
	 * Called from ITypeHierarchyLifeCycleListener.
	 * Can be called from any thread
	 */
	private void doTypeHierarchyChanged(TypeHierarchyLifeCycle typeHierarchy) {
		Display display= getDisplay();
		if (display != null) {
			display.syncExec(new Runnable() {
				public void run() {
					if (!fHierarchyLifeCycle.getHierarchy().exists()) {
						clearInput();
					} else {
						try {
							fHierarchyLifeCycle.ensureRefreshedTypeHierarchy(fInput);
						} catch (JavaModelException e) {
							JavaPlugin.log(e.getStatus());
							clearInput();
							return;
						}
						updateTypesViewer();
					}
				}
			});
		}
	}	
	
	private Display getDisplay() {
		if (fPagebook != null && !fPagebook.isDisposed()) {
			return fPagebook.getDisplay();
		}
		return null;
	}		
	
	private boolean isChildVisible(Composite pb, Control child) {
		Control[] children= pb.getChildren();
		for (int i= 0; i < children.length; i++) {
			if (children[i] == child && children[i].isVisible())
				return true;
		}
		return false;
	}
	
	private void updateTitle() {
		String title= getCurrentViewer().getTitle();
		setTitle(getCurrentViewer().getTitle());
		String tooltip;
		if (fInput != null) {
			String[] args= new String[] { title, JavaModelUtil.getFullyQualifiedName(fInput) };
			tooltip= TypeHierarchyMessages.getFormattedString("TypeHierarchyViewPart.tooltip", args); //$NON-NLS-1$
		} else {
			tooltip= title;
		}
		setTitleToolTip(tooltip);
	}
		
	/**
	 * Sets the current view (see view id)
	 * called from ToggleViewAction
	 */	
	public void setView(int viewerIndex) {
		if (viewerIndex < fAllViewers.length && fCurrentViewerIndex != viewerIndex) {			
			fCurrentViewerIndex= viewerIndex;
			
			updateTypesViewer();
			if (fInput != null) {
				ISelection currSelection= getCurrentViewer().getSelection();
				if (currSelection == null || currSelection.isEmpty()) {
					getCurrentViewer().setSelection(new StructuredSelection(getInput()));
				}
				if (!fIsEnableMemberFilter) {
					typeSelectionChanged(getCurrentViewer().getSelection());
				}
			}		
			updateTitle();
			
			if (fHierarchyProblemListener != null) {
				JavaPlugin.getDefault().getProblemMarkerManager().removeListener(fHierarchyProblemListener);
			}
			fHierarchyProblemListener= getCurrentViewer();
			JavaPlugin.getDefault().getProblemMarkerManager().addListener(fHierarchyProblemListener);
			
			
			fDialogSettings.put(DIALOGSTORE_HIERARCHYVIEW, viewerIndex);
			getCurrentViewer().getTree().setFocus();
		}
		for (int i= 0; i < fViewActions.length; i++) {
			ToggleViewAction action= fViewActions[i];
			action.setChecked(fCurrentViewerIndex == action.getViewerIndex());
		}
	}

	/**
	 * Gets the curret active view index
	 */		
	public int getViewIndex() {
		return fCurrentViewerIndex;
	}
	
	private TypeHierarchyViewer getCurrentViewer() {
		return fAllViewers[fCurrentViewerIndex];
	}

	/**
	 * called from EnableMemberFilterAction
	 */	
	public void enableMemberFilter(boolean on) {
		if (on != fIsEnableMemberFilter) {
			fIsEnableMemberFilter= on;
			if (!on) {
				Object methodViewerInput= fMethodsViewer.getInput();
				IType input= getInput();
				setMemberFilter(null);
				if (methodViewerInput != null && getCurrentViewer().isElementShown(methodViewerInput)) {
					// avoid that the method view changes content by selecting the previous input
					getCurrentViewer().setSelection(new StructuredSelection(methodViewerInput));
				} else if (input != null) {
					// choose a input that exists
					getCurrentViewer().setSelection(new StructuredSelection(input));
					updateMethodViewer(input);
				}
			} else {
				methodSelectionChanged(fMethodsViewer.getSelection());
			}
		}
		fEnableMemberFilterAction.setChecked(on);
	}
	
	/**
	 * called from ToggleOrientationAction
	 */	
	public void setOrientation(boolean horizontal) {
		if (fTypeMethodsSplitter != null && !fTypeMethodsSplitter.isDisposed()) {
			fTypeMethodsSplitter.setOrientation(horizontal ? SWT.HORIZONTAL : SWT.VERTICAL);
		}
		fToggleOrientationAction.setChecked(horizontal);
		fDialogSettings.put(DIALOGSTORE_VIEWORIENTATION, horizontal);
	}
	
	/**
	 * Determines the input element to be used initially 
	 */	
	private IType determineInputElement() {
		Object input= getSite().getPage().getInput();
		if (input instanceof IType) { 
			return (IType)input;
		} 
		return null;	
	}
	
	/* (non-Javadoc)
	 * @see IViewPart#init
	 */
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		fMemento= memento;
	}	
	
	/**
	 * @see ViewPart#saveState(IMemento)
	 */
	public void saveState(IMemento memento) {
		if (fPagebook == null) {
			// part has not been created
			if (fMemento != null) { //Keep the old state;
				memento.putMemento(fMemento);
			}
			return;
		}
		if (fInput != null) {
			memento.putString(TAG_INPUT, fInput.getHandleIdentifier());
		}
		memento.putInteger(TAG_VIEW, getViewIndex());
		memento.putInteger(TAG_ORIENTATION, fToggleOrientationAction.isChecked() ? 1 : 0);	
		int weigths[]= fTypeMethodsSplitter.getWeights();
		int ratio= (weigths[0] * 1000) / (weigths[0] + weigths[1]);
		memento.putInteger(TAG_RATIO, ratio);
		
		ScrollBar bar= getCurrentViewer().getTree().getVerticalBar();
		int position= bar != null ? bar.getSelection() : 0;
		memento.putInteger(TAG_VERTICAL_SCROLL, position);

		IJavaElement selection= (IJavaElement)((IStructuredSelection) getCurrentViewer().getSelection()).getFirstElement();
		if (selection != null) {
			memento.putString(TAG_SELECTION, selection.getHandleIdentifier());
		}
			
		fMethodsViewer.saveState(memento);
	}
	
	/*
	 * Restores the type hierarchy settings from a memento
	 */
	private void restoreState(IMemento memento, IType defaultInput) {
		IType input= defaultInput;
		String elementId= memento.getString(TAG_INPUT);
		if (elementId != null) {
			input= (IType) JavaCore.create(elementId);
			if (!input.exists()) {
				input= null;
			}
		}
		setInput(input);

		Integer viewerIndex= memento.getInteger(TAG_VIEW);
		if (viewerIndex != null) {
			setView(viewerIndex.intValue());
		}
		Integer orientation= memento.getInteger(TAG_ORIENTATION);
		if (orientation != null) {
			setOrientation(orientation.intValue() == 1);
		}
		Integer ratio= memento.getInteger(TAG_RATIO);
		if (ratio != null) {
			fTypeMethodsSplitter.setWeights(new int[] { ratio.intValue(), 1000 - ratio.intValue() });
		}
		ScrollBar bar= getCurrentViewer().getTree().getVerticalBar();
		if (bar != null) {
			Integer vScroll= memento.getInteger(TAG_VERTICAL_SCROLL);
			if (vScroll != null) {
				bar.setSelection(vScroll.intValue());
			}
		}
		
		String selectionId= memento.getString(TAG_SELECTION);
		if (selectionId != null) {
			IJavaElement elem= JavaCore.create(selectionId);
			if (getCurrentViewer().isElementShown(elem)) {
				getCurrentViewer().setSelection(new StructuredSelection(elem));
			}
		}
		fMethodsViewer.restoreState(memento);
	}
	
	/*
	 * Link selection to active editor
	 */
	private void editorActivated(IEditorPart editor) {
		if (!JavaBasePreferencePage.linkTypeHierarchySelectionToEditor()) {
			return;
		}
		if (fInput == null) {
			// no type hierarchy shown
			return;
		}
		
		IJavaElement elem= (IJavaElement)editor.getEditorInput().getAdapter(IJavaElement.class);
		try {
			TypeHierarchyViewer currentViewer= getCurrentViewer();
			if (elem instanceof IClassFile) {
				IType type= ((IClassFile)elem).getType();
				if (currentViewer.isElementShown(type)) {
					currentViewer.setSelection(new StructuredSelection(type));
				}
			} else if (elem instanceof ICompilationUnit) {
				IType[] allTypes= ((ICompilationUnit)elem).getAllTypes();
				for (int i= 0; i < allTypes.length; i++) {
					if (currentViewer.isElementShown(allTypes[i])) {
						currentViewer.setSelection(new StructuredSelection(allTypes[i]));
						return;
					}
				}
			}	
		} catch (JavaModelException e) {
			JavaPlugin.log(e.getStatus());
		}
		
	}	
	

}