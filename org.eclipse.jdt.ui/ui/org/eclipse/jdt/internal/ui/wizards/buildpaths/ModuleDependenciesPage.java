/*******************************************************************************
 * Copyright (c) 2019 GK Software SE, and others.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.ibm.icu.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IProject;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleDependenciesList.ModuleKind;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.LimitModules;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.ModulePatch;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.TreeListDialogField;

/*
 * TODO:
 * LHS:
 * - module kind "Upgrade" of a System Library (incl. icon decoration)
 * General:
 * - distinguish test/main dependencies
 * - special elements: ALL-UNNAMED, ALL-SYSTEM ...
 * - Help pages and reference to it
 *    (add to ModuleSelectionDialog.configureShell(), ModuleDependenciesPage.getControl())
 */
public class ModuleDependenciesPage extends BuildPathBasePage {

	/** Composed image descriptor consisting of a base image and optionally a decoration overlay. */
	static class DecoratedImageDescriptor extends CompositeImageDescriptor {
		private ImageDescriptor fBaseImage;
		private ImageDescriptor fOverlay;
		private boolean fDrawAtOffset;
		public DecoratedImageDescriptor(ImageDescriptor baseImage, ImageDescriptor overlay, boolean drawAtOffset) {
			fBaseImage= baseImage;
			fOverlay= overlay;
			fDrawAtOffset= drawAtOffset;
		}
		@Override
		protected void drawCompositeImage(int width, int height) {
			drawImage(createCachedImageDataProvider(fBaseImage), 0, 0);
			if (fOverlay != null) {
				CachedImageDataProvider provider= createCachedImageDataProvider(fOverlay);
				if (fDrawAtOffset) {
					drawImage(provider, getSize().x - provider.getWidth(), 0);
				} else {
					drawImage(provider, 0, 0);
				}
			}
		}
		@Override
		protected Point getSize() {
			return ModuleDependenciesList.MEDIUM_SIZE;
		}
		@Override
		public int hashCode() {
			return Objects.hash(fBaseImage, fOverlay);
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DecoratedImageDescriptor other= (DecoratedImageDescriptor) obj;
			if (!Objects.equals(fBaseImage, other.fBaseImage)) {
				return false;
			}
			if (!Objects.equals(fOverlay, other.fOverlay)) {
				return false;
			}
			return true;
		}
	}

	private final ListDialogField<CPListElement> fClassPathList; // shared with other pages
	private final IStatusChangeListener fContext;
	private IJavaProject fCurrJProject;

	// LHS list:
	private ModuleDependenciesList fModuleList;
	private Button fAddSystemModuleButton;

	// RHS tree:
	private final TreeListDialogField<Object> fDetailsList;

	// bi-directional dependency graph:
	private Map<String,List<String>> fModule2RequiredModules;
	private Map<String,List<String>> fModuleRequiredByModules;

	// cached JRE content:
	private IPackageFragmentRoot[] fAllSystemRoots; // unfiltered
	private Collection<String> fAllDefaultSystemModules; // if current is unnamed module: transitive closure of default root modules (names)

	public final Map<String,String> fPatchMap= new HashMap<>();
	private boolean needReInit= false;

	public ModuleDependenciesPage(IStatusChangeListener context, CheckedListDialogField<CPListElement> classPathList) {
		fClassPathList= classPathList;
		fContext= context;
		fSWTControl= null;

		String[] buttonLabels= new String[] {
				NewWizardMessages.ModuleDependenciesPage_modules_remove_button,
				/* */ null,
				NewWizardMessages.ModuleDependenciesPage_modules_read_button,
				NewWizardMessages.ModuleDependenciesPage_modules_expose_package_button,
				/* */ null,
				NewWizardMessages.ModuleDependenciesPage_modules_patch_button,
				/* */ null,
				NewWizardMessages.ModuleDependenciesPage_modules_edit_button,
				/* */ null,
				NewWizardMessages.ModuleDependenciesPage_showJPMSOptions_button
			};

		fModuleList= new ModuleDependenciesList();

		ModuleDependenciesAdapter adapter= new ModuleDependenciesAdapter(this);
		fDetailsList= new TreeListDialogField<>(adapter, buttonLabels, new ModuleDependenciesAdapter.ModularityDetailsLabelProvider());
		fDetailsList.setDialogFieldListener(adapter);
		fDetailsList.setLabelText(NewWizardMessages.ModuleDependenciesPage_details_label);

		adapter.setList(fDetailsList);

		fDetailsList.setViewerComparator(new ModuleDependenciesAdapter.ElementSorter());
	}

