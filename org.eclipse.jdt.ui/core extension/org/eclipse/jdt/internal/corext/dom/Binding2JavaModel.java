/*
 * (c) Copyright IBM Corp. 2000, 2001, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import org.eclipse.jdt.internal.corext.Assert;

/**
 * A helper class to convert compiler bindings into corresponding 
 * Java elements.
 */
public class Binding2JavaModel {
	
	private Binding2JavaModel(){}
	
	/**
	 * Converts the given <code>ReferenceBinding</code> into a <code>IType</code>
	 * using the classpath defined by the given Java project. Returns <code>null</code>
	 * if the conversion isn't possible.
	 */
	public static IType find(ITypeBinding type, IJavaProject scope) throws JavaModelException {
		if (type.isPrimitive())
			return null;
		String[] typeElements= Bindings.getNameComponents(type);
		IJavaElement element= scope.findElement(getPathToCompilationUnit(type.getPackage(), typeElements[0]));
		IType candidate= null;
		if (element instanceof ICompilationUnit) {
			candidate= ((ICompilationUnit)element).getType(typeElements[0]);
		} else if (element instanceof IClassFile) {
			candidate= ((IClassFile)element).getType();
		}
		
		if (candidate == null || typeElements.length == 1)
			return candidate;
			
		return findTypeInType(typeElements, candidate);
	}

	/**
	 * Finds the given <code>MethodBinding</code> in the given <code>IType</code>. Returns
	 * <code>null</code> if the type doesn't contain a corresponding method.
	 */
	public static IMethod find(IMethodBinding method, IType type) throws JavaModelException {
		IMethod[] candidates= type.getMethods();
		for (int i= 0; i < candidates.length; i++) {
			IMethod candidate= candidates[i];
			if (candidate.getElementName().equals(method.getName()) && sameParameters(method, candidate)) {
				return candidate;
			}
		}
		return null;
	}

//	public static IMethod lookupIMethod(IMethodBinding method, IType type) throws JavaModelException {
//		return find(method, type);
//	}
	
	public static IMethod find(IMethodBinding method, IJavaProject scope) throws JavaModelException {
		IType type= find(method.getDeclaringClass(), scope);
		if (type == null)
			return null;
		return find(method, type);	
	}
	
	//---- Helper methods to convert a type --------------------------------------------
	
	private static IPath getPathToCompilationUnit(IPackageBinding packageBinding, String topLevelTypeName) {
		IPath result= new Path(""); //$NON-NLS-1$
		String[] packageNames= packageBinding.getNameComponents();
		for (int i= 0; i < packageNames.length; i++) {
			result= result.append(packageNames[i]);
		}
		return result.append(topLevelTypeName + ".java"); //$NON-NLS-1$
	}
	
	private static IType findTypeInType(String[] typeElements, IType jmType) {
		IType result= jmType;
		for (int i= 1; i < typeElements.length; i++) {
			result= result.getType(typeElements[i]);
			if (!result.exists())
				return null;
		}
		return result == jmType ? null : result;
	}
	
	private static String getQualifiedName(char[][] compoundName, int start, int length, char separator) {
		StringBuffer buffer= new StringBuffer();
		int lastSlash= length - 1;
		int end= Math.min(compoundName.length, start + length);
		for (int i= start; i < end; i++) {
			buffer.append(compoundName[i]);
			if (i < lastSlash)
				buffer.append(separator);
		}
		return buffer.toString();
	}
	
	//---- Helper methods to convert a method ---------------------------------------------
	
	private static boolean sameParameters(IMethodBinding method, IMethod candidate) throws JavaModelException {
		ITypeBinding[] methodParamters= method.getParameterTypes();
		String[] candidateParameters= candidate.getParameterTypes();
		if (methodParamters.length != candidateParameters.length)
			return false;
		IType scope= candidate.getDeclaringType();
		for (int i= 0; i < methodParamters.length; i++) {
			ITypeBinding methodParameter= methodParamters[i];
			String candidateParameter= candidateParameters[i];
			if (!sameParameter(methodParameter, candidateParameter, scope))
				return false;
		}
		return true;
	}
	
