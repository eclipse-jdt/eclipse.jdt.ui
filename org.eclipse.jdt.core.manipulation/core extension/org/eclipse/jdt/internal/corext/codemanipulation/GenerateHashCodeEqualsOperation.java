/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Pierre-Yves B. <pyvesdev@gmail.com> - Generation of equals and hashcode with java 7 Objects.equals and Objects.hashcode - https://bugs.eclipse.org/424214
 *     Pierre-Yves B. <pyvesdev@gmail.com> - Different behaviour when generating hashCode and equals - https://bugs.eclipse.org/539589
 *     Pierre-Yves B. <pyvesdev@gmail.com> - Confusing name when generating hashCode and equals with outer type - https://bugs.eclipse.org/539872
 *     Red Hat Inc. - refactored to jdt.core.manipulation
 *     Pierre-Yves B. <pyvesdev@gmail.com> - Allow hashCode and equals generation when no fields but a super/enclosing class that implements them - https://bugs.eclipse.org/539901
 *     Pierre-Yves B. <pyvesdev@gmail.com> - [hashcode/equals] Redundant null check when instanceof is used - https://bugs.eclipse.org/545424
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.PrimitiveType.Code;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.manipulation.CodeGeneration;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

/**
 * <p>
 * Workspace runnable to add implementations for
 * <code>{@link java.lang.Object#equals(java.lang.Object)}</code> and
 * <code>{@link java.lang.Object#hashCode()}</code>.
 * </p>
 *
 * <p>
 * This implementation creates a hashCode() and an equals() method intended to
 * be used in value types: The implementation assumes that two objects are equal
 * (and provide the same hashCode) if all values of all fields are equal.
 * </p>
 *
 * <p>
 * About the implementation:
 * <ul>
 * <li>To deal with reference types of fields and with supertypes, the
 * implementation calls hashCode() and equals() on reference types of fields and
 * on the superclass (except if the superclass is Object). It is an error if
 * these types do not provide an equals() and hashCode() implementation and the
 * comparison or hash code creation <strong>will fail</strong> if these methods
 * are not correctly implemented.</li>
 * <li>The implementation for primitive typed fields is the same as in the JDK
 * implementations in the wrapper types (and the java.util.Arrays class).</li>
 * <li>The equals() implementation uses equality of the declaring class instead
 * of an instanceof check.</li>
 * <li>A test for null in equals() is only implemented in direct subclasses of
 * Object. This is only sufficient if every subimplementation calls
 * super.equals() before any argument checks.</li>
 * <li>Both equals() and hashCode() use methods from java.util.Arrays to test
 * for equality and to generate a hash code for arrays. Note that this has an
 * implication for Double and Float values (consider values -0.0 and 0.0 as well
 * as border values like NaN and their equality in bit patterns) - however, the
 * implementation is consistent with the wrapper types java.lang.Float and
 * java.lang.Double.</li>
 * </ul>
 * </p>
 *
 * @since 3.2
 */
public final class GenerateHashCodeEqualsOperation implements IWorkspaceRunnable {

	private interface IHashCodeAccessProvider {

		Expression getThisAccess(String name);
	}

	private static final String JAVA_UTIL_ARRAYS= "java.util.Arrays"; //$NON-NLS-1$

	private static final String BOOLEAN_TRUE_CONSTANT= "1231"; //$NON-NLS-1$

	private static final String BOOLEAN_FALSE_CONSTANT= "1237"; //$NON-NLS-1$

	private static final String JAVA_LANG_OBJECT= "java.lang.Object"; //$NON-NLS-1$

	private static final String METHODNAME_GETCLASS= "getClass"; //$NON-NLS-1$

	private static final String METHODNAME_EQUALS= "equals"; //$NON-NLS-1$

	private static final String METHODNAME_DEEP_EQUALS= "deepEquals"; //$NON-NLS-1$

	private static final String METHODNAME_HASH_CODE= "hashCode"; //$NON-NLS-1$

	private static final String METHODNAME_HASH= "hash"; //$NON-NLS-1$

	private static final String METHODNAME_DEEP_HASH_CODE= "deepHashCode"; //$NON-NLS-1$

	private static final String METHODNAME_GET_ENCLOSING_INSTANCE= "getEnclosingInstance"; //$NON-NLS-1$

	private static final String PRIME_NUMBER= "31"; //$NON-NLS-1$

	private static final String INITIAL_HASHCODE_VALUE= "1"; //$NON-NLS-1$

	private static final String VARIABLE_NAME_DOUBLE_TEMPORARY= "temp"; //$NON-NLS-1$

	private static final String VARIABLE_NAME_PRIME= "prime"; //$NON-NLS-1$

	private static final String VARIABLE_NAME_RESULT= "result"; //$NON-NLS-1$

	private static final String VARIABLE_NAME_EQUALS_PARAM= "obj"; //$NON-NLS-1$

	private static final String VARIABLE_NAME_HASHCODE_PARAM= "array"; //$NON-NLS-1$

	private static final String VARIABLE_NAME_EQUALS_CASTED= "other"; //$NON-NLS-1$

	private static final String VARIABLE_NAME_INDEX= "index"; //$NON-NLS-1$

	private static final String JAVA_UTIL_OBJECTS= "java.util.Objects"; //$NON-NLS-1$

	private static final String TYPE_NAME_CLONEABLE= "Cloneable"; //$NON-NLS-1$

	private static final String TYPE_NAME_SERIALIZABLE= "Serializable"; //$NON-NLS-1$

	private static final String TYPE_NAME_OBJECT= "Object"; //$NON-NLS-1$

	/** Should the resulting edit be applied? */
	private final boolean fApply;

	/** The resulting text edit */
	private TextEdit fEdit= null;

	/** The insertion point, or <code>null</code> */
	private final IJavaElement fInsert;

	/** The variable binding keys to implement */
	private final IVariableBinding[] fFields;

	/** Should the regeneration of the methods be enforced? */
	private final boolean fForce;

	/** Should the compilation unit content be saved? */
	private final boolean fSave;

	/** The code generation settings to use */
	private final CodeGenerationSettings fSettings;

	/** The type declaration to add the methods to */
	private final ITypeBinding fType;

	/** The compilation unit ast node */
	private final CompilationUnit fUnit;

	/** The CURewrite to be used */
	private final CompilationUnitRewrite fRewrite;

	/** The ast to be used. Convenience accessor field */
	private final AST fAst;

	/** The number of double-typed fields handled so far */
	private int fDoubleCount;

	/** The primitive types to generate custom hashCode() methods for */
	private List<ITypeBinding> fCustomHashCodeTypes= new ArrayList<>();

	/** <code>true</code> to use 'instanceof' to compare types, <code>false</code> otherwise */
	private final boolean fUseInstanceOf;

	/**
	 * <code>true</code> to use newer hashcode and equals method generation, using Java 7+
	 * Objects.hash and Objects.equals, <code>false</code> to generate default methods
	 */
	private final boolean fUseJ7HashEquals;

	/** <code>true</code> to use blocks for then */
	private boolean fUseBlocksForThen;

	/** The import rewrite context, only initialized in {@link #run(IProgressMonitor)}. */
	private ImportRewriteContext fImportRewriteContext;

	/**
	 * Creates a new add hash code equals operation.
	 *
	 * @param type the type to add the methods to
	 * @param fields the method binding keys to implement
	 * @param unit the compilation unit ast node
	 * @param insert the insertion point, or <code>null</code>
	 * @param settings the code generation settings to use
	 * @param useInstanceof <code>true</code> to use 'instanceof' to compare types, <code>false</code> otherwise
	 * @param useJ7HashEquals <code>true</code> to use Java 7+ Objects.hash and Objects.equals methods, <code>false</code> otherwise
	 * @param force <code>true</code> to force the regeneration of existing methods,
	 *            <code>false</code> otherwise
	 * @param apply <code>true</code> if the resulting edit should be applied,
	 *            <code>false</code> otherwise
	 * @param save <code>true</code> if the changed compilation unit should be
	 *            saved, <code>false</code> otherwise
	 */
	public GenerateHashCodeEqualsOperation(final ITypeBinding type, final IVariableBinding[] fields, final CompilationUnit unit,
			final IJavaElement insert, final CodeGenerationSettings settings, final boolean useInstanceof, final boolean useJ7HashEquals, final boolean force, final boolean apply,
			final boolean save) {
		Assert.isNotNull(type);
		Assert.isNotNull(fields);
		Assert.isNotNull(unit);
		Assert.isNotNull(settings);
		Assert.isTrue(unit.getTypeRoot() instanceof ICompilationUnit);

		fType= type;
		fInsert= insert;
		fUnit= unit;
		fFields= fields;
		fSettings= settings;
		fUseInstanceOf= useInstanceof;
		fUseJ7HashEquals= useJ7HashEquals;
		fSave= save;
		fApply= apply;
		fDoubleCount= 0;
		fRewrite= new CompilationUnitRewrite((ICompilationUnit) fUnit.getTypeRoot(), fUnit);
		fForce= force;
		fAst= fRewrite.getAST();
		fUseBlocksForThen= false;
	}

	/**
	 * Defines if then statements should use blocks or not.
	 *
	 * @param useBlocksForThen if set, blocks are forced in if-then statements
	 */
	public void setUseBlocksForThen(boolean useBlocksForThen) {
		fUseBlocksForThen= useBlocksForThen;
	}

	/**
	 * Returns the resulting text edit.
	 *
	 * @return the resulting edit
	 */
	public TextEdit getResultingEdit() {
		return fEdit;
	}

	/**
	 * Returns the scheduling rule for this operation.
	 *
	 * @return the scheduling rule
	 */
	public ISchedulingRule getSchedulingRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	/*
	 * @see org.eclipse.core.resources.IWorkspaceRunnable#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void run(IProgressMonitor monitor) throws CoreException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(CodeGenerationMessages.GenerateHashCodeEqualsOperation_description);

			fCustomHashCodeTypes.clear();

			// get the declaration and the rewrite
			AbstractTypeDeclaration declaration= (AbstractTypeDeclaration) ASTNodes.findDeclaration(fType, fRewrite.getRoot());
			ListRewrite rewriter= fRewrite.getASTRewrite().getListRewrite(declaration, declaration.getBodyDeclarationsProperty());
			List<BodyDeclaration> list= declaration.bodyDeclarations();
			if (fType != null && rewriter != null) {

				ICompilationUnit cu= (ICompilationUnit) fUnit.getJavaElement();

				ASTNode insertion= StubUtility2Core.getNodeToInsertBefore(rewriter, fInsert);

				// equals(..)
				ITypeBinding[] objectAsParam= { declaration.getAST().resolveWellKnownType(JAVA_LANG_OBJECT) };
				BodyDeclaration oldEquals= fForce ? findMethodToReplace(list, METHODNAME_EQUALS, objectAsParam) : null;

				fImportRewriteContext= new ContextSensitiveImportRewriteContext(declaration, fRewrite.getImportRewrite());
				MethodDeclaration equalsMethod= createEqualsMethod();
				addMethod(rewriter, insertion, equalsMethod, oldEquals);

				if (monitor.isCanceled())
					throw new OperationCanceledException();

				// hashCode()
				BodyDeclaration oldHash= fForce ? findMethodToReplace(list, METHODNAME_HASH_CODE, new ITypeBinding[0]) : null;

				MethodDeclaration hashCodeMethod= createHashCodeMethod();
				addMethod(rewriter, equalsMethod, hashCodeMethod, oldHash);

				// helpers
				for (ITypeBinding binding : fCustomHashCodeTypes) {
					if (findMethodToReplace(list, METHODNAME_HASH_CODE, objectAsParam) == null) {
						final MethodDeclaration helperDecl= createHashCodeHelper(binding);
						addHelper(rewriter, null, helperDecl);
					}
				}

				if (isMemberType()) {
					if (findMethodToReplace(list, METHODNAME_GET_ENCLOSING_INSTANCE, new ITypeBinding[0]) == null) {
						final MethodDeclaration helperDecl= createGetEnclosingInstanceHelper();
						rewriter.insertLast(helperDecl, null);
					}
				}

				fEdit= fRewrite.createChange(true).getEdit();
				if (fApply)
					JavaModelUtil.applyEdit(cu, fEdit, fSave, monitor);
			}
		} finally {
			monitor.done();
		}
	}

	private boolean isMemberType() {
		return fType.isMember() && !Modifier.isStatic(fType.getModifiers());
	}

	private BodyDeclaration findMethodToReplace(final List<BodyDeclaration> list, String name, ITypeBinding[] paramTypes) {
		for (BodyDeclaration bodyDecl : list) {
			if (bodyDecl instanceof MethodDeclaration) {
				final MethodDeclaration method= (MethodDeclaration) bodyDecl;
				final IMethodBinding binding= method.resolveBinding();
				if (binding != null && binding.getName().equals(name)) {
					if (Bindings.equals(binding.getParameterTypes(), paramTypes)) {
						return method;
					}
				}
			}
		}
		return null;
	}

	private void addHelper(ListRewrite rewriter, ASTNode insertion, MethodDeclaration stub) {
		if (insertion != null)
			rewriter.insertBefore(stub, insertion, null);
		else
			rewriter.insertFirst(stub, null);
	}

	private void addMethod(ListRewrite rewriter, ASTNode insertion, MethodDeclaration stub, BodyDeclaration replace) {
		if (replace != null) {
			rewriter.replace(replace, stub, null);
		} else {
		if (insertion != null)
			rewriter.insertBefore(stub, insertion, null);
		else
			rewriter.insertLast(stub, null);
		}
	}

	// ******************* HASHCODE *******************

	private MethodDeclaration createHashCodeMethod() throws CoreException {

		MethodDeclaration hashCodeMethod= fAst.newMethodDeclaration();
		hashCodeMethod.modifiers().addAll(ASTNodeFactory.newModifiers(fAst, Modifier.PUBLIC));
		hashCodeMethod.setName(fAst.newSimpleName(METHODNAME_HASH_CODE));
		hashCodeMethod.setConstructor(false);
		hashCodeMethod.setReturnType2(fAst.newPrimitiveType(PrimitiveType.INT));

		Block body= fAst.newBlock();
		hashCodeMethod.setBody(body);

		boolean needsNoSuperCall= needsNoSuperCall(fType, METHODNAME_HASH_CODE, new ITypeBinding[0]);
		boolean memberType= isMemberType();
		ReturnStatement endReturn= fAst.newReturnStatement();
		if (!memberType && fFields.length == 0) {
			// return super.hashCode();
			SuperMethodInvocation invoc= fAst.newSuperMethodInvocation();
			invoc.setName(fAst.newSimpleName(METHODNAME_HASH_CODE));
			endReturn.setExpression(invoc);
		} else if (fUseJ7HashEquals && needsNoSuperCall && !memberType && containsNoArrays()) {
			// return Objects.hash(...);
			endReturn.setExpression(createStandaloneJ7HashCall());
		} else {
			// final int prime = 31;
			VariableDeclarationFragment frag= fAst.newVariableDeclarationFragment();
			frag.setName(fAst.newSimpleName(VARIABLE_NAME_PRIME));
			frag.setInitializer(fAst.newNumberLiteral(PRIME_NUMBER));

			VariableDeclarationStatement primeNumberDeclaration= fAst.newVariableDeclarationStatement(frag);
			primeNumberDeclaration.modifiers().add(fAst.newModifier(ModifierKeyword.FINAL_KEYWORD));
			primeNumberDeclaration.setType(fAst.newPrimitiveType(PrimitiveType.INT));
			body.statements().add(primeNumberDeclaration);

			VariableDeclarationFragment fragment= fAst.newVariableDeclarationFragment();
			fragment.setName(fAst.newSimpleName(VARIABLE_NAME_RESULT));

			VariableDeclarationStatement resultDeclaration= fAst.newVariableDeclarationStatement(fragment);
			resultDeclaration.setType(fAst.newPrimitiveType(PrimitiveType.INT));
			body.statements().add(resultDeclaration);

			if (needsNoSuperCall) {
				// int result = 1;
				fragment.setInitializer(fAst.newNumberLiteral(INITIAL_HASHCODE_VALUE));
			} else {
				// int result = super.hashCode();
				SuperMethodInvocation invoc= fAst.newSuperMethodInvocation();
				invoc.setName(fAst.newSimpleName(METHODNAME_HASH_CODE));
				fragment.setInitializer(invoc);
			}

			if (memberType) {
				// result = prime * result + getEnclosingInstance().hashCode();
				body.statements().add(createAddEnclosingInstanceHashCode());
			}

			MethodInvocation j7Invoc= fAst.newMethodInvocation();
			for (IVariableBinding field : fFields) {
				if (field.getType().isArray()) {
					body.statements().add(createAddArrayHashCode(field));
				} else if (fUseJ7HashEquals) {
					j7Invoc.arguments().add(fAst.newSimpleName(field.getName()));
				} else if (field.getType().isPrimitive()) {
					Statement[] sts= createAddSimpleHashCode(field.getType(), this::getThisAccessForHashCode, field.getName(), false);
					body.statements().addAll(Arrays.asList(sts));
				} else {
					body.statements().add(createAddQualifiedHashCode(field));
				}
			}
			if (!j7Invoc.arguments().isEmpty()) {
				j7Invoc.setExpression(getQualifiedName(JAVA_UTIL_OBJECTS));
				j7Invoc.setName(fAst.newSimpleName(METHODNAME_HASH));
				body.statements().add(prepareAssignment(j7Invoc));
			}
			endReturn.setExpression(fAst.newSimpleName(VARIABLE_NAME_RESULT));
		}

		// the last return:
		body.statements().add(endReturn);

		// method comment
		if (fSettings != null) {
			ITypeBinding object= fAst.resolveWellKnownType(JAVA_LANG_OBJECT);
			IMethodBinding objectMethod= null;
			for (IMethodBinding objm : object.getDeclaredMethods()) {
				if (METHODNAME_HASH_CODE.equals(objm.getName()) && objm.getParameterTypes().length == 0) {
					objectMethod= objm;
				}
			}
			createMethodComment(hashCodeMethod, objectMethod);
		}

		return hashCodeMethod;
	}

	private boolean containsNoArrays() {
		return Arrays.stream(fFields).map(IVariableBinding::getType).noneMatch(ITypeBinding::isArray);
	}

	private MethodInvocation createStandaloneJ7HashCall() {
		MethodInvocation j7Invoc= fAst.newMethodInvocation();
		for (IVariableBinding field : fFields) {
			j7Invoc.arguments().add(fAst.newSimpleName(field.getName()));
		}
		j7Invoc.setExpression(getQualifiedName(JAVA_UTIL_OBJECTS));
		j7Invoc.setName(fAst.newSimpleName(METHODNAME_HASH));
		return j7Invoc;
	}

	private Statement createAddEnclosingInstanceHashCode() {
		MethodInvocation enclosing= fAst.newMethodInvocation();
		enclosing.setName(fAst.newSimpleName(METHODNAME_GET_ENCLOSING_INSTANCE));
		MethodInvocation hashAccess= fAst.newMethodInvocation();
		hashAccess.setName(fAst.newSimpleName(METHODNAME_HASH_CODE));
		hashAccess.setExpression(enclosing);
		return prepareAssignment(hashAccess);
	}

	private Statement[] createAddSimpleHashCode(ITypeBinding type, IHashCodeAccessProvider provider, String name, boolean singleTemp) {

		List<Statement> statements= new ArrayList<>();

		if (!type.isPrimitive()) {
			// (element == null ? 0 : element.hashCode())
			ConditionalExpression ce= fAst.newConditionalExpression();
			InfixExpression exp= fAst.newInfixExpression();
			ArrayAccess access= fAst.newArrayAccess();
			access.setArray(fAst.newSimpleName(VARIABLE_NAME_HASHCODE_PARAM));
			access.setIndex(fAst.newSimpleName(VARIABLE_NAME_INDEX));
			exp.setLeftOperand(access);
			exp.setOperator(Operator.EQUALS);
			exp.setRightOperand(fAst.newNullLiteral());
			ce.setExpression(exp);
			ce.setThenExpression(fAst.newNumberLiteral("0")); //$NON-NLS-1$
			MethodInvocation invoc= fAst.newMethodInvocation();
			access= fAst.newArrayAccess();
			access.setArray(fAst.newSimpleName(VARIABLE_NAME_HASHCODE_PARAM));
			access.setIndex(fAst.newSimpleName(VARIABLE_NAME_INDEX));
			invoc.setExpression(access);
			invoc.setName(fAst.newSimpleName(METHODNAME_HASH_CODE));
			ce.setElseExpression(invoc);
			statements.add(prepareAssignment(parenthesize(ce)));
		} else if (isPrimitiveType(type, PrimitiveType.BOOLEAN)) {
			ConditionalExpression ce= fAst.newConditionalExpression();
			ce.setExpression(provider.getThisAccess(name));
			// see Boolean.hashCode(boolean)
			ce.setThenExpression(fAst.newNumberLiteral(BOOLEAN_TRUE_CONSTANT));
			ce.setElseExpression(fAst.newNumberLiteral(BOOLEAN_FALSE_CONSTANT));
			statements.add(prepareAssignment(parenthesize(ce)));
		} else if (isPrimitiveType(type, new PrimitiveType.Code[] { PrimitiveType.CHAR, PrimitiveType.INT, PrimitiveType.SHORT, PrimitiveType.BYTE })) {
			statements.add(prepareAssignment(provider.getThisAccess(name)));
		} else if (isPrimitiveType(type, PrimitiveType.FLOAT)) {
			// Float.floatToIntBits(aFloat)
			statements.add(prepareAssignment(createFloatInvocation(provider.getThisAccess(name))));
		} else if (isPrimitiveType(type, PrimitiveType.LONG)) {
			statements.add(prepareAssignment(createShiftAssignment(provider.getThisAccess(name), provider.getThisAccess(name))));
		} else if (isPrimitiveType(type, PrimitiveType.DOUBLE)) {

			VariableDeclarationFragment fragment= null;
			if (singleTemp || fDoubleCount == 0) {
				fragment= fAst.newVariableDeclarationFragment();
				fragment.setName(fAst.newSimpleName(VARIABLE_NAME_DOUBLE_TEMPORARY));

				VariableDeclarationStatement st2= fAst.newVariableDeclarationStatement(fragment);
				st2.setType(fAst.newPrimitiveType(PrimitiveType.LONG));
				statements.add(st2);
			}
			fDoubleCount++;

			// Double.doubleToIntBits(aDouble)
			Expression comparison= createDoubleInvocation(provider.getThisAccess(name));

			if (singleTemp)
				fragment.setInitializer(comparison);
			else {
				Assignment ass= fAst.newAssignment();
				ass.setLeftHandSide(fAst.newSimpleName(VARIABLE_NAME_DOUBLE_TEMPORARY));
				ass.setRightHandSide(comparison);
				statements.add(fAst.newExpressionStatement(ass));
			}
			statements.add(prepareAssignment(createShiftAssignment(fAst.newSimpleName(VARIABLE_NAME_DOUBLE_TEMPORARY), fAst.newSimpleName(VARIABLE_NAME_DOUBLE_TEMPORARY))));
		}

		return statements.toArray(new Statement[statements.size()]);
	}

	private Statement createAddArrayHashCode(IVariableBinding binding) {
		MethodInvocation invoc= fAst.newMethodInvocation();
		if (JavaModelUtil.is50OrHigher(fRewrite.getCu().getJavaProject())) {
			if (needsDeepMethod(binding.getType())) {
				invoc.setName(fAst.newSimpleName(METHODNAME_DEEP_HASH_CODE));
			} else {
				invoc.setName(fAst.newSimpleName(METHODNAME_HASH_CODE));
			}
			invoc.setExpression(getQualifiedName(JAVA_UTIL_ARRAYS));
			invoc.arguments().add(getThisAccessForHashCode(binding.getName()));
		} else {
			invoc.setName(fAst.newSimpleName(METHODNAME_HASH_CODE));
			final IJavaElement element= fType.getJavaElement();
			if (element != null && !"".equals(element.getElementName())) //$NON-NLS-1$
				invoc.setExpression(fAst.newSimpleName(element.getElementName()));
			invoc.arguments().add(getThisAccessForHashCode(binding.getName()));
			ITypeBinding type= binding.getType().getElementType();
			if (!Bindings.isVoidType(type)) {
				if (!type.isPrimitive() || binding.getType().getDimensions() >= 2)
					type= fAst.resolveWellKnownType(JAVA_LANG_OBJECT);
				if (!fCustomHashCodeTypes.contains(type))
					fCustomHashCodeTypes.add(type);
			}
		}
		return prepareAssignment(invoc);
	}

	private MethodDeclaration createGetEnclosingInstanceHelper() {
		String enclosingTypeName= fType.getDeclaringClass().getTypeDeclaration().getName();

		MethodDeclaration helperMethod= fAst.newMethodDeclaration();
		helperMethod.modifiers().addAll(ASTNodeFactory.newModifiers(fAst, Modifier.PRIVATE));
		helperMethod.setName(fAst.newSimpleName(METHODNAME_GET_ENCLOSING_INSTANCE));
		helperMethod.setConstructor(false);
		helperMethod.setReturnType2(fAst.newSimpleType(fAst.newSimpleName(enclosingTypeName)));

		Block body= fAst.newBlock();
		helperMethod.setBody(body);

		ThisExpression thisExpression= fAst.newThisExpression();
		thisExpression.setQualifier(fAst.newSimpleName(enclosingTypeName));

		ReturnStatement endReturn= fAst.newReturnStatement();
		endReturn.setExpression(thisExpression);
		body.statements().add(endReturn);

		return helperMethod;
	}


	private MethodDeclaration createHashCodeHelper(ITypeBinding binding) {
		Assert.isTrue(!binding.isArray());

		MethodDeclaration hashCodeMethod= fAst.newMethodDeclaration();
		hashCodeMethod.modifiers().addAll(ASTNodeFactory.newModifiers(fAst, Modifier.PRIVATE | Modifier.STATIC));
		hashCodeMethod.setName(fAst.newSimpleName(METHODNAME_HASH_CODE));
		hashCodeMethod.setConstructor(false);
		hashCodeMethod.setReturnType2(fAst.newPrimitiveType(PrimitiveType.INT));

		// ARGUMENTS
		List<SingleVariableDeclaration> parameters= hashCodeMethod.parameters();
		SingleVariableDeclaration hashCodeParam= fAst.newSingleVariableDeclaration();
		if (!binding.isPrimitive())
			hashCodeParam.setType(fAst.newArrayType(fAst.newSimpleType(getQualifiedName(JAVA_LANG_OBJECT)), 1));
		else
			hashCodeParam.setType(fAst.newArrayType(fAst.newPrimitiveType(PrimitiveType.toCode(binding.getName())), 1));
		hashCodeParam.setName(fAst.newSimpleName(VARIABLE_NAME_HASHCODE_PARAM));
		parameters.add(hashCodeParam);

		Block body= fAst.newBlock();
		hashCodeMethod.setBody(body);

		// PRIME NUMBER
		VariableDeclarationFragment frag= fAst.newVariableDeclarationFragment();
		frag.setName(fAst.newSimpleName(VARIABLE_NAME_PRIME));
		frag.setInitializer(fAst.newNumberLiteral(PRIME_NUMBER));

		VariableDeclarationStatement primeNumberDeclaration= fAst.newVariableDeclarationStatement(frag);
		primeNumberDeclaration.setType(fAst.newPrimitiveType(PrimitiveType.INT));
		body.statements().add(primeNumberDeclaration);

		// IF STATEMENT
		IfStatement ifStatement= fAst.newIfStatement();
		final InfixExpression newInfixExpression= fAst.newInfixExpression();
		newInfixExpression.setLeftOperand(fAst.newSimpleName(VARIABLE_NAME_HASHCODE_PARAM));
		newInfixExpression.setRightOperand(fAst.newNullLiteral());
		newInfixExpression.setOperator(Operator.EQUALS);
		ifStatement.setExpression(newInfixExpression);
		final ReturnStatement returnStatement= fAst.newReturnStatement();
		returnStatement.setExpression(fAst.newNumberLiteral("0")); //$NON-NLS-1$
		ifStatement.setThenStatement(getThenStatement(returnStatement));
		body.statements().add(ifStatement);

		// RESULT
		VariableDeclarationFragment resultFragment= fAst.newVariableDeclarationFragment();
		resultFragment.setName(fAst.newSimpleName(VARIABLE_NAME_RESULT));
		resultFragment.setInitializer(fAst.newNumberLiteral(INITIAL_HASHCODE_VALUE));
		VariableDeclarationStatement resultDeclaration= fAst.newVariableDeclarationStatement(resultFragment);
		resultDeclaration.setType(fAst.newPrimitiveType(PrimitiveType.INT));
		body.statements().add(resultDeclaration);

		// FOR LOOP
		ForStatement forStatement= fAst.newForStatement();

		VariableDeclarationFragment indexDeclaration= fAst.newVariableDeclarationFragment();
		indexDeclaration.setName(fAst.newSimpleName(VARIABLE_NAME_INDEX));
		indexDeclaration.setInitializer(fAst.newNumberLiteral("0")); //$NON-NLS-1$
		final VariableDeclarationExpression declExpression= fAst.newVariableDeclarationExpression(indexDeclaration);
		declExpression.setType(fAst.newPrimitiveType(PrimitiveType.INT));
		forStatement.initializers().add(declExpression);
		InfixExpression infixExpr= fAst.newInfixExpression();
		infixExpr.setLeftOperand(fAst.newSimpleName(VARIABLE_NAME_INDEX));
		FieldAccess access= fAst.newFieldAccess();
		access.setExpression(fAst.newSimpleName(VARIABLE_NAME_HASHCODE_PARAM));
		access.setName(fAst.newSimpleName("length")); //$NON-NLS-1$
		infixExpr.setRightOperand(access);
		infixExpr.setOperator(Operator.LESS);
		forStatement.setExpression(infixExpr);
		PostfixExpression postfixExpr= fAst.newPostfixExpression();
		postfixExpr.setOperand(fAst.newSimpleName(VARIABLE_NAME_INDEX));
		postfixExpr.setOperator(org.eclipse.jdt.core.dom.PostfixExpression.Operator.INCREMENT);
		forStatement.updaters().add(postfixExpr);
		body.statements().add(forStatement);

		Block forBody= fAst.newBlock();
		Statement[] statements= createAddSimpleHashCode(binding, name -> {
			ArrayAccess a= fAst.newArrayAccess();
			a.setArray(fAst.newSimpleName(VARIABLE_NAME_HASHCODE_PARAM));
			a.setIndex(fAst.newSimpleName(name));
			return a;
		}, VARIABLE_NAME_INDEX, true);
		forBody.statements().addAll(Arrays.asList(statements));
		forStatement.setBody(forBody);

		// END RETURN
		ReturnStatement endReturn= fAst.newReturnStatement();
		endReturn.setExpression(fAst.newSimpleName(VARIABLE_NAME_RESULT));
		body.statements().add(endReturn);

		// COMMENT
		if (fSettings != null && fSettings.createComments) {
			Javadoc javadoc= fAst.newJavadoc();
			final TagElement tagComment= fAst.newTagElement();
			TextElement text= fAst.newTextElement();
			text.setText(CodeGenerationMessages.GenerateHashCodeEqualsOperation_hash_code_comment);
			tagComment.fragments().add(text);
			javadoc.tags().add(tagComment);
			final TagElement tagParam= fAst.newTagElement();
			tagParam.setTagName(CodeGenerationMessages.GenerateHashCodeEqualsOperation_tag_param);
			tagParam.fragments().add(fAst.newSimpleName(VARIABLE_NAME_HASHCODE_PARAM));
			text= fAst.newTextElement();
			text.setText(CodeGenerationMessages.GenerateHashCodeEqualsOperation_hash_code_argument);
			tagParam.fragments().add(text);
			javadoc.tags().add(tagParam);
			final TagElement tagReturn= fAst.newTagElement();
			tagReturn.setTagName(CodeGenerationMessages.GenerateHashCodeEqualsOperation_tag_return);
			text= fAst.newTextElement();
			text.setText(CodeGenerationMessages.GenerateHashCodeEqualsOperation_return_comment);
			tagReturn.fragments().add(text);
			javadoc.tags().add(tagReturn);
			hashCodeMethod.setJavadoc(javadoc);
		}
		return hashCodeMethod;
	}

	private Statement createAddQualifiedHashCode(IVariableBinding binding) {

		MethodInvocation invoc= fAst.newMethodInvocation();
		invoc.setExpression(getThisAccessForHashCode(binding.getName()));
		invoc.setName(fAst.newSimpleName(METHODNAME_HASH_CODE));

		InfixExpression expr= fAst.newInfixExpression();
		expr.setOperator(Operator.EQUALS);
		expr.setLeftOperand(getThisAccessForHashCode(binding.getName()));
		expr.setRightOperand(fAst.newNullLiteral());

		ConditionalExpression cexpr= fAst.newConditionalExpression();
		cexpr.setThenExpression(fAst.newNumberLiteral("0")); //$NON-NLS-1$
		cexpr.setElseExpression(invoc);
		cexpr.setExpression(parenthesize(expr));

		return prepareAssignment(parenthesize(cexpr));
	}

	private Expression createShiftAssignment(Expression shift1, Expression shift2) {
		// (int)(element ^ (element >>> 32));
		// see implementation in Arrays.hashCode(), Double.hashCode() and
		// Long.hashCode()
		CastExpression ce= fAst.newCastExpression();
		ce.setType(fAst.newPrimitiveType(PrimitiveType.INT));

		InfixExpression unsignedShiftRight= fAst.newInfixExpression();
		unsignedShiftRight.setLeftOperand(shift1);
		unsignedShiftRight.setRightOperand(fAst.newNumberLiteral("32")); //$NON-NLS-1$
		unsignedShiftRight.setOperator(Operator.RIGHT_SHIFT_UNSIGNED);

		InfixExpression xor= fAst.newInfixExpression();
		xor.setLeftOperand(shift2);
		xor.setRightOperand(parenthesize(unsignedShiftRight));
		xor.setOperator(InfixExpression.Operator.XOR);

		ce.setExpression(parenthesize(xor));
		return ce;
	}

	private Statement prepareAssignment(Expression rightHand) {
		// result = PRIME*result + (...)
		InfixExpression mul= fAst.newInfixExpression();
		mul.setLeftOperand(fAst.newSimpleName(VARIABLE_NAME_PRIME));
		mul.setRightOperand(fAst.newSimpleName(VARIABLE_NAME_RESULT));
		mul.setOperator(Operator.TIMES);

		Assignment ass= fAst.newAssignment();
		ass.setLeftHandSide(fAst.newSimpleName(VARIABLE_NAME_RESULT));

		InfixExpression plus= fAst.newInfixExpression();
		plus.setLeftOperand(mul);
		plus.setOperator(Operator.PLUS);
		plus.setRightOperand(rightHand);

		ass.setRightHandSide(plus);

		return fAst.newExpressionStatement(ass);
	}

	// *************** EQUALS ***************

	private MethodDeclaration createEqualsMethod() throws CoreException {

		MethodDeclaration equalsMethodDeclaration= fAst.newMethodDeclaration();
		equalsMethodDeclaration.modifiers().addAll(ASTNodeFactory.newModifiers(fAst, Modifier.PUBLIC));
		equalsMethodDeclaration.setName(fAst.newSimpleName(METHODNAME_EQUALS));
		equalsMethodDeclaration.setConstructor(false);
		equalsMethodDeclaration.setReturnType2(fAst.newPrimitiveType(PrimitiveType.BOOLEAN));

		List<SingleVariableDeclaration> parameters= equalsMethodDeclaration.parameters();
		SingleVariableDeclaration equalsParam= fAst.newSingleVariableDeclaration();
		equalsParam.setType(fRewrite.getImportRewrite().addImport(fAst.resolveWellKnownType(JAVA_LANG_OBJECT), fAst, fImportRewriteContext, TypeLocation.PARAMETER));
		equalsParam.setName(fAst.newSimpleName(VARIABLE_NAME_EQUALS_PARAM));
		parameters.add(equalsParam);

		Block body= fAst.newBlock();
		equalsMethodDeclaration.setBody(body);

		// if (this == obj) return true;
		body.statements().add(
				createReturningIfStatement(fAst.newThisExpression(), fAst.newSimpleName(VARIABLE_NAME_EQUALS_PARAM), Operator.EQUALS, true));

		if (needsNoSuperCall(fType, METHODNAME_EQUALS, new ITypeBinding[] {fAst.resolveWellKnownType(JAVA_LANG_OBJECT)})) {
			if (!fUseInstanceOf) {
				// if (obj == null) return false;
				body.statements().add(
						createReturningIfStatement(fAst.newSimpleName(VARIABLE_NAME_EQUALS_PARAM), fAst.newNullLiteral(), Operator.EQUALS, false));
			}
		} else {
			// if (!super.equals(obj)) return false;
			SuperMethodInvocation superEqualsCall= fAst.newSuperMethodInvocation();
			superEqualsCall.setName(fAst.newSimpleName(METHODNAME_EQUALS));
			superEqualsCall.arguments().add(fAst.newSimpleName(VARIABLE_NAME_EQUALS_PARAM));

			PrefixExpression pe= fAst.newPrefixExpression();
			pe.setOperator(PrefixExpression.Operator.NOT);
			pe.setOperand(superEqualsCall);

			IfStatement superEqualsIf= fAst.newIfStatement();
			superEqualsIf.setExpression(pe);
			superEqualsIf.setThenStatement(getThenStatement(getReturnFalse()));

			body.statements().add(superEqualsIf);
		}

		if (fUseInstanceOf) {
			// if (!(obj instanceof Type)) return false;
			InstanceofExpression expression= fAst.newInstanceofExpression();
			expression.setLeftOperand(fAst.newSimpleName(VARIABLE_NAME_EQUALS_PARAM));
			expression.setRightOperand(fRewrite.getImportRewrite().addImport(fType, fAst, fImportRewriteContext, TypeLocation.INSTANCEOF));

			PrefixExpression notExpression= fAst.newPrefixExpression();
			notExpression.setOperator(org.eclipse.jdt.core.dom.PrefixExpression.Operator.NOT);

			ParenthesizedExpression parenthesizedExpression= fAst.newParenthesizedExpression();
			parenthesizedExpression.setExpression(expression);

			notExpression.setOperand(parenthesizedExpression);

			body.statements().add(createReturningIfStatement(false, notExpression));
		} else {
			// if (getClass() != obj.getClass()) return false;
			MethodInvocation thisClass= fAst.newMethodInvocation();
			thisClass.setName(fAst.newSimpleName(METHODNAME_GETCLASS));

			MethodInvocation objGetClass= fAst.newMethodInvocation();
			objGetClass.setExpression(fAst.newSimpleName(VARIABLE_NAME_EQUALS_PARAM));
			objGetClass.setName(fAst.newSimpleName(METHODNAME_GETCLASS));

			body.statements().add(createReturningIfStatement(thisClass, objGetClass, Operator.NOT_EQUALS, false));
		}

		boolean memberType= isMemberType();
		if (memberType || fFields.length > 0) {
			// Type other= (Type) obj;
			VariableDeclarationFragment sd= fAst.newVariableDeclarationFragment();
			sd.setName(fAst.newSimpleName(VARIABLE_NAME_EQUALS_CASTED));

			CastExpression cast= fAst.newCastExpression();
			cast.setType(fAst.newSimpleType(fAst.newSimpleName(fType.getName())));
			cast.setExpression(fAst.newSimpleName(VARIABLE_NAME_EQUALS_PARAM));
			sd.setInitializer(cast);

			VariableDeclarationStatement otherDeclaration= fAst.newVariableDeclarationStatement(sd);
			otherDeclaration.setType(fAst.newSimpleType(fAst.newSimpleName(fType.getName())));

			body.statements().add(otherDeclaration);
		}

		if (memberType) {
			body.statements().add(createEnclosingInstanceComparison());
		}

		if (fUseJ7HashEquals && fFields.length > 0) {
			body.statements().add(createJ7EqualsStatement());
		} else {
			for (IVariableBinding field : fFields) {
				ITypeBinding type= field.getType();
				if (type.isPrimitive() || type.isEnum())
					body.statements().add(createSimpleComparison(field));
				else if (type.isArray()) {
					IJavaProject project= fUnit.getJavaElement().getJavaProject();
					if (needsDeepMethod(type) && JavaModelUtil.is50OrHigher(project)) {
						body.statements().add(createMultiArrayComparison(field.getName()));
					} else {
						body.statements().add(createArrayComparison(field.getName()));
					}
				} else
					body.statements().add(createQualifiedComparison(field.getName()));
			}

			// the last return true:
			ReturnStatement endReturn= fAst.newReturnStatement();
			endReturn.setExpression(fAst.newBooleanLiteral(true));

			body.statements().add(endReturn);
		}

		// method comment
		if (fSettings != null) {
			ITypeBinding object= fAst.resolveWellKnownType(JAVA_LANG_OBJECT);
			IMethodBinding objectMethod= null;
			for (IMethodBinding objm : object.getDeclaredMethods()) {
				if (METHODNAME_EQUALS.equals(objm.getName()) && objm.getParameterTypes().length == 1 && JAVA_LANG_OBJECT.equals(objm.getParameterTypes()[0].getQualifiedName())) {
					objectMethod= objm;
				}
			}
			createMethodComment(equalsMethodDeclaration, objectMethod);
		}

		return equalsMethodDeclaration;
	}

	private Statement createJ7EqualsStatement() {
		// return Object.equals(anObjet, other.anObject) && ... ;
		ReturnStatement returnStatement= fAst.newReturnStatement();

		Expression equalsExp= createJ7EqualsExpression(fFields[0]);
		if (fFields.length == 1) {
			returnStatement.setExpression(equalsExp);
		} else {
			InfixExpression exp= fAst.newInfixExpression();
			exp.setLeftOperand(equalsExp);
			InfixExpression refExp= exp;
			for (int i= 1; i < fFields.length; i++) {
				equalsExp= createJ7EqualsExpression(fFields[i]);

				refExp.setOperator(Operator.CONDITIONAL_AND);
				if (i == fFields.length - 1) {
					refExp.setRightOperand(equalsExp);
				} else {
					InfixExpression infixExp= fAst.newInfixExpression();
					infixExp.setLeftOperand(equalsExp);
					refExp.setRightOperand(infixExp);
					refExp= infixExp;
				}
			}
			returnStatement.setExpression(exp);
		}
		return returnStatement;
	}

	private Expression createJ7EqualsExpression(IVariableBinding variable) {
		ITypeBinding type= variable.getType();
		String name= variable.getName();
		if (type.isPrimitive() || type.isEnum()) {
			InfixExpression expression= fAst.newInfixExpression();
			expression.setOperator(Operator.EQUALS);
			if (isPrimitiveType(type, PrimitiveType.FLOAT)) {
				// Float.floatToIntBits(aFloat) == Float.floatToIntBits(other.aFloat)
				expression.setLeftOperand(createFloatInvocation(getThisAccessForEquals(name)));
				expression.setRightOperand(createFloatInvocation(getOtherAccess(name)));
			} else if (isPrimitiveType(type, PrimitiveType.DOUBLE)) {
				// Double.doubleToLongBits(aDouble) == Double.doubleToLongBits(other.aDouble)
				expression.setLeftOperand(createDoubleInvocation(getThisAccessForEquals(name)));
				expression.setRightOperand(createDoubleInvocation(getOtherAccess(name)));
			} else {
				// anInt == other.anInt
				expression.setLeftOperand(getThisAccessForEquals(name));
				expression.setRightOperand(getOtherAccess(name));
			}
			return expression;
		} else {
			MethodInvocation invoc= fAst.newMethodInvocation();
			if (type.isArray()) {
				// Arrays.equals(anArray, other.anArray) or Arrays.deepEquals(anArray, other.anArray)
				invoc.setExpression(getQualifiedName(JAVA_UTIL_ARRAYS));
				invoc.setName(needsDeepMethod(type) ? fAst.newSimpleName(METHODNAME_DEEP_EQUALS) : fAst.newSimpleName(METHODNAME_EQUALS));
			} else {
				// Objects.equals(anObj, other.anObj)
				invoc.setExpression(getQualifiedName(JAVA_UTIL_OBJECTS));
				invoc.setName(fAst.newSimpleName(METHODNAME_EQUALS));
			}
			invoc.arguments().add(getThisAccessForEquals(name));
			invoc.arguments().add(getOtherAccess(name));
			return invoc;
		}
	}

	private Statement createEnclosingInstanceComparison() {
		MethodInvocation enclosing1= fAst.newMethodInvocation();
		enclosing1.setName(fAst.newSimpleName(METHODNAME_GET_ENCLOSING_INSTANCE));

		MethodInvocation enclosing2= fAst.newMethodInvocation();
		enclosing2.setName(fAst.newSimpleName(METHODNAME_GET_ENCLOSING_INSTANCE));
		enclosing2.setExpression(fAst.newSimpleName(VARIABLE_NAME_EQUALS_CASTED));

		MethodInvocation enclosingEql= fAst.newMethodInvocation();
		enclosingEql.setName(fAst.newSimpleName(METHODNAME_EQUALS));
		enclosingEql.setExpression(enclosing1);
		enclosingEql.arguments().add(enclosing2);

		PrefixExpression not= fAst.newPrefixExpression();
		not.setOperand(enclosingEql);
		not.setOperator(PrefixExpression.Operator.NOT);

		IfStatement notEqNull= fAst.newIfStatement();
		notEqNull.setExpression(not);
		notEqNull.setThenStatement(getThenStatement(getReturnFalse()));
		return notEqNull;
	}

	private Statement createSimpleComparison(IVariableBinding binding) {
		if (isPrimitiveType(binding.getType(), PrimitiveType.FLOAT)) {
			return createReturningIfStatement(createFloatInvocation(getThisAccessForEquals(binding.getName())), createFloatInvocation(getOtherAccess(binding
					.getName())), Operator.NOT_EQUALS, false);
		} else if (isPrimitiveType(binding.getType(), PrimitiveType.DOUBLE)) {
			return createReturningIfStatement(createDoubleInvocation(getThisAccessForEquals(binding.getName())), createDoubleInvocation(getOtherAccess(binding
					.getName())), Operator.NOT_EQUALS, false);
		} else
			return createReturningIfStatement(getThisAccessForEquals(binding.getName()), getOtherAccess(binding.getName()), Operator.NOT_EQUALS, false);
	}

	private Statement createArrayComparison(String name) {
		MethodInvocation invoc= fAst.newMethodInvocation();
		invoc.setName(fAst.newSimpleName(METHODNAME_EQUALS));
		invoc.setExpression(getQualifiedName(JAVA_UTIL_ARRAYS));
		invoc.arguments().add(getThisAccessForEquals(name));
		invoc.arguments().add(getOtherAccess(name));

		PrefixExpression pe= fAst.newPrefixExpression();
		pe.setOperator(PrefixExpression.Operator.NOT);
		pe.setOperand(invoc);

		IfStatement ifSt= fAst.newIfStatement();
		ifSt.setExpression(pe);
		ifSt.setThenStatement(getThenStatement(getReturnFalse()));

		return ifSt;
	}

	private Statement createMultiArrayComparison(String name) {
		MethodInvocation invoc= fAst.newMethodInvocation();
		invoc.setName(fAst.newSimpleName(METHODNAME_DEEP_EQUALS));
		invoc.setExpression(getQualifiedName(JAVA_UTIL_ARRAYS));
		invoc.arguments().add(getThisAccessForEquals(name));
		invoc.arguments().add(getOtherAccess(name));

		PrefixExpression pe= fAst.newPrefixExpression();
		pe.setOperator(PrefixExpression.Operator.NOT);
		pe.setOperand(invoc);

		IfStatement ifSt= fAst.newIfStatement();
		ifSt.setExpression(pe);
		ifSt.setThenStatement(getThenStatement(getReturnFalse()));

		return ifSt;
	}

	/**
	 * Creates a comparison of reference types.
	 *
	 * <pre>
	 * if (this.a == null) {
	 * 	if (other.a != null)
	 * 		return false;
	 * } else {
	 * 	if (!this.a.equals(other.a))
	 * 		return false;
	 * }
	 * </pre>
	 * @param name the field name
	 * @return the comparison statement
	 */
	private Statement createQualifiedComparison(String name) {
		InfixExpression newCondition= fAst.newInfixExpression();
		newCondition.setOperator(Operator.EQUALS);
		newCondition.setLeftOperand(getThisAccessForEquals(name));
		newCondition.setRightOperand(fAst.newNullLiteral());

		// THEN
		InfixExpression notEqNull= fAst.newInfixExpression();
		notEqNull.setOperator(Operator.NOT_EQUALS);
		notEqNull.setLeftOperand(getOtherAccess(name));
		notEqNull.setRightOperand(fAst.newNullLiteral());

		IfStatement thenPart= fAst.newIfStatement();
		thenPart.setExpression(notEqNull);
		thenPart.setThenStatement(getThenStatement(getReturnFalse()));

		Block thenPart2= fAst.newBlock();
		thenPart2.statements().add(thenPart);

		// ELSE
		MethodInvocation invoc= fAst.newMethodInvocation();
		invoc.setName(fAst.newSimpleName(METHODNAME_EQUALS));
		invoc.setExpression(getThisAccessForEquals(name));
		invoc.arguments().add(getOtherAccess(name));

		PrefixExpression pe= fAst.newPrefixExpression();
		pe.setOperator(PrefixExpression.Operator.NOT);
		pe.setOperand(invoc);

		IfStatement elsePart= fAst.newIfStatement();
		elsePart.setExpression(pe);
		elsePart.setThenStatement(getThenStatement(getReturnFalse()));

		// ALL
		IfStatement isNull= fAst.newIfStatement();
		isNull.setExpression(newCondition);
		isNull.setThenStatement(thenPart2);
		isNull.setElseStatement(elsePart);

		return isNull;
	}

	// ************************ HELPERS **************************

	private Statement createReturningIfStatement(Expression left, Expression right, Operator operator, boolean whatToReturn) {
		InfixExpression newCondition= fAst.newInfixExpression();
		newCondition.setOperator(operator);
		newCondition.setLeftOperand(left);
		newCondition.setRightOperand(right);
		return createReturningIfStatement(whatToReturn, newCondition);
	}

	private Statement createReturningIfStatement(boolean result, Expression condition) {
		IfStatement firstIf= fAst.newIfStatement();
		firstIf.setExpression(condition);

		ReturnStatement returner= fAst.newReturnStatement();
		returner.setExpression(fAst.newBooleanLiteral(result));
		firstIf.setThenStatement(getThenStatement(returner));
		return firstIf;
	}

	private void createMethodComment(MethodDeclaration newDeclaration, IMethodBinding copyFrom) throws CoreException {
		if (fSettings.createComments) {
			String string= CodeGeneration.getMethodComment(fRewrite.getCu(), fType.getQualifiedName(), newDeclaration, copyFrom, StubUtility.getLineDelimiterUsed(fRewrite.getCu()));
			if (string != null) {
				Javadoc javadoc= (Javadoc) fRewrite.getASTRewrite().createStringPlaceholder(string, ASTNode.JAVADOC);
				newDeclaration.setJavadoc(javadoc);
			}
		}
		IJavaProject project= fUnit.getJavaElement().getJavaProject();
		StubUtility2Core.addOverrideAnnotation(fSettings, project, fRewrite.getASTRewrite(), fRewrite.getImportRewrite(), newDeclaration, copyFrom.getDeclaringClass().isInterface(), null);
	}

	private boolean needsNoSuperCall(ITypeBinding typeBinding, String name, ITypeBinding[] parameters) {
		Assert.isNotNull(typeBinding);
		IMethodBinding binding= Bindings.findMethodInHierarchy(typeBinding.getSuperclass(), name, parameters);
		if (binding != null && !Modifier.isAbstract(binding.getModifiers())) {
			ITypeBinding declaring= binding.getDeclaringClass();
			return JAVA_LANG_OBJECT.equals(declaring.getQualifiedName());
		}
		return true;
	}


	private Expression getThisAccessForEquals(String name) {
		return getThisAccess(name, false);
	}

	private Expression getThisAccessForHashCode(String name) {
		return getThisAccess(name, true);
	}

	private Expression getThisAccess(String name, boolean forHashCode) {
		if (fSettings.useKeywordThis || needsThisQualification(name, forHashCode)) {
			FieldAccess fa= fAst.newFieldAccess();
			fa.setExpression(fAst.newThisExpression());
			fa.setName(fAst.newSimpleName(name));
			return fa;
		}
		return fAst.newSimpleName(name);
	}

	private Expression getOtherAccess(String name) {
		return fAst.newQualifiedName(fAst.newSimpleName(VARIABLE_NAME_EQUALS_CASTED), fAst.newSimpleName(name));
	}

	private boolean isPrimitiveType(ITypeBinding binding, PrimitiveType.Code code) {
		return (binding.getName().equals(code.toString()));
	}

	private boolean isPrimitiveType(ITypeBinding type, PrimitiveType.Code[] codes) {
		for (Code code : codes) {
			if (isPrimitiveType(type, code))
				return true;
		}
		return false;
	}

	private Name getQualifiedName(String name) {
		String importedType= fRewrite.getImportRewrite().addImport(name, fImportRewriteContext);
		return ASTNodeFactory.newName(fAst, importedType);
	}

	private ReturnStatement getReturnFalse() {
		ReturnStatement falseReturn= fAst.newReturnStatement();
		falseReturn.setExpression(fAst.newBooleanLiteral(false));
		return falseReturn;
	}

	private Statement getThenStatement(Statement statement) {
		if (fUseBlocksForThen && !(statement instanceof Block)) {
			Block block= fAst.newBlock();
			block.statements().add(statement);
			return block;
		}
		return statement;
	}


	private Expression parenthesize(Expression expression) {
		ParenthesizedExpression pe= fAst.newParenthesizedExpression();
		pe.setExpression(expression);
		return pe;
	}

	private Expression createFloatInvocation(Expression access) {
		return createMethodInvocation(access, "java.lang.Float", "floatToIntBits"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private Expression createDoubleInvocation(Expression access) {
		return createMethodInvocation(access, "java.lang.Double", "doubleToLongBits"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private Expression createMethodInvocation(Expression access, String qualifiedClassName, String methodName) {
		MethodInvocation invoc= fAst.newMethodInvocation();
		invoc.setExpression(getQualifiedName(qualifiedClassName));
		invoc.setName(fAst.newSimpleName(methodName));
		invoc.arguments().add(access);
		return invoc;
	}

	private boolean needsThisQualification(String name, boolean isHashCode) {
		if (isHashCode)
			return ( (fDoubleCount > 0 && VARIABLE_NAME_DOUBLE_TEMPORARY.equals(name)) || (VARIABLE_NAME_PRIME.equals(name)) || (VARIABLE_NAME_RESULT
					.equals(name)));
		return ( (VARIABLE_NAME_EQUALS_CASTED.equals(name)) || (VARIABLE_NAME_EQUALS_PARAM.equals(name)));
	}

	private boolean needsDeepMethod(ITypeBinding type) {
		String elementTypeName= type.getErasure().getElementType().getName();
		return type.getDimensions() > 1 || TYPE_NAME_CLONEABLE.equals(elementTypeName) || TYPE_NAME_SERIALIZABLE.equals(elementTypeName) || TYPE_NAME_OBJECT.equals(elementTypeName);
	}

}
