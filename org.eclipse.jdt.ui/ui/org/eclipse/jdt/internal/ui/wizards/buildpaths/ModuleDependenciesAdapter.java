/*******************************************************************************
 * Copyright (c) 2019, 2020 GK Software SE, and others.
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.ibm.icu.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleDependenciesList.ModuleKind;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleDependenciesPage.DecoratedImageDescriptor;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.ModuleAddExport;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.ModuleAddExpose;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.ModuleAddOpens;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.ModuleAddReads;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.ModulePatch;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ITreeListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.TreeListDialogField;

/**
 * Implementation of the right-hand pane of the {@link ModuleDependenciesPage}.
 */
class ModuleDependenciesAdapter implements IDialogFieldListener, ITreeListAdapter<Object> {

	private static final int IDX_REMOVE= 0;

	private static final int IDX_READ_MODULE= 2;
	private static final int IDX_EXPOSE_PACKAGE= 3;

	private static final int IDX_PATCH= 5;

	private static final int IDX_EDIT= 7;

	private static final int IDX_JPMS_OPTIONS= 9;

	/** Supertype of the two synthetic toplevel tree nodes {@link DeclaredDetails} and {@link ConfiguredDetails} */
	abstract static class Details {

		/** the module selected in the LHS pane, for which details are being shown / edited in this RHS pane. */
		protected final IModuleDescription fFocusModule;
		/** the classpath element by which the current project refers to the focus module. */
		protected final CPListElement fElem;

		public Details(IModuleDescription focusModule, CPListElement elem) {
			fFocusModule= focusModule;
			fElem= elem;
		}

		/**
		 * Answer the module of the current IJavaProject for which the build path is being configured.
		 * @return the module of the current project, or {@code null}.
		 */
		protected IModuleDescription getContextModule() {
			try {
				IModuleDescription moduleDescription= fElem.getJavaProject().getModuleDescription();
				if (moduleDescription != null) {
					return moduleDescription;
				}
			} catch (JavaModelException jme) {
				JavaPlugin.log(jme);
			}
			return null;
		}
		protected String getContextModuleName() {
			try {
				IModuleDescription moduleDescription= fElem.getJavaProject().getModuleDescription();
				if (moduleDescription != null) {
					return moduleDescription.getElementName();
				}
			} catch (JavaModelException jme) {
				JavaPlugin.log(jme);
			}
			return ""; //$NON-NLS-1$
		}

		protected IJavaProject getContextProject() {
			return fElem.getJavaProject();
		}
	}

	/** Synthetic tree node as a parent for details that are declared by the focus module (in its module-info) */
	static class DeclaredDetails extends Details {

		public DeclaredDetails(IModuleDescription mod, CPListElement elem) {
			super(mod, elem);
		}