	private static boolean sameParameter(ITypeBinding type, String candidate, IType scope) throws JavaModelException {
		if (type.getDimensions() != Signature.getArrayCount(candidate))
			return false;
			
		// Normalizes types
		if (type.isArray())
			type= type.getElementType();
		candidate= Signature.getElementType(candidate);
		
		if (isPrimitiveType(candidate)) {
			return type.getName().equals(Signature.toString(candidate));
		} else {
			if (isResolvedType(candidate)) {
				return Signature.toString(candidate).equals(Bindings.getFullyQualifiedName(type));
			} else {
				String[][] qualifiedCandidates= scope.resolveType(Signature.toString(candidate));
				if (qualifiedCandidates == null || qualifiedCandidates.length == 0l)
					return false;
				String packageName= type.getPackage().getName();
				String typeName= Bindings.getTypeQualifiedName(type);
				for (int i= 0; i < qualifiedCandidates.length; i++) {
					String[] qualifiedCandidate= qualifiedCandidates[i];
					if (	qualifiedCandidate[0].equals(packageName) &&
							qualifiedCandidate[1].equals(typeName))
						return true;
				}
			}
		}
		return false;
	}
	
	private static boolean isPrimitiveType(String s) {
		char c= s.charAt(0);
		return c != Signature.C_RESOLVED && c != Signature.C_UNRESOLVED;
	}
	
	private static boolean isResolvedType(String s) {
		int arrayCount= Signature.getArrayCount(s);
		return s.charAt(arrayCount) == Signature.C_RESOLVED;
	}	

//----
	public static IField lookupIField(IVariableBinding field, IJavaProject in) throws JavaModelException {
		Assert.isTrue(field.isField());
		IType declaringClass = lookupIType(field.getDeclaringClass(), in);
		if (declaringClass == null)
			return null;
	    return declaringClass.getField(field.getName());
	}
//
//  public static IMethod lookupIMethod(IMethodBinding method, IJavaProject in) throws JavaModelException {
//     IType declaringClass= lookupIType(method.getDeclaringClass(), in);
//     if (declaringClass == null) 
//     	return null;
//	return declaringClass.getMethod(method.getName(), getParameterTypeSignatures(method));
//  }
//
	private static IType lookupIType(ITypeBinding type, IJavaProject in) throws JavaModelException {
	    String fullyQualifiedName= getFullyQualifiedName(type);
	    if (fullyQualifiedName == null)
	    	return null;
	     return in.findType(fullyQualifiedName);
	}

	private static String getFullyQualifiedName(ITypeBinding type) {
	    if (type.isAnonymous()) 
	      return null;
	    StringBuffer buff=  new StringBuffer();
	    buff.append(getPackageName(type))
	    	 .append(getTypeQualifiedName(type))
	    	 .append(type.getName());
	    return buff.toString();
	}

	private static String getTypeQualifiedName(ITypeBinding type){
		StringBuffer buff= new StringBuffer();
	    ITypeBinding declaringType = type;
	    while (declaringType.isNested()) {
	      declaringType = declaringType.getDeclaringClass();
	      buff.append(declaringType.getName()).append(".");
	    }
	    return buff.toString();
	}
	
	private static ITypeBinding getBaseType(ITypeBinding type) {
		return (type.isArray() ? type.getElementType() : type);
	}
	
	private static String getPackageName(ITypeBinding type){
	    IPackageBinding typePackage = getBaseType(type).getPackage();
	    if (typePackage == null || typePackage.isUnnamed())
	    	return "";
		return typePackage.getName() + ".";
	}

//	private static String[] getParameterTypeSignatures(IMethodBinding method){
//		String[] parameterTypeSignatures = new String[method.getParameterTypes().length];
//		for (int i = 0; i < parameterTypeSignatures.length; i++) {
//			parameterTypeSignatures[i]= Signature.createTypeSignature(getFullyQualifiedName(method.getParameterTypes()[i]), true);
//		}
//		return parameterTypeSignatures;
//	}
//
}

