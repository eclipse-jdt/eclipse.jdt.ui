/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.wizards;

import java.util.List;import org.eclipse.swt.SWT;import org.eclipse.swt.graphics.Image;import org.eclipse.swt.widgets.Composite;import org.eclipse.core.resources.IProject;import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IWorkspaceRoot;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.SubProgressMonitor;import org.eclipse.jface.dialogs.ErrorDialog;import org.eclipse.jface.viewers.LabelProvider;import org.eclipse.jdt.core.Flags;import org.eclipse.jdt.core.IBuffer;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IMethod;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jdt.core.ISourceRange;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.JavaConventions;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.Signature;import org.eclipse.jdt.core.search.IJavaSearchScope;import org.eclipse.jdt.core.search.SearchEngine;import org.eclipse.jdt.ui.IJavaElementSearchConstants;import org.eclipse.jdt.internal.compiler.ConfigurableOption;import org.eclipse.jdt.internal.compiler.env.IConstants;import org.eclipse.jdt.internal.formatter.CodeFormatter;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaPluginImages;import org.eclipse.jdt.internal.ui.codemanipulation.ImportsStructure;import org.eclipse.jdt.internal.ui.codemanipulation.StubUtility;import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;import org.eclipse.jdt.internal.ui.dialogs.TypeSelectionDialog;import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;import org.eclipse.jdt.internal.ui.preferences.ImportOrganizePreferencePage;import org.eclipse.jdt.internal.ui.util.JavaModelUtility;import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;import org.eclipse.jdt.internal.ui.wizards.dialogfields.Separator;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;import org.eclipse.jdt.internal.ui.wizards.swt.MGridData;

public abstract class TypePage extends ContainerPackagePage {
	
	private final static String PAGE_NAME= "TypePage";
	
	protected final static String TYPENAME= PAGE_NAME + ".typename";
	protected final static String SUPER= PAGE_NAME + ".superclass";
	protected final static String INTERFACES= PAGE_NAME + ".interfaces";
	protected final static String MODIFIERS= PAGE_NAME + ".modifiers";
	protected final static String METHODS= PAGE_NAME + ".methods";
	
	private final static String ERROR_TYPE_ENTERNAME= PAGE_NAME + ".error.EnterTypeName";
	private final static String ERROR_TYPE_NAMEEXISTS= PAGE_NAME + ".error.TypeNameExists";
	private final static String ERROR_TYPE_INVALIDNAME= PAGE_NAME + ".error.InvalidTypeName";
	private final static String ERROR_TYPE_QUALIFIEDNAME= PAGE_NAME + ".error.QualifiedName";

	private final static String WARNING_TYPE_NAMEDISCOURAGED= PAGE_NAME + ".warning.TypeNameDiscouraged";
	
	private final static String ERROR_SUPER_ENTERNAME= PAGE_NAME + ".error.EnterSuperClassName";
	private final static String ERROR_SUPER_INVALIDNAME= PAGE_NAME + ".error.InvalidSuperClassName";
	private final static String WARNING_SUPER_NOTEXISTS= PAGE_NAME + ".warning.SuperClassNotExists";
	private final static String WARNING_SUPER_ISFINAL= PAGE_NAME + ".warning.SuperClassIsFinal";
	private final static String WARNING_SUPER_NOTVISIBLE= PAGE_NAME + ".warning.SuperClassIsNotVisible";
	private final static String WARNING_SUPER_NOTCLASS= PAGE_NAME + ".warning.SuperClassIsNotClass";
	
	private final static String WARNING_INTFC_NOTVISIBLE= PAGE_NAME + ".warning.InterfaceIsNotVisible";
	private final static String WARNING_INTFC_NOTEXISTS= PAGE_NAME + ".warning.InterfaceNotExists";
	private final static String WARNING_INTFC_NOTINTERFACE= PAGE_NAME + ".warning.InterfaceIsNotInterface";

	private final static String ERROR_MODIFIERS_FINALANDABS= PAGE_NAME + ".error.ModifiersFinalAndAbstract";

	private final static String SUPERCLASS_DIALOG= PAGE_NAME + ".SuperClassDialog";
	private final static String INTERFACES_DIALOG= PAGE_NAME + ".InterfacesDialog";
	
	private final static String OPERATION_DESC= PAGE_NAME + ".operationdesc";
	
	private class InterfacesListLabelProvider extends LabelProvider {
		
		private Image fInterfaceImage;
		
		public InterfacesListLabelProvider() {
			super();
			fInterfaceImage= JavaPlugin.getDefault().getImageRegistry().get(JavaPluginImages.IMG_OBJS_INTERFACE);
		}
		
		public Image getImage(Object element) {
			return fInterfaceImage;
		}
	}	
	
	private StringDialogField fTypeNameDialogField;
	private StatusInfo fTypeNameStatus;
	
	private StringButtonDialogField fSuperClassDialogField;
	private StatusInfo fSuperClassStatus;
	
	private ListDialogField fSuperInterfacesDialogField;
	private StatusInfo fSuperInterfacesStatus;
	
	private SelectionButtonDialogFieldGroup fAccMdfButtons;
	private SelectionButtonDialogFieldGroup fOtherMdfButtons;
	private StatusInfo fModifierStatus;
			
	private IType fCreatedType;
	
	private boolean fIsClass;
	private int fStaticMdfIndex;
	
	public TypePage(boolean isClass, String pageName, IWorkspaceRoot root) {
		super(pageName, root);
		fCreatedType= null;
		
		fIsClass= isClass;
		
		TypeFieldsAdapter adapter= new TypeFieldsAdapter();
				
		fTypeNameDialogField= new StringDialogField();
		fTypeNameDialogField.setDialogFieldListener(adapter);
		fTypeNameDialogField.setLabelText(getResourceString(TYPENAME + ".label"));
		
		fSuperClassDialogField= new StringButtonDialogField(adapter);
		fSuperClassDialogField.setDialogFieldListener(adapter);
		fSuperClassDialogField.setLabelText(getResourceString(SUPER + ".label"));
		fSuperClassDialogField.setButtonLabel(getResourceString(SUPER + ".button"));
		
		String[] addButtons= new String[] { getResourceString(INTERFACES + ".add") };
		fSuperInterfacesDialogField= new ListDialogField(adapter, addButtons, new InterfacesListLabelProvider(), 0);
		fSuperInterfacesDialogField.setDialogFieldListener(adapter);
		fSuperInterfacesDialogField.setLabelText(getResourceString(INTERFACES + ".label"));
		fSuperInterfacesDialogField.setRemoveButtonLabel(getResourceString(INTERFACES + ".remove"));
	
		String[] buttonNames1= new String[] {
			getResourceString(MODIFIERS + ".public"), getResourceString(MODIFIERS + ".default"),
			getResourceString(MODIFIERS + ".private"), getResourceString(MODIFIERS + ".protected")
		};
		fAccMdfButtons= new SelectionButtonDialogFieldGroup(SWT.RADIO, buttonNames1, 4);
		fAccMdfButtons.setDialogFieldListener(adapter);
		fAccMdfButtons.setLabelText(getResourceString(MODIFIERS + ".acc.label"));		
		fAccMdfButtons.setSelection(0, true);
		
		String[] buttonNames2;
		int staticMdfIndex;
		if (fIsClass) {
			buttonNames2= new String[] {
				getResourceString(MODIFIERS + ".abstract"), getResourceString(MODIFIERS + ".final"),
				getResourceString(MODIFIERS + ".static")
			};
			fStaticMdfIndex= 2;
		} else {
			buttonNames2= new String[] {
				getResourceString(MODIFIERS + ".static")
			};
			fStaticMdfIndex= 0;
		}

		fOtherMdfButtons= new SelectionButtonDialogFieldGroup(SWT.CHECK, buttonNames2, 4);
		fOtherMdfButtons.setDialogFieldListener(adapter);
		fOtherMdfButtons.setLabelText(getResourceString(MODIFIERS + ".other.label"));		
		
		fAccMdfButtons.enableSelectionButton(2, false);
		fAccMdfButtons.enableSelectionButton(3, false);
		fOtherMdfButtons.enableSelectionButton(fStaticMdfIndex, false);
					
		fTypeNameStatus= new StatusInfo();
		fSuperClassStatus= new StatusInfo();
		fSuperInterfacesStatus= new StatusInfo();
		fModifierStatus= new StatusInfo();
	}
	
	// -------- UI Creation ---------
	
	protected void createSeparator(Composite composite, int nColumns) {
		(new Separator()).doFillIntoGrid(composite, nColumns, 12);		
		(new Separator(SWT.SEPARATOR | SWT.HORIZONTAL)).doFillIntoGrid(composite, nColumns, 8);		
	}	
	
	protected void createTypeNameControls(Composite composite, int nColumns) {
		fTypeNameDialogField.doFillIntoGrid(composite, nColumns - 1);
		DialogField.createEmptySpace(composite);
		
	}
	
	protected void createModifierControls(Composite composite, int nColumns) {
		LayoutUtil.setHorizontalSpan(fAccMdfButtons.getLabelControl(composite), 1);
		LayoutUtil.setHorizontalSpan(fAccMdfButtons.getSelectionButtonsGroup(composite), nColumns - 2);
		fAccMdfButtons.setButtonsMinWidth(70);
		DialogField.createEmptySpace(composite);
		
		DialogField.createEmptySpace(composite);
		LayoutUtil.setHorizontalSpan(fOtherMdfButtons.getSelectionButtonsGroup(composite), nColumns - 2);
		fOtherMdfButtons.setButtonsMinWidth(70);
		DialogField.createEmptySpace(composite);
	}
	
	protected void createSuperClassControls(Composite composite, int nColumns) {
		fSuperClassDialogField.doFillIntoGrid(composite, nColumns);
	}
		
	protected void createSuperInterfacesControls(Composite composite, int nColumns) {
		fSuperInterfacesDialogField.doFillIntoGrid(composite, nColumns);
		MGridData gd= (MGridData)fSuperInterfacesDialogField.getListControl(null).getLayoutData();
		gd.heightHint= 80;
		gd.grabExcessVerticalSpace= false;
	}
	
	protected void setFocus() {
		fTypeNameDialogField.setFocus();
	}
				
	// -------- TypeFieldsAdapter --------

	private class TypeFieldsAdapter implements IStringButtonAdapter, IDialogFieldListener, IListAdapter {
		
		// -------- IStringButtonAdapter
		
		public void changeControlPressed(DialogField field) {
			if (field == fSuperClassDialogField) {
				IType type= chooseSuperType();
				if (type != null) {
					fSuperClassDialogField.setText(JavaModelUtility.getFullyQualifiedName(type));
				}
			}
		}
		
		// -------- IListAdapter
		
		public void customButtonPressed(DialogField field, int index) {
			if (field == fSuperInterfacesDialogField) {
				chooseSuperInterfaces();
			}
		}
		
		public void selectionChanged(DialogField field) {}
		
		
		// -------- IDialogFieldListener
		
		public void dialogFieldChanged(DialogField field) {
			String fieldName= null;
			if (field == fTypeNameDialogField) {
				updateTypeNameStatus();
				fieldName= TYPENAME;
			} else if (field == fSuperClassDialogField) {
				updateSuperClassStatus();
				fieldName= SUPER;
			} else if (field == fSuperInterfacesDialogField) {
				updateInterfacesStatus();
				fieldName= INTERFACES;
			} else if (field == fOtherMdfButtons) {
				updateModifiersStatus();
				fieldName= MODIFIERS;
			} else {
				fieldName= METHODS;
			}
			// tell all others
			fieldUpdated(fieldName);
		}
	}
		
	// -------- update message ----------------		

	/**
	 * Called when a field on this page changed
	 * @see ContainerPage#fieldUpdated
	 * Overridden (extended) by sub types	 
	 */			
	protected void fieldUpdated(String fieldName) {
		super.fieldUpdated(fieldName);
		if (fieldName == CONTAINER) {
			updateTypeNameStatus();
			updateSuperClassStatus();
			updateInterfacesStatus();
		} else if (fieldName ==  ENCLOSING || fieldName == PACKAGE) {
			updateTypeNameStatus();
		} else if (fieldName ==  ENCLOSINGSELECTION) {
			boolean isEnclosedType= isEnclosingTypeSelected();
			if (!isEnclosedType) {
				if (fAccMdfButtons.isSelected(2) || fAccMdfButtons.isSelected(3)) {
					fAccMdfButtons.setSelection(2, false);
					fAccMdfButtons.setSelection(3, false);
					fAccMdfButtons.setSelection(0, true);
				}
				
				if (fOtherMdfButtons.isSelected(fStaticMdfIndex)) {
					fOtherMdfButtons.setSelection(fStaticMdfIndex, false);
				}
			}
			fAccMdfButtons.enableSelectionButton(2, isEnclosedType);
			fAccMdfButtons.enableSelectionButton(3, isEnclosedType);
			fOtherMdfButtons.enableSelectionButton(fStaticMdfIndex, isEnclosedType);
			updateTypeNameStatus();
		}
	}
	
	
	// ----------- set / get -----------
	
	public String getTypeName() {
		return fTypeNameDialogField.getText();
	}
	
	public void setTypeName(String name, boolean canBeModified) {
		fTypeNameDialogField.setText(name);
		fTypeNameDialogField.setEnabled(canBeModified);
	}	
	
	/**
	 * Gets the selected modifiers.
	 * @see Flags 
	 */	
	public int getModifiers() {
		int mdf= 0;
		if (fAccMdfButtons.isSelected(0)) {
			mdf+= IConstants.AccPublic;
		} else if (fAccMdfButtons.isSelected(2)) {
			mdf+= IConstants.AccPrivate;
		} else if (fAccMdfButtons.isSelected(3)) {	
			mdf+= IConstants.AccProtected;
		}
		if (fOtherMdfButtons.isSelected(0)) {	
			mdf+= IConstants.AccAbstract;
		}
		if (fOtherMdfButtons.isSelected(1)) {	
			mdf+= IConstants.AccFinal;
		}
		if (fOtherMdfButtons.isSelected(fStaticMdfIndex)) {	
			mdf+= IConstants.AccStatic;
		}
		return mdf;
	}

	/**
	 * Sets the modifiers.
	 * @see IConstants 
	 */		
	public void setModifiers(int modifiers, boolean canBeModified) {
		if (Flags.isPublic(modifiers)) {
			fAccMdfButtons.setSelection(0, true);
		} else if (Flags.isPrivate(modifiers)) {
			fAccMdfButtons.setSelection(2, true);
		} else if (Flags.isProtected(modifiers)) {
			fAccMdfButtons.setSelection(3, true);
		} else {
			fAccMdfButtons.setSelection(1, true); // default
		}
		if (Flags.isAbstract(modifiers)) {
			fOtherMdfButtons.setSelection(0, true);
		}
		if (Flags.isFinal(modifiers)) {
			fOtherMdfButtons.setSelection(1, true);
		}		
		if (Flags.isStatic(modifiers)) {
			fOtherMdfButtons.setSelection(fStaticMdfIndex, true);
		}
		
		fAccMdfButtons.setEnabled(canBeModified);
		fOtherMdfButtons.setEnabled(canBeModified);
	}
	
	public String getSuperClass() {
		return fSuperClassDialogField.getText();
	}
	
	public void setSuperClass(String name, boolean canBeModified) {
		fSuperClassDialogField.setText(name);
		fSuperClassDialogField.setEnabled(canBeModified);
	}	
	
	/**
	 * Gets the currently chosen super interfaces
	 * @return returns a list of String
	 */
	public List getSuperInterfaces() {
		return fSuperInterfacesDialogField.getElements();
	}

	/**
	 * Sets the super interfaces
	 * @param interfacesNames a list of String
	 */	
	public void setSuperInterfaces(List interfacesNames, boolean canBeModified) {
		fSuperInterfacesDialogField.setElements(interfacesNames);
		fSuperInterfacesDialogField.setEnabled(canBeModified);
	}
			
	// ----------- validation ----------
	
	protected StatusInfo getTypeNameStatus() {
		return fTypeNameStatus;
	}
	
	protected StatusInfo getModifierStatus() {
		return fModifierStatus;
	}
	
	protected StatusInfo getSuperClassStatus() {
		return fSuperClassStatus;
	}
	
	protected StatusInfo getSuperInterfacesStatus() {
		return fSuperInterfacesStatus;
	}		
		
	/**
	 * Verify if the class name input field is valid
	 */
	private void updateTypeNameStatus() {
		fTypeNameStatus.setOK();
		String typeName= getTypeName();
		// must not be empty
		if ("".equals(typeName)) {
			fTypeNameStatus.setError(getResourceString(ERROR_TYPE_ENTERNAME));
			return;
		}
		if (typeName.indexOf('.') != -1) {
			fTypeNameStatus.setError(getResourceString(ERROR_TYPE_QUALIFIEDNAME));
			return;
		}
		IStatus val= JavaConventions.validateJavaTypeName(typeName);
		if (val.getSeverity() == IStatus.ERROR) {
			fTypeNameStatus.setError(getFormattedString(ERROR_TYPE_INVALIDNAME, val.getMessage()));
			return;
		} else if (val.getSeverity() == IStatus.ERROR) {
			fTypeNameStatus.setWarning(getFormattedString(WARNING_TYPE_NAMEDISCOURAGED, val.getMessage()));
			// continue checking
		}		

		// must not exist
		if (!isEnclosingTypeSelected()) {
			IPackageFragment pack= getPackageFragment();
			if (pack != null) {
				ICompilationUnit cu= pack.getCompilationUnit(typeName + ".java");
				if (cu.exists()) {
					fTypeNameStatus.setError(getResourceString(ERROR_TYPE_NAMEEXISTS));
					return;
				}
			}
		} else {
			IType type= getEnclosingType();
			if (type != null) {
				IType member= type.getType(typeName);
				if (member.exists()) {
					fTypeNameStatus.setError(getResourceString(ERROR_TYPE_NAMEEXISTS));
					return;
				}
			}
		}
	}
	
	/**
	 * Verify if the superclass name input field is valid
	 */
	private void updateSuperClassStatus() {
		fSuperClassStatus.setOK();
		IPackageFragmentRoot root= getPackageFragmentRoot();
		fSuperClassDialogField.enableButton(root != null);
		
		String sclassName= getSuperClass();
		if ("".equals(sclassName)) {
			// accept the empty field (stands for java.lang.Object)
			return;
		}
		IStatus val= JavaConventions.validateJavaTypeName(sclassName);
		if (!val.isOK()) {
			fSuperClassStatus.setError(getResourceString(ERROR_SUPER_INVALIDNAME));
			return;
		}
		
		if (root != null) {
			try {		
				IType type= JavaModelUtility.findType(root.getJavaProject(), sclassName);
				if (type == null) {
					fSuperClassStatus.setWarning(getResourceString(WARNING_SUPER_NOTEXISTS));
					return;
				} else {
					if (type.isInterface()) {
						fSuperClassStatus.setWarning(getFormattedString(WARNING_SUPER_NOTCLASS, sclassName));
						return;
					}
					int flags= type.getFlags();
					if (Flags.isFinal(flags)) {
						fSuperClassStatus.setWarning(getFormattedString(WARNING_SUPER_ISFINAL, sclassName));
						return;
					} else if (!JavaModelUtility.isVisible(getPackageFragment(), flags, type.getPackageFragment())) {
						fSuperClassStatus.setWarning(getFormattedString(WARNING_SUPER_NOTVISIBLE, sclassName));
						return;
					}
				}
			} catch (JavaModelException e) {
				ErrorDialog.openError(getShell(), "Error", null, e.getStatus());
			}							
		}
	}
	
	/**
	 * Verify if the list of superinterfaces is valid
	 */
	private void updateInterfacesStatus() {
		IPackageFragmentRoot root= getPackageFragmentRoot();
		fSuperInterfacesDialogField.enableCustomButton(0, root != null);
						
		if (root != null) {
			List elements= fSuperInterfacesDialogField.getElements();
			int nElements= elements.size();
			for (int i= 0; i < nElements; i++) {
				String intfname= (String)elements.get(i);
				try {
					IType type= JavaModelUtility.findType(root.getJavaProject(), intfname);
					if (type == null) {
						fSuperInterfacesStatus.setWarning(getFormattedString(WARNING_INTFC_NOTEXISTS, intfname));
						return;
					} else {
						if (type.isClass()) {
							fSuperInterfacesStatus.setWarning(getFormattedString(WARNING_INTFC_NOTINTERFACE, intfname));
							return;
						}
						if (!JavaModelUtility.isVisible(getPackageFragment(), type.getFlags(), type.getPackageFragment())) {
							fSuperInterfacesStatus.setWarning(getFormattedString(WARNING_INTFC_NOTVISIBLE, intfname));
							return;
						}
					}
				} catch (JavaModelException e) {
					ErrorDialog.openError(getShell(), "Error", null, e.getStatus());
				}					
			}				
		}
		fSuperInterfacesStatus.setOK();
	}

	/**
	 * Verify if modifiers are valid
	 */	
	private void updateModifiersStatus() {
		int modifiers= getModifiers();
		if (Flags.isFinal(modifiers) && Flags.isAbstract(modifiers)) {
			fModifierStatus.setError(getResourceString(ERROR_MODIFIERS_FINALANDABS));
		} else {
			fModifierStatus.setOK();
		}
	}
	
	private IType chooseSuperType() {
		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root == null) {
			return null;
		}	

		IResource[] resources= new IResource[] { root.getJavaProject().getProject() };
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(resources);
		scope.setIncludesBinaries(true);
		scope.setIncludesClasspaths(true);	

		IProject project= root.getJavaProject().getProject();
		TypeSelectionDialog dialog= new TypeSelectionDialog(getShell(), getWizard().getContainer(), scope, IJavaElementSearchConstants.CONSIDER_CLASSES, true, true);
		dialog.setTitle(getResourceString(SUPERCLASS_DIALOG + ".title"));
		dialog.setMessage(getResourceString(SUPERCLASS_DIALOG + ".message"));

		if (dialog.open() == dialog.OK) {
			return (IType) dialog.getPrimaryResult();
		}
		return null;
	}
	
	private void chooseSuperInterfaces() {
		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root == null) {
			return;
		}	

		IProject project= root.getJavaProject().getProject();
		SuperInterfaceSelectionDialog dialog= new SuperInterfaceSelectionDialog(getShell(), getWizard().getContainer(), fSuperInterfacesDialogField, project);
		dialog.setTitle(getResourceString(INTERFACES_DIALOG + ".title"));
		dialog.setMessage(getResourceString(INTERFACES_DIALOG + ".message"));
		dialog.open();
		return;
	}	
	
	
		
	// ---- creation ----------------

	
	public void createType(IProgressMonitor monitor) throws CoreException, InterruptedException {		
		monitor.beginTask(getResourceString(OPERATION_DESC), 10);
		
		IPackageFragment pack= createPackage(monitor);
		monitor.worked(1);
		
		String clName= fTypeNameDialogField.getText();
		
		boolean isInnerClass= isEnclosingTypeSelected();
		
		IType createdType;
		ImportsStructure imports;
		int indent= 0;
		if (!isInnerClass) {
			ICompilationUnit parentCU= pack.getCompilationUnit(clName + ".java");
			
			String[] prefOrder= ImportOrganizePreferencePage.getImportOrderPreference();
			int threshold= ImportOrganizePreferencePage.getImportNumberThreshold();			
			imports= new ImportsStructure(parentCU, prefOrder, threshold);
			
			String content= createTypeBody(imports, indent);
			createdType= parentCU.createType(content, null, true, new SubProgressMonitor(monitor, 5));
		} else {
			IType enclosingType= getEnclosingType();
			
			// if we are working on a enclosed type that is open in an editor,
			// then replace the enclosing type with its working copy
			IType workingCopy= EditorUtility.getWorkingCopy(enclosingType);
			if (workingCopy != null) {
				enclosingType= workingCopy;
			}

			ICompilationUnit parentCU= enclosingType.getCompilationUnit();
			imports= new ImportsStructure(parentCU);
			
			indent= StubUtility.getIndentUsed(enclosingType, 4) + 1;
			String content= createTypeBody(imports, indent);
			createdType= enclosingType.createType(content, null, true, new SubProgressMonitor(monitor, 1));
		}
		
		// add imports for superclass/interfaces, so the type can be parsed correctly
		imports.create(!isInnerClass, new SubProgressMonitor(monitor, 1));
		
		String[] methods= evalMethods(createdType, indent + 1, imports, new SubProgressMonitor(monitor, 1));
		if (methods.length > 0) {
			IMethod lastCreated= null;
			for (int i= 0; i < methods.length; i++) {
				String curr= methods[i];
				lastCreated= createdType.createMethod(curr, lastCreated, true, monitor);
			}		
			
			// add imports
			imports.create(!isInnerClass, new SubProgressMonitor(monitor, 1));
		}
		
		ConfigurableOption[] options= JavaPlugin.getDefault().getCodeFormatterOptions();
		String formattedContent= CodeFormatter.format(createdType.getSource(), indent, options);
		
		ISourceRange range= createdType.getSourceRange();
		IBuffer buf= createdType.getCompilationUnit().getBuffer();
		buf.replace(range.getOffset(), range.getLength(), formattedContent);
		
		fCreatedType= createdType;
		monitor.done();
	}	
			
	public IType getNewType() {
		return fCreatedType;
	}
	
	// ---- construct cu body----------------
		
	private void writeSuperClass(StringBuffer buf, ImportsStructure imports) {
		String typename= getSuperClass();
		if (fIsClass && !"".equals(typename) && !"java.lang.Object".equals(typename)) {
			buf.append(" extends ");
			imports.sortIn(typename);
			buf.append(Signature.getSimpleName(typename));
		}
	}
	
	private void writeSuperInterfaces(StringBuffer buf, ImportsStructure imports) {
		List interfaces= getSuperInterfaces();
		int last= interfaces.size() - 1;
		if (last >= 0) {
			if (fIsClass) {
				buf.append(" implements ");
			} else {
				buf.append(" extends ");
			}
			for (int i= 0; i <= last; i++) {
				String typename= (String) interfaces.get(i);
				imports.sortIn(typename);
				buf.append(Signature.getSimpleName(typename));
				if (i < last) {
					buf.append(", ");
				}
			}
		}
	}
	
	protected String createTypeBody(ImportsStructure imports, int indent) {	
		StringBuffer buf= new StringBuffer();
		
		int modifiers= getModifiers();
		buf.append(Flags.toString(modifiers));
		if (modifiers != 0) {
			buf.append(' ');
		}
		buf.append(fIsClass ? "class " : "interface ");
		buf.append(getTypeName());
		writeSuperClass(buf, imports);
		writeSuperInterfaces(buf, imports);	
		buf.append(" {\n");
		buf.append('\n');
		buf.append("}\n");
		return buf.toString();
	}
	
	protected String[] evalMethods(IType parent, int indent, ImportsStructure imports, IProgressMonitor monitor) throws CoreException {
		return new String[0];
	}
		
			
}