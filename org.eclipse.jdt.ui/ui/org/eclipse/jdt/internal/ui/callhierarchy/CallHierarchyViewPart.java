/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation
 *             (report 36180: Callers/Callees view)
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.actions.CCPActionGroup;
import org.eclipse.jdt.ui.actions.GenerateActionGroup;
import org.eclipse.jdt.ui.actions.JavaSearchActionGroup;
import org.eclipse.jdt.ui.actions.OpenEditorActionGroup;
import org.eclipse.jdt.ui.actions.OpenViewActionGroup;
import org.eclipse.jdt.ui.actions.RefactorActionGroup;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.CompositeActionGroup;
import org.eclipse.jdt.internal.ui.dnd.DelegatingDropAdapter;
import org.eclipse.jdt.internal.ui.dnd.JdtViewerDragAdapter;
import org.eclipse.jdt.internal.ui.dnd.LocalSelectionTransfer;
import org.eclipse.jdt.internal.ui.dnd.TransferDragSourceListener;
import org.eclipse.jdt.internal.ui.dnd.TransferDropTargetListener;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.packageview.SelectionTransferDragAdapter;
import org.eclipse.jdt.internal.ui.util.JavaUIHelp;
import org.eclipse.jdt.internal.ui.viewsupport.StatusBarUpdater;

import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.CallLocation;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

/**
 * This is the main view for the callers plugin. It builds a tree of callers/callees
 * and allows the user to double click an entry to go to the selected method.
 *
 */
public class CallHierarchyViewPart extends ViewPart implements IDoubleClickListener,
    ISelectionChangedListener {
    private CallHierarchyViewSiteAdapter fViewSiteAdapter;
    private CallHierarchyViewAdapter fViewAdapter;
    public static final String CALLERS_VIEW_ID = "org.eclipse.jdt.callhierarchy.view"; //$NON-NLS-1$
    private static final String DIALOGSTORE_VIEWORIENTATION = "CallHierarchyViewPart.orientation"; //$NON-NLS-1$
    private static final String DIALOGSTORE_CALL_MODE = "CallHierarchyViewPart.call_mode"; //$NON-NLS-1$
    private static final String DIALOGSTORE_JAVA_LABEL_FORMAT = "CallHierarchyViewPart.java_label_format"; //$NON-NLS-1$
    private static final String TAG_ORIENTATION = "orientation"; //$NON-NLS-1$
    private static final String TAG_CALL_MODE = "call_mode"; //$NON-NLS-1$
    private static final String TAG_JAVA_LABEL_FORMAT = "java_label_format"; //$NON-NLS-1$
    private static final String TAG_RATIO = "ratio"; //$NON-NLS-1$
    static final int VIEW_ORIENTATION_VERTICAL = 0;
    static final int VIEW_ORIENTATION_HORIZONTAL = 1;
    static final int VIEW_ORIENTATION_SINGLE = 2;
    static final int CALL_MODE_CALLERS = 0;
    static final int CALL_MODE_CALLEES = 1;
    static final int JAVA_LABEL_FORMAT_DEFAULT = JavaElementLabelProvider.SHOW_DEFAULT;
    static final int JAVA_LABEL_FORMAT_SHORT = JavaElementLabelProvider.SHOW_BASICS;
    static final int JAVA_LABEL_FORMAT_LONG = JavaElementLabelProvider.SHOW_OVERLAY_ICONS |
        JavaElementLabelProvider.SHOW_PARAMETERS |
        JavaElementLabelProvider.SHOW_RETURN_TYPE |
        JavaElementLabelProvider.SHOW_POST_QUALIFIED;
    static final String GROUP_SEARCH_SCOPE = "MENU_SEARCH_SCOPE"; //$NON-NLS-1$
    static final String ID_CALL_HIERARCHY = "org.eclipse.jdt.ui.CallHierarchy"; //$NON-NLS-1$
    private static final String GROUP_FOCUS = "group.focus"; //$NON-NLS-1$
    private static final int PAGE_EMPTY = 0;
    private static final int PAGE_VIEWER = 1;
    private Label fNoHierarchyShownLabel;
    private PageBook fPagebook;
    private IDialogSettings fDialogSettings;
    private int fCurrentOrientation;
    private int fCurrentCallMode;
    private int fCurrentJavaLabelFormat;
    private MethodWrapper fCalleeRoot;
    private MethodWrapper fCallerRoot;
    private IMemento fMemento;
    private IMethod fShownMethod;
    private SelectionProviderMediator fSelectionProviderMediator;
    private List fMethodHistory;
    private TableViewer fLocationViewer;
    private Menu fLocationContextMenu;
    private SashForm fHierarchyLocationSplitter;
    private SearchScopeActionGroup fSearchScopeActions;
    private ToggleOrientationAction[] fToggleOrientationActions;
    private ToggleCallModeAction[] fToggleCallModeActions;
    private ToggleJavaLabelFormatAction[] fToggleJavaLabelFormatActions;
    private CallHierarchyFiltersActionGroup fFiltersActionGroup;
    private HistoryDropDownAction fHistoryDropDownAction;
    private RefreshAction fRefreshAction;
    private OpenLocationAction fOpenLocationAction;
    private FocusOnSelectionAction fFocusOnSelectionAction;
    private CompositeActionGroup fActionGroups;
    private CallHierarchyViewer fCallHierarchyViewer;
    private boolean fShowCallDetails;

    public CallHierarchyViewPart() {
        super();

        fDialogSettings = JavaPlugin.getDefault().getDialogSettings();

        fMethodHistory = new ArrayList();
    }

    public void setFocus() {
        fPagebook.setFocus();
    }

    /**
     * Sets the history entries
     */
    public void setHistoryEntries(IMethod[] elems) {
        fMethodHistory.clear();

        for (int i = 0; i < elems.length; i++) {
            fMethodHistory.add(elems[i]);
        }

        updateHistoryEntries();
    }

    /**
     * Gets all history entries.
     */
    public IMethod[] getHistoryEntries() {
        if (fMethodHistory.size() > 0) {
            updateHistoryEntries();
        }

        return (IMethod[]) fMethodHistory.toArray(new IMethod[fMethodHistory.size()]);
    }

    /**
     * Method setMethod.
     * @param method
     */
    public void setMethod(IMethod method) {
        if (method == null) {
            showPage(PAGE_EMPTY);

            return;
        }

        if ((method != null) && !method.equals(fShownMethod)) {
            addHistoryEntry(method);
        }

        this.fShownMethod = method;

        refresh();
    }

    public IMethod getMethod() {
        return fShownMethod;
    }

    /**
     * called from ToggleOrientationAction.
     * @param orientation VIEW_ORIENTATION_HORIZONTAL or VIEW_ORIENTATION_VERTICAL
     */
    void setOrientation(int orientation) {
        if (fCurrentOrientation != orientation) {
            if ((fLocationViewer != null) && !fLocationViewer.getControl().isDisposed() &&
                        (fHierarchyLocationSplitter != null) &&
                        !fHierarchyLocationSplitter.isDisposed()) {
                if (orientation == VIEW_ORIENTATION_SINGLE) {
                    setShowCallDetails(false);
                } else {
                    if (fCurrentOrientation == VIEW_ORIENTATION_SINGLE) {
                        setShowCallDetails(true);
                    }

                    boolean horizontal = orientation == VIEW_ORIENTATION_HORIZONTAL;
                    fHierarchyLocationSplitter.setOrientation(horizontal ? SWT.HORIZONTAL
                                                                         : SWT.VERTICAL);
                }

                fHierarchyLocationSplitter.layout();
            }

            for (int i = 0; i < fToggleOrientationActions.length; i++) {
                fToggleOrientationActions[i].setChecked(orientation == fToggleOrientationActions[i].getOrientation());
            }

            fCurrentOrientation = orientation;
            fDialogSettings.put(DIALOGSTORE_VIEWORIENTATION, orientation);
        }
    }

    /**
     * called from ToggleCallModeAction.
     * @param orientation CALL_MODE_CALLERS or CALL_MODE_CALLEES
     */
    void setCallMode(int mode) {
        if (fCurrentCallMode != mode) {
            for (int i = 0; i < fToggleCallModeActions.length; i++) {
                fToggleCallModeActions[i].setChecked(mode == fToggleCallModeActions[i].getMode());
            }

            fCurrentCallMode = mode;
            fDialogSettings.put(DIALOGSTORE_CALL_MODE, mode);

            updateView();
        }
    }

    /**
     * called from ToggleCallModeAction.
     * @param orientation CALL_MODE_CALLERS or CALL_MODE_CALLEES
     */
    void setJavaLabelFormat(int format) {
        if (fCurrentJavaLabelFormat != format) {
            for (int i = 0; i < fToggleJavaLabelFormatActions.length; i++) {
                fToggleJavaLabelFormatActions[i].setChecked(format == fToggleJavaLabelFormatActions[i].getFormat());
            }

            fCurrentJavaLabelFormat = format;
            fDialogSettings.put(DIALOGSTORE_JAVA_LABEL_FORMAT, format);

            fCallHierarchyViewer.setJavaLabelFormat(fCurrentJavaLabelFormat);
        }
    }

    public IJavaSearchScope getSearchScope() {
        return fSearchScopeActions.getSearchScope();
    }

    public void setShowCallDetails(boolean show) {
        fShowCallDetails = show;
        showOrHideCallDetailsView();
    }

    private void initDragAndDrop() {
        Transfer[] transfers= new Transfer[] { LocalSelectionTransfer.getInstance() };
        int ops= DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;

        addDragAdapters(fCallHierarchyViewer, ops, transfers);
        addDropAdapters(fCallHierarchyViewer, ops | DND.DROP_DEFAULT, transfers);
        addDropAdapters(fLocationViewer, ops | DND.DROP_DEFAULT, transfers);

        //dnd on empty hierarchy
        DropTarget dropTarget = new DropTarget(fNoHierarchyShownLabel, ops | DND.DROP_DEFAULT);
        dropTarget.setTransfer(transfers);
        dropTarget.addDropListener(new CallHierarchyTransferDropAdapter(this, fCallHierarchyViewer));
    }
    
    private void addDropAdapters(StructuredViewer viewer, int ops, Transfer[] transfers){
        TransferDropTargetListener[] dropListeners= new TransferDropTargetListener[] {
            new CallHierarchyTransferDropAdapter(this, viewer)
        };
        viewer.addDropSupport(ops, transfers, new DelegatingDropAdapter(dropListeners));
    }

    private void addDragAdapters(StructuredViewer viewer, int ops, Transfer[] transfers) {
        TransferDragSourceListener[] dragListeners= new TransferDragSourceListener[] {
            new SelectionTransferDragAdapter(new SelectionProviderAdapter(viewer))
        };
        viewer.addDragSupport(ops, transfers, new JdtViewerDragAdapter(viewer, dragListeners));
    }   
            
    public void createPartControl(Composite parent) {
        fPagebook = new PageBook(parent, SWT.NONE);

        // Page 1: Viewers
        createHierarchyLocationSplitter(fPagebook);
        createCallHierarchyViewer(fHierarchyLocationSplitter);
        createLocationViewer(fHierarchyLocationSplitter);

        // Page 2: Nothing selected
        fNoHierarchyShownLabel = new Label(fPagebook, SWT.TOP + SWT.LEFT + SWT.WRAP);
        fNoHierarchyShownLabel.setText(CallHierarchyMessages.getString(
                "CallHierarchyViewPart.empty")); //$NON-NLS-1$   

        showPage(PAGE_EMPTY);

        WorkbenchHelp.setHelp(fPagebook, IJavaHelpContextIds.CALL_HIERARCHY_VIEW);
        
        fSelectionProviderMediator = new SelectionProviderMediator(new Viewer[] {
                    fCallHierarchyViewer, fLocationViewer
                });

        IStatusLineManager slManager = getViewSite().getActionBars().getStatusLineManager();
        fSelectionProviderMediator.addSelectionChangedListener(new StatusBarUpdater(
                slManager));
        getSite().setSelectionProvider(fSelectionProviderMediator);

        makeActions();
        fillViewMenu();
        fillActionBars();

        initOrientation();
        initCallMode();
        initJavaLabelFormat();

        if (fMemento != null) {
            restoreState(fMemento);
        }
        
        initDragAndDrop();      
    }

    /**
     * @param PAGE_EMPTY
     */
    private void showPage(int page) {
        if (page == PAGE_EMPTY) {
            fPagebook.showPage(fNoHierarchyShownLabel);
        } else {
            fPagebook.showPage(fHierarchyLocationSplitter);
        }

        enableActions(page != PAGE_EMPTY);
    }

    /**
     * @param b
     */
    private void enableActions(boolean enabled) {
        fLocationContextMenu.setEnabled(enabled);

        // TODO: Is it possible to disable the actions on the toolbar and on the view menu? 
    }

    /**
     * Restores the type hierarchy settings from a memento.
     */
    private void restoreState(IMemento memento) {
        Integer orientation = memento.getInteger(TAG_ORIENTATION);

        if (orientation != null) {
            setOrientation(orientation.intValue());
        }

        Integer callMode = memento.getInteger(TAG_CALL_MODE);

        if (callMode != null) {
            setCallMode(callMode.intValue());
        }

        Integer javaLabelFormat = memento.getInteger(TAG_JAVA_LABEL_FORMAT);

        if (javaLabelFormat != null) {
            setJavaLabelFormat(javaLabelFormat.intValue());
        }

        Integer ratio = memento.getInteger(TAG_RATIO);

        if (ratio != null) {
            fHierarchyLocationSplitter.setWeights(new int[] {
                    ratio.intValue(), 1000 - ratio.intValue()
                });
        }
    }

    private void initCallMode() {
        int mode;

        try {
            mode = fDialogSettings.getInt(DIALOGSTORE_CALL_MODE);

            if ((mode < 0) || (mode > 1)) {
                mode = CALL_MODE_CALLERS;
            }
        } catch (NumberFormatException e) {
            mode = CALL_MODE_CALLERS;
        }

        // force the update
        fCurrentCallMode = -1;

        // will fill the main tool bar
        setCallMode(mode);
    }

    private void initOrientation() {
        int orientation;

        try {
            orientation = fDialogSettings.getInt(DIALOGSTORE_VIEWORIENTATION);

            if ((orientation < 0) || (orientation > 2)) {
                orientation = VIEW_ORIENTATION_VERTICAL;
            }
        } catch (NumberFormatException e) {
            orientation = VIEW_ORIENTATION_VERTICAL;
        }

        // force the update
        fCurrentOrientation = -1;

        // will fill the main tool bar
        setOrientation(orientation);
    }

    private void initJavaLabelFormat() {
        int format;

        try {
            format = fDialogSettings.getInt(DIALOGSTORE_JAVA_LABEL_FORMAT);

            if (format > 0) {
                format = JAVA_LABEL_FORMAT_DEFAULT;
            }
        } catch (NumberFormatException e) {
            format = JAVA_LABEL_FORMAT_DEFAULT;
        }

        // force the update
        fCurrentJavaLabelFormat = -1;

        // will fill the main tool bar
        setJavaLabelFormat(format);
    }

    private void fillViewMenu() {
        IActionBars actionBars = getViewSite().getActionBars();
        IMenuManager viewMenu = actionBars.getMenuManager();
        viewMenu.add(new Separator());

        for (int i = 0; i < fToggleCallModeActions.length; i++) {
            viewMenu.add(fToggleCallModeActions[i]);
        }

        viewMenu.add(new Separator());

        for (int i = 0; i < fToggleJavaLabelFormatActions.length; i++) {
            viewMenu.add(fToggleJavaLabelFormatActions[i]);
        }

        viewMenu.add(new Separator());

        for (int i = 0; i < fToggleOrientationActions.length; i++) {
            viewMenu.add(fToggleOrientationActions[i]);
        }
    }

    /**
     *
     */
    public void dispose() {
        disposeMenu(fLocationContextMenu);

        if (fActionGroups != null)
            fActionGroups.dispose();

        super.dispose();
    }

    /**
     * Double click listener which jumps to the method in the source code.
     *
     * @return IDoubleClickListener
     */
    public void doubleClick(DoubleClickEvent event) {
        jumpToSelection(event.getSelection());
    }

    /**
     * Goes to the selected entry, without updating the order of history entries.
     */
    public void gotoHistoryEntry(IMethod entry) {
        if (fMethodHistory.contains(entry)) {
            setMethod(entry);
        }
    }

    /* (non-Javadoc)
     * Method declared on IViewPart.
     */
    public void init(IViewSite site, IMemento memento)
        throws PartInitException {
        super.init(site, memento);
        fMemento = memento;
    }

    public void jumpToDeclarationOfSelection() {
        ISelection selection = null;

        selection = getSelection();

        if ((selection != null) && selection instanceof IStructuredSelection) {
            Object structuredSelection = ((IStructuredSelection) selection).getFirstElement();

            if (structuredSelection instanceof IMember) {
                CallHierarchyUI.jumpToMember((IMember) structuredSelection);
            } else if (structuredSelection instanceof MethodWrapper) {
                MethodWrapper methodWrapper = (MethodWrapper) structuredSelection;

                CallHierarchyUI.jumpToMember(methodWrapper.getMember());
            } else if (structuredSelection instanceof CallLocation) {
                CallHierarchyUI.jumpToMember(((CallLocation) structuredSelection).getCalledMember());
            }
        }
    }

    public void jumpToSelection(ISelection selection) {
        if ((selection != null) && selection instanceof IStructuredSelection) {
            Object structuredSelection = ((IStructuredSelection) selection).getFirstElement();

            if (structuredSelection instanceof MethodWrapper) {
                MethodWrapper methodWrapper = (MethodWrapper) structuredSelection;
                CallLocation firstCall = methodWrapper.getMethodCall()
                                                      .getFirstCallLocation();

                if (firstCall != null) {
                    CallHierarchyUI.jumpToLocation(firstCall);
                } else {
                    CallHierarchyUI.jumpToMember(methodWrapper.getMember());
                }
            } else if (structuredSelection instanceof CallLocation) {
                CallHierarchyUI.jumpToLocation((CallLocation) structuredSelection);
            }
        }
    }

    /**
     *
     */
    public void refresh() {
        setCalleeRoot(null);
        setCallerRoot(null);

        updateView();
    }

    public void saveState(IMemento memento) {
        if (fPagebook == null) {
            // part has not been created
            if (fMemento != null) { //Keep the old state;
                memento.putMemento(fMemento);
            }

            return;
        }

        memento.putInteger(TAG_CALL_MODE, fCurrentCallMode);
        memento.putInteger(TAG_ORIENTATION, fCurrentOrientation);
        memento.putInteger(TAG_JAVA_LABEL_FORMAT, fCurrentJavaLabelFormat);

        int[] weigths = fHierarchyLocationSplitter.getWeights();
        int ratio = (weigths[0] * 1000) / (weigths[0] + weigths[1]);
        memento.putInteger(TAG_RATIO, ratio);
    }

    /**
     * @return ISelectionChangedListener
     */
    public void selectionChanged(SelectionChangedEvent e) {
        if (e.getSelectionProvider() == fCallHierarchyViewer) {
            methodSelectionChanged(e.getSelection());
        } else {
            locationSelectionChanged(e.getSelection());
        }
    }

    /**
     * @param selection
     */
    private void locationSelectionChanged(ISelection selection) {}

    /**
     * @param selection
     */
    private void methodSelectionChanged(ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            Object selectedElement = ((IStructuredSelection) selection).getFirstElement();

            if (selectedElement instanceof MethodWrapper) {
                MethodWrapper methodWrapper = (MethodWrapper) selectedElement;

                revealElementInEditor(methodWrapper, fCallHierarchyViewer);
                updateLocationsView(methodWrapper);
            } else {
                updateLocationsView(null);
            }
        }
    }

    private void revealElementInEditor(Object elem, Viewer originViewer) {
        // only allow revealing when the type hierarchy is the active pagae
        // no revealing after selection events due to model changes
        if (getSite().getPage().getActivePart() != this) {
            return;
        }

        if (fSelectionProviderMediator.getViewerInFocus() != originViewer) {
            return;
        }

        if (elem instanceof MethodWrapper) {
            CallLocation callLocation = CallHierarchy.getCallLocation(elem);

            if (callLocation != null) {
                IEditorPart editorPart = CallHierarchyUI.isOpenInEditor(callLocation);

                if (editorPart != null) {
                    getSite().getPage().bringToTop(editorPart);

                    if (editorPart instanceof ITextEditor) {
                        ITextEditor editor = (ITextEditor) editorPart;
                        editor.selectAndReveal(callLocation.getStart(),
                            (callLocation.getEnd() - callLocation.getStart()));
                    }
                }
            } else {
                IEditorPart editorPart = CallHierarchyUI.isOpenInEditor(elem);
                getSite().getPage().bringToTop(editorPart);
                EditorUtility.revealInEditor(editorPart,
                    ((MethodWrapper) elem).getMember());
            }
        } else if (elem instanceof IJavaElement) {
            IEditorPart editorPart = EditorUtility.isOpenInEditor(elem);

            if (editorPart != null) {
                //            getSite().getPage().removePartListener(fPartListener);
                getSite().getPage().bringToTop(editorPart);
                EditorUtility.revealInEditor(editorPart, (IJavaElement) elem);

                //            getSite().getPage().addPartListener(fPartListener);
            }
        }
    }

    /**
     * Returns the current selection.
     */
    protected ISelection getSelection() {
        return getSite().getSelectionProvider().getSelection();
    }

    protected void fillContextMenu(IMenuManager menu) {
        JavaPlugin.createStandardGroups(menu);

        menu.appendToGroup(IContextMenuConstants.GROUP_SHOW, fRefreshAction);
    }

    protected void handleKeyEvent(KeyEvent event) {
        if (event.stateMask == 0) {
            if (event.keyCode == SWT.F5) {
                if ((fRefreshAction != null) && fRefreshAction.isEnabled()) {
                    fRefreshAction.run();

                    return;
                }
            }
        }
    }

    private IActionBars getActionBars() {
        return getViewSite().getActionBars();
    }

    private void setCalleeRoot(MethodWrapper calleeRoot) {
        this.fCalleeRoot = calleeRoot;
    }

    private MethodWrapper getCalleeRoot() {
        if (fCalleeRoot == null) {
            fCalleeRoot = (MethodWrapper) CallHierarchy.getDefault().getCalleeRoot(fShownMethod);
        }

        return fCalleeRoot;
    }

    private void setCallerRoot(MethodWrapper callerRoot) {
        this.fCallerRoot = callerRoot;
    }

    private MethodWrapper getCallerRoot() {
        if (fCallerRoot == null) {
            fCallerRoot = (MethodWrapper) CallHierarchy.getDefault().getCallerRoot(fShownMethod);
        }

        return fCallerRoot;
    }

    /**
     * Adds the entry if new. Inserted at the beginning of the history entries list.
     */
    private void addHistoryEntry(IJavaElement entry) {
        if (fMethodHistory.contains(entry)) {
            fMethodHistory.remove(entry);
        }

        fMethodHistory.add(0, entry);
        fHistoryDropDownAction.setEnabled(!fMethodHistory.isEmpty());
    }

    /**
     * @param parent
     */
    private void createLocationViewer(Composite parent) {
        fLocationViewer = new TableViewer(parent, SWT.NONE);

        fLocationViewer.setContentProvider(new ArrayContentProvider());
        fLocationViewer.setLabelProvider(new LocationLabelProvider());
        fLocationViewer.setInput(new ArrayList());
        fLocationViewer.getControl().addKeyListener(createKeyListener());

        JavaUIHelp.setHelp(fLocationViewer, IJavaHelpContextIds.CALL_HIERARCHY_VIEW);

        fOpenLocationAction = new OpenLocationAction(getSite());
        fLocationViewer.addOpenListener(new IOpenListener() {
                public void open(OpenEvent event) {
                    fOpenLocationAction.run();
                }
            });

        //        fListViewer.addDoubleClickListener(this);
        MenuManager menuMgr = new MenuManager(); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
                /* (non-Javadoc)
                 * @see org.eclipse.jface.action.IMenuListener#menuAboutToShow(org.eclipse.jface.action.IMenuManager)
                 */
                public void menuAboutToShow(IMenuManager manager) {
                    fillContextMenu(manager);
                }
            });

        fLocationContextMenu = menuMgr.createContextMenu(fLocationViewer.getControl());
        fLocationViewer.getControl().setMenu(fLocationContextMenu);

        // Register viewer with site. This must be done before making the actions.
        getSite().registerContextMenu(menuMgr, fLocationViewer);
    }

    private void createHierarchyLocationSplitter(Composite parent) {
        fHierarchyLocationSplitter = new SashForm(parent, SWT.NONE);

        fHierarchyLocationSplitter.addKeyListener(createKeyListener());
    }

    private void createCallHierarchyViewer(Composite parent) {
        fCallHierarchyViewer = new CallHierarchyViewer(parent, this);

        fCallHierarchyViewer.addKeyListener(createKeyListener());
        fCallHierarchyViewer.addSelectionChangedListener(this);

        fCallHierarchyViewer.initContextMenu(new IMenuListener() {
                public void menuAboutToShow(IMenuManager menu) {
                    fillCallHierarchyViewerContextMenu(menu);
                }
            }, ID_CALL_HIERARCHY, getSite());
    }

    /**
     * @param fCallHierarchyViewer
     * @param menu
     */
    protected void fillCallHierarchyViewerContextMenu(IMenuManager menu) {
        JavaPlugin.createStandardGroups(menu);

        menu.appendToGroup(IContextMenuConstants.GROUP_SHOW, fRefreshAction);
        menu.appendToGroup(IContextMenuConstants.GROUP_SHOW, new Separator(GROUP_FOCUS));

        if (fFocusOnSelectionAction.canActionBeAdded()) {
            menu.appendToGroup(GROUP_FOCUS, fFocusOnSelectionAction);
        }

        fActionGroups.setContext(new ActionContext(getSelection()));
        fActionGroups.fillContextMenu(menu);
        fActionGroups.setContext(null);
    }

    private void disposeMenu(Menu contextMenu) {
        if ((contextMenu != null) && !contextMenu.isDisposed()) {
            contextMenu.dispose();
        }
    }

    private void fillActionBars() {
        IActionBars actionBars = getActionBars();
        IToolBarManager toolBar = actionBars.getToolBarManager();

        fActionGroups.fillActionBars(actionBars);
        toolBar.add(fHistoryDropDownAction);

        for (int i = 0; i < fToggleCallModeActions.length; i++) {
            toolBar.add(fToggleCallModeActions[i]);
        }
    }

    private KeyListener createKeyListener() {
        KeyListener keyListener = new KeyAdapter() {
                public void keyReleased(KeyEvent event) {
                    handleKeyEvent(event);
                }
            };

        return keyListener;
    }

    /**
     *
     */
    private void makeActions() {
        fRefreshAction = new RefreshAction(this);
        fFocusOnSelectionAction = new FocusOnSelectionAction(this);
        fSearchScopeActions = new SearchScopeActionGroup(this);
        fFiltersActionGroup = new CallHierarchyFiltersActionGroup(this,
                fCallHierarchyViewer);
        fHistoryDropDownAction = new HistoryDropDownAction(this);
        fHistoryDropDownAction.setEnabled(false);
        fToggleOrientationActions = new ToggleOrientationAction[] {
                new ToggleOrientationAction(this, VIEW_ORIENTATION_VERTICAL),
                new ToggleOrientationAction(this, VIEW_ORIENTATION_HORIZONTAL),
                new ToggleOrientationAction(this, VIEW_ORIENTATION_SINGLE)
            };
        fToggleCallModeActions = new ToggleCallModeAction[] {
                new ToggleCallModeAction(this, CALL_MODE_CALLERS),
                new ToggleCallModeAction(this, CALL_MODE_CALLEES)
            };
        fToggleJavaLabelFormatActions = new ToggleJavaLabelFormatAction[] {
                new ToggleJavaLabelFormatAction(this, JAVA_LABEL_FORMAT_DEFAULT),
                new ToggleJavaLabelFormatAction(this, JAVA_LABEL_FORMAT_SHORT),
                new ToggleJavaLabelFormatAction(this, JAVA_LABEL_FORMAT_LONG)
            };

        fActionGroups = new CompositeActionGroup(new ActionGroup[] {
                    new OpenEditorActionGroup(getViewAdapter()), 
                    new OpenViewActionGroup(getViewAdapter()),
                    new CCPActionGroup(getViewAdapter()),
                    new GenerateActionGroup(getViewAdapter()), 
                    new RefactorActionGroup(getViewAdapter()),
                    new JavaSearchActionGroup(getViewAdapter()),
                    fSearchScopeActions, fFiltersActionGroup
                });
    }

    private CallHierarchyViewAdapter getViewAdapter() {
        if (fViewAdapter == null) {
            fViewAdapter= new CallHierarchyViewAdapter(getViewSiteAdapter());
        }
        return fViewAdapter; 
    }

    private CallHierarchyViewSiteAdapter getViewSiteAdapter() {
        if (fViewSiteAdapter == null) {
            fViewSiteAdapter= new CallHierarchyViewSiteAdapter(this.getViewSite());
        }
        return fViewSiteAdapter;
    }

    private void showOrHideCallDetailsView() {
        if (fShowCallDetails) {
            fHierarchyLocationSplitter.setMaximizedControl(null);
        } else {
            fHierarchyLocationSplitter.setMaximizedControl(fCallHierarchyViewer.getControl());
        }
    }

    private void updateLocationsView(MethodWrapper methodWrapper) {
        if (methodWrapper != null) {
            fLocationViewer.setInput(methodWrapper.getMethodCall().getCallLocations());
        } else {
            fLocationViewer.setInput(""); //$NON-NLS-1$
        }
    }

    private void updateHistoryEntries() {
        for (int i = fMethodHistory.size() - 1; i >= 0; i--) {
            IMethod method = (IMethod) fMethodHistory.get(i);

            if (!method.exists()) {
                fMethodHistory.remove(i);
            }
        }

        fHistoryDropDownAction.setEnabled(!fMethodHistory.isEmpty());
    }

    /**
     * Method updateView.
     */
    private void updateView() {
        if ((fShownMethod != null)) {
            showPage(PAGE_VIEWER);

            CallHierarchy.getDefault().setSearchScope(getSearchScope());

            if (fCurrentCallMode == CALL_MODE_CALLERS) {
                setTitle(CallHierarchyMessages.getString("CallHierarchyViewPart.callsToMethod")); //$NON-NLS-1$
                fCallHierarchyViewer.setMethodWrapper(getCallerRoot());
            } else {
                setTitle(CallHierarchyMessages.getString("CallHierarchyViewPart.callsFromMethod")); //$NON-NLS-1$
                fCallHierarchyViewer.setMethodWrapper(getCalleeRoot());
            }

            updateLocationsView(null);
        }
    }

    static CallHierarchyViewPart findAndShowCallersView(IWorkbenchPartSite site) {
        IWorkbenchPage workbenchPage = site.getPage();
        CallHierarchyViewPart callersView = null;

        try {
            callersView = (CallHierarchyViewPart) workbenchPage.showView(CallHierarchyViewPart.CALLERS_VIEW_ID);
        } catch (PartInitException e) {
            JavaPlugin.log(e);
        }

        return callersView;
    }
}
