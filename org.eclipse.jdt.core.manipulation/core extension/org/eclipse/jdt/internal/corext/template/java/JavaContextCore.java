/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel  <lars.vogel@gmail.com> - [templates][content assist] Ctrl+Space without any starting letter shows to no templates - https://bugs.eclipse.org/406463
 *     Lukas Hanke <hanke@yatta.de> - [templates][content assist] Content assist for 'for' loop should suggest member variables - https://bugs.eclipse.org/117215
 *     Nicolaj Hoess <nicohoess@gmail.com> - Make some internal methods accessible to help Postfix Code Completion plug-in - https://bugs.eclipse.org/433500
 *     Microsoft Corporation - moved template related code to jdt.core.manipulation - https://bugs.eclipse.org/549989
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateTranslator;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jface.text.templates.TemplateVariableType;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;
import org.eclipse.jdt.core.manipulation.TypeKinds;
import org.eclipse.jdt.core.manipulation.TypeNameMatchCollector;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameMatch;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.template.java.CompilationUnitCompletion.Variable;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.text.template.contentassist.MultiVariable;


/**
 * A context for Java source.
 */
public class JavaContextCore extends CompilationUnitContext implements IJavaContext {

	/** A code completion requester for guessing local variable names. */
	private CompilationUnitCompletion fCompletion;
	/**
	 * The list of used local names.
	 * @since 3.3
	 */
	private Set<String> fUsedNames= new HashSet<>();
	private Map<String, MultiVariable> fVariables= new HashMap<>();
	private ImportRewrite fImportRewrite;

	private Set<String> fCompatibleContextTypeIds;

	/**
	 * Creates a java template context.
	 *
	 * @param type   the context type.
	 * @param document the document.
	 * @param completionOffset the completion offset within the document.
	 * @param completionLength the completion length.
	 * @param compilationUnit the compilation unit (may be <code>null</code>).
	 */
	public JavaContextCore(TemplateContextType type, IDocument document, int completionOffset, int completionLength, ICompilationUnit compilationUnit) {
		super(type, document, completionOffset, completionLength, compilationUnit);
	}

	/**
	 * Creates a java template context.
	 *
	 * @param type   the context type.
	 * @param document the document.
	 * @param completionPosition the position defining the completion offset and length
	 * @param compilationUnit the compilation unit (may be <code>null</code>).
	 */
	public JavaContextCore(TemplateContextType type, IDocument document, Position completionPosition, ICompilationUnit compilationUnit) {
		super(type, document, completionPosition, compilationUnit);
	}

	/**
	 * Adds a context type that is also compatible. That means the context can also process templates of that context type.
	 *
	 * @param contextTypeId the context type to accept
	 */
	@Override
	public void addCompatibleContextType(String contextTypeId) {
		if (fCompatibleContextTypeIds == null)
			fCompatibleContextTypeIds= new HashSet<>();
		fCompatibleContextTypeIds.add(contextTypeId);
	}

	/*
	 * @see TemplateContext#evaluate(Template template)
	 */
	@Override
	public TemplateBuffer evaluate(Template template) throws BadLocationException, TemplateException {
		clear();

		if (!canEvaluate(template))
			throw new TemplateException(JavaTemplateMessages.Context_error_cannot_evaluate);

		TemplateTranslator translator= new TemplateTranslator() {
			@Override
			protected TemplateVariable createVariable(TemplateVariableType type, String name, int[] offsets) {
//				TemplateVariableResolver resolver= getContextType().getResolver(type.getName());
//				return resolver.createVariable();

				MultiVariable variable= new JavaVariable(type, name, offsets);
				fVariables.put(name, variable);
				return variable;
			}
		};
		TemplateBuffer buffer= translator.translate(template);

		getContextType().resolve(buffer, this);

		rewriteImports();

		clear();

		return buffer;
	}

	private void clear() {
		fUsedNames.clear();
		fImportRewrite= null;
	}