		public Object[] getChildren() {
			List<Object> children= new ArrayList<>();
			getRequires(children);
			getPackages(children);
			return children.toArray();
		}
		private void getRequires(List<Object> children) {
			if (fFocusModule != null) {
				if (!fFocusModule.isAutoModule()) {
					try {
						for (String required : fFocusModule.getRequiredModuleNames())
							children.add(new ReadModule(required, this));
					} catch (JavaModelException e) {
						JavaPlugin.log(e);
					}
				}
			}
		}
		private void getPackages(List<Object> result) {
			try {
				if (fFocusModule != null) {
					if (fFocusModule.isAutoModule()) {
						IPackageFragmentRoot root= (IPackageFragmentRoot) fFocusModule.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
						if (root != null) {
							// auto module exports all non-empty packages:
							for (IJavaElement child : root.getChildren()) {
								if (child instanceof IPackageFragment
										&& !child.getElementName().isEmpty()
										&& ((IPackageFragment) child).containsJavaResources())
								{
									result.add(new AccessiblePackage(child.getElementName(), AccessiblePackage.Kind.Exports, "", this)); //$NON-NLS-1$
								}
							}
						}
					} else {
						IModuleDescription contextModule= getContextModule();
						String[] exported= fFocusModule.getExportedPackageNames(contextModule);
						for (String export : exported) {
							result.add(new AccessiblePackage(export, AccessiblePackage.Kind.Exports, "", this)); //$NON-NLS-1$
						}
						String[] opened= fFocusModule.getOpenedPackageNames(contextModule);
						for (String open : opened) {
							result.add(new AccessiblePackage(open, AccessiblePackage.Kind.Opens, "", this)); //$NON-NLS-1$
						}
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
	}

	/** Synthetic tree node. */
	static class ConfiguredDetails extends Details {
		private ModuleKind fKind;
		private ModuleDependenciesPage fDependenciesPage;

		public ConfiguredDetails(IModuleDescription focusModule, CPListElement elem, ModuleKind moduleKind, ModuleDependenciesPage dependenciesPage) {
			super(focusModule, elem);
			fKind= moduleKind;
			fDependenciesPage= dependenciesPage;
		}

		public Object[] getChildren() {
			// For containers like JRE / Libraries: aggregate attribute is in the parent
			Object parent= fElem.getParentContainer();
			if (parent instanceof CPListElement) {
				Object attribute= ((CPListElement) parent).getAttribute(CPListElement.MODULE);
				if (attribute instanceof ModuleEncapsulationDetail[]) {
					return convertEncapsulationDetails((ModuleEncapsulationDetail[]) attribute, fFocusModule.getElementName());
				}
			}
			Object attribute= fElem.getAttribute(CPListElement.MODULE);
			if (attribute instanceof ModuleEncapsulationDetail[]) {
				return convertEncapsulationDetails((ModuleEncapsulationDetail[]) attribute, null);
			}
			if (fKind == ModuleKind.Focus) {
				return new Object[0];
			}
			return fElem.getChildren(true);
		}

		private DetailNode<?>[] convertEncapsulationDetails(ModuleEncapsulationDetail[] attribute, String filterModule) {
			List<DetailNode<?>> filteredDetails= new ArrayList<>();
			for (ModuleEncapsulationDetail detail : attribute) {
				if (detail instanceof ModuleAddExpose) {
					ModuleAddExpose moduleAddExpose= (ModuleAddExpose) detail;
					if (filterModule == null || filterModule.equals(moduleAddExpose.fSourceModule)) {
						AccessiblePackage.Kind kind= moduleAddExpose instanceof ModuleAddExport ? AccessiblePackage.Kind.Exports : AccessiblePackage.Kind.Opens;
						filteredDetails.add(new AccessiblePackage(moduleAddExpose.fPackage, kind, moduleAddExpose.fTargetModules, this));
					}
				} else if (detail instanceof ModuleAddReads) {
					ModuleAddReads moduleAddReads= (ModuleAddReads) detail;
					if (filterModule == null || filterModule.equals(moduleAddReads.fSourceModule)) {
						filteredDetails.add(new ReadModule(moduleAddReads.fTargetModule, this));
					}
				} else if (detail instanceof ModulePatch) {
					ModulePatch modulePatch= (ModulePatch) detail;
					if (filterModule == null || filterModule.equals(modulePatch.fModule)) {
						if (modulePatch.fPaths != null) {
							for (String path : modulePatch.getPathArray()) {
								Path iPath= new Path(path);
								if (iPath.segmentCount() == 1) { // is a project
									IProject project= ResourcesPlugin.getWorkspace().getRoot().getProject(iPath.segment(0));
									filteredDetails.add(new PatchModule(JavaCore.create(project), this));
								} else {
									IFolder folder= ResourcesPlugin.getWorkspace().getRoot().getFolder(iPath);
									IPackageFragmentRoot elem= JavaCore.create(folder.getProject()).getPackageFragmentRoot(folder);
									if (elem != null) {
										filteredDetails.add(new PatchModule(elem, this));
									}
								}
							}
						} else {
							// compatibility with old format not defining paths:
							filteredDetails.add(new PatchModule(fElem.getJavaProject(), this));
						}
					}
				}
			}
			return filteredDetails.toArray(new DetailNode<?>[filteredDetails.size()]);
		}
		public void removeAll() {
			if (fKind == ModuleKind.System) {
				// aggregate attribute is in the parent (corresponding to the JRE)
				Object parent= fElem.getParentContainer();
				if (parent instanceof CPListElement) {
					CPListElement jreElement= (CPListElement) parent;
					Object attribute= jreElement.getAttribute(CPListElement.MODULE);
					if (attribute instanceof ModuleEncapsulationDetail[]) {
						// need to filter so we remove only affected details:
						ModuleEncapsulationDetail[] filtered= Arrays.stream((ModuleEncapsulationDetail[]) attribute)
									.filter(d -> !d.affects(fFocusModule.getElementName()))
									.toArray(ModuleEncapsulationDetail[]::new);
						jreElement.setAttribute(CPListElement.MODULE, filtered);
						return;
					}
				}
			}
			Object attribute= fElem.getAttribute(CPListElement.MODULE);
			if (attribute instanceof ModuleEncapsulationDetail[]) {
				fElem.setAttribute(CPListElement.MODULE, new ModuleEncapsulationDetail[0]);
			}
		}
		public boolean addOrEditAccessiblePackage(AccessiblePackage selectedPackage, Shell shell) {
			Object container= fElem.getParentContainer();
			CPListElement element= (container instanceof CPListElement) ? (CPListElement) container : fElem;
			CPListElementAttribute moduleAttribute= element.findAttributeElement(CPListElement.MODULE);

			IJavaElement jContainer= fFocusModule.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
			if (jContainer == null) {
				jContainer= fFocusModule.getJavaProject();
			}
			String packageName= selectedPackage != null ? selectedPackage.getName() : ""; //$NON-NLS-1$

			ModuleAddExpose initial;
			try {
				initial= (selectedPackage != null)
						? selectedPackage.convertToCP(moduleAttribute)
						: new ModuleAddExport(fFocusModule.getElementName(), packageName, getContextModuleName(), moduleAttribute);
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
				MessageDialog.openError(shell, NewWizardMessages.ModuleDependenciesAdapter_configure_error, e.getMessage());
				return false;
			}

			Set<String> possibleTargetModules= new HashSet<>(fDependenciesPage.getAllModules());
			possibleTargetModules.remove(fFocusModule.getElementName());
			Object moduleAttributes= fElem.getAttribute(CPListElement.MODULE);
			Set<String> alreadyExportedPackages= new HashSet<>();
			if (moduleAttributes instanceof ModuleEncapsulationDetail[]) {
				ModuleEncapsulationDetail encapDetails[]= (ModuleEncapsulationDetail[]) moduleAttributes;
				for (ModuleEncapsulationDetail moduleEncapsulationDetail : encapDetails) {
					if (moduleEncapsulationDetail instanceof ModuleAddExport) {
						alreadyExportedPackages.add(((ModuleAddExport) moduleEncapsulationDetail).fPackage);
					}
				}
			}
			ModuleAddExportsDialog dialog= new ModuleAddExportsDialog(shell, new IJavaElement[] { jContainer }, possibleTargetModules, initial, alreadyExportedPackages);
			if (dialog.open() == Window.OK) {
				try {
					moduleAttribute = ensureModuleAttribute(moduleAttribute);
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
					return false;
				}
				ModuleAddExpose expose= dialog.getExport(moduleAttribute);
				if (expose != null) {
					Object attribute= moduleAttribute.getValue();
					ModuleEncapsulationDetail[] arrayValue= null;
					if (attribute instanceof ModuleEncapsulationDetail[]) {
						arrayValue= (ModuleEncapsulationDetail[]) attribute;
						if (selectedPackage != null) {
							// editing: replace existing entry
							for (int i= 0; i < arrayValue.length; i++) {
								ModuleEncapsulationDetail detail= arrayValue[i];
								if (detail.equals(initial)) {
									arrayValue[i]= expose;
									break;
								}
							}
						} else {
							arrayValue= Arrays.copyOf(arrayValue, arrayValue.length+1);
							arrayValue[arrayValue.length-1]= expose;
						}
					} else {
						arrayValue= new ModuleEncapsulationDetail[] { expose };
					}
					element.setAttribute(CPListElement.MODULE, arrayValue);
					return true;
				}
			}
			return false;
		}

		public boolean remove(List<Object> selectedElements) {
			try {
				return internalRemove(selectedElements);
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
				MessageDialog.openError(fDependenciesPage.getShell(), NewWizardMessages.ModuleDependenciesAdapter_configure_error, e.getMessage());
				return false;
			}
		}
		private boolean internalRemove(List<Object> selectedElements) throws JavaModelException {
			CPListElementAttribute moduleAttribute;
			Object parentContainer= fElem.getParentContainer();
			if (parentContainer instanceof CPListElement) {
				moduleAttribute= ((CPListElement) parentContainer).findAttributeElement(CPListElement.MODULE);
			} else {
				moduleAttribute= fElem.findAttributeElement(CPListElement.MODULE);
			}
			if (moduleAttribute == null) {
				throwNewJavaModelException("module attribute is unexpectecly missing for : "+fElem); //$NON-NLS-1$
				return false; // not reached
			}
			Object value= moduleAttribute.getValue();
			if (!(value instanceof ModuleEncapsulationDetail[])) {
				throwNewJavaModelException("Value of module attribute has unexpected type: "+value); //$NON-NLS-1$
			}
			List<ModuleEncapsulationDetail> details= new ArrayList<>(Arrays.asList((ModuleEncapsulationDetail[]) value));
			boolean patchUpdated= false;
			for (Object node : selectedElements) {
				if (node instanceof DetailNode<?>) {
					if (node instanceof PatchModule) {
						// need to merge details:
						PatchModule patch= (PatchModule) node;
						ModuleEncapsulationDetail.removePatchLocation(details, fFocusModule.getElementName(), patch.getPath());
						if (getContextProject().equals(patch.fSource.getAncestor(IJavaElement.JAVA_PROJECT))) {
							fDependenciesPage.unsetFocusModule(fElem);
						}
						patchUpdated= true;
					} else {
						ModuleEncapsulationDetail med= ((DetailNode<?>) node).convertToCP(moduleAttribute);
						if (med != null) {
							if (!details.remove(med)) {
								throwNewJavaModelException("Detail "+med+" was not removed");  //$NON-NLS-1$//$NON-NLS-2$
							}
						}
					}
				} else if (node instanceof TargetModule) {
					TargetModule targetModule= (TargetModule) node;
					AccessiblePackage parent= targetModule.fParent;
					parent.removeTargetModule(targetModule.fName);
					for (int i=0; i<details.size(); i++) {
						ModuleEncapsulationDetail detail= details.get(i);
						if (parent.matches(detail)) {
							details.set(i, parent.convertToCP(moduleAttribute));
							break;
						}
					}
				} else if (node instanceof ConfiguredDetails) {
					((ConfiguredDetails) node).removeAll();
					return true; // covers all details, changes in 'details' are irrelevant
				}
			}
			moduleAttribute.setValue(details.toArray(new ModuleEncapsulationDetail[details.size()]));
			if (patchUpdated) {
				fDependenciesPage.buildPatchMap();
			}
			return true;
		}

		public boolean addReads(Shell shell) {
			try {
				return internalAddReads(shell);
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
				MessageDialog.openError(fDependenciesPage.getShell(), NewWizardMessages.ModuleDependenciesAdapter_configure_error, e.getMessage());
				return false;
			}
		}
		boolean internalAddReads(Shell shell) throws JavaModelException {
			Object container= fElem.getParentContainer();
			CPListElement element= (container instanceof CPListElement) ? (CPListElement) container : fElem;
			CPListElementAttribute moduleAttribute= element.findAttributeElement(CPListElement.MODULE);
			if (moduleAttribute == null && fKind != ModuleKind.Focus) {
				throwNewJavaModelException("module attribute is unexpectecly missing for : "+fElem); //$NON-NLS-1$
				return false; // not reached
			}

			List<String> irrelevantModules;
			try {
				irrelevantModules= Arrays.asList(fFocusModule.getRequiredModuleNames());
			} catch (JavaModelException e) {
				JavaPlugin.log(e); // not fatal
				irrelevantModules= Collections.emptyList();
			}
			irrelevantModules= new ArrayList<>(irrelevantModules);
			irrelevantModules.add(fFocusModule.getElementName());

			IClasspathEntry jreEntry= fDependenciesPage.findSystemLibraryElement().getClasspathEntry();
			//Get addread from fElem and add to irrelevantModules, dont want to re-add it
			Object moduleAttributes= fElem.getAttribute(CPListElement.MODULE);
			if (moduleAttributes instanceof ModuleEncapsulationDetail[]) {
				ModuleEncapsulationDetail encapDetails[]= (ModuleEncapsulationDetail[]) moduleAttributes;
				for (ModuleEncapsulationDetail moduleEncapsulationDetail : encapDetails) {
					if (moduleEncapsulationDetail instanceof ModuleAddReads) {
						String fTargetModule= ((ModuleAddReads) moduleEncapsulationDetail).fTargetModule;
						irrelevantModules.add(fTargetModule);
					}
				}
			}
			ModuleSelectionDialog dialog= ModuleSelectionDialog.forReads(shell, fElem.getJavaProject(), jreEntry, irrelevantModules);
			if (dialog.open() != 0) {
				return false;
			}
			List<IModuleDescription> result= dialog.getResult();
			if (!handleUnavailableModulesIfNeeded(shell, result, fDependenciesPage)) {
				return false;
			}

			moduleAttribute= ensureModuleAttribute(moduleAttribute);

			ModuleEncapsulationDetail[] arrayValue= null;
			int idx= 0;
			Object attribute= moduleAttribute.getValue();
			if (attribute instanceof ModuleEncapsulationDetail[]) {
				arrayValue= (ModuleEncapsulationDetail[]) attribute;
				idx= arrayValue.length;
				arrayValue= Arrays.copyOf(arrayValue, arrayValue.length+result.size());
			} else {
				arrayValue= new ModuleEncapsulationDetail[result.size()];
			}
			for (IModuleDescription module : result) {
				arrayValue[idx++]= new ModuleAddReads(fFocusModule.getElementName(), module.getElementName(), moduleAttribute);
			}
			element.setAttribute(CPListElement.MODULE, arrayValue);
			return true;
		}

		public boolean addPatch(Shell shell, Map<String, String> patchMap) {
			try {
				return internalAddPatch(shell, patchMap);
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
				MessageDialog.openError(fDependenciesPage.getShell(), NewWizardMessages.ModuleDependenciesAdapter_configure_error, e.getMessage());
				return false;
			}
		}
		private boolean internalAddPatch(Shell shell, Map<String, String> patchMap) throws JavaModelException {
			Object container= fElem.getParentContainer();
			CPListElement element= (container instanceof CPListElement) ? (CPListElement) container : fElem;
			CPListElementAttribute moduleAttribute= ensureModuleAttribute(element.findAttributeElement(CPListElement.MODULE));
			if (!(moduleAttribute.getValue() instanceof ModuleEncapsulationDetail[])) {
				throwNewJavaModelException("Value of module attribute has unexpected type: "+moduleAttribute.getValue()); //$NON-NLS-1$
				return false;
			}

			IJavaProject contextProject= getContextProject();
			ModulePatchSourceSelectionDialog dialog= new ModulePatchSourceSelectionDialog(shell, fFocusModule, contextProject);
			if (dialog.open() != 0) {
				return false;
			}
			List<IJavaElement> result= dialog.getResult();
			for (IJavaElement source : result) {
				IPath sourcePath= source.getPath();
				String sourcePathString= sourcePath.toString();
				if (patchMap.containsKey(sourcePathString)) {
					// direct conflict
					if (!unpatchModule(shell, patchMap.get(sourcePathString), sourcePathString, sourcePathString)) {
						return false;
					}
				} else {
					if (sourcePath.segmentCount() > 1) {
						String sourcePathString2= sourcePath.removeLastSegments(sourcePath.segmentCount()-1).toString();
						if (patchMap.containsKey(sourcePathString2)) {
							// conflict project (exist) vs. folder (new)
							if (!unpatchModule(shell, patchMap.get(sourcePathString2), sourcePathString2, sourcePathString)) {
								return false;
							}
						}
					} else {
						String pattern= sourcePathString+'/';
						for (Entry<String, String> entry : patchMap.entrySet()) {
							if (entry.getKey().startsWith(pattern)) {
								// conflict folder (exist) vs. project (new)
								if (!unpatchModule(shell, entry.getValue(), entry.getKey(), sourcePathString)) {
									return false;
								}
								break;
							}
						}
					}
				}
			}
			String newPaths= result.stream()
					.map(je -> je.getPath().toString())
					.collect(Collectors.joining(File.pathSeparator));

			Object attribute= moduleAttribute.getValue();
			List<ModuleEncapsulationDetail> detailList= new ArrayList<>(Arrays.asList((ModuleEncapsulationDetail[]) attribute));
			ModuleEncapsulationDetail.addPatchLocations(detailList, fFocusModule.getElementName(), newPaths, moduleAttribute);
			element.setAttribute(CPListElement.MODULE, detailList.toArray(new ModuleEncapsulationDetail[detailList.size()]));
			fDependenciesPage.buildPatchMap();
			fDependenciesPage.refreshModulesList();
			return true;
		}

		private boolean unpatchModule(Shell shell, String moduleToUnpatch, String oldPath, String newPath) {
			String pathKind= oldPath.indexOf('/', 1) != -1
					? NewWizardMessages.ModuleDependenciesAdapter_sourceFolder_kind : NewWizardMessages.ModuleDependenciesAdapter_project_kind;
			if (!MessageDialog.openQuestion(shell, NewWizardMessages.ModuleDependenciesAdapter_patchConflict_title,
					MessageFormat.format(NewWizardMessages.ModuleDependenciesAdapter_patchConflict_message,
							pathKind, oldPath.substring(1), moduleToUnpatch, newPath.substring(1), fFocusModule.getElementName()))) {
				return false;
			}

			CPListElementAttribute otherAttribute= fDependenciesPage.findModuleAttribute(
					d -> d instanceof ModulePatch && ((ModulePatch) d).affects(moduleToUnpatch));
			List<ModuleEncapsulationDetail> otherDetailList= new ArrayList<>(Arrays.asList((ModuleEncapsulationDetail[]) otherAttribute.getValue()));
			ModuleEncapsulationDetail.removePatchLocation(otherDetailList, moduleToUnpatch, oldPath);
			otherAttribute.setValue(otherDetailList.toArray(new ModuleEncapsulationDetail[otherDetailList.size()]));
			return true;
		}

		private CPListElementAttribute ensureModuleAttribute(CPListElementAttribute existing) throws JavaModelException {
			if (existing != null) return existing;
			if (fElem.getClasspathEntry().getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				// initialize missing attribute for focus module (source folder)
				return fElem.createAttributeElement(CPListElement.MODULE, new ModuleEncapsulationDetail[0], true);
			} else {
				throwNewJavaModelException("module attribute is unexpectecly missing for : "+fElem); //$NON-NLS-1$
				return null; // not reached
			}
		}
	}

	abstract static class DetailNode<D extends ModuleEncapsulationDetail> {
		protected String fName;
		protected Details fParent;

		protected DetailNode(Details parent) {
			fParent= parent;
		}
		protected abstract int rank();
		public String getName() {
			return fName;
		}
		public boolean isIsConfigured() {
			return fParent instanceof ConfiguredDetails;
		}
		public abstract D convertToCP(CPListElementAttribute attribElem) throws JavaModelException;
	}

	/**
	 * Declare that the package denoted by {@link #fName} is accessible (exported/opened) to the current context module,
	 * or a list of target modules (given as comma separated string).
	 */
	static class AccessiblePackage extends DetailNode<ModuleAddExpose> {
		// separator for target modules
		private static final String COMMA= ","; //$NON-NLS-1$

		enum Kind { Exports, Opens;
			ImageDescriptor getDecoration() {
				switch (this) {
					case Exports: return JavaPluginImages.DESC_OVR_EXPORTS;
					case Opens: return JavaPluginImages.DESC_OVR_OPENS;
					default: return null;
				}
			}
		}
		private Kind fKind;
		private String fTargetModules;

		public AccessiblePackage(String name, Kind kind, String targetModules, Details parent) {
			super(parent);
			fName= name;
			fKind= kind;
			fTargetModules= targetModules;
		}
		@Override
		protected int rank() {
			return 2;
		}
		public Kind getKind() {
			return fKind;
		}
		public boolean matches(ModuleEncapsulationDetail detail) {
			// match ignoring target modules:
			if (!detail.affects(fParent.fFocusModule.getElementName())) {
				return false;
			}
			switch (fKind) {
				case Exports:
					if (!(detail instanceof ModuleAddExport)) return false;
					break;
				case Opens:
					if (!(detail instanceof ModuleAddOpens)) return false;
					break;
				default: return false;
			}
			ModuleAddExpose expose= (ModuleAddExpose) detail;
			return expose.fPackage.equals(fName);
		}
		public Object[] getTargetModules() {
			if (fTargetModules != null && !fTargetModules.isEmpty()) {
				String[] targets= fTargetModules.split(COMMA);
				Object[] result= new Object[targets.length];
				for (int i= 0; i < targets.length; i++) {
					result[i]= new TargetModule(this, targets[i]);
				}
				return result;
			}
			return null;
		}
		public boolean removeTargetModule(String module) {
			if (fTargetModules == null) {
				return false;
			}
			List<String> targets= new ArrayList<>(Arrays.asList(fTargetModules.split(COMMA)));
			if (targets.remove(module)) {
				fTargetModules= String.join(COMMA, targets);
			}
			return false;
		}
		@Override
		public ModuleAddExpose convertToCP(CPListElementAttribute attribElem) throws JavaModelException {
			if (fParent instanceof ConfiguredDetails) {
				if (fTargetModules != null) {
					switch (fKind) {
						case Exports:
							return new ModuleAddExport(fParent.fFocusModule.getElementName(), fName, fTargetModules, attribElem);
						case Opens:
							return new ModuleAddOpens(fParent.fFocusModule.getElementName(), fName, fTargetModules, attribElem);
						default:
							break;
					}
				}
			}
			throwNewJavaModelException("Failed to convert attribute "+attribElem+" with value "+attribElem.getValue());  //$NON-NLS-1$//$NON-NLS-2$
			return null; // never reached
		}
	}

	/** children of AccessiblePackage to denote a referenced target module. */
	static class TargetModule {
		final AccessiblePackage fParent;
		final String fName;

		public TargetModule(AccessiblePackage parent, String name) {
			fParent= parent;
			fName= name;
		}

		public String getText() {
			return "to "+fName; //$NON-NLS-1$ (don't translate this keyword)
		}
	}

	/** Declare that the module given by {@link #fName} is read by the selected focus module. */
	static class ReadModule extends DetailNode<ModuleAddReads> {

		public ReadModule(String targetModule, Details parent) {
			super(parent);
			fName= targetModule;
		}
		@Override
		protected int rank() {
			return 0;
		}
		@Override
		public ModuleAddReads convertToCP(CPListElementAttribute attribElem) throws JavaModelException {
			if (fParent instanceof ConfiguredDetails) {
				return new ModuleAddReads(fParent.fFocusModule.getElementName(), fName, attribElem);
			}
			throw new JavaModelException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), "Failed to convert attribute "+attribElem+" with value "+attribElem.getValue()));  //$NON-NLS-1$//$NON-NLS-2$
		}
	}
	/** Declare that the selected focus module is patched by the content of the given java element
	 * (should be either an {@link IJavaProject} or an {@link IPackageFragmentRoot}). */
	static class PatchModule extends DetailNode<ModulePatch> {
		IJavaElement fSource;

