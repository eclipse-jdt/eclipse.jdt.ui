/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.progress.IProgressService;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.ValidateEditException;
import org.eclipse.jdt.internal.corext.codemanipulation.OrganizeImportsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.OrganizeImportsOperation.IChooseImportQuery;
import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.browsing.LogicalPackage;
import org.eclipse.jdt.internal.ui.dialogs.MultiElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.ProblemDialog;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.util.ElementValidator;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.TypeInfoLabelProvider;

/**
 * Organizes the imports of a compilation unit.
 * <p>
 * The action is applicable to selections containing elements of
 * type <code>ICompilationUnit</code> or <code>IPackage
 * </code>.
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class OrganizeImportsAction extends SelectionDispatchAction {
	
	private JavaEditor fEditor;

	/* (non-Javadoc)
	 * Class implements IObjectActionDelegate
	 */
	public static class ObjectDelegate implements IObjectActionDelegate {
		private OrganizeImportsAction fAction;
		public void setActivePart(IAction action, IWorkbenchPart targetPart) {
			fAction= new OrganizeImportsAction(targetPart.getSite());
		}
		public void run(IAction action) {
			fAction.run();
		}
		public void selectionChanged(IAction action, ISelection selection) {
			if (fAction == null)
				action.setEnabled(false);
		}
	}

	/**
	 * Creates a new <code>OrganizeImportsAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public OrganizeImportsAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.getString("OrganizeImportsAction.label")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("OrganizeImportsAction.tooltip")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("OrganizeImportsAction.description")); //$NON-NLS-1$

		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.ORGANIZE_IMPORTS_ACTION);					
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public OrganizeImportsAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(getCompilationUnit(fEditor) != null);
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(ITextSelection selection) {
		setEnabled(getCompilationUnit(fEditor) != null);
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(isEnabled(selection));
	}
	
	private ICompilationUnit[] getCompilationUnits(IStructuredSelection selection) {
		HashSet result= new HashSet();
		Object[] selected= selection.toArray();
		for (int i= 0; i < selected.length; i++) {
			try {
				if (selected[i] instanceof IJavaElement) {
					IJavaElement elem= (IJavaElement) selected[i];
					if (elem.exists()) {
					
						switch (elem.getElementType()) {
							case IJavaElement.TYPE:
								if (elem.getParent().getElementType() == IJavaElement.COMPILATION_UNIT) {
									result.add(elem.getParent());
								}
								break;						
							case IJavaElement.COMPILATION_UNIT:
								result.add(elem);
								break;
							case IJavaElement.IMPORT_CONTAINER:
								result.add(elem.getParent());
								break;							
							case IJavaElement.PACKAGE_FRAGMENT:
								collectCompilationUnits((IPackageFragment) elem, result);
								break;
							case IJavaElement.PACKAGE_FRAGMENT_ROOT:
								collectCompilationUnits((IPackageFragmentRoot) elem, result);
								break;
							case IJavaElement.JAVA_PROJECT:
								IPackageFragmentRoot[] roots= ((IJavaProject) elem).getPackageFragmentRoots();
								for (int k= 0; k < roots.length; k++) {
									collectCompilationUnits(roots[k], result);
								}
								break;			
						}
					}
				} else if (selected[i] instanceof LogicalPackage) {
					IPackageFragment[] packageFragments= ((LogicalPackage)selected[i]).getFragments();
					for (int k= 0; k < packageFragments.length; k++) {
						IPackageFragment pack= packageFragments[k];
						if (pack.exists()) {
							collectCompilationUnits(pack, result);
						}
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		return (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
	}
	
	private void collectCompilationUnits(IPackageFragment pack, Collection result) throws JavaModelException {
		result.addAll(Arrays.asList(pack.getCompilationUnits()));
	}

	private void collectCompilationUnits(IPackageFragmentRoot root, Collection result) throws JavaModelException {
		if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
			IJavaElement[] children= root.getChildren();
			for (int i= 0; i < children.length; i++) {
				collectCompilationUnits((IPackageFragment) children[i], result);
			}
		}
	}	
	
	
	private boolean isEnabled(IStructuredSelection selection) {
		Object[] selected= selection.toArray();
		for (int i= 0; i < selected.length; i++) {
			try {
				if (selected[i] instanceof IJavaElement) {
					IJavaElement elem= (IJavaElement) selected[i];
					if (elem.exists()) {
						switch (elem.getElementType()) {
							case IJavaElement.TYPE:
								return elem.getParent().getElementType() == IJavaElement.COMPILATION_UNIT; // for browsing perspective
							case IJavaElement.COMPILATION_UNIT:
								return true;
							case IJavaElement.IMPORT_CONTAINER:
								return true;
							case IJavaElement.PACKAGE_FRAGMENT:
							case IJavaElement.PACKAGE_FRAGMENT_ROOT:
								IPackageFragmentRoot root= (IPackageFragmentRoot) elem.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
								return (root.getKind() == IPackageFragmentRoot.K_SOURCE);
							case IJavaElement.JAVA_PROJECT:
								// https://bugs.eclipse.org/bugs/show_bug.cgi?id=65638
								return true;
						}
					}
				} else if (selected[i] instanceof LogicalPackage) {
					return true;
				}
			} catch (JavaModelException e) {
				if (!e.isDoesNotExist()) {
					JavaPlugin.log(e);
				}
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(ITextSelection selection) {
		ICompilationUnit cu= getCompilationUnit(fEditor);
		if (cu != null) {
			run(cu);
		}
	}

	private static ICompilationUnit getCompilationUnit(JavaEditor editor) {
		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
		ICompilationUnit cu= manager.getWorkingCopy(editor.getEditorInput());
		return cu;
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(IStructuredSelection selection) {
		ICompilationUnit[] cus= getCompilationUnits(selection);
		if (cus.length == 0)
			return;
		if (cus.length == 1) {
			run(cus[0]);
		} else {
			runOnMultiple(cus);
		}
	}

	/**
	 * Peform organize import on multiple compilation units. No editors are opened.
	 * @param cus The compilation units to run on
	 */
	public void runOnMultiple(final ICompilationUnit[] cus) {
		try {
			String message= ActionMessages.getString("OrganizeImportsAction.multi.status.description"); //$NON-NLS-1$
			final MultiStatus status= new MultiStatus(JavaUI.ID_PLUGIN, IStatus.OK, message, null);
			
			IProgressService progressService= PlatformUI.getWorkbench().getProgressService();
			progressService.run(true, true, new WorkbenchRunnableAdapter(new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) {
					doRunOnMultiple(cus, status, monitor);
				}
			})); // workspace lock
			if (!status.isOK()) {
				String title= ActionMessages.getString("OrganizeImportsAction.multi.status.title"); //$NON-NLS-1$
				ProblemDialog.open(getShell(), title, null, status);
			}
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), ActionMessages.getString("OrganizeImportsAction.error.title"), ActionMessages.getString("OrganizeImportsAction.error.message")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (InterruptedException e) {
			// cancelled by user
		}		
		
	}
	
	static final class OrganizeImportError extends RuntimeException {
		private static final long serialVersionUID= 1L;
	}
	
	private void doRunOnMultiple(ICompilationUnit[] cus, MultiStatus status, IProgressMonitor monitor) throws OperationCanceledException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}	
		monitor.setTaskName(ActionMessages.getString("OrganizeImportsAction.multi.op.description")); //$NON-NLS-1$
	
		monitor.beginTask("", cus.length); //$NON-NLS-1$
		try {
			IPreferenceStore store= PreferenceConstants.getPreferenceStore();
			String[] prefOrder= JavaPreferencesSettings.getImportOrderPreference(store);
			int threshold= JavaPreferencesSettings.getImportNumberThreshold(store);
			boolean ignoreLowerCaseNames= store.getBoolean(PreferenceConstants.ORGIMPORTS_IGNORELOWERCASE);
	
			IChooseImportQuery query= new IChooseImportQuery() {
				public TypeInfo[] chooseImports(TypeInfo[][] openChoices, ISourceRange[] ranges) {
					throw new OrganizeImportError();
				}
			};
			
	
			for (int i= 0; i < cus.length; i++) {
				ICompilationUnit cu= cus[i];
				if (testOnBuildPath(cu, status)) {
					String cuLocation= cu.getPath().makeRelative().toString();
					
					monitor.subTask(cuLocation);

					try {
						OrganizeImportsOperation op= new OrganizeImportsOperation(cu, prefOrder, threshold, ignoreLowerCaseNames, !cu.isWorkingCopy(), true, query);
						runInSync(op, cuLocation, status, monitor);

						IProblem parseError= op.getParseError();
						if (parseError != null) {
							String message= ActionMessages.getFormattedString("OrganizeImportsAction.multi.error.parse", cuLocation); //$NON-NLS-1$
							status.add(new Status(IStatus.INFO, JavaUI.ID_PLUGIN, IStatus.ERROR, message, null));
						} 	
					} catch (CoreException e) {
						JavaPlugin.log(e);
						String message= ActionMessages.getFormattedString("OrganizeImportsAction.multi.error.unexpected", e.getStatus().getMessage()); //$NON-NLS-1$
						status.add(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, message, null));					
					}

					if (monitor.isCanceled()) {
						throw new OperationCanceledException();
					}
				}
			}
		} finally {
			monitor.done();
		}
	}
	
	private boolean testOnBuildPath(ICompilationUnit cu, MultiStatus status) {
		IJavaProject project= cu.getJavaProject();
		if (!project.isOnClasspath(cu)) {
			String cuLocation= cu.getPath().makeRelative().toString();
			String message= ActionMessages.getFormattedString("OrganizeImportsAction.multi.error.notoncp", cuLocation); //$NON-NLS-1$
			status.add(new Status(IStatus.INFO, JavaUI.ID_PLUGIN, IStatus.ERROR, message, null));
			return false;
		}
		return true;
	}
	
		
	private void runInSync(final OrganizeImportsOperation op, final String cuLocation, final MultiStatus status, final IProgressMonitor monitor) {
		Runnable runnable= new Runnable() {
			public void run() {
				try {
					op.run(new SubProgressMonitor(monitor, 1));
				} catch (ValidateEditException e) {
					status.add(e.getStatus());
				} catch (CoreException e) {
					JavaPlugin.log(e);
					String message= ActionMessages.getFormattedString("OrganizeImportsAction.multi.error.unexpected", e.getStatus().getMessage()); //$NON-NLS-1$
					status.add(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, message, null));					
				} catch (OrganizeImportError e) {
					String message= ActionMessages.getFormattedString("OrganizeImportsAction.multi.error.unresolvable", cuLocation); //$NON-NLS-1$
					status.add(new Status(IStatus.INFO, JavaUI.ID_PLUGIN, IStatus.ERROR, message, null));
				}
			}
		};
		getShell().getDisplay().syncExec(runnable);
	}
				

	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 * @param cu The compilation unit to process
	 */
	public void run(ICompilationUnit cu) {
		if (!ElementValidator.check(cu, getShell(), ActionMessages.getString("OrganizeImportsAction.error.title"), fEditor != null)) //$NON-NLS-1$ 
			return;
		if (!ActionUtil.isProcessable(getShell(), cu))
			return;
			
		try {
			IPreferenceStore store= PreferenceConstants.getPreferenceStore();
			String[] prefOrder= JavaPreferencesSettings.getImportOrderPreference(store);
			int threshold= JavaPreferencesSettings.getImportNumberThreshold(store);
			boolean ignoreLowerCaseNames= store.getBoolean(PreferenceConstants.ORGIMPORTS_IGNORELOWERCASE);
			
			if (fEditor == null && EditorUtility.isOpenInEditor(cu) == null) {
				IEditorPart editor= EditorUtility.openInEditor(cu);
				if (editor instanceof JavaEditor) {
					fEditor= (JavaEditor) editor;
				}			
			}
			OrganizeImportsOperation op= new OrganizeImportsOperation(cu, prefOrder, threshold, ignoreLowerCaseNames, !cu.isWorkingCopy(), true, createChooseImportQuery());
		
			IRewriteTarget target= null;
			if (fEditor != null) {
				target= (IRewriteTarget) fEditor.getAdapter(IRewriteTarget.class);
				if (target != null) {
					target.beginCompoundChange();
				}
			}
			
			IProgressService progressService= PlatformUI.getWorkbench().getProgressService();
			IRunnableContext context= getSite().getWorkbenchWindow();
			if (context == null) {
				context= progressService;
			}
			try {
				progressService.runInUI(context, new WorkbenchRunnableAdapter(op, op.getScheduleRule()), op.getScheduleRule());
				IProblem parseError= op.getParseError();
				if (parseError != null) {
					String message= ActionMessages.getFormattedString("OrganizeImportsAction.single.error.parse", parseError.getMessage()); //$NON-NLS-1$
					MessageDialog.openInformation(getShell(), ActionMessages.getString("OrganizeImportsAction.error.title"), message); //$NON-NLS-1$ 
					if (fEditor != null && parseError.getSourceStart() != -1) {
						fEditor.selectAndReveal(parseError.getSourceStart(), parseError.getSourceEnd() - parseError.getSourceStart() + 1);
					}
				} else {
					if (fEditor != null) {
						setStatusBarMessage(getOrganizeInfo(op));
					}
				}
			} catch (InvocationTargetException e) {
				ExceptionHandler.handle(e, getShell(), ActionMessages.getString("OrganizeImportsAction.error.title"), ActionMessages.getString("OrganizeImportsAction.error.message")); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (InterruptedException e) {
			} finally {
				if (target != null) {
					target.endCompoundChange();
				}
			}
		} catch (CoreException e) {	
			ExceptionHandler.handle(e, getShell(), ActionMessages.getString("OrganizeImportsAction.error.title"), ActionMessages.getString("OrganizeImportsAction.error.message")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	private String getOrganizeInfo(OrganizeImportsOperation op) {
		int nImportsAdded= op.getNumberOfImportsAdded();
		if (nImportsAdded >= 0) {
			return ActionMessages.getFormattedString("OrganizeImportsAction.summary_added", String.valueOf(nImportsAdded)); //$NON-NLS-1$
		} else {
			return ActionMessages.getFormattedString("OrganizeImportsAction.summary_removed", String.valueOf(-nImportsAdded)); //$NON-NLS-1$
		}
	}
		
	private IChooseImportQuery createChooseImportQuery() {
		return new IChooseImportQuery() {
			public TypeInfo[] chooseImports(TypeInfo[][] openChoices, ISourceRange[] ranges) {
				return doChooseImports(openChoices, ranges);
			}
		};
	}
	
	private TypeInfo[] doChooseImports(TypeInfo[][] openChoices, final ISourceRange[] ranges) {
		// remember selection
		ISelection sel= fEditor != null ? fEditor.getSelectionProvider().getSelection() : null;
		TypeInfo[] result= null;
		ILabelProvider labelProvider= new TypeInfoLabelProvider(TypeInfoLabelProvider.SHOW_FULLYQUALIFIED);
		
		MultiElementListSelectionDialog dialog= new MultiElementListSelectionDialog(getShell(), labelProvider) {
			protected void handleSelectionChanged() {
				super.handleSelectionChanged();
				// show choices in editor
				doListSelectionChanged(getCurrentPage(), ranges);
			}
		};
		dialog.setTitle(ActionMessages.getString("OrganizeImportsAction.selectiondialog.title")); //$NON-NLS-1$
		dialog.setMessage(ActionMessages.getString("OrganizeImportsAction.selectiondialog.message")); //$NON-NLS-1$
		dialog.setElements(openChoices);
		if (dialog.open() == Window.OK) {
			Object[] res= dialog.getResult();			
			result= new TypeInfo[res.length];
			for (int i= 0; i < res.length; i++) {
				Object[] array= (Object[]) res[i];
				if (array.length > 0)
					result[i]= (TypeInfo) array[0];
			}
		}
		// restore selection
		if (sel instanceof ITextSelection) {
			ITextSelection textSelection= (ITextSelection) sel;
			fEditor.selectAndReveal(textSelection.getOffset(), textSelection.getLength());
		}
		return result;
	}
	
	private void doListSelectionChanged(int page, ISourceRange[] ranges) {
		if (fEditor != null && ranges != null && page >= 0 && page < ranges.length) {
			ISourceRange range= ranges[page];
			fEditor.selectAndReveal(range.getOffset(), range.getLength());
		}
	}
	
	private void setStatusBarMessage(String message) {
		IEditorActionBarContributor contributor= fEditor.getEditorSite().getActionBarContributor();
		if (contributor instanceof EditorActionBarContributor) {
			IStatusLineManager manager= ((EditorActionBarContributor) contributor).getActionBars().getStatusLineManager();
			manager.setMessage(message);
		}
	}
	
}
