/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementSorter;

import org.eclipse.jdt.internal.corext.codemanipulation.AddGetterSetterOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.jdt.internal.corext.codemanipulation.IRequestQuery;
import org.eclipse.jdt.internal.corext.codemanipulation.NameProposer;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.CodeGenerationPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

/**
 * Creates getter and setter methods for a type's fields. Opens a dialog
 * with a list of fields for which a setter or getter can be generated.
 * User is able to check or uncheck items before setters or getters
 * are generated.
 * <p>
 * Will open the parent compilation unit in a Java editor. The result is 
 * unsaved, so the user can decide if the changes are acceptable.
 * <p>
 * The action is applicable to structured selections containing elements
 * of type <code>IField</code> or <code>IType</code>.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @since 2.0
 */
public class AddGetterSetterAction extends SelectionDispatchAction {
	
	private CompilationUnitEditor fEditor;
	private static final String dialogTitle= ActionMessages.getString("AddGetterSetterAction.error.title"); //$NON-NLS-1$

	/**
	 * Creates a new <code>AddGetterSetterAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public AddGetterSetterAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.getString("AddGetterSetterAction.label")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("AddGetterSetterAction.description")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("AddGetterSetterAction.tooltip")); //$NON-NLS-1$
		
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.GETTERSETTER_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public AddGetterSetterAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(checkEnabledEditor());
	}
	
	//---- Structured Viewer -----------------------------------------------------------
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(canEnable(selection));
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e);
			setEnabled(false);
		}
	}
		
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	protected void run(IStructuredSelection selection) {
		try {
			IField[] selectedFields= getSelectedFields(selection);
			if (canEnableOn(selectedFields)){
				run(selectedFields[0].getDeclaringType(), selectedFields);
				return;
			}	
			Object firstElement= selection.getFirstElement();
			if (firstElement instanceof IType)
				run((IType)firstElement, new IField[0]);
			else if (firstElement instanceof ICompilationUnit)	
				run(JavaElementUtil.getMainType((ICompilationUnit)firstElement), new IField[0]);
		} catch (CoreException e) {
			JavaPlugin.log(e.getStatus());
			showError(ActionMessages.getString("AddGetterSetterAction.error.actionfailed")); //$NON-NLS-1$
		}
				
	}
	
	private boolean canEnable(IStructuredSelection selection) throws JavaModelException{
		if (canEnableOn(getSelectedFields(selection)))
			return true;
			
		if ((selection.size() == 1) && (selection.getFirstElement() instanceof IType))
			return canEnableOn((IType)selection.getFirstElement());

		if ((selection.size() == 1) && (selection.getFirstElement() instanceof ICompilationUnit))
			return canEnableOn(JavaElementUtil.getMainType((ICompilationUnit)selection.getFirstElement()));

		return false;	
	}

	private static boolean canEnableOn(IType type) throws JavaModelException {
		if (type == null)
			return false;
		if (type.getFields().length == 0)
			return false;
		if (type.getCompilationUnit() == null)
			return false;
		if (JavaModelUtil.isEditable(type.getCompilationUnit()))
			return true;
		return false;	
	}
	
	private static boolean canEnableOn(IField[] fields) throws JavaModelException {
		return fields != null && fields.length > 0 && JavaModelUtil.isEditable(fields[0].getCompilationUnit());
	}
	
	private void run(IType type, IField[] preselected) throws CoreException{
		ILabelProvider lp= new AddGetterSetterLabelProvider(createNameProposer());
		Map entries= createGetterSetterMapping(type);
		if (entries.isEmpty()){
			MessageDialog.openInformation(getShell(), dialogTitle, ActionMessages.getString("AddGettSetterAction.typeContainsNoFields.message")); //$NON-NLS-1$
			return;
		}	
		ITreeContentProvider cp= new AddGetterSetterContentProvider(entries);
		CheckedTreeSelectionDialog dialog= new CheckedTreeSelectionDialog(getShell(), lp, cp);
		dialog.setSorter(new JavaElementSorter());
		dialog.setTitle(dialogTitle);
		String message= ActionMessages.getFormattedString("AddGetterSetterAction.dialog.title", JavaElementUtil.createSignature(type));//$NON-NLS-1$
		dialog.setMessage(message);
		dialog.setValidator(createValidator());
		dialog.setContainerMode(true);
		dialog.setSize(60, 18);
		dialog.setInput(type);
		dialog.setExpandedElements(type.getFields());
		dialog.setInitialSelections(preselected);
		dialog.open();
		Object[] result= dialog.getResult();
		if (result == null)
			return;
		IField[] getterFields= getGetterFields(result);
		IField[] setterFields= getSetterFields(result);
		generate(getterFields, setterFields);
	}

	private static ISelectionStatusValidator createValidator() {
		return new ISelectionStatusValidator(){
			public IStatus validate(Object[] selection) {
				int count= countSelectedMethods(selection);
				if (count == 0)
					return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
				if (count == 1)	
					return new StatusInfo(IStatus.INFO, ActionMessages.getString("AddGetterSetterAction.one_selected")); //$NON-NLS-1$
					
				String message= ActionMessages.getFormattedString("AddGetterSetterAction.methods_selected", String.valueOf(count));//$NON-NLS-1$
				return new StatusInfo(IStatus.INFO, message);
			}
		};
	}
	
	private static int countSelectedMethods(Object[] selection){
		int count= 0;
		for (int i = 0; i < selection.length; i++) {
			if (selection[i] instanceof GetterSetterEntry)
				count++;
		}
		return count;
	}
	
	private static IField[] getGetterFields(Object[] result){
		Collection list= new ArrayList(0);
		for (int i = 0; i < result.length; i++) {
			Object each= result[i];
			if ((each instanceof GetterSetterEntry)){ 
				GetterSetterEntry entry= (GetterSetterEntry)each;
				if (entry.isGetterEntry)
					list.add(entry.field);
			}	
		}
		return (IField[]) list.toArray(new IField[list.size()]);			
	}
	
	private static IField[] getSetterFields(Object[] result){
		Collection list= new ArrayList(0);
		for (int i = 0; i < result.length; i++) {
			Object each= result[i];
			if ((each instanceof GetterSetterEntry)){ 
				GetterSetterEntry entry= (GetterSetterEntry)each;
				if (! entry.isGetterEntry)
					list.add(entry.field);
			}	
		}
		return (IField[]) list.toArray(new IField[list.size()]);			
	}
	
	private static NameProposer createNameProposer(){
		return new NameProposer(getGetterSetterPrefixes(), getGetterSetterSuffixes());
	}
	
	private static String[] getGetterSetterPrefixes(){
		return CodeGenerationPreferencePage.getGetterStetterPrefixes();
	}
	
	private static String[] getGetterSetterSuffixes(){
		return CodeGenerationPreferencePage.getGetterStetterSuffixes();
	}
	
	private void generate(IField[] getterFields, IField[] setterFields) throws CoreException{
		if (getterFields.length == 0 && setterFields.length == 0)
			return;
		
		ICompilationUnit cu= null;
		if (getterFields.length != 0)
			cu= getterFields[0].getCompilationUnit();
		else
			cu= setterFields[0].getCompilationUnit();
		//open the editor, forces the creation of a working copy
		IEditorPart editor= EditorUtility.openInEditor(cu);
		
		IField[] workingCopyGetterFields= getWorkingCopyFields(getterFields);
		IField[] workingCopySetterFields= getWorkingCopyFields(setterFields);
		if (workingCopyGetterFields != null && workingCopySetterFields != null)
			run(workingCopyGetterFields, workingCopySetterFields, editor);
	}
	
	private IField[] getWorkingCopyFields(IField[] fields) throws CoreException{
		if (fields.length == 0)
			return new IField[0];
		ICompilationUnit cu= fields[0].getCompilationUnit();
		
		ICompilationUnit workingCopyCU;
		IField[] workingCopyFields;
		if (cu.isWorkingCopy()) {
			workingCopyCU= cu;
			workingCopyFields= fields;
		} else {
			workingCopyCU= EditorUtility.getWorkingCopy(cu);
			if (workingCopyCU == null) {
				showError(ActionMessages.getString("AddGetterSetterAction.error.actionfailed")); //$NON-NLS-1$
				return null;
			}
			workingCopyFields= new IField[fields.length];
			for (int i= 0; i < fields.length; i++) {
				IField field= fields[i];
				IField workingCopyField= (IField) JavaModelUtil.findMemberInCompilationUnit(workingCopyCU, field);
				if (workingCopyField == null) {
					showError(ActionMessages.getFormattedString("AddGetterSetterAction.error.fieldNotExisting", field.getElementName())); //$NON-NLS-1$
					return null;
				}
				workingCopyFields[i]= workingCopyField;
			}
		}
		return workingCopyFields;	
	}
	//---- Java Editior --------------------------------------------------------------
    
    /* (non-Javadoc)
     * Method declared on SelectionDispatchAction
     */		
    protected void selectionChanged(ITextSelection selection) {
    }
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	protected void run(ITextSelection selection) {
		try {
			IJavaElement[] elements= SelectionConverter.codeResolve(fEditor);
			if (elements.length == 1 && (elements[0] instanceof IField)) {
				IField field= (IField)elements[0];
				if (! checkCu(field))
					return;
				run(field.getDeclaringType(), new IField[] {field});
				return;
			}
			IJavaElement element= SelectionConverter.getElementAtOffset(fEditor);
			if (element != null){
				IType type= (IType)element.getAncestor(IJavaElement.TYPE);
				if (type != null){
					if (! checkCu(type))
						return; //dialog already shown - just return
					if (type.getFields().length > 0){
						run(type, new IField[0]);	
						return;
					} 
				}
			} 
			MessageDialog.openInformation(getShell(), dialogTitle, 
				ActionMessages.getString("AddGetterSetterAction.not_applicable")); //$NON-NLS-1$
		} catch (CoreException e) {
			JavaPlugin.log(e.getStatus());
			showError(ActionMessages.getString("AddGetterSetterAction.error.actionfailed")); //$NON-NLS-1$
		}
	}
	
