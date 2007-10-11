/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Assert;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IRegion;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.template.contentassist.PositionBasedCompletionProposal;
import org.eclipse.jdt.internal.ui.util.StringMatcher;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;


/**
 * This class triggers a code-completion that will track all local and member variables for later
 * use as a parameter guessing proposal.
 */
public class ParameterGuesser {

	final class Variable {

		/**
		 * Variable type. Used to choose the best guess based on scope (Local beats instance beats inherited).
		 */
		public static final int LOCAL= 0;
		public static final int FIELD= 1;
		public static final int METHOD= 1;
		public static final int INHERITED_FIELD= 3;
		public static final int INHERITED_METHOD= 3;

		public final String typePackage;
		public final String typeName;
		public final String name;
		public final int variableType;
		public final int positionScore;
		public boolean alreadyMatched;
		public char[] triggerChars;
		public ImageDescriptor descriptor;
		public boolean isAutoboxingMatch;
		private String fFQN;
		private IType type;
		private boolean fIsPrimitive;


		public Variable(String typePackage, String typeName, String name, int variableType, int positionScore, char[] triggerChars, ImageDescriptor descriptor) {
			if (typePackage == null)
				typePackage= ""; //$NON-NLS-1$
			if (typeName == null)
				typeName= ""; //$NON-NLS-1$
			
			this.typePackage= typePackage;
			this.typeName= typeName;
			this.name= name;
			this.variableType= variableType;
			this.positionScore= positionScore;
			this.triggerChars= triggerChars;
			this.descriptor= descriptor;
			
			fFQN= computeFQN();
			fIsPrimitive= ParameterGuesser.PRIMITIVE_ASSIGNMENTS.containsKey(fFQN);
			if (!fIsPrimitive)
				type= computeType();
		}

		/*
		 * @see Object#toString()
		 */
		public String toString() {

			StringBuffer buffer= new StringBuffer();

			if (typePackage.length() != 0) {
				buffer.append(typePackage);
				buffer.append('.');
			}

			buffer.append(typeName);
			buffer.append(' ');
			buffer.append(name);
			buffer.append(" ("); //$NON-NLS-1$
			buffer.append(variableType);
			buffer.append(')');

			return buffer.toString();
		}

		String getFullyQualifiedName() {
			return fFQN;
		}

		private String computeFQN() {
			if (typePackage.length() != 0)
				return typePackage + '.' + typeName; 
			return typeName;
		}

		private IType computeType() {
			try {
				return fCompilationUnit.getJavaProject().findType(fFQN);
			} catch (JavaModelException e) {
				return null;
			}
		}

		boolean isPrimitive() {
			return fIsPrimitive;
		}

		boolean isArrayType() {
			// check for an exact match (fast)
			return getFullyQualifiedName().endsWith("[]"); //$NON-NLS-1$
		}

		boolean isHierarchyAssignable(Variable rhs) throws JavaModelException {
			if (rhs.type == null || type == null)
				return false;

			if ("java.lang.Object".equals(getFullyQualifiedName())) //$NON-NLS-1$
				return true;

			if (rhs.type.equals(type))
				return true;

			// XXX: No simpler way, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=112635
			return  JavaModelUtil.isSuperType(fVariablesHierarchy, type, rhs.type);
		}

		boolean isAutoBoxingAssignable(Variable rhs) {
			// auto-unbox variable to match primitive parameter
			if (isPrimitive()) {
				String unboxedVariable= ParameterGuesser.getAutoUnboxedType(rhs.getFullyQualifiedName());
				return ParameterGuesser.isPrimitiveAssignable(typeName, unboxedVariable);
			}
		
			// variable is primitive, auto-box to match parameter type
			if (rhs.isPrimitive()) {
				String unboxedType= ParameterGuesser.getAutoUnboxedType(getFullyQualifiedName());
				return ParameterGuesser.isPrimitiveAssignable(unboxedType, rhs.typeName);
			}
		
			return false;
		}

		/**
		 * Return true if <code>rhs</code> is assignable to the receiver.
		 * 
		 * @param rhs the variable on the right-hand-side 
		 * @return <code>true</code> if assignable
		 * @throws JavaModelException if it fails
		 */
		boolean isAssignable(Variable rhs) throws JavaModelException {
		
			// if there is no package specified, do the check on type name only.  This will work for primitives
			// and for local variables that cannot be resolved.
			if (type == null || rhs.type == null) {
		
				if (rhs.typeName.equals(typeName))
					return true;
		
				if (ParameterGuesser.isPrimitiveAssignable(typeName, rhs.typeName))
					return true;
		
				if (fAllowAutoBoxing && isAutoBoxingAssignable(rhs)) {
					rhs.isAutoboxingMatch= true;
					return true;
				}
		
				return false;
			}
		
			// if we get to here, we're doing a "fully qualified match" -- meaning including packages, no primitives
			// and no unresolved variables.
		
			// if there is an exact textual match, there is no need to search type hierarchy.. this is
			// a quick way to pick up an exact match.
			if (rhs.getFullyQualifiedName().equals(getFullyQualifiedName()))
				return true;
		
			// otherwise, we get a match/no match by searching the type hierarchy
			return isHierarchyAssignable(rhs);
		}
	}

	private static final char[] NO_TRIGGERS= new char[0];
	private static final char[] VOID= "void".toCharArray(); //$NON-NLS-1$
	private static final char[] HASHCODE= "hashCode()".toCharArray(); //$NON-NLS-1$
	private static final char[] TOSTRING= "toString()".toCharArray(); //$NON-NLS-1$
	private static final char[] CLONE= "clone()".toCharArray(); //$NON-NLS-1$
	
	private final class VariableCollector extends CompletionRequestor {

		/** The enclosing type name */
		private String fEnclosingTypeName;
		/** The local and member variables */
		private List fVars;
		
		
		VariableCollector() {
			setIgnored(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, true);
			setIgnored(CompletionProposal.FIELD_REF, false);
			setIgnored(CompletionProposal.KEYWORD, true);
			setIgnored(CompletionProposal.LABEL_REF, true);
			setIgnored(CompletionProposal.METHOD_DECLARATION, true);
			setIgnored(CompletionProposal.METHOD_NAME_REFERENCE, true);
			setIgnored(CompletionProposal.METHOD_REF, false);
			setIgnored(CompletionProposal.PACKAGE_REF, true);
			setIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION, true);
			setIgnored(CompletionProposal.VARIABLE_DECLARATION, true);
			setIgnored(CompletionProposal.TYPE_REF, true);
			setIgnored(CompletionProposal.ANNOTATION_ATTRIBUTE_REF, false);
			setIgnored(CompletionProposal.LOCAL_VARIABLE_REF, false);
		}

		public List collect(int codeAssistOffset, ICompilationUnit compilationUnit) throws JavaModelException {
			Assert.isTrue(codeAssistOffset >= 0);
			Assert.isNotNull(compilationUnit);

			fVars= new ArrayList();

			String source= compilationUnit.getSource();
			if (source == null)
				return fVars;

			fEnclosingTypeName= getEnclosingTypeName(codeAssistOffset, compilationUnit);

			// find some whitespace to start our variable-finding code complete from.
			// this allows the VariableTracker to find all available variables (no prefix to match for the code completion)
			int completionOffset= getCompletionOffset(source, codeAssistOffset);

			compilationUnit.codeComplete(completionOffset, this);

			// add this, true, false
			int dotPos= fEnclosingTypeName.lastIndexOf('.');
			String thisType;
			String thisPkg;
			if (dotPos != -1) {
				thisType= fEnclosingTypeName.substring(dotPos + 1);
				thisPkg= fEnclosingTypeName.substring(0, dotPos);
			} else {
				thisPkg= new String();
				thisType= fEnclosingTypeName;
			}
			addVariable(Variable.FIELD, thisPkg.toCharArray(), thisType.toCharArray(), "this".toCharArray(), new char[] {'.'}, getFieldDescriptor(Flags.AccPublic | Flags.AccFinal)); //$NON-NLS-1$
			addVariable(Variable.FIELD, NO_TRIGGERS, "boolean".toCharArray(), "true".toCharArray(), NO_TRIGGERS, null);  //$NON-NLS-1$//$NON-NLS-2$
			addVariable(Variable.FIELD, NO_TRIGGERS, "boolean".toCharArray(), "false".toCharArray(), NO_TRIGGERS, null);  //$NON-NLS-1$//$NON-NLS-2$

			return fVars;
		}

