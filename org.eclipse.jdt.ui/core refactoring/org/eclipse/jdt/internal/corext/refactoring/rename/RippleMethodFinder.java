/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.IWorkingCopy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

/**
 * This class is used to find methods along the 'ripple'. When you rename a method that is declared in an interface,
 * you must also rename its implementations. But because of multiple interface inheritance you have to go up and down the hierarchy 
 * to collect all the methods.
 */
public class RippleMethodFinder {
	
	//no instances
	private RippleMethodFinder(){
	}
	
	//assert(method is defined in the most abstract type that declares it )
	public static IMethod[] getRelatedMethods(IMethod method, IProgressMonitor pm, IWorkingCopy[] workingCopies) throws JavaModelException {
		try{
			if (! MethodChecks.isVirtual(method) && ! method.getDeclaringType().isInterface())
				return new IMethod[]{method};
		
			if (method.getDeclaringType().isInterface())
				return getAllRippleMethods(method, pm, workingCopies);

			return getVirtualMethodsInHierarchy(method, pm, workingCopies);
		} finally{
			pm.done();
		}	
	}
	
		
	/*
	 * We use the following algorithm to find methods to rename:
	 * Input: type T, method m
	   Assumption: No supertype of T declares m
	   Output: variable result contains the list of types that declared the method to be renamed 

	 	result:= empty set // set of types that declare methods to rename
 		visited:= empty set //set of already visited types
	 	q:= empty queue //queue of types to visit
	 	q.insert(T) 

		while (!q.isEmpty()){
			t:= q.remove();
			//assert(t is an interface or declares m as virtual)
			//assert(!visited.contains(t))
			visited.add(t);
			result.add(t);
			forall: i in: t.subTypes do: 
				if ((! visited.contains(i)) && (i declares m)) result.add(i);
			forall: i in: t.subTypes do:
				q.insert(x) 
					where x is any type satisfying the followowing:
					a. x is a supertype of i
					b. x is an interface and declares m or
					    x declares m as a virtual method
					c. no supertype of x is an interface that declares m and
					    no supertype of x is a class that declares m as a virtual method
					d. ! visited.contains(x)
					e. ! q.contains(x)
		}
	 */
	private static IMethod[] getAllRippleMethods(IMethod method, IProgressMonitor pm, IWorkingCopy[] workingCopies) throws JavaModelException {
		pm.beginTask("", 4); //$NON-NLS-1$
		Set result= new HashSet();
		Set visitedTypes= new HashSet();
		List methodQueue= new ArrayList();
		Set hierarchies= new HashSet();
		methodQueue.add(method);
		while (! methodQueue.isEmpty()){
			IMethod m= (IMethod)methodQueue.remove(0);
			
			/* must check for binary - otherwise will go all the way on all types
			 * happens on toString() for example */  
			if (m.isBinary())
				continue; 
			IType type= m.getDeclaringType();
			Assert.isTrue(! visitedTypes.contains(type), "! visitedTypes.contains(type)"); //$NON-NLS-1$
			Assert.isTrue(type.isInterface() || declaresAsVirtual(type, method), "second condition"); //$NON-NLS-1$
			
			visitedTypes.add(type);
			result.add(m);
			
			IType[] subTypes= getAllSubtypes(pm, workingCopies, type, hierarchies);
			for (int i= 0; i < subTypes.length; i++){
				if (!visitedTypes.contains(subTypes[i])){ 
					IMethod subTypeMethod= Checks.findMethod(m, subTypes[i]);
					if (subTypeMethod != null)
						result.add(subTypeMethod);
				}	
			}
			
			for (int i= 0; i < subTypes.length; i++){
				IMethod toAdd= findAppropriateMethod(workingCopies, visitedTypes, methodQueue, subTypes[i], method, new NullProgressMonitor());
				if (toAdd != null)
					methodQueue.add(toAdd);
			}
		}
		return (IMethod[]) result.toArray(new IMethod[result.size()]);
	}
	
	private static IType[] getAllSubtypes(IProgressMonitor pm, IWorkingCopy[] workingCopies, IType type, Set cachedHierarchies) throws JavaModelException {
		//first, try in the cached hierarchies
		for (Iterator iter= cachedHierarchies.iterator(); iter.hasNext();) {
			ITypeHierarchy hierarchy= (ITypeHierarchy) iter.next();
			if (hierarchy.contains(type))
				return hierarchy.getAllSubtypes(type);
		}
		ITypeHierarchy hierarchy= type.newTypeHierarchy(workingCopies, new SubProgressMonitor(pm, 1));
		cachedHierarchies.add(hierarchy);
		return hierarchy.getAllSubtypes(type);
	}
	
	private static IMethod findAppropriateMethod(IWorkingCopy[] workingCopies, Set visitedTypes, List methodQueue, IType type, IMethod method, IProgressMonitor pm)throws JavaModelException{
		pm.beginTask(RefactoringCoreMessages.getString("RippleMethodFinder.analizing_hierarchy"), 1); //$NON-NLS-1$
		IType[] superTypes= type.newSupertypeHierarchy(workingCopies, new SubProgressMonitor(pm, 1)).getAllSupertypes(type);
		for (int i= 0; i< superTypes.length; i++){
			IType t= superTypes[i];
			if (visitedTypes.contains(t))
				continue;
			IMethod found= Checks.findMethod(method, t);	
			if (found == null)
				continue;
			if (! declaresAsVirtual(t, method))	
				continue;	
			if (methodQueue.contains(found))	
				continue;
			return getTopMostMethod(workingCopies, visitedTypes, methodQueue, method, t, new NullProgressMonitor());	
		}
		return null;
	}
	
	private static IMethod getTopMostMethod(IWorkingCopy[] workingCopies, Set visitedTypes, List methodQueue, IMethod method, IType type, IProgressMonitor pm)throws JavaModelException{
		pm.beginTask("", 1); //$NON-NLS-1$
		IMethod methodInThisType= Checks.findMethod(method, type);
		Assert.isTrue(methodInThisType != null);
		IType[] superTypes= type.newSupertypeHierarchy(workingCopies, new SubProgressMonitor(pm, 1)).getAllSupertypes(type);
		for (int i= 0; i < superTypes.length; i++){
			IType t= superTypes[i];
			if (visitedTypes.contains(t))
				continue;
			IMethod found= Checks.findMethod(method, t);	
			if (found == null)
				continue;
			if (! declaresAsVirtual(t, method))	
				continue;
			if (methodQueue.contains(found))	
				continue;
			return getTopMostMethod(workingCopies, visitedTypes, methodQueue, method, t, new NullProgressMonitor());
		}
		return methodInThisType;
	}
	
	private static boolean declaresAsVirtual(IType type, IMethod m) throws JavaModelException{
		IMethod found= Checks.findMethod(m, type);
		if (found == null)
			return false;
		if (JdtFlags.isStatic(found))	
			return false;
		if (JdtFlags.isPrivate(found))	
			return false;	
		return true;	
	}
	
	//---
	private static IMethod[] getVirtualMethodsInHierarchy(IMethod method, IProgressMonitor pm, IWorkingCopy[] workingCopies) throws JavaModelException{
		List methods= new ArrayList();
		//
		methods.add(method);
		//
		IType type= method.getDeclaringType();
		ITypeHierarchy hier= type.newTypeHierarchy(workingCopies, pm);
		IType[] subtypes= hier.getAllSubtypes(type);
		for (int i= 0; i < subtypes.length; i++){
			IMethod subMethod= Checks.findMethod(method, subtypes[i]);
			if (subMethod != null){
				methods.add(subMethod);
			}
		}
		return (IMethod[]) methods.toArray(new IMethod[methods.size()]);
	}
	
}
