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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;

import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleDependenciesList.ModuleKind;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleDependenciesList.ModulesLabelProvider;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleDialog.ListContentProvider;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;

public class ModuleSelectionDialog extends TrayDialog {

	// widgets:
	private String fTitle;
	private String fMessage;
	private TableViewer fViewer;
	private Button fOkButton;
	private Runnable fFlipMessage; // may show a wait message first, use this to flip to the normal message
	private SelectionButtonDialogField fSelectAllCheckbox;

	boolean fInSetSelection= false; // to avoid re-entrance -> StackOverflow
	boolean fWaitingForSearch= false;

	// internal storage and a client-provided function:
	private Set<String> fAllIncluded; 		// transitive closure over modules already shown
	private List<String> fAvailableModules;	// additional modules outside fAllIncluded
	private Function<List<String>, Set<String>> fClosureComputation;
	private Map<String,IModuleDescription> fModulesByName= new HashMap<>();
	private Map<String,ModuleKind> fKinds= new HashMap<>();

	// result:
	private List<String> fSelectedModules;

	/**
	 * Let the user select among available modules that are not yet included (explicitly or implicitly).
	 * @param shell for showing the dialog
	 * @param javaProject the java project whose build path is being configured
	 * @param jreEntry a classpath entry representing the JRE system library
	 * @param shownModules list of modules already shown in the LHS list ({@link ModuleDependenciesList})
	 * @param closureComputation a function from module names to their full transitive closure over 'requires'.
	 * @return the configured dialog
	 */
	public static ModuleSelectionDialog forSystemModules(Shell shell, IJavaProject javaProject, IClasspathEntry jreEntry, List<String> shownModules, Function<List<String>, Set<String>> closureComputation) {
		return new ModuleSelectionDialog(shell, javaProject, jreEntry, false, shownModules, closureComputation,
				NewWizardMessages.ModuleSelectionDialog_addSystemModules_title, NewWizardMessages.ModuleSelectionDialog_addSystemModules_message);
	}
	/**
	 * Let the user select a module from all modules found in the workspace, except those in {@code irrelevantModules}.
	 * @param shell for showing the dialog
	 * @param javaProject the java project whose build path is being configured
	 * @param jreEntry a classpath entry representing the JRE system library
	 * @param irrelevantModules list of modules not relevant for selection
	 * @return the configured dialog
	 */
	public static ModuleSelectionDialog forReads(Shell shell, IJavaProject javaProject, IClasspathEntry jreEntry, List<String> irrelevantModules) {
		return new ModuleSelectionDialog(shell, javaProject, jreEntry, true, irrelevantModules, HashSet::new,
				NewWizardMessages.ModuleSelectionDialog_selectModule_title, NewWizardMessages.ModuleSelectionDialog_selectReadModule_message);
	}
	private ModuleSelectionDialog(Shell shell, IJavaProject javaProject, IClasspathEntry jreEntry, boolean searchWorkspace,
			List<String> shownModules, Function<List<String>, Set<String>> closureComputation, String title, String message) {
		super(shell);
		fTitle= title;
		fMessage= message;
		fAllIncluded= closureComputation.apply(shownModules);
		fClosureComputation= closureComputation;
		if (jreEntry != null) {  // find system modules from this JRE entry (quick)
			Set<String> result= new HashSet<>();
			for (IPackageFragmentRoot root : javaProject.findUnfilteredPackageFragmentRoots(jreEntry)) {
				checkAddModule(result, root.getModuleDescription());
			}
			List<String> list= new ArrayList<>(result);
			list.sort(String::compareTo);
			fAvailableModules= list;
		}
		if (searchWorkspace) {  // searching all modules in the workspace (slow)
			fWaitingForSearch= true;
			new Job(NewWizardMessages.ModuleSelectionDialog_searchModules_job) {
				@Override
				public IStatus run(IProgressMonitor monitor) {
					try {
						fAvailableModules.addAll(searchAvailableModules(monitor));
						fAvailableModules.sort(String::compareTo);
						if (getReturnCode() == Window.CANCEL) {
							return Status.CANCEL_STATUS;
						}
						shell.getDisplay().asyncExec(() -> {
							if (fFlipMessage != null) {
								fFlipMessage.run();
							}
							fViewer.setInput(fAvailableModules);
							fViewer.refresh();
						});
					} catch (CoreException e) {
						return e.getStatus();
					}
					return Status.OK_STATUS;
				}
			}.schedule();
		}
	}

