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
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
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
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.corext.codemanipulation.AddCustomConstructorOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.dom.TokenScanner;
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
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

/**
 * Creates constructors for a type based on existing fields.
 * <p>
 * Will open the parent compilation unit in a Java editor. Opens a dialog
 * with a list fields from which a constructor will be generated.
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
 * @since 3.0
 */
public class GenerateNewConstructorUsingFieldsAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;
	private static final String DIALOG_TITLE= ActionMessages.getString("GenerateConstructorUsingFieldsAction.error.title"); //$NON-NLS-1$
	private static final int UP_INDEX= 0;
	private static final int DOWN_INDEX= 1;

	/**
	 * Creates a new <code>GenerateConstructorUsingFieldsAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public GenerateNewConstructorUsingFieldsAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.getString("GenerateConstructorUsingFieldsAction.label")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("GenerateConstructorUsingFieldsAction.description")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("GenerateConstructorUsingFieldsAction.tooltip")); //$NON-NLS-1$

		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.CREATE_NEW_CONSTRUCTOR_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public GenerateNewConstructorUsingFieldsAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(checkEnabledEditor());
	}

	//---- Structured Viewer -----------------------------------------------------------

	private static String getDialogTitle() {
		return DIALOG_TITLE;
	}

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
			return type.getCompilationUnit() != null && type.isClass() && !type.isLocal();
			// look if class: not cheap but done by all source generation actions
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
			IType selectionType= getSelectedType(selection);
			if (selectionType == null) {
				MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.not_applicable")); //$NON-NLS-1$
				return;
			}

			IField[] selectedFields= getSelectedFields(selection);

			if (canRunOn(selectedFields)) {
				run(selectedFields[0].getDeclaringType(), selectedFields, false);
				return;
			}
			Object firstElement= selection.getFirstElement();

			if (firstElement instanceof IType) {
				run((IType) firstElement, new IField[0], false);
			}
			else if (firstElement instanceof ICompilationUnit) {
				IType type= ((ICompilationUnit) firstElement).findPrimaryType();
				if (type.isInterface()) {
					MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.interface_not_applicable")); //$NON-NLS-1$					
					return;
				} else
					run(((ICompilationUnit) firstElement).findPrimaryType(), new IField[0], false);
			}
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.error.actionfailed")); //$NON-NLS-1$
		}
	}

	private IType getSelectedType(IStructuredSelection selection) throws JavaModelException {
		Object[] elements= selection.toArray();
		if (elements.length == 1 && (elements[0] instanceof IType)) {
			IType type= (IType) elements[0];
			if (type.getCompilationUnit() != null && type.isClass()) {
				return type;
			}
		} else if (elements[0] instanceof ICompilationUnit) {
			ICompilationUnit cu= (ICompilationUnit) elements[0];
			IType type= cu.findPrimaryType();
			if (type != null && !type.isInterface())
				return type;
		} else if (elements[0] instanceof IField) {
			return ((IField) elements[0]).getCompilationUnit().findPrimaryType();
		}
		return null;
	}

	private static boolean canRunOn(IField[] fields) {
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
					IField fld= (IField) curr;

					if (i == 0) {
						// remember the CU of the first element
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

	//---- Java Editor --------------------------------------------------------------

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
				IField field= (IField) elements[0];
				run(field.getDeclaringType(), new IField[] { field },false);
				return;
			}
			IJavaElement element= SelectionConverter.getElementAtOffset(fEditor);

			if (element != null) {
				IType type= (IType) element.getAncestor(IJavaElement.TYPE);
				if (type != null) {
					if (type.getFields().length > 0) {
						run(type, new IField[0], true);
						return;
					}
				}
			}
			MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.not_applicable")); //$NON-NLS-1$
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), null);
		}
	}

	private boolean checkEnabledEditor() {
		return fEditor != null && SelectionConverter.canOperateOn(fEditor);
	}

	//---- Helpers -------------------------------------------------------------------

	private void run(IType type, IField[] preselected, boolean activatedFromEditor) throws CoreException {
		if (!ElementValidator.check(type, getShell(), getDialogTitle(), activatedFromEditor)) {
			return;
		}
		if (!ActionUtil.isProcessable(getShell(), type)) {
			return;
		}

		IField[] constructorFields= type.getFields();

		ArrayList constructorFieldsList= new ArrayList();
		for (int i= 0; i < constructorFields.length; i++) {
			boolean isStatic= Flags.isStatic(constructorFields[i].getFlags());
			boolean isFinal= Flags.isFinal(constructorFields[i].getFlags());
			if (!isStatic) {
				if (isFinal) {
					try {
						// Do not add final fields which have been set in the <clinit>
						IScanner scanner= ToolFactory.createScanner(true, false, false, false);
						scanner.setSource(constructorFields[i].getSource().toCharArray());
						TokenScanner tokenScanner= new TokenScanner(scanner);
						tokenScanner.getTokenStartOffset(ITerminalSymbols.TokenNameEQUAL, 0);
					} catch (JavaModelException e) {
					} catch (CoreException e) {
						constructorFieldsList.add(constructorFields[i]);
					}			
				} else
					constructorFieldsList.add(constructorFields[i]);
			}
		}
		if (constructorFieldsList.isEmpty()) {
			MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.typeContainsNoFields.message")); //$NON-NLS-1$
			return;
		}
		IMethod[] superConstructors= getSuperConstructors(type);
		if (superConstructors.length == 0) {
			MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("GenerateConstructorUsingFieldsAction.error.nothing_found")); //$NON-NLS-1$
			return;
		}		

		JavaElementLabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		GenerateConstructorUsingFieldsContentProvider contentProvider= new GenerateConstructorUsingFieldsContentProvider(constructorFieldsList);
		GenerateConstructorUsingFieldsSelectionDialog dialog= new GenerateConstructorUsingFieldsSelectionDialog(getShell(), labelProvider, contentProvider, fEditor, type, superConstructors);
		dialog.setCommentString(ActionMessages.getString("SourceActionDialog.createConstructorComment")); //$NON-NLS-1$
		dialog.setTitle(ActionMessages.getString("GenerateConstructorUsingFieldsAction.dialog.title")); //$NON-NLS-1$
		dialog.setInitialSelections(preselected);
		dialog.setContainerMode(true);
		dialog.setSize(60, 18);
		dialog.setInput(new Object());
		dialog.setMessage(ActionMessages.getString("GenerateConstructorUsingFieldsAction.dialog.label")); //$NON-NLS-1$
		dialog.setValidator(createValidator(constructorFieldsList.size(), dialog, type));

		int dialogResult= dialog.open();
		if (dialogResult == Window.OK) {
			Object[] checkedElements= dialog.getResult();
			if (checkedElements == null)
				return;
			ArrayList result= new ArrayList(checkedElements.length);
			for (int i= 0; i < checkedElements.length; i++) {
				Object curr= checkedElements[i];
				if (curr instanceof IField) {
					result.add(curr);
				}
			}
			IEditorPart editor= EditorUtility.openInEditor(type.getCompilationUnit());
					
			IField[] workingCopyFields= (IField[]) result.toArray(new IField[result.size()]);

			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
			settings.createComments= dialog.getGenerateComment();

			IJavaElement elementPosition= dialog.getElementPosition();
			
			IMethod selectedConstructor= dialog.getSuperConstructorChoice();

			IRewriteTarget target= editor != null ? (IRewriteTarget) editor.getAdapter(IRewriteTarget.class) : null;
			if (target != null) {
				target.beginCompoundChange();
			}
			try {						
				AddCustomConstructorOperation op= new AddCustomConstructorOperation(type, settings, workingCopyFields, false, elementPosition, selectedConstructor);
				op.setVisbility(dialog.getVisibilityModifier());
				// Ignore the omit super() checkbox if the default constructor is not chosen
				if (selectedConstructor.getParameterNames().length == 0)
					op.setOmitSuper(dialog.isOmitSuper());
				

				IRunnableContext context= JavaPlugin.getActiveWorkbenchWindow();
				if (context == null) {
					context= new BusyIndicatorRunnableContext();
				}
				context.run(false, true, new WorkbenchRunnableAdapter(op, op.getScheduleRule()));
				IMethod res= op.getCreatedConstructor();
				JavaModelUtil.reconcile(res.getCompilationUnit());
				EditorUtility.revealInEditor(editor, res);

			} catch (InvocationTargetException e) {
				ExceptionHandler.handle(e, getShell(), getDialogTitle(), null);
			} catch (InterruptedException e) {
				// Do nothing. Operation has been cancelled by user.
			} finally {
				if (target != null) {
					target.endCompoundChange();
				}
			}
		}
	}
	
	private static IMethod[] getSuperConstructors(IType type) throws CoreException {
		List constructorMethods= new ArrayList();
		
		ITypeHierarchy hierarchy= type.newSupertypeHierarchy(null);				
		IType supertype= hierarchy.getSuperclass(type);

		if (supertype != null) {
			IMethod[] superMethods= supertype.getMethods();
			boolean constuctorFound= false;
			for (int i= 0; i < superMethods.length; i++) {
					IMethod curr= superMethods[i];
					if (curr.isConstructor())  {
						constuctorFound= true;
						if (JavaModelUtil.isVisibleInHierarchy(curr, type.getPackageFragment())) {
							constructorMethods.add(curr);
						}
					}
			}
			
			if (!constuctorFound)  {
				IType objectType= type.getJavaProject().findType("java.lang.Object"); //$NON-NLS-1$
				IMethod curr= objectType.getMethod("Object", new String[0]);  //$NON-NLS-1$
				constructorMethods.add(curr);
			}
		}
		return (IMethod[]) constructorMethods.toArray(new IMethod[constructorMethods.size()]);
	}
	

	private static ISelectionStatusValidator createValidator(int entries, GenerateConstructorUsingFieldsSelectionDialog dialog, IType type) {
		GenerateConstructorUsingFieldsValidator validator= new GenerateConstructorUsingFieldsValidator(entries, dialog, type);
		return validator;
	}

	private static class GenerateConstructorUsingFieldsValidator implements ISelectionStatusValidator {
		private static int fEntries;
		private IType fType;
		private GenerateConstructorUsingFieldsSelectionDialog fDialog;
		List fExistingSigs;

		GenerateConstructorUsingFieldsValidator(int entries) {
			super();
			fEntries= entries;
			fType= null;
		}

		GenerateConstructorUsingFieldsValidator(int entries, GenerateConstructorUsingFieldsSelectionDialog dialog, IType type) {
			super();
			fEntries= entries;
			fDialog= dialog;
			fType= type;
			// Create the potential signature and compare it to the existing ones	
			fExistingSigs= getExistingConstructorSignatures();
		}

		public IStatus validate(Object[] selection) {
			StringBuffer buffer= new StringBuffer();
			buffer.append('(');
			// first form the part of the signature corresponding to the super constructor combo choice
			IMethod chosenSuper= fDialog.getSuperConstructorChoice();
			try {
				String superParamTypes[]= chosenSuper.getParameterTypes();
				for (int i= 0; i < superParamTypes.length; i++) {
					buffer.append(superParamTypes[i]);
				}

				// second form the part of the signature corresponding to the fields selected
				for (int i= 0; i < selection.length; i++) {
					if (selection[i] instanceof IField) {
						buffer.append(((IField) selection[i]).getTypeSignature());
					}
				}
			} catch (JavaModelException e) {
			}

			buffer.append(")V"); //$NON-NLS-1$
			if (fExistingSigs.contains(buffer.toString())) {
				return new StatusInfo(IStatus.WARNING, ActionMessages.getString("GenerateConstructorUsingFieldsAction.error.duplicate_constructor")); //$NON-NLS-1$							
			}

			int fieldCount= countSelectedFields(selection);
			String message= ActionMessages.getFormattedString("GenerateConstructorUsingFieldsAction.fields_selected", new Object[] { String.valueOf(fieldCount), String.valueOf(fEntries)}); //$NON-NLS-1$
			return new StatusInfo(IStatus.INFO, message);
		}

		private int countSelectedFields(Object[] selection) {
			int count= 0;
			for (int i= 0; i < selection.length; i++) {
				if (selection[i] instanceof IField)
					count++;
			}
			return count;
		}

		private List getExistingConstructorSignatures() {
			List constructorMethods= new ArrayList();
			try {
				IMethod[] methods= fType.getMethods();
				for (int i= 0; i < methods.length; i++) {
					IMethod curr= methods[i];
					if (curr.isConstructor()) {
						constructorMethods.add(curr.getSignature());
					}
				}
			} catch (JavaModelException e) {
			}
			return constructorMethods;
		}
	}

	private static class GenerateConstructorUsingFieldsSelectionDialog extends SourceActionDialog {
		private GenerateConstructorUsingFieldsContentProvider fContentProvider;
		private int fSuperIndex;
		private int fWidth= 60;
		private int fHeight= 18;
		protected Button[] fButtonControls;
		private boolean[] fButtonsEnabled;
		private boolean fOmitSuper;
		private IMethod[] fSuperConstructors;
		private IDialogSettings fGenConstructorSettings;

		protected CheckboxTreeViewer fTreeViewer;
		private GenerateConstructorUsingFieldsTreeViewerAdapter fTreeViewerAdapter;

		private static final int UP_BUTTON= IDialogConstants.CLIENT_ID + 1;
		private static final int DOWN_BUTTON= IDialogConstants.CLIENT_ID + 2;
		
		private final String SETTINGS_SECTION= "GenerateConstructorUsingFieldsSelectionDialog"; //$NON-NLS-1$
		private final String OMIT_SUPER="OmitCallToSuper"; //$NON-NLS-1$
		private Button fOmitSuperButton;

		public GenerateConstructorUsingFieldsSelectionDialog(Shell parent, ILabelProvider labelProvider, GenerateConstructorUsingFieldsContentProvider contentProvider, CompilationUnitEditor editor, IType type, IMethod[] superConstructors) throws JavaModelException {
			super(parent, labelProvider, contentProvider, editor, type, true);
			fContentProvider= contentProvider;
			fTreeViewerAdapter= new GenerateConstructorUsingFieldsTreeViewerAdapter();

			fSuperConstructors= superConstructors;
			
			IDialogSettings dialogSettings= JavaPlugin.getDefault().getDialogSettings();
			fGenConstructorSettings= dialogSettings.getSection(SETTINGS_SECTION);
			if (fGenConstructorSettings == null) {
				fGenConstructorSettings= dialogSettings.addNewSection(SETTINGS_SECTION);
				fGenConstructorSettings.put(OMIT_SUPER, false); //$NON-NLS-1$
			}				
			
			fOmitSuper= fGenConstructorSettings.getBoolean(OMIT_SUPER);			
		}
		
		protected Composite createVisibilityControlAndModifiers(Composite parent, final IVisibilityChangeListener visibilityChangeListener, int[] availableVisibilities, int correctVisibility) {
			Composite visibilityComposite= createVisibilityControl(parent, visibilityChangeListener, availableVisibilities, correctVisibility);	
			return visibilityComposite;			
		}

		protected Control createDialogArea(Composite parent) {
			initializeDialogUnits(parent);

			Composite composite= new Composite(parent, SWT.NONE);
			GridLayout layout= new GridLayout();
			GridData gd= null;

			layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
			layout.marginWidth= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
			layout.verticalSpacing= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
			layout.horizontalSpacing= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
			composite.setLayout(layout);

			Composite classConstructorComposite= addSuperClassConstructorChoices(composite);
			gd= new GridData(GridData.FILL_BOTH);
			classConstructorComposite.setLayoutData(gd);

			Composite inner= new Composite(composite, SWT.NONE);
			GridLayout innerLayout= new GridLayout();
			innerLayout.numColumns= 2;
			innerLayout.marginHeight= 0;
			innerLayout.marginWidth= 0;
			inner.setLayout(innerLayout);

			Label messageLabel= createMessageArea(inner);
			if (messageLabel != null) {
				gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
				gd.horizontalSpan= 2;
				messageLabel.setLayoutData(gd);
			}

			fTreeViewer= createTreeViewer(inner);
			gd= new GridData(GridData.FILL_BOTH);
			gd.widthHint= convertWidthInCharsToPixels(fWidth);
			gd.heightHint= convertHeightInCharsToPixels(fHeight);
			fTreeViewer.getControl().setLayoutData(gd);
			fTreeViewer.setContentProvider(fContentProvider);
			fTreeViewer.addSelectionChangedListener(fTreeViewerAdapter);

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

		protected Composite createOmitSuper(Composite composite) {
			Composite omitSuperComposite= new Composite(composite, SWT.NONE);
			GridLayout layout= new GridLayout();
			layout.marginHeight= 0;
			layout.marginWidth= 0;
			omitSuperComposite.setLayout(layout);

			fOmitSuperButton= new Button(omitSuperComposite, SWT.CHECK);
			fOmitSuperButton.setText(ActionMessages.getString("GenerateConstructorUsingFieldsSelectionDialog.omit.super")); //$NON-NLS-1$
			fOmitSuperButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

			fOmitSuperButton.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					boolean isSelected= (((Button) e.widget).getSelection());
					setOmitSuper(isSelected);
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
			});
			fOmitSuperButton.setSelection(isOmitSuper());
			// Disable omit super checkbox unless default constructor
			fOmitSuperButton.setEnabled(getSuperConstructorChoice().getNumberOfParameters() == 0);
			GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
			gd.horizontalSpan= 2;
			fOmitSuperButton.setLayoutData(gd);

			return omitSuperComposite;
		}

		protected Composite createSelectionButtons(Composite composite) {
			Composite buttonComposite= super.createSelectionButtons(composite);

			GridLayout layout= new GridLayout();
			buttonComposite.setLayout(layout);

			createUpDownButtons(buttonComposite);

			layout.marginHeight= 0;
			layout.marginWidth= 0;
			layout.numColumns= 1;

			return buttonComposite;
		}

		private void createUpDownButtons(Composite buttonComposite) {
			int numButtons= 2; // up, down
			fButtonControls= new Button[numButtons];
			fButtonsEnabled= new boolean[numButtons];
			fButtonControls[UP_INDEX]= createButton(buttonComposite, UP_BUTTON, ActionMessages.getString("GenerateConstructorUsingFieldsSelectionDialog.up_button"), false); //$NON-NLS-1$	
			fButtonControls[DOWN_INDEX]= createButton(buttonComposite, DOWN_BUTTON, ActionMessages.getString("GenerateConstructorUsingFieldsSelectionDialog.down_button"), false); //$NON-NLS-1$			
			boolean defaultState= false;
			fButtonControls[UP_INDEX].setEnabled(defaultState);
			fButtonControls[DOWN_INDEX].setEnabled(defaultState);
			fButtonsEnabled[UP_INDEX]= defaultState;
			fButtonsEnabled[DOWN_INDEX]= defaultState;
		}

		protected void buttonPressed(int buttonId) {
			super.buttonPressed(buttonId);
			switch (buttonId) {
				case UP_BUTTON :
					{
						fContentProvider.up(getElementList(), getTreeViewer());
						updateOKStatus();
						break;
					}
				case DOWN_BUTTON :
					{
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
			addVisibilityAndModifiersChoices(entryComposite);
			return entryComposite;
		}

		private Composite addSuperClassConstructorChoices(Composite composite) {
			Label label= new Label(composite, SWT.NONE);
			label.setText(ActionMessages.getString("GenerateConstructorUsingFieldsSelectionDialog.sort_constructor_choices.label")); //$NON-NLS-1$
			GridData gd= new GridData(GridData.FILL_HORIZONTAL);
			label.setLayoutData(gd);

			final Combo combo= new Combo(composite, SWT.READ_ONLY);
			for (int i= 0; i < fSuperConstructors.length; i++) {
				combo.add(JavaElementLabels.getElementLabel(fSuperConstructors[i], JavaElementLabels.M_PARAMETER_TYPES));
			}

			// TODO: Can we be a little more intelligent about guessing the super() ?
			combo.setText(combo.getItem(0));
			combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			combo.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					fSuperIndex= combo.getSelectionIndex();
					// Disable omit super checkbox unless default constructor
					fOmitSuperButton.setEnabled(getSuperConstructorChoice().getNumberOfParameters() == 0);
					updateOKStatus();
				}
			});

			return composite;
		}

		public CheckboxTreeViewer getTreeViewer() {
			return fTreeViewer;
		}

		public IMethod getSuperConstructorChoice() {
			return fSuperConstructors[fSuperIndex];
		}

		private class GenerateConstructorUsingFieldsTreeViewerAdapter implements ISelectionChangedListener, IDoubleClickListener {

			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection= (IStructuredSelection) fTreeViewer.getSelection();

				List selectedList= selection.toList();
				GenerateConstructorUsingFieldsContentProvider cp= (GenerateConstructorUsingFieldsContentProvider) getContentProvider();

				fButtonControls[UP_INDEX].setEnabled(cp.canMoveUp(selectedList));
				fButtonControls[DOWN_INDEX].setEnabled(cp.canMoveDown(selectedList));
			}

			public void doubleClick(DoubleClickEvent event) {
				// Do nothing
			}
		}

		public void setOmitSuper(boolean omitSuper) {
			if (fOmitSuper != omitSuper)  {
				fOmitSuper= omitSuper;
				fGenConstructorSettings.put(OMIT_SUPER, omitSuper);
			}	
		}

		public boolean isOmitSuper() {
			return fOmitSuper;
		}

	}

	private static class GenerateConstructorUsingFieldsContentProvider implements ITreeContentProvider {
		private List fFieldsList;
		private static final Object[] EMPTY= new Object[0];

		public GenerateConstructorUsingFieldsContentProvider(List fieldList) {
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
			tree.setSelection(new StructuredSelection(checkedElements));
		}

		public void down(List checkedElements, CheckboxTreeViewer tree) {
			if (checkedElements.size() > 0) {
				setElements(reverse(moveUp(reverse(fFieldsList), checkedElements)), tree);
				tree.reveal(checkedElements.get(checkedElements.size() - 1));
			}
			tree.setSelection(new StructuredSelection(checkedElements));
		}

		public boolean canMoveUp(List selectedElements) {
			int nSelected= selectedElements.size();
			int nElements= fFieldsList.size();
			for (int i= 0; i < nElements && nSelected > 0; i++) {
				if (!selectedElements.contains(fFieldsList.get(i))) {
					return true;
				}
				nSelected--;
			}
			return false;
		}

		public boolean canMoveDown(List selectedElements) {
			int nSelected= selectedElements.size();
			for (int i= fFieldsList.size() - 1; i >= 0 && nSelected > 0; i--) {
				if (!selectedElements.contains(fFieldsList.get(i))) {
					return true;
				}
				nSelected--;
			}
			return false;
		}

		public List getFieldsList() {
			return fFieldsList;
		}

	}

}
