package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;

public class ResourceUtil {
	
	private ResourceUtil(){
	}
	
	public static IFile[] getFiles(ICompilationUnit[] cus) throws JavaModelException{
		List files= new ArrayList(cus.length);
		for (int i= 0; i < cus.length; i++) {
			IResource resource= Refactoring.getResource(cus[i]);
			if (resource.getType() == IResource.FILE)
				files.add(resource);
		}
		return (IFile[]) files.toArray(new IFile[files.size()]);
	}

}
