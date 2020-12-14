/*******************************************************************************
 * Copyright (c) 2017, 2020 GK Software AG, and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.util.BidiUtils;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.JavaPackageCompletionProcessor;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.ModuleAddExport;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.ModuleAddExpose;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.ModuleAddOpens;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;


/**
 * UI to define one additional exports (add-exports) or opens (add-opens).
 */
public class ModuleAddExportsBlock {

	private static final String[] EXPORTS_OPENS_LABELS= new String[] {"exports", "opens"}; //$NON-NLS-1$ //$NON-NLS-2$
	private static final int IDX_EXPORTS= 0;
	private static final int IDX_OPENS= 1;

	private final IStatusChangeListener fContext;

	private StringDialogField fSourceModule;
	private StringDialogField fPackage;
	private StringDialogField fTargetModules;
	private SelectionButtonDialogFieldGroup fExposeKindButtons;

	private IStatus fSourceModuleStatus;
	private IStatus fPackageStatus;

	private Control fSWTWidget;

	private final ModuleAddExpose fInitialValue;

	private IJavaElement[] fSourceJavaElements;
	private Collection<String> fPossibleTargetModules;
	private Set<String> fAlreadyExportedPackages = new HashSet<>();

	/**
	 * @param context listeners for status updates
	 * @param sourceJavaElements java element representing the source modules from where packages should be exported
	 * @param possibleTargetModules modules to be offered in content assist, or {@code null}
	 * @param initialValue The value to edit
	 * @param alreadyExportedPackages The packages for which add-exports already done
	 */
	public ModuleAddExportsBlock(IStatusChangeListener context, IJavaElement[] sourceJavaElements, Collection<String> possibleTargetModules, ModuleAddExpose initialValue, Set<String> alreadyExportedPackages) {
		fContext= context;
		fInitialValue= initialValue;
		fSourceJavaElements= sourceJavaElements;
		fPossibleTargetModules= possibleTargetModules;
		fAlreadyExportedPackages= alreadyExportedPackages;

		fSourceModuleStatus= new StatusInfo();
		fPackageStatus= new StatusInfo();

		IDialogFieldListener adapter= this::addExportsDialogFieldChanged;

		// create the dialog fields (no widgets yet)
		fSourceModule= new StringDialogField();
		fSourceModule.setDialogFieldListener(adapter);
		fSourceModule.setLabelText(NewWizardMessages.AddExportsBlock_sourceModule_label);

		fPackage= new StringDialogField();
		fPackage.setDialogFieldListener(adapter);
		fPackage.setLabelText(NewWizardMessages.AddExportsBlock_package_label);

		fTargetModules= new StringDialogField();
		fTargetModules.setDialogFieldListener(adapter);
		fTargetModules.setLabelText(NewWizardMessages.AddExportsBlock_targetModules_label);

		fExposeKindButtons= new SelectionButtonDialogFieldGroup(SWT.RADIO, EXPORTS_OPENS_LABELS, 2);
		fExposeKindButtons.setSelection(IDX_EXPORTS, initialValue instanceof ModuleAddExport);
		fExposeKindButtons.setSelection(IDX_OPENS, initialValue instanceof ModuleAddOpens);
		fExposeKindButtons.setDialogFieldListener(adapter);

		setDefaults();
	}

	private void setDefaults() {
		if (fInitialValue != null) {
			fSourceModule.setText(fInitialValue.fSourceModule);
			if (!fInitialValue.fSourceModule.isEmpty() && (fSourceJavaElements == null || fSourceJavaElements.length <= 1)) {
				fSourceModule.setEnabled(false);
			}
			fPackage.setText(fInitialValue.fPackage);
			fTargetModules.setText(fInitialValue.fTargetModules);
			fTargetModules.setEnabled(fPossibleTargetModules != null);
		}
	}

	private Set<String> moduleNames() {
		Set<String> moduleNames= new HashSet<>();
		if (fSourceJavaElements != null) {
			for (IJavaElement element : fSourceJavaElements) {
				if (element instanceof IPackageFragmentRoot) {
					IModuleDescription module= ((IPackageFragmentRoot) element).getModuleDescription();
					if (module != null) {
						moduleNames.add(module.getElementName());
					}
				}
			}
		}
		return moduleNames;
	}

	private String getSourceModuleText() {
		return fSourceModule.getText().trim();
	}

	private String getPackageText() {
		return fPackage.getText().trim();
	}

	private String getTargetModulesText() {
		return fTargetModules.getText().trim();
	}

	/* Answer all roots whose (@Nullable) module matches the given predicat. */
	private IPackageFragmentRoot[] findRoots(Predicate<IModuleDescription> match) {
		List<IPackageFragmentRoot> result= new ArrayList<>();
		if (fSourceJavaElements != null) {
			for (IJavaElement root : fSourceJavaElements) {
				if (root instanceof IJavaProject) {
					try {
						IJavaProject project= (IJavaProject) root;
						IModuleDescription module= project.getModuleDescription();
						if (match.test(module)) {
							return project.getPackageFragmentRoots();
						}
					} catch (JavaModelException e) {
						JavaPlugin.log(e);
					}
				} else if (root instanceof IPackageFragmentRoot) {
					IPackageFragmentRoot packageRoot= (IPackageFragmentRoot) root;
					IModuleDescription module= packageRoot.getModuleDescription();
					if (match.test(module)) {
						result.add(packageRoot);
					}
				}
			}
		}
		if (!result.isEmpty()) {
			return result.toArray(new IPackageFragmentRoot[result.size()]);
		}
		return null;
	}

	/**
	 * Gets the add-export value entered by the user
	 * @return the add-export value, or an empty string if any of the fields was left empty.
	 */
	public String getValue() {
		String sourceModule= getSourceModuleText();
		String pack= getPackageText();
		String targetModules= getTargetModulesText();
		if (sourceModule.isEmpty() || pack.isEmpty() || targetModules.isEmpty())
			return ""; //$NON-NLS-1$
		return sourceModule+'/'+pack+'='+targetModules;
	}

	public ModuleAddExpose getExport(CPListElementAttribute parentAttribute) {
		String sourceModule= getSourceModuleText();
		String pack= getPackageText();
		String targetModules= getTargetModulesText();
		if (sourceModule.isEmpty() || pack.isEmpty())
			return null;
		if (fExposeKindButtons.isSelected(IDX_EXPORTS))
			return new ModuleAddExport(sourceModule, pack, targetModules, parentAttribute);
		else
			return new ModuleAddOpens(sourceModule, pack, targetModules, parentAttribute);
	}

