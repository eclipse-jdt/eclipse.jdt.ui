/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.reorg;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ui.actions.WorkspaceModifyOperation;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementContentProvider;
import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIException;
import org.eclipse.jdt.internal.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.ISelectionValidator;
import org.eclipse.jdt.internal.ui.packageview.PackageViewerSorter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;


public class CopyAction extends ReorgAction {
	private final static String PREFIX= "action.copy.";
	protected final static String ACTION_NAME= "name";
	private final static String LABEL= PREFIX+"label";
	protected final static String DESCRIPTION= PREFIX+"description";
	protected final static String DESTINATION_DIALOG= "destination_dialog.";
	protected final static String TASK_NAME= "task.label";
	protected final static String ERROR_EXCEPTION="error.exception.message";
	protected final static String ERROR_DUPLICATE="error.duplicate.message";

	public CopyAction(ISelectionProvider viewer) {
		this(viewer, JavaPlugin.getResourceString(LABEL));
		setDescription(JavaPlugin.getResourceString(DESCRIPTION));
	}

	public CopyAction(ISelectionProvider viewer, String name) {
		super(viewer, name);
	}

	public void doActionPerformed() {
		Shell activeShell= JavaPlugin.getActiveWorkbenchShell();
		IJavaElement root= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
		
		List v= new ArrayList();
		Iterator iter= getStructuredSelection().iterator();
		while (iter.hasNext())
			v.add(iter.next());
		if (!ensureSaved(v))
			return;
		Object destination= selectDestination(root, v, activeShell);
		if (destination instanceof IJavaProject) {
			IJavaProject p= (IJavaProject)destination;
			try {
				if (ReorgSupportFactory.isPackageFragmentRoot(p)) {
					destination= ReorgSupportFactory.getPackageFragmentRoot(p);
				}
			} catch (JavaModelException e) {
			}
		}
		if (destination != null) {
			processElements(activeShell, destination, v);
		}
	}
	
	protected String[] getRenamings(Shell w, final Object destination, final List elements, final List existing) {
		final String[] names= new String[elements.size()];
		for (int i= 0; i < elements.size(); i++) {
			final Object o= elements.get(i);
			final INamingPolicy support= ReorgSupportFactory.createNamingPolicy(o);
			if (support.isValidNewName(o, destination, getElementName(o)) != null) {
				final boolean allowReplace= support.canReplace(o, destination, getElementName(o));
				Shell parent= w.getShell();
				NameClashDialog dialog= new NameClashDialog(parent, new IInputValidator() {
					public String isValid(String newText) {
						for (int j= 0; j < names.length; j++) {
							if (names[j] != null && names[j].equals(newText))
								return getErrorDuplicate();
						}
						return support.isValidNewName(o, destination, newText);
					}
				}, getElementName(o), allowReplace);
				if (dialog.open() == dialog.CANCEL)
					return null;
				String newName= null;
				if (dialog.isReplace()) {
					newName= getElementName(o);
					names[i]= null;
				} else {
					newName= dialog.getNewName();
					names[i]= dialog.getNewName();
				}
				Object replaced= support.getElement(destination, newName);
				if (replaced != null)
					existing.add(replaced);
			} else {
				names[i]= null;
			}
		}
		return names;
	}
	
	protected void processElements(Shell activeShell, final Object destination, final List elements) {
		ArrayList toBeReplaced= new ArrayList();
		final String[] names= getRenamings(activeShell, destination, elements, toBeReplaced);
		if (!confirmIfUnsaved(toBeReplaced))
			return;
		if (names == null)
			return;
		String id= JavaPlugin.getDefault().getDescriptor().getUniqueIdentifier();
		final MultiStatus status= new MultiStatus(id, IStatus.OK, "status", null);
		final List createdElements= new ArrayList();
		WorkspaceModifyOperation op= new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor pm) {
				int size= elements.size();
				pm.beginTask(getTaskName(), size);
				ICopySupport support= ReorgSupportFactory.createCopySupport(elements);
				for (int i= 0; i < size; i++) {
					IProgressMonitor subPM= new SubProgressMonitor(pm, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL);
					Object o= elements.get(i);
					pm.subTask(support.getElementName(o));
					try {
						Object newElement= support.copyTo(o, destination, names[i], subPM);
						createdElements.add(newElement);
					} catch (CoreException e) {
						status.merge(e.getStatus());
					}
				}
				pm.done();
			}
		};
		try {
			new ProgressMonitorDialog(activeShell).run(false, true, op);
		} catch (InvocationTargetException e) {
			// this will never happen
		} catch (InterruptedException e) {
			return;
		}
		if (!status.isOK()) {
			Throwable t= new JavaUIException(status);
			ResourceBundle bundle= JavaPlugin.getResourceBundle();
			ExceptionHandler.handle(t, activeShell, bundle, ERROR_EXCEPTION);
		} else {
			select(activeShell, createdElements);
		}
	}
	
	String[] getElementNames(List elements){
		String[] result= new String[elements.size()];
			for (int i= 0; i < elements.size(); i++)
			   result[i]= getElementName(elements.get(i));
		return result;
	}
		
	
	protected String getErrorDuplicate() {
		return JavaPlugin.getResourceString(getPrefix()+ERROR_DUPLICATE);
	}	
	
	protected String getTaskName() {
		return JavaPlugin.getResourceString(getPrefix()+TASK_NAME);
	}
	
	protected String getActionName() {
		return JavaPlugin.getResourceString(getPrefix()+ACTION_NAME);
	}
	
	protected String getDestinationDialogMessage() {
		return JavaPlugin.getResourceString(getPrefix()+DESTINATION_DIALOG+"message");
	}
	
	protected String getPrefix() {
		return PREFIX;
	}

	protected String getElementName(Object o) {
		if (o instanceof IJavaElement)
			return ((IJavaElement)o).getElementName();
		if (o instanceof IResource) {
			return ((IResource)o).getName();
		}
		return o.toString();
	}
	
	
	protected ISelectionValidator getDestinationValidator(List elements) {
		return new CopyElementsValidator(elements);
	}
	
	protected Object selectDestination(IJavaElement root, List elements, Shell parent) {
		JavaElementContentProvider cp= new JavaElementContentProvider();
		ContainerFilter filter= new ContainerFilter(elements);
		ISelectionValidator v= getDestinationValidator(elements);
		ILabelProvider labelProvider= new DestinationRenderer(
			JavaElementLabelProvider.SHOW_SMALL_ICONS
		);
		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(parent, getActionName(), null, labelProvider, cp);
		dialog.setValidator(v);
		dialog.addFilter(filter);
		dialog.setSorter(new PackageViewerSorter());
		dialog.setMessage(getDestinationDialogMessage());
		dialog.setInitialSizeInCharacters(60, 18);
		
		if (dialog.open(root) != dialog.CANCEL)
			return dialog.getSelectedElement();
		return null;
	}

	protected boolean canExecute(IStructuredSelection sel) {
		Iterator iter= sel.iterator();
		if (sel.isEmpty())
			return false;
		List allElements= new ArrayList();
		
		while (iter.hasNext()) {
			allElements.add(iter.next());
			
		}
		ICopySupport support= ReorgSupportFactory.createCopySupport(allElements);
		for (int i= 0; i < allElements.size(); i++) {
			if (!support.isCopyable(allElements.get(i)))
				return false;
		}
		return true;
	}
}
