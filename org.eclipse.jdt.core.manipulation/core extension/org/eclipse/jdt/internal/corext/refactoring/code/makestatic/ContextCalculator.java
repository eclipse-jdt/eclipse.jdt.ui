/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.refactoring.code.makestatic;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NodeFinder;

import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;

/**
 * This class calculates and provides context for a static method refactoring process. It processes
 * the given input (SelectionFields), which can be an ICompilationUnit or an IMethod, and calculates
 * the corresponding TargetFields, which are used in the actual refactoring process.
 */
public class ContextCalculator {

	/**
	 * Represents the type of selection input. This can be either SelectionInputType.IMETHOD or
	 * SelectionInputType.TEXT_SELECTION.
	 */
	private SelectionInputType fSelectionInputType;

	/**
	 * Represents the text-based selection made by the user in the editor.
	 */
	private Selection fSelectionEditorText;

	/**
	 * Represents the {@code ICompilationUnit} of the original selected element.
	 */
	private ICompilationUnit fSelectionICompilationUnit;

	/**
	 * Represents the parsed {@code CompilationUnit} of the original selected element.
	 */
	private CompilationUnit fSelectionCompilationUnit;

	/**
	 * Represents the AST (Abstract Syntax Tree) node of the original selected element.
	 */
	private ASTNode fSelectionASTNode;

	/**
	 * The {@code IMethodBinding} object representing the binding of the refactored method.
	 */
	private IMethodBinding fTargetIMethodBinding;

	/**
	 * The {@code IMethod} object representing the selected method on which the refactoring should
	 * be performed.
	 */
	private IMethod fTargetIMethod;

	/**
	 * The {@code ICompilationUnit} object representing the {@code ICompilationUnit} of the selected
	 * method where the refactoring should be performed.
	 */
	private ICompilationUnit fTargetICompilationUnit;

	/**
	 * Represents the parsed {@code CompilationUnit} of the selected method where the refactoring
	 * should be performed.
	 */
	private CompilationUnit fTargetCompilationUnit;

	/**
	 * Represents the {@code MethodDeclaration} of the selected method on which the refactoring
	 * should be performed.
	 */
	private MethodDeclaration fTargetMethodDeclaration;

	/**
	 * Enum representing the type of selection input for the refactoring process. It can have one of
	 * the following values:
	 * <ul>
	 * <li>{@code IMETHOD}: Indicates that the selection input is an {@code IMethod} object,
	 * representing a method being selected for refactoring e.g. from the outline menu.</li>
	 * <li>{@code TEXT_SELECTION}: Indicates that the selection input is a textual selection made by
	 * the user in the editor.</li>
	 * </ul>
	 */
	public enum SelectionInputType {
		IMETHOD, TEXT_SELECTION
	}


	/**
	 * Constructs a ContextCalculator using a CompilationUnit and a text Selection. These parameters
	 * serve as SelectionFields, from which the TargetFields will be calculated.
	 *
	 * @param inputAsICompilationUnit the CompilationUnit to be processed.
	 * @param targetSelection the Selection specifying the part of the CompilationUnit to be
	 *            processed.
	 */
	public ContextCalculator(ICompilationUnit inputAsICompilationUnit, Selection targetSelection) {
		this.fSelectionInputType= SelectionInputType.TEXT_SELECTION;
		this.fSelectionEditorText= targetSelection;
		this.fSelectionICompilationUnit= inputAsICompilationUnit;
	}

	/**
	 * Constructs a ContextCalculator using an IMethod. This parameter serves as a SelectionField,
	 * from which the TargetFields will be calculated.
	 *
	 * @param method the IMethod to be processed.
	 */
	public ContextCalculator(IMethod method) {
		this.fSelectionInputType= SelectionInputType.IMETHOD;
		this.fTargetIMethod= method;
	}

	public SelectionInputType getSelectionInputType() {
		return fSelectionInputType;
	}

	public Selection getSelectionEditorText() {
		return fSelectionEditorText;
	}

	public ICompilationUnit getSelectionICompilationUnit() {
		return fSelectionICompilationUnit;
	}

	public ASTNode getOrComputeSelectionASTNode() {
		if (fSelectionASTNode == null) {
			calculateSelectionASTNode();
		}
		return fSelectionASTNode;
	}

	public IMethodBinding getOrComputeTargetIMethodBinding() {
		if (fTargetIMethodBinding == null) {
			calculateTargetIMethodBinding();
		}
		return fTargetIMethodBinding;
	}

	public IMethod getOrComputeTargetIMethod() {
		if (fTargetIMethod == null) {
			calculateTargetIMethod();
		}
		return fTargetIMethod;
	}

	public ICompilationUnit getOrComputeTargetICompilationUnit() {
		if (fTargetICompilationUnit == null) {
			calculateTargetICompilationUnit();
		}
		return fTargetICompilationUnit;
	}

	public CompilationUnit getOrComputeTargetCompilationUnit() {
		if( fTargetCompilationUnit==null) {
			calculateTargetCompilationUnit();
		}
		return fTargetCompilationUnit;
	}

	public MethodDeclaration getOrComputeTargetMethodDeclaration() throws JavaModelException {
		if (fTargetMethodDeclaration == null) {
			calculateMethodDeclaration();
		}
		return fTargetMethodDeclaration;
	}

	/**
	 * This method calculates the selected ASTNode {@link #fSelectionASTNode}. It finds the Node
	 * that is inside the {@link #fSelectionICompilationUnit} CompilationUnit at the
	 * {@link #fSelectionEditorText} Selection.
	 *
	 */
	private void calculateSelectionASTNode() {
		ICompilationUnit selectionICompilationUnit = getSelectionICompilationUnit();
		Selection selectionEditorText= getSelectionEditorText();

		if (selectionICompilationUnit == null || selectionEditorText == null) {
			return;
		}
		fSelectionCompilationUnit= convertICompilationUnitToCompilationUnit(selectionICompilationUnit);
		ASTNode selectionASTNode= NodeFinder.perform(fSelectionCompilationUnit, selectionEditorText.getOffset(), selectionEditorText.getLength());

		if (selectionASTNode == null) {
			return;
		}

		if (selectionASTNode.getNodeType() == ASTNode.SIMPLE_NAME) {
			fSelectionASTNode= selectionASTNode.getParent();
		} else if (selectionASTNode.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
			fSelectionASTNode= ((ExpressionStatement) selectionASTNode).getExpression();
		}
		else {
			fSelectionASTNode= selectionASTNode;
		}
	}

	/**
	 * This method calculates the {@link #fTargetIMethodBinding}. If the {@link #fSelectionASTNode}
	 * is an instance of {@link MethodInvocation} or {@link MethodDeclaration}, it resolves the
	 * corresponding {@link IMethodBinding} from that instance.
	 */
	private void calculateTargetIMethodBinding() {
		ASTNode selectionASTNode = getOrComputeSelectionASTNode();
		if (selectionASTNode == null) {
			return;
		}

		if (selectionASTNode instanceof MethodInvocation selectionMethodInvocation) {
			fTargetIMethodBinding= selectionMethodInvocation.resolveMethodBinding();
		} else if (selectionASTNode instanceof MethodDeclaration selectionMethodDeclaration) {
			fTargetIMethodBinding= selectionMethodDeclaration.resolveBinding();
		}
	}

	/**
	 * Calculates the target IMethod from the target IMethodBinding. This method retrieves the
	 * IMethod represented by the {@link #fTargetIMethodBinding} and assigns it to
	 * {@link #fTargetIMethod}
	 */
	private void calculateTargetIMethod() {
		IMethodBinding targetIMethodBinding = getOrComputeTargetIMethodBinding();
		if (targetIMethodBinding == null) {
			return;
		}
		fTargetIMethod= (IMethod) targetIMethodBinding.getJavaElement();
	}

	/**
	 * Calculates the target ICompilationUnit from the target IMethod. This method retrieves the
	 * declaring type of the {@link #fTargetIMethod}, then gets its associated compilation unit, and
	 * assigns it to {@link #fTargetICompilationUnit}.
	 */
	private void calculateTargetICompilationUnit() {
		IMethod targetIMethod = getOrComputeTargetIMethod();
		if (targetIMethod == null) {
			return;
		}
		fTargetICompilationUnit= targetIMethod.getDeclaringType().getCompilationUnit();
	}

	/**
	 * Converts the target {@link ICompilationUnit} to a {@link CompilationUnit} and assigns it to
	 * {@link #fTargetCompilationUnit}.
	 */
	private void calculateTargetCompilationUnit() {
		ICompilationUnit targetICompilationUnit = getOrComputeTargetICompilationUnit();
		if (targetICompilationUnit == null) {
			return;
		}
		fTargetCompilationUnit= convertICompilationUnitToCompilationUnit(targetICompilationUnit);
	}

	/**
	 * Resolves the method declaration and binding for the target method.
	 *
	 * @throws JavaModelException if the target method or the target compilationUnit is invalid.
	 */
	private void calculateMethodDeclaration() throws JavaModelException {
		IMethod targetIMethod = getOrComputeTargetIMethod();
		CompilationUnit targetCompilationUnit = getOrComputeTargetCompilationUnit();
		if (targetIMethod == null || targetCompilationUnit == null) {
			return;
		}
		fTargetMethodDeclaration= getMethodDeclarationFromIMethod(targetIMethod, targetCompilationUnit);
		fTargetIMethodBinding= fTargetMethodDeclaration.resolveBinding();
	}



	/**
	 * Converts an ICompilationUnit to a CompilationUnit. This method is used in the process of
	 * calculating TargetFields from the SelectionFields.
	 *
	 * @param compilationUnit the ICompilationUnit to convert.
	 * @return the converted CompilationUnit.
	 */
	private static CompilationUnit convertICompilationUnitToCompilationUnit(ICompilationUnit compilationUnit) {
		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(compilationUnit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null);
	}

	private MethodDeclaration getMethodDeclarationFromIMethod(IMethod iMethod, CompilationUnit compilationUnit) throws JavaModelException {
		return ASTNodeSearchUtil.getMethodDeclarationNode(iMethod, compilationUnit);
	}
}
