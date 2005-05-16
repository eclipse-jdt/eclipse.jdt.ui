/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.Corext;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.dom.fragments.ASTFragmentFactory;
import org.eclipse.jdt.internal.corext.dom.fragments.IASTFragment;
import org.eclipse.jdt.internal.corext.dom.fragments.IExpressionFragment;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaRefactorings;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringAnalyzeUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.CodeGeneration;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;


public class ExtractConstantRefactoring extends Refactoring {

	public static final String PUBLIC= 	JdtFlags.VISIBILITY_STRING_PUBLIC;
	public static final String PROTECTED= JdtFlags.VISIBILITY_STRING_PROTECTED;
	public static final String PACKAGE= 	JdtFlags.VISIBILITY_STRING_PACKAGE;
	public static final String PRIVATE= 	JdtFlags.VISIBILITY_STRING_PRIVATE;

	private static final String MODIFIER= "static final"; //$NON-NLS-1$
	private static final String[] KNOWN_METHOD_NAME_PREFIXES= {"get", "is"}; //$NON-NLS-2$ //$NON-NLS-1$
	
	
	private CompilationUnitRewrite fCuRewrite;
	private final int fSelectionStart;
	private final int fSelectionLength;
	private final ICompilationUnit fCu;

	private IExpressionFragment fSelectedExpression;
	private Type fConstantType;
	private boolean fReplaceAllOccurrences= true; //default value
	private boolean fQualifyReferencesWithDeclaringClassName= false;	//default value

	private String fAccessModifier= PRIVATE; //default value
	private boolean fTargetIsInterface= false;
	private String fConstantName= ""; //$NON-NLS-1$;
	private String[] fExcludedVariableNames;

	private boolean fSelectionAllStaticFinal;
	private boolean fAllStaticFinalCheckPerformed= false;
	
	private List fBodyDeclarations;
	
	//Constant Declaration Location
	private BodyDeclaration fToInsertAfter;
	private boolean fInsertFirst;
	
	private CompilationUnitChange fChange;