		private String getEnclosingTypeName(int codeAssistOffset, ICompilationUnit compilationUnit) throws JavaModelException {

			IJavaElement element= compilationUnit.getElementAt(codeAssistOffset);
			if (element == null)
				return null;

			element= element.getAncestor(IJavaElement.TYPE);
			if (element == null)
				return null;

			return element.getElementName();
		}

		/**
		 * Determine if the declaring type matches the type of the code completion invocation
		 * 
		 * @param declaringTypeName the name of the declaring type 
		 * @return <code>true</code> if the given type matches 
		 */
		private final boolean isInherited(String declaringTypeName) {
			return !declaringTypeName.equals(fEnclosingTypeName);
		}

		private void addVariable(int varType, char[] typePackageName, char[] typeName, char[] name, char[] triggers, ImageDescriptor descriptor) {
			fVars.add(new Variable(new String(typePackageName), new String(typeName), new String(name), varType, fVars.size(), triggers, descriptor));
		}

		private void acceptField(char[] declaringTypeName, char[] name, char[] typePackageName, char[] typeName, int modifiers) {
			if (!isInherited(new String(declaringTypeName)))
				addVariable(Variable.FIELD, typePackageName, typeName, name, NO_TRIGGERS, getFieldDescriptor(modifiers));
			else
				addVariable(Variable.INHERITED_FIELD, typePackageName, typeName, name, NO_TRIGGERS, getFieldDescriptor(modifiers));
		}

		private void acceptLocalVariable(char[] name, char[] typePackageName, char[] typeName, int modifiers) {
			addVariable(Variable.LOCAL, typePackageName, typeName, name, NO_TRIGGERS, decorate(JavaPluginImages.DESC_OBJS_LOCAL_VARIABLE, modifiers, false));
		}

		private void acceptMethod(char[] declaringTypeName, char[] returnTypePackageName, char[] returnTypeName, char[] completionName, int modifiers) {
			if (!filter(returnTypeName, completionName))
				addVariable(isInherited(new String(declaringTypeName)) ? Variable.INHERITED_METHOD : Variable.METHOD, returnTypePackageName, returnTypeName, completionName, NO_TRIGGERS, getMemberDescriptor(modifiers));
		}

		private boolean filter(char[] returnTypeName, char[] completionName) {
			completionName= Signature.getSimpleName(completionName);
			return Arrays.equals(VOID, returnTypeName) || Arrays.equals(HASHCODE, completionName) || Arrays.equals(TOSTRING, completionName) || Arrays.equals(CLONE, completionName);
		}

		protected ImageDescriptor getMemberDescriptor(int modifiers) {
			ImageDescriptor desc= JavaElementImageProvider.getMethodImageDescriptor(false, modifiers);
			return decorate(desc, modifiers, false);
		}

		protected ImageDescriptor getFieldDescriptor(int modifiers) {
			ImageDescriptor desc= JavaElementImageProvider.getFieldImageDescriptor(false, modifiers);
			return decorate(desc, modifiers, true);
		}

		private ImageDescriptor decorate(ImageDescriptor descriptor, int modifiers, boolean isField) {
			int flags= 0;

			if (Flags.isDeprecated(modifiers))
				flags |= JavaElementImageDescriptor.DEPRECATED;

			if (Flags.isStatic(modifiers))
				flags |= JavaElementImageDescriptor.STATIC;

			if (Flags.isFinal(modifiers))
				flags |= JavaElementImageDescriptor.FINAL;

			if (Flags.isSynchronized(modifiers))
				flags |= JavaElementImageDescriptor.SYNCHRONIZED;

			if (Flags.isAbstract(modifiers))
				flags |= JavaElementImageDescriptor.ABSTRACT;
			
			if (isField) {
				if (Flags.isVolatile(modifiers))
					flags |= JavaElementImageDescriptor.VOLATILE;
	
				if (Flags.isTransient(modifiers))
					flags |= JavaElementImageDescriptor.TRANSIENT;
			}
			
			return new JavaElementImageDescriptor(descriptor, flags, JavaElementImageProvider.SMALL_SIZE);

		}

