/*******************************************************************************
 * Copyright (c) 2017, 2020 GK Software SE, and others.
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

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.util.BidiUtils;
import org.eclipse.jface.util.Util;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.JavaPrecomputedNamesAssistProcessor;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.LimitModules;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.ModuleAddExport;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.ModuleAddExpose;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.ModuleAddReads;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.ModulePatch;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

public class ModuleDialog extends StatusDialog {

	private static final String NO_NAME= ""; //$NON-NLS-1$

	static class ListContentProvider implements IStructuredContentProvider {
		List<?> fContents;

		@Override
		public Object[] getElements(Object input) {
			if (fContents != null && fContents == input)
				return fContents.toArray();
			return new Object[0];
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput instanceof List<?>)
				fContents= (List<?>)newInput;
			else
				fContents= null;
		}
	}

	static class ModulesLabelProvider extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return JavaPlugin.getImageDescriptorRegistry().get(JavaPluginImages.DESC_OBJS_MODULE);
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			return element.toString();
		}

	}

	public static class AddDetailsLabelProvider extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof ModuleAddExport) {
				ModuleAddExport export= (ModuleAddExport) element;
				switch (columnIndex) {
					case 0: return export.fSourceModule;
					case 1: return export.fPackage;
					case 2: return export.fTargetModules;
					default:
						throw new IllegalArgumentException("Illegal column index "+columnIndex); //$NON-NLS-1$
				}
			} else if (element instanceof ModuleAddReads) {
				ModuleAddReads reads= (ModuleAddReads) element;
				switch (columnIndex) {
					case 0: return reads.fSourceModule;
					case 1: return reads.fTargetModule;
					default:
						throw new IllegalArgumentException("Illegal column index "+columnIndex); //$NON-NLS-1$
				}
			}
			return NO_NAME;
		}

	}

	private final SelectionButtonDialogField fIsModuleCheckbox;

	static class ModuleList {
		TableViewer fViewer;
		List<String> fNames;
		public ModuleList(List<String> names) {
			fNames= names;
		}
	}
	private ModuleList[] fModuleLists= new ModuleList[3];
	private static final int IDX_AVAILABLE= 0;
	private static final int IDX_INCLUDED= 1;
	private static final int IDX_IMPLICITLY_INCLUDED= 2;

	private BuildPathBasePage fBasePage;

	private Button fAddIncludedButton;
	private Button fRemoveIncludedButton;
	private Button fPromoteIncludedButton;

	private final SelectionButtonDialogField fIsPatchCheckbox;
	private final StringDialogField fPatchedModule;

	private final ListDialogField<ModuleAddExpose> fAddExportsList;

	private final ListDialogField<ModuleAddReads> fAddReadsList;

	private final CPListElement fCurrCPElement;
	/** The element(s) targeted by the current CP entry, which will be the source module(s) of the added exports. */
	private IJavaElement[] fJavaElements;
	private Set<String> fModuleNames;

	private Map<String,List<String>> fModule2RequiredModules;

	private static final int IDX_ADD= 0;
	private static final int IDX_EDIT= 1;
	private static final int IDX_REMOVE= 2;


	public ModuleDialog(Shell parent, CPListElement entryToEdit, IJavaElement[] selectedElements, BuildPathBasePage basePage) {
		super(parent);

		fBasePage= basePage;
		fCurrCPElement= entryToEdit;
		fJavaElements= selectedElements;

		setTitle(NewWizardMessages.ModuleDialog_title);

		fIsModuleCheckbox= new SelectionButtonDialogField(SWT.CHECK);
		fIsModuleCheckbox.setLabelText(NewWizardMessages.ModuleDialog_defines_modules_label);
		fIsModuleCheckbox.setSelection(entryToEdit.getAttribute(CPListElement.MODULE) != null);
		fIsModuleCheckbox.setDialogFieldListener(field -> doSelectionChangedAllLists());

		// -- contents page initialized in createContentsTab()

		// -- details page:

		fIsPatchCheckbox= new SelectionButtonDialogField(SWT.CHECK);
		fIsPatchCheckbox.setLabelText(NewWizardMessages.ModuleDialog_patches_module_label);
		fIsPatchCheckbox.setDialogFieldListener(this::doPatchSelectionChanged);

		fPatchedModule= new StringDialogField();
		fPatchedModule.setLabelText(NewWizardMessages.ModuleDialog_patched_module_label);
		fPatchedModule.setDialogFieldListener(this::validateDetails);

		fAddExportsList= createDetailListContents(entryToEdit, NewWizardMessages.ModuleDialog_exports_label, new AddExportsAdapter(), ModuleAddExpose.class);
		fAddReadsList= createDetailListContents(entryToEdit, NewWizardMessages.ModuleDialog_reads_label, new AddReadsAdapter(), ModuleAddReads.class);

		initializeValues();

		doPatchSelectionChanged(fIsPatchCheckbox);
		doSelectionChangedAllLists();
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	private <T extends ModuleEncapsulationDetail> ListDialogField<T> createDetailListContents(CPListElement entryToEdit, String label, ListAdapter<T> adapter, Class<T> clazz) {
		String[] buttonLabels= new String[] {
				NewWizardMessages.ModuleDialog_detail_add,
				NewWizardMessages.ModuleDialog_detail_edit,
				NewWizardMessages.ModuleDialog_detail_remove
		};

		AddDetailsLabelProvider labelProvider= new AddDetailsLabelProvider();

		ListDialogField<T> detailsList= new ListDialogField<>(adapter, buttonLabels, labelProvider);

		detailsList.setLabelText(label);
		detailsList.setRemoveButtonIndex(IDX_REMOVE);
		detailsList.enableButton(IDX_EDIT, false);

		detailsList.setElements(entryToEdit.getModuleEncapsulationDetails(clazz));
		detailsList.selectFirstElement();
		return detailsList;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);
		GridLayout layout= new GridLayout(1, true);
		layout.marginBottom= 0;
		composite.setLayout(layout);

		Label description= new Label(composite, SWT.WRAP);

		description.setText(getDescriptionString());

		GridData data= new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1);
		data.widthHint= convertWidthInCharsToPixels(100);
		description.setLayoutData(data);

		fIsModuleCheckbox.doFillIntoGrid(composite, 3);


		TabFolder tabFolder= new TabFolder(composite, SWT.NONE);
		tabFolder.setFont(composite.getFont());
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		final TabItem tabItemContents= new TabItem(tabFolder, SWT.NONE);
		tabItemContents.setText(NewWizardMessages.ModuleDialog_contents_tab);
		tabItemContents.setImage(JavaPlugin.getImageDescriptorRegistry().get(JavaPluginImages.DESC_OBJS_MODULE));
		tabItemContents.setControl(createContentsTab(tabFolder));

		final TabItem tabItemDetails= new TabItem(tabFolder, SWT.NONE);
		tabItemDetails.setText(NewWizardMessages.ModuleDialog_details_tab);
		tabItemDetails.setImage(JavaPlugin.getImageDescriptorRegistry().get(JavaPluginImages.DESC_OBJS_MODULE_ATTRIB));
		tabItemDetails.setControl(createDetailsTab(tabFolder));

		tabFolder.addSelectionListener(widgetSelectedAdapter(e -> validateTab(e.widget, tabItemContents, tabItemDetails)));

		applyDialogFont(composite);
		updateStatus(new StatusInfo(IStatus.WARNING, NewWizardMessages.ModuleDialog_deprecated_warning));
		return composite;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		if (fBasePage.fSWTControl != null) {
			Button switchButton= createButton(parent, 2, NewWizardMessages.ModuleDialog_switchToTab_button, false);
			switchButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
				fBasePage.switchToTab(ModuleDependenciesPage.class);
				cancelPressed();
			}));
		}
		super.createButtonsForButtonBar(parent);
	}

	private String getDescriptionString() {
		String desc;
		String name= BasicElementLabels.getResourceName(fCurrCPElement.getPath().lastSegment());
		switch (fCurrCPElement.getEntryKind()) {
			case IClasspathEntry.CPE_CONTAINER:
				try {
					name= JavaElementLabels.getContainerEntryLabel(fCurrCPElement.getPath(), fCurrCPElement.getJavaProject());
				} catch (JavaModelException e) {
					name= BasicElementLabels.getPathLabel(fCurrCPElement.getPath(), false);
				}
				desc= NewWizardMessages.ModuleDialog_container_description;
				break;
			case IClasspathEntry.CPE_PROJECT:
				desc=  NewWizardMessages.ModuleDialog_project_description;
				break;
			default:
				desc=  NewWizardMessages.ModuleDialog_description;
		}

		return Messages.format(desc, name);
	}

	Composite createContentsTab(Composite parent) {
		Composite contentsPage= new Composite(parent, SWT.NONE);
		contentsPage.setFont(parent.getFont());
		applyDialogFont(contentsPage);

		GridLayout layout= new GridLayout();
		layout.marginTop= 5;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 3; // list , buttons , list
		contentsPage.setLayout(layout);
		contentsPage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// top
		createContentListContents(contentsPage,
				NewWizardMessages.ModuleDialog_availableModules_list,
				NewWizardMessages.ModuleDialog_availableModules_tooltip,
				IDX_AVAILABLE, IDX_INCLUDED);
		createHorizontalButtons(contentsPage);
		createContentListContents(contentsPage,
				NewWizardMessages.ModuleDialog_explicitlyIncludedModules_list,
				NewWizardMessages.ModuleDialog_explicitlyIncludedModules_tooltip,
				IDX_INCLUDED, IDX_AVAILABLE);

		// bottom
		Label spacer= new Label(contentsPage, SWT.NONE);
		GridData gd= new GridData();
		gd.horizontalSpan= 2;
		spacer.setLayoutData(gd);

		Composite lowerRight= new Composite(contentsPage, SWT.NONE);
		lowerRight.setLayout(new GridLayout(1, true));
		gd= new GridData(SWT.FILL, SWT.FILL, true, true);
		lowerRight.setLayoutData(gd);

		createVerticalButton(lowerRight);
		createContentListContents(lowerRight,
				NewWizardMessages.ModuleDialog_implicitelyIncludedModules_list,
				NewWizardMessages.ModuleDialog_implicitlyIncludedModule_tooltip,
				IDX_IMPLICITLY_INCLUDED, IDX_INCLUDED);

		validateContents();

		return contentsPage;
	}

	Composite createDetailsTab(Composite parent) {
		Composite detailPage= new Composite(parent, SWT.NONE);
		detailPage.setFont(parent.getFont());
		applyDialogFont(detailPage);

		GridLayout layout= new GridLayout();
		layout.marginTop= 5;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 3;
		detailPage.setLayout(layout);
		detailPage.setLayoutData(new GridData(GridData.FILL_BOTH));

		fIsPatchCheckbox.doFillIntoGrid(detailPage, 3);
		fPatchedModule.doFillIntoGrid(detailPage, 2);
		GridData data= (GridData) fPatchedModule.getLabelControl(null).getLayoutData();
		data.horizontalIndent= LayoutUtil.getIndent();

		Text patchedModuleText= fPatchedModule.getTextControl(null);
		BidiUtils.applyBidiProcessing(patchedModuleText, StructuredTextTypeHandlerFactory.JAVA);
		if (fJavaElements != null) {
			configureModuleContentAssist(patchedModuleText, moduleNames());
		}

		ColumnLayoutData[] columnDta= {
				new ColumnWeightData(2),
				new ColumnWeightData(3),
				new ColumnWeightData(2),
		};
		String[] headers= {
				NewWizardMessages.ModuleDialog_source_module_header,
				NewWizardMessages.ModuleDialog_package_header,
				NewWizardMessages.ModuleDialog_target_module_header
		};
		fAddExportsList.setTableColumns(new ListDialogField.ColumnsDescription(columnDta, headers, true));

		fAddExportsList.doFillIntoGrid(detailPage, 4);

		LayoutUtil.setHorizontalSpan(fAddExportsList.getLabelControl(null), 3);

		data= (GridData) fAddExportsList.getListControl(null).getLayoutData();
		data.grabExcessHorizontalSpace= true;
		data.heightHint= SWT.DEFAULT;

		columnDta= new ColumnWeightData[] {
				new ColumnWeightData(1),
				new ColumnWeightData(1),
		};
		headers= new String[] {
				NewWizardMessages.ModuleDialog_source_module_header,
				NewWizardMessages.ModuleDialog_target_module_header
		};
		fAddReadsList.setTableColumns(new ListDialogField.ColumnsDescription(columnDta, headers, true));

		fAddReadsList.doFillIntoGrid(detailPage, 4);

		LayoutUtil.setHorizontalSpan(fAddReadsList.getLabelControl(null), 3);

		data= (GridData) fAddReadsList.getListControl(null).getLayoutData();
		data.grabExcessHorizontalSpace= true;
		data.heightHint= SWT.DEFAULT;
		return detailPage;
	}

	// ======== widgets for the Contents tab: ========

	private void createContentListContents(Composite parent, String title, String tooltip, int idx, int targetIdx) {
		Composite box= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout(1, false);
		layout.marginBottom= 0;
		box.setLayout(layout);
		GridData gd= new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumWidth= 0;
		box.setLayoutData(gd);

		Label label= new Label(box, SWT.NONE);
		label.setText(title);
		label.setToolTipText(tooltip);

		TableViewer tableViewer= new TableViewer(box, SWT.MULTI | SWT.BORDER);
		tableViewer.setContentProvider(new ListContentProvider());
		tableViewer.setLabelProvider(new ModulesLabelProvider());
		tableViewer.addDoubleClickListener(e -> moveModuleEntry(idx, targetIdx));
		tableViewer.setInput(fModuleLists[idx].fNames);
		tableViewer.addSelectionChangedListener(e -> validateContents());
		tableViewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				return ((String)e1).compareTo((String)e2);
			}
		});

		PixelConverter converter= new PixelConverter(parent);
		gd= new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint= converter.convertWidthInCharsToPixels(30);
		gd.heightHint= converter.convertHeightInCharsToPixels(6);
		tableViewer.getControl().setLayoutData(gd);

		fModuleLists[idx].fViewer= tableViewer;
	}

	private void createHorizontalButtons(Composite parent) {
		org.eclipse.ui.ISharedImages sharedImages= PlatformUI.getWorkbench().getSharedImages();

		Composite box= new Composite(parent, SWT.NONE);
		box.setLayout(new GridLayout(1, true));

		fAddIncludedButton= new Button(box, SWT.PUSH);
		fAddIncludedButton.setImage(sharedImages.getImage(org.eclipse.ui.ISharedImages.IMG_TOOL_FORWARD));
		fAddIncludedButton.setToolTipText(NewWizardMessages.ModuleDialog_addToIncluded_tooltip);
		fAddIncludedButton.addSelectionListener(widgetSelectedAdapter(e -> moveModuleEntry(IDX_AVAILABLE, IDX_INCLUDED)));

		fRemoveIncludedButton= new Button(box, SWT.PUSH);
		fRemoveIncludedButton.setImage(sharedImages.getImage(org.eclipse.ui.ISharedImages.IMG_TOOL_BACK));
		fRemoveIncludedButton.setToolTipText(NewWizardMessages.ModuleDialog_removeFromIncluded_tooltip);
		fRemoveIncludedButton.addSelectionListener(widgetSelectedAdapter(e -> moveModuleEntry(IDX_INCLUDED, IDX_AVAILABLE)));
	}

	private void createVerticalButton(Composite parent) {
		fPromoteIncludedButton= new Button(parent, SWT.PUSH);
		fPromoteIncludedButton.setImage(JavaPlugin.getImageDescriptorRegistry().get(JavaPluginImages.DESC_BUTTON_MOVE_UP));
		fPromoteIncludedButton.setToolTipText(NewWizardMessages.ModuleDialog_addToExplicitlyIncluded_tooltip);
		fPromoteIncludedButton.addSelectionListener(widgetSelectedAdapter(e -> moveModuleEntry(IDX_IMPLICITLY_INCLUDED, IDX_INCLUDED)));
		GridData gd= new GridData();
		gd.horizontalAlignment= SWT.CENTER;
		fPromoteIncludedButton.setLayoutData(gd);
	}

	private boolean canRemoveIncludedModules() {
		if (fModuleLists[IDX_INCLUDED].fViewer.getSelection().isEmpty())
			return false;
		return fModuleLists[IDX_INCLUDED].fNames.size() + fModuleLists[IDX_AVAILABLE].fNames.size() > 1;
	}

	private void moveModuleEntry(int sourceIdx, int targetIdx) {
		ISelection selection= fModuleLists[sourceIdx].fViewer.getSelection();
		if (selection instanceof IStructuredSelection) {
			if (sourceIdx == IDX_INCLUDED && !canRemoveIncludedModules()) {
				return;
			}
			List<String> sourceList= fModuleLists[sourceIdx].fNames;
			List<String> targetList= fModuleLists[targetIdx].fNames;
			for (Object selected : ((IStructuredSelection) selection)) {
				if (selected instanceof String) {
					sourceList.remove(selected);
					targetList.add((String) selected);
				}
			}
			updateImplicitlyIncluded();
			fModuleLists[IDX_AVAILABLE].fViewer.refresh();
			fModuleLists[IDX_INCLUDED].fViewer.refresh();
			fModuleLists[IDX_IMPLICITLY_INCLUDED].fViewer.refresh();
			validateContents();
		}
	}

	public static void configureModuleContentAssist(Text textControl, Collection<String> moduleNames) {
		if (moduleNames.size() == 1) {
			textControl.setText(moduleNames.iterator().next());
		} else if (!moduleNames.isEmpty()) {
			Image image= JavaPlugin.getImageDescriptorRegistry().get(JavaPluginImages.DESC_OBJS_MODULE);
			JavaPrecomputedNamesAssistProcessor processor= new JavaPrecomputedNamesAssistProcessor(moduleNames, image);
			ControlContentAssistHelper.createTextContentAssistant(textControl, processor);
		}
	}

	// ======== updating & validation: ========

	protected void doPatchSelectionChanged(DialogField field) {
		fPatchedModule.setEnabled(fIsPatchCheckbox.isSelected() && moduleNames().size() != 1);
		validateDetails(field);
	}

	protected void doSelectionChangedAllLists() {
		doSelectionChanged(fAddExportsList);
		doSelectionChanged(fAddReadsList);
	}

	protected void doSelectionChanged(ListDialogField<? extends ModuleEncapsulationDetail> field) {
		boolean isModular= fIsModuleCheckbox.isSelected();
		List<? extends ModuleEncapsulationDetail> selected= field.getSelectedElements();
		field.enableButton(IDX_ADD, isModular && fJavaElements != null);
		field.enableButton(IDX_EDIT, isModular && canEdit(selected));
		field.enableButton(IDX_REMOVE, isModular && selected.size() > 0);
		validateDetails(field);
	}

	private boolean canEdit(List<? extends ModuleEncapsulationDetail> selected) {
		return selected.size() == 1;
	}

	private void validateTab(Widget widget, Widget contentItem, Widget detailsItem) {
		if (widget instanceof TabFolder) {
			TabItem[] selected= ((TabFolder) widget).getSelection();
			for (TabItem selectedItem : selected) {
				if (selectedItem == contentItem) {
					validateContents();
					return;
				} else if (selectedItem == detailsItem) {
					validateDetails(null);
					return;
				}
			}
		}
	}

	private void validateContents() {
		fAddIncludedButton.setEnabled(!fModuleLists[IDX_AVAILABLE].fViewer.getSelection().isEmpty());
		fRemoveIncludedButton.setEnabled(canRemoveIncludedModules());
		fPromoteIncludedButton.setEnabled(!fModuleLists[IDX_IMPLICITLY_INCLUDED].fViewer.getSelection().isEmpty());
		IStatus status= computeContentsStatus();
		updateStatus(status);
		if (status.isOK()) {
			status= computeDetailsStatus(null);
			if (status.getSeverity() == IStatus.ERROR) {
				updateStatus(new StatusInfo(IStatus.ERROR, NewWizardMessages.ModuleDialog_errorOnDetailsTab_error));
			}
		}
	}

	private IStatus computeContentsStatus() {
		StatusInfo info= new StatusInfo();
		if (fIsModuleCheckbox.isSelected()) {
			if (fJavaElements != null) {
				if (fModuleLists[IDX_INCLUDED].fNames.isEmpty()) {
					info.setError(NewWizardMessages.ModuleDialog_mustIncludeModule_error);
				} else if (fModuleLists[IDX_INCLUDED].fNames.size() + fModuleLists[IDX_AVAILABLE].fNames.size() == 1) {
					info.setInfo(NewWizardMessages.ModuleDialog_cannotLimitSingle_error);
				}
			} else {
				info.setInfo(NewWizardMessages.ModuleDialog_unknownModules_info);
			}
		}
		return info;
	}

	private void validateDetails(DialogField field) {
		IStatus status= computeDetailsStatus(field);
		updateStatus(status);
		if (status.isOK()) {
			status= computeContentsStatus();
			if (status.getSeverity() == IStatus.ERROR) {
				updateStatus(new StatusInfo(IStatus.ERROR, NewWizardMessages.ModuleDialog_errorOnContentsTab_error));
			}
		}
	}

	private StatusInfo computeDetailsStatus(DialogField field) {
		Set<String> packages= new HashSet<>();
		StatusInfo status= new StatusInfo();
		if (fIsPatchCheckbox.isSelected()) {
			String patchedModule= fPatchedModule.getText().trim();
			if (patchedModule.isEmpty()) {
				if (field == fIsPatchCheckbox) {
					status= newSilentError(); // silently disable OK button until user input is given
				} else {
					status.setError(NewWizardMessages.ModuleDialog_missingPatch_error);
				}
				Shell shell= getShell();
				if (shell != null) {
					fPatchedModule.getTextControl(shell).setFocus();
				}
			} else if (!moduleNames().isEmpty() && !moduleNames().contains(patchedModule)) {
				status.setError(Messages.format(NewWizardMessages.ModuleDialog_wrongPatch_error, patchedModule));
			} else if (isModuleExcluded(patchedModule)) {
				status.setError(Messages.format(NewWizardMessages.ModuleDialog_patchedModuleExcluded_error, patchedModule));
			}
		}
		if (status.isOK()) {
			for (ModuleAddExpose export : fAddExportsList.getElements()) {
				if (!packages.add(export.fPackage)) {
					status.setError(Messages.format(NewWizardMessages.ModuleDialog_duplicatePackage_error, export.fPackage));
					break;
				}
				if (isModuleExcluded(export.fSourceModule)) {
					status.setError(Messages.format(NewWizardMessages.ModuleDialog_exportSourceModuleExcluded_error,
							new String[]{ export.fPackage, export.fSourceModule }));
				}
			}
		}
		if (status.isOK()) {
			Set<String> readModules= new HashSet<>();
			for (ModuleAddReads reads : fAddReadsList.getElements()) {
				if (!readModules.add(reads.toString())) {
					status.setError(Messages.format(NewWizardMessages.ModuleDialog_duplicateReads_error, reads.toString()));
					break;
				}
				if (isModuleExcluded(reads.fSourceModule)) {
					status.setError(Messages.format(NewWizardMessages.ModuleDialog_readsSourceModuleExcluded_error,
							reads.fSourceModule));
				}
			}
		}
		if (status.isOK() && fJavaElements == null) {
			status.setInfo(NewWizardMessages.ModuleDialog_cannotEditDetails_info);
		}
		return status;
	}

	private boolean isModuleExcluded(String moduleName) {
		return fModuleLists[IDX_AVAILABLE].fNames.contains(moduleName);
	}

	// ======== operations on values, i.e., classpath entries, modules and packages: ========

	private void initializeValues() {
		fModule2RequiredModules= new HashMap<>();

		boolean isJava9JRE= isJava9JRE();
		List<String> availableNames= new ArrayList<>(moduleNames());
		List<String> includedNames= new ArrayList<>();
		List<LimitModules> limits= fCurrCPElement.getModuleEncapsulationDetails(LimitModules.class);
		if (!limits.isEmpty()) {
			for (LimitModules limitModules : limits) {
				includedNames.addAll(limitModules.fExplicitlyIncludedModules);
				availableNames.removeAll(limitModules.fExplicitlyIncludedModules);
			}
		} else if (isJava9JRE && isUnnamedModule()) {
			includedNames= defaultIncludedModuleNamesForUnnamedModule();
			availableNames.removeAll(includedNames);
		} else {
			includedNames= availableNames;
			availableNames= new ArrayList<>();
		}
		fModuleLists[IDX_AVAILABLE]= new ModuleList(availableNames);
		fModuleLists[IDX_INCLUDED]= new ModuleList(new ArrayList<>(includedNames));
		fModuleLists[IDX_IMPLICITLY_INCLUDED]= new ModuleList(new ArrayList<>());
		updateImplicitlyIncluded();

		// access to widgets may trigger validation, which needs all values to be initialized (non-null):

		if (isJava9JRE) {
			fIsModuleCheckbox.setEnabled(false);
		}

		List<ModulePatch> patchedModules= fCurrCPElement.getModuleEncapsulationDetails(ModulePatch.class);
		fIsPatchCheckbox.setSelection(!patchedModules.isEmpty());

		if (patchedModules.size() == 1)
			fPatchedModule.setText(patchedModules.iterator().next().fModule);
	}

	private boolean isJava9JRE() {
		if (fCurrCPElement.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
			IPackageFragmentRoot[] roots= findRoots(fCurrCPElement);
			if (roots.length > 1 && roots[0].getModuleDescription() != null)
				return true; // assume multi-module container is Java 9 JRE
		}
		return false;
	}
	private IPackageFragmentRoot[] findRoots(CPListElement element) {
		IClasspathEntry entry= element.getClasspathEntry();
		IPackageFragmentRoot[] roots= element.getJavaProject().findPackageFragmentRoots(entry);
		if (roots.length == 0) {
			// 2nd attempt in case "module=true" is not explicit on the real cp entry:
			entry= copyCPEntryWithoutModuleAttribute(entry);
			if (entry != null)
				roots= element.getJavaProject().findPackageFragmentRoots(entry);
		}
		return roots;
	}

	private IClasspathEntry copyCPEntryWithoutModuleAttribute(IClasspathEntry entry) {
		IClasspathAttribute[] oldAttributes= entry.getExtraAttributes();
		IClasspathAttribute[] newAttributes= new IClasspathAttribute[oldAttributes.length];
		int count= 0;
		for (IClasspathAttribute oldAttribute : oldAttributes) {
			if (!IClasspathAttribute.MODULE.equals(oldAttribute.getName())) {
				newAttributes[count++]= oldAttribute;
			}
		}
		if (count == oldAttributes.length)
			return null;
		newAttributes= count == 0 ? new IClasspathAttribute[0] : Arrays.copyOf(newAttributes, count);
		return JavaCore.newContainerEntry(entry.getPath(), entry.getAccessRules(), newAttributes, entry.isExported());
	}

	private Set<String> moduleNames() {
		if (fModuleNames != null)
			return fModuleNames;
		Set<String> moduleNames= new HashSet<>();
		if (fJavaElements != null) {
			for (IJavaElement element : fJavaElements) {
				if (element instanceof IPackageFragmentRoot) {
					IModuleDescription module= ((IPackageFragmentRoot) element).getModuleDescription();
					if (module != null) {
						recordModule(module, moduleNames);
					} else {
						try {
							recordModule(JavaCore.getAutomaticModuleDescription(element), moduleNames);
						}catch (JavaModelException e) {
							JavaPlugin.log(e);
						}
					}
				} else if (element instanceof IJavaProject) {
					try {
						IModuleDescription module= ((IJavaProject) element).getModuleDescription();
						if (module != null) {
							recordModule(module, moduleNames);
						} else {
							recordModule(JavaCore.getAutomaticModuleDescription(element), moduleNames);
						}
					} catch (JavaModelException e) {
						JavaPlugin.log(e);
					}
				}
			}
		}
		return fModuleNames= moduleNames;
	}

	private List<String> defaultIncludedModuleNamesForUnnamedModule() {
		if (fJavaElements != null) {
			List<IPackageFragmentRoot> roots= new ArrayList<>();
			for (IJavaElement element : fJavaElements) {
				if (element instanceof IPackageFragmentRoot) {
					roots.add((IPackageFragmentRoot) element);
				}
			}
			return JavaCore.defaultRootModules(roots);
		}
		return Collections.emptyList();
	}

	private void recordModule(IModuleDescription module, Set<String> moduleNames) {
		String moduleName= module.getElementName();
		if (moduleNames.add(moduleName)) {
			try {
				for (String required : module.getRequiredModuleNames()) {
					List<String> requiredModules= fModule2RequiredModules.get(moduleName);
					if (requiredModules == null) {
						requiredModules= new ArrayList<>();
						fModule2RequiredModules.put(moduleName, requiredModules);
					}
					requiredModules.add(required);
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
	}

	private void updateImplicitlyIncluded() {
		List<String> implicitNames= fModuleLists[IDX_IMPLICITLY_INCLUDED].fNames;
		fModuleLists[IDX_AVAILABLE].fNames.addAll(implicitNames);
		implicitNames.clear();
		for (String explicitName : fModuleLists[IDX_INCLUDED].fNames) {
			if (!implicitNames.contains(explicitName)) {
				collectRequired(explicitName);
			}
		}
	}

	private void collectRequired(String explicitName) {
		List<String> requireds= fModule2RequiredModules.get(explicitName);
		if (requireds == null) {
			return;
		}
		List<String> implicitNames= fModuleLists[IDX_IMPLICITLY_INCLUDED].fNames;
		for (String required : requireds) {
			if (!fModuleLists[IDX_INCLUDED].fNames.contains(required)) {
				if (!implicitNames.contains(required)) {
					if (fModuleLists[IDX_AVAILABLE].fNames.remove(required)) {
						implicitNames.add(required);
						collectRequired(required);
					}
				}
			}
		}
	}

	private String getSourceModuleName() {
		if (fJavaElements == null || fJavaElements.length != 1) {
			return NO_NAME;
		}
		IModuleDescription module= null;
		switch (fJavaElements[0].getElementType()) {
			case IJavaElement.JAVA_PROJECT:
				try {
					module= ((IJavaProject) fJavaElements[0]).getModuleDescription();
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
				break;
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				module= ((IPackageFragmentRoot) fJavaElements[0]).getModuleDescription();
				break;
			default:
				// not applicable
		}
		return module != null ? module.getElementName() : NO_NAME;
	}

	private String getCurrentModuleName() {
		IModuleDescription module= null;
		try {
			module= fCurrCPElement.getJavaProject().getModuleDescription();
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return module != null ? module.getElementName() : JavaModelUtil.ALL_UNNAMED;
	}

	private boolean isUnnamedModule() {
		IModuleDescription module= null;
		try {
			module= fCurrCPElement.getJavaProject().getModuleDescription();
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return module == null;
	}

	// -------- TypeRestrictionAdapter --------

	private abstract class ListAdapter<T extends ModuleEncapsulationDetail> implements IListAdapter<T> {
		/**
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter#customButtonPressed(org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField, int)
		 */
		@Override
		public void customButtonPressed(ListDialogField<T> field, int index) {
			if (index == IDX_ADD) {
				addEntry(field);
			} else if (index == IDX_EDIT) {
				doubleClicked(field);
			}
		}

		/**
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter#selectionChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField)
		 */
		@Override
		public void selectionChanged(ListDialogField<T> field) {
			doSelectionChanged(field);
		}
		/**
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter#doubleClicked(org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField)
		 */
		@Override
		public void doubleClicked(ListDialogField<T> field) {
			List<T> selElements= field.getSelectedElements();
			if (selElements.size() != 1) {
				return;
			}
			editEntry(field, selElements.get(0));
		}

		abstract void addEntry(ListDialogField<T> field);

		abstract void editEntry(ListDialogField<T> field, T detail);
	}

	private class AddExportsAdapter extends ListAdapter<ModuleAddExpose> {

		@Override
		void addEntry(ListDialogField<ModuleAddExpose> field) {
			ModuleAddExport initialValue= new ModuleAddExport(getSourceModuleName(), NO_NAME, getCurrentModuleName(), null);
			ModuleAddExportsDialog dialog= new ModuleAddExportsDialog(getShell(), fJavaElements, null, initialValue, null);
			if (dialog.open() == Window.OK) {
				ModuleAddExpose export= dialog.getExport(fCurrCPElement.findAttributeElement(CPListElement.MODULE));
				if (export != null)
					field.addElement(export);
			}
		}

		@Override
		void editEntry(ListDialogField<ModuleAddExpose> field, ModuleAddExpose export) {
			ModuleAddExportsDialog dialog= new ModuleAddExportsDialog(getShell(), fJavaElements, null, export, null);
			if (dialog.open() == Window.OK) {
				ModuleAddExpose newExport= dialog.getExport(fCurrCPElement.findAttributeElement(CPListElement.MODULE));
				if (newExport != null) {
					field.replaceElement(export, newExport);
				} else {
					field.removeElement(export);
				}
			}
		}
	}

	private class AddReadsAdapter extends ListAdapter<ModuleAddReads> {

		@Override
		void addEntry(ListDialogField<ModuleAddReads> field) {
			ModuleAddReads initialValue= new ModuleAddReads(getSourceModuleName(), NO_NAME, null);
			ModuleAddReadsDialog dialog= new ModuleAddReadsDialog(getShell(), fJavaElements, initialValue);
			if (dialog.open() == Window.OK) {
				ModuleAddReads reads= dialog.getReads(fCurrCPElement.findAttributeElement(CPListElement.MODULE));
				if (reads != null)
					field.addElement(reads);
			}
		}

		@Override
		void editEntry(ListDialogField<ModuleAddReads> field, ModuleAddReads reads) {
			ModuleAddReadsDialog dialog= new ModuleAddReadsDialog(getShell(), fJavaElements, reads);
			if (dialog.open() == Window.OK) {
				ModuleAddReads newReads= dialog.getReads(fCurrCPElement.findAttributeElement(CPListElement.MODULE));
				if (newReads != null) {
					field.replaceElement(reads, newReads);
				} else {
					field.removeElement(reads);
				}
			}
		}
	}

	/**
	 * When the dialog is closed, this method provides the results.
	 * @return an array of ModuleEncapsulationDetail encoding the result of editing
	 */
	public ModuleEncapsulationDetail[] getAllDetails() {
		if (!fIsModuleCheckbox.isSelected())
			return null;
		CPListElementAttribute attribute= fCurrCPElement.findAttributeElement(CPListElement.MODULE);
		List<ModuleEncapsulationDetail> allElements= new ArrayList<>();
		allElements.addAll(fAddExportsList.getElements());
		allElements.addAll(fAddReadsList.getElements());
		if (fIsPatchCheckbox.isSelected()) {
			String patchedModule= fPatchedModule.getText().trim();
			if (!patchedModule.isEmpty())
				allElements.add(ModulePatch.fromString(attribute, patchedModule));
		}
		if (modifiesContents()) {
			allElements.add(new ModuleEncapsulationDetail.LimitModules(fModuleLists[IDX_INCLUDED].fNames, attribute));
		}
		return allElements.toArray(new ModuleEncapsulationDetail[allElements.size()]);
	}

	private boolean modifiesContents() {
		if (fModuleLists[IDX_AVAILABLE].fNames.isEmpty() && fModuleLists[IDX_IMPLICITLY_INCLUDED].fNames.isEmpty()) {
			return false; // all modules are "included" - this includes the single-module case.
		}
		if (isUnnamedModule()) {
			// for an unnamed module we need to compare current selection state against defaults per JEP 261:
			Set<String> initialNames = new HashSet<>(defaultIncludedModuleNamesForUnnamedModule());
			for (String name : fModuleLists[IDX_INCLUDED].fNames) {
				if (!initialNames.remove(name))
					return true;
			}
			return !initialNames.isEmpty();
		} else {
			return true;
		}
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.MODULE_DIALOG);
	}

	public static StatusInfo newSilentError() {
		return new StatusInfo(IStatus.ERROR, Util.ZERO_LENGTH_STRING);
	}
}
