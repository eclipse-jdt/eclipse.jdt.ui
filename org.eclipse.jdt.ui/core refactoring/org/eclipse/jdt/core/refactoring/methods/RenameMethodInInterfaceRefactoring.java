/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.core.refactoring.methods;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.refactoring.RefactoringStatus;
import org.eclipse.jdt.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.util.HackFinder;

/**
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public class RenameMethodInInterfaceRefactoring extends RenameMethodRefactoring {

	public RenameMethodInInterfaceRefactoring(ITextBufferChangeCreator changeCreator, IJavaSearchScope scope, IMethod method, String newName){
		super(changeCreator, scope, method, newName);
	}
	
	public RenameMethodInInterfaceRefactoring(ITextBufferChangeCreator changeCreator, IMethod method){
		super(changeCreator, method);
	}
		
	//---- Conditions ---------------------------
		
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 10);
		pm.subTask("checking preconditions");
		RefactoringStatus result= new RefactoringStatus();
		result.merge(super.checkInput(new SubProgressMonitor(pm, 6)));
		pm.subTask("analyzing hierarchy");
		if (isSpecialCase())
			result.addError("Cannot rename this method - it is a special case (see the spec. 9.2)");
		pm.worked(1);
		pm.subTask("analyzing hierarchy");
		if (relatedTypeDeclaresMethodName(new SubProgressMonitor(pm, 3), getMethod(), getNewName()))
			result.addError("A related type declares a method with the new name (and same number of parameters)");
		pm.done();
		return result;
	}
		
	public RefactoringStatus checkPreactivation() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(super.checkPreactivation());
		if (! getMethod().getDeclaringType().isInterface()){
			result.addFatalError("Not applicable to class methods");
		}
		return result;
	}
		
	private boolean relatedTypeDeclaresMethodName(IProgressMonitor pm, IMethod method, String newName) throws JavaModelException{
		pm.beginTask("", 2);
		HashSet types= getRelatedTypes(new SubProgressMonitor(pm, 1));
		Iterator iter= types.iterator();
		int parameterCount= method.getParameterTypes().length;
		while (iter.hasNext()) {
			if (null != findMethod(newName, parameterCount, false, (IType) iter.next())) {
				pm.done();
				return true;
			}
		}
		pm.done();
		return false;
	}

	/**
	 * special cases are mentioned in the java lang spec 2nd edition (9.2)
	 */
	private boolean isSpecialCase() throws JavaModelException {
		String[] noParams= new String[0];
		String[] specialNames= new String[]{"toString", "toString", "toString", "toString", "equals",
											"equals", "getClass", "getClass", "hashCode", "notify",
											"notifyAll", "wait", "wait", "wait"};
		String[][] specialParamTypes= new String[][]{noParams, noParams, noParams, noParams,
													 {"QObject;"}, {"Qjava.lang.Object;"}, noParams, noParams,
													 noParams, noParams, noParams, {Signature.SIG_LONG, Signature.SIG_INT},
													 {Signature.SIG_LONG}, noParams};
		String[] specialReturnTypes= new String[]{"QString;", "QString;", "Qjava.lang.String;", "Qjava.lang.String;",
												   Signature.SIG_BOOLEAN, Signature.SIG_BOOLEAN, "QClass;", "Qjava.lang.Class;",
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
	
	/************ Changes ***************/

	private HashSet getRelatedTypes(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 2);
		IType type= getMethod().getDeclaringType();
		ITypeHierarchy hierarchy= type.newTypeHierarchy(new SubProgressMonitor(pm, 1));
		HashSet result= getRelatedTypes(new HashSet(), hierarchy, type, getMethod(), new SubProgressMonitor(pm, 1));
		pm.done();
		return result;
	}

	private boolean containsNew(IMethod method, IType type) throws JavaModelException {
		IMethod found= findMethod(getNewName(), method.getParameterTypes().length, false, type);
		return (found != null && !Flags.isPrivate(found.getFlags()) && !Flags.isStatic(found.getFlags()));
	}

	private boolean containsOld(IMethod method, IType type) throws JavaModelException {
		IMethod found= findMethod(method, type);
		return (found != null && !Flags.isPrivate(found.getFlags()) && !Flags.isStatic(found.getFlags()));
	}
	
	/**
	 * almost duplicated logic from doGetMethodToRename
	 * need serious rework
	 * see into private/static method issues
	 * needs a better name
	 */ 
	private HashSet getRelatedTypes(HashSet typesVisited, ITypeHierarchy hier, IType type, IMethod method, IProgressMonitor pm) throws JavaModelException {
		
		HackFinder.fixMeSoon("see  comment");
		
		HashSet typeSet= new HashSet();
		typesVisited.add(type);
		typeSet.add(type);
		typeSet.addAll(Arrays.asList(hier.getAllSuperInterfaces(type)));
		HashSet subtypes= findAllSubtypes(hier, type);
		typeSet.addAll(subtypes);
		
		Iterator subtypesIter= subtypes.iterator();
		while (subtypesIter.hasNext()){
			IType each= (IType) subtypesIter.next();
			IType[] superTypes= hier.getAllSupertypes(each);
			for (int i=0; i < superTypes.length; i++){
				if (!typesVisited.contains(superTypes[i])){
					if (containsNew(method, superTypes[i])
						|| containsOld(method, superTypes[i])){
						typeSet.addAll(getRelatedTypes(typesVisited, hier, superTypes[i], method, pm));
					}
					typeSet.add(superTypes[i]);
				}
			}
		}
		return typeSet;
	}
	
	private HashSet findAllSubtypes(ITypeHierarchy hier, IType type){
		IType[] extenders= hier.getSubtypes(type);
		HashSet res= new HashSet(Arrays.asList(extenders));
		for (int i= 0; i < extenders.length; i++){
			res.addAll(findAllSubtypes(hier, extenders[i]));
		}
		return res;
	}
	
	private HashSet getMatchingMethods(IMethod method, HashSet typeSet) throws JavaModelException {
		HashSet methods= new HashSet();
		Iterator iter= typeSet.iterator();
		while (iter.hasNext()){
			IMethod subMethod= findMethod(method, (IType)iter.next());
				if (subMethod != null && !Flags.isPrivate(subMethod.getFlags())){
					methods.add(subMethod);
				}
		}
		return methods;
	}
	
	/**
	 * RenameMethodRefactoring#methodsToRename
	 */
	private HashSet doGetMethodsToRename(HashSet typesVisited, ITypeHierarchy hier, IType type, IMethod method, IProgressMonitor pm) throws JavaModelException {
		
		HackFinder.fixMeSoon("needs serious rethinking and rework");
		
		HashSet typeSet= new HashSet();
		typesVisited.add(type);
		typeSet.addAll(Arrays.asList(hier.getAllSuperInterfaces(type)));
		HashSet subtypes= findAllSubtypes(hier, type);
		typeSet.addAll(subtypes);
		HashSet matchingMethods= new HashSet();
		Iterator subtypesIter= subtypes.iterator();
		while (subtypesIter.hasNext()){
			IType each= (IType) subtypesIter.next();
			IType[] superTypes= hier.getAllSupertypes(each);
			for (int i=0; i < superTypes.length; i++){
				if (!typesVisited.contains(superTypes[i])
					&& containsOld(method, superTypes[i])){
					matchingMethods.addAll(doGetMethodsToRename(typesVisited, hier, superTypes[i], method, pm));
					typeSet.add(superTypes[i]);
				}
			}
		}
		matchingMethods.addAll(getMatchingMethods(method, typeSet));
		return matchingMethods;
	}
	
	/*package*/ HashSet getMethodsToRename(IMethod method, IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 4);
		IType type= method.getDeclaringType();
		ITypeHierarchy hierarchy= type.newTypeHierarchy(new SubProgressMonitor(pm, 1));
		HashSet result= doGetMethodsToRename(new HashSet(), hierarchy, type, method, pm);
		pm.worked(3);
		pm.done();
		return result;
	}
	
}