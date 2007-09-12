/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.jface.text.formatter.MultiPassContentFormatter;

import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.Resources;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.IJavaPartitions;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.browsing.LogicalPackage;
import org.eclipse.jdt.internal.ui.dialogs.OptionalMessageDialog;
import org.eclipse.jdt.internal.ui.text.comment.CommentFormattingContext;
import org.eclipse.jdt.internal.ui.text.comment.CommentFormattingStrategy;
import org.eclipse.jdt.internal.ui.text.java.JavaFormattingStrategy;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.workingsets.JavaWorkingSetUpdater;

/**
 * Formats the code of the compilation units contained in the selection.
 * <p>
 * The action is applicable to selections containing elements of
 * type <code>ICompilationUnit</code>, <code>IPackage
 * </code>, <code>IPackageFragmentRoot/code> and
 * <code>IJavaProject</code>.
 * </p>
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 3.0
 */
public class FormatAllAction extends SelectionDispatchAction {
	
	private DocumentRewriteSession fRewriteSession;
	
	/* (non-Javadoc)
	 * Class implements IObjectActionDelegate
	 */
	public static class ObjectDelegate implements IObjectActionDelegate {
		private FormatAllAction fAction;
		public void setActivePart(IAction action, IWorkbenchPart targetPart) {
			fAction= new FormatAllAction(targetPart.getSite());
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
	 * Creates a new <code>FormatAllAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public FormatAllAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.FormatAllAction_label); 
		setToolTipText(ActionMessages.FormatAllAction_tooltip); 
		setDescription(ActionMessages.FormatAllAction_description); 

		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.FORMAT_ALL);					
	}
	
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(ITextSelection selection) {
		// do nothing
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
			collectCompilationUnits(selected[i], result);
		}
		return (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
	}
	
	private void collectCompilationUnits(Object element, Collection result) {
		try {
			if (element instanceof IJavaElement) {
				IJavaElement elem= (IJavaElement) element;
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
			} else if (element instanceof LogicalPackage) {
				IPackageFragment[] packageFragments= ((LogicalPackage)element).getFragments();
				for (int k= 0; k < packageFragments.length; k++) {
					IPackageFragment pack= packageFragments[k];
					if (pack.exists()) {
						collectCompilationUnits(pack, result);
					}
				}
			} else if (element instanceof IWorkingSet) {
				IWorkingSet workingSet= (IWorkingSet) element;
				IAdaptable[] elements= workingSet.getElements();
				for (int j= 0; j < elements.length; j++) {
					collectCompilationUnits(elements[j], result);
				}
			}
		} catch (JavaModelException e) {
			if (JavaModelUtil.isExceptionToBeLogged(e))
				JavaPlugin.log(e);
		}		
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
				} else if (selected[i] instanceof IWorkingSet) {
					IWorkingSet workingSet= (IWorkingSet) selected[i];
					return JavaWorkingSetUpdater.ID.equals(workingSet.getId());
				}
			} catch (JavaModelException e) {
				if (JavaModelUtil.isExceptionToBeLogged(e))
					JavaPlugin.log(e);
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(ITextSelection selection) {
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(IStructuredSelection selection) {
		ICompilationUnit[] cus= getCompilationUnits(selection);
		if (cus.length == 0) {
			MessageDialog.openInformation(getShell(), ActionMessages.FormatAllAction_EmptySelection_title, ActionMessages.FormatAllAction_EmptySelection_description);
			return;
		}
		try {
			if (cus.length == 1) {
				JavaUI.openInEditor(cus[0]);
			} else {
				int returnCode= OptionalMessageDialog.open("FormatAll",  //$NON-NLS-1$
						getShell(), 
						ActionMessages.FormatAllAction_noundo_title, 
						null,
						ActionMessages.FormatAllAction_noundo_message,  
						MessageDialog.WARNING, 		
						new String[] {IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL}, 
						0);
				if (returnCode != OptionalMessageDialog.NOT_SHOWN && returnCode != Window.OK )
					return;
			}
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), ActionMessages.FormatAllAction_error_title, ActionMessages.FormatAllAction_error_message); 
		}
		runOnMultiple(cus);
	}

	private IResource[] getResources(ICompilationUnit[] cus) {
		IResource[] res= new IResource[cus.length];
		for (int i= 0; i < res.length; i++) {
			res[i]= cus[i].getResource();
		}
		return res;
	}

	/**
	 * Perform format all on the given compilation units.
	 * @param cus The compilation units to format.
	 */
	public void runOnMultiple(final ICompilationUnit[] cus) {
		try {
			final MultiStatus status= new MultiStatus(JavaUI.ID_PLUGIN, IStatus.OK, ActionMessages.FormatAllAction_status_description, null);
			
			IStatus valEditStatus= Resources.makeCommittable(getResources(cus), getShell());
			if (valEditStatus.matches(IStatus.CANCEL)) {
				return;
			}
			status.merge(valEditStatus);
			if (!status.matches(IStatus.ERROR)) {
				PlatformUI.getWorkbench().getProgressService().run(true, true, new WorkbenchRunnableAdapter(new IWorkspaceRunnable() {
					public void run(IProgressMonitor monitor) {
						doRunOnMultiple(cus, status, monitor);
					}
				})); // workspace lock
			}
			if (!status.isOK()) {
				String title= ActionMessages.FormatAllAction_multi_status_title;
				ErrorDialog.openError(getShell(), title, null, status);
			}
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), ActionMessages.FormatAllAction_error_title, ActionMessages.FormatAllAction_error_message); 
		} catch (InterruptedException e) {
			// Canceled by user
		}
	}
	
	private static Map getFomatterSettings(IJavaProject project) {
		return new HashMap(project.getOptions(true));
	}
	
	private void doFormat(IDocument document, Map options) {
		final IFormattingContext context = new CommentFormattingContext();
		try {
			context.setProperty(FormattingContextProperties.CONTEXT_PREFERENCES, options);
			context.setProperty(FormattingContextProperties.CONTEXT_DOCUMENT, Boolean.valueOf(true));
			
			final MultiPassContentFormatter formatter= new MultiPassContentFormatter(IJavaPartitions.JAVA_PARTITIONING, IDocument.DEFAULT_CONTENT_TYPE);
			
			formatter.setMasterStrategy(new JavaFormattingStrategy());
			formatter.setSlaveStrategy(new CommentFormattingStrategy(), IJavaPartitions.JAVA_DOC);
			formatter.setSlaveStrategy(new CommentFormattingStrategy(), IJavaPartitions.JAVA_SINGLE_LINE_COMMENT);
			formatter.setSlaveStrategy(new CommentFormattingStrategy(), IJavaPartitions.JAVA_MULTI_LINE_COMMENT);		

			try {
				startSequentialRewriteMode(document);
				formatter.format(document, context);
			} finally {
				stopSequentialRewriteMode(document);
			}
		} finally {
		    context.dispose();
		}
    }

	private void startSequentialRewriteMode(IDocument document) {
		if (document instanceof IDocumentExtension4) {
			IDocumentExtension4 extension= (IDocumentExtension4) document;
			fRewriteSession= extension.startRewriteSession(DocumentRewriteSessionType.SEQUENTIAL);
		} else if (document instanceof IDocumentExtension) {
			IDocumentExtension extension= (IDocumentExtension) document;
			extension.startSequentialRewrite(false);
		}
	}
	
	private void stopSequentialRewriteMode(IDocument document) {
		if (document instanceof IDocumentExtension4) {
			IDocumentExtension4 extension= (IDocumentExtension4) document;
			extension.stopRewriteSession(fRewriteSession);
		} else if (document instanceof IDocumentExtension) {
			IDocumentExtension extension= (IDocumentExtension)document;
			extension.stopSequentialRewrite();
		}
	}
	
	private void doRunOnMultiple(ICompilationUnit[] cus, MultiStatus status, IProgressMonitor monitor) throws OperationCanceledException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}	
		monitor.setTaskName(ActionMessages.FormatAllAction_operation_description); 
	
		monitor.beginTask("", cus.length * 4); //$NON-NLS-1$
		try {
			Map lastOptions= null;
			IJavaProject lastProject= null;
			
			for (int i= 0; i < cus.length; i++) {
				ICompilationUnit cu= cus[i];
				IPath path= cu.getPath();
				if (lastProject == null || !lastProject.equals(cu.getJavaProject())) {
					lastProject= cu.getJavaProject();
					lastOptions= getFomatterSettings(lastProject);
				}
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
				if (cu.getResource().getResourceAttributes().isReadOnly()) {
					String message= Messages.format(ActionMessages.FormatAllAction_read_only_skipped, path.toString());
					status.add(new Status(IStatus.WARNING, JavaUI.ID_PLUGIN, IStatus.WARNING, message, null));
					continue;
				}
				
				ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
				try {
					try {
						manager.connect(path, LocationKind.IFILE, new SubProgressMonitor(monitor, 1));
		
						monitor.subTask(path.makeRelative().toString());
						ITextFileBuffer fileBuffer= manager.getTextFileBuffer(path, LocationKind.IFILE);
						
						formatCompilationUnit(fileBuffer, lastOptions);
						
						if (fileBuffer.isDirty() && !fileBuffer.isShared()) {
							fileBuffer.commit(new SubProgressMonitor(monitor, 2), false);
						} else {
							monitor.worked(2);
						}
					} finally {
						manager.disconnect(path, LocationKind.IFILE, new SubProgressMonitor(monitor, 1));
					}
				} catch (CoreException e) {
					String message= Messages.format(ActionMessages.FormatAllAction_problem_accessing, new String[] { path.toString(), e.getLocalizedMessage() });
					status.add(new Status(IStatus.WARNING, JavaUI.ID_PLUGIN, IStatus.WARNING, message, e));
				}
			}
		} finally {
			monitor.done();
		}
	}
	
	private void formatCompilationUnit(final ITextFileBuffer fileBuffer, final Map options) {
		if (fileBuffer.isShared()) {
			getShell().getDisplay().syncExec(new Runnable() {
				public void run() {
					doFormat(fileBuffer.getDocument(), options);
				}
			});
		} else {
			doFormat(fileBuffer.getDocument(), options); // run in context thread
		}
	}
	
}
