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
package org.eclipse.jdt.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.corext.codemanipulation.AddUnimplementedMethodsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.dialogs.SourceActionDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.ElementValidator;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.ViewerPane;


/**
 * Adds unimplemented methods of a type. Action opens a dialog from
 * which the user can chosse the methods to be added.
 * <p>
 * Will open the parent compilation unit in a Java editor. The result is 
 * unsaved, so the user can decide if the changes are acceptable.
 * <p>
 * The action is applicable to structured selections containing elements
 * of type <code>IType</code>.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class OverrideMethodsAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;	

	/**
	 * Creates a new <code>OverrideMethodsAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public OverrideMethodsAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.getString("OverrideMethodsAction.label")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("OverrideMethodsAction.description")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("OverrideMethodsAction.tooltip")); //$NON-NLS-1$		
		
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.ADD_UNIMPLEMENTED_METHODS_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public OverrideMethodsAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(checkEnabledEditor());
	}
		
	//---- Structured Viewer -----------------------------------------------------------
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		boolean enabled= false;
		try {
			enabled= getSelectedType(selection) != null;
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e);
		}
		setEnabled(enabled);
	}	
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	protected void run(IStructuredSelection selection) {
		Shell shell= getShell();
		try {
			IType type= getSelectedType(selection);
			if (type == null || !ElementValidator.check(type, getShell(), getDialogTitle(), false) || !ActionUtil.isProcessable(getShell(), type)) {
				return;
			}
			
			// open an editor and work on a working copy
			IEditorPart editor= EditorUtility.openInEditor(type);
			type= (IType)EditorUtility.getWorkingCopy(type);
			
			if (type == null) {
				MessageDialog.openError(shell, getDialogTitle(), ActionMessages.getString("OverrideMethodsAction.error.type_removed_in_editor")); //$NON-NLS-1$
				return;
			}
			
			run(shell, type, editor);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, shell, getDialogTitle(), null); 
		}			
	}

	//---- Java Editior --------------------------------------------------------------
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	protected void selectionChanged(ITextSelection selection) {
	}
	
	private boolean checkEnabledEditor() {
		return fEditor != null && SelectionConverter.canOperateOn(fEditor);
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	protected void run(ITextSelection selection) {
		Shell shell= getShell();
		try {
			IType type= SelectionConverter.getTypeAtOffset(fEditor);
			if (type != null) {
				if (!ElementValidator.check(type, shell, getDialogTitle(), false) || !ActionUtil.isProcessable(shell, type)) {
					return;
				}						
				run(shell, type, fEditor);
			} else {
				MessageDialog.openInformation(shell, getDialogTitle(), ActionMessages.getString("OverrideMethodsAction.not_applicable")); //$NON-NLS-1$
			}
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), null);
		}
	}

	//---- Helpers -------------------------------------------------------------------
	
	private void run(Shell shell, IType type, IEditorPart editor) throws JavaModelException {
		ITypeHierarchy typeHierarchy= type.newSupertypeHierarchy(null);

		IMethod[] inheritedMethods= StubUtility.getOverridableMethods(type, typeHierarchy, false);
		
		List toImplement= new ArrayList();
		for (int i= 0; i < inheritedMethods.length; i++) {
			IMethod curr= inheritedMethods[i];
			if (JdtFlags.isAbstract(curr)) {
				toImplement.add(curr);
			}
		}
		IMethod[] toImplementArray= (IMethod[]) toImplement.toArray(new IMethod[toImplement.size()]);		

		HashSet types= new HashSet(inheritedMethods.length);
		for (int i= 0; i < inheritedMethods.length; i++) {
			types.add(inheritedMethods[i].getDeclaringType());
		}
		Object[] typesArrays= types.toArray();
		ViewerSorter sorter= new OverrideMethodSorter(typeHierarchy);
		sorter.sort(null, typesArrays);

		JavaElementLabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		OverrideMethodContentProvider contentProvider = new OverrideMethodContentProvider(inheritedMethods, typesArrays);			
		
		HashSet expanded= new HashSet(toImplementArray.length); 
		for (int i= 0; i < toImplementArray.length; i++) {
			expanded.add(toImplementArray[i].getDeclaringType());
		}

		OverrideTreeSelectionDialog dialog= new OverrideTreeSelectionDialog(shell, contentProvider, labelProvider, fEditor, typeHierarchy.getType());
		if (expanded.isEmpty() && typesArrays.length > 0) {
			expanded.add(typesArrays[0]);
		}
				
		dialog.setValidator(new OverrideMethodValidator(inheritedMethods.length));
		dialog.setTitle(ActionMessages.getString("OverrideMethodDialog.dialog.title")); //$NON-NLS-1$
		dialog.setInitialSelections(toImplementArray);
		dialog.setExpandedElements(expanded.toArray());
		dialog.setContainerMode(true);
		dialog.setSorter(sorter);
		dialog.setSize(60, 18);			
		dialog.setInput(new Object());
		dialog.setMessage(null);

		IMethod[] selected= null;
		int dialogResult= dialog.open();
		if (dialogResult == Window.OK) {		
			Object[] checkedElements= dialog.getResult();
			if (checkedElements == null)
				return;
			ArrayList result= new ArrayList(checkedElements.length);
			for (int i= 0; i < checkedElements.length; i++) {
				Object curr= checkedElements[i];
				if (curr instanceof IMethod) {
					result.add(curr);
				}
			}
			selected= (IMethod[]) result.toArray(new IMethod[result.size()]);
			
			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
			settings.createComments= dialog.getGenerateComment();
	
			IJavaElement elementPosition= dialog.getElementPosition();
			AddUnimplementedMethodsOperation op= new AddUnimplementedMethodsOperation(type, settings, selected, false, elementPosition);
			
			IRewriteTarget target= editor != null ? (IRewriteTarget) editor.getAdapter(IRewriteTarget.class) : null;
			if (target != null) {
				target.beginCompoundChange();		
			}
			try {
				BusyIndicatorRunnableContext context= new BusyIndicatorRunnableContext();
				context.run(false, true, new WorkbenchRunnableAdapter(op));
				IMethod[] res= op.getCreatedMethods();
				if (res == null || res.length == 0) {
					MessageDialog.openInformation(shell, getDialogTitle(), ActionMessages.getString("OverrideMethodsAction.error.nothing_found")); //$NON-NLS-1$
				} else if (editor != null) {
					EditorUtility.revealInEditor(editor, res[0]);
				}
			} catch (InvocationTargetException e) {
				ExceptionHandler.handle(e, shell, getDialogTitle(), null); 
			} catch (InterruptedException e) {
				// Do nothing. Operation has been canceled by user.
			} finally {
				if (target != null) {
					target.endCompoundChange();		
				}
			}
		}
	}
		
	private IType getSelectedType(IStructuredSelection selection) throws JavaModelException {
		Object[] elements= selection.toArray();
		if (elements.length == 1 && (elements[0] instanceof IType)) {
			IType type= (IType) elements[0];
			if (type.getCompilationUnit() != null && type.isClass()) {
				return type;
			}
		}
		return null;
	}
	
	private String getDialogTitle() {
		return ActionMessages.getString("OverrideMethodsAction.error.title"); //$NON-NLS-1$
	}	
	
	public static class OverrideMethodContentProvider implements ITreeContentProvider {
	
		private final String SETTINGS_SECTION= "OverrideMethodDialog"; //$NON-NLS-1$
		private final String SETTINGS_SHOWTYPES= "showtypes"; //$NON-NLS-1$
	
		private Object[] fTypes;
		private IMethod[] fMethods;
		private final Object[] fEmpty= new Object[0];
			
		private boolean fShowTypes;
		private Viewer fViewer;
		private IDialogSettings fSettings;
	
		/**
		 * Constructor for OverrideMethodContentProvider.
		 */
		public OverrideMethodContentProvider(IMethod[] methods, Object[] types) {
			fMethods= methods;
			fTypes= types;
			IDialogSettings dialogSettings= JavaPlugin.getDefault().getDialogSettings();
			fSettings= dialogSettings.getSection(SETTINGS_SECTION);
			if (fSettings == null) {
				fSettings= dialogSettings.addNewSection(SETTINGS_SECTION);
				fSettings.put(SETTINGS_SHOWTYPES, true);
			}
			fShowTypes= fSettings.getBoolean(SETTINGS_SHOWTYPES);
		}			
				
		/*
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof IType) {
				ArrayList result= new ArrayList(fMethods.length);
				for (int i= 0; i < fMethods.length; i++) {
					if (fMethods[i].getDeclaringType().equals(parentElement)) {
						result.add(fMethods[i]);
					}
				}
				return result.toArray();
			}
			return fEmpty;
		}
	
		/*
		 * @see ITreeContentProvider#getParent(Object)
		 */
		public Object getParent(Object element) {
			if (element instanceof IMethod) {
				return ((IMethod)element).getDeclaringType();
			}
			return null;
		}
	
		/*
		 * @see ITreeContentProvider#hasChildren(Object)
		 */
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}
	
		/*
		 * @see IStructuredContentProvider#getElements(Object)
		 */
		public Object[] getElements(Object inputElement) {
			return fShowTypes ? fTypes : fMethods;
		}
	
		/*
		 * @see IContentProvider#dispose()
		 */
		public void dispose() {
		}
	
		/*
		 * @see IContentProvider#inputChanged(Viewer, Object, Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			fViewer= viewer;
		}
			
			
		public boolean isShowTypes() {
			return fShowTypes;
		}
	
		public void setShowTypes(boolean showTypes) {
			if (fShowTypes != showTypes) {
				fShowTypes= showTypes;
				fSettings.put(SETTINGS_SHOWTYPES, showTypes);
				if (fViewer != null) {
					fViewer.refresh();
				}
			}
		}												
	}
	
	public static class OverrideTreeSelectionDialog extends SourceActionDialog {
		
		protected static OverrideMethodContentProvider fContentProvider;

		public OverrideTreeSelectionDialog(Shell parent, OverrideMethodContentProvider cp, JavaElementLabelProvider lp, CompilationUnitEditor editor, IType type) {
			super(parent, lp, cp, editor, type);
			fContentProvider= cp;			
		}

		/*
		 * @see CheckedTreeSelectionDialog#createTreeViewer(Composite)
		 */
		protected CheckboxTreeViewer createTreeViewer(Composite composite) {
			initializeDialogUnits(composite);
			ViewerPane pane= new ViewerPane(composite, SWT.BORDER | SWT.FLAT);
			pane.setText(ActionMessages.getString("OverrideMethodDialog.dialog.description")); //$NON-NLS-1$
			ToolBarManager tbm= pane.getToolBarManager();
			tbm.add(new OverrideFlatTreeAction());
			tbm.update(true);			
		
			CheckboxTreeViewer treeViewer= super.createTreeViewer(pane);
			pane.setContent(treeViewer.getControl());
			GridLayout paneLayout= new GridLayout();
			paneLayout.marginHeight= 0;
			paneLayout.marginWidth= 0;
			paneLayout.numColumns= 1;
			pane.setLayout(paneLayout);
			GridData gd= new GridData(GridData.FILL_BOTH);
			gd.widthHint = convertWidthInCharsToPixels(55);
			gd.heightHint = convertHeightInCharsToPixels(15);
			pane.setLayoutData(gd);
						
			return treeViewer;		
		}		
		
		/*
		 * @see org.eclipse.jface.window.Window#configureShell(Shell)
		 */
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.OVERRIDE_TREE_SELECTION_DIALOG);
		}
		
		private class OverrideFlatTreeAction extends Action {
			private boolean fToggle;
			
			public OverrideFlatTreeAction() {
				super(ActionMessages.getString("OverrideMethodDialog.groupMethodsByTypes")); //$NON-NLS-1$
				setDescription(ActionMessages.getString("OverrideMethodDialog.groupMethodsByTypes")); //$NON-NLS-1$
				setToolTipText(ActionMessages.getString("OverrideMethodDialog.groupMethodsByTypes")); //$NON-NLS-1$
		
				JavaPluginImages.setLocalImageDescriptors(this, "impl_co.gif"); //$NON-NLS-1$
				IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
				fToggle= preferenceStore.getBoolean("OverrideMethodDialogTree.isFlat"); //$NON-NLS-1$
				fContentProvider.setShowTypes(fToggle);
				setChecked(fToggle);
			}

			public void run() {
				IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore(); 
				fContentProvider.setShowTypes(!fToggle);
				preferenceStore.setValue("OverrideMethodDialogTree.isFlat", !fToggle); //$NON-NLS-1$
				fToggle= !fToggle;
				setChecked(fToggle);
			}
		}	
	}
	
	public static class OverrideMethodSorter extends ViewerSorter {
		
		private IType[] fAllTypes;
		
		public OverrideMethodSorter(ITypeHierarchy typeHierarchy) {
			IType curr= typeHierarchy.getType();
			IType[] superTypes= typeHierarchy.getAllSupertypes(curr);
			fAllTypes= new IType[superTypes.length + 1];
			fAllTypes[0]= curr;
			System.arraycopy(superTypes, 0, fAllTypes, 1, superTypes.length);
		}
		
		/*
		 * @see ViewerSorter#compare(Viewer, Object, Object)
		 */
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1 instanceof IType && e2 instanceof IType) {
				if (e1.equals(e2)) {
					return 0;
				}
				for (int i= 0; i < fAllTypes.length; i++) {
					IType curr= fAllTypes[i];
					if (curr.equals(e1)) {
						return -1;
					}
					if (curr.equals(e2)) {
						return 1;
					}	
				}
				return 0;
			} else {
				return super.compare(viewer, e1, e2);
			}
		}
		
	}
		
	public static class OverrideMethodValidator implements ISelectionStatusValidator {
			
		private static int fNumMethods;
			
		public OverrideMethodValidator(int entries) {
			super();
			fNumMethods= entries;
		}		
			
		/*
		 * @see ISelectionValidator#validate(Object[])
		 */
		public IStatus validate(Object[] selection) {
			int count= 0;
			for (int i= 0; i < selection.length; i++) {
				if (selection[i] instanceof IMethod) {
					count++;
				}
			}
			if (count == 0) {
				return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
			}
			String message;
			message= ActionMessages.getFormattedString("OverrideMethodDialog.selectioninfo.more", //$NON-NLS-1$
														new Object[] { String.valueOf(count), String.valueOf(fNumMethods)} ); 

			return new StatusInfo(IStatus.INFO, message);
		}
		
	}		

}