	/* package */ void editorStateChanged() {
		setEnabled(checkEnabledEditor());
	}
	
	private boolean checkEnabledEditor() {
		return fEditor != null && !fEditor.isEditorInputReadOnly() && SelectionConverter.canOperateOn(fEditor);
	}	
	
	private boolean checkCu(IMember member) throws JavaModelException{
		if (JavaModelUtil.isEditable(member.getCompilationUnit())) 
			return true;
		MessageDialog.openInformation(getShell(), dialogTitle, 
			ActionMessages.getFormattedString("AddGetterSetterAction.read_only", member.getElementName())); //$NON-NLS-1$
		return false;
	}
	
	//---- Helpers -------------------------------------------------------------------
	
	private void run(IField[] getterFields, IField[] setterFields, IEditorPart editor) {
		try{
			AddGetterSetterOperation op= createAddGetterSetterOperation(getterFields, setterFields);
			new ProgressMonitorDialog(getShell()).run(false, true, new WorkbenchRunnableAdapter(op));
		
			IMethod[] createdMethods= op.getCreatedAccessors();
			if (createdMethods.length > 0) {
				EditorUtility.revealInEditor(editor, createdMethods[0]);
			}		
		} catch (InvocationTargetException e) {
			JavaPlugin.log(e);
			showError(ActionMessages.getString("AddGetterSetterAction.error.actionfailed")); //$NON-NLS-1$
		} catch (InterruptedException e) {
			// operation cancelled
		}
	}

	private AddGetterSetterOperation createAddGetterSetterOperation(IField[] getterFields, IField[] setterFields) {
		IRequestQuery skipSetterForFinalQuery= skipSetterForFinalQuery();
		IRequestQuery skipReplaceQuery= skipReplaceQuery();
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();		
		return new AddGetterSetterOperation(getterFields, setterFields, createNameProposer(), settings, skipSetterForFinalQuery, skipReplaceQuery);
	}
	
	private IRequestQuery skipSetterForFinalQuery() {
		return new IRequestQuery() {
			public int doQuery(IMember field) {
				// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=19367
				int[] returnCodes= {IRequestQuery.YES, IRequestQuery.YES_ALL, IRequestQuery.NO, IRequestQuery.CANCEL};
				String[] options= {IDialogConstants.YES_LABEL, IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL};
				String fieldName= JavaElementLabels.getElementLabel(field, 0);
				String formattedMessage= ActionMessages.getFormattedString("AddGetterSetterAction.SkipSetterForFinalDialog.message", fieldName); //$NON-NLS-1$
				return showQueryDialog(formattedMessage, options, returnCodes);	
			}
		};
	}
	
	private IRequestQuery skipReplaceQuery() {
		return new IRequestQuery() {
			public int doQuery(IMember method) {
				int[] returnCodes= {IRequestQuery.YES, IRequestQuery.NO, IRequestQuery.YES_ALL, IRequestQuery.CANCEL};
				String skipLabel= ActionMessages.getString("AddGetterSetterAction.SkipExistingDialog.skip.label"); //$NON-NLS-1$
				String replaceLabel= ActionMessages.getString("AddGetterSetterAction.SkipExistingDialog.replace.label"); //$NON-NLS-1$
				String skipAllLabel= ActionMessages.getString("AddGetterSetterAction.SkipExistingDialog.skipAll.label"); //$NON-NLS-1$
				String[] options= { skipLabel, replaceLabel, skipAllLabel, IDialogConstants.CANCEL_LABEL};
				String methodName= JavaElementLabels.getElementLabel(method, JavaElementLabels.M_PARAMETER_TYPES);
				String formattedMessage= ActionMessages.getFormattedString("AddGetterSetterAction.SkipExistingDialog.message", methodName); //$NON-NLS-1$
				return showQueryDialog(formattedMessage, options, returnCodes);		
			}
		};
	}
	
	
	private int showQueryDialog(final String message, final String[] buttonLabels, int[] returnCodes) {
		final Shell shell= getShell();
		if (shell == null) {
			JavaPlugin.logErrorMessage("AddGetterSetterAction.showQueryDialog: No active shell found"); //$NON-NLS-1$
			return IRequestQuery.CANCEL;
		}		
		final int[] result= { MessageDialog.CANCEL };
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				String title= ActionMessages.getString("AddGetterSetterAction.QueryDialog.title"); //$NON-NLS-1$
				MessageDialog dialog= new MessageDialog(shell, title, null, message, MessageDialog.QUESTION, buttonLabels, 0);
				result[0]= dialog.open();				
			}
		});
		int returnVal= result[0];
		return returnVal < 0 ? IRequestQuery.CANCEL : returnCodes[returnVal];
	}	

	private void showError(String message) {
		MessageDialog.openError(getShell(), dialogTitle, message);
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
							// no setters/getters for interfaces
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
	
	private static class GetterSetterEntry {
		public final IField field;
		public final boolean isGetterEntry;
		GetterSetterEntry(IField field, boolean isGetterEntry){
			this.field= field;
			this.isGetterEntry= isGetterEntry;
		}
	}
	
	private static class AddGetterSetterLabelProvider extends JavaElementLabelProvider {
		private final NameProposer fNameProposer;
		
		AddGetterSetterLabelProvider(NameProposer nameProposer) {
			fNameProposer= nameProposer;
		}
		
		/*
		 * @see ILabelProvider#getText(Object)
		 */
		public String getText(Object element) {
			if (element instanceof GetterSetterEntry) {
				GetterSetterEntry entry= (GetterSetterEntry) element;
				try {
					if (entry.isGetterEntry) {
						return fNameProposer.proposeGetterName(entry.field) + "()"; //$NON-NLS-1$
					} else {
						return fNameProposer.proposeSetterName(entry.field) + '(' + Signature.getSimpleName(Signature.toString(entry.field.getTypeSignature())) + ')';
					}
				} catch (JavaModelException e) {
					return ""; //$NON-NLS-1$
				}
			}
			return super.getText(element);
		}

		/*
		 * @see ILabelProvider#getImage(Object)
		 */
		public Image getImage(Object element) {
			if (element instanceof GetterSetterEntry) {
				int flags= 0;
				try {
					flags= ((GetterSetterEntry) element).field.getFlags();
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
				ImageDescriptor desc= JavaElementImageProvider.getFieldImageDescriptor(false, Flags.AccPublic);
				int adornmentFlags= Flags.isStatic(flags) ? JavaElementImageDescriptor.STATIC : 0;
				desc= new JavaElementImageDescriptor(desc, adornmentFlags, JavaElementImageProvider.BIG_SIZE);
				return JavaPlugin.getImageDescriptorRegistry().get(desc);
			}
			return super.getImage(element);
		}
	}
	
	
	/**
	 * @return map IField -> GetterSetterEntry[]
	 */
	private static Map createGetterSetterMapping(IType type) throws JavaModelException{
		IField[] fields= type.getFields();
		Map result= new HashMap();
		String[] prefixes= AddGetterSetterAction.getGetterSetterPrefixes();
		String[] suffixes= AddGetterSetterAction.getGetterSetterSuffixes();
		for (int i= 0; i < fields.length; i++) {
			List l= new ArrayList(2);
			if (GetterSetterUtil.getGetter(fields[i], prefixes, suffixes) == null)
				l.add(new GetterSetterEntry(fields[i], true));
				
			if (GetterSetterUtil.getSetter(fields[i], prefixes, suffixes) == null)
				l.add(new GetterSetterEntry(fields[i], false));	

			if (! l.isEmpty())
				result.put(fields[i], (GetterSetterEntry[]) l.toArray(new GetterSetterEntry[l.size()]));
		}
		return result;
	}
	
	private static class AddGetterSetterContentProvider implements ITreeContentProvider{
		private static final Object[] EMPTY= new Object[0];
		private Map fGetterSetterEntries;
		public AddGetterSetterContentProvider(Map entries) throws JavaModelException {
			fGetterSetterEntries= entries;
		}
		
		/*
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof IField)
				return (Object[])fGetterSetterEntries.get(parentElement);	
			return EMPTY;
		}

		/*
		 * @see ITreeContentProvider#getParent(Object)
		 */
		public Object getParent(Object element) {
			if (element instanceof IMember) 
				return ((IMember)element).getDeclaringType();
			if (element instanceof GetterSetterEntry)
				return ((GetterSetterEntry)element).field;
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
			return fGetterSetterEntries.keySet().toArray();
		}

		/*
		 * @see IContentProvider#dispose()
		 */
		public void dispose() {
			fGetterSetterEntries.clear();
			fGetterSetterEntries= null;
		}

		/*
		 * @see IContentProvider#inputChanged(Viewer, Object, Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		
	}
}