/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.util.CharOperation;

/**
 * A helper class to convert compiler bindings into corresponding 
 * Java elements.
 */
public class Binding2JavaModel {

	/**
	 * Converts the given <code>ReferenceBinding</code> into a <code>IType</code>
	 * using the classpath defined by the given Java project. Returns <code>null</code>
	 * if the conversion isn't possible.
	 */
	public static IType find(ReferenceBinding type, IJavaProject scope) throws JavaModelException {
		PackageBinding packageBinding= type.fPackage;
		int packageComponents= packageBinding.compoundName.length;
		int typeComponents= type.compoundName.length;
		IJavaElement element= scope.findElement(getPathToCompilationUnit(type));
		if (element instanceof ICompilationUnit) {
			IType candidate= ((ICompilationUnit)element).getType(new String(type.compoundName[packageComponents]));
			if (packageComponents + 1 == typeComponents)
				return candidate;
			return findTypeInType(type, candidate); 
		}
		if (element instanceof IClassFile) {
			IType candidate= ((IClassFile)element).getType();
			if (packageComponents + 1 == typeComponents)
				return candidate;
			return findTypeInType(type, candidate);
		}
			
		return null;
	}

	/**
	 * Finds the given <code>MethodBinding</code> in the given <code>IType</code>. Returns
	 * <code>null</code> if the type doesn't contains a corresponding method.
	 */
	public static IMethod find(MethodBinding method, IType type) throws JavaModelException {
		IMethod[] candidates= type.getMethods();
		for (int i= 0; i < candidates.length; i++) {
			IMethod candidate= candidates[i];
			if (Strings.equals(candidate.getElementName(), method.selector) && sameParameters(method, candidate)) {
				return candidate;
			}
		}
		return null;
	}
	
	//---- Helper methods to convert a type --------------------------------------------
	
	private static IPath getPathToCompilationUnit(ReferenceBinding type) {
		PackageBinding packageBinding= type.fPackage;
		IPath result= new Path("");
		for (int i= 0; i < packageBinding.compoundName.length; i++) {
			char[] element= packageBinding.compoundName[i];
			result= result.append(new String(element));
		}
		if (type instanceof SourceTypeBinding) {
			result= result.append(new String(((SourceTypeBinding)type).getFileName()));
		} else {
			result= result.append(new String(type.compoundName[packageBinding.compoundName.length]) + ".java");
		}
		return result;
	}
	
	private static IType findTypeInType(ReferenceBinding type, IType jmType) {
		int start= type.fPackage.compoundName.length;
		char[][] compoundName= type.compoundName;
		IType result= jmType;
		for (int i= start + 1; i < compoundName.length; i++) {
			result= result.getType(new String(compoundName[i]));
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
	
	private static boolean sameParameters(MethodBinding method, IMethod candidate) throws JavaModelException {
		TypeBinding[] methodParamters= method.parameters;
		String[] candidateParameters= candidate.getParameterTypes();
		if (methodParamters.length != candidateParameters.length)
			return false;
		IType scope= candidate.getDeclaringType();
		for (int i= 0; i < methodParamters.length; i++) {
			TypeBinding methodParameter= methodParamters[i];
			String candidateParameter= candidateParameters[i];
			if (!sameParameter(methodParameter, candidateParameter, scope))
				return false;
		}
		return true;
	}
	
	private static boolean sameParameter(TypeBinding type, String candidate, IType scope) throws JavaModelException {
		if (isBaseType(candidate)) {
			return Strings.equals(Signature.toString(candidate), type.qualifiedSourceName());
		} else {
			if (isResolvedType(candidate)) {
				return Signature.toString(candidate).equals(qualifiedName(type));
			} else {
				String[][] qualifiedCandidates= scope.resolveType(Signature.toString(candidate));
				if (qualifiedCandidates == null)
					return false;
				for (int i= 0; i < qualifiedCandidates.length; i++) {
					String[] qualifiedCandidate= qualifiedCandidates[i];
					if (	Strings.equals(qualifiedCandidate[0], type.qualifiedPackageName()) &&
							Strings.equals(qualifiedCandidate[1], type.qualifiedSourceName()))
						return true;
				}
			}
		}
		return false;
	}
	
	private static boolean isBaseType(String s) {
		int arrayCount= Signature.getArrayCount(s);
		char c= s.charAt(arrayCount);
		return c != Signature.C_RESOLVED && c != Signature.C_UNRESOLVED;
	}
	
	private static boolean isResolvedType(String s) {
		int arrayCount= Signature.getArrayCount(s);
		return s.charAt(arrayCount) == Signature.C_RESOLVED;
	}
	
	private static String qualifiedName(TypeBinding type) {
		StringBuffer result= new StringBuffer();
		result.append(type.qualifiedPackageName());
		result.append('.');
		result.append(type.qualifiedSourceName());
		return result.toString();
	}
}

