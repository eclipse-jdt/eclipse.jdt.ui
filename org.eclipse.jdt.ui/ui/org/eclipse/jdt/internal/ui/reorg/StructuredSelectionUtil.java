package org.eclipse.jdt.internal.ui.reorg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;

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
			IResource resource= ResourceUtil.getResource(iter.next());
			if (resource != null)
				result.add(resource);
		}
		return result;
	}
}