	/**
	 * Creates the control
	 * @param parent the parent
	 * @return the created control
	 */
	public Control createControl(Composite parent) {
		PixelConverter converter= new PixelConverter(parent);

		fSWTWidget= parent;

		Composite composite= new Composite(parent, SWT.NONE);

		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		composite.setLayout(layout);


		int widthHint= converter.convertWidthInCharsToPixels(60);

		GridData gd= new GridData(GridData.FILL, GridData.BEGINNING, false, false, 2, 1);
		gd.widthHint= converter.convertWidthInCharsToPixels(50);

		Label message= new Label(composite, SWT.LEFT + SWT.WRAP);
		message.setLayoutData(gd);
		message.setText(NewWizardMessages.AddExportsBlock_message);

		DialogField.createEmptySpace(composite, 2);

		fSourceModule.doFillIntoGrid(composite, 2);
		Text sourceModuleField= fSourceModule.getTextControl(null);
		LayoutUtil.setWidthHint(sourceModuleField, widthHint);
		LayoutUtil.setHorizontalGrabbing(sourceModuleField);
		BidiUtils.applyBidiProcessing(sourceModuleField, StructuredTextTypeHandlerFactory.JAVA);
		if (fSourceJavaElements != null) {
			ModuleDialog.configureModuleContentAssist(fSourceModule.getTextControl(composite), moduleNames());
		}

		DialogField.createEmptySpace(composite, 2);

		fPackage.doFillIntoGrid(composite, 2);
		Text packageField= fPackage.getTextControl(null);
		LayoutUtil.setWidthHint(packageField, widthHint);
		LayoutUtil.setHorizontalGrabbing(packageField);
		BidiUtils.applyBidiProcessing(packageField, StructuredTextTypeHandlerFactory.JAVA);
		configurePackageContentAssist(fPackage.getTextControl(composite));

		DialogField.createEmptySpace(composite, 2);


		fTargetModules.doFillIntoGrid(composite, 2);
		Text targetModulesField= fTargetModules.getTextControl(null);
		LayoutUtil.setWidthHint(targetModulesField, widthHint);
		LayoutUtil.setHorizontalGrabbing(targetModulesField);
		BidiUtils.applyBidiProcessing(targetModulesField, StructuredTextTypeHandlerFactory.JAVA);

		if (fPossibleTargetModules != null) {
			ModuleDialog.configureModuleContentAssist(fTargetModules.getTextControl(parent), fPossibleTargetModules);
		}

		DialogField.createEmptySpace(composite, 2);

		fExposeKindButtons.doFillIntoGrid(composite, 2);

		Dialog.applyDialogFont(composite);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJavaHelpContextIds.EXTERNAL_ANNOTATIONS_ATTACHMENT_DIALOG); // FIXME
		return composite;
	}

	private void configurePackageContentAssist(Text textControl) {
		if (fSourceJavaElements == null || fSourceJavaElements.length == 0) {
			return;
		}
		JavaPackageCompletionProcessor packageCompletionProcessor= new JavaPackageCompletionProcessor();
		packageCompletionProcessor.setFilter(fragment -> {
			try {
				if (!fragment.containsJavaResources()) {
					return false; // don't propose "empty" packages
				}
				// don't propose the packages already added
				if (fAlreadyExportedPackages != null) {
					return !fAlreadyExportedPackages.contains(fragment.getElementName());
				}
				String sourceModule= getSourceModuleText();
				if (!sourceModule.isEmpty()) {
					IModuleDescription module= ((IPackageFragmentRoot) fragment.getParent()).getModuleDescription();
					if (module != null) {
						return sourceModule.equals(module.getElementName());
					}
				}
				return true;
			} catch (JavaModelException e) {
				return false;
			}
		});
		switch (fSourceJavaElements[0].getElementType()) {
			case IJavaElement.JAVA_PROJECT:
				try {
					assert fSourceJavaElements.length == 1;
					IPackageFragmentRoot[] packageFragmentRoots= ((IJavaProject) fSourceJavaElements[0]).getPackageFragmentRoots();
					int count= 0;
					for (IPackageFragmentRoot packageFragmentRoot : packageFragmentRoots) {
						if (packageFragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE) {
							packageFragmentRoots[count++]= packageFragmentRoot;
						}
					}
					if (count < packageFragmentRoots.length)
						packageFragmentRoots= Arrays.copyOf(packageFragmentRoots, count);
					packageCompletionProcessor.setPackageFragmentRoot(packageFragmentRoots);
					ControlContentAssistHelper.createTextContentAssistant(textControl, packageCompletionProcessor);
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
				break;
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				IPackageFragmentRoot[] roots= new IPackageFragmentRoot[fSourceJavaElements.length];
				for (int i= 0; i < fSourceJavaElements.length; i++) {
					roots[i]= (IPackageFragmentRoot) fSourceJavaElements[i];
				}
				packageCompletionProcessor.setPackageFragmentRoot(roots);
				ControlContentAssistHelper.createTextContentAssistant(textControl, packageCompletionProcessor);
				break;
			default:
				// no other types applicable.
		}
	}

	// ---------- IDialogFieldListener --------

	private void addExportsDialogFieldChanged(DialogField field) {
		if (fSWTWidget != null) {
			if (field == fSourceModule && fSourceModule.isEnabled()) {
				updateModuleStatus();
			} else if (field == fPackage && fPackage.isEnabled()) {
				updatePackageStatus();
			}
			doStatusLineUpdate(field);
		}
	}

	private void updateModuleStatus() {
		fSourceModuleStatus= computeSourceModuleStatus(getSourceModuleText());
	}

	private void updatePackageStatus() {
		fPackageStatus= computePackageStatus(getPackageText());
	}

	private IStatus computeSourceModuleStatus(String value) {
		StatusInfo status= new StatusInfo();
		if (value.isEmpty()) {
			status.setError(NewWizardMessages.ModuleAddExportsBlock_sourceModuleEmpty_error);
			return status;
		}
		if (moduleNames().contains(value)) {
			if (!getPackageText().isEmpty()) {
				updatePackageStatus();
			}
			return status;
		}
		status.setError(Messages.format(NewWizardMessages.ModuleAddExportsBlock_wrongSourceModule_error, value));
		return status;
	}

	private IStatus computePackageStatus(String value) {
		StatusInfo status= new StatusInfo();
		if (value.isEmpty()) {
			status.setError(NewWizardMessages.ModuleAddExportsBlock_packageEmpty_error);
			return status;
		}
		boolean needToSetSource= false;
		String moduleName= getSourceModuleText();
		IPackageFragmentRoot[] roots;
		if (!moduleName.isEmpty()) {
			roots= findRoots(mod -> (mod != null && mod.getElementName().equals(moduleName)));
		} else {
			roots= findRoots(mod -> true);
			needToSetSource= true;
		}
		if (roots != null) {
			for (IPackageFragmentRoot root : roots) {
				IPackageFragment packageFragment= root.getPackageFragment(value);
				try {
					if (packageFragment.exists() && packageFragment.containsJavaResources()) {
						if (needToSetSource) {
							IModuleDescription module= root.getModuleDescription();
							if (module != null) {
								fSourceModule.setText(module.getElementName());
							}
						}
						return status;
					}
				} catch (JavaModelException e) {
					return e.getStatus();
				}
			}
		}
		status.setError(Messages.format(NewWizardMessages.ModuleAddExportsBlock_wrongPackage_error, new Object[] {value, getSourceModuleText()}));
		return status;
	}

	private void doStatusLineUpdate(DialogField currentField) {
		IStatus status= null;
		if (!fSourceModuleStatus.isOK()) {
			status= fSourceModuleStatus; 	// priority
		} else if (!fPackageStatus.isOK()) {
			status= fPackageStatus; 		// 2nd
			if (currentField != fPackage) {
				fPackage.setFocus(); // package may have been invalidated by change in source, draw attention to it
			}
		}
		if (status == null) {
			if (getPackageText().isEmpty()) { // not yet validated but empty?
				status= ModuleDialog.newSilentError();
			} else {
				status= Status.OK_STATUS;
			}
		}
		fContext.statusChanged(status);
	}
}
