/*******************************************************************************
 * Copyright (c) 2007, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Andrew Obuchowicz <aobuchow@redhat.com> - Rename Resource should be inline
 ******************************************************************************/
package org.eclipse.ltk.internal.ui.refactoring.actions;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.internal.core.refactoring.resource.RenameResourceProcessor;
import org.eclipse.ltk.internal.ui.refactoring.InternalAPI;
import org.eclipse.ltk.internal.ui.refactoring.RefactoringUIMessages;
import org.eclipse.ltk.internal.ui.refactoring.RefactoringUIPlugin;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ltk.ui.refactoring.resource.RenameResourceWizard;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.NamingConventions;


public class RenameResourceHandler extends AbstractResourcesHandler {
	private static final String LTK_RENAME_COMMAND_NEWNAME_PARAMETER_KEY= "org.eclipse.ltk.ui.refactoring.commands.renameResource.newName.parameter.key"; //$NON-NLS-1$
	private static final String LTK_CHECK_COMPOSITE_RENAME_PARAMETER_KEY= "org.eclipse.ltk.ui.refactoring.commands.checkCompositeRename.parameter.key"; //$NON-NLS-1$
	private IField fSelectedField;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Object checkCompositeRename= HandlerUtil.getVariable(event, LTK_CHECK_COMPOSITE_RENAME_PARAMETER_KEY);
		if (checkCompositeRename instanceof Boolean) {
			return checkForCompositeRename(event);
		} else {
			performRename(event);
		}
		return null;
	}

	@SuppressWarnings("boxing")
	private Object checkForCompositeRename(ExecutionEvent event) {
		ISelection sel= HandlerUtil.getCurrentSelection(event);
		if (sel instanceof IStructuredSelection) {
			IResource resource= getCurrentResource((IStructuredSelection) sel);
			if (resource != null) {
				// A new name is required in order to compute whether the change is composite or not
				String placeHolderFileName = 'a' + resource.getName();
				RenameResourceWizard refactoringWizard= new RenameResourceWizard(resource, placeHolderFileName);
				Change change= getChange(refactoringWizard);
				return isCompositeChange(change);
			}
		}
		return null;
	}

	private void performRename(ExecutionEvent event) {
		Shell activeShell= HandlerUtil.getActiveShell(event);
		Object newNameValue= HandlerUtil.getVariable(event, LTK_RENAME_COMMAND_NEWNAME_PARAMETER_KEY);
		String newName= null;
		if (newNameValue instanceof String) {
			newName= (String) newNameValue;
		}

		ISelection sel= HandlerUtil.getCurrentSelection(event);
		fSelectedField= null;
		ICompilationUnit compilationUnit = null;
		if (sel instanceof IStructuredSelection) {
		    Object firstElement = ((IStructuredSelection) sel).getFirstElement();
		    if (firstElement instanceof IJavaElement) {
		        IJavaElement javaElement = (IJavaElement) firstElement;
		        fSelectedField = javaElement.getAdapter(IField.class);
		        compilationUnit = javaElement.getAdapter(ICompilationUnit.class);
		    }
		}

		IEditorPart editorPart = HandlerUtil.getActiveEditor(event);
		IFile file = editorPart.getEditorInput().getAdapter(IFile.class);
		IJavaProject project= JavaCore.create(file.getProject());

		String[] excluded = new String[0];
		if (compilationUnit != null) {
		        IType[] types;
				try {
					types= compilationUnit.getAllTypes();
					 for (IType type : types) {
				            IField[] fields = type.getFields();
				            for (IField field : fields) {
				                String fieldName = field.getElementName();
				                excluded = Arrays.copyOf(excluded, excluded.length + 1);
				                excluded[excluded.length - 1] = fieldName;
				            }
				        }
					 int fieldModifiers = fSelectedField.getFlags();
						String[] newNames = getFieldNameSuggestions(project, newName,fieldModifiers, excluded);
						if(newNames.length>0) {
							newName = newNames[0];
						}
				} catch (JavaModelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

		}
		if (sel instanceof IStructuredSelection) {
			IResource resource= getCurrentResource((IStructuredSelection) sel);
			if (resource != null) {
				RenameResourceWizard refactoringWizard;
				Change change= null;
				RefactoringProcessor processor= null;
				if (newName != null) {
					refactoringWizard= new RenameResourceWizard(resource, newName);
					processor= ((ProcessorBasedRefactoring) refactoringWizard.getRefactoring()).getProcessor();
					change= getChange(refactoringWizard);
					//Reset the state of the wizard once we have the change it will perform
					refactoringWizard= new RenameResourceWizard(resource, newName);
				} else {
					refactoringWizard= new RenameResourceWizard(resource);
				}

				try {
					// Let user see rename dialog with preview page for composite changes or if another RefactoringProcessor is used (which may offer rename options)
					if (newName == null || change == null || isCompositeChange(change) || !(processor instanceof RenameResourceProcessor)) {
						RefactoringWizardOpenOperation op= new RefactoringWizardOpenOperation(refactoringWizard);
						op.run(activeShell, RefactoringUIMessages.RenameResourceHandler_title);
					} else {
						//Silently perform the rename without the dialog
						RefactoringCore.getUndoManager().aboutToPerformChange(change);
						Change undo= change.perform(new NullProgressMonitor());
						RefactoringCore.getUndoManager().changePerformed(change, true);
						RefactoringCore.getUndoManager().addUndo(RefactoringUIMessages.RenameResourceHandler_title, undo);
					}
				} catch (InterruptedException e) {
					// do nothing
				} catch (CoreException e) {
					RefactoringCore.getUndoManager().changePerformed(change, false);
					RefactoringUIPlugin.log(e);
				}
			}
		}
	}

	private Change getChange(RenameResourceWizard refactoringWizard) {
		refactoringWizard.setChangeCreationCancelable(true);
		refactoringWizard.setInitialComputationContext((boolean fork, boolean cancelable, IRunnableWithProgress runnable) -> runnable.run(new NullProgressMonitor()));
		return refactoringWizard.internalCreateChange(InternalAPI.INSTANCE,
				new CreateChangeOperation(new CheckConditionsOperation(refactoringWizard.getRefactoring(), CheckConditionsOperation.FINAL_CONDITIONS), RefactoringStatus.FATAL), true);
	}

	private boolean isCompositeChange(Change change) {
		return (change instanceof CompositeChange && ((CompositeChange) change).getChildren().length > 1);
	}

	private IResource getCurrentResource(IStructuredSelection sel) {
		IResource[] resources= getSelectedResources(sel);
		if (resources.length == 1) {
			return resources[0];
		}
		return null;
	}
	public static String[] getFieldNameSuggestions(IJavaProject project,String originalField, int fieldModifiers, String[] excluded) {
		return getFieldNameSuggestions(project, originalField, 0, fieldModifiers, excluded);
	}
	public static String[] getFieldNameSuggestions(IJavaProject project, String baseName, int dimensions, int modifiers, String[] excluded) {
		if (Flags.isFinal(modifiers) && Flags.isStatic(modifiers)) {
			return getVariableNameSuggestions(NamingConventions.VK_STATIC_FINAL_FIELD, project, baseName, dimensions, new ExcludedCollection(excluded), true);
		} else if (Flags.isStatic(modifiers)) {
			return getVariableNameSuggestions(NamingConventions.VK_STATIC_FIELD, project, baseName, dimensions, new ExcludedCollection(excluded), true);
		}
		return getVariableNameSuggestions(NamingConventions.VK_INSTANCE_FIELD, project, baseName, dimensions, new ExcludedCollection(excluded), true);
	}
	public static String[] getVariableNameSuggestions(int variableKind, IJavaProject project, String baseName, int dimensions, Collection<String> excluded, boolean evaluateDefault) {
		return NamingConventions.suggestVariableNames(variableKind, NamingConventions.BK_TYPE_NAME, removeTypeArguments(baseName), project, dimensions, getExcludedArray(excluded), evaluateDefault);
	}
	private static class ExcludedCollection extends AbstractList<String> {
		private String[] fExcluded;

		public ExcludedCollection(String[] excluded) {
			fExcluded= excluded;
		}

		public String[] getExcludedArray() {
			return fExcluded;
		}

		@Override
		public int size() {
			return fExcluded.length;
		}

		@Override
		public String get(int index) {
			return fExcluded[index];
		}

		@Override
		public int indexOf(Object o) {
			if (o instanceof String) {
				for (int i= 0; i < fExcluded.length; i++) {
					if (o.equals(fExcluded[i]))
						return i;
				}
			}
			return -1;
		}

		@Override
		public boolean contains(Object o) {
			return indexOf(o) != -1;
		}
	}
	private static String removeTypeArguments(String baseName) {
		int idx= baseName.indexOf('<');
		if (idx != -1) {
			return baseName.substring(0, idx);
		}
		return baseName;
	}
	private static String[] getExcludedArray(Collection<String> excluded) {
		if (excluded == null) {
			return null;
		} else if (excluded instanceof ExcludedCollection) {
			return ((ExcludedCollection)excluded).getExcludedArray();
		}
		return excluded.toArray(new String[excluded.size()]);
	}
}
