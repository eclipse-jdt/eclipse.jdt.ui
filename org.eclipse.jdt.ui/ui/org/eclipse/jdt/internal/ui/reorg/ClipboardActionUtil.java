package org.eclipse.jdt.internal.ui.reorg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgRefactoring;
import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;

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

	public static JdtCopyAction createDnDCopyAction(List elems, final IResource destination){
		StructuredSelectionProvider provider= StructuredSelectionProvider.createFrom(new SimpleSelectionProvider(elems.toArray()));
		JdtCopyAction action= new JdtCopyAction("#PASTE", provider){
			protected Object selectDestination(ReorgRefactoring ref) {
				return tryConvertingToJava(destination);			
			}
		};
		return action;
	}
	
	public static JdtCopyAction createDnDCopyAction(IResource[] resourceData, final IResource destination){
		return createDnDCopyAction(getConvertedResources(resourceData), destination);
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
	
	private static class SimpleSelectionProvider implements ISelectionProvider {
		private Object[] fElems;
		SimpleSelectionProvider(Object[] elements){
			fElems= elements;
		}
		
		public void addSelectionChangedListener(ISelectionChangedListener listener) {
		}
	
		public ISelection getSelection() {
			return new StructuredSelection(fElems);
		}
	
		public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		}
	
		public void setSelection(ISelection selection) {
		}
	}
}
