/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.reorg;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.DebugUtils;
import org.eclipse.jdt.internal.core.refactoring.reorg.CopyRefactoring;
import org.eclipse.jdt.internal.core.refactoring.reorg.ReorgRefactoring;
import org.eclipse.jdt.internal.core.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIException;
import org.eclipse.jdt.internal.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.ISelectionValidator;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.packageview.PackageFilter;
import org.eclipse.jdt.internal.ui.packageview.PackageViewerSorter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.viewsupport.ListContentProvider;
import org.eclipse.jdt.ui.JavaElementContentProvider;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.dialogs.ListSelectionDialog;

abstract class ReorgDestinationAction extends ReorgAction {

	public ReorgDestinationAction(ISelectionProvider p, String name) {
		super(p, name);
	}

	public void run() {
		List elements= getStructuredSelection().toList();
		if (!ensureSaved(elements, getActionName()))
			return;
		ReorgRefactoring refactoring= createRefactoring(elements);
		
		setUnsavedFileList(refactoring, elements);
		Object destination= selectDestination(refactoring);
		if (destination == null)
			return;
		try{			
			String duplicate= getDuplicatedElementName(elements);
			if (duplicate != null){
				String message= "Two or more elements named " + duplicate + " are selected."; 
				MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell().getShell(), "Duplicate Element Name",	message);
				return;
			}
			
			refactoring.setDestination(destination);
			
			Set excluded= getExcluded(refactoring);
			if (excluded == null) //canceled
				return;
			if (excluded.size() == elements.size())
				return;
			refactoring.setExcludedElements(excluded);	
				
			if (! isOkToProceed(refactoring))
				return;
			doReorg(refactoring);
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, "Exception", "Unexpected exception occurred. See log for details.");
		}	
	}
	
	/**
	 * returns null if no 2 elements have the same name
	 */
	private static  String getDuplicatedElementName(List elements){
		String[] names= getElementNames(elements);
		Arrays.sort(names);
		if (names.length == 0)
			return null;
		String last= names[0];	
		for (int i= 1; i < names.length; i++){ //non standard loop
			if (last.equals(names[i]))
				return last;
			last= names[i];	
		}
		return null;
	}
	
	abstract String getActionName();
	abstract String getDestinationDialogMessage();
	abstract ReorgRefactoring createRefactoring(List elements);
	
	//hook to override
	boolean isOkToProceed(ReorgRefactoring refactoring) throws JavaModelException{
		return true;
	}
	
	private static void setUnsavedFileList(ReorgRefactoring refactoring, List elements){
		List unsavedEditors= new ArrayList(0);
		collectUnsavedEditors(elements, unsavedEditors, new ArrayList(0));
		List files= getFiles(unsavedEditors);
		refactoring.setUnsavedFiles((IFile[])files.toArray(new IFile[files.size()]));
	}
	
	//XX code copied from UserInputWizardPage
	private static List getFiles(List editorParts){
		List result= new ArrayList(editorParts.size());
		for (Iterator iter= editorParts.iterator(); iter.hasNext(); ){
			IEditorPart each= (IEditorPart)iter.next();
			IEditorInput input= each.getEditorInput();
			if (input instanceof IFileEditorInput)
				result.add(((IFileEditorInput)input).getFile());
		}
		return result;
	}
	
 	void doReorg(ReorgRefactoring refactoring) throws JavaModelException{
		MultiStatus status= perform(refactoring);
		if (status.isOK()) 
			return;
		ExceptionHandler.handle(new JavaUIException(status), 
													ReorgMessages.getString("copyAction.exception.title"), //$NON-NLS-1$
													ReorgMessages.getString("copyAction.exception.label")); //$NON-NLS-1$ 
	}	
	
	//returns null iff canceled
	private static Set getExcluded(ReorgRefactoring refactoring) throws JavaModelException{
		Set elements= refactoring.getElementsThatExistInTarget();
		Set result= new HashSet();
		for (Iterator iter= elements.iterator(); iter.hasNext(); ){
			Object o= iter.next();
			int action= askIfOverwrite(ReorgUtils.getName(o));
			if (action == IDialogConstants.CANCEL_ID)
				return null;
			if (action == IDialogConstants.YES_TO_ALL_ID)	
				return new HashSet(0); //nothing excluded
			if (action == IDialogConstants.NO_ID)		
				result.add(o);	
		}
		return result;
	}
	
	private static int askIfOverwrite(String elementName){
		Shell shell= JavaPlugin.getActiveWorkbenchShell().getShell();
		String title= "Resource Exists";
		String question= "Element " + elementName + " already exists. Would you like to overwrite?";
		
		String[] labels= new String[] {IDialogConstants.YES_LABEL, IDialogConstants.YES_TO_ALL_LABEL,
															 IDialogConstants.NO_LABEL,  IDialogConstants.CANCEL_LABEL };
		final MessageDialog dialog = new MessageDialog(shell,	title, null, question, MessageDialog.QUESTION,	labels,  0);
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				dialog.open();
			}
		});
		int result = dialog.getReturnCode();
		if (result == 0)
			return IDialogConstants.YES_ID;
		if (result == 1)
			return IDialogConstants.YES_TO_ALL_ID;
		if (result == 2)
			return IDialogConstants.NO_ID;
		return IDialogConstants.CANCEL_ID;
	}
	
	//--- 
	private static boolean ensureSaved(List elements, String actionName) {
		List unsavedEditors= new ArrayList();
		List unsavedElements= new ArrayList();
		
		collectUnsavedEditors(elements, unsavedEditors, unsavedElements);
		
		if (unsavedEditors.isEmpty())
			return true;
			
		ListSelectionDialog dialog = createUnsavedEditorDialog(unsavedElements);
		if (dialog.open() != dialog.OK)
			return false;
		
		IRunnableWithProgress r= createSaveEditorOperation(dialog.getResult(), elements, unsavedEditors);
		try {
			new ProgressMonitorDialog(JavaPlugin.getActiveWorkbenchShell()).run(false, false, r);
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, actionName, ReorgMessages.getString("ReorgAction.exception.saving")); //$NON-NLS-1$
			return false;
		} catch (InterruptedException e) {
		}
		return true;
	}

	private static IRunnableWithProgress createSaveEditorOperation(final Object[] elementsToSave, final List elements, final List unsavedEditors) {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) {
				pm.beginTask(ReorgMessages.getString("ReorgAction.task.saving"), elementsToSave.length); //$NON-NLS-1$
				for (int i= 0; i < elementsToSave.length; i++) {
					IEditorPart editor= (IEditorPart)unsavedEditors.get(elements.indexOf(elementsToSave[i]));
					editor.doSave(new SubProgressMonitor(pm, 1));
				}
				pm.done();
			}
		};
	}

	private static ListSelectionDialog createUnsavedEditorDialog(List unsavedElements) {
		int labelFlags= JavaElementLabelProvider.SHOW_DEFAULT | JavaElementLabelProvider.SHOW_CONTAINER_QUALIFICATION 
					| JavaElementLabelProvider.SHOW_POSTIFIX_QUALIFICATION;
		Shell parent= JavaPlugin.getActiveWorkbenchShell();			
		ListSelectionDialog dialog= new ListSelectionDialog(parent, unsavedElements, new ListContentProvider(), 
			new JavaElementLabelProvider(labelFlags), getSaveTargetsMessage());
		dialog.setInitialSelections(unsavedElements.toArray());
		return dialog;
	}
	
	private static void collectUnsavedEditors(List elements, List unsavedEditors, List unsavedElements) {
		IEditorPart[] editors= JavaPlugin.getDirtyEditors();
		for (int i= 0; i < editors.length; i++) {
			for (Iterator iter= elements.iterator(); iter.hasNext(); ){
				Object element= iter.next();
				if (EditorUtility.isEditorInput(element, editors[i])) {
					unsavedEditors.add(editors[i]);
					unsavedElements.add(element);
				}
			}
		}
	}
	
	private static String getSaveTargetsMessage() {
		return ReorgMessages.getString("ReorgAction.checkSaveTargets"); //$NON-NLS-1$
	}
	
	private static String[] getElementNames(List elements){
		String[] result= new String[elements.size()];
		int i= 0;
		for (Iterator iter= elements.iterator(); iter.hasNext(); ){
			result[i]= ReorgUtils.getName(iter.next());
			i++;
		}   
		return result;
	}
	
	//overriden by d'n'd - must be protected
	protected Object selectDestination(ReorgRefactoring refactoring) {
		JavaElementContentProvider cp= new JavaElementContentProvider() {
			public boolean hasChildren(Object element) {
				// prevent the + from being shown in front of packages
				return !(element instanceof IPackageFragment) && super.hasChildren(element);
			}
		};
		ILabelProvider labelProvider= new DestinationRenderer(JavaElementLabelProvider.SHOW_SMALL_ICONS	);
		Shell parent= JavaPlugin.getActiveWorkbenchShell();
		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(parent, labelProvider, cp);
		dialog.setTitle(getActionName());
		dialog.setValidator(new ReorgSelectionValidator(refactoring));
		dialog.addFilter(new ContainerFilter(refactoring));
		dialog.setSorter(new PackageViewerSorter());
		dialog.setMessage(getDestinationDialogMessage());
		dialog.setSize(60, 18);
		dialog.setInput(JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()));
		
		if (dialog.open() != dialog.CANCEL)
			return dialog.getFirstResult();
		return null;
	}

	boolean canExecute(List sel) {
		return canActivate(createRefactoring(sel));
	}
	
	//-----
	private static class ContainerFilter extends PackageFilter {
		private ReorgRefactoring fRefactoring;
	
		ContainerFilter(ReorgRefactoring refactoring) {
			Assert.isNotNull(refactoring);
			fRefactoring= refactoring;
		}
	
		public boolean select(Viewer viewer, Object parent, Object o) {
			if (fRefactoring.getElementsToReorg().contains(o))
				return false;
			return fRefactoring.canBeAncestor(o);
		}
	}
	
	//-----
	private static class DestinationRenderer extends JavaElementLabelProvider {
		public DestinationRenderer(int flags) {
			super(flags);
		}
	
		public String getText(Object element) {
			try {
				if (element instanceof IPackageFragmentRoot) {
					IPackageFragmentRoot root= (IPackageFragmentRoot)element;
					if (root.getUnderlyingResource() instanceof IProject)
						return ReorgMessages.getString("DestinationRenderer.packages"); //$NON-NLS-1$
				}
			} catch (JavaModelException e) {
				ExceptionHandler.handle(e, "Exception", "Unexpected exception occurred. See log for details.");
			}
			return super.getText(element);
		}
	}
	//------
	private static class ReorgSelectionValidator implements ISelectionValidator {
		private ReorgRefactoring fRefactoring;
		
		public ReorgSelectionValidator(ReorgRefactoring refactoring) {
			Assert.isNotNull(refactoring);
			fRefactoring= refactoring;
		}
		
		public IStatus validate(Object[] selection)  {
			if (selection.length != 1)
				return new StatusInfo(IStatus.ERROR, "");
			try{	
				boolean valid= fRefactoring.isValidDestination(selection[0]);
				if (valid)
					return new StatusInfo();
				return new StatusInfo(IStatus.ERROR, "");	
			} catch (JavaModelException e){
				ExceptionHandler.handle(e, "Exception", "Unexpected exception occurred. See log for details.");
				return new StatusInfo(IStatus.ERROR, "");
			}	
		}
	}
}

