/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.compiler.env.IConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.codemanipulation.IImportsStructure;
import org.eclipse.jdt.internal.ui.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.ui.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.TypeSelectionDialog;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.ImportOrganizePreferencePage;
import org.eclipse.jdt.internal.ui.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.Separator;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonStatusDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridData;

/**
 * <code>TypePage</code> contains controls and validation routines for a 'New Type WizardPage'
 * Implementors decide which components to add and to enable. Implementors can also
 * customize the validation code.
 * <code>TypePage</code> is intended to serve as base class of all wizards that create types.
 * Applets, Servlets, Classes, Interfaces...
 * See <code>NewClassCreationWizardPage</code> or <code>NewInterfaceCreationWizardPage</code> for an
 * example usage of TypePage.
 */
public abstract class TypePage extends ContainerPage {
	
	private final static String PAGE_NAME= "TypePage"; //$NON-NLS-1$
	
	protected final static String PACKAGE= PAGE_NAME + ".package";	 //$NON-NLS-1$
	protected final static String ENCLOSING= PAGE_NAME + ".enclosing"; //$NON-NLS-1$
	protected final static String ENCLOSINGSELECTION= ENCLOSING + ".selection"; //$NON-NLS-1$
	
	protected final static String TYPENAME= PAGE_NAME + ".typename"; //$NON-NLS-1$
	protected final static String SUPER= PAGE_NAME + ".superclass"; //$NON-NLS-1$
	protected final static String INTERFACES= PAGE_NAME + ".interfaces"; //$NON-NLS-1$
	protected final static String MODIFIERS= PAGE_NAME + ".modifiers"; //$NON-NLS-1$
	protected final static String METHODS= PAGE_NAME + ".methods"; //$NON-NLS-1$

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
	
	private IType fSuperClass;
	
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
	
	private final int PUBLIC_INDEX= 0, DEFAULT_INDEX= 1, PRIVATE_INDEX= 2, PROTECTED_INDEX= 3;
	private final int ABSTRACT_INDEX= 0, FINAL_INDEX= 1;
	
