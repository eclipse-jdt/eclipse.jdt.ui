/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import java.util.ArrayList;import java.util.Iterator;import java.util.List;import java.util.ResourceBundle;import org.eclipse.swt.SWT;import org.eclipse.swt.custom.CLabel;import org.eclipse.swt.custom.SashForm;import org.eclipse.swt.custom.ViewForm;import org.eclipse.swt.events.KeyAdapter;import org.eclipse.swt.events.KeyEvent;import org.eclipse.swt.events.KeyListener;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Display;import org.eclipse.swt.widgets.Event;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Listener;import org.eclipse.swt.widgets.ToolBar;import org.eclipse.core.resources.IFile;import org.eclipse.core.resources.IResource;import org.eclipse.jface.action.IMenuListener;import org.eclipse.jface.action.IMenuManager;import org.eclipse.jface.action.IStatusLineManager;import org.eclipse.jface.action.IToolBarManager;import org.eclipse.jface.action.MenuManager;import org.eclipse.jface.action.Separator;import org.eclipse.jface.action.ToolBarManager;import org.eclipse.jface.viewers.ILabelProvider;import org.eclipse.jface.viewers.ISelection;import org.eclipse.jface.viewers.ISelectionChangedListener;import org.eclipse.jface.viewers.ISelectionProvider;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.viewers.SelectionChangedEvent;import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.jface.viewers.Viewer;import org.eclipse.ui.actions.OpenWithMenu;import org.eclipse.ui.part.PageBook;import org.eclipse.ui.part.ViewPart;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IMember;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.ui.IContextMenuConstants;import org.eclipse.jdt.ui.ITypeHierarchyViewPart;import org.eclipse.jdt.ui.JavaElementLabelProvider;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaPluginImages;import org.eclipse.jdt.internal.ui.actions.AddMethodStubAction;import org.eclipse.jdt.internal.ui.compare.JavaReplaceWithEditionAction;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jdt.internal.ui.util.JavaModelUtility;import org.eclipse.jdt.internal.ui.util.SelectionUtil;import org.eclipse.jdt.internal.ui.viewsupport.SelectionProviderMediator;import org.eclipse.jdt.internal.ui.viewsupport.StatusBarUpdater;

/**
 * view showing the supertypes/subtypes of its input.
 */
public class TypeHierarchyViewPart extends ViewPart implements ITypeHierarchyLifeCycleListener, ITypeHierarchyViewPart {
	
	private static final String KEY_SHOW_SUPERTYPES= "TypeHierarchyViewPart.message";
	private static final String PREFIX_CREATE_ERROR= "TypeHierarchyViewPart.error.createhierarchy.";
	
	private static final String NO_DECL_IN_VIEWER= "TypeHierarchyViewPart.nodecl";
	private static final String PREFIX_TOGGLE_SUB= "TypeHierarchyViewPart.toggleaction.subtypes.";
	private static final String PREFIX_TOGGLE_SUPER= "TypeHierarchyViewPart.toggleaction.supertypes.";
	private static final String PREFIX_TOGGLE_VAJ= "TypeHierarchyViewPart.toggleaction.vajhierarchy.";
	
	private static final String TITLE_TOOLTIP= "TypeHierarchyViewPart.tooltip";
	
	private IType fInput;
	private TypeHierarchyLifeCycle fHierarchyLifeCycle;
		
	private boolean fTypesViewRefreshNeeded;
	
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
	
	private ToggleViewAction fSwitchSubViewAction;
	private ToggleViewAction fSwitchSuperViewAction;
	private ToggleViewAction fSwitchVAJViewAction;
	
	private EnableMemberFilterAction fEnableMemberFilterAction;
	private AddMethodStubAction fAddStubAction; 
	
	public TypeHierarchyViewPart() {
		fHierarchyLifeCycle= new TypeHierarchyLifeCycle();
		fHierarchyLifeCycle.addChangedListener(this);
		fIsEnableMemberFilter= false;
		
		ResourceBundle bundle= JavaPlugin.getResourceBundle();
		
		fSwitchSubViewAction= new ToggleViewAction(this, 1, bundle, PREFIX_TOGGLE_SUB);
		//fSwitchSubViewAction.setImageDescriptor(JavaPluginImages.DESC_LCL_SUBTYPES_VIEW);
		fSwitchSubViewAction.setImageDescriptors("lcl16", "sub_co.gif");
		
		fSwitchSuperViewAction= new ToggleViewAction(this, 0, bundle, PREFIX_TOGGLE_SUPER);
		//fSwitchSuperViewAction.setImageDescriptor(JavaPluginImages.DESC_LCL_SUPERTYPES_VIEW);
		fSwitchSuperViewAction.setImageDescriptors("lcl16", "super_co.gif");

		fSwitchVAJViewAction= new ToggleViewAction(this, 2, bundle, PREFIX_TOGGLE_VAJ);
		//fSwitchVAJViewAction.setImageDescriptor(JavaPluginImages.DESC_LCL_VAJHIERARCHY_VIEW);
		fSwitchVAJViewAction.setImageDescriptors("lcl16", "hierarchy_co.gif");
		
		fSwitchSubViewAction.setOthers(new ToggleViewAction[] { fSwitchSuperViewAction, fSwitchVAJViewAction });
		fSwitchSuperViewAction.setOthers(new ToggleViewAction[] { fSwitchSubViewAction, fSwitchVAJViewAction });
		fSwitchVAJViewAction.setOthers(new ToggleViewAction[] { fSwitchSubViewAction, fSwitchSuperViewAction });
		
		fEnableMemberFilterAction= new EnableMemberFilterAction(this, false);
		
		fPaneLabelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_BASICS);
		
		fAllViewers= new TypeHierarchyViewer[0];
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
	
	/**
	 * Sets a new input type
	 */
	public void setInput(IType type) {
		if (type != null) {
			ICompilationUnit cu= type.getCompilationUnit();
			if (cu != null && cu.isWorkingCopy()) {
				type= (IType)cu.getOriginal(type);
			}
		}	
		
		if (type == null) {
			clearInput();
			return;
		}
		
		if (!fPagebook.isVisible()) {
			// a change while the type hierachy was not visible
			if (!type.equals(fInput)) {
				fInput= type;
				fTypesViewRefreshNeeded= true;
			}
		} else {
			// turn off member filtering
			fEnableMemberFilterAction.setChecked(false);
			enableMemberFilter(false);
			
			IType prevInput= fInput;
			fInput= type;
			try {
				fHierarchyLifeCycle.ensureRefreshedTypeHierarchy(fInput);
			} catch (JavaModelException e) {
				ExceptionHandler.handle(e, JavaPlugin.getResourceBundle(), PREFIX_CREATE_ERROR);
				clearInput();
				return;
			}
				
			if (!isChildVisible(fPagebook, fTypeMethodsSplitter)) {
				fPagebook.showPage(fTypeMethodsSplitter);
				for (int i =0; i < fAllViewers.length; i++) {
					fAllViewers[i].setInput(fAllViewers[i]);
				}						
			}
			
			if (fTypesViewRefreshNeeded || !fInput.equals(prevInput)) {
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
		
		if (fPagebook.isVisible()) {
			updateTypesViewer();
		} else {
			fTypesViewRefreshNeeded= true;
		}
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
		
		fAllViewers= new TypeHierarchyViewer[] { superTypesViewer, subTypesViewer, vajViewer };
		fCurrentViewer= superTypesViewer;
		fEmptyTypesViewer= new Label(fViewerbook, SWT.LEFT);
		
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
				if (fIsEnableMemberFilter) {
					methodSelectionChanged(event.getSelection());
				}
			}
		});
		
