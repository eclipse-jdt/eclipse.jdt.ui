/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import java.util.ArrayList;import java.util.List;import org.eclipse.swt.SWT;import org.eclipse.swt.graphics.Image;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.core.resources.IProject;import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IWorkspaceRoot;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.SubProgressMonitor;import org.eclipse.jface.dialogs.ErrorDialog;import org.eclipse.jface.viewers.LabelProvider;import org.eclipse.jdt.core.Flags;import org.eclipse.jdt.core.IBuffer;import org.eclipse.jdt.core.IClassFile;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jdt.core.ISourceRange;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.ITypeHierarchy;import org.eclipse.jdt.core.JavaConventions;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.Signature;import org.eclipse.jdt.core.search.IJavaSearchScope;import org.eclipse.jdt.core.search.SearchEngine;import org.eclipse.jdt.ui.IJavaElementSearchConstants;import org.eclipse.jdt.ui.JavaElementLabelProvider;import org.eclipse.jdt.internal.compiler.ConfigurableOption;import org.eclipse.jdt.internal.compiler.env.IConstants;import org.eclipse.jdt.internal.formatter.CodeFormatter;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaPluginImages;import org.eclipse.jdt.internal.ui.codemanipulation.IImportsStructure;import org.eclipse.jdt.internal.ui.codemanipulation.ImportsStructure;import org.eclipse.jdt.internal.ui.codemanipulation.StubUtility;import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;import org.eclipse.jdt.internal.ui.dialogs.TypeSelectionDialog;import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;import org.eclipse.jdt.internal.ui.preferences.ImportOrganizePreferencePage;import org.eclipse.jdt.internal.ui.util.JavaModelUtility;import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;import org.eclipse.jdt.internal.ui.wizards.dialogfields.Separator;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonStatusDialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;import org.eclipse.jdt.internal.ui.wizards.swt.MGridData;import org.eclipse.jdt.internal.ui.wizards.swt.MGridUtil;

public abstract class TypePage extends ContainerPage {
	
	private final static String PAGE_NAME= "TypePage";
	
	protected static final String PACKAGE= PAGE_NAME + ".package";	
	protected static final String ENCLOSING= PAGE_NAME + ".enclosing";
	protected static final String ENCLOSINGSELECTION= ENCLOSING + ".selection";
	
	protected final static String TYPENAME= PAGE_NAME + ".typename";
	protected final static String SUPER= PAGE_NAME + ".superclass";
	protected final static String INTERFACES= PAGE_NAME + ".interfaces";
	protected final static String MODIFIERS= PAGE_NAME + ".modifiers";
	protected final static String METHODS= PAGE_NAME + ".methods";
	
	private static final String OPERATION_DESC= PAGE_NAME + ".operationdesc";
	
	private static final String STATUS_DEFAULT= PAGE_NAME + ".default";
	
	private static final String ERROR_PACKAGE_INVALIDNAME= PAGE_NAME + ".error.InvalidPackageName";
	private static final String ERROR_PACKAGE_CLASHOUTPUTLOCATION= PAGE_NAME + ".error.ClashOutputLocation";
	private static final String WARNING_PACKAGE_DISCOURAGEDNAME= PAGE_NAME + ".warning.DiscouragedPackageName";
	
	private static final String ERROR_ENCLOSING_ENTERNAME= PAGE_NAME + ".error.EnclosingTypeEnterName";
	private static final String ERROR_ENCLOSING_NOTEXISTS= PAGE_NAME + ".error.EnclosingTypeNotExists";
	private static final String ERROR_ENCLOSING_PARENTISBINARY= PAGE_NAME + ".error.EnclosingNotInCU";

	private static final String PACKAGE_DIALOG= PAGE_NAME + ".ChoosePackageDialog";
	private static final String ENCLOSING_DIALOG= PAGE_NAME + ".ChooseEnclosingTypeDialog";
	
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

	private StringButtonStatusDialogField fPackageDialogField;
	
	private SelectionButtonDialogField fEnclosingTypeSelection;
	private StringButtonDialogField fEnclosingTypeDialogField;
		
	private boolean fCanModifyPackage;
	private boolean fCanModifyEnclosingType;
	
	private IPackageFragment fCurrPackage;
	
	private IType fCurrEnclosingType;	
	private StringDialogField fTypeNameDialogField;
	
	private StringButtonDialogField fSuperClassDialogField;
	private ListDialogField fSuperInterfacesDialogField;
	
	private SelectionButtonDialogFieldGroup fAccMdfButtons;
	private SelectionButtonDialogFieldGroup fOtherMdfButtons;
	
	private IType fCreatedType;
	
	protected IStatus fEnclosingTypeStatus;
	protected IStatus fPackageStatus;
	protected IStatus fTypeNameStatus;
	protected IStatus fSuperClassStatus;
	protected IStatus fModifierStatus;
	protected IStatus fSuperInterfacesStatus;	
	
	private boolean fIsClass;
	private int fStaticMdfIndex;
	
	public TypePage(boolean isClass, String pageName, IWorkspaceRoot root) {
		super(pageName, root);
		fCreatedType= null;
		
		fIsClass= isClass;
		
		TypeFieldsAdapter adapter= new TypeFieldsAdapter();
		
		
		fPackageDialogField= new StringButtonStatusDialogField(adapter);
		fPackageDialogField.setDialogFieldListener(adapter);
		fPackageDialogField.setLabelText(getResourceString(PACKAGE + ".label"));
		fPackageDialogField.setButtonLabel(getResourceString(PACKAGE + ".button"));
		fPackageDialogField.setStatusWidthHint(getResourceString(STATUS_DEFAULT));
				
		fEnclosingTypeSelection= new SelectionButtonDialogField(SWT.CHECK);
		fEnclosingTypeSelection.setDialogFieldListener(adapter);
		fEnclosingTypeSelection.setLabelText(getResourceString(ENCLOSINGSELECTION + ".label"));
		
		fEnclosingTypeDialogField= new StringButtonDialogField(adapter);
		fEnclosingTypeDialogField.setDialogFieldListener(adapter);
		fEnclosingTypeDialogField.setLabelText(getResourceString(ENCLOSING + ".label"));
		fEnclosingTypeDialogField.setButtonLabel(getResourceString(ENCLOSING + ".button"));
		
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

		fPackageStatus= new StatusInfo();
		fEnclosingTypeStatus= new StatusInfo();
		
		fCanModifyPackage= true;
		fCanModifyEnclosingType= true;
		updateEnableState();
					
		fTypeNameStatus= new StatusInfo();
		fSuperClassStatus= new StatusInfo();
		fSuperInterfacesStatus= new StatusInfo();
		fModifierStatus= new StatusInfo();
	}
	
	/**
	 * Initializes all fields provided by the type page with a give
	 * java element as selection.
	 * @param elem The initial selection of this page or null if no
	 *             selection was available
	 */
	protected void initTypePage(IJavaElement elem) {
		String initSuperclass= "java.lang.Object";
		ArrayList initSuperinterfaces= new ArrayList(5);

		IPackageFragment pack= null;
				
		if (elem != null) {
			pack= (IPackageFragment) JavaModelUtility.findElementOfKind(elem, IJavaElement.PACKAGE_FRAGMENT);
			try {
				IType type= null;
				switch (elem.getElementType()) {
				case IJavaElement.TYPE:
					type= (IType)elem;
					break;
				}
				if (type != null && type.exists()) {
					String superName= JavaModelUtility.getFullyQualifiedName(type);
					if (type.isInterface()) {
						initSuperinterfaces.add(superName);
					} else {
						initSuperclass= superName;
					}
				}
			} catch (JavaModelException e) {
				// ignore this exception now
			}
		}			

		setPackageFragment(pack, true);
		setEnclosingType(null, true);
		setEnclosingTypeSelection(false, true);
	
		setTypeName("", true);
		setSuperClass(initSuperclass, true);
		setSuperInterfaces(initSuperinterfaces, true);
		
	}		
	
	// -------- UI Creation ---------
	
	/**
	 * Creates a separator line
	 * @param composite The parent composite
	 * @param nColumns Number of columns to span
	 */
	protected void createSeparator(Composite composite, int nColumns) {
		(new Separator()).doFillIntoGrid(composite, nColumns, 12);		
		(new Separator(SWT.SEPARATOR | SWT.HORIZONTAL)).doFillIntoGrid(composite, nColumns, 8);		
	}

	/**
	 * Creates the controls for the package name field
	 * @param composite The parent composite
	 * @param nColumns Number of columns to span
	 */	
	protected void createPackageControls(Composite composite, int nColumns) {
		fPackageDialogField.doFillIntoGrid(composite, 4);
	}

	/**
	 * Creates the controls for the enclosing type name field
	 * @param composite The parent composite
	 * @param nColumns Number of columns to span
	 */		
	protected void createEnclosingTypeControls(Composite composite, int nColumns) {
		fEnclosingTypeSelection.doFillIntoGrid(composite, 1);
		
		Control c= fEnclosingTypeDialogField.getTextControl(composite);
		c.setLayoutData(MGridUtil.createHorizontalFill());
		LayoutUtil.setHorizontalSpan(c, 2);
		c= fEnclosingTypeDialogField.getChangeControl(composite);
		c.setLayoutData(new MGridData(MGridData.HORIZONTAL_ALIGN_FILL));
	}	

	/**
	 * Creates the controls for the type name field
	 * @param composite The parent composite
	 * @param nColumns Number of columns to span
	 */		
	protected void createTypeNameControls(Composite composite, int nColumns) {
		fTypeNameDialogField.doFillIntoGrid(composite, nColumns - 1);
		DialogField.createEmptySpace(composite);
		
	}

	/**
	 * Creates the controls for the modifiers radio/ceckbox buttons
	 * @param composite The parent composite
	 * @param nColumns Number of columns to span
	 */		
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

	/**
	 * Creates the controls for the superclass name field
	 * @param composite The parent composite
	 * @param nColumns Number of columns to span
	 */		
	protected void createSuperClassControls(Composite composite, int nColumns) {
		fSuperClassDialogField.doFillIntoGrid(composite, nColumns);
	}

	/**
	 * Creates the controls for the superclass name field
	 * @param composite The parent composite
	 * @param nColumns Number of columns to span
	 */			
	protected void createSuperInterfacesControls(Composite composite, int nColumns) {
		fSuperInterfacesDialogField.doFillIntoGrid(composite, nColumns);
		MGridData gd= (MGridData)fSuperInterfacesDialogField.getListControl(null).getLayoutData();
		gd.heightHint= 80;
		gd.grabExcessVerticalSpace= false;
	}

	
	/**
	 * Sets the focus on the container if empty, elso on type name
	 */		
	protected void setFocus() {
		if (getContainerText().length() == 0) {
			setFocusOnContainer();
		} else {
			fTypeNameDialogField.setFocus();
		}
	}
				
	// -------- TypeFieldsAdapter --------

	private class TypeFieldsAdapter implements IStringButtonAdapter, IDialogFieldListener, IListAdapter {
		
		// -------- IStringButtonAdapter
		
		public void changeControlPressed(DialogField field) {
			if (field == fPackageDialogField) {
				IPackageFragment pack= choosePackage();	
				if (pack != null) {
					fPackageDialogField.setText(pack.getElementName());
				}
			} else if (field == fEnclosingTypeDialogField) {
				IType type= chooseEnclosingType();
				if (type != null) {
					fEnclosingTypeDialogField.setText(JavaModelUtility.getFullyQualifiedName(type));
				}
			} else if (field == fSuperClassDialogField) {
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
			if (field == fPackageDialogField) {
				fPackageStatus= packageChanged();
				updatePackageStatusLabel();
				fieldName= PACKAGE;
			} else if (field == fEnclosingTypeDialogField) {
				fEnclosingTypeStatus= enclosingTypeChanged();
				fieldName= ENCLOSING;
			} else if (field == fEnclosingTypeSelection) {
				updateEnableState();
				fieldName= ENCLOSINGSELECTION;
			} else if (field == fTypeNameDialogField) {
				fTypeNameStatus= typeNameChanged();
				fieldName= TYPENAME;
			} else if (field == fSuperClassDialogField) {
				fSuperClassStatus= superClassChanged();
				fieldName= SUPER;
			} else if (field == fSuperInterfacesDialogField) {
				fSuperInterfacesStatus= superInterfacesChanged();
				fieldName= INTERFACES;
			} else if (field == fOtherMdfButtons) {
				fModifierStatus= modifiersChanged();
				fieldName= MODIFIERS;
			} else {
				fieldName= METHODS;
			}
			// tell all others
			handleFieldChanged(fieldName);
		}
	}
		
	// -------- update message ----------------		

	/**
	 * Called whenever a content of a field has changed.
	 * @see ContainerPage#handleFieldChanged
	 */			
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);
		if (fieldName == CONTAINER) {
			fPackageStatus= packageChanged();
			fEnclosingTypeStatus= enclosingTypeChanged();			
			fTypeNameStatus= typeNameChanged();
			fSuperClassStatus= superClassChanged();
			fSuperInterfacesStatus= superInterfacesChanged();
		} else if (fieldName ==  ENCLOSING || fieldName == PACKAGE) {
			fTypeNameStatus= typeNameChanged();
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
			fTypeNameStatus= typeNameChanged();
		}
	}
	
	
	
	// ---- set / get ----------------
	
	/**
	 * Gets the text of package field
	 */
	public String getPackageText() {
		return fPackageDialogField.getText();
	}

	/**
	 * Gets the text of enclosing type field
	 */	
	public String getEnclosingTypeText() {
		return fEnclosingTypeDialogField.getText();
	}	
	
	
	/**
	 * Returns the package fragment corresponding to the current input.
	 * Can be null if the input could not be resolved.
	 */
	public IPackageFragment getPackageFragment() {
		if (!isEnclosingTypeSelected()) {
			return fCurrPackage;
		} else {
			if (fCurrEnclosingType != null) {
				return fCurrEnclosingType.getPackageFragment();
			}
		}
		return null;
	}
	
	/**
	 * Sets the package fragment.
	 * This will update model and the text of the control.
	 * @param canBeModified Selects if the package fragment can be changed by the user
	 */
	public void setPackageFragment(IPackageFragment pack, boolean canBeModified) {
		fCurrPackage= pack;
		fCanModifyPackage= canBeModified;
		String str= (pack == null) ? "" : pack.getElementName();
		fPackageDialogField.setText(str);
		updateEnableState();
	}	

	/**
	 * Returns the encloding type corresponding to the current input.
	 * Can be null if enclosing type is not selected or the input could not
	 * be resolved.
	 */
	public IType getEnclosingType() {
		if (isEnclosingTypeSelected()) {
			return fCurrEnclosingType;
		}
		return null;
	}

	/**
	 * Sets the package fragment.
	 * This will update model and the text of the control.
	 * @param canBeModified Selects if the enclosing type can be changed by the user
	 */	
	public void setEnclosingType(IType type, boolean canBeModified) {
		fCurrEnclosingType= type;
		fCanModifyEnclosingType= canBeModified;
		String str= (type == null) ? "" : JavaModelUtility.getFullyQualifiedName(type);
		fEnclosingTypeDialogField.setText(str);
		updateEnableState();
	}
	
	/**
	 * Returns true if the enclosing type selection check box is enabled
	 */
	public boolean isEnclosingTypeSelected() {
		return fEnclosingTypeSelection.isSelected();
	}

	/**
	 * Sets the enclosing type selection checkbox
	 * @param canBeModified Selects if the enclosing type selection can be changed by the user
	 */	
	public void setEnclosingTypeSelection(boolean isSelected, boolean canBeModified) {
		fEnclosingTypeSelection.setSelection(isSelected);
		fEnclosingTypeSelection.setEnabled(canBeModified);
		updateEnableState();
	}
	
	/**
	 * Gets the type name
	 */
	public String getTypeName() {
		return fTypeNameDialogField.getText();
	}

	/**
	 * Sets the type name
	 * @param canBeModified Selects if the type name can be changed by the user
	 */	
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
	 * @param canBeModified Selects if the modifiers can be changed by the user
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
	
	/**
	 * Gets the content of the super class text field
	 */
	public String getSuperClass() {
		return fSuperClassDialogField.getText();
	}

	/**
	 * Sets the super class name
	 * @param canBeModified Selects if the super class can be changed by the user
	 */		
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
		
	/**
	 * Called when the package field has changed
	 * The method validates the package name and returns the status of the validation
	 * This also updates the package fragment model.
	 * Can be extended to add more validation
	 */
	protected IStatus packageChanged() {
		StatusInfo status= new StatusInfo();
		fPackageDialogField.enableButton(getPackageFragmentRoot() != null);
		
		String packName= getPackageText();
		if (!"".equals(packName)) {
			IStatus val= JavaConventions.validatePackageName(packName);
			if (val.getSeverity() == IStatus.ERROR) {
				status.setError(getFormattedString(ERROR_PACKAGE_INVALIDNAME, val.getMessage()));
				return status;
			} else if (val.getSeverity() == IStatus.WARNING) {
				status.setWarning(getFormattedString(WARNING_PACKAGE_DISCOURAGEDNAME, val.getMessage()));
				// continue
			}
		}
		
		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root != null) {
			IPackageFragment pack= root.getPackageFragment(packName);
			try {
				IPath rootPath= root.getPath();
				IPath outputPath= root.getJavaProject().getOutputLocation();
				if (rootPath.isPrefixOf(outputPath) && !rootPath.equals(outputPath)) {
					// if the bin folder is inside of our root, dont allow to name a package
					// like the bin folder
					IPath packagePath= pack.getUnderlyingResource().getFullPath();
					if (outputPath.isPrefixOf(packagePath)) {
						status.setError(getResourceString(ERROR_PACKAGE_CLASHOUTPUTLOCATION));
						return status;
					}
				}
			} catch (JavaModelException e) {
			}
			
			fCurrPackage= pack;
		} else {
			status.setError("root undef");
		}
		return status;
	}

	/*
	 * Updates the 'default' label next to the package field
	 */	
	private void updatePackageStatusLabel() {
		String packName= fPackageDialogField.getText();
		
		if ("".equals(packName)) {
			fPackageDialogField.setStatus(getResourceString(STATUS_DEFAULT));
		} else {
			fPackageDialogField.setStatus("");
		}
	}
	
	/*
	 * Updates the enable state of buttons related to the enclosing type selection checkbox
	 */
	private void updateEnableState() {
		boolean enclosing= isEnclosingTypeSelected();
		fPackageDialogField.setEnabled(fCanModifyPackage && !enclosing);
		fEnclosingTypeDialogField.setEnabled(fCanModifyEnclosingType && enclosing);
	}	

	/**
	 * Called when the enclosing type name has changed
	 * The method validates the enclosing type and returns the status of the validation
	 * This also updates the enclosing type model.
	 * Can be extended to add more validation
	 */
	protected IStatus enclosingTypeChanged() {
		StatusInfo status= new StatusInfo();
		fCurrEnclosingType= null;
		
		IPackageFragmentRoot root= getPackageFragmentRoot();
		
		fEnclosingTypeDialogField.enableButton(root != null);
		if (root == null) {
			status.setError("");
			return status;
		}
		
		String enclName= getEnclosingTypeText();
		if ("".equals(enclName)) {
			status.setError(getResourceString(ERROR_ENCLOSING_ENTERNAME));
			return status;
		}
		try {
			IType type= JavaModelUtility.findType(root.getJavaProject(), enclName);
			if (type == null) {
				status.setError(getResourceString(ERROR_ENCLOSING_NOTEXISTS));
				return status;
			}

			if (type.getCompilationUnit() == null) {
				status.setError(getResourceString(ERROR_ENCLOSING_PARENTISBINARY));
				return status;
			}
			fCurrEnclosingType= type;
			return status;
		} catch (JavaModelException e) {
			status.setError(getResourceString(ERROR_ENCLOSING_NOTEXISTS));
			JavaPlugin.getDefault().getLog().log(e.getStatus());
			return status;
		}
	}
	
	/**
	 * Called when the type name has changed
	 * The method validates the type name and returns the status of the validation
	 * Can be extended to add more validation
	 */
	protected IStatus typeNameChanged() {
		StatusInfo status= new StatusInfo();
		String typeName= getTypeName();
		// must not be empty
		if ("".equals(typeName)) {
			status.setError(getResourceString(ERROR_TYPE_ENTERNAME));
			return status;
		}
		if (typeName.indexOf('.') != -1) {
			status.setError(getResourceString(ERROR_TYPE_QUALIFIEDNAME));
			return status;
		}
		IStatus val= JavaConventions.validateJavaTypeName(typeName);
		if (val.getSeverity() == IStatus.ERROR) {
			status.setError(getFormattedString(ERROR_TYPE_INVALIDNAME, val.getMessage()));
			return status;
		} else if (val.getSeverity() == IStatus.ERROR) {
			status.setWarning(getFormattedString(WARNING_TYPE_NAMEDISCOURAGED, val.getMessage()));
			// continue checking
		}		

		// must not exist
		if (!isEnclosingTypeSelected()) {
			IPackageFragment pack= getPackageFragment();
			if (pack != null) {
				ICompilationUnit cu= pack.getCompilationUnit(typeName + ".java");
				if (cu.exists()) {
					status.setError(getResourceString(ERROR_TYPE_NAMEEXISTS));
					return status;
				}
			}
		} else {
			IType type= getEnclosingType();
			if (type != null) {
				IType member= type.getType(typeName);
				if (member.exists()) {
					status.setError(getResourceString(ERROR_TYPE_NAMEEXISTS));
					return status;
				}
			}
		}
		return status;
	}
	
	/**
	 * Called when the superclass name has changed
	 * The method validates the superclass name and returns the status of the validation
	 * Can be extended to add more validation
	 */
	protected IStatus superClassChanged() {
		StatusInfo status= new StatusInfo();
		IPackageFragmentRoot root= getPackageFragmentRoot();
		fSuperClassDialogField.enableButton(root != null);
		
		String sclassName= getSuperClass();
		if ("".equals(sclassName)) {
			// accept the empty field (stands for java.lang.Object)
			return status;
		}
		IStatus val= JavaConventions.validateJavaTypeName(sclassName);
		if (!val.isOK()) {
			status.setError(getResourceString(ERROR_SUPER_INVALIDNAME));
			return status;
		}
		
		if (root != null) {
			try {		
				IType type= JavaModelUtility.findType(root.getJavaProject(), sclassName);
				if (type == null) {
					status.setWarning(getResourceString(WARNING_SUPER_NOTEXISTS));
					return status;
				} else {
					if (type.isInterface()) {
						status.setWarning(getFormattedString(WARNING_SUPER_NOTCLASS, sclassName));
						return status;
					}
					int flags= type.getFlags();
					if (Flags.isFinal(flags)) {
						status.setWarning(getFormattedString(WARNING_SUPER_ISFINAL, sclassName));
						return status;
					} else if (!JavaModelUtility.isVisible(getPackageFragment(), flags, type.getPackageFragment())) {
						status.setWarning(getFormattedString(WARNING_SUPER_NOTVISIBLE, sclassName));
						return status;
					}
				}
			} catch (JavaModelException e) {
				ErrorDialog.openError(getShell(), "Error", null, e.getStatus());
			}							
		} else {
			status.setError("");
		}
		return status;
		
	}
	
	/**
	 * Called when the list of super interface has changed
	 * The method validates the superinterfaces and returns the status of the validation
	 * Can be extended to add more validation
	 */
	protected IStatus superInterfacesChanged() {
		StatusInfo status= new StatusInfo();
		
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
						status.setWarning(getFormattedString(WARNING_INTFC_NOTEXISTS, intfname));
						return status;
					} else {
						if (type.isClass()) {
							status.setWarning(getFormattedString(WARNING_INTFC_NOTINTERFACE, intfname));
							return status;
						}
						if (!JavaModelUtility.isVisible(getPackageFragment(), type.getFlags(), type.getPackageFragment())) {
							status.setWarning(getFormattedString(WARNING_INTFC_NOTVISIBLE, intfname));
							return status;
						}
					}
				} catch (JavaModelException e) {
					ErrorDialog.openError(getShell(), "Error", null, e.getStatus());
				}					
			}				
		}
		return status;
	}

	/**
	 * Called when the modifiers have changed
	 * The method validates the modifiers and returns the status of the validation
	 * Can be extended to add more validation
	 */
	protected IStatus modifiersChanged() {
		StatusInfo status= new StatusInfo();
		int modifiers= getModifiers();
		if (Flags.isFinal(modifiers) && Flags.isAbstract(modifiers)) {
			status.setError(getResourceString(ERROR_MODIFIERS_FINALANDABS));
		}
		return status;
	}
	
	// selection dialogs
	
	
	private IPackageFragment choosePackage() {
		IPackageFragmentRoot froot= getPackageFragmentRoot();
		IJavaElement[] packages= null;
		try {
			if (froot != null) {
				packages= froot.getChildren();
			}
		} catch (JavaModelException e) {
		}
		if (packages == null) {
			packages= new IJavaElement[0];
		}
		
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT), false, false);
		dialog.setTitle(getResourceString(PACKAGE_DIALOG + ".title"));
		dialog.setMessage(getResourceString(PACKAGE_DIALOG + ".description"));
		dialog.setEmptyListMessage(getResourceString(PACKAGE_DIALOG + ".empty"));
		if (dialog.open(packages) == dialog.OK) {;
			return (IPackageFragment) dialog.getPrimaryResult();
		}
		return null;
	}
	
	private IType chooseEnclosingType() {
		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root == null) {
			return null;
		}

		IResource[] resources= new IResource[] { root.getJavaProject().getProject() };
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(resources);
		scope.setIncludesBinaries(false);
		scope.setIncludesClasspaths(false);	
			
		TypeSelectionDialog dialog= new TypeSelectionDialog(getShell(), getWizard().getContainer(), scope, IJavaElementSearchConstants.CONSIDER_TYPES, false, false);
		dialog.setTitle(getResourceString(ENCLOSING_DIALOG + ".title"));
		dialog.setMessage(getResourceString(ENCLOSING_DIALOG + ".description"));
		if (dialog.open() == dialog.OK) {	
			return (IType) dialog.getPrimaryResult();
		}
		return null;
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

	/**
	 * Creates a type using the current field values
	 */
	public void createType(IProgressMonitor monitor) throws CoreException, InterruptedException {		
		monitor.beginTask(getResourceString(OPERATION_DESC), 10);
		
		IPackageFragmentRoot root= getPackageFragmentRoot();
		IPackageFragment pack= getPackageFragment();
		if (pack == null) {
			pack= root.getPackageFragment("");
		}
		
		if (!pack.exists()) {
			String packName= pack.getElementName();
			pack= root.createPackageFragment(packName, true, null);
		}		
		
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
			
			String content= createTypeBody(imports);
			createdType= parentCU.createType(content, null, false, new SubProgressMonitor(monitor, 5));
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
			
			String content= createTypeBody(imports);
			createdType= enclosingType.createType(content, null, false, new SubProgressMonitor(monitor, 1));
		
			indent= StubUtility.getIndentUsed(enclosingType, 4) + 1;
		}
		
		// add imports for superclass/interfaces, so the type can be parsed correctly
		imports.create(!isInnerClass, new SubProgressMonitor(monitor, 1));
		
		String[] methods= evalMethods(createdType, imports, new SubProgressMonitor(monitor, 1));
		if (methods.length > 0) {
			for (int i= 0; i < methods.length; i++) {
				createdType.createMethod(methods[i], null, false, null);
			}
			// add imports
			imports.create(!isInnerClass, null);
		} 
		monitor.worked(1);	
		
		
		ConfigurableOption[] options= JavaPlugin.getDefault().getCodeFormatterOptions();
		String formattedContent= CodeFormatter.format(createdType.getSource(), indent, options);
		
		ISourceRange range= createdType.getSourceRange();
		IBuffer buf= createdType.getCompilationUnit().getBuffer();
		buf.replace(range.getOffset(), range.getLength(), formattedContent);
		if (!isInnerClass) {
			buf.save(new SubProgressMonitor(monitor, 1), false);
		} else {
			monitor.worked(1);
		}
		fCreatedType= createdType;
		monitor.done();
	}	

	/**
	 * Returns the created type. Only valid after createType has been invoked
	 */			
	public IType getCreatedType() {
		return fCreatedType;
	}
	
	// ---- construct cu body----------------
		
	private void writeSuperClass(StringBuffer buf, IImportsStructure imports) {
		String typename= getSuperClass();
		if (fIsClass && !"".equals(typename) && !"java.lang.Object".equals(typename)) {
			buf.append(" extends ");
			imports.addImport(typename);
			buf.append(Signature.getSimpleName(typename));
		}
	}
	
	private void writeSuperInterfaces(StringBuffer buf, IImportsStructure imports) {
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
				imports.addImport(typename);
				buf.append(Signature.getSimpleName(typename));
				if (i < last) {
					buf.append(", ");
				}
			}
		}
	}

	/*
	 * Called from createType to construct the source for this type
	 */		
	private String createTypeBody(IImportsStructure imports) {	
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

	/**
	 * Called from createType to allow adding methods for the newly created type
	 * Returns array of sources of the methods that have to be added
	 * @param parent The type where the methods will be added to 
	 */		
	protected String[] evalMethods(IType parent, IImportsStructure imports, IProgressMonitor monitor) throws CoreException {
		return new String[0];
	}
	
	/**
	 * Creates the bodies of all unimplemented methods or/and all constructors
	 * Can be used by implementors of TypePage to add method stub checkboxes
	 */
	protected String[] constructInheritedMethods(IType type, boolean doConstructors, boolean doUnimplementedMethods, IImportsStructure imports, IProgressMonitor monitor) throws CoreException {
		List newMethods= new ArrayList();
		ITypeHierarchy hierarchy= type.newSupertypeHierarchy(monitor);
		if (doConstructors) {
			IType superclass= hierarchy.getSuperclass(type);
			if (superclass != null) {
				StubUtility.evalConstructors(type, superclass, newMethods, imports);
			}
		}
		if (doUnimplementedMethods) {
			StubUtility.evalUnimplementedMethods(type, hierarchy, newMethods, imports);
		}
		return (String[]) newMethods.toArray(new String[newMethods.size()]);		
		
	}	
		
			
}