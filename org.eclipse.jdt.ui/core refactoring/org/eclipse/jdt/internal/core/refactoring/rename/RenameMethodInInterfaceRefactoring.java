/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.rename;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
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
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;

class RenameMethodInInterfaceRefactoring extends RenameMethodRefactoring {

	RenameMethodInInterfaceRefactoring(IMethod method){
		super(method);
	}
		
	//---- preconditions ---------------------------
	
	/* non java-doc
	 * @see IPreactivatedRefactoring#checkPreactivation
	 */	
	public RefactoringStatus checkPreactivation() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(super.checkPreactivation());
		if (! getMethod().getDeclaringType().isInterface())
			result.addFatalError(RefactoringCoreMessages.getString("RenameMethodInInterfaceRefactoring.no_class_method")); //$NON-NLS-1$
		return result;
	}
	
	/* non java-doc
	 * @see Refactoring#checkInput
	 */	
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 11); //$NON-NLS-1$
		try{
			pm.subTask(RefactoringCoreMessages.getString("RenameMethodInInterfaceRefactoring.checking")); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			result.merge(super.checkInput(new SubProgressMonitor(pm, 6)));
			pm.subTask(RefactoringCoreMessages.getString("RenameMethodInInterfaceRefactoring.analyzing_hierarchy")); //$NON-NLS-1$
			if (isSpecialCase())
				result.addError(RefactoringCoreMessages.getString("RenameMethodInInterfaceRefactoring.special_case")); //$NON-NLS-1$
			pm.worked(1);
			pm.subTask(RefactoringCoreMessages.getString("RenameMethodInInterfaceRefactoring.analyzing_hierarchy")); //$NON-NLS-1$
			if (relatedTypeDeclaresMethodName(new SubProgressMonitor(pm, 3), getMethod(), getNewName()))
				result.addError(RefactoringCoreMessages.getString("RenameMethodInInterfaceRefactoring.already_defined")); //$NON-NLS-1$
			if (overridesAnotherMethod(new SubProgressMonitor(pm, 1)))
				result.addError("This method overrides another method from - please rename it in the base type.");
			return result;
		} finally {
			pm.done();
		}
	}
		
	private boolean relatedTypeDeclaresMethodName(IProgressMonitor pm, IMethod method, String newName) throws JavaModelException{
		try{
			pm.beginTask("", 2); //$NON-NLS-1$
			Set types= getRelatedTypes(new SubProgressMonitor(pm, 1));
			for (Iterator iter= types.iterator(); iter.hasNext(); ) {
				IMethod m= Checks.findMethod(method, (IType)iter.next());
				if (hierarchyDeclaresMethodName(new SubProgressMonitor(pm, 1), m, newName))
					return true;
			}
			return false;
		} finally {
			pm.done();
		}	
	}
	/*
	 * special cases are mentioned in the java lang spec 2nd edition (9.2)
	 */
	private boolean isSpecialCase() throws JavaModelException {
		String[] noParams= new String[0];
		String[] specialNames= new String[]{"toString", "toString", "toString", "toString", "equals", //$NON-NLS-5$ //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
											"equals", "getClass", "getClass", "hashCode", "notify", //$NON-NLS-5$ //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
											"notifyAll", "wait", "wait", "wait"}; //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
		String[][] specialParamTypes= new String[][]{noParams, noParams, noParams, noParams,
													 {"QObject;"}, {"Qjava.lang.Object;"}, noParams, noParams, //$NON-NLS-2$ //$NON-NLS-1$
													 noParams, noParams, noParams, {Signature.SIG_LONG, Signature.SIG_INT},
													 {Signature.SIG_LONG}, noParams};
		String[] specialReturnTypes= new String[]{"QString;", "QString;", "Qjava.lang.String;", "Qjava.lang.String;", //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
												   Signature.SIG_BOOLEAN, Signature.SIG_BOOLEAN, "QClass;", "Qjava.lang.Class;", //$NON-NLS-2$ //$NON-NLS-1$
												   Signature.SIG_INT, Signature.SIG_VOID, Signature.SIG_VOID, Signature.SIG_VOID,
												   Signature.SIG_VOID, Signature.SIG_VOID};
		Assert.isTrue((specialNames.length == specialParamTypes.length) && (specialParamTypes.length == specialReturnTypes.length));
		for (int i= 0; i < specialNames.length; i++){
			if (specialNames[i].equals(getNewName()) 
				&& Checks.compareParamTypes(getMethod().getParameterTypes(), specialParamTypes[i]) 
				&& !specialReturnTypes[i].equals(getMethod().getReturnType())){
					return true;
			}
		}
		return false;		
	}
	
	private Set getRelatedTypes(IProgressMonitor pm) throws JavaModelException {
		Set methods= getMethodsToRename(getMethod(), pm);
		Set result= new HashSet(methods.size());
		for (Iterator iter= methods.iterator(); iter.hasNext(); ){
			result.add(((IMethod)iter.next()).getDeclaringType());
		}
		return result;
	}
	private boolean overridesAnotherMethod(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 1); //$NON-NLS-1$
			ITypeHierarchy hierarchy= getMethod().getDeclaringType().newSupertypeHierarchy(new SubProgressMonitor(pm, 1));
			IType[] supertypes= hierarchy.getAllSupertypes(getMethod().getDeclaringType());
				for (int i= 0; i < supertypes.length; i++){
					IMethod found= Checks.findMethod(getMethod(), supertypes[i]);
					if (found != null) 
						return true;
				}
			return false;
		} finally{
			pm.done();
		}	
	}
	
	//--------------
	
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
	
	/* non java-doc
	 * method declared in RenameMethodRefactoring
	 */ 
	Set getMethodsToRename(final IMethod method, IProgressMonitor pm) throws JavaModelException {
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
		return result;
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
}