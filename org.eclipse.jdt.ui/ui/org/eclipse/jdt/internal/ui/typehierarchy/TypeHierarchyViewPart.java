/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import java.util.ArrayList;import java.util.List;import java.util.ResourceBundle;import org.eclipse.swt.SWT;import org.eclipse.swt.custom.CLabel;import org.eclipse.swt.custom.SashForm;import org.eclipse.swt.custom.ViewForm;import org.eclipse.swt.events.KeyAdapter;import org.eclipse.swt.events.KeyEvent;import org.eclipse.swt.events.KeyListener;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Display;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.ToolBar;import org.eclipse.core.resources.IFile;import org.eclipse.core.resources.IResource;import org.eclipse.jface.action.IMenuListener;import org.eclipse.jface.action.IMenuManager;import org.eclipse.jface.action.IStatusLineManager;import org.eclipse.jface.action.IToolBarManager;import org.eclipse.jface.action.MenuManager;import org.eclipse.jface.action.Separator;import org.eclipse.jface.action.ToolBarManager;import org.eclipse.jface.dialogs.IDialogSettings;import org.eclipse.jface.viewers.IInputSelectionProvider;import org.eclipse.jface.viewers.ILabelProvider;import org.eclipse.jface.viewers.ISelection;import org.eclipse.jface.viewers.ISelectionChangedListener;import org.eclipse.jface.viewers.ISelectionProvider;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.viewers.SelectionChangedEvent;import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.jface.viewers.Viewer;import org.eclipse.ui.IEditorPart;import org.eclipse.ui.IMemento;import org.eclipse.ui.IViewSite;import org.eclipse.ui.PartInitException;import org.eclipse.ui.actions.OpenWithMenu;import org.eclipse.ui.part.PageBook;import org.eclipse.ui.part.ViewPart;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IMember;import org.eclipse.jdt.core.ISourceReference;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.ui.IContextMenuConstants;import org.eclipse.jdt.ui.ITypeHierarchyViewPart;import org.eclipse.jdt.ui.JavaElementLabelProvider;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.actions.AddMethodStubAction;import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;import org.eclipse.jdt.internal.ui.actions.OpenHierarchyPerspectiveItem;import org.eclipse.jdt.internal.ui.compare.JavaReplaceWithEditionAction;import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;import org.eclipse.jdt.internal.ui.refactoring.RefactoringResources;import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringGroup;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jdt.internal.ui.util.JavaModelUtility;import org.eclipse.jdt.internal.ui.viewsupport.SelectionProviderMediator;import org.eclipse.jdt.internal.ui.viewsupport.StatusBarUpdater;

/**
 * view showing the supertypes/subtypes of its input.
 */
public class TypeHierarchyViewPart extends ViewPart implements ITypeHierarchyLifeCycleListener, ITypeHierarchyViewPart {
	
	private static final String PART_NAME= "TypeHierarchyViewPart";
	
	private static final String KEY_SHOW_SUPERTYPES= PART_NAME + ".message";
	private static final String PREFIX_CREATE_ERROR= PART_NAME + ".error.createhierarchy.";
	
	private static final String NO_DECL_IN_VIEWER= PART_NAME + ".nodecl";
	private static final String PREFIX_TOGGLE_SUB= PART_NAME + ".toggleaction.subtypes.";
	private static final String PREFIX_TOGGLE_SUPER= PART_NAME + ".toggleaction.supertypes.";
	private static final String PREFIX_TOGGLE_VAJ= PART_NAME + ".toggleaction.vajhierarchy.";
	
	private static final String PREFIX_FORWARD= PART_NAME + ".historyaction.forward.";
	private static final String PREFIX_BACKWARD= PART_NAME + ".historyaction.backward.";
	
	private static final String TITLE_TOOLTIP= PART_NAME + ".tooltip";
	
	private static final String DIALOGSTORE_HIERARCHYVIEW= PART_NAME + ".hierarchyview";	

	private static final String TAG_INPUT= "input";
	private static final String TAG_VIEW= "view";