	private List<String> searchAvailableModules(IProgressMonitor monitor) throws CoreException {
		Set<String> result= new HashSet<>();
		SearchPattern pattern= SearchPattern.createPattern("*", IJavaSearchConstants.MODULE, IJavaSearchConstants.DECLARATIONS, SearchPattern.R_PATTERN_MATCH|SearchPattern.R_CASE_SENSITIVE); //$NON-NLS-1$
		SearchRequestor requestor= new SearchRequestor() {
			@Override
			public void acceptSearchMatch(SearchMatch match) throws CoreException {
				Object element= match.getElement();
				if (element instanceof IModuleDescription) {
					IModuleDescription module= (IModuleDescription) element;
					if (!module.isSystemModule())
						checkAddModule(result, module);
				}
			}
		};
		SearchParticipant[] participants= new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
		IJavaSearchScope scope= JavaSearchScopeFactory.getInstance().createWorkspaceScope(false); // skip JRE modules, which are found directly via the jreEntry
		new SearchEngine().search(pattern, participants, scope, requestor, monitor);
		if (getReturnCode() == Window.CANCEL) { // should cancelPressed() actively abort the search?
			return Collections.emptyList();
		}
		// also search for automatic modules:
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			if (!project.isOpen() || !project.hasNature(JavaCore.NATURE_ID)) continue;
			IJavaProject jPrj= JavaCore.create(project);
			if (jPrj.getModuleDescription() == null) {
				checkAddModule(result, JavaCore.getAutomaticModuleDescription(jPrj));
			}
			for (IPackageFragmentRoot root : jPrj.getAllPackageFragmentRoots()) {
				if (root.isArchive() && root.getModuleDescription() == null) {
					if (!isJREChild(jPrj, root)) { // don't show jars of non-modular JRE as auto modules
						checkAddModule(result, JavaCore.getAutomaticModuleDescription(root));
					}
				}
			}
		}
		return new ArrayList<>(result);
	}

	boolean isJREChild(IJavaProject jPrj, IPackageFragmentRoot root) {
		try {
			IClasspathEntry cpEntry= jPrj.getClasspathEntryFor(root.getPath());
			for (IClasspathEntry rawEntry : jPrj.getRawClasspath()) {
				if (LibrariesWorkbookPage.isJREContainer(rawEntry.getPath())) {
					IClasspathContainer container= JavaCore.getClasspathContainer(rawEntry.getPath(), jPrj);
					if (container != null) {
						for (IClasspathEntry containerEntry : container.getClasspathEntries()) {
							if (containerEntry.equals(cpEntry))
								return true;
						}
					}
					return false;
				}
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return false;
	}

	void checkAddModule(Set<String> result, IModuleDescription moduleDescription) {
		if (moduleDescription == null || moduleDescription.getElementName().isEmpty())
			return;
		if (!fAllIncluded.contains(moduleDescription.getElementName())) {
			if (!result.add(moduleDescription.getElementName()))
				return;
		}
		fModulesByName.put(moduleDescription.getElementName(), moduleDescription); // hold on to module description to be used for getResult()
		fKinds.put(moduleDescription.getElementName(), getKind(moduleDescription));
	}

	private ModuleKind getKind(IModuleDescription moduleDescription) {
		if (moduleDescription.isAutoModule()) {
			return ModuleKind.Automatic;
		}
		IPackageFragmentRoot root= (IPackageFragmentRoot) moduleDescription.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		try {
			IPath path= root.getRawClasspathEntry().getPath();
			if (LibrariesWorkbookPage.isJREContainer(path)) {
				return ModuleKind.System;
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return ModuleKind.Normal;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(fTitle);
	}

	@Override
	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);
		Label message= new Label(composite, SWT.NONE);

		TableViewer tableViewer= new TableViewer(composite, SWT.MULTI | SWT.BORDER);
		tableViewer.setContentProvider(new ListContentProvider());
		tableViewer.setLabelProvider(new ModulesLabelProvider(fKinds::get, s -> false));
		tableViewer.addSelectionChangedListener(this::selectionChanged);

		PixelConverter converter= new PixelConverter(parent);
		GridData gd= new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint= converter.convertWidthInCharsToPixels(50);
		gd.heightHint= converter.convertHeightInCharsToPixels(20);
		tableViewer.getControl().setLayoutData(gd);

		if (fWaitingForSearch) {
			message.setText(NewWizardMessages.ModuleSelectionDialog_searchModules_temp_message);
			fFlipMessage= () ->  {
				message.setText(fMessage);
			};
		} else {
			message.setText(fMessage);
		}
		if (fAvailableModules != null) {
			tableViewer.setInput(fAvailableModules);
		}
		fViewer= tableViewer;

		fSelectAllCheckbox= new SelectionButtonDialogField(SWT.CHECK);
		fSelectAllCheckbox.setLabelText(NewWizardMessages.ModuleSelectionDialog_selectAll_button);
		fSelectAllCheckbox.setSelection(false);
		fSelectAllCheckbox.setDialogFieldListener(field -> selectAll(fSelectAllCheckbox.isSelected()));
		fSelectAllCheckbox.doFillIntoGrid(composite, 2);

		return composite;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		fOkButton= createButton(parent, IDialogConstants.OK_ID,
				NewWizardMessages.ModuleSelectionDialog_add_button, true);
		fOkButton.setEnabled(false); // until elements have been selected
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}
	private void selectAll(boolean selected) {
		if (fInSetSelection) return;
		if (selected) {
			fViewer.setSelection(new StructuredSelection(fAvailableModules));
		} else {
			fViewer.setSelection(StructuredSelection.EMPTY);
		}
	}

	private void selectionChanged(SelectionChangedEvent e) {
		if (fInSetSelection) return;
		fInSetSelection= true;
		try {
			IStructuredSelection selection= e.getStructuredSelection();
			if (selection == null || selection.isEmpty()) {
				fOkButton.setEnabled(false);
				fSelectAllCheckbox.setSelection(false);
				return;
			}
			List<String> selectedNames= selection.toList();
			Set<String> closure= fClosureComputation.apply(selectedNames);
			if (closure.size() > selectedNames.size()) {
				// select all members of the closure:
				fViewer.setSelection(new StructuredSelection(new ArrayList<>(closure)));
			}
			fOkButton.setEnabled(true);
			fSelectedModules= new ArrayList<>(closure); // remember result
			fSelectAllCheckbox.setSelection(closure.containsAll(fAvailableModules));
		} finally {
			fInSetSelection= false;
		}
	}

	public List<IModuleDescription> getResult() {
		return fSelectedModules.stream()
				.filter(m -> !fAllIncluded.contains(m)) // skip modules that are already included
				.map(fModulesByName::get)
				.collect(Collectors.toList());
	}
}