	@Override
	public Control getControl(Composite parent) {
		PixelConverter converter= new PixelConverter(parent);

		Composite composite= new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());
		GridLayout layout= new GridLayout(2, false);
		layout.marginBottom= 0;
		composite.setLayout(layout);
		GridData gd= new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumWidth= 0;
		composite.setLayoutData(gd);

		// === left: ===
		Composite left= new Composite(composite, SWT.NONE);
		layout= new GridLayout(1, false);
		layout.marginBottom= 0;
		left.setLayout(layout);
		gd= new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumWidth= 0;
		left.setLayoutData(gd);

		Label title= new Label(left, SWT.NONE);
		title.setText(NewWizardMessages.ModuleDependenciesPage_modules_label);

		fModuleList.createViewer(left, converter);
		fModuleList.setSelectionChangedListener(this::selectModule);

		fAddSystemModuleButton= new Button(left, SWT.NONE);
		fAddSystemModuleButton.setText(NewWizardMessages.ModuleDependenciesPage_addSystemModule_button);
		fAddSystemModuleButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> addSystemModules()));

		// === right: ===
		Composite right= new Composite(composite, SWT.NONE);
		layout= new GridLayout(2, false);
		layout.marginBottom= 0;
		right.setLayout(layout);
		gd= new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumWidth= 0;
		right.setLayoutData(gd);

		LayoutUtil.doDefaultLayout(right, new DialogField[] { fDetailsList }, true, SWT.DEFAULT, SWT.DEFAULT);
		LayoutUtil.setHorizontalGrabbing(fDetailsList.getTreeControl(null));

		int buttonBarWidth= converter.convertWidthInCharsToPixels(24);
		fDetailsList.setButtonsMinWidth(buttonBarWidth);

		fDetailsList.setViewerComparator(new CPListElementSorter());

		((CTabFolder) parent).addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			if (e.item.getData() == this && fCurrJProject != null)
				init(fCurrJProject);
		}));
		fSWTControl= composite;

		return composite;
	}

	@Override
	public void init(IJavaProject jproject) {
		fCurrJProject= jproject;
		if (Display.getCurrent() != null) {
			scanModules();
		} else {
			Display.getDefault().asyncExec(this::scanModules);
		}
	}

	protected void scanModules() {
		fModuleList.clear();
		if (!JavaModelUtil.is9OrHigher(fCurrJProject)) {
			fModuleList.fNames.add(NewWizardMessages.ModuleDependenciesPage_nonModularProject_dummy);
			fModuleList.refresh();
			fModuleList.setEnabled(false);
			fAddSystemModuleButton.setEnabled(false);
			fDetailsList.removeAllElements();
			fDetailsList.refresh();
			ModuleDependenciesAdapter.updateButtonEnablement(fDetailsList, false, false, false, false);
			return;
		}
		fModuleList.setEnabled(true);
		fAddSystemModuleButton.setEnabled(true);
		fModule2RequiredModules= new HashMap<>();
		fModuleRequiredByModules= new HashMap<>();
		Set<String> recordedModules= new HashSet<>();

		List<CPListElement> cpelements= fClassPathList.getElements();

		for (CPListElement cpe : cpelements) {
			switch (cpe.getEntryKind()) {
				case IClasspathEntry.CPE_SOURCE:
					IPackageFragmentRoot[] fragmentRoots= fCurrJProject.findPackageFragmentRoots(cpe.getClasspathEntry());
					if (fragmentRoots != null && fragmentRoots.length == 1) {
						for (IPackageFragmentRoot fragmentRoot : fragmentRoots) {
							IModuleDescription module= fragmentRoot.getModuleDescription();
							if (module != null) {
								recordModule(module, recordedModules, cpe, ModuleKind.Focus);
								break;
							}
						}
					}
					break;
				case IClasspathEntry.CPE_PROJECT:
					IProject project= fCurrJProject.getProject().getWorkspace().getRoot().getProject(cpe.getClasspathEntry().getPath().toString());
					try {
						IJavaProject jProject= JavaCore.create(project);
						IModuleDescription module= jProject.getModuleDescription();
						ModuleKind kind= ModuleKind.Normal;
						if (module == null) {
							module= JavaCore.getAutomaticModuleDescription(jProject);
							kind= ModuleKind.Automatic;
						}
						if (module != null) {
							recordModule(module, recordedModules, cpe, kind);
						}
					} catch (JavaModelException e) {
						// ignore
					}
					break;
				case IClasspathEntry.CPE_CONTAINER:
					ModuleKind kind= LibrariesWorkbookPage.isJREContainer(cpe.getPath()) ? ModuleKind.System : ModuleKind.Normal;
					int shownModules= 0;
					boolean isModular= cpe.getAttribute(CPListElement.MODULE) instanceof ModuleEncapsulationDetail[];
					for (Object object : cpe.getChildren(true)) {
						if (object instanceof CPListElement) {
							CPListElement childElement= (CPListElement) object;
							IModuleDescription childModule= childElement.getModule();
							if (childModule != null) {
								fModuleList.addModule(childModule, childElement, kind);
								shownModules++;
							} else if (isModular && kind != ModuleKind.System) {
								for (IPackageFragmentRoot packageRoot : fCurrJProject.findUnfilteredPackageFragmentRoots(childElement.getClasspathEntry())) {
									try {
										IModuleDescription autoModule= JavaCore.getAutomaticModuleDescription(packageRoot);
										recordModule(autoModule, recordedModules, childElement, ModuleKind.Automatic);
									} catch (JavaModelException | IllegalArgumentException e) {
										JavaPlugin.log(e);
									}
								}
							}
						}
					}
					if (kind == ModuleKind.System) {
						// additionally capture dependency information about all system module disregarding --limit-modules
						fAllSystemRoots= fCurrJProject.findUnfilteredPackageFragmentRoots(cpe.getClasspathEntry());
						for (IPackageFragmentRoot packageRoot : fAllSystemRoots) {
							IModuleDescription module= packageRoot.getModuleDescription();
							if (module != null) {
								recordModule(module, recordedModules, null/*don't add to fModuleList*/, kind);
							}
						}
						if (fAllSystemRoots.length == shownModules) {
							fAddSystemModuleButton.setEnabled(false);
						}
						try {
							if (fCurrJProject.getModuleDescription() == null) { // cache default roots when compiling the unnamed module:
								fAllDefaultSystemModules= closure(JavaCore.defaultRootModules(Arrays.asList(fAllSystemRoots)));
							}
						} catch (JavaModelException e) {
							JavaPlugin.log(e);
						}
					}
					break;
				default: // LIBRARY & VARIABLE:
					IPackageFragmentRoot[] roots= fCurrJProject.findPackageFragmentRoots(cpe.getClasspathEntry());
					if (roots.length == 0) {
						fContext.statusChanged(new StatusInfo(IStatus.WARNING, NewWizardMessages.ModuleDependenciesPage_outOfSync_warning));
						break;
					}
					for (IPackageFragmentRoot packageRoot : roots) {
						IModuleDescription module= packageRoot.getModuleDescription();
						kind= ModuleKind.Normal;
						if (module == null) {
							try {
								module= JavaCore.getAutomaticModuleDescription(packageRoot);
								kind= ModuleKind.Automatic;
							} catch (JavaModelException | IllegalArgumentException e) {
								// ignore
							}
						}
						if (module != null) {
							recordModule(module, recordedModules, cpe, kind);
							break;
						}
					}
			}
		}
		buildPatchMap();
		fModuleList.captureInitial();
		fModuleList.refresh();
	}

	public Collection<String> getAllModules() {
		return fModuleList.fNames;
	}

	public void buildPatchMap() {
		fPatchMap.clear();
		for (CPListElement cpe : fClassPathList.getElements()) {
			Object value= cpe.getAttribute(CPListElement.MODULE);
			if (value instanceof ModuleEncapsulationDetail[]) {
				for (ModuleEncapsulationDetail detail : (ModuleEncapsulationDetail[]) value) {
					if (detail instanceof ModulePatch) {
						ModulePatch patch= (ModulePatch) detail;
						for (String path : patch.getPathArray()) {
							fPatchMap.put(path, patch.fModule);
							if (path.startsWith(fCurrJProject.getPath().toString())) {
								fModuleList.setFocusModule(patch.fModule);
							}
						}
					}
				}
			}
		}
	}

	private void recordModule(IModuleDescription module, Set<String> moduleNames, CPListElement cpe, ModuleKind kind) {
		if (module.getElementName().isEmpty()) return; // assume this to be an ill-configured auto module
		if (cpe != null) {
			fModuleList.addModule(module, cpe, kind);
		}
		String moduleName= module.getElementName();
		if (moduleNames.add(moduleName)) {
			try {
				for (String required : module.getRequiredModuleNames()) {
					List<String> otherModules= fModule2RequiredModules.get(moduleName);
					if (otherModules == null) {
						otherModules= new ArrayList<>();
						fModule2RequiredModules.put(moduleName, otherModules);
					}
					otherModules.add(required);

					otherModules= fModuleRequiredByModules.get(required);
					if (otherModules == null) {
						otherModules= new ArrayList<>();
						fModuleRequiredByModules.put(required, otherModules);
					}
					otherModules.add(moduleName);
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
	}


	@Override
	public List<?> getSelection() {
		return fDetailsList.getSelectedElements();
	}

	@Override
	public void setSelection(List<?> selElements, boolean expand) {
		fDetailsList.selectElements(new StructuredSelection(selElements));
		if (expand) {
			for (Object selElement : selElements) {
				fDetailsList.expandElement(selElement, 1);
			}
		}
	}

	public void setSelectionToModule(String moduleName) {
		int idx= fModuleList.fNames.indexOf(moduleName);
		if (idx != -1) {
			fModuleList.setSelectionToModule(moduleName);
		}
	}

	private void selectModule(List<CPListElement> elements, IModuleDescription module) {
		fDetailsList.removeAllElements();
		boolean enableAddExport= true;
		if (elements.size() == 1) {
			CPListElement element= elements.get(0);
			fDetailsList.addElement(new ModuleDependenciesAdapter.DeclaredDetails(module, element));
			ModuleKind moduleKind= fModuleList.getModuleKind(element);
			ModuleDependenciesAdapter.ConfiguredDetails configured= new ModuleDependenciesAdapter.ConfiguredDetails(module, element, moduleKind, this);
			fDetailsList.addElement(configured);
			fDetailsList.expandElement(configured, 1);
			if (moduleKind == ModuleKind.System) {
				enableAddExport = JavaCore.DISABLED.equals(this.fCurrJProject.getOption(JavaCore.COMPILER_RELEASE, false));
			}
		}
		if (!enableAddExport) {
			setStatus(new Status(IStatus.INFO, JavaUI.ID_PLUGIN,
					MessageFormat.format(NewWizardMessages.ModuleDependenciesPage_addExport_notWith_release_info, module.getElementName())));
		} else {
			setStatus(StatusInfo.OK_STATUS);
		}
		ModuleDependenciesAdapter.updateButtonEnablement(fDetailsList, elements.size() == 1, !elements.isEmpty(), true, enableAddExport);
	}

	@Override
	public boolean isEntryKind(int kind) {
		return true;
	}

	public void setStatus(IStatus status) {
		fContext.statusChanged(status);
	}

	@Override
	public void setFocus() {
    	fDetailsList.setFocus();
	}

	public void unsetFocusModule(CPListElement elem) {
		fModuleList.unsetFocusModule(elem);
		fModuleList.refresh();
	}

	public void refreshModulesList() {
		fModuleList.refresh();
	}

	void addSystemModules() {
		try {
			CPListElement cpListElement= findSystemLibraryElement();
			ModuleSelectionDialog dialog= ModuleSelectionDialog.forSystemModules(getShell(), fCurrJProject, cpListElement.getClasspathEntry(), fModuleList.fNames, this::computeForwardClosure);
			if (dialog.open() == IDialogConstants.OK_ID) {
				List<IModuleDescription> modulesToAdd= dialog.getResult();
				addSystemModules(cpListElement, modulesToAdd);
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
	}

	void addSystemModules(CPListElement cpListElement, List<IModuleDescription> modulesToAdd) {
		for (IModuleDescription addedModule : modulesToAdd) {
			fModuleList.addModule(addedModule, getOrCreateModuleCPE(cpListElement, addedModule), ModuleKind.System);
		}
		updateLimitModules(cpListElement.findAttributeElement(CPListElement.MODULE));
		fModuleList.refresh();
		this.needReInit= true;
	}

	public void addToSystemModules(List<IModuleDescription> modulesToAdd) throws JavaModelException {
		addSystemModules(findSystemLibraryElement(), modulesToAdd);
	}

	CPListElement getOrCreateModuleCPE(CPListElement parentCPE, IModuleDescription module) {
		CPListElement element= fModuleList.fModule2Element.get(module.getElementName());
		if (element != null) {
			return element;
		}
		try {
			IClasspathEntry entry= fCurrJProject.getClasspathEntryFor(module.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT).getPath());
			if (entry == null) {
				return null;
			}
			return CPListElement.create(parentCPE, entry, module, true, fCurrJProject);
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return null;
		}
	}
	public CPListElement findSystemLibraryElement() throws JavaModelException {
		for (CPListElement cpListElement : fClassPathList.getElements()) {
			if (LibrariesWorkbookPage.isJREContainer(cpListElement.getPath()))
				return cpListElement;
		}
		throw new JavaModelException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), "Project "+fCurrJProject.getElementName()+" has no system library"));  //$NON-NLS-1$//$NON-NLS-2$
	}

	void removeModules() {
		List<CPListElement> selectedElements= fModuleList.getSelectedElements();
		List<String> selectedModuleNames= new ArrayList<>();
		Set<String> allModulesToRemove= new HashSet<>();
		boolean removeConfirmed= false;
		for (CPListElement selectedElement : selectedElements) {
			if (fModuleList.getModuleKind(selectedElement) == ModuleKind.Focus) {
				MessageDialog.openError(getShell(), NewWizardMessages.ModuleDependenciesPage_removeModule_dialog_title,
						NewWizardMessages.ModuleDependenciesPage_removeCurrentModule_error);
				return;
			}
			IModuleDescription mod= selectedElement.getModule();
			if (mod == null) {
				// offer to switch to the suitable Modulepath tab, but need to collect some info, first:
				IClasspathEntry selectedEntry= selectedElement.getClasspathEntry();
				String moduleName= selectedElement.getPath().lastSegment();
				boolean isLibrary;
				switch (selectedEntry.getEntryKind()) {
					case IClasspathEntry.CPE_LIBRARY:
						for (IPackageFragmentRoot packageRoot : fCurrJProject.findPackageFragmentRoots(selectedEntry)) {
							IModuleDescription module= packageRoot.getModuleDescription();
							if (module == null) {
								try {
									module= JavaCore.getAutomaticModuleDescription(packageRoot);
								} catch (JavaModelException | IllegalArgumentException e) {
									// ignore
								}
							}
							if (module != null) {
								moduleName= module.getElementName();
								break;
							}
						}
						//$FALL-THROUGH$
					case IClasspathEntry.CPE_CONTAINER:
					case IClasspathEntry.CPE_VARIABLE:
						isLibrary= true;
						break;
					default:
						isLibrary= false;
				}
				offerSwitchToTab(getShell(),
						NewWizardMessages.ModuleDependenciesPage_removeModule_dialog_title,
						MessageFormat.format(NewWizardMessages.ModuleDependenciesPage_removeModule_error_with_hint,
								moduleName, NewWizardMessages.ModuleDependenciesPage_removeSystemModule_error_hint),
						isLibrary);
				return;
			}
			String moduleName= mod.getElementName();
			if ("java.base".equals(moduleName)) { //$NON-NLS-1$
				MessageDialog.openError(getShell(), NewWizardMessages.ModuleDependenciesPage_removeModule_dialog_title,
						MessageFormat.format(NewWizardMessages.ModuleDependenciesPage_removeModule_error_with_hint, moduleName, "")); //$NON-NLS-1$
				return;
			}
			selectedModuleNames.add(moduleName);
			String problemModule= collectModulesToRemove(moduleName, allModulesToRemove);
			if (problemModule != null) {
				int lastArrow= problemModule.lastIndexOf("->"); //$NON-NLS-1$
				String leafMod= lastArrow == -1 ? problemModule : problemModule.substring(lastArrow+2);
				int answer= MessageDialog.open(MessageDialog.WARNING, getShell(), NewWizardMessages.ModuleDependenciesPage_removeModule_dialog_title,
								MessageFormat.format(NewWizardMessages.ModuleDependenciesPage_moduleIsRequired_warning, leafMod, problemModule),
								SWT.NONE,
								NewWizardMessages.ModuleDependenciesPage_remove_button, IDialogConstants.CANCEL_LABEL);
				if (answer != 0) {
					return;
				}
				removeConfirmed= true;
			}
		}
		String seedModules= String.join(", ", selectedModuleNames); //$NON-NLS-1$
		if (allModulesToRemove.size() == selectedModuleNames.size()) {
			if (removeConfirmed || confirmRemoveModule(MessageFormat.format(NewWizardMessages.ModuleDependenciesPage_removingModule_message, seedModules))) {
				fModuleList.fNames.removeAll(selectedModuleNames);
				fModuleList.refresh();
			}
		} else {
			StringBuilder message= new StringBuilder(
					MessageFormat.format(NewWizardMessages.ModuleDependenciesPage_removingModuleTransitive_message, seedModules));
			// append sorted list minus the selected module:
			message.append(allModulesToRemove.stream()
					.filter(m -> !seedModules.contains(m))
					.sorted()
					.collect(Collectors.joining("\n\t", "\t", ""))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (!confirmRemoveModule(message.toString()))
				return;
			fModuleList.fNames.removeAll(allModulesToRemove);
			fModuleList.refresh();
		}
		for (CPListElement elem: selectedElements) {
			Object container= elem.getParentContainer();
			if (container instanceof CPListElement) {
				CPListElement containerElement= (CPListElement) container;
				if (LibrariesWorkbookPage.isJREContainer(containerElement.getPath())) {
					CPListElementAttribute attribute= containerElement.findAttributeElement(CPListElement.MODULE);
					updateLimitModules(attribute);
					break;
				}
			}
		}
		this.needReInit= true;
	}

	/** Destructively reed the needReInit flag.
	 * @return {@code true} if the classpath needs to be reinitialized due to change of extent of the system library
	 */
	public boolean needReInit() {
		try {
			return this.needReInit;
		} finally {
			this.needReInit= false;
		}
	}
	private Set<String> computeForwardClosure(List<String> seeds) {
		Set<String> closure= new HashSet<>();
		collectForwardClosure(seeds, closure);
		return closure;
	}
	private void collectForwardClosure(List<String> seeds, Set<String> closure) {
		for (String seed : seeds) {
			if (closure.add(seed) && !fModuleList.fNames.contains(seed)) {
				List<String> deps= fModule2RequiredModules.get(seed);
				if (deps != null) {
					collectForwardClosure(deps, closure);
				}
			}
		}
	}

	private String collectModulesToRemove(String mod, Set<String> modulesToRemove) {
		if (fModuleList.fNames.contains(mod) && modulesToRemove.add(mod)) {
			String problemModules= null; // at most one path is detected, even if several exist
			List<String> requireds= fModuleRequiredByModules.get(mod);
			if (requireds != null) {
				for (String required : requireds) {
					if (fModuleList.getModuleKind(required) == ModuleKind.Focus) {
						// direct dependency beats any indirect one:
						problemModules= required + "->" + mod; //$NON-NLS-1$
						continue; // do not attempt to remove the focus module
					}
					String probMods= collectModulesToRemove(required, modulesToRemove);
					if (probMods != null && problemModules == null)
						problemModules= probMods + "->" + mod; //$NON-NLS-1$
				}
			}
			return problemModules;
		}
		return null;
	}

	private boolean confirmRemoveModule(String message) {
		int answer= MessageDialog.open(MessageDialog.QUESTION, getShell(),
				NewWizardMessages.ModuleDependenciesPage_removeModule_dialog_title, message, SWT.NONE,
				NewWizardMessages.ModuleDependenciesPage_remove_button, IDialogConstants.CANCEL_LABEL);
		return answer == 0;
	}

	private void updateLimitModules(CPListElementAttribute moduleAttribute) {
		Object value= moduleAttribute.getValue();
		if (value instanceof ModuleEncapsulationDetail[]) {
			Collection<String> allSystemModules= allDefaultSystemModules();
			if (allSystemModules.size() == fModuleList.fNames.size() && allSystemModules.containsAll(fModuleList.fNames)) {
				// no longer relevant, remove:
				ModuleEncapsulationDetail[] details= (ModuleEncapsulationDetail[]) value;
				int retainCount= 0;
				for (ModuleEncapsulationDetail detail : details) {
					if (!(detail instanceof LimitModules)) {
						details[retainCount++]= detail;
					}
				}
				if (retainCount < details.length)
					moduleAttribute.setValue(Arrays.copyOf(details, retainCount));
				return;
			}
		}
		LimitModules limitModules= new ModuleEncapsulationDetail.LimitModules(reduceNames(fModuleList.fNames), moduleAttribute);
		if (value instanceof ModuleEncapsulationDetail[]) {
			ModuleEncapsulationDetail[] details= (ModuleEncapsulationDetail[]) value;
			for (int i= 0; i < details.length; i++) {
				if (details[i] instanceof LimitModules) {
					// replace existing --limit-modules
					details[i]= limitModules;
					moduleAttribute.setValue(details);
					return;
				}
			}
			if (details.length > 0) {
				// append to existing list of other details:
				ModuleEncapsulationDetail[] newDetails= Arrays.copyOf(details, details.length+1);
				newDetails[newDetails.length-1]= limitModules;
				moduleAttribute.setValue(newDetails);
				return;
			}
		}
		// set as singleton detail:
		moduleAttribute.setValue(new ModuleEncapsulationDetail[] { limitModules });
	}

	private Collection<String> allDefaultSystemModules() {
		if (fAllDefaultSystemModules != null) { // if current project is in the unnamed module
			return fAllDefaultSystemModules;
		}
		if (fAllSystemRoots != null) {
			return Arrays.stream(fAllSystemRoots).map(pfr -> pfr.getModuleDescription().getElementName()).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	Collection<String> reduceNames(Collection<String> names) {
		List<String> reduced= new ArrayList<>();
		outer:
		for (String name : names) {
			if (fModuleList.getModuleKind(name) == ModuleKind.System) {
				List<String> dominators= fModuleRequiredByModules.get(name);
				if (dominators != null) {
					for (String dominator : dominators) {
						if (names.contains(dominator) && fModuleList.getModuleKind(dominator) == ModuleKind.System) {
							continue outer;
						}
					}
				}
				reduced.add(name);
			}
		}
		return reduced;
	}

	Collection<String> closure(Collection<String> selected) {
		HashSet<String> copy= new HashSet<>();
		collectRequired(selected, copy);
		return copy;
	}

	private void collectRequired(Collection<String> src, Set<String> tgt) {
		for (String mod : src) {
			if (tgt.add(mod)) {
				List<String> required= fModule2RequiredModules.get(mod);
				if (required != null) {
					collectRequired(required, tgt);
				}
			}
		}
	}

	/**
	 * Find a module attribute in the current classpath that satisfies the given predicate.
	 * @param predicate this predicate must be fulfilled by any detail of a found module attribte
	 * @return if a predicate match was found the enclosing module attribute will be returned, else {@code null}
	 */
	public CPListElementAttribute findModuleAttribute(Predicate<ModuleEncapsulationDetail> predicate) {
		for (CPListElement element : fClassPathList.getElements()) {
			CPListElementAttribute attribute= element.findAttributeElement(CPListElement.MODULE);
			if (attribute != null && attribute.getValue() instanceof ModuleEncapsulationDetail[]) {
				for (ModuleEncapsulationDetail detail : (ModuleEncapsulationDetail[]) attribute.getValue()) {
					if (predicate.test(detail)) {
						return attribute;
					}
				}
			}
		}
		return null;
	}

	public void showJMPSOptionsDialog() {
		new ShowJPMSOptionsDialog(getShell(), fClassPathList, allDefaultSystemModules(), this::closure, this::reduceNames).open();
	}

	public void offerSwitchToTab(Shell shell, String dialogTitle, String dialogMessage, boolean isLibrary) {
		String tabButton= isLibrary ? NewWizardMessages.ModuleDependenciesAdapter_goToLibrariesTab_button
				: NewWizardMessages.ModuleDependenciesAdapter_goToProjectsTab_button;
		MessageDialog dialog= new MessageDialog(shell,
				dialogTitle, null, dialogMessage, MessageDialog.QUESTION, 0,
				tabButton, IDialogConstants.CANCEL_LABEL);
		if (dialog.open() == 0) {
			if (isLibrary) {
				showLibrariesPage();
			} else {
				showProjectsPage();
			}
		}
	}

	void showLibrariesPage() {
		BuildPathBasePage newTab= switchToTab(LibrariesWorkbookPage.class);
		if (newTab instanceof LibrariesWorkbookPage) {
			((LibrariesWorkbookPage) newTab).selectRootNode(true);
		}
	}

	void showProjectsPage() {
		BuildPathBasePage newTab= switchToTab(ProjectsWorkbookPage.class);
		if (newTab instanceof ProjectsWorkbookPage) {
			((ProjectsWorkbookPage) newTab).selectRootNode(true);
		}
	}
}