		return fMethodsViewer.getTable();
	}
	
	private void setViewFormMargins(ViewForm viewerViewForm) {
		viewerViewForm.marginWidth= 0;
		viewerViewForm.marginHeight= 0;
		//viewerViewForm.verticalSpacing= 0;
		//viewerViewForm.horizontalSpacing= 0;
	}
	
	
	
	/**
	 * Returns the inner component in a workbench part.
	 * @see IWorkbenchPart#createPartControl
	 */
	public void createPartControl(Composite container) {
						
		fPagebook= new PageBook(container, SWT.NONE);
		
		fPagebook.addListener(SWT.Show, new Listener() {
			public void handleEvent(Event evt) {
				becomesVisible();
			}
		});
				
		// page 1 of pagebook (viewers)

		fTypeMethodsSplitter= new SashForm(fPagebook, SWT.VERTICAL);
		fTypeMethodsSplitter.setVisible(false);

		ViewForm typeViewerViewForm= new ViewForm(fTypeMethodsSplitter, SWT.NONE);
		setViewFormMargins(typeViewerViewForm);
		
		Control typeViewerControl= createTypeViewerControl(typeViewerViewForm);
		typeViewerViewForm.setContent(typeViewerControl);
				
		ViewForm methodViewerViewForm= new ViewForm(fTypeMethodsSplitter, SWT.NONE);
		methodViewerViewForm.setTopCenterSeparate(true);
		setViewFormMargins(methodViewerViewForm);
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
			
		tbmanager.add(fSwitchSuperViewAction);
		tbmanager.add(fSwitchSubViewAction);
		tbmanager.add(fSwitchVAJViewAction);
		
		ToolBarManager lowertbmanager= new ToolBarManager(methodViewerToolBar);
		lowertbmanager.add(fEnableMemberFilterAction);			
		lowertbmanager.add(new Separator());
		fMethodsViewer.contributeToToolBar(lowertbmanager);
		lowertbmanager.update(true);
		
		fSwitchSubViewAction.setActive(false);
		fSwitchSuperViewAction.setActive(true);
		fSwitchVAJViewAction.setActive(false);
				
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
	
	}
	
	/**
	 * Creates the context menu for the hierarchy viewers
	 */
	private void fillTypesViewerContextMenu(TypeHierarchyViewer viewer, IMenuManager menu) {
		JavaPlugin.createStandardGroups(menu);
		// viewer entries
		viewer.contributeToContextMenu(menu);
		addOpenWithMenu(menu, (IStructuredSelection)viewer.getSelection());
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

	private void updateViewerVisibility(boolean showEmptyViewer) {
		if (!showEmptyViewer) {
			fViewerbook.showPage(fCurrentViewer.getControl());
		} else {
			fViewerbook.showPage(fEmptyTypesViewer);
		}
	}		
		
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
		fTypesViewRefreshNeeded= false;
	}	
			
	private void setMemberFilter(IMember[] memberFilter) {
		for (int i= 0; i < fAllViewers.length; i++) {
			fAllViewers[i].setMemberFilter(memberFilter);
		}
		updateTypesViewer();
		updateTitle();
	}
	
	
	private void methodSelectionChanged(ISelection sel) {
		IMember[] memberFilter;
		if (!sel.isEmpty() && sel instanceof IStructuredSelection) {
			IStructuredSelection structSel= (IStructuredSelection)sel;
			memberFilter= new IMember[structSel.size()];
			Iterator iter= ((IStructuredSelection)sel).iterator();
			for (int i= 0; iter.hasNext(); i++) {
				memberFilter[i]= (IMember) iter.next();
			}
		} else {
			memberFilter= null;
		}		
		setMemberFilter(memberFilter);
	}
	
	private void typeSelectionChanged(ISelection sel) {
		Object[] selectedElements= SelectionUtil.toArray(sel);
		int nSelected= selectedElements.length;
		if (nSelected != 0) {
			List types= new ArrayList(nSelected);
			for (int i= nSelected-1; i >= 0; i--) {
				Object elem= selectedElements[i];
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
		} else {
			if (fMethodsViewer.getInput() != null) {
				updateMethodViewer(null);
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
					if (fPagebook.isVisible()) {
						try {
							fHierarchyLifeCycle.ensureRefreshedTypeHierarchy(fInput);
						} catch (JavaModelException e) {
							JavaPlugin.log(e.getStatus());
							clearInput();
							return;
						}
						updateTypesViewer();
					} else {
						fTypesViewRefreshNeeded= true;
					}
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
	
	private void becomesVisible() {
		if (fTypesViewRefreshNeeded) {
			if (!isChildVisible(fPagebook, fTypeMethodsSplitter)) {
				setInput(fInput);
			} else {
				try {
					fHierarchyLifeCycle.ensureRefreshedTypeHierarchy(fInput);
				} catch (JavaModelException e) {
					ExceptionHandler.handle(e, JavaPlugin.getResourceBundle(), PREFIX_CREATE_ERROR);
					clearInput();
					return;
				}	
				updateTypesViewer();
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
		}
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
					// avoid that the method view chnages content by selecting the previous input
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
}