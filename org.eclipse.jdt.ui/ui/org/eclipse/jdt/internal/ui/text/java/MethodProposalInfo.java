/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

 
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.text.Assert;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.template.java.SignatureUtil;

import org.eclipse.jdt.ui.JavadocContentAccess;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDoc2HTMLTextReader;


/**
 * Proposal info that computes the javadoc lazily when it is queried.
 * <p>
 * TODO this class only subclasses ProposalInfo to be compatible - it does not
 * use any thing from it.
 * </p>
 * 
 * @since 3.1
 */
public final class MethodProposalInfo extends ProposalInfo {
	
	/* configuration received from the completion proposal */
	private final IJavaProject fJavaProject;
	private final char[] fName;
	private final char[] fSignature;
	private final char[] fDeclarationSignature;
	
	/* cache filled lazily */
	private boolean fMemberResolved= false;
	private IMember fMember= null;

	/**
	 * Creates a new proposal info.
	 * 
	 * @param project the java project to reference when resolving types
	 * @param proposal the proposal to generate information for
	 */
	public MethodProposalInfo(IJavaProject project, CompletionProposal proposal) {
		super(null);
		Assert.isNotNull(project);
		Assert.isNotNull(proposal);
		fJavaProject= project;
		// TODO copy values if it turns out that the values returned from the proposal are not to be kept  
		fName= proposal.getName();
		fSignature= proposal.getSignature();
		fDeclarationSignature= proposal.getDeclarationSignature();
	}
	
	/**
	 * Returns the member for described by the receiver, resolving the
	 * corresponding type and member if necessary.
	 * 
	 * @return the member described by the receiver
	 * @throws JavaModelException if accessing the java model fails
	 */
	protected final IMember getMember() throws JavaModelException {
		if (!fMemberResolved) {
			fMemberResolved= true;
			fMember= resolveMember();
		}
		
		return fMember;
	}

	/**
	 * Resolves the member described by the receiver and returns it if found.
	 * Returns <code>null</code> if no corresponding member can be found.
	 * 
	 * @return the resolved member or <code>null</code> if none is found
	 * @throws JavaModelException if accessing the java model fails
	 */
	protected final IMember resolveMember() throws JavaModelException {
		String typeName= SignatureUtil.stripSignatureToFQN(String.valueOf(fDeclarationSignature));
		IType type= fJavaProject.findType(typeName);
		if (type != null) {
			String name= String.valueOf(fName);
			String[] parameters= Signature.getParameterTypes(String.valueOf(SignatureUtil.fix83600(fSignature)));
			for (int i= 0; i < parameters.length; i++) {
				parameters[i]= SignatureUtil.getLowerBound(parameters[i]);
			}
			boolean isConstructor= false;
			
			return findMethod(name, parameters, isConstructor, type);
		}
		
		return null;
	}
	
	/**
	 * Gets the text for this proposal info formatted as HTML, or
	 * <code>null</code> if no text is available.
	 */	
	public String getInfo() {
		try {
			return extractJavadoc(getMember());
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		} catch (IOException e) {
			JavaPlugin.log(e);
		}
		return null;
	}

	/**
	 * Extracts the javadoc for the given <code>IMember</code> and returns it
	 * as HTML.
	 * 
	 * @param member the member to get the documentation for
	 * @return the javadoc for <code>member</code> or <code>null</code> if
	 *         it is not available
	 * @throws JavaModelException if accessing the javadoc fails
	 * @throws IOException if reading the javadoc fails
	 */
	protected final String extractJavadoc(IMember member) throws JavaModelException, IOException {
		if (member != null) {
			Reader reader= JavadocContentAccess.getContentReader(member, true);
			if (reader != null) {
				return new JavaDoc2HTMLTextReader(reader).getString();
			}
		}
		return null;
	}
	
	/* adapted from JavaModelUtil */
	
	/**
	 * Finds a method in a type. This searches for a method with the same name
	 * and signature. Parameter types are only compared by the simple name, no
	 * resolving for the fully qualified type name is done. Constructors are
	 * only compared by parameters, not the name.
	 * 
	 * @param name The name of the method to find
	 * @param paramTypes The type signatures of the parameters e.g.
	 *        <code>{"QString;","I"}</code>
	 * @param isConstructor If the method is a constructor
	 * @return The first found method or <code>null</code>, if nothing found
	 */
	private IMethod findMethod(String name, String[] paramTypes, boolean isConstructor, IType type) throws JavaModelException {
		Map typeVariables= computeTypeVariables(type);
		return findMethod(name, paramTypes, isConstructor, type.getMethods(), typeVariables);
	}
	
	/**
	 * The type and method signatures received in
	 * <code>CompletionProposals</code> of type <code>METHOD_REF</code>
	 * contain concrete type bounds. When comparing parameters of the signature
	 * with an <code>IMethod</code>, we have to make sure that we match the
	 * case where the formal method declaration uses a type variable which in
	 * the signature is already substituted with a concrete type (bound).
	 * <p>
	 * This method creates a map from type variable names to type signatures
	 * based on the position they appear in the type declaration. The type
	 * signatures are filtered through
	 * {@link SignatureUtil#getLowerBound(char[])}.
	 * </p>
	 * 
	 * @param type the type to get the variables from
	 * @return a map from type variables to concrete type signatures
	 * @throws JavaModelException if accessing the java model fails
	 */
	private Map computeTypeVariables(IType type) throws JavaModelException {
		char[][] concreteParameters= Signature.getTypeArguments(fDeclarationSignature);
		
		Map map= new HashMap();
		ITypeParameter[] typeParameters= type.getTypeParameters();
		for (int i= 0; i < typeParameters.length; i++) {
			String variable= typeParameters[i].getElementName();
			// use lower bound since method searching is only parameter based
			if (concreteParameters.length > i)
				map.put(variable, SignatureUtil.getLowerBound(concreteParameters[i]));
		}
		
		return map;
	}

	/**
	 * Finds a method by name. This searches for a method with a name and
	 * signature. Parameter types are only compared by the simple name, no
	 * resolving for the fully qualified type name is done. Constructors are
	 * only compared by parameters, not the name.
	 * 
	 * @param name The name of the method to find
	 * @param paramTypes The type signatures of the parameters e.g.
	 *        <code>{"QString;","I"}</code>
	 * @param isConstructor If the method is a constructor
	 * @param methods The methods to search in
	 * @param typeVariables a map from type variables to concretely used types
	 * @return The found method or <code>null</code>, if nothing found
	 */
	private IMethod findMethod(String name, String[] paramTypes, boolean isConstructor, IMethod[] methods, Map typeVariables) throws JavaModelException {
		for (int i= methods.length - 1; i >= 0; i--) {
			if (isSameMethodSignature(name, paramTypes, isConstructor, methods[i], typeVariables)) {
				return methods[i];
			}
		}
		return null;
	}

	/**
	 * Tests if a method equals to the given signature. Parameter types are only
	 * compared by the simple name, no resolving for the fully qualified type
	 * name is done. Constructors are only compared by parameters, not the name.
	 * 
	 * @param name Name of the method
	 * @param paramTypes The type signatures of the parameters e.g.
	 *        <code>{"QString;","I"}</code>
	 * @param isConstructor Specifies if the method is a constructor
	 * @param typeVariables a map from type variables to types
	 * @return Returns <code>true</code> if the method has the given name and
	 *         parameter types and constructor state.
	 */
	private boolean isSameMethodSignature(String name, String[] paramTypes, boolean isConstructor, IMethod curr, Map typeVariables) throws JavaModelException {
		if (isConstructor || name.equals(curr.getElementName())) {
			if (isConstructor == curr.isConstructor()) {
				String[] currParamTypes= curr.getParameterTypes();
				if (paramTypes.length == currParamTypes.length) {
					// no need to check method type variables since these are not yet bound
					// when proposing a method
					for (int i= 0; i < paramTypes.length; i++) {
						// method equality uses erased types 
						String erasure1= Signature.getTypeErasure(paramTypes[i]);
						String erasure2= Signature.getTypeErasure(currParamTypes[i]);
						String t1= Signature.getSimpleName(Signature.toString(erasure1));
						String t2= Signature.getSimpleName(Signature.toString(erasure2));
						char[] replacement= (char[]) typeVariables.get(t2);
						if (replacement != null)
							t2= String.valueOf(Signature.getSignatureSimpleName(replacement));
						if (!t1.equals(t2)) {
							return false;
						}
					}
					return true;
				}
			}
		}
		return false;
	}
}