	public TypePage(boolean isClass, String pageName, IWorkspaceRoot root) {
		super(pageName, root);
		fCreatedType= null;
		
		fIsClass= isClass;
		
		TypeFieldsAdapter adapter= new TypeFieldsAdapter();
		
		fPackageDialogField= new StringButtonStatusDialogField(adapter);
		fPackageDialogField.setDialogFieldListener(adapter);
		fPackageDialogField.setLabelText(NewWizardMessages.getString("TypePage.package.label")); //$NON-NLS-1$
		fPackageDialogField.setButtonLabel(NewWizardMessages.getString("TypePage.package.button")); //$NON-NLS-1$
		fPackageDialogField.setStatusWidthHint(NewWizardMessages.getString("TypePage.default")); //$NON-NLS-1$
				
		fEnclosingTypeSelection= new SelectionButtonDialogField(SWT.CHECK);
		fEnclosingTypeSelection.setDialogFieldListener(adapter);
		fEnclosingTypeSelection.setLabelText(NewWizardMessages.getString("TypePage.enclosing.selection.label")); //$NON-NLS-1$
		
		fEnclosingTypeDialogField= new StringButtonDialogField(adapter);
		fEnclosingTypeDialogField.setDialogFieldListener(adapter);
		fEnclosingTypeDialogField.setButtonLabel(NewWizardMessages.getString("TypePage.enclosing.button")); //$NON-NLS-1$
		
		fTypeNameDialogField= new StringDialogField();
		fTypeNameDialogField.setDialogFieldListener(adapter);
		fTypeNameDialogField.setLabelText(NewWizardMessages.getString("TypePage.typename.label")); //$NON-NLS-1$
		
		fSuperClassDialogField= new StringButtonDialogField(adapter);
		fSuperClassDialogField.setDialogFieldListener(adapter);
		fSuperClassDialogField.setLabelText(NewWizardMessages.getString("TypePage.superclass.label")); //$NON-NLS-1$
		fSuperClassDialogField.setButtonLabel(NewWizardMessages.getString("TypePage.superclass.button")); //$NON-NLS-1$
		
		String[] addButtons= new String[] {
			/* 0 */ NewWizardMessages.getString("TypePage.interfaces.add"), //$NON-NLS-1$
			/* 1 */ null,
			/* 2 */ NewWizardMessages.getString("TypePage.interfaces.remove") //$NON-NLS-1$
		}; 
		fSuperInterfacesDialogField= new ListDialogField(adapter, addButtons, new InterfacesListLabelProvider());
		fSuperInterfacesDialogField.setDialogFieldListener(adapter);
		String interfaceLabel= fIsClass ? NewWizardMessages.getString("TypePage.interfaces.class.label") : NewWizardMessages.getString("TypePage.interfaces.ifc.label"); //$NON-NLS-1$ //$NON-NLS-2$
		fSuperInterfacesDialogField.setLabelText(interfaceLabel);
		fSuperInterfacesDialogField.setRemoveButtonIndex(2);
	
		String[] buttonNames1= new String[] {
			/* 0 == PUBLIC_INDEX */ NewWizardMessages.getString("TypePage.modifiers.public"), //$NON-NLS-1$
			/* 1 == DEFAULT_INDEX */ NewWizardMessages.getString("TypePage.modifiers.default"), //$NON-NLS-1$
			/* 2 == PRIVATE_INDEX */ NewWizardMessages.getString("TypePage.modifiers.private"), //$NON-NLS-1$
			/* 3 == PROTECTED_INDEX*/ NewWizardMessages.getString("TypePage.modifiers.protected") //$NON-NLS-1$
		};
		fAccMdfButtons= new SelectionButtonDialogFieldGroup(SWT.RADIO, buttonNames1, 4);
		fAccMdfButtons.setDialogFieldListener(adapter);
		fAccMdfButtons.setLabelText(NewWizardMessages.getString("TypePage.modifiers.acc.label"));		 //$NON-NLS-1$
		fAccMdfButtons.setSelection(0, true);
		
		String[] buttonNames2;
		if (fIsClass) {
			buttonNames2= new String[] {
				/* 0 == ABSTRACT_INDEX */ NewWizardMessages.getString("TypePage.modifiers.abstract"), //$NON-NLS-1$
				/* 1 == FINAL_INDEX */ NewWizardMessages.getString("TypePage.modifiers.final"), //$NON-NLS-1$
				/* 2 */ NewWizardMessages.getString("TypePage.modifiers.static") //$NON-NLS-1$
			};
			fStaticMdfIndex= 2; // index of the static checkbox is 2
		} else {
			buttonNames2= new String[] {
				NewWizardMessages.getString("TypePage.modifiers.static") //$NON-NLS-1$
			};
			fStaticMdfIndex= 0; // index of the static checkbox is 0
		}

		fOtherMdfButtons= new SelectionButtonDialogFieldGroup(SWT.CHECK, buttonNames2, 4);
		fOtherMdfButtons.setDialogFieldListener(adapter);
		
		fAccMdfButtons.enableSelectionButton(PRIVATE_INDEX, false);
		fAccMdfButtons.enableSelectionButton(PROTECTED_INDEX, false);
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
	 * Initializes all fields provided by the type page with a given
	 * Java element as selection.
	 * @param elem The initial selection of this page or null if no
	 *             selection was available
	 */
	protected void initTypePage(IJavaElement elem) {
		String initSuperclass= "java.lang.Object"; //$NON-NLS-1$
		ArrayList initSuperinterfaces= new ArrayList(5);

		IPackageFragment pack= null;
				
		if (elem != null) {
			pack= (IPackageFragment) JavaModelUtil.findElementOfKind(elem, IJavaElement.PACKAGE_FRAGMENT);
			try {
				IType type= null;
				if (elem.getElementType() == IJavaElement.TYPE) {
					type= (IType)elem;
					if (type.exists()) {
						String superName= JavaModelUtil.getFullyQualifiedName(type);
						if (type.isInterface()) {
							initSuperinterfaces.add(superName);
						} else {
							initSuperclass= superName;
						}
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e.getStatus());
				// ignore this exception now
			}
		}			

		setPackageFragment(pack, true);
		setEnclosingType(null, true);
		setEnclosingTypeSelection(false, true);
	
		setTypeName("", true); //$NON-NLS-1$
		setSuperClass(initSuperclass, true);
		setSuperInterfaces(initSuperinterfaces, true);
	}		
	
	// -------- UI Creation ---------
	
	/**
	 * Creates a separator line.
	 * @param composite The parent composite
	 * @param nColumns Number of columns to span
	 */
	protected void createSeparator(Composite composite, int nColumns) {
		initializeDialogUnits(composite);
		(new Separator(SWT.SEPARATOR | SWT.HORIZONTAL)).doFillIntoGrid(composite, nColumns, convertHeightInCharsToPixels(1));		
	}

	/**
	 * Creates the controls for the package name field.
	 * @param composite The parent composite
	 * @param nColumns Number of columns to span
	 */	
	protected void createPackageControls(Composite composite, int nColumns) {
		fPackageDialogField.doFillIntoGrid(composite, 4);
	}

	/**
	 * Creates the controls for the enclosing type name field.
	 * @param composite The parent composite
	 * @param nColumns Number of columns to span
	 */		
	protected void createEnclosingTypeControls(Composite composite, int nColumns) {
		fEnclosingTypeSelection.doFillIntoGrid(composite, 1);
		
		Control c= fEnclosingTypeDialogField.getTextControl(composite);
		c.setLayoutData(new MGridData(MGridData.FILL_HORIZONTAL));
		LayoutUtil.setHorizontalSpan(c, 2);
		c= fEnclosingTypeDialogField.getChangeControl(composite);
		c.setLayoutData(new MGridData(MGridData.HORIZONTAL_ALIGN_FILL));
	}	

	/**
	 * Creates the controls for the type name field.
	 * @param composite The parent composite
	 * @param nColumns Number of columns to span
	 */		
	protected void createTypeNameControls(Composite composite, int nColumns) {
		fTypeNameDialogField.doFillIntoGrid(composite, nColumns - 1);
		DialogField.createEmptySpace(composite);
		
	}

	/**
	 * Creates the controls for the modifiers radio/ceckbox buttons.
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
	 * Creates the controls for the superclass name field.
	 * @param composite The parent composite
	 * @param nColumns Number of columns to span
	 */		
	protected void createSuperClassControls(Composite composite, int nColumns) {
		fSuperClassDialogField.doFillIntoGrid(composite, nColumns);
	}

	/**
	 * Creates the controls for the superclass name field.
	 * @param composite The parent composite
	 * @param nColumns Number of columns to span
	 */			
	protected void createSuperInterfacesControls(Composite composite, int nColumns) {
		initializeDialogUnits(composite);
		fSuperInterfacesDialogField.doFillIntoGrid(composite, nColumns);
		MGridData gd= (MGridData)fSuperInterfacesDialogField.getListControl(null).getLayoutData();
		if (fIsClass) {
			gd.heightHint= convertHeightInCharsToPixels(3);
		} else {
			gd.heightHint= convertHeightInCharsToPixels(6);
		}
		gd.grabExcessVerticalSpace= false;
	}

	
	/**
	 * Sets the focus on the container if empty, elso on type name.
	 */		
	protected void setFocus() {
		fTypeNameDialogField.setFocus();
	}
				
	// -------- TypeFieldsAdapter --------

	private class TypeFieldsAdapter implements IStringButtonAdapter, IDialogFieldListener, IListAdapter {
		
		// -------- IStringButtonAdapter
		public void changeControlPressed(DialogField field) {
			typePageChangeControlPressed(field);
		}
		
		// -------- IListAdapter
		public void customButtonPressed(DialogField field, int index) {
			typePageCustomButtonPressed(field, index);
		}
		
		public void selectionChanged(DialogField field) {}
		
		// -------- IDialogFieldListener
		public void dialogFieldChanged(DialogField field) {
			typePageDialogFieldChanged(field);
		}
	}
	
	private void typePageChangeControlPressed(DialogField field) {
		if (field == fPackageDialogField) {
			IPackageFragment pack= choosePackage();	
			if (pack != null) {
				fPackageDialogField.setText(pack.getElementName());
			}
		} else if (field == fEnclosingTypeDialogField) {
			IType type= chooseEnclosingType();
			if (type != null) {
				fEnclosingTypeDialogField.setText(JavaModelUtil.getFullyQualifiedName(type));
			}
		} else if (field == fSuperClassDialogField) {
			IType type= chooseSuperType();
			if (type != null) {
				fSuperClassDialogField.setText(JavaModelUtil.getFullyQualifiedName(type));
			}
		}
	}
	
	private void typePageCustomButtonPressed(DialogField field, int index) {		
		if (field == fSuperInterfacesDialogField) {
			chooseSuperInterfaces();
		}
	}
	
	/*
	 * A field on the type has changed. The fields' status and all dependend
	 * status are updated.
	 */
	private void typePageDialogFieldChanged(DialogField field) {
		String fieldName= null;
		if (field == fPackageDialogField) {
			fPackageStatus= packageChanged();
			updatePackageStatusLabel();
			fTypeNameStatus= typeNameChanged();
			fSuperClassStatus= superClassChanged();			
			fieldName= PACKAGE;
		} else if (field == fEnclosingTypeDialogField) {
			fEnclosingTypeStatus= enclosingTypeChanged();
			fTypeNameStatus= typeNameChanged();
			fSuperClassStatus= superClassChanged();				
			fieldName= ENCLOSING;
		} else if (field == fEnclosingTypeSelection) {
			updateEnableState();
			boolean isEnclosedType= isEnclosingTypeSelected();
			if (!isEnclosedType) {
				if (fAccMdfButtons.isSelected(PRIVATE_INDEX) || fAccMdfButtons.isSelected(PROTECTED_INDEX)) {
					fAccMdfButtons.setSelection(PRIVATE_INDEX, false);
					fAccMdfButtons.setSelection(PROTECTED_INDEX, false); 
					fAccMdfButtons.setSelection(PUBLIC_INDEX, true);
				}
				if (fOtherMdfButtons.isSelected(fStaticMdfIndex)) {
					fOtherMdfButtons.setSelection(fStaticMdfIndex, false);
				}
			}
			fAccMdfButtons.enableSelectionButton(PRIVATE_INDEX, isEnclosedType && fIsClass);
			fAccMdfButtons.enableSelectionButton(PROTECTED_INDEX, isEnclosedType && fIsClass);
			fOtherMdfButtons.enableSelectionButton(fStaticMdfIndex, isEnclosedType);
			fTypeNameStatus= typeNameChanged();
			fSuperClassStatus= superClassChanged();
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
	
	
		
	// -------- update message ----------------		

	/**
	 * Called whenever a content of a field has changed.
	 * Implementors of TypePage can hook in.
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
		}
	}
	
	// ---- set / get ----------------
	
	/**
	 * Gets the text of package field.
	 */
	public String getPackageText() {
		return fPackageDialogField.getText();
	}

	/**
	 * Gets the text of enclosing type field.
	 */	
	public String getEnclosingTypeText() {
		return fEnclosingTypeDialogField.getText();
	}	
	
	
	/**
	 * Returns the package fragment corresponding to the current input.
	 * @return Returns <code>null</code> if the input could not be resolved.
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
		String str= (pack == null) ? "" : pack.getElementName(); //$NON-NLS-1$
		fPackageDialogField.setText(str);
		updateEnableState();
	}	

	/**
	 * Returns the encloding type corresponding to the current input.
	 * @return Returns <code>null</code> if enclosing type is not selected or the input could not
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
		String str= (type == null) ? "" : JavaModelUtil.getFullyQualifiedName(type); //$NON-NLS-1$
		fEnclosingTypeDialogField.setText(str);
		updateEnableState();
	}
	
	/**
	 * Returns <code>true</code> if the enclosing type selection check box is enabled.
	 */
	public boolean isEnclosingTypeSelected() {
		return fEnclosingTypeSelection.isSelected();
	}

	/**
	 * Sets the enclosing type selection checkbox.
	 * @param canBeModified Selects if the enclosing type selection can be changed by the user
	 */	
	public void setEnclosingTypeSelection(boolean isSelected, boolean canBeModified) {
		fEnclosingTypeSelection.setSelection(isSelected);
		fEnclosingTypeSelection.setEnabled(canBeModified);
		updateEnableState();
	}
	
	/**
	 * Gets the type name.
	 */
	public String getTypeName() {
		return fTypeNameDialogField.getText();
	}

	/**
	 * Sets the type name.
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
		if (fAccMdfButtons.isSelected(PUBLIC_INDEX)) {
			mdf+= IConstants.AccPublic;
		} else if (fAccMdfButtons.isSelected(PRIVATE_INDEX)) {
			mdf+= IConstants.AccPrivate;
		} else if (fAccMdfButtons.isSelected(PROTECTED_INDEX)) {	
			mdf+= IConstants.AccProtected;
		}
		if (fOtherMdfButtons.isSelected(ABSTRACT_INDEX) && (fStaticMdfIndex != 0)) {	
			mdf+= IConstants.AccAbstract;
		}
		if (fOtherMdfButtons.isSelected(FINAL_INDEX)) {	
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
			fAccMdfButtons.setSelection(PUBLIC_INDEX, true);
		} else if (Flags.isPrivate(modifiers)) {
			fAccMdfButtons.setSelection(PRIVATE_INDEX, true);
		} else if (Flags.isProtected(modifiers)) {
			fAccMdfButtons.setSelection(PROTECTED_INDEX, true);
		} else {
			fAccMdfButtons.setSelection(DEFAULT_INDEX, true);
		}
		if (Flags.isAbstract(modifiers)) {
			fOtherMdfButtons.setSelection(ABSTRACT_INDEX, true);
		}
		if (Flags.isFinal(modifiers)) {
			fOtherMdfButtons.setSelection(FINAL_INDEX, true);
		}		
		if (Flags.isStatic(modifiers)) {
			fOtherMdfButtons.setSelection(fStaticMdfIndex, true);
		}
		
		fAccMdfButtons.setEnabled(canBeModified);
		fOtherMdfButtons.setEnabled(canBeModified);
	}
	
	/**
	 * Gets the content of the super class text field.
	 */
	public String getSuperClass() {
		return fSuperClassDialogField.getText();
	}

	/**
	 * Sets the super class name.
	 * @param canBeModified Selects if the super class can be changed by the user
	 */		
	public void setSuperClass(String name, boolean canBeModified) {
		fSuperClassDialogField.setText(name);
		fSuperClassDialogField.setEnabled(canBeModified);
	}	
	
	/**
	 * Gets the currently chosen super interfaces.
	 * @return returns a list of String
	 */
	public List getSuperInterfaces() {
		return fSuperInterfacesDialogField.getElements();
	}

	/**
	 * Sets the super interfaces.
	 * @param interfacesNames a list of String
	 */	
	public void setSuperInterfaces(List interfacesNames, boolean canBeModified) {
		fSuperInterfacesDialogField.setElements(interfacesNames);
		fSuperInterfacesDialogField.setEnabled(canBeModified);
	}
			
	// ----------- validation ----------
		
	/**
	 * Called when the package field has changed.
	 * The method validates the package name and returns the status of the validation
	 * This also updates the package fragment model.
	 * Can be extended to add more validation
	 */
	protected IStatus packageChanged() {
		StatusInfo status= new StatusInfo();
		fPackageDialogField.enableButton(getPackageFragmentRoot() != null);
		
		String packName= getPackageText();
		if (packName.length() > 0) {
			IStatus val= JavaConventions.validatePackageName(packName);
			if (val.getSeverity() == IStatus.ERROR) {
				status.setError(NewWizardMessages.getFormattedString("TypePage.error.InvalidPackageName", val.getMessage())); //$NON-NLS-1$
				return status;
			} else if (val.getSeverity() == IStatus.WARNING) {
				status.setWarning(NewWizardMessages.getFormattedString("TypePage.warning.DiscouragedPackageName", val.getMessage())); //$NON-NLS-1$
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
						status.setError(NewWizardMessages.getString("TypePage.error.ClashOutputLocation")); //$NON-NLS-1$
						return status;
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e.getStatus());
				// let pass			
			}
			
			fCurrPackage= pack;
		} else {
			status.setError(""); //$NON-NLS-1$
		}
		return status;
	}

	/*
	 * Updates the 'default' label next to the package field.
	 */	
	private void updatePackageStatusLabel() {
		String packName= fPackageDialogField.getText();
		
		if (packName.length() == 0) {
			fPackageDialogField.setStatus(NewWizardMessages.getString("TypePage.default")); //$NON-NLS-1$
		} else {
			fPackageDialogField.setStatus(""); //$NON-NLS-1$
		}
	}
	
	/*
	 * Updates the enable state of buttons related to the enclosing type selection checkbox.
	 */
	private void updateEnableState() {
		boolean enclosing= isEnclosingTypeSelected();
		fPackageDialogField.setEnabled(fCanModifyPackage && !enclosing);
		fEnclosingTypeDialogField.setEnabled(fCanModifyEnclosingType && enclosing);
	}	

	/**
	 * Called when the enclosing type name has changed.
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
			status.setError(""); //$NON-NLS-1$
			return status;
		}
		
		String enclName= getEnclosingTypeText();
		if (enclName.length() == 0) {
			status.setError(NewWizardMessages.getString("TypePage.error.EnclosingTypeEnterName")); //$NON-NLS-1$
			return status;
		}
		try {
			IType type= JavaModelUtil.findType(root.getJavaProject(), enclName);
			if (type == null) {
				status.setError(NewWizardMessages.getString("TypePage.error.EnclosingTypeNotExists")); //$NON-NLS-1$
				return status;
			}

			if (type.getCompilationUnit() == null) {
				status.setError(NewWizardMessages.getString("TypePage.error.EnclosingNotInCU")); //$NON-NLS-1$
				return status;
			}
			fCurrEnclosingType= type;
			return status;
		} catch (JavaModelException e) {
			status.setError(NewWizardMessages.getString("TypePage.error.EnclosingTypeNotExists")); //$NON-NLS-1$
			JavaPlugin.log(e.getStatus());
			return status;
		}
	}
	
	/**
	 * Called when the type name has changed.
	 * The method validates the type name and returns the status of the validation.
	 * Can be extended to add more validation
	 */
	protected IStatus typeNameChanged() {
		StatusInfo status= new StatusInfo();
		String typeName= getTypeName();
		// must not be empty
		if (typeName.length() == 0) {
			status.setError(NewWizardMessages.getString("TypePage.error.EnterTypeName")); //$NON-NLS-1$
			return status;
		}
		if (typeName.indexOf('.') != -1) {
			status.setError(NewWizardMessages.getString("TypePage.error.QualifiedName")); //$NON-NLS-1$
			return status;
		}
		IStatus val= JavaConventions.validateJavaTypeName(typeName);
		if (val.getSeverity() == IStatus.ERROR) {
			status.setError(NewWizardMessages.getFormattedString("TypePage.error.InvalidTypeName", val.getMessage())); //$NON-NLS-1$
			return status;
		} else if (val.getSeverity() == IStatus.WARNING) {
			status.setWarning(NewWizardMessages.getFormattedString("TypePage.warning.TypeNameDiscouraged", val.getMessage())); //$NON-NLS-1$
			// continue checking
		}		

		// must not exist
		if (!isEnclosingTypeSelected()) {
			IPackageFragment pack= getPackageFragment();
			if (pack != null) {
				ICompilationUnit cu= pack.getCompilationUnit(typeName + ".java"); //$NON-NLS-1$
				if (cu.exists()) {
					status.setError(NewWizardMessages.getString("TypePage.error.TypeNameExists")); //$NON-NLS-1$
					return status;
				}
			}
		} else {
			IType type= getEnclosingType();
			if (type != null) {
				IType member= type.getType(typeName);
				if (member.exists()) {
					status.setError(NewWizardMessages.getString("TypePage.error.TypeNameExists")); //$NON-NLS-1$
					return status;
				}
			}
		}
		return status;
	}
	
	/**
	 * Called when the superclass name has changed.
	 * The method validates the superclass name and returns the status of the validation.
	 * Can be extended to add more validation
	 */
	protected IStatus superClassChanged() {
		StatusInfo status= new StatusInfo();
		IPackageFragmentRoot root= getPackageFragmentRoot();
		fSuperClassDialogField.enableButton(root != null);
		
		fSuperClass= null;
		
		String sclassName= getSuperClass();
		if (sclassName.length() == 0) {
			// accept the empty field (stands for java.lang.Object)
			return status;
		}
		IStatus val= JavaConventions.validateJavaTypeName(sclassName);
		if (!val.isOK()) {
			status.setError(NewWizardMessages.getString("TypePage.error.InvalidSuperClassName")); //$NON-NLS-1$
			return status;
		}
		
		if (root != null) {
			try {		
				IType type= resolveSuperTypeName(root.getJavaProject(), sclassName);
				if (type == null) {
					status.setWarning(NewWizardMessages.getString("TypePage.warning.SuperClassNotExists")); //$NON-NLS-1$
					return status;
				} else {
					if (type.isInterface()) {
						status.setWarning(NewWizardMessages.getFormattedString("TypePage.warning.SuperClassIsNotClass", sclassName)); //$NON-NLS-1$
						return status;
					}
					int flags= type.getFlags();
					if (Flags.isFinal(flags)) {
						status.setWarning(NewWizardMessages.getFormattedString("TypePage.warning.SuperClassIsFinal", sclassName)); //$NON-NLS-1$
						return status;
					} else if (!JavaModelUtil.isVisible(type, getPackageFragment())) {
						status.setWarning(NewWizardMessages.getFormattedString("TypePage.warning.SuperClassIsNotVisible", sclassName)); //$NON-NLS-1$
						return status;
					}
				}
				fSuperClass= type;
			} catch (JavaModelException e) {
				status.setError(NewWizardMessages.getString("TypePage.error.InvalidSuperClassName")); //$NON-NLS-1$
				JavaPlugin.log(e.getStatus());
			}							
		} else {
			status.setError(""); //$NON-NLS-1$
		}
		return status;
		
	}
	
	private IType resolveSuperTypeName(IJavaProject jproject, String sclassName) throws JavaModelException {
		IType type= null;
		if (isEnclosingTypeSelected()) {
			// search in the context of the enclosing type
			IType enclosingType= getEnclosingType();
			if (enclosingType != null) {
				String[][] res= enclosingType.resolveType(sclassName);
				if (res != null && res.length > 0) {
					type= JavaModelUtil.findType(jproject, res[0][0], res[0][1]);
				}
			}
		} else {
			IPackageFragment currPack= getPackageFragment();
			if (type == null && currPack != null) {
				String packName= currPack.getElementName();
				// search in own package
				if (!currPack.isDefaultPackage()) {
					type= JavaModelUtil.findType(jproject, packName, sclassName);
				}
				// search in java.lang
				if (type == null && !"java.lang".equals(packName)) { //$NON-NLS-1$
					type= JavaModelUtil.findType(jproject, "java.lang", sclassName); //$NON-NLS-1$
				}
			}
			// search fully qualified
			if (type == null) {
				type= JavaModelUtil.findType(jproject, sclassName);
			}
		}
		return type;
	}		
	
	/**
	 * Called when the list of super interface has changed.
	 * The method validates the superinterfaces and returns the status of the validation.
	 * Can be extended to add more validation.
	 */
	protected IStatus superInterfacesChanged() {
		StatusInfo status= new StatusInfo();
		
		IPackageFragmentRoot root= getPackageFragmentRoot();
		fSuperInterfacesDialogField.enableButton(0, root != null);
						
		if (root != null) {
			List elements= fSuperInterfacesDialogField.getElements();
			int nElements= elements.size();
			for (int i= 0; i < nElements; i++) {
				String intfname= (String)elements.get(i);
				try {
					IType type= JavaModelUtil.findType(root.getJavaProject(), intfname);
					if (type == null) {
						status.setWarning(NewWizardMessages.getFormattedString("TypePage.warning.InterfaceNotExists", intfname)); //$NON-NLS-1$
						return status;
					} else {
						if (type.isClass()) {
							status.setWarning(NewWizardMessages.getFormattedString("TypePage.warning.InterfaceIsNotInterface", intfname)); //$NON-NLS-1$
							return status;
						}
						if (!JavaModelUtil.isVisible(type, getPackageFragment())) {
							status.setWarning(NewWizardMessages.getFormattedString("TypePage.warning.InterfaceIsNotVisible", intfname)); //$NON-NLS-1$
							return status;
						}
					}
				} catch (JavaModelException e) {
					JavaPlugin.log(e.getStatus());
					// let pass, checking is an extra
				}					
			}				
		}
		return status;
	}

	/**
	 * Called when the modifiers have changed.
	 * The method validates the modifiers and returns the status of the validation.
	 * Can be extended to add more validation.
	 */
	protected IStatus modifiersChanged() {
		StatusInfo status= new StatusInfo();
		int modifiers= getModifiers();
		if (Flags.isFinal(modifiers) && Flags.isAbstract(modifiers)) {
			status.setError(NewWizardMessages.getString("TypePage.error.ModifiersFinalAndAbstract")); //$NON-NLS-1$
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
		
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT));
		dialog.setIgnoreCase(false);
		dialog.setTitle(NewWizardMessages.getString("TypePage.ChoosePackageDialog.title")); //$NON-NLS-1$
		dialog.setMessage(NewWizardMessages.getString("TypePage.ChoosePackageDialog.description")); //$NON-NLS-1$
		dialog.setEmptyListMessage(NewWizardMessages.getString("TypePage.ChoosePackageDialog.empty")); //$NON-NLS-1$
		dialog.setElements(packages);
		if (fCurrPackage != null) {
			dialog.setInitialSelections(new Object[] { fCurrPackage });
		}

		if (dialog.open() == dialog.OK) {
			return (IPackageFragment) dialog.getFirstResult();
		}
		return null;
	}
	
	private IType chooseEnclosingType() {
		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root == null) {
			return null;
		}

		IJavaElement[] elements= new IJavaElement[] { root.getJavaProject() };
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(elements);
			
		TypeSelectionDialog dialog= new TypeSelectionDialog(getShell(), getWizard().getContainer(), scope, IJavaElementSearchConstants.CONSIDER_TYPES);
		dialog.setTitle(NewWizardMessages.getString("TypePage.ChooseEnclosingTypeDialog.title")); //$NON-NLS-1$
		dialog.setMessage(NewWizardMessages.getString("TypePage.ChooseEnclosingTypeDialog.description")); //$NON-NLS-1$
		if (fCurrEnclosingType != null) {
			dialog.setInitialSelections(new Object[] { fCurrEnclosingType });
			dialog.setFilter(fCurrEnclosingType.getElementName().substring(0, 1));
		}
		
		if (dialog.open() == dialog.OK) {	
			return (IType) dialog.getFirstResult();
		}
		return null;
	}	
	
	private IType chooseSuperType() {
		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root == null) {
			return null;
		}	

		IJavaElement[] elements= new IJavaElement[] { root.getJavaProject() };
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(elements);

		TypeSelectionDialog dialog= new TypeSelectionDialog(getShell(), getWizard().getContainer(), scope, IJavaElementSearchConstants.CONSIDER_CLASSES);
		dialog.setTitle(NewWizardMessages.getString("TypePage.SuperClassDialog.title")); //$NON-NLS-1$
		dialog.setMessage(NewWizardMessages.getString("TypePage.SuperClassDialog.message")); //$NON-NLS-1$
		if (fSuperClass != null) {
			dialog.setFilter(fSuperClass.getElementName());
		}

		if (dialog.open() == dialog.OK) {
			return (IType) dialog.getFirstResult();
		}
		return null;
	}
	
