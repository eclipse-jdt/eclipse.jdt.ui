package org.eclipse.jdt.internal.ui.reorg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;

class StructuredSelectionUtil {
	
	private StructuredSelectionUtil(){
	}
	
	static boolean hasNonResources(IStructuredSelection ss) {
		return getResources(ss).length != ss.size();
	}

	static IResource[] getResources(IStructuredSelection ss){
		List selectedResources= getResourceList(ss);
		return ((IResource[]) selectedResources.toArray(new IResource[selectedResources.size()]));		
	}

	private static List getResourceList(IStructuredSelection ss) {
		if (ss == null)
			return new ArrayList(0);
		List result= new ArrayList(0);
		for (Iterator iter= ss.iterator(); iter.hasNext();) {
			IResource resource= getResource(iter.next());
			if (resource != null)
				result.add(resource);
		}
		return result;
	}

	private static IResource getResource(Object o){
		if (o instanceof IResource)
			return (IResource)o;
		if (o instanceof IJavaElement)
			return getResource((IJavaElement)o);
		return null;	
	}

	private static IResource getResource(IJavaElement element){
		if (! element.exists())
			return null;
		try {
			if (element.getCorrespondingResource() != null)
				return element.getCorrespondingResource();
			if (element.getElementType() == IJavaElement.COMPILATION_UNIT)	
				return Refactoring.getResource((ICompilationUnit)element);
			return null;
		} catch(JavaModelException e) {
			//no action - simply do not put to clipboard
			return null;
		}	
	}







}