		public PatchModule(IJavaElement source, Details parent) {
			super(parent);
			fSource= source;
			fName= source.getPath().makeRelative().toString();
		}
		@Override
		protected int rank() {
			return 1;
		}
		public String getPath() {
			return fSource.getPath().toString();
		}

		@Override
		public ModulePatch convertToCP(CPListElementAttribute attribElem) {
			return new ModulePatch(fParent.fFocusModule.getElementName(), getPath(), attribElem);
		}
	}

	static class ModularityDetailsLabelProvider extends CPListLabelProvider {
		private ImageDescriptorRegistry fRegistry= JavaPlugin.getImageDescriptorRegistry();
		private JavaElementImageProvider fImageLabelProvider= new JavaElementImageProvider();
		@Override
		public String getText(Object element) {
			if (element instanceof DeclaredDetails) {
				return MessageFormat.format(NewWizardMessages.ModuleDependenciesAdapter_declared_node, ((DeclaredDetails) element).fFocusModule.getElementName());
			}
			if (element instanceof ConfiguredDetails) {
				return NewWizardMessages.ModuleDependenciesAdapter_configured_node;
			}
			if (element instanceof DetailNode) {
				return ((DetailNode<?>) element).getName();
			}
			if (element instanceof TargetModule) {
				return ((TargetModule) element).getText();
			}
			return super.getText(element);
		}
		@Override
		public Image getImage(Object element) {
			if (element instanceof DeclaredDetails || element instanceof TargetModule) {
				return fRegistry.get(JavaPluginImages.DESC_OBJS_MODULE);
			}
			if (element instanceof ConfiguredDetails) {
				return fRegistry.get(JavaPluginImages.DESC_OBJS_MODULE_ATTRIB);
			}
			if (element instanceof AccessiblePackage) {
				AccessiblePackage.Kind kind= ((AccessiblePackage) element).getKind();
				ImageDescriptor imgDesc= new DecoratedImageDescriptor(JavaPluginImages.DESC_OBJS_PACKAGE, kind.getDecoration(), true);
				return JavaPlugin.getImageDescriptorRegistry().get(imgDesc);
			}
			if (element instanceof ReadModule) {
				ImageDescriptor imgDesc= new DecoratedImageDescriptor(JavaPluginImages.DESC_OBJS_MODULE,
												JavaPluginImages.DESC_OVR_READS, true);
				return JavaPlugin.getImageDescriptorRegistry().get(imgDesc);
			}
			if (element instanceof PatchModule) {
				ImageDescriptor baseImg= fImageLabelProvider.getBaseImageDescriptor(((PatchModule) element).fSource, 0);
				ImageDescriptor decoratedImgDesc= new DecoratedImageDescriptor(baseImg, JavaPluginImages.DESC_OVR_PATCH, true);
				return JavaPlugin.getImageDescriptorRegistry().get(decoratedImgDesc);
			}
			return super.getImage(element);
		}
	}