		/*
		 * @see org.eclipse.jdt.core.CompletionRequestor#accept(org.eclipse.jdt.core.CompletionProposal)
		 */
		public void accept(CompletionProposal proposal) {
			if (isIgnored(proposal.getKind()))
				return;
			
			switch (proposal.getKind()) {
				case CompletionProposal.FIELD_REF:
					acceptField(
							Signature.getSignatureSimpleName(proposal.getDeclarationSignature()),
							proposal.getName(),
							Signature.getSignatureQualifier(proposal.getSignature()),
							Signature.getSignatureSimpleName(proposal.getSignature()), 
							proposal.getFlags());
					return;
				case CompletionProposal.LOCAL_VARIABLE_REF:
					acceptLocalVariable(
							proposal.getCompletion(),
							Signature.getSignatureQualifier(proposal.getSignature()),
							Signature.getSignatureSimpleName(proposal.getSignature()),
							proposal.getFlags());
					return;
				case CompletionProposal.METHOD_REF:
					if (Signature.getParameterCount(proposal.getSignature()) == 0)
						acceptMethod(
								Signature.getSignatureSimpleName(proposal.getDeclarationSignature()),
								Signature.getSignatureQualifier(Signature.getReturnType(proposal.getSignature())),
								Signature.getSignatureSimpleName(Signature.getReturnType(proposal.getSignature())),
								proposal.getCompletion(),
								proposal.getFlags());

			}
			
		}
	}

	private static final Map PRIMITIVE_ASSIGNMENTS;
	private static final Map AUTOUNBOX;

	static {
		HashMap primitiveAssignments= new HashMap();
		// put (LHS, RHS)
		primitiveAssignments.put("boolean", Collections.singleton("boolean")); //$NON-NLS-1$ //$NON-NLS-2$
		primitiveAssignments.put("byte", Collections.singleton("byte")); //$NON-NLS-1$ //$NON-NLS-2$
		primitiveAssignments.put("short", Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[] {"short", "byte"})))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		primitiveAssignments.put("char", Collections.singleton("char")); //$NON-NLS-1$ //$NON-NLS-2$
		primitiveAssignments.put("int", Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[] {"int", "short", "char", "byte"})))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		primitiveAssignments.put("long", Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[] {"long", "int", "short", "char", "byte"})))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		primitiveAssignments.put("float", Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[] {"float", "long", "int", "short", "char", "byte"})))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
		primitiveAssignments.put("double", Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[] {"double", "float", "long", "int", "short", "char", "byte"})))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		primitiveAssignments.put("primitive number", Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[] {"double", "float", "long", "int", "short", "byte"})))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
		PRIMITIVE_ASSIGNMENTS= Collections.unmodifiableMap(primitiveAssignments);

		HashMap autounbox= new HashMap();
		autounbox.put("java.lang.Boolean", "boolean"); //$NON-NLS-1$ //$NON-NLS-2$
		autounbox.put("java.lang.Byte", "byte"); //$NON-NLS-1$ //$NON-NLS-2$
		autounbox.put("java.lang.Short", "short"); //$NON-NLS-1$ //$NON-NLS-2$
		autounbox.put("java.lang.Character", "char"); //$NON-NLS-1$ //$NON-NLS-2$
		autounbox.put("java.lang.Integer", "int"); //$NON-NLS-1$ //$NON-NLS-2$
		autounbox.put("java.lang.Long", "long"); //$NON-NLS-1$ //$NON-NLS-2$
		autounbox.put("java.lang.Float", "float"); //$NON-NLS-1$ //$NON-NLS-2$
		autounbox.put("java.lang.Double", "double"); //$NON-NLS-1$ //$NON-NLS-2$
		autounbox.put("java.lang.Number", "primitive number"); // dummy for reverse assignment //$NON-NLS-1$ //$NON-NLS-2$
		AUTOUNBOX= Collections.unmodifiableMap(autounbox);
	}

	private static final boolean isPrimitiveAssignable(String lhs, String rhs) {
		Set targets= (Set) PRIMITIVE_ASSIGNMENTS.get(lhs);
		return targets != null && targets.contains(rhs);
	}

	private static final String getAutoUnboxedType(String type) {
		String primitive= (String) AUTOUNBOX.get(type);
		return primitive;
	}

	/** The compilation unit we are computing the completion for */
	private final ICompilationUnit fCompilationUnit;
	/** The code assist offset. */
	private final int fCodeAssistOffset;
	/** Local and member variables of the compilation unit */
	private List fVariables;
	private ImageDescriptorRegistry fRegistry= JavaPlugin.getImageDescriptorRegistry();
	private boolean fAllowAutoBoxing;
	private ITypeHierarchy fVariablesHierarchy;


	/**
	 * Creates a parameter guesser for compilation unit and offset.
	 *
	 * @param codeAssistOffset the offset at which to perform code assist
	 * @param compilationUnit the compilation unit in which code assist is performed
	 */
	public ParameterGuesser(int codeAssistOffset, ICompilationUnit compilationUnit) {
		Assert.isTrue(codeAssistOffset >= 0);
		Assert.isNotNull(compilationUnit);

		fCodeAssistOffset= codeAssistOffset;
		fCompilationUnit= compilationUnit;


		IJavaProject project= fCompilationUnit.getJavaProject();
		String sourceVersion= project == null
				? JavaCore.getOption(JavaCore.COMPILER_SOURCE)
				: project.getOption(JavaCore.COMPILER_SOURCE, true);

		fAllowAutoBoxing= JavaCore.VERSION_1_5.compareTo(sourceVersion) <= 0;
	}

	/**
	 * Returns the offset at which code assist is performed.
	 * 
	 * @return the code assist offset 
	 */
	public int getCodeAssistOffset() {
		return fCodeAssistOffset;
	}

	/**
	 * Returns the compilation unit in which code assist is performed.
	 * 
	 * @return the compilation unit 
	 */
	public ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
	}

	/**
	 * Returns the matches for the type and name argument, ordered by match quality.
	 *
	 * @param paramPackage - the package of the parameter we are trying to match
	 * @param paramType - the qualified name of the parameter we are trying to match
	 * @param paramName - the name of the parameter (used to find similarly named matches)
	 * @param pos
	 * @param document
	 * @return returns the name of the best match, or <code>null</code> if no match found
	 * @throws JavaModelException if it fails
	 */
	public ICompletionProposal[] parameterProposals(String paramPackage, String paramType, String paramName, Position pos, IDocument document) throws JavaModelException {

		Variable parameter= new Variable(paramPackage, paramType, paramName, Variable.LOCAL, 0, null, null);
		
		if (fVariables == null) {
			VariableCollector variableCollector= new VariableCollector();
			fVariables= variableCollector.collect(fCodeAssistOffset, fCompilationUnit);
		}

		if (fVariablesHierarchy == null && !parameter.isPrimitive()) {
			// Compute supertype hierarchies
			IRegion scope= JavaCore.newRegion();
			for (ListIterator iterator= fVariables.listIterator(fVariables.size()); iterator.hasPrevious(); ) {
				Variable variable= (Variable) iterator.previous();
				IType varType= variable.type;
				if (varType != null)
					scope.add(varType);
			}
			fVariablesHierarchy= fCompilationUnit.getJavaProject().newTypeHierarchy(scope, null);
		}

		List typeMatches= findProposalsMatchingType(fVariables, parameter);
		orderMatches(typeMatches, paramName);

		ICompletionProposal[] ret= new ICompletionProposal[typeMatches.size()];
		int i= 0; int replacementLength= 0;
		for (Iterator it= typeMatches.iterator(); it.hasNext();) {
			Variable v= (Variable)it.next();
			if (i == 0) {
				v.alreadyMatched= true;
				replacementLength= v.name.length();
			}

			final char[] triggers= new char[v.triggerChars.length + 1];
			System.arraycopy(v.triggerChars, 0, triggers, 0, v.triggerChars.length);
			String displayString= v.isAutoboxingMatch ? v.name : v.name;
			triggers[triggers.length - 1]= ';';
			ICompletionProposal proposal= new PositionBasedCompletionProposal(v.name, pos, replacementLength, getImage(v.descriptor), displayString, null, null) {
				public char[] getTriggerCharacters() {
					return triggers;
				}
			};
			ret[i++]= proposal;
		}

		return ret;
	}

	private static class MatchComparator implements Comparator {

		private String fParamName;

		MatchComparator(String paramName) {
			fParamName= paramName;
		}
		public int compare(Object o1, Object o2) {
			Variable one= (Variable)o1;
			Variable two= (Variable)o2;

			return score(two) - score(one);
		}

		/**
		 * The four order criteria as described below - put already used into bit 10, all others into
		 * bits 0-9, 11-20, 21-30; 31 is sign - always 0
		 * @param v
		 * @return the score for <code>v</code>
		 */
		private int score(Variable v) {
			int variableScore= 100 - v.variableType; // since these are increasing with distance
			int subStringScore= getLongestCommonSubstring(v.name, fParamName).length();
			// substring scores under 60% are not considered
			// this prevents marginal matches like a - ba and false - isBool that will
			// destroy the sort order
			int shorter= Math.min(v.name.length(), fParamName.length());
			if (subStringScore < 0.6 * shorter)
				subStringScore= 0;

			int positionScore= v.positionScore; // since ???
			int matchedScore= v.alreadyMatched ? 0 : 1;
			int autoboxingScore= v.isAutoboxingMatch ? 0 : 1;

			int score= autoboxingScore << 30 | variableScore << 21 | subStringScore << 11 | matchedScore << 10 | positionScore;
			return score;
		}

	}

	/**
	 * Determine the best match of all possible type matches.  The input into this method is all
	 * possible completions that match the type of the argument. The purpose of this method is to
	 * choose among them based on the following simple rules:
	 *
	 * 	1) Local Variables > Instance/Class Variables > Inherited Instance/Class Variables
	 *
	 * 	2) A longer case insensitive substring match will prevail
	 *
	 *  3) Variables that have not been used already during this completion will prevail over
	 * 		those that have already been used (this avoids the same String/int/char from being passed
	 * 		in for multiple arguments)
	 *
	 * 	4) A better source position score will prevail (the declaration point of the variable, or
	 * 		"how close to the point of completion?"
	 * 
	 * @param typeMatches the list of type matches 
	 * @param paramName the parameter name
	 */
	private static void orderMatches(List typeMatches, String paramName) {
		if (typeMatches != null) Collections.sort(typeMatches, new MatchComparator(paramName));
	}

	/**
	 * Finds a local or member variable that matched the type of the parameter
	 * 
	 * @param proposals the list of completion proposals 
	 * @param parameter the parameter
	 * @return the list with matching proposals
	 * @throws JavaModelException 
	 */
	private List findProposalsMatchingType(List proposals, Variable parameter) throws JavaModelException {

		if (parameter.getFullyQualifiedName().length() == 0)
			return null;

		// traverse the lists in reverse order, since it is empirically true that the code
		// completion engine returns variables in the order they are found -- and we want to find
		// matches closest to the code completion point.. No idea if this behavior is guaranteed.

		List matches= new ArrayList();

		for (ListIterator iterator= proposals.listIterator(proposals.size()); iterator.hasPrevious(); ) {
			Variable variable= (Variable) iterator.previous();
			variable.isAutoboxingMatch= false;
			if (parameter.isAssignable(variable))
				matches.add(variable);
		}

		return matches;
	}

	/**
	 * Returns the longest common substring of two strings.
	 * 
	 * @param first the first string 
	 * @param second the second string
	 * @return the longest common substring
	 */
	private static String getLongestCommonSubstring(String first, String second) {

		String shorter= (first.length() <= second.length()) ? first : second;
		String longer= shorter == first ? second : first;

		int minLength= shorter.length();

		StringBuffer pattern= new StringBuffer(shorter.length() + 2);
		String longestCommonSubstring= ""; //$NON-NLS-1$

		for (int i= 0; i < minLength; i++) {
			for (int j= i + 1; j <= minLength; j++) {
				if (j - i < longestCommonSubstring.length())
					continue;

				String substring= shorter.substring(i, j);
				pattern.setLength(0);
				pattern.append('*');
				pattern.append(substring);
				pattern.append('*');

				StringMatcher matcher= new StringMatcher(pattern.toString(), true, false);
				if (matcher.match(longer))
					longestCommonSubstring= substring;
			}
		}

		return longestCommonSubstring;
	}

	private Image getImage(ImageDescriptor descriptor) {
		return (descriptor == null) ? null : fRegistry.get(descriptor);
	}

	private static int getCompletionOffset(String source, int start) {
		int index= start;
		char c;
		while (index > 0 && (c= source.charAt(index - 1)) != '{' && c != ';')
			index--;
		return Math.min(index + 1, source.length());
	}

}