	private ExtractConstantRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		Assert.isTrue(cu.exists());
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCu= cu;
	}
	
	public static ExtractConstantRefactoring create(ICompilationUnit cu, int selectionStart, int selectionLength) {
		return new ExtractConstantRefactoring(cu, selectionStart, selectionLength);
	}
	
	public String getName() {
		return RefactoringCoreMessages.ExtractConstantRefactoring_name; 
	}

	public boolean replaceAllOccurrences() {
		return fReplaceAllOccurrences;
	}

	public void setReplaceAllOccurrences(boolean replaceAllOccurrences) {
		fReplaceAllOccurrences= replaceAllOccurrences;
	}
	
	public void setAccessModifier(String am) {
		Assert.isTrue(
			am == PRIVATE || am == PROTECTED || am == PACKAGE || am == PUBLIC
		);
		fAccessModifier= am;
	}
	
	public String getAccessModifier() {
		return fAccessModifier;	
	}
	
	public boolean getTargetIsInterface() {
		return fTargetIsInterface;
	}

	public boolean qualifyReferencesWithDeclaringClassName() {
		return fQualifyReferencesWithDeclaringClassName;
	}
	
	public void setQualifyReferencesWithDeclaringClassName(boolean qualify) {
		fQualifyReferencesWithDeclaringClassName= qualify;
	}
	
	public String guessConstantName() throws JavaModelException {
		String[] proposals= guessConstantNames();
		if (proposals.length > 0)
			return proposals[0];
		else
			return ""; //$NON-NLS-1$
	}
	
	/**
	 * @return proposed variable names (may be empty, but not null).
	 * The first proposal should be used as "best guess" (if it exists).
	 */
	public String[] guessConstantNames() {
		LinkedHashSet proposals= new LinkedHashSet(); // retain ordering, but prevent duplicates
		try {
			String[] excludedVariableNames= getExcludedVariableNames();
			ASTNode associatedNode= getSelectedExpression().getAssociatedNode();
			if (associatedNode instanceof StringLiteral) {
				String literal= ((StringLiteral) associatedNode).getLiteralValue();
				String guess= guessConstantNameFromString(literal, excludedVariableNames);
				return guess.length() == 0 ? new String[0] : new String[] { guess };
			} else if (associatedNode instanceof NumberLiteral) {
				String literal= ((NumberLiteral) associatedNode).getToken();
				String guess= guessConstantNameFromString(literal, excludedVariableNames);
				return guess.length() == 0 ? new String[0] : new String[] { guess };
			}
				
			if (associatedNode instanceof MethodInvocation) {
				proposals.addAll(guessConstNamesFromMethodInvocation((MethodInvocation) associatedNode, excludedVariableNames));
			} else if (associatedNode instanceof CastExpression) {
				Expression expression= ((CastExpression) associatedNode).getExpression();
				if (expression instanceof MethodInvocation) {
					proposals.addAll(guessConstNamesFromMethodInvocation((MethodInvocation) expression, excludedVariableNames));
				}
			}
			if (associatedNode instanceof Expression) {
				proposals.addAll(guessConstNamesFromExpression((Expression) associatedNode, excludedVariableNames));
			}
		} catch (JavaModelException e) {
			// too bad ... no proposals this time
			JavaPlugin.log(e); //no ui here, just log
			return new String[0];
		}
		return (String[]) proposals.toArray(new String[proposals.size()]);
	}
	
	private String[] getExcludedVariableNames() {
		if (fExcludedVariableNames == null) {
			try {
				IBinding[] bindings= new ScopeAnalyzer(fCuRewrite.getRoot()).getDeclarationsInScope(getSelectedExpression().getStartPosition(), ScopeAnalyzer.VARIABLES);
				fExcludedVariableNames= new String[bindings.length];
				for (int i= 0; i < bindings.length; i++) {
					fExcludedVariableNames[i]= bindings[i].getName();
				}
			} catch (JavaModelException e) {
				fExcludedVariableNames= new String[0];
			}
		}
		return fExcludedVariableNames;
	}
	
	private static String guessConstantNameFromString(String string, String[] excludedNames) {
		StringBuffer result= new StringBuffer();
		int i= 0;
		char ch= '_';

		// run to first valid identifier part:
		for (; i < string.length(); i++) {
			ch= string.charAt(i);
			if (Character.isJavaIdentifierStart(ch)) {
				result.append(Character.toUpperCase(ch));
				i++;
				break;
			} else if (Character.isJavaIdentifierPart(ch)) {
				result.append('_').append(Character.toUpperCase(ch));
				i++;
				break;
			}
		}

		// add remaining characters, replacing titleCase by TITLE_CASE and sequences of invalid characters by _:
		boolean wasLastCharLowerCase= Character.isLowerCase(ch);
		boolean wasJavaIdentifierPart= Character.isJavaIdentifierPart(ch);
		for (; i < string.length(); i++) {
			ch= string.charAt(i);
			if (Character.isJavaIdentifierPart(ch)) {
				if (wasLastCharLowerCase && Character.isUpperCase(ch))
					result.append('_').append(Character.toUpperCase(ch));
				else
					result.append(Character.toUpperCase(ch));
				wasLastCharLowerCase= Character.isLowerCase(ch);
				wasJavaIdentifierPart= true;
				
			} else {
				if (wasLastCharLowerCase || wasJavaIdentifierPart)
					result.append('_');
				wasLastCharLowerCase= false;
				wasJavaIdentifierPart= false;
			}
		}
		
		if (result.length() > 0 && result.charAt(result.length() - 1) == '_')
			result.deleteCharAt(result.length() - 1);
		
		return result.toString();
	}
	
	private List guessConstNamesFromMethodInvocation(MethodInvocation selectedMethodInvocation, String[] excludedVariableNames) {
		String methodName= selectedMethodInvocation.getName().getIdentifier();
		for (int i= 0; i < KNOWN_METHOD_NAME_PREFIXES.length; i++) {
			String prefix= KNOWN_METHOD_NAME_PREFIXES[i];
			if (!methodName.startsWith(prefix))
				continue; // not this prefix
			if (methodName.length() == prefix.length())
				return Collections.EMPTY_LIST;
			char firstAfterPrefix= methodName.charAt(prefix.length());
			if (!Character.isUpperCase(firstAfterPrefix))
				continue;
			String proposal= Character.toLowerCase(firstAfterPrefix) + methodName.substring(prefix.length() + 1);
			methodName= proposal;
			break;
		}
		return getConstantNameSuggestions(methodName, 0, excludedVariableNames);
	}

	private List guessConstNamesFromExpression(Expression selectedExpression, String[] excludedVariableNames) {
		ITypeBinding expressionBinding= selectedExpression.resolveTypeBinding();
		ITypeBinding normalizedBinding= Bindings.normalizeTypeBinding(expressionBinding).getTypeDeclaration();
		if (normalizedBinding.isArray())
			normalizedBinding= normalizedBinding.getElementType();
		
		if (normalizedBinding.isPrimitive())
			return Collections.EMPTY_LIST;
		
		String typeName= normalizedBinding.getName();
		if (typeName.length() == 0)
			return Collections.EMPTY_LIST;
		int typeParamStart= typeName.indexOf("<"); //$NON-NLS-1$
		if (typeParamStart != -1)
			typeName= typeName.substring(0, typeParamStart);
		
		return getConstantNameSuggestions(typeName, expressionBinding.getDimensions(), excludedVariableNames);
	}

	private List getConstantNameSuggestions(String baseName, int dimensions, String[] excludedVariableNames) {
		int staticFinal= Flags.AccStatic | Flags.AccFinal;
		String[] proposals= StubUtility.getFieldNameSuggestions(fCu.getJavaProject(), baseName, dimensions, staticFinal, excludedVariableNames);
		return Arrays.asList(proposals);
	}

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("", 7); //$NON-NLS-1$
	
			RefactoringStatus result= Checks.validateEdit(fCu, getValidationContext());
			if (result.hasFatalError())
				return result;
			pm.worked(1);
	
			if (!fCu.isStructureKnown())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractConstantRefactoring_syntax_error); 
			pm.worked(1);
	
			fCuRewrite= new CompilationUnitRewrite(fCu);
	
			result.merge(checkSelection(new SubProgressMonitor(pm, 5)));
			if (result.hasFatalError())
				return result;
			
			if (isLiteralNodeSelected())
				fReplaceAllOccurrences= false;
			
			ITypeBinding targetType= getContainingTypeBinding();
			if (targetType.isAnnotation() || targetType.isInterface()) {
				fTargetIsInterface= true;
				fAccessModifier= PUBLIC;
			}
			
			return result;
		} finally {
			pm.done();
		}
	}
	
	public boolean selectionAllStaticFinal() {
		Assert.isTrue(fAllStaticFinalCheckPerformed);
		return fSelectionAllStaticFinal;
	}

	private void checkAllStaticFinal() throws JavaModelException {
		fSelectionAllStaticFinal= ConstantChecks.isStaticFinalConstant(getSelectedExpression());
		fAllStaticFinalCheckPerformed= true;
	}

	private RefactoringStatus checkSelection(IProgressMonitor pm) throws JavaModelException {
		try {
			pm.beginTask("", 2); //$NON-NLS-1$
			
			IExpressionFragment selectedExpression= getSelectedExpression();
			
			if (selectedExpression == null) {
				String message= RefactoringCoreMessages.ExtractConstantRefactoring_select_expression; 
				return CodeRefactoringUtil.checkMethodSyntaxErrors(fSelectionStart, fSelectionLength, fCuRewrite.getRoot(), message);
			}
			pm.worked(1);
			
			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkExpression());
			if (result.hasFatalError())
				return result;
			pm.worked(1);
			
			return result;
		} finally {
			pm.done();
		}
	}

	private RefactoringStatus checkExpressionBinding() throws JavaModelException {
		return checkExpressionFragmentIsRValue();
	}
	
	private RefactoringStatus checkExpressionFragmentIsRValue() throws JavaModelException {
		/* Moved this functionality to Checks, to allow sharing with
		   ExtractTempRefactoring, others */
		switch(Checks.checkExpressionIsRValue(getSelectedExpression().getAssociatedExpression())) {
			case Checks.NOT_RVALUE_MISC:
				return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.ExtractConstantRefactoring_select_expression, null, Corext.getPluginId(), RefactoringStatusCodes.EXPRESSION_NOT_RVALUE, null); 
			case Checks.NOT_RVALUE_VOID:
				return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.ExtractConstantRefactoring_no_void, null, Corext.getPluginId(), RefactoringStatusCodes.EXPRESSION_NOT_RVALUE_VOID, null); 
			case Checks.IS_RVALUE:
				return new RefactoringStatus();
			default:
				Assert.isTrue(false); return null;
		}		
	}

	//	 !!! -- same as in ExtractTempRefactoring
	private boolean isLiteralNodeSelected() throws JavaModelException {
		IExpressionFragment fragment= getSelectedExpression();
		if (fragment == null)
			return false;
		Expression expression= fragment.getAssociatedExpression();
		if (expression == null)
			return false;
		switch (expression.getNodeType()) {
			case ASTNode.BOOLEAN_LITERAL :
			case ASTNode.CHARACTER_LITERAL :
			case ASTNode.NULL_LITERAL :
			case ASTNode.NUMBER_LITERAL :
				return true;
			
			default :
				return false;
		}
	}

	private RefactoringStatus checkExpression() throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkExpressionBinding());
		if(result.hasFatalError())
			return result;
		checkAllStaticFinal();

		IExpressionFragment selectedExpression= getSelectedExpression();
		Expression associatedExpression= selectedExpression.getAssociatedExpression();
		if (associatedExpression instanceof NullLiteral)
			result.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractConstantRefactoring_null_literals)); 
		else if (!ConstantChecks.isLoadTimeConstant(selectedExpression))
			result.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractConstantRefactoring_not_load_time_constant)); 
		else if (associatedExpression instanceof SimpleName) {
			if (associatedExpression.getParent() instanceof QualifiedName && associatedExpression.getLocationInParent() == QualifiedName.NAME_PROPERTY
					|| associatedExpression.getParent() instanceof FieldAccess && associatedExpression.getLocationInParent() == FieldAccess.NAME_PROPERTY)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractConstantRefactoring_select_expression);
		}
		
		return result;
	}

	public void setConstantName(String newName) {
		Assert.isNotNull(newName);
		fConstantName= newName;
	}

	public String getConstantName() {
		return fConstantName;
	}

	/**
	 * This method performs checks on the constant name which are
	 * quick enough to be performed every time the ui input component
	 * contents are changed.
	 */
	public RefactoringStatus checkConstantNameOnChange() throws JavaModelException {
		if (Arrays.asList(getExcludedVariableNames()).contains(fConstantName))
			return RefactoringStatus.createErrorStatus(Messages.format(RefactoringCoreMessages.ExtractConstantRefactoring_another_variable, getConstantName())); 
		return Checks.checkConstantName(getConstantName());
	}
	
	// !! similar to ExtractTempRefactoring equivalent
	public String getConstantSignaturePreview() throws JavaModelException {
		String space= " "; //$NON-NLS-1$
		return getAccessModifier() + space + MODIFIER + space + getConstantTypeName() + space + fConstantName;
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		pm.beginTask(RefactoringCoreMessages.ExtractConstantRefactoring_checking_preconditions, 4); 
		
		/* Note: some checks are performed on change of input widget
		 * values. (e.g. see ExtractConstantRefactoring.checkConstantNameOnChange())
		 */ 
		
		//TODO: possibly add more checking for name conflicts that might
		//      lead to a change in behaviour
		
		try {
			RefactoringStatus result= new RefactoringStatus();
			
			createConstantDeclaration();
			fChange= fCuRewrite.createChange();
			pm.worked(1);
			
			TextEdit[] replaceEdits= createReplaceExpressionWithConstantEdits();
			for (int i= 0; i < replaceEdits.length; i++) {
				TextChangeCompatibility.addTextEdit(fChange, RefactoringCoreMessages.ExtractConstantRefactoring_replace, replaceEdits[i]); 
			}
			pm.worked(1);
			
			String newCuSource= fChange.getPreviewContent(new NullProgressMonitor());
			ASTParser p= ASTParser.newParser(AST.JLS3);
			p.setSource(newCuSource.toCharArray());
			p.setUnitName(fCu.getElementName());
			p.setProject(fCu.getJavaProject());
			p.setCompilerOptions(RefactoringASTParser.getCompilerOptions(fCu));
			CompilationUnit newCUNode= (CompilationUnit) p.createAST(null);
			IProblem[] newProblems= RefactoringAnalyzeUtil.getIntroducedCompileProblems(newCUNode, fCuRewrite.getRoot());
			for (int i= 0; i < newProblems.length; i++) {
				IProblem problem= newProblems[i];
				if (problem.isError())
					result.addEntry(JavaRefactorings.createStatusEntry(problem, newCuSource));
			}

			return result;
		} finally {
			pm.done();
		}
	}

	private void createConstantDeclaration() throws CoreException {
		Type type= getConstantType();
		
		IExpressionFragment fragment= getSelectedExpression();
		String initializerSource= fCu.getBuffer().getText(fragment.getStartPosition(), fragment.getLength());
		
		AST ast= fCuRewrite.getAST();
		VariableDeclarationFragment variableDeclarationFragment= ast.newVariableDeclarationFragment();
		variableDeclarationFragment.setName(ast.newSimpleName(fConstantName));
		variableDeclarationFragment.setInitializer((Expression) fCuRewrite.getASTRewrite().createStringPlaceholder(initializerSource, ASTNode.SIMPLE_NAME));
		
		FieldDeclaration fieldDeclaration= ast.newFieldDeclaration(variableDeclarationFragment);
		fieldDeclaration.setType(type);
		Modifier.ModifierKeyword accessModifier= Modifier.ModifierKeyword.toKeyword(fAccessModifier);
		if (accessModifier != null)
			fieldDeclaration.modifiers().add(ast.newModifier(accessModifier));
		fieldDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD));
		fieldDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD));
		
		boolean createComments= JavaPreferencesSettings.getCodeGenerationSettings(fCu.getJavaProject()).createComments;
		if (createComments) {
			String comment= CodeGeneration.getFieldComment(fCu, getConstantTypeName(), fConstantName, StubUtility.getLineDelimiterUsed(fCu));
			if (comment != null && comment.length() > 0) {
				Javadoc doc= (Javadoc) fCuRewrite.getASTRewrite().createStringPlaceholder(comment, ASTNode.JAVADOC);
				fieldDeclaration.setJavadoc(doc);
			}
		}
		
		AbstractTypeDeclaration parent= getContainingTypeDeclarationNode();
		ListRewrite listRewrite= fCuRewrite.getASTRewrite().getListRewrite(parent, parent.getBodyDeclarationsProperty());
		TextEditGroup msg= fCuRewrite.createGroupDescription(RefactoringCoreMessages.ExtractConstantRefactoring_declare_constant); 
		if (insertFirst()) {
			listRewrite.insertFirst(fieldDeclaration, msg);
		} else {
			listRewrite.insertAfter(fieldDeclaration, getNodeToInsertConstantDeclarationAfter(), msg);
		}
	}

	private Type getConstantType() throws JavaModelException {
		if (fConstantType == null) {
			IExpressionFragment fragment= getSelectedExpression();
			ITypeBinding typeBinding= fragment.getAssociatedExpression().resolveTypeBinding();
			AST ast= fCuRewrite.getAST();
			typeBinding= Bindings.normalizeForDeclarationUse(typeBinding, ast);
			fConstantType= fCuRewrite.getImportRewrite().addImport(typeBinding, ast);
		}
		return fConstantType;
	}

	public Change createChange(IProgressMonitor monitor) throws CoreException {
		return fChange;
	}

	private TextEdit[] createReplaceExpressionWithConstantEdits() throws JavaModelException {
		IASTFragment[] fragmentsToReplace= getFragmentsToReplace();
		TextEdit[] result= new TextEdit[fragmentsToReplace.length];
		for (int i= 0; i < fragmentsToReplace.length; i++)
			result[i]= createReplaceEdit(fragmentsToReplace[i]);
		
		return result;
	}

	private ReplaceEdit createReplaceEdit(IASTFragment fragment) throws JavaModelException {
		int offset= fragment.getStartPosition();
		int length= fragment.getLength();
		String constantReference= getNewConstantReference();
		ReplaceEdit replaceEdit= new ReplaceEdit(offset, length, constantReference);
		return replaceEdit;
	}
	
	private String getNewConstantReference() throws JavaModelException {
		if(qualifyReferencesWithDeclaringClassName())
			return getContainingTypeBinding().getName() + "." + fConstantName; //$NON-NLS-1$
		else
			return fConstantName; //$NON-NLS-1$
	}

	private void computeConstantDeclarationLocation() throws JavaModelException {
		if (isDeclarationLocationComputed())
			return;

		BodyDeclaration lastStaticDependency= null;
		Iterator decls= getBodyDeclarations();
		
		Assert.isTrue(decls.hasNext()); /* Admissible selected expressions must occur
		                                   within a body declaration.  Thus, the 
		                                   class/interface in which such an expression occurs
		                                   must have at least one body declaration */
		
		while (decls.hasNext()) {
			BodyDeclaration decl= (BodyDeclaration) decls.next();
			
			int modifiers;
			if (decl instanceof FieldDeclaration)
				modifiers= ((FieldDeclaration) decl).getModifiers();
			else if (decl instanceof Initializer)
				modifiers= ((Initializer) decl).getModifiers();
			else {
				continue; /* this declaration is not a field declaration
				              or initializer, so the placement of the constant
				              declaration relative to it does not matter */
			}
			
			if (Modifier.isStatic(modifiers) && depends(getSelectedExpression(), decl))
				lastStaticDependency= decl;
		}
		
		if(lastStaticDependency == null)
			fInsertFirst= true;
		else
			fToInsertAfter= lastStaticDependency;
	}
	
	/** bd is a static field declaration or static initializer */
	private static boolean depends(IExpressionFragment selected, BodyDeclaration bd) {
		/* We currently consider selected to depend on bd only if db includes a declaration
		 * of a static field on which selected depends.
		 * 
		 * A more accurate strategy might be to also check if bd contains (or is) a
		 * static initializer containing code which changes the value of a static field on 
		 * which selected depends.  However, if a static is written to multiple times within
		 * during class initialization, it is difficult to predict which value should be used.
		 * This would depend on which value is used by expressions instances for which the new 
		 * constant will be substituted, and there may be many of these; in each, the
		 * static field in question may have taken on a different value (if some of these uses
		 * occur within static initializers).
		 */
		
		if(bd instanceof FieldDeclaration) {
			FieldDeclaration fieldDecl = (FieldDeclaration) bd;
			for(Iterator fragments = fieldDecl.fragments().iterator(); fragments.hasNext();) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.next();
				SimpleName staticFieldName = fragment.getName();
				if(selected.getSubFragmentsMatching(ASTFragmentFactory.createFragmentForFullSubtree(staticFieldName)).length != 0)
					return true;
			}
		}
		return false;
	}

	private boolean isDeclarationLocationComputed() {
		return fInsertFirst == true || fToInsertAfter != null;	
	}
	
	private boolean insertFirst() throws JavaModelException {
		if(!isDeclarationLocationComputed())
			computeConstantDeclarationLocation();
		return fInsertFirst;
	}
	
	private BodyDeclaration getNodeToInsertConstantDeclarationAfter() throws JavaModelException {
		if(!isDeclarationLocationComputed())
			computeConstantDeclarationLocation();
		return fToInsertAfter;
	}
	
	private Iterator getBodyDeclarations() throws JavaModelException {
		if(fBodyDeclarations == null)
			fBodyDeclarations= getContainingTypeDeclarationNode().bodyDeclarations();
		return fBodyDeclarations.iterator();
	}

	private String getConstantTypeName() throws JavaModelException {
		return ASTNodes.asString(getConstantType());
	}

	private static boolean isStaticFieldOrStaticInitializer(BodyDeclaration node) {
		if(node instanceof MethodDeclaration || node instanceof AbstractTypeDeclaration)
			return false;
		
		int modifiers;
		if(node instanceof FieldDeclaration) {
			modifiers = ((FieldDeclaration) node).getModifiers();
		} else if(node instanceof Initializer) {
			modifiers = ((Initializer) node).getModifiers();
		} else {
			Assert.isTrue(false);
			return false;
		}
		
		if(!Modifier.isStatic(modifiers))
			return false;
		
		return true;
	}
	/**
	 *   Elements returned by next() are BodyDeclaration
	 *   instances.
	 */
	private Iterator getReplacementScope() throws JavaModelException {
		boolean declPredecessorReached= false;
		
		Collection scope= new ArrayList();
		for(Iterator bodyDeclarations = getBodyDeclarations(); bodyDeclarations.hasNext();) {
		    BodyDeclaration bodyDeclaration= (BodyDeclaration) bodyDeclarations.next();
		    
		    if(bodyDeclaration == getNodeToInsertConstantDeclarationAfter())
		    	declPredecessorReached= true;
		    
		    if(insertFirst() || declPredecessorReached || !isStaticFieldOrStaticInitializer(bodyDeclaration))
		    	scope.add(bodyDeclaration);
		}
		return scope.iterator();
	}

	private IASTFragment[] getFragmentsToReplace() throws JavaModelException {
		List toReplace = new ArrayList();
		if (fReplaceAllOccurrences) {
			Iterator replacementScope = getReplacementScope();
			while(replacementScope.hasNext()) {
				BodyDeclaration bodyDecl = (BodyDeclaration) replacementScope.next();
				IASTFragment[] allMatches= ASTFragmentFactory.createFragmentForFullSubtree(bodyDecl).getSubFragmentsMatching(getSelectedExpression());
				IASTFragment[] replaceableMatches = retainOnlyReplacableMatches(allMatches);
				for(int i = 0; i < replaceableMatches.length; i++)
					toReplace.add(replaceableMatches[i]);
			}
		} else if (canReplace(getSelectedExpression()))
			toReplace.add(getSelectedExpression());
		return (IASTFragment[]) toReplace.toArray(new IASTFragment[toReplace.size()]);
	}

	// !! - like one in ExtractTempRefactoring
	private static IASTFragment[] retainOnlyReplacableMatches(IASTFragment[] allMatches) {
		List result= new ArrayList(allMatches.length);
		for (int i= 0; i < allMatches.length; i++) {
			if (canReplace(allMatches[i]))
				result.add(allMatches[i]);
		}
		return (IASTFragment[]) result.toArray(new IASTFragment[result.size()]);
	}

	// !! - like one in ExtractTempRefactoring
	private static boolean canReplace(IASTFragment fragment) {
		ASTNode node= fragment.getAssociatedNode();
		ASTNode parent= node.getParent();
		if (parent instanceof VariableDeclarationFragment) {
			VariableDeclarationFragment vdf= (VariableDeclarationFragment) parent;
			if (node.equals(vdf.getName()))
				return false;
		}
		if (parent instanceof ExpressionStatement)
			return false;
		if (parent instanceof SwitchCase)
			return false;
		return true;
	}

	private IExpressionFragment getSelectedExpression() throws JavaModelException {
		if(fSelectedExpression != null)
			return fSelectedExpression;
		
		IASTFragment selectedFragment= ASTFragmentFactory.createFragmentForSourceRange(new SourceRange(fSelectionStart, fSelectionLength), fCuRewrite.getRoot(), fCu);
		
		if (selectedFragment instanceof IExpressionFragment
				&& ! Checks.isInsideJavadoc(selectedFragment.getAssociatedNode())) {
			fSelectedExpression= (IExpressionFragment) selectedFragment;
		}
		
		if (fSelectedExpression != null && Checks.isEnumCase(fSelectedExpression.getAssociatedExpression().getParent())) {
			fSelectedExpression= null;
		}
		
		return fSelectedExpression;
	}

	private AbstractTypeDeclaration getContainingTypeDeclarationNode() throws JavaModelException {
		AbstractTypeDeclaration result= (AbstractTypeDeclaration) ASTNodes.getParent(getSelectedExpression().getAssociatedNode(), AbstractTypeDeclaration.class);  
		Assert.isNotNull(result);
		return result;
	}

	private ITypeBinding getContainingTypeBinding() throws JavaModelException {
		ITypeBinding result= getContainingTypeDeclarationNode().resolveBinding();
		Assert.isNotNull(result);
		return result;
	}

}