	static class ElementSorter extends CPListElementSorter {
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			// sorting for root nodes: DeclaredDetails > ConfiguredDetails
			if (e1 instanceof DeclaredDetails) {
				return e2 instanceof ConfiguredDetails ? -1 : 1;
			}
			if (e1 instanceof ConfiguredDetails) {
				return e2 instanceof DeclaredDetails ? 1 : -1;
			}
			if (e1 instanceof DetailNode<?> && e2 instanceof DetailNode<?>) {
				int rank1= ((DetailNode<?>) e1).rank();
				int rank2= ((DetailNode<?>) e2).rank();
				if (rank1 < rank2) return -1;
				if (rank1 > rank2) return 1;
			}
			return super.compare(viewer, e1, e2);
		}
	}

	public static void updateButtonEnablement(TreeListDialogField<?> list, boolean enableModify, boolean enableRemove, boolean enableShow, boolean enableAddExport) {
		list.enableButton(IDX_REMOVE, enableRemove);
		list.enableButton(IDX_EXPOSE_PACKAGE, enableModify && enableAddExport);
		list.enableButton(IDX_READ_MODULE, enableModify);
		list.enableButton(IDX_PATCH, enableModify);
		list.enableButton(IDX_JPMS_OPTIONS, enableShow);
	}