	private void chooseSuperInterfaces() {
		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root == null) {
			return;
		}	

		IJavaProject project= root.getJavaProject();
		SuperInterfaceSelectionDialog dialog= new SuperInterfaceSelectionDialog(getShell(), getWizard().getContainer(), fSuperInterfacesDialogField, project);
		dialog.setTitle(NewWizardMessages.getString("TypePage.InterfacesDialog.title")); //$NON-NLS-1$
		dialog.setMessage(NewWizardMessages.getString("TypePage.InterfacesDialog.message")); //$NON-NLS-1$
		dialog.open();
		return;
	}	
	
	
		
	// ---- creation ----------------

	/**
	 * Creates a type using the current field values.
	 */
	public void createType(IProgressMonitor monitor) throws CoreException, InterruptedException {		
		monitor.beginTask(NewWizardMessages.getString("TypePage.operationdesc"), 10); //$NON-NLS-1$
		
		IPackageFragmentRoot root= getPackageFragmentRoot();
		IPackageFragment pack= getPackageFragment();
		if (pack == null) {
			pack= root.getPackageFragment(""); //$NON-NLS-1$
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
		
		String lineDelimiter= null;	
		if (!isInnerClass) {
			ICompilationUnit parentCU= pack.getCompilationUnit(clName + ".java"); //$NON-NLS-1$
			
			String[] prefOrder= ImportOrganizePreferencePage.getImportOrderPreference();
			int threshold= ImportOrganizePreferencePage.getImportNumberThreshold();			
			imports= new ImportsStructure(parentCU, prefOrder, threshold);
			
			lineDelimiter= StubUtility.getLineDelimiterUsed(parentCU);
			
			String content= createTypeBody(imports, lineDelimiter);
			createdType= parentCU.createType(content, null, false, new SubProgressMonitor(monitor, 5));
		} else {
			IType enclosingType= getEnclosingType();
			
			// if we are working on a enclosed type that is open in an editor,
			// then replace the enclosing type with its working copy
			IType workingCopy= (IType) EditorUtility.getWorkingCopy(enclosingType);
			if (workingCopy != null) {
				enclosingType= workingCopy;
			}

			ICompilationUnit parentCU= enclosingType.getCompilationUnit();
			imports= new ImportsStructure(parentCU);
			
			lineDelimiter= StubUtility.getLineDelimiterUsed(enclosingType);
			String content= createTypeBody(imports, lineDelimiter);
			createdType= enclosingType.createType(content, null, false, new SubProgressMonitor(monitor, 1));
		
			indent= StubUtility.getIndentUsed(enclosingType) + 1;
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
		
		String formattedContent= StubUtility.codeFormat(createdType.getSource(), indent, lineDelimiter);
		
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
		if (fIsClass && typename.length() > 0 && !"java.lang.Object".equals(typename)) { //$NON-NLS-1$
			buf.append(" extends "); //$NON-NLS-1$
			buf.append(Signature.getSimpleName(typename));
			if (fSuperClass != null) {
				imports.addImport(JavaModelUtil.getFullyQualifiedName(fSuperClass));
			} else {
				imports.addImport(typename);
			}
		}
	}
	
	private void writeSuperInterfaces(StringBuffer buf, IImportsStructure imports) {
		List interfaces= getSuperInterfaces();
		int last= interfaces.size() - 1;
		if (last >= 0) {
			if (fIsClass) {
				buf.append(" implements "); //$NON-NLS-1$
			} else {
				buf.append(" extends "); //$NON-NLS-1$
			}
			for (int i= 0; i <= last; i++) {
				String typename= (String) interfaces.get(i);
				imports.addImport(typename);
				buf.append(Signature.getSimpleName(typename));
				if (i < last) {
					buf.append(", "); //$NON-NLS-1$
				}
			}
		}
	}

	/*
	 * Called from createType to construct the source for this type
	 */		
	private String createTypeBody(IImportsStructure imports, String lineDelimiter) {	
		StringBuffer buf= new StringBuffer();
		
		int modifiers= getModifiers();
		buf.append(Flags.toString(modifiers));
		if (modifiers != 0) {
			buf.append(' ');
		}
		buf.append(fIsClass ? "class " : "interface "); //$NON-NLS-2$ //$NON-NLS-1$
		buf.append(getTypeName());
		writeSuperClass(buf, imports);
		writeSuperInterfaces(buf, imports);	
		buf.append(" {"); //$NON-NLS-1$
		buf.append(lineDelimiter);
		buf.append(lineDelimiter);
		buf.append('}');
		buf.append(lineDelimiter);
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
			StubUtility.evalUnimplementedMethods(type, hierarchy, false, newMethods, imports);
		}
		return (String[]) newMethods.toArray(new String[newMethods.size()]);		
		
	}
	
	// ---- creation ----------------

	/**
	 * @see NewElementWizardPage#getRunnable
	 */		
	public IRunnableWithProgress getRunnable() {				
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					if (monitor == null) {
						monitor= new NullProgressMonitor();
					}
					createType(monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} 				
			}
		};
	}	
		
			
}