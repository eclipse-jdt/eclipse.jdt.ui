package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;

/**
 * This class is used to find methods along the 'ripple' - e.g. when you rename a method that is declared in an interface,
 * you must also rename its implementations. But because of multiple interface inheritance you have to go up and down the hierarchy 
 * to collect all the methods.
 */
class RippleMethodFinder {
	
	//no instances
	private RippleMethodFinder(){
	}
	
	//assert(method is defined in the most abstract type that declares it )
	static IMethod[] getRelatedMethods(IMethod method, IProgressMonitor pm) throws JavaModelException {
		try{
			if (Flags.isPrivate(method.getFlags()))
				return new IMethod[]{method};
		
			if (Flags.isStatic(method.getFlags()))
				return new IMethod[]{method};
		
			if (method.getDeclaringType().isInterface())
				return getAllRippleMethods(method, pm);

			return getVirtualMethodsInHierarchy(method, pm);
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
	private static IMethod[] getAllRippleMethods(IMethod method, IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 4); //$NON-NLS-1$
		Set result= new HashSet();
		Set visitedTypes= new HashSet();
		List methodQueue= new ArrayList();
		methodQueue.add(method);
		while (! methodQueue.isEmpty()){
			IMethod m= (IMethod)methodQueue.remove(0);
			
			/* must check for binary - otherwise will go all the way on all types
			 * happens on toString() for example */  
			if (m.isBinary())
				continue; 
			IType type= m.getDeclaringType();
			Assert.isTrue(! visitedTypes.contains(type), "! visitedTypes.contains(type)");
			Assert.isTrue(type.isInterface() || declaresAsVirtual(type, method), "second condition");
			
			visitedTypes.add(type);
			result.add(m);
			
			IType[] subTypes= type.newTypeHierarchy(new SubProgressMonitor(pm, 1)).getAllSubtypes(type);
			for (int i= 0; i < subTypes.length; i++){
				if (!visitedTypes.contains(subTypes[i]) && declares(subTypes[i], method)){
					result.add(Checks.findMethod(m, subTypes[i]));
				}	
			}
			
			for (int i= 0; i < subTypes.length; i++){
				IMethod toAdd= findAppropriateMethod(visitedTypes, methodQueue, subTypes[i], method, new NullProgressMonitor());
				if (toAdd != null)
					methodQueue.add(toAdd);
			}
		}
		return (IMethod[]) result.toArray(new IMethod[result.size()]);
	}
	 
		
	private static IMethod findAppropriateMethod(Set visitedTypes, List methodQueue, IType type, IMethod method, IProgressMonitor pm)throws JavaModelException{
		pm.beginTask("analizing hierarchy", 1);
		IType[] superTypes= type.newSupertypeHierarchy(new SubProgressMonitor(pm, 1)).getAllSupertypes(type);
		for (int i= 0; i< superTypes.length; i++){
			IType x= superTypes[i];
			if (visitedTypes.contains(x))
				continue;
			IMethod found= Checks.findMethod(method, x);	
			if (found == null)
				continue;
			if (! declaresAsVirtual(x, method))	
				continue;	
			if (methodQueue.contains(found))	
				continue;
			return getTopMostMethod(visitedTypes, methodQueue, method, x, new NullProgressMonitor());	
		}
		return null;
	}
	
	private static IMethod getTopMostMethod(Set visitedTypes, List methodQueue, IMethod method, IType type, IProgressMonitor pm)throws JavaModelException{
		pm.beginTask("", 1);
		Assert.isTrue(Checks.findMethod(method, type) != null);
		IType[] superTypes= type.newSupertypeHierarchy(new SubProgressMonitor(pm, 1)).getAllSupertypes(type);
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
			return getTopMostMethod(visitedTypes, methodQueue, method, t, new NullProgressMonitor());
		}
		return Checks.findMethod(method, type);
	}
	
	private static boolean declares(IType type, IMethod m) throws JavaModelException{
		return Checks.findMethod(m, type) != null;
	}
	
	private static boolean declaresAsVirtual(IType type, IMethod m) throws JavaModelException{
		IMethod found= Checks.findMethod(m, type);
		if (found == null)
			return false;
		int flags= found.getFlags();	
		if (Flags.isStatic(flags))	
			return false;
		if (Flags.isPrivate(flags))	
			return false;	
		return true;	
	}
	
	//---
	private static IMethod[] getVirtualMethodsInHierarchy(IMethod method, IProgressMonitor pm) throws JavaModelException{
		List methods= new ArrayList();
		IType type= method.getDeclaringType();
		ITypeHierarchy hier= type.newTypeHierarchy(pm);
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