	static void throwNewJavaModelException(String message) throws JavaModelException {
		throw new JavaModelException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), message));
	}

	static boolean handleUnavailableModulesIfNeeded(Shell shell, List<IModuleDescription> result, ModuleDependenciesPage masterPage) {
		List<IModuleDescription> unavailableSystemModules= new ArrayList<>();
		Collection<String> allModules= masterPage.getAllModules();
		for (IModuleDescription module : result) {
			if (!allModules.contains(module.getElementName())) {
				if (module.isSystemModule()) {
					unavailableSystemModules.add(module);
				} else {
					IPackageFragmentRoot pfr= (IPackageFragmentRoot) module.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
					boolean isLibrary= pfr != null && pfr.isArchive();
					masterPage.offerSwitchToTab(shell,
							NewWizardMessages.ModuleSelectionDialog_selectModule_title,
							MessageFormat.format(NewWizardMessages.ModuleDependenciesAdapter_addReadsNotOnModulepath_error, module.getElementName()),
							isLibrary);
					return false; // the new add-reads is not yet handled
				}
			}
		}
		if (!unavailableSystemModules.isEmpty()) {
			StringBuilder message= new StringBuilder();
			message.append(NewWizardMessages.ModuleDependenciesAdapter_addSystemModules_question);
			message.append(unavailableSystemModules.stream()
					.map(IJavaElement::getElementName)
					.sorted()
					.collect(Collectors.joining("\n\t", "\t", ""))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			int answer= MessageDialog.open(MessageDialog.QUESTION, shell,
					NewWizardMessages.ModuleDependenciesAdapter_addSystemModule_title, message.toString(), SWT.NONE,
					NewWizardMessages.ModuleDependenciesAdapter_add_button, IDialogConstants.CANCEL_LABEL);
			if (answer == 0) {
				try {
					masterPage.addToSystemModules(unavailableSystemModules);
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
					MessageDialog.openError(shell, NewWizardMessages.ModuleDependenciesAdapter_configure_error, e.getMessage());
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}

	private final ModuleDependenciesPage fModuleDependenciesPage; // parent structure
	private TreeListDialogField<Object> fDetailsList; // RHS widget managed by this class

	public ModuleDependenciesAdapter(ModuleDependenciesPage moduleDependenciesPage) {
		fModuleDependenciesPage= moduleDependenciesPage;
	}

	public void setList(TreeListDialogField<Object> detailsList) {
		fDetailsList= detailsList;
		fDetailsList.enableButton(IDX_REMOVE, false);
		fDetailsList.enableButton(IDX_EXPOSE_PACKAGE, false);
		fDetailsList.enableButton(IDX_READ_MODULE, false);
		fDetailsList.enableButton(IDX_PATCH, false);
		fDetailsList.enableButton(IDX_EDIT, false);
	}

	// -------- IListAdapter --------
	@Override
	public void customButtonPressed(TreeListDialogField<Object> field, int index) {
		AccessiblePackage selectedPackage= null;
		List<Object> selectedElements= field.getSelectedElements();
		int numDetails= countConfiguredDetails();
		switch (index) {
			case IDX_REMOVE:
				if (selectedElements.isEmpty()) {
					// no detail selected, remove the module(s) (with question):
					fModuleDependenciesPage.removeModules();
				} else {
					if (getConfiguredDetails(true).remove(selectedElements)) {
						field.refresh();
					}
				}
				validate();
				break;
			case IDX_EDIT:
				if (selectedElements.size() == 1 && selectedElements.get(0) instanceof AccessiblePackage) {
					selectedPackage= (AccessiblePackage) selectedElements.get(0);
				} else {
					break;
				}
				//$FALL-THROUGH$
			case IDX_EXPOSE_PACKAGE:
				if (getConfiguredDetails(true).addOrEditAccessiblePackage(selectedPackage, fModuleDependenciesPage.getShell())) {
					field.refresh();
				}
				break;
			case IDX_READ_MODULE:
				if (getConfiguredDetails(true).addReads(fModuleDependenciesPage.getShell())) {
					field.refresh();
				}
				break;
			case IDX_PATCH:
				if (getConfiguredDetails(true).addPatch(fModuleDependenciesPage.getShell(), fModuleDependenciesPage.fPatchMap)) {
					field.refresh();
					validate();
				}
				break;
			case IDX_JPMS_OPTIONS:
				fModuleDependenciesPage.showJMPSOptionsDialog();
				break;
			default:
				throw new IllegalArgumentException("Non-existent button index "+index); //$NON-NLS-1$
		}
		int newNum= countConfiguredDetails();
		if ((numDetails == 0 || newNum == 0) && numDetails != newNum) {
			fModuleDependenciesPage.refreshModulesList(); // let ModuleDependenciesList react to changes in hasConfiguredDetails()
		}
	}

	int countConfiguredDetails() {
		for (Object object : fDetailsList.getElements()) {
			if (object instanceof ConfiguredDetails)
				return ((ConfiguredDetails) object).getChildren().length;
		}
		return 0;
	}

	private ConfiguredDetails getConfiguredDetails(boolean failIfMissing) {
		for (Object object : fDetailsList.getElements()) {
			if (object instanceof ConfiguredDetails)
				return (ConfiguredDetails) object;
		}
		if (failIfMissing)
			throw new IllegalStateException("detail list has no ConfiguredDetails element"); //$NON-NLS-1$
		return null;
	}

	@Override
	public void selectionChanged(TreeListDialogField<Object> field) {
		List<Object> selected= fDetailsList.getSelectedElements();
		boolean enable= false;
		boolean disableAddExport= false;
		if (selected.size() == 1) {
			Object selectedNode= selected.get(0);
			enable= isConfigurableNode(selectedNode);
			fDetailsList.enableButton(IDX_EDIT, enable && selectedNode instanceof AccessiblePackage);
		} else {
			enable= !fDetailsList.getElements().isEmpty() && allAreConfigurable(selected);
			fDetailsList.enableButton(IDX_EDIT, false);
		}
		ConfiguredDetails configuredDetails= getConfiguredDetails(false);
		if (enable && configuredDetails != null && configuredDetails.fKind == ModuleKind.System) {
			IJavaProject javaProject= configuredDetails.fFocusModule.getJavaProject();
			disableAddExport= JavaCore.ENABLED.equals(javaProject.getOption(JavaCore.COMPILER_RELEASE, false));
		}
		fDetailsList.enableButton(IDX_EXPOSE_PACKAGE, enable && !disableAddExport);
		fDetailsList.enableButton(IDX_READ_MODULE, enable);
		fDetailsList.enableButton(IDX_PATCH, enable);
		if (enable) {
			enable &= selected.size() > 0;
			for (Object sel : selected) {
				if (sel instanceof ConfiguredDetails) {
					enable= false;
					break;
				}
			}
		}
		fDetailsList.enableButton(IDX_REMOVE, enable);
	}

	private boolean allAreConfigurable(List<Object> selected) {
		for (Object node : selected) {
			if (!isConfigurableNode(node))
				return false;
		}
		return true;
	}

	private boolean isConfigurableNode(Object node) {
		if (node instanceof ConfiguredDetails) {
			return true;
		}
		if (node instanceof DeclaredDetails) {
			return false;
		}
		if (node instanceof DetailNode) {
			return ((DetailNode<?>) node).isIsConfigured();
		}
		if (node instanceof TargetModule)  {
			return true;
		}
		return false; // no other nodes
	}

	@Override
	public void doubleClicked(TreeListDialogField<Object> field) {
		List<Object> selectedElements= fDetailsList.getSelectedElements();
		if (selectedElements.size() == 1) {
			Object selected= selectedElements.get(0);
			if (selected instanceof ReadModule) {
				String moduleName= ((ReadModule) selected).getName();
				fModuleDependenciesPage.setSelectionToModule(moduleName);
			} else {
				TreeViewer treeViewer= fDetailsList.getTreeViewer();
				boolean isExpanded= treeViewer.getExpandedState(selectedElements.get(0));
				if (isExpanded) {
					treeViewer.collapseToLevel(selectedElements.get(0), 1);
				} else {
					treeViewer.expandToLevel(selectedElements.get(0), 1);
				}
				if (isConfigurableNode(selected)) {
					if (selected instanceof AccessiblePackage) {
						if (getConfiguredDetails(true).addOrEditAccessiblePackage((AccessiblePackage) selected, fModuleDependenciesPage.getShell())) {
							field.refresh();
						}
					}
				}
			}
		}
	}

	@Override
	public void keyPressed(TreeListDialogField<Object> field, KeyEvent event) {
		if (field == fDetailsList) {
			if (event.character == SWT.DEL && event.stateMask == 0 && fDetailsList.getButton(IDX_REMOVE).isEnabled()) {
				List<Object> selectedElements= field.getSelectedElements();
				if (getConfiguredDetails(true).remove(selectedElements)) {
					field.refresh();
					validate();
				}
			}
		}
	}

	@Override
	public Object[] getChildren(TreeListDialogField<Object> field, Object element) {
		if (element instanceof DeclaredDetails) {
			return ((DeclaredDetails) element).getChildren();
		} else if (element instanceof ConfiguredDetails) {
			return ((ConfiguredDetails) element).getChildren();
		} else if (element instanceof AccessiblePackage) {
			return ((AccessiblePackage) element).getTargetModules();
		}
		return new Object[0];
	}

	@Override
	public Object getParent(TreeListDialogField<Object> field, Object element) {
		if (element instanceof CPListElementAttribute) {
			return ((CPListElementAttribute) element).getParent();
		} else if (element instanceof DetailNode<?>) {
			return ((DetailNode<?>) element).fParent;
		}
		return null;
	}

	@Override
	public boolean hasChildren(TreeListDialogField<Object> field, Object element) {
		Object[] children= getChildren(field, element);
		return children != null && children.length > 0;
	}

	void validate() {
		IWorkspaceRoot wsRoot= ResourcesPlugin.getWorkspace().getRoot();
		Map<IPath, Map<String,List<IResource>>> out2mod2src= new HashMap<>(); // output -> (module -> sources)
		for (Entry<String, String> entry : fModuleDependenciesPage.fPatchMap.entrySet()) {
			Path path= new Path(entry.getKey());
			IResource resource= wsRoot.findMember(path);
			IJavaProject javaProject= JavaCore.create(resource.getProject());
			IPath output= findOutputFor(javaProject, resource.getFullPath());
			if (output != null) {
				Map<String, List<IResource>> module2source= out2mod2src.get(output);
				if (module2source == null) {
					out2mod2src.put(output, module2source= new HashMap<>());
				}
				List<IResource> sources= module2source.get(entry.getValue());
				if (sources == null) {
					module2source.put(entry.getValue(), sources= new ArrayList<>());
				}
				sources.add(resource);
			}
		}
		for (Map<String, List<IResource>> mod2src : out2mod2src.values()) {
			// does any output location map to more than one module?
			if (mod2src.entrySet().size() > 1) {
				fModuleDependenciesPage.setStatus(new StatusInfo(IStatus.ERROR,
					MessageFormat.format(NewWizardMessages.ModuleDependenciesAdapter_patchOutputConflict_validationError,
							mod2src.values().stream()
							.flatMap(folders -> folders.stream().map(f -> f.getFullPath().makeRelative().toString()))
							.collect(Collectors.joining(", "))))); //$NON-NLS-1$
				return;
			}
		}
		fModuleDependenciesPage.setStatus(StatusInfo.OK_STATUS);
	}
	private IPath findOutputFor(IJavaProject project, IPath source) {
		try {
			if (project.getPath().equals(source)) {
				return project.getOutputLocation();
			}
			IClasspathEntry classpathEntry= project.getClasspathEntryFor(source);
			if (classpathEntry != null) {
				if (classpathEntry.getOutputLocation() != null)
					return classpathEntry.getOutputLocation();
				return project.getOutputLocation();
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return null;
	}

	// ---------- IDialogFieldListener --------

	@Override
	public void dialogFieldChanged(DialogField field) {
//			libaryPageDialogFieldChanged(field);
	}

}