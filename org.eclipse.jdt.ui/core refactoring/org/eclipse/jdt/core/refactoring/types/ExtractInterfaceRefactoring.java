/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.core.refactoring.types;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.refactoring.IChange;
import org.eclipse.jdt.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.CreateCompilationUnitChange;
import org.eclipse.jdt.internal.core.util.HackFinder;

/**
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public class ExtractInterfaceRefactoring extends TypeRefactoring {
	
	private String fInterfaceName;
	private IMethod[] fMethods;
	
	public ExtractInterfaceRefactoring(IType type) {
		super(type);
	}
	
	public String getName() {
		return "Extract Interface";
	}
	
	public void setInterfaceName(String name) {
		Assert.isNotNull(name);
		fInterfaceName= name;
	}
	
	private String getNewCompilationUnitName(){
		return fInterfaceName + ".java";
	}
	
	private void checkMethods(IMethod[] methods){
		Assert.isNotNull(methods);
		try{
			for (int i= 0; i < methods.length; i++){
				Assert.isTrue(methods[i].exists(), "method must exist");
				Assert.isTrue(methods[i].getDeclaringType() == getType(), "method must be declared in this type");
				Assert.isTrue(Flags.isPublic(methods[i].getFlags()), "method must be public");
				Assert.isTrue(! Flags.isStatic(methods[i].getFlags()), "method must not be static");
			}	
		} catch (JavaModelException e){
			//do nothing ?
		}
	}
	
	public final void setMethods(IMethod[] methods){
		checkMethods(methods);
		fMethods= methods;
	}
	
	// ------------ Preconditions -----------------
	
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask("", 2);
		Assert.isNotNull(getType());
		result.merge(checkAvailability(getType()));
		pm.worked(1);
		if (! getType().isClass() || !Checks.isTopLevel(getType()) || !Flags.isPublic(getType().getFlags()))
			result.addFatalError("Only applicable to top-level public classes");
		pm.worked(1);
		pm.done();
		return result;
	}

	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		if (fMethods == null || fMethods.length == 0)
			result.addError("No methods selected");
		if (resourceExists(getNewFilePath(getType(), fInterfaceName)))
			result.addFatalError("Cannot create an interface named " + fInterfaceName + " - compilation unit name is already used by another file in this directory");
		if (typeNameExistsInPackage(getType().getPackageFragment(), fInterfaceName))
			result.addFatalError("Type " + fInterfaceName + " already exists in package " + getType().getPackageFragment().getElementName());	

		HackFinder.fixMeSoon("must analyze import declarations");	
		
		return result;
	}
	
	public RefactoringStatus checkUserInput(){
		//methods are already checked in setMethods
		
		Assert.isNotNull(getType(), "type");
		Assert.isNotNull(fInterfaceName, "new name");
		RefactoringStatus result= new RefactoringStatus();
		result.merge(Checks.checkTypeName(fInterfaceName));
		return result;
	}
	
	// ------------ Changes -----------------
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		return new CreateCompilationUnitChange(getType().getPackageFragment(), computeSource(pm), getNewCompilationUnitName());
	}
	
	private String computeSource(IProgressMonitor pm) throws JavaModelException {
		StringBuffer buff= new StringBuffer();
		addHeader(buff);
		addImports(buff);
		buff.append("public interface " + fInterfaceName + " {\n\n");
		addMethods(buff);
		buff.append("}");
		return buff.toString();
	}
	
	private void addHeader(StringBuffer buff) {
		buff.append("package " + getType().getPackageFragment().getElementName())
		    .append(";\n\n");
	}
	
	private void addImports(StringBuffer buff) throws JavaModelException {
		//BOGUS!
		buff.append(getType().getCompilationUnit().getImportContainer().getSource())
		    .append("\n\n");
	}
	
	private void addMethods(StringBuffer buff) throws JavaModelException {
		for (int i= 0; i < fMethods.length; i++){
			buff.append("\t")
			    .append(getMethod(fMethods[i]))
			    .append("\n\n");
		}
	}
	
	private String getMethod(IMethod method) throws JavaModelException {
		StringBuffer buff= new StringBuffer();
		return buff.append("public ")
			     .append(Signature.toString(method.getReturnType()))
			     .append(" ")
		    	     .append(method.getElementName())
		    	     .append("(")
		    	     .append(getMethodParameters(method))
		    	     .append(")")
		    	     .append(getExceptions(method))
		    	     .append(";")
		    	     .toString();
	}
	
	private String getMethodParameters(IMethod method) throws JavaModelException {
		String[] paramNames= method.getParameterNames();
		String[] paramTypes= method.getParameterTypes();
		if (paramNames == null || paramTypes == null)		 
			return "";
		Assert.isTrue(paramNames.length == paramTypes.length);
		StringBuffer buff= new StringBuffer();
		for (int i= 0; i < paramNames.length; i++){
			buff.append(Signature.toString(paramTypes[i]))
			    .append(" ")
			    .append(paramNames[i]);
			if (i+1 < paramNames.length)
				buff.append(", "); 
		}
		return buff.toString();
	}
	
	private String getExceptions(IMethod method) throws JavaModelException {
		String[] exceptions= method.getExceptionTypes();
		if (exceptions == null || exceptions.length == 0)
			return "";
		StringBuffer buff= new StringBuffer();	
		buff.append(" throws ");	
		for (int i= 0; i < exceptions.length; i++){
			buff.append(Signature.toString(exceptions[i]));
			if (i+1 < exceptions.length)
				buff.append(", ");
		}	
		return buff.toString();
	}
}