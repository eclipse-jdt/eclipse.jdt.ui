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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementSorter;

import org.eclipse.jdt.internal.corext.codemanipulation.AddUnimplementedConstructorsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.dialogs.SourceActionDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.IVisibilityChangeListener;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.ElementValidator;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Creates unimplemented constructors for a type.
 * <p>
 * Will open the parent compilation unit in a Java editor. Opens a dialog
 * with a list of constructors from the super class which can be generated.
 * User is able to check or uncheck items before constructors are generated.
 * The result is unsaved, so the user can decide if the changes are acceptable.
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
public class AddUnimplementedConstructorsAction extends SelectionDispatchAction {
	
	private CompilationUnitEditor fEditor;
	private static final String DIALOG_TITLE= ActionMessages.getString("AddUnimplementedConstructorsAction.error.title"); //$NON-NLS-1$

	/**
	 * Creates a new <code>AddUnimplementedConstructorsAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public AddUnimplementedConstructorsAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.getString("AddUnimplementedConstructorsAction.label")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("AddUnimplementedConstructorsAction.description")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("AddUnimplementedConstructorsAction.tooltip")); //$NON-NLS-1$
		
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.ADD_UNIMPLEMENTED_CONSTRUCTORS_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public AddUnimplementedConstructorsAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(checkEnabledEditor());
	}
	
	//---- Structured Viewer -----------------------------------------------------------
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(canEnable(selection));
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e);
			setEnabled(false);
		}
	}
	
	private boolean canEnable(IStructuredSelection selection) throws JavaModelException {
		if ((selection.size() == 1) && (selection.getFirstElement() instanceof IType)) {
			IType type= (IType) selection.getFirstElement();
			return type.getCompilationUnit() != null && type.isClass() && !type.isLocal(); // look if class: not cheap but done by all source generation actions
		}

		if ((selection.size() == 1) && (selection.getFirstElement() instanceof ICompilationUnit))
			return true;

		return false;
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	public void run(IStructuredSelection selection) {
		Shell shell= getShell();
		try {
			IType type= getSelectedType(selection);
			if (type == null) {
				MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("AddUnimplementedConstructorsAction.not_applicable")); //$NON-NLS-1$
				return;
			}		
			
			if (type == null) {
				MessageDialog.openError(shell, getDialogTitle(), ActionMessages.getString("AddUnimplementedConstructorsAction.error.type_removed_in_editor")); //$NON-NLS-1$
				return;
			}
			
			run(shell, type, false);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, shell, getDialogTitle(), null);
		}			
	}

	//---- Java Editior --------------------------------------------------------------
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	public void selectionChanged(ITextSelection selection) {
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	public void run(ITextSelection selection) {
		Shell shell= getShell();
		try {
			IType type= SelectionConverter.getTypeAtOffset(fEditor);
			if (type != null)
				run(shell, type, true);
			else
				MessageDialog.openInformation(shell, getDialogTitle(), ActionMessages.getString("AddUnimplementedConstructorsAction.not_applicable")); //$NON-NLS-1$
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), null);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), null);
		}
	}
	
	private boolean checkEnabledEditor() {
		return fEditor != null && SelectionConverter.canOperateOn(fEditor);
	}	
	
	//---- Helpers -------------------------------------------------------------------
	
	private void run(Shell shell, IType type, boolean activatedFromEditor) throws CoreException {
		if (!ElementValidator.check(type, getShell(), getDialogTitle(), activatedFromEditor)) {
			return;
		}
		if (!ActionUtil.isProcessable(getShell(), type)) {
			return;		
		}		

		IMethod[] constructorMethods= StubUtility.getOverridableConstructors(type);
		
		if (constructorMethods.length == 0) {
			MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("AddUnimplementedConstructorsAction.error.nothing_found")); //$NON-NLS-1$
			return;
		}
		
		JavaElementLabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		AddUnimplementedConstructorsContentProvider contentProvider = new AddUnimplementedConstructorsContentProvider(constructorMethods);			

		AddUnimplementedConstructorsDialog dialog= new AddUnimplementedConstructorsDialog(shell, labelProvider, contentProvider, fEditor, type);
		dialog.setCommentString(ActionMessages.getString("SourceActionDialog.createConstructorComment")); //$NON-NLS-1$
		dialog.setTitle(ActionMessages.getString("AddUnimplementedConstructorsAction.dialog.title")); //$NON-NLS-1$
		dialog.setInitialSelections(constructorMethods);
		dialog.setContainerMode(true);
		dialog.setSorter(new JavaElementSorter());
		dialog.setSize(60, 18);			
		dialog.setInput(new Object());
		dialog.setMessage(ActionMessages.getString("AddUnimplementedConstructorsAction.dialog.label")); //$NON-NLS-1$
		dialog.setValidator(createValidator(constructorMethods.length));
		
		IMethod[] selected= null;
		int dialogResult = dialog.open();
		if (dialogResult == Window.OK) {			
			Object[] checkedElements = dialog.getResult();
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
	
			IEditorPart editor= EditorUtility.openInEditor(type);

			IJavaElement elementPosition= dialog.getElementPosition();
						
			IRewriteTarget target= editor != null ? (IRewriteTarget) editor.getAdapter(IRewriteTarget.class) : null;
			if (target != null) {
				target.beginCompoundChange();		
			}
			try {
				AddUnimplementedConstructorsOperation op= new AddUnimplementedConstructorsOperation(type, settings, selected, false, elementPosition);
				op.setVisbility(dialog.getVisibilityModifier());
				op.setOmitSuper(dialog.isOmitSuper());

				IRunnableContext context= JavaPlugin.getActiveWorkbenchWindow();
				if (context == null) {
					context= new BusyIndicatorRunnableContext();
				}				
				context.run(false, true, new WorkbenchRunnableAdapter(op, type.getResource()));
				IMethod[] res= op.getCreatedMethods();
				if (res == null || res.length == 0) {
					MessageDialog.openInformation(shell, getDialogTitle(), ActionMessages.getString("AddUnimplementedConstructorsAction.error.nothing_found")); //$NON-NLS-1$
				} else if (editor != null) {
					JavaModelUtil.reconcile(res[0].getCompilationUnit());
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
	
	private static ISelectionStatusValidator createValidator(int entries) {
		AddUnimplementedConstructorsValidator validator= new AddUnimplementedConstructorsValidator(entries);
		return validator;
	}
		
	private IType getSelectedType(IStructuredSelection selection) throws JavaModelException {
		Object[] elements= selection.toArray();
		if (elements.length == 1 && (elements[0] instanceof IType)) {
			IType type= (IType) elements[0];
			if (type.getCompilationUnit() != null && type.isClass()) {
				return type;
			}
		}
		else if (elements[0] instanceof ICompilationUnit) {
			ICompilationUnit cu= (ICompilationUnit) elements[0];
			IType type= cu.findPrimaryType();
			if (type != null && !type.isInterface())
				return type;
		}
		return null;
	}	
	
	private String getDialogTitle() {
		return DIALOG_TITLE;
	}	

	private static class AddUnimplementedConstructorsContentProvider implements ITreeContentProvider {
		
		private IMethod[] fMethodsList;
		private static final Object[] EMPTY= new Object[0];
		
		public AddUnimplementedConstructorsContentProvider(IMethod[] methodList) {
			fMethodsList= methodList;
		}
		
		/*
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		public Object[] getChildren(Object parentElement) {
			return EMPTY;
		}

		/*
		 * @see ITreeContentProvider#getParent(Object)
		 */
		public Object getParent(Object element) {
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
			return fMethodsList;
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
		}
									
	}
	
	private static class AddUnimplementedConstructorsValidator implements ISelectionStatusValidator {
		private static int fEntries;
			
		AddUnimplementedConstructorsValidator(int entries) {
			super();
			fEntries= entries;
		}

		public IStatus validate(Object[] selection) {
			int count= countSelectedMethods(selection);
			if (count == 0)
				return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
			String message= ActionMessages.getFormattedString("AddUnimplementedConstructorsAction.methods_selected", //$NON-NLS-1$
																new Object[] { String.valueOf(count), String.valueOf(fEntries)} ); 																	
			return new StatusInfo(IStatus.INFO, message);
		}

		private int countSelectedMethods(Object[] selection){
			int count= 0;
			for (int i = 0; i < selection.length; i++) {
				if (selection[i] instanceof IMethod)
					count++;
			}
			return count;
		}		
	}	
	
	private static class AddUnimplementedConstructorsDialog extends SourceActionDialog {
		private boolean fOmitSuper;
		private int fWidth= 60;
		private int fHeight= 18;
		private IDialogSettings fAddConstructorsSettings;
		
		private final String SETTINGS_SECTION= "AddUnimplementedConstructorsDialog"; //$NON-NLS-1$
		private final String OMIT_SUPER="OmitCallToSuper"; //$NON-NLS-1$

		public AddUnimplementedConstructorsDialog(Shell parent, ILabelProvider labelProvider, ITreeContentProvider contentProvider, CompilationUnitEditor editor, IType type) {
			super(parent, labelProvider, contentProvider, editor, type);
			
			IDialogSettings dialogSettings= JavaPlugin.getDefault().getDialogSettings();
			fAddConstructorsSettings= dialogSettings.getSection(SETTINGS_SECTION);
			if (fAddConstructorsSettings == null) {
				fAddConstructorsSettings= dialogSettings.addNewSection(SETTINGS_SECTION);
				fAddConstructorsSettings.put(OMIT_SUPER, false); //$NON-NLS-1$
			}				
			
			fOmitSuper= fAddConstructorsSettings.getBoolean(OMIT_SUPER);
		}
	
		protected Composite createEntryPtCombo(Composite composite) {
			Composite entryComposite= super.createEntryPtCombo(composite);			
			addVisibilityAndModifiersChoices(entryComposite);
				
			return entryComposite;						
		}
		
		protected Composite createVisibilityControlAndModifiers(Composite parent, final IVisibilityChangeListener visibilityChangeListener, int[] availableVisibilities, int correctVisibility) {
			Composite visibilityComposite= createVisibilityControl(parent, visibilityChangeListener, availableVisibilities, correctVisibility);	
			return visibilityComposite;			
		}
		
		public boolean isOmitSuper() {
			return fOmitSuper;
		}

		public void setOmitSuper(boolean omitSuper) {
			if (fOmitSuper != omitSuper)  {
				fOmitSuper= omitSuper;
				fAddConstructorsSettings.put(OMIT_SUPER, omitSuper);
			}		
		}
				
		private Composite createOmitSuper(Composite composite) {
			Composite omitSuperComposite= new Composite(composite, SWT.NONE);
			GridLayout layout= new GridLayout();
			layout.marginHeight= 0;
			layout.marginWidth= 0;
			omitSuperComposite.setLayout(layout);
			omitSuperComposite.setFont(composite.getFont());
			
			Button omitSuperButton= new Button(omitSuperComposite, SWT.CHECK);
			omitSuperButton.setText(ActionMessages.getString("AddUnimplementedConstructorsDialog.omit.super")); //$NON-NLS-1$
			omitSuperButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

			omitSuperButton.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					boolean isSelected= (((Button) e.widget).getSelection());
					setOmitSuper(isSelected);
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
			});
			omitSuperButton.setSelection(isOmitSuper());
			GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
			gd.horizontalSpan= 2;
			omitSuperButton.setLayoutData(gd);

			return omitSuperComposite;
		}

		protected Control createDialogArea(Composite parent) {
			initializeDialogUnits(parent);
			
			Composite composite= new Composite(parent, SWT.NONE);
			GridLayout layout= new GridLayout();
			GridData gd= null;
		
			layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
			layout.marginWidth= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
			layout.verticalSpacing=	convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
			layout.horizontalSpacing= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);			
			composite.setLayout(layout);
						
			Label messageLabel = createMessageArea(composite);			
			if (messageLabel != null) {
				gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
				gd.horizontalSpan= 2;
				messageLabel.setLayoutData(gd);	
			}
			
			Composite inner= new Composite(composite, SWT.NONE);
			GridLayout innerLayout = new GridLayout();
			innerLayout.numColumns= 2;
			innerLayout.marginHeight= 0;
			innerLayout.marginWidth= 0;
			inner.setLayout(innerLayout);
			inner.setFont(parent.getFont());		
			
			CheckboxTreeViewer treeViewer= createTreeViewer(inner);
			gd= new GridData(GridData.FILL_BOTH);
			gd.widthHint = convertWidthInCharsToPixels(fWidth);
			gd.heightHint = convertHeightInCharsToPixels(fHeight);
			treeViewer.getControl().setLayoutData(gd);			
					
			Composite buttonComposite= createSelectionButtons(inner);
			gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
			buttonComposite.setLayoutData(gd);
			
			gd= new GridData(GridData.FILL_BOTH);
			inner.setLayoutData(gd);
		
			Composite entryComposite= createEntryPtCombo(composite); 
			entryComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
			Composite commentComposite= createCommentSelection(composite);
			commentComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));		
			
			Composite overrideSuperComposite= createOmitSuper(composite);
			overrideSuperComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			gd= new GridData(GridData.FILL_BOTH);
			composite.setLayoutData(gd);
			
			applyDialogFont(composite);
			
			return composite;
		}

	}
}
