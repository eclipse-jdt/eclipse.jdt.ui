package org.eclipse.jdt.internal.ui.reorg;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.CreateChangeOperation;
import org.eclipse.jdt.internal.ui.refactoring.PerformChangeOperation;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

class ClipboardActionUtil {
	
	private ClipboardActionUtil(){
	}
	
	static boolean resourcesAreOfType(IResource[] resources, int resourceMask) {
		for (int i= 0; i < resources.length; i++) {
			if (!resourceIsType(resources[i], resourceMask))
				return false;
		}
		return true;
	}

	private static boolean resourceIsType(IResource resource, int resourceMask) {
		return (resource.getType() & resourceMask) != 0;
	}

	private static boolean isJavaResource(IResource resource){
		return JavaCore.create(resource) != null;
	}

	public static IJavaElement[] getJavaElements(IResource[] resources){
		List jElements= new ArrayList(resources.length);
		for (int i= 0; i < resources.length; i++) {
			if (isJavaResource(resources[i]))
				jElements.add(JavaCore.create(resources[i]));
		}
		return (IJavaElement[]) jElements.toArray(new IJavaElement[jElements.size()]);
	}


	public static IResource[] getNonJavaResources(IResource[] resources){
		List nonJava= new ArrayList(resources.length);
		for (int i= 0; i < resources.length; i++) {
			if (! isJavaResource(resources[i]))
				nonJava.add(resources[i]);
		}
		return (IResource[]) nonJava.toArray(new IResource[nonJava.size()]);
	}

	public static List getConvertedResources(IResource[] resourceData) {
		List elems= new ArrayList(resourceData.length);
		elems.addAll(Arrays.asList(ClipboardActionUtil.getJavaElements(resourceData)));
		elems.addAll(Arrays.asList(ClipboardActionUtil.getNonJavaResources(resourceData)));
		return elems;
	}

	public static IContainer getContainer(IResource selected) {
		if (selected.getType() == IResource.FILE)
			return ((IFile)selected).getParent();
		else 
			return (IContainer)selected;
	}

	public static IResource getFirstResource(IStructuredSelection selection){
		return StructuredSelectionUtil.getResources(selection)[0];
	}

	public static Object tryConvertingToJava(IResource resource) {
		IJavaElement je= JavaCore.create(resource);
		if (je != null)
			return je;
		else 
			return resource;	
	}

	public static boolean isOneOpenProject(IResource[] resourceData) {
		return resourceData != null && resourceData.length == 1
			&& resourceData[0].getType() == IResource.PROJECT
			&& ((IProject)resourceData[0]).isOpen();
	}

	//-------------------------------------------------------
	static boolean hasOnlyProjects(IStructuredSelection selection){
		return (! selection.isEmpty() && selection.size() == getSelectedProjects(selection).size());
	}


	static List getSelectedProjects(IStructuredSelection selection) {
		List result= new ArrayList(selection.size());
		for(Iterator iter= selection.iterator(); iter.hasNext(); ) {
			Object element= iter.next();
			if (element instanceof IJavaProject) {
				result.add(((IJavaProject)element).getResource());
			}
		}
		return result;
	}	


	static boolean canActivate(Refactoring ref){
		try {
			return ref.checkActivation(new NullProgressMonitor()).isOK();
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, ReorgMessages.getString("ReorgAction.reorganize"), ReorgMessages.getString("ReorgAction.exception")); //$NON-NLS-2$ //$NON-NLS-1$
			return false;
		}	
	}


	static MultiStatus perform(Refactoring ref) throws JavaModelException{	
		PerformChangeOperation op= new PerformChangeOperation(new CreateChangeOperation(ref, CreateChangeOperation.CHECK_NONE));
		ReorgExceptionHandler handler= new ReorgExceptionHandler();
		op.setChangeContext(new ChangeContext(handler));		
		try {
			//cannot fork - must run in the ui thread
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(false, true, op);
		} catch (InvocationTargetException e) {
			Throwable target= e.getTargetException();
			if (target instanceof CoreException)
				handler.getStatus().merge(((CoreException) target).getStatus());
			JavaPlugin.log(e);	
			//fall thru
		} catch (InterruptedException e) {
			//fall thru
		}
		return handler.getStatus();
	}		

	
}