	/*
	 * @see TemplateContext#canEvaluate(Template templates)
	 */
	@Override
	public boolean canEvaluate(Template template) {
		if (!hasCompatibleContextType(template))
			return false;

		if (this.isForceEvaluation())
			return true;

		String key= getKey().toLowerCase();
		if (key.length() > 0 || !isAfterDot()) {
			String templateName= template.getName().toLowerCase();
			return JavaManipulationPlugin.CODEASSIST_SUBSTRING_MATCH_ENABLED
					? templateName.contains(key)
					: templateName.startsWith(key);
		}
		return false;
	}

	private boolean isAfterDot() {
		try {
			IDocument document= getDocument();
			int offset= getCompletionOffset();
			return document.get(offset - 1, 1).charAt(0) == '.';
		} catch (BadLocationException e) {
			return false;
		}
	}

	private boolean hasCompatibleContextType(Template template) {
		String key= getKey();
		if (template.matches(key, getContextType().getId()))
			return true;

		if (fCompatibleContextTypeIds == null)
			return false;

		Iterator<String> iter= fCompatibleContextTypeIds.iterator();
		while (iter.hasNext()) {
			if (template.matches(key, iter.next()))
				return true;
		}

		return false;
	}

	/*
	 * @see DocumentTemplateContext#getCompletionPosition();
	 */
	@Override
	public int getStart() {

		if (this.isManaged() && getCompletionLength() > 0)
			return super.getStart();

		try {
			IDocument document= getDocument();

			int start= getCompletionOffset();
			int end= getCompletionOffset() + getCompletionLength();

			while (start != 0 && Character.isUnicodeIdentifierPart(document.getChar(start - 1)))
				start--;

			while (start != end && Character.isWhitespace(document.getChar(start)))
				start++;

			if (start == end)
				start= getCompletionOffset();

				return start;

		} catch (BadLocationException e) {
			return super.getStart();
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.template.DocumentTemplateContext#getEnd()
	 */
	@Override
	public int getEnd() {

		if (this.isManaged() || getCompletionLength() == 0)
			return super.getEnd();

		try {
			IDocument document= getDocument();

			int start= getCompletionOffset();
			int end= getCompletionOffset() + getCompletionLength();

			while (start != end && Character.isWhitespace(document.getChar(end - 1)))
				end--;

			return end;

		} catch (BadLocationException e) {
			return super.getEnd();
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.template.DocumentTemplateContext#getKey()
	 */
	@Override
	public String getKey() {

		if (getCompletionLength() == 0)
			return super.getKey();

		try {
			IDocument document= getDocument();

			int start= getStart();
			int end= getCompletionOffset();
			return start <= end
				? document.get(start, end - start)
				: ""; //$NON-NLS-1$

		} catch (BadLocationException e) {
			return super.getKey();
		}
	}

	/**
	 * Returns the character before the start position of the completion.
	 *
	 * @return the character before the start position of the completion
	 */
	public char getCharacterBeforeStart() {
		int start= getStart();

		try {
			return start == 0
				? ' '
				: getDocument().getChar(start - 1);

		} catch (BadLocationException e) {
			return ' ';
		}
	}

	@Override
	public void handleException(Exception e) {
		JavaManipulationPlugin.log(e);
	}

	private CompilationUnitCompletion getCompletion() {
		ICompilationUnit compilationUnit= getCompilationUnit();
		if (fCompletion == null) {
			fCompletion= new CompilationUnitCompletion(compilationUnit);

			if (compilationUnit != null) {
				try {
					compilationUnit.codeComplete(getStart(), fCompletion);
				} catch (JavaModelException e) {
					// ignore
				}
			}
		}

		return fCompletion;
	}

	/**
	 * Returns the names of arrays available in the current {@link CompilationUnit}'s scope.
	 *
	 * @return the names of local arrays available in the current {@link CompilationUnit}'s scope
	 */
	@Override
	public Variable[] getArrays() {
		Variable[] arrays= getCompletion().findArraysInCurrentScope();
		arrange(arrays);
		return arrays;
	}

	/**
	 * Sorts already used variables behind any that are not yet used.
	 *
	 * @param variables the variables to sort
	 * @since 3.3
	 */
	private void arrange(Variable[] variables) {
		Arrays.sort(variables, new Comparator<Variable>() {
			@Override
			public int compare(Variable o1, Variable o2) {
				return rank(o1) - rank(o2);
			}

			private int rank(Variable l) {
				return fUsedNames.contains(l.getName()) ? 1 : 0;
			}
		});
	}

	/**
	 * Returns the names of local variables matching <code>type</code>.
	 *
	 * @param type the type of the variables
	 * @return the names of local variables matching <code>type</code>
	 * @since 3.3
	 */
	@Override
	public Variable[] getLocalVariables(String type) {
		Variable[] localVariables= getCompletion().findLocalVariables(type);
		arrange(localVariables);
		return localVariables;
	}

	/**
	 * Returns the names of fields matching <code>type</code>.
	 *
	 * @param type the type of the fields
	 * @return the names of fields matching <code>type</code>
	 * @since 3.3
	 */
	@Override
	public Variable[] getFields(String type) {
		Variable[] fields= getCompletion().findFieldVariables(type);
		arrange(fields);
		return fields;
	}

	/**
	 * Returns the names of iterables or arrays available in the current {@link CompilationUnit}'s scope.
	 *
	 * @return the names of iterables or arrays available in the current {@link CompilationUnit}'s scope
	 */
	@Override
	public Variable[] getIterables() {
		Variable[] iterables= getCompletion().findIterablesInCurrentScope();
		arrange(iterables);
		return iterables;
	}

	@Override
	public void markAsUsed(String name) {
		fUsedNames.add(name);
	}

	@Override
	public String[] suggestVariableNames(String type) throws IllegalArgumentException {
		String[] excludes= computeExcludes();
		// TODO erasure, arrays, etc.
		String[] result= suggestVariableName(type, excludes);
		return result;
	}

	public String[] computeExcludes() {
		String[] excludes= getCompletion().getLocalVariableNames();
		if (!fUsedNames.isEmpty()) {
			String[] allExcludes= new String[fUsedNames.size() + excludes.length];
			System.arraycopy(excludes, 0, allExcludes, 0, excludes.length);
			System.arraycopy(fUsedNames.toArray(), 0, allExcludes, 0, fUsedNames.size());
			excludes= allExcludes;
		}
		return excludes;
	}

	private String[] suggestVariableName(String type, String[] excludes) throws IllegalArgumentException {
		int dim=0;
		while (type.endsWith("[]")) {//$NON-NLS-1$
			dim++;
			type= type.substring(0, type.length() - 2);
		}

		IJavaProject project= getJavaProject();
		if (project != null)
			return StubUtility.getVariableNameSuggestions(NamingConventions.VK_LOCAL, project, type, dim, Arrays.asList(excludes), true);

		// fallback if we lack proper context: roll-our own lowercasing
		return new String[] {Signature.getSimpleName(type).toLowerCase()};
	}

	/**
	 * Adds an import for type with type name <code>type</code> if possible.
	 * Returns a string which can be used to reference the type.
	 *
	 * @param type the fully qualified name of the type to import
	 * @return returns a type to which the type binding can be assigned to.
	 * 	The returned type contains is unqualified when an import could be added or was already known.
	 * 	It is fully qualified, if an import conflict prevented the import.
	 * @since 3.4
	 */
	@Override
	public String addImport(String type) {
		if (isReadOnly())
			return type;

		ICompilationUnit cu= getCompilationUnit();
		if (cu == null)
			return type;

		try {
			boolean qualified= type.indexOf('.') != -1;
			if (!qualified) {
				IJavaSearchScope searchScope= SearchEngine.createJavaSearchScope(new IJavaElement[] { cu.getJavaProject() });
				SimpleName nameNode= null;
				TypeNameMatch[] matches= findAllTypes(type, searchScope, nameNode, null, cu);
				if (matches.length != 1) // only add import if we have a single match
					return type;
				type= matches[0].getFullyQualifiedName();
			}

			CompilationUnit root= getASTRoot(cu);
			if (fImportRewrite == null) {
				if (root == null) {
					fImportRewrite= StubUtility.createImportRewrite(cu, true);
				} else {
					fImportRewrite= StubUtility.createImportRewrite(root, true);
				}
			}

			ImportRewriteContext context;
			if (root == null)
				context= null;
			else
				context= new ContextSensitiveImportRewriteContext(root, getCompletionOffset(), fImportRewrite);

			return fImportRewrite.addImport(type, context);
		} catch (JavaModelException e) {
			handleException(e);
			return type;
		}
	}

	/**
	 * Adds a static import for the member with name <code>qualifiedMemberName</code>. The member is
	 * either a static field or a static method or a '*' to import all static members of a type.
	 *
	 * @param qualifiedMemberName the fully qualified name of the member to import or a qualified type
	 * 			name plus a '.*' suffix.
	 * @return returns either the simple member name if the import was successful or else the qualified name.
	 * @since 3.4
	 */
	public String addStaticImport(String qualifiedMemberName) {
		if (isReadOnly())
			return qualifiedMemberName;

		ICompilationUnit cu= getCompilationUnit();
		if (cu == null)
			return qualifiedMemberName;

		int memberOffset= qualifiedMemberName.lastIndexOf('.');
		if (memberOffset == -1)
			return qualifiedMemberName;

		String typeName= qualifiedMemberName.substring(0, memberOffset);
		String memberName= qualifiedMemberName.substring(memberOffset + 1, qualifiedMemberName.length());
		try {
			boolean isField;
			if ("*".equals(memberName)) { //$NON-NLS-1$
				isField= true;
			} else {
				IJavaProject javaProject= cu.getJavaProject();

				IType type= javaProject.findType(typeName);
				if (type == null)
					return qualifiedMemberName;

				IField field= type.getField(memberName);
				if (field.exists()) {
					isField= true;
				} else if (hasMethod(type, memberName)) {
					isField= false;
				} else {
					return qualifiedMemberName;
				}
			}

			CompilationUnit root= getASTRoot(cu);
			if (fImportRewrite == null) {
				if (root == null) {
					fImportRewrite= StubUtility.createImportRewrite(cu, true);
				} else {
					fImportRewrite= StubUtility.createImportRewrite(root, true);
				}
			}

			ImportRewriteContext context;
			if (root == null)
				context= null;
			else
				context= new ContextSensitiveImportRewriteContext(root, getCompletionOffset(), fImportRewrite);

			return fImportRewrite.addStaticImport(typeName, memberName, isField, context);
		} catch (JavaModelException e) {
			handleException(e);
			return typeName;
		}
	}

	/**
	 * Does <code>type</code> contain a method with <code>name</code>?
	 *
	 * @param type the type to inspect
	 * @param name the name of the method to search for
	 * @return true if has such a method
	 * @throws JavaModelException if methods could not be retrieved
	 * @since 3.4
	 */
	private boolean hasMethod(IType type, String name) throws JavaModelException {
		IMethod[] methods= type.getMethods();
		for (IMethod method : methods) {
			if (name.equals(method.getElementName()))
				return true;
		}

		return false;
	}

	private void rewriteImports() {
		if (fImportRewrite == null)
			return;

		if (isReadOnly())
			return;

		ICompilationUnit cu= getCompilationUnit();
		if (cu == null)
			return;

		try {
			Position position= new Position(getCompletionOffset(), 0);
			IDocument document= getDocument();
			final String category= "__template_position_importer" + System.currentTimeMillis(); //$NON-NLS-1$
			IPositionUpdater updater= new DefaultPositionUpdater(category);
			document.addPositionCategory(category);
			document.addPositionUpdater(updater);
			document.addPosition(position);

			try {
				JavaModelUtil.applyEdit(cu, fImportRewrite.rewriteImports(null), false, null);

				setCompletionOffset(position.getOffset());
			} catch (CoreException e) {
				handleException(e);
			} finally {
				document.removePosition(position);
				document.removePositionUpdater(updater);
				document.removePositionCategory(category);
			}
		} catch (BadLocationException | BadPositionCategoryException e) {
			handleException(e);
		}
	}

	private CompilationUnit getASTRoot(ICompilationUnit compilationUnit) {
		return SharedASTProviderCore.getAST(compilationUnit, SharedASTProviderCore.WAIT_NO, new NullProgressMonitor());
	}

	/*
	 * Finds a type by the simple name. From AddImportsOperation
	 */
	private TypeNameMatch[] findAllTypes(String simpleTypeName, IJavaSearchScope searchScope, SimpleName nameNode, IProgressMonitor monitor, ICompilationUnit cu) throws JavaModelException {
		boolean is50OrHigher= JavaModelUtil.is50OrHigher(cu.getJavaProject());

		int typeKinds= TypeKinds.ALL_TYPES;
		if (nameNode != null) {
			typeKinds= ASTResolving.getPossibleTypeKinds(nameNode, is50OrHigher);
		}

		ArrayList<TypeNameMatch> typeInfos= new ArrayList<>();
		TypeNameMatchCollector requestor= new TypeNameMatchCollector(typeInfos);
		new SearchEngine().searchAllTypeNames(null, 0, simpleTypeName.toCharArray(), SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE, getSearchForConstant(typeKinds), searchScope, requestor, IJavaSearchConstants.FORCE_IMMEDIATE_SEARCH, monitor);

		ArrayList<TypeNameMatch> typeRefsFound= new ArrayList<>(typeInfos.size());
		for (TypeNameMatch curr : typeInfos) {
			if (curr.getPackageName().length() > 0) { // do not suggest imports from the default package
				if (isOfKind(curr, typeKinds, is50OrHigher) && isVisible(curr, cu)) {
					typeRefsFound.add(curr);
				}
			}
		}
		return typeRefsFound.toArray(new TypeNameMatch[typeRefsFound.size()]);
	}

	private int getSearchForConstant(int typeKinds) {
		final int CLASSES= TypeKinds.CLASSES;
		final int INTERFACES= TypeKinds.INTERFACES;
		final int ENUMS= TypeKinds.ENUMS;
		final int ANNOTATIONS= TypeKinds.ANNOTATIONS;

		switch (typeKinds & (CLASSES | INTERFACES | ENUMS | ANNOTATIONS)) {
			case CLASSES: return IJavaSearchConstants.CLASS;
			case INTERFACES: return IJavaSearchConstants.INTERFACE;
			case ENUMS: return IJavaSearchConstants.ENUM;
			case ANNOTATIONS: return IJavaSearchConstants.ANNOTATION_TYPE;
			case CLASSES | INTERFACES: return IJavaSearchConstants.CLASS_AND_INTERFACE;
			case CLASSES | ENUMS: return IJavaSearchConstants.CLASS_AND_ENUM;
			default: return IJavaSearchConstants.TYPE;
		}
	}

	private boolean isOfKind(TypeNameMatch curr, int typeKinds, boolean is50OrHigher) {
		int flags= curr.getModifiers();
		if (Flags.isAnnotation(flags)) {
			return is50OrHigher && ((typeKinds & TypeKinds.ANNOTATIONS) != 0);
		}
		if (Flags.isEnum(flags)) {
			return is50OrHigher && ((typeKinds & TypeKinds.ENUMS) != 0);
		}
		if (Flags.isInterface(flags)) {
			return (typeKinds & TypeKinds.INTERFACES) != 0;
		}
		return (typeKinds & TypeKinds.CLASSES) != 0;
	}


	private boolean isVisible(TypeNameMatch curr, ICompilationUnit cu) {
		int flags= curr.getModifiers();
		if (Flags.isPrivate(flags)) {
			return false;
		}
		if (Flags.isPublic(flags) || Flags.isProtected(flags)) {
			return true;
		}
		return curr.getPackageName().equals(cu.getParent().getElementName());
	}

	@Override
	public TemplateVariable getTemplateVariable(String name) {
		TemplateVariable variable= fVariables.get(name);
		if (variable != null && !variable.isResolved())
			getContextType().resolve(variable, this);
		return variable;
	}

	@Override
	public void addDependency(MultiVariable master, MultiVariable slave) {
		// Do nothing
	}

}