	private IType fInput;
	
	private IMemento fMemento;
	
	private ArrayList fInputHistory;
	private int fCurrHistoryIndex;
	
	private TypeHierarchyLifeCycle fHierarchyLifeCycle;
		
	private MethodsViewer fMethodsViewer;
			
	private TypeHierarchyViewer fCurrentViewer;
	private TypeHierarchyViewer[] fAllViewers;
	
	private boolean fIsEnableMemberFilter;
	
	private SashForm fTypeMethodsSplitter;
	private PageBook fViewerbook;
	private PageBook fPagebook;
	private Label fNoHierarchyShownLabel;
	private Label fEmptyTypesViewer;
	
	private CLabel fMethodViewerPaneLabel;
	private ILabelProvider fPaneLabelProvider;
	
	private IDialogSettings fDialogSettings;
	
	private ToggleViewAction[] fViewActions;
	
	private HistoryAction fForwardAction;
	private HistoryAction fBackwardAction;
	
	private EnableMemberFilterAction fEnableMemberFilterAction;
	private AddMethodStubAction fAddStubAction;
		
	public TypeHierarchyViewPart() {
		fHierarchyLifeCycle= new TypeHierarchyLifeCycle();
		fHierarchyLifeCycle.addChangedListener(this);
		fIsEnableMemberFilter= false;
		
		fInputHistory= new ArrayList();
		fCurrHistoryIndex= -1;
		
		ResourceBundle bundle= JavaPlugin.getResourceBundle();
				
		ToggleViewAction superViewAction= new ToggleViewAction(this, 0, bundle, PREFIX_TOGGLE_SUPER);
		superViewAction.setImageDescriptors("lcl16", "super_co.gif");

		ToggleViewAction subViewAction= new ToggleViewAction(this, 1, bundle, PREFIX_TOGGLE_SUB);
		subViewAction.setImageDescriptors("lcl16", "sub_co.gif");

		ToggleViewAction vajViewAction= new ToggleViewAction(this, 2, bundle, PREFIX_TOGGLE_VAJ);
		vajViewAction.setImageDescriptors("lcl16", "hierarchy_co.gif");
		
		fViewActions= new ToggleViewAction[] { vajViewAction, superViewAction, subViewAction };
		
		superViewAction.setOthers(fViewActions);
		subViewAction.setOthers(fViewActions);
		vajViewAction.setOthers(fViewActions);
		
		fForwardAction= new HistoryAction(this, true, PREFIX_FORWARD);
		fForwardAction.setImageDescriptors("lcl16", "forward_nav.gif");

		fBackwardAction= new HistoryAction(this, false, PREFIX_BACKWARD);
		fBackwardAction.setImageDescriptors("lcl16", "bkward_nav.gif");
		
		fEnableMemberFilterAction= new EnableMemberFilterAction(this, false);
		
		fPaneLabelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_BASICS);
		
		fAllViewers= new TypeHierarchyViewer[0];

		
		fDialogSettings= JavaPlugin.getDefault().getDialogSettings();
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
			fEnableMemberFilterAction.setChecked(false);
			enableMemberFilter(false);			
			
			try {
				fHierarchyLifeCycle.ensureRefreshedTypeHierarchy(fInput);
			} catch (JavaModelException e) {
				ExceptionHandler.handle(e, JavaPlugin.getResourceBundle(), PREFIX_CREATE_ERROR);
				clearInput();
				return;
			}
				
			fPagebook.showPage(fTypeMethodsSplitter);						
			
			if (typeChanged) {
				updateTypesViewer();
			}
			
			for (int i =0; i < fAllViewers.length; i++) {
				fAllViewers[i].setSelection(new StructuredSelection(fInput));
			}
			
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
		fHierarchyLifeCycle.removeChangedListener(this);
		fPaneLabelProvider.dispose();
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
				if (event.keyCode == SWT.F4) {
					IStructuredSelection structSel= (IStructuredSelection)fCurrentViewer.getSelection();
					if (structSel.size() == 1) {
							Object firstSelection= structSel.getFirstElement();
							if (firstSelection instanceof IType) {
								setInput((IType)firstSelection);
								return;
							}
					}
					fCurrentViewer.getControl().getDisplay().beep();
				}
			}
		};
				
		// Create the viewers
		final TypeHierarchyViewer superTypesViewer= new SuperTypeHierarchyViewer(fViewerbook, fHierarchyLifeCycle, this);
		superTypesViewer.getControl().setVisible(false);
		superTypesViewer.setMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager menu) {
				fillTypesViewerContextMenu(superTypesViewer, menu);
			}
		});
		superTypesViewer.addSelectionChangedListener(selectionChangedListener);		
		superTypesViewer.getControl().addKeyListener(keyListener);
		
		final TypeHierarchyViewer subTypesViewer= new SubTypeHierarchyViewer(fViewerbook, fHierarchyLifeCycle, this);
		subTypesViewer.getControl().setVisible(false);
		subTypesViewer.setMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager menu) {
				fillTypesViewerContextMenu(subTypesViewer, menu);
			}
		});
		subTypesViewer.addSelectionChangedListener(selectionChangedListener);
		subTypesViewer.getControl().addKeyListener(keyListener);
		
		final TypeHierarchyViewer vajViewer= new TraditionalHierarchyViewer(fViewerbook, fHierarchyLifeCycle, this);
		vajViewer.getControl().setVisible(false);
		vajViewer.setMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager menu) {
				fillTypesViewerContextMenu(vajViewer, menu);
			}
		});
		vajViewer.addSelectionChangedListener(selectionChangedListener);
		vajViewer.getControl().addKeyListener(keyListener);
		

		fAllViewers= new TypeHierarchyViewer[] { superTypesViewer, subTypesViewer, vajViewer};
		
		int currViewerIndex;
		try {
			currViewerIndex= fDialogSettings.getInt(DIALOGSTORE_HIERARCHYVIEW);
			if (currViewerIndex < 0 || currViewerIndex > 2) {
				currViewerIndex= 2;
			}
		} catch (NumberFormatException e) {
			currViewerIndex= 2;
		}
			
		fCurrentViewer= fAllViewers[currViewerIndex];		
		fEmptyTypesViewer= new Label(fViewerbook, SWT.LEFT);
		
		for (int i= 0; i < fAllViewers.length; i++) {
			fAllViewers[i].setInput(fAllViewers[i]);
		}
				
		return fViewerbook;
	}
	


	private Control createMethodViewerControl(Composite parent) {
	
		fMethodsViewer= new MethodsViewer(parent, this);
		fMethodsViewer.setMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager menu) {
				fillMethodsViewerContextMenu(menu);
			}
		});
		fMethodsViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				methodSelectionChanged(event.getSelection());
			}
		});
		
		return fMethodsViewer.getTable();
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
		//setViewFormMargins(typeViewerViewForm);
		
		Control typeViewerControl= createTypeViewerControl(typeViewerViewForm);
		typeViewerViewForm.setContent(typeViewerControl);
				
		ViewForm methodViewerViewForm= new ViewForm(fTypeMethodsSplitter, SWT.NONE);
		//methodViewerViewForm.setTopCenterSeparate(true);
		//setViewFormMargins(methodViewerViewForm);
		fTypeMethodsSplitter.setWeights(new int[] {35, 65});
		
		
		Control methodViewerPart= createMethodViewerControl(methodViewerViewForm);
		methodViewerViewForm.setContent(methodViewerPart);
		
		fMethodViewerPaneLabel= new CLabel(methodViewerViewForm, SWT.NONE);
		methodViewerViewForm.setTopLeft(fMethodViewerPaneLabel);
		
		ToolBar methodViewerToolBar= new ToolBar(methodViewerViewForm, SWT.FLAT | SWT.WRAP);
		methodViewerViewForm.setTopCenter(methodViewerToolBar);
		
		// page 2 of pagebook (no hierarchy label)

		fNoHierarchyShownLabel= new Label(fPagebook, SWT.LEFT + SWT.WRAP);
		fNoHierarchyShownLabel.setText(JavaPlugin.getResourceString(KEY_SHOW_SUPERTYPES));
		
		fPagebook.showPage(fNoHierarchyShownLabel);
		
		// toolbar actions	
		IToolBarManager tbmanager= getViewSite().getActionBars().getToolBarManager();
		
		tbmanager.add(fBackwardAction);
		tbmanager.add(fForwardAction);
		
		for (int i= 0; i < fViewActions.length; i++) {
			ToggleViewAction action= fViewActions[i];
			tbmanager.add(action);
			action.setActive(fCurrentViewer == fAllViewers[action.getViewerIndex()]);
		}
		
		ToolBarManager lowertbmanager= new ToolBarManager(methodViewerToolBar);
		lowertbmanager.add(fEnableMemberFilterAction);			
		lowertbmanager.add(new Separator());
		fMethodsViewer.contributeToToolBar(lowertbmanager);
		lowertbmanager.update(true);
						
		fAddStubAction= new AddMethodStubAction(fMethodsViewer);
		updateViewerVisibility(false);
		
		
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
		
		IType input= determineInputElement();
		if (fMemento != null) {
			restoreState(fMemento, input);
		} else if (input != null) {
			setInput(input);
		}
	}
	
	/**
	 * Creates the context menu for the hierarchy viewers
	 */
	private void fillTypesViewerContextMenu(TypeHierarchyViewer viewer, IMenuManager menu) {
		JavaPlugin.createStandardGroups(menu);
		// viewer entries
		viewer.contributeToContextMenu(menu);
		IStructuredSelection selection= (IStructuredSelection)viewer.getSelection();
		addOpenPerspectiveItem(menu, selection);
		addOpenWithMenu(menu, selection);
		addRefactoring(menu, viewer);
	}

	/**
	 * Creates the context menu for the method viewer
	 */	
	private void fillMethodsViewerContextMenu(IMenuManager menu) {
		JavaPlugin.createStandardGroups(menu);
		// viewer entries
		fMethodsViewer.contributeToContextMenu(menu);
		if (fAddStubAction != null) {
			fAddStubAction.setParentType(fInput);
			if (fAddStubAction.canActionBeAdded()) {
				menu.appendToGroup(IContextMenuConstants.GROUP_REORGANIZE, fAddStubAction);
			}
		}
		menu.appendToGroup(IContextMenuConstants.GROUP_REORGANIZE, new JavaReplaceWithEditionAction(fMethodsViewer));	
		addOpenWithMenu(menu, (IStructuredSelection)fMethodsViewer.getSelection());
		addRefactoring(menu, fMethodsViewer);
	}
	
	private void addRefactoring(IMenuManager menu, IInputSelectionProvider viewer){
		MenuManager refactoring= new MenuManager(RefactoringResources.getResourceString("Refactoring.submenu"));
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
		MenuManager submenu= new MenuManager("Open &With");
		submenu.add(new OpenWithMenu(getSite().getPage(), (IFile) resource));

		// Add the submenu.
		menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, submenu);

	}

	private void addOpenPerspectiveItem(IMenuManager menu, IStructuredSelection selection) {
		// If one file is selected get it.
		// Otherwise, do not show the "open with" menu.
		if (selection.size() != 1)
			return;

		Object element= selection.getFirstElement();
		if (!(element instanceof IType))
			return;
		IType[] input= {(IType)element};
		menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, new OpenHierarchyPerspectiveItem(getSite().getWorkbenchWindow(), input));
	}

	/**
	 * Toggles between the empty viewer page and the hierarchy
	 */
	private void updateViewerVisibility(boolean showEmptyViewer) {
		if (!showEmptyViewer) {
			fViewerbook.showPage(fCurrentViewer.getControl());
		} else {
			fViewerbook.showPage(fEmptyTypesViewer);
		}
	}		
	
	/**
	 * When the input changed or the hierarchy pane becomes visible,
	 * <code>updateTypesViewer<code> brings up the correct view and refreshes
	 * the current tree
	 */
	private void updateTypesViewer() {
		if (fInput == null) {
			fPagebook.showPage(fNoHierarchyShownLabel);
		} else {
			if (fCurrentViewer.containsElements()) {
				fCurrentViewer.updateContent();
				if (!isChildVisible(fViewerbook, fCurrentViewer.getControl())) {
					updateViewerVisibility(false);
				}
			} else {							
				fEmptyTypesViewer.setText(JavaPlugin.getFormattedString(NO_DECL_IN_VIEWER, fInput.getElementName()));				
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
		if (input != null) {
			fMethodViewerPaneLabel.setText(fPaneLabelProvider.getText(input));
			fMethodViewerPaneLabel.setImage(fPaneLabelProvider.getImage(input));
		} else {
			fMethodViewerPaneLabel.setText("");
			fMethodViewerPaneLabel.setImage(null);
		}
		fMethodsViewer.setInput(input);
	}
	
	/**
	 * @see ITypeHierarchyLifeCycleListener#typeHierarchyChanged
	 * Can be called from any thread
	 */
	public void typeHierarchyChanged(TypeHierarchyLifeCycle typeHierarchy) {
		checkedSyncExec(new Runnable() {
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
	
	private void checkedSyncExec(Runnable r) {
		if (fPagebook != null && !fPagebook.isDisposed()) {
			Display d= fPagebook.getDisplay();
			if (d != null) {
				d.syncExec(r);
			}
		}
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
		String title= fCurrentViewer.getTitle();
		setTitle(fCurrentViewer.getTitle());
		String tooltip;
		if (fInput != null) {
			String[] args= new String[] { title, JavaModelUtility.getFullyQualifiedName(fInput) };
			tooltip= JavaPlugin.getFormattedString(TITLE_TOOLTIP, args);
		} else {
			tooltip= title;
		}
		setTitleToolTip(tooltip);
	}
	
	/**
	 * Sets the current view (superhierarchy (0), subhierarchy (1), typehierarcy(2))
	 * called from ToggleViewAction
	 */	
	public void setView(int viewerIndex) {
		if (viewerIndex < fAllViewers.length && fAllViewers[viewerIndex] != fCurrentViewer) {
			fCurrentViewer= fAllViewers[viewerIndex];
			updateTypesViewer();
			if (fInput != null) {
				ISelection currSelection= fCurrentViewer.getSelection();
				if (currSelection == null || currSelection.isEmpty()) {
					fCurrentViewer.setSelection(new StructuredSelection(getInput()));
				}
				if (!fIsEnableMemberFilter) {
					typeSelectionChanged(fCurrentViewer.getSelection());
				}
			}
			updateTitle();
			fDialogSettings.put(DIALOGSTORE_HIERARCHYVIEW, viewerIndex);
		}
	}

	/**
	 * Gets the curret active view index
	 */		
	public int getViewIndex() {
		for (int i= 0; i < fAllViewers.length; i++) {
			if (fCurrentViewer == fAllViewers[i]) {
				return i;
			}
		}
		return 0;
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
				if (methodViewerInput != null && fCurrentViewer.containsElement(methodViewerInput)) {
					// avoid that the method view changes content by selecting the previous input
					fCurrentViewer.setSelection(new StructuredSelection(methodViewerInput));
				} else if (input != null) {
					// choose a input that exists
					fCurrentViewer.setSelection(new StructuredSelection(input));
					updateMethodViewer(input);
				}
			} else {
				methodSelectionChanged(fMethodsViewer.getSelection());
			}
		}
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
		memento.putString(TAG_VIEW, Integer.toString(getViewIndex()));
				
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
		}
		setInput(input);

		String viewerIndex= memento.getString(TAG_VIEW);
		try {
			setView(Integer.parseInt(viewerIndex));
		} catch (NumberFormatException e) {
		}
		
		fMethodsViewer.restoreState(memento);
	}	

}