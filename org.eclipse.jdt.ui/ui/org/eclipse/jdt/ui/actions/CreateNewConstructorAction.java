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
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.corext.codemanipulation.AddCustomConstructorOperation;
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
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.ElementValidator;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;


public class CreateNewConstructorAction extends SelectionDispatchAction {
	
	private CompilationUnitEditor fEditor;
	private static final String fDialogTitle= ActionMessages.getString("CreateNewConstructorAction.error.title"); //$NON-NLS-1$
	
	/**
	 * Creates a new <code>CreateNewConstructorAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public CreateNewConstructorAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.getString("CreateNewConstructorAction.label")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("CreateNewConstructorAction.description")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("CreateNewConstructorAction.tooltip")); //$NON-NLS-1$
		
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.CREATE_NEW_CONSTRUCTOR_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public CreateNewConstructorAction(CompilationUnitEditor editor) {
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
		if (getSelectedFields(selection) != null)
			return true;

		if ((selection.size() == 1) && (selection.getFirstElement() instanceof IType)) {
			IType type= (IType) selection.getFirstElement();
			return type.getCompilationUnit() != null && type.isClass(); // look if class: not cheap but done by all source generation actions
		}

		if ((selection.size() == 1) && (selection.getFirstElement() instanceof ICompilationUnit))
			return true;

		return false;
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	public void run(IStructuredSelection selection) {
		try {
			IField[] selectedFields= getSelectedFields(selection);
			// open an editor and work on a working copy
			IEditorPart editor= null;
			if (selectedFields != null)
				editor= EditorUtility.openInEditor(selectedFields[0]);			
			else
				editor= EditorUtility.openInEditor(getSelectedType(selection).getCompilationUnit());	
			
			if (canRunOn(selectedFields)){
				run((IType)EditorUtility.getWorkingCopy(selectedFields[0].getDeclaringType()), selectedFields, editor, false);
				return;
			}	
			Object firstElement= selection.getFirstElement();

			if (firstElement instanceof IType)
				run((IType)EditorUtility.getWorkingCopy((IType)firstElement), new IField[0], editor, false);
			else if (firstElement instanceof ICompilationUnit)	{			
				IType type= ((ICompilationUnit) firstElement).findPrimaryType();
				if (type.isInterface()) {
					MessageDialog.openInformation(getShell(), fDialogTitle, ActionMessages.getString("CreateNewConstructorAction.interface_not_applicable")); //$NON-NLS-1$					
					return;
				}
				else 
					run((IType)EditorUtility.getWorkingCopy(((ICompilationUnit) firstElement).findPrimaryType()), new IField[0], editor, false);
			}
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), fDialogTitle, ActionMessages.getString("CreateNewConstructorAction.error.actionfailed")); //$NON-NLS-1$
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
		else if (elements[0] instanceof ICompilationUnit) {
			ICompilationUnit cu= (ICompilationUnit) elements[0];
			IType type= cu.findPrimaryType();
			if (!type.isInterface())
				return type;
		}
		return null;
	}
	
	private static boolean canRunOn(IField[] fields) throws JavaModelException {
		return fields != null && fields.length > 0;
	}
	
	/*
	 * Returns fields in the selection or <code>null</code> if the selection is 
	 * empty or not valid.
	 */
	private IField[] getSelectedFields(IStructuredSelection selection) {
		List elements= selection.toList();
		int nElements= elements.size();
		if (nElements > 0) {
			IField[] res= new IField[nElements];
			ICompilationUnit cu= null;
			for (int i= 0; i < nElements; i++) {
				Object curr= elements.get(i);
				if (curr instanceof IField) {
					IField fld= (IField)curr;
					
					if (i == 0) {
						// remember the cu of the first element
						cu= fld.getCompilationUnit();
						if (cu == null) {
							return null;
						}
					} else if (!cu.equals(fld.getCompilationUnit())) {
						// all fields must be in the same CU
						return null;
					}
					try {
						if (fld.getDeclaringType().isInterface()) {
							// no constructors for interfaces
							return null;
						}
					} catch (JavaModelException e) {
						JavaPlugin.log(e);
						return null;
					}
					
					res[i]= fld;
				} else {
					return null;
				}
			}
			return res;
		}
		return null;
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

		try {
			IJavaElement[] elements= SelectionConverter.codeResolve(fEditor);			
			if (elements.length == 1 && (elements[0] instanceof IField)) {
				IField field= (IField)elements[0];
				run(field.getDeclaringType(), new IField[] {field}, fEditor, false);
				return;
			}
			IJavaElement element= SelectionConverter.getElementAtOffset(fEditor);
			
			if (element != null){
				IType type= (IType)element.getAncestor(IJavaElement.TYPE);
				if (type != null) {
					if (type.getFields().length > 0) {
						run(type, new IField[0], fEditor, true);
						return;
					} 
				}
			} 
			MessageDialog.openInformation(getShell(), fDialogTitle, 
				ActionMessages.getString("CreateNewConstructorAction.not_applicable")); //$NON-NLS-1$
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), null);
		}		
	}
	
	private boolean checkEnabledEditor() {
		return fEditor != null && SelectionConverter.canOperateOn(fEditor);
	}	
	
	//---- Helpers -------------------------------------------------------------------
	
	private void run(IType type, IField[] preselected, IEditorPart editor, boolean activatedFromEditor) throws CoreException {
		if (!ElementValidator.check(type, getShell(), getDialogTitle(), activatedFromEditor)) {
			return;
		}
		if (!ActionUtil.isProcessable(getShell(), type)) {
			return;		
		}		

		IField[] constructorFields= type.getFields();
		ArrayList constructorFieldsList= new ArrayList();
		for (int i= 0; i < constructorFields.length; i++) {
			constructorFieldsList.add(constructorFields[i]);
		}
		if (constructorFieldsList.isEmpty()){
			MessageDialog.openInformation(getShell(), fDialogTitle, ActionMessages.getString("CreateNewConstructorAction.typeContainsNoFields.message")); //$NON-NLS-1$
			return;
		}
		
		JavaElementLabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		CreateNewConstructorContentProvider contentProvider = new CreateNewConstructorContentProvider(constructorFieldsList);			
		CreateNewConstructorSelectionDialog dialog= new CreateNewConstructorSelectionDialog(getShell(), labelProvider, contentProvider, fEditor, type);
		dialog.setCommentString(ActionMessages.getString("SourceActionDialog.createConstructorComment")); //$NON-NLS-1$
		dialog.setTitle(getDialogTitle());
		dialog.setInitialSelections(preselected);
		dialog.setContainerMode(true);
		dialog.setSize(60, 18);			
		dialog.setInput(new Object());
		dialog.setMessage(ActionMessages.getString("CreateNewConstructorAction.dialog.label")); //$NON-NLS-1$
		dialog.setValidator(createValidator(constructorFields.length));
		
		IField[] selected= null;
		int dialogResult = dialog.open();
		if (dialogResult == Window.OK) {			
			Object[] checkedElements = dialog.getResult();
			if (checkedElements == null)
				return;
			ArrayList result= new ArrayList(checkedElements.length);
			for (int i= 0; i < checkedElements.length; i++) {
				Object curr= checkedElements[i];
				if (curr instanceof IField) {
					result.add(curr);
				}
			}
						
			selected= (IField[]) result.toArray(new IField[result.size()]);
			
			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
			settings.createComments= dialog.getGenerateComment();
	
			IJavaElement elementPosition= dialog.getElementPosition();
			int superIndex= dialog.getSuperIndex();
			AddCustomConstructorOperation op= new AddCustomConstructorOperation(type, settings, selected, false, elementPosition, superIndex);

			IRewriteTarget target= editor != null ? (IRewriteTarget) editor.getAdapter(IRewriteTarget.class) : null;
			if (target != null) {
				target.beginCompoundChange();		
			}
			try {
				IRunnableContext context= JavaPlugin.getActiveWorkbenchWindow();
				if (context == null) {
					context= new BusyIndicatorRunnableContext();
				}				
				context.run(false, true, new WorkbenchRunnableAdapter(op));
				IMethod res= op.getCreatedConstructor();

				if (res.getCompilationUnit().isWorkingCopy())  {
					synchronized(res.getCompilationUnit())  {
						res.getCompilationUnit().reconcile();
					}
				}
				EditorUtility.revealInEditor(editor, res);

			} catch (InvocationTargetException e) {
				ExceptionHandler.handle(e, getShell(), getDialogTitle(), null); 
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
		CreateNewConstructorValidator validator= new CreateNewConstructorValidator(entries);
		return validator;
	}
	
	private String getDialogTitle() {
		return fDialogTitle;
	}	

	private static class CreateNewConstructorContentProvider implements ITreeContentProvider {
		
		private List fFieldsList;
		private static final Object[] EMPTY= new Object[0];
		
		public CreateNewConstructorContentProvider(List fieldList) {
			fFieldsList= fieldList;
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
			return fFieldsList.toArray();
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

		private List moveUp(List elements, List move) {
			int nElements= elements.size();
			List res= new ArrayList(nElements);
			Object floating= null;
			for (int i= 0; i < nElements; i++) {
				Object curr= elements.get(i);
				if (move.contains(curr)) {
					res.add(curr);
				} else {
					if (floating != null) {
						res.add(floating);
					}
					floating= curr;
				}
			}
			if (floating != null) {
				res.add(floating);
			}
			return res;
		}
		
		private List reverse(List p) {
			List reverse= new ArrayList(p.size());
			for (int i= p.size() - 1; i >= 0; i--) {
				reverse.add(p.get(i));
			}
			return reverse;
		}
				
		public void setElements(List elements, CheckboxTreeViewer tree) {
			fFieldsList= new ArrayList(elements);
			if (tree != null)
				tree.refresh();
		}		

		public void up(List checkedElements, CheckboxTreeViewer tree) {
			if (checkedElements.size() > 0) {
				setElements(moveUp(fFieldsList, checkedElements), tree);
				tree.reveal(checkedElements.get(0));
			}			
		}		

		public void down(List checkedElements, CheckboxTreeViewer tree) {
			if (checkedElements.size() > 0) {
				setElements(reverse(moveUp(reverse(fFieldsList), checkedElements)), tree);
				tree.reveal(checkedElements.get(checkedElements.size() - 1));
			}
		}
	}
	
	private static class CreateNewConstructorValidator implements ISelectionStatusValidator {
		private static int fEntries;
			
		CreateNewConstructorValidator(int entries) {
			super();
			fEntries= entries;
		}

		public IStatus validate(Object[] selection) {
			int count= countSelectedFields(selection);

			String message= ActionMessages.getFormattedString("CreateNewConstructorAction.fields_selected", //$NON-NLS-1$
																new Object[] { String.valueOf(count), String.valueOf(fEntries)} ); 																	
			return new StatusInfo(IStatus.INFO, message);
		}

		private int countSelectedFields(Object[] selection){
			int count= 0;
			for (int i = 0; i < selection.length; i++) {
				if (selection[i] instanceof IField)
					count++;
			}
			return count;
		}		
	}	
	
	private static class CreateNewConstructorSelectionDialog extends SourceActionDialog {
		private CreateNewConstructorContentProvider fContentProvider;
		private IType fType;
		private int fSuperIndex;
		private int fWidth = 60;
		private int fHeight = 18;
		
		private static final int UP_BUTTON= IDialogConstants.CLIENT_ID + 1;
		private static final int DOWN_BUTTON= IDialogConstants.CLIENT_ID + 2;		
		
		public CreateNewConstructorSelectionDialog(Shell parent, ILabelProvider labelProvider, CreateNewConstructorContentProvider contentProvider, CompilationUnitEditor editor, IType type) {
			super(parent, labelProvider, contentProvider, editor, type);
			fContentProvider= contentProvider;
			fType= type;
		}	
		
		protected Control createDialogArea(Composite parent) {
			initializeDialogUnits(parent);
			
			Composite composite = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			GridData gd= null;
		
			layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
			layout.marginWidth= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
			layout.verticalSpacing=	convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
			layout.horizontalSpacing= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);			
			composite.setLayout(layout);
			composite.setFont(parent.getFont());	
						
			Label messageLabel = createMessageArea(composite);			
			if (messageLabel != null) {
				gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
				gd.horizontalSpan= 2;
				messageLabel.setLayoutData(gd);	
			}
			
			Composite classConstructorComposite= addSuperClassConstructorChoices(composite);
			gd= new GridData(GridData.FILL_BOTH);
			classConstructorComposite.setLayoutData(gd);
			
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
			entryComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
			Composite commentComposite= createCommentSelection(composite);
			commentComposite.setLayoutData(new GridData(GridData.FILL_BOTH));		

			gd= new GridData(GridData.FILL_BOTH);
			composite.setLayoutData(gd);
			
			return composite;
		}
		
		protected Composite createSelectionButtons(Composite composite) {
			Composite buttonComposite= super.createSelectionButtons(composite);

			GridLayout layout = new GridLayout();
			buttonComposite.setLayout(layout);						

			createUpDownButtons(buttonComposite);
			
			layout.marginHeight= 0;
			layout.marginWidth= 0;						
			layout.numColumns= 1;
			
			return buttonComposite;
		}
		
		protected void createUpDownButtons(Composite buttonComposite) {
			createButton(buttonComposite, UP_BUTTON, ActionMessages.getString("CreateNewConstructorSelectionDialog.up_button"), false); //$NON-NLS-1$	
			createButton(buttonComposite, DOWN_BUTTON, ActionMessages.getString("CreateNewConstructorSelectionDialog.down_button"), false); //$NON-NLS-1$				
		}
		
		protected void buttonPressed(int buttonId) {
			super.buttonPressed(buttonId);
			switch(buttonId) {
				case UP_BUTTON: {
					fContentProvider.up(getElementList(), getTreeViewer());
					updateOKStatus();						
					break;
				}
				case DOWN_BUTTON: {
					fContentProvider.down(getElementList(), getTreeViewer());
					updateOKStatus();									
					break;
				}
			}
		}
		
		private List getElementList() {
			IStructuredSelection selection= (IStructuredSelection) getTreeViewer().getSelection();
			List elements= selection.toList();						
			ArrayList elementList= new ArrayList();

			for (int i= 0; i < elements.size(); i++) {
				elementList.add(elements.get(i));		
			}
			return elementList;
		}		

		protected Composite createEntryPtCombo(Composite composite) {
			Composite entryComposite= super.createEntryPtCombo(composite);						
			return entryComposite;						
		}
		
		private Composite addSuperClassConstructorChoices(Composite composite) {
			try {
				Label label= new Label(composite, SWT.NONE);
				label.setText(ActionMessages.getString("CreateNewConstructorSelectionDialog.sort_constructor_choices.label")); //$NON-NLS-1$
				GridData gd= new GridData(GridData.FILL_BOTH);
				label.setLayoutData(gd);
				
				final Combo combo= new Combo(composite, SWT.READ_ONLY);
				IMethod[] constructorMethods= StubUtility.getOverridableConstructors(fType);					
				
				for (int i= 0; i < constructorMethods.length; i++) {
					combo.add(JavaElementLabels.getElementLabel(constructorMethods[i], JavaElementLabels.M_PARAMETER_TYPES));
				}
				// TODO: Can we be a little more intelligent about guessing the super() ?
				combo.setText(combo.getItem(0));
				combo.setLayoutData(new GridData(GridData.FILL_BOTH));
				combo.addSelectionListener(new SelectionAdapter(){
					public void widgetSelected(SelectionEvent e) {
						fSuperIndex= combo.getSelectionIndex();
					}
				});	

			} catch (CoreException e) {
			}
			return composite;
		}		
				
		public int getSuperIndex() {
			return fSuperIndex;
		}

	}
	
}
