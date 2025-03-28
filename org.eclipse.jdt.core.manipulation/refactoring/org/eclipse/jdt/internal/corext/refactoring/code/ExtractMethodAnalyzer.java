/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
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
 *     Benjamin Muskalla <bmuskalla@eclipsesource.com> - [extract method] Extract method and continue https://bugs.eclipse.org/bugs/show_bug.cgi?id=48056
 *     Benjamin Muskalla <bmuskalla@eclipsesource.com> - [extract method] Name ambiguous return value in error message - https://bugs.eclipse.org/bugs/show_bug.cgi?id=50607
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.IRegion;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.YieldStatement;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;

import org.eclipse.jdt.internal.core.manipulation.BindingLabelProviderCore;
import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.AbortSearchException;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.LocalVariableIndex;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.TokenScanner;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.FlowContext;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.FlowInfo;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.InOutFlowAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.InputFlowAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.util.CodeAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaStatusContext;
import org.eclipse.jdt.internal.corext.util.Messages;

public class ExtractMethodAnalyzer extends CodeAnalyzer {

	public static final int ERROR=					-2;
	public static final int UNDEFINED=				-1;
	public static final int NO=						0;
	public static final int EXPRESSION=				1;
	public static final int ACCESS_TO_LOCAL=		2;
	public static final int RETURN_STATEMENT_VOID=	3;
	public static final int RETURN_STATEMENT_VALUE=	4;
	public static final int MULTIPLE=				5;

	/** This is either a method declaration or an initializer */
	private BodyDeclaration fEnclosingBodyDeclaration;
	private IMethodBinding fEnclosingMethodBinding;
	private int fMaxVariableId;

	private int fReturnKind;
	private Type fReturnType;
	private ITypeBinding fReturnTypeBinding;

	private FlowInfo fInputFlowInfo;
	private FlowContext fInputFlowContext;

	private IVariableBinding[] fArguments;
	private IVariableBinding[] fMethodLocals;
	private ITypeBinding[] fTypeVariables;

	private IVariableBinding fReturnValue;
	private IVariableBinding[] fCallerLocals;
	private IVariableBinding fReturnLocal;

	private ITypeBinding[] fAllExceptions;
	private ITypeBinding fExpressionBinding;

	private boolean fForceStatic;
	private boolean fIsLastStatementSelected;
	private SimpleName fEnclosingLoopLabel;

	private boolean fSelectionChanged;

	public ExtractMethodAnalyzer(ICompilationUnit unit, Selection selection) throws CoreException {
		super(unit, selection, false);
	}

	public BodyDeclaration getEnclosingBodyDeclaration() {
		return fEnclosingBodyDeclaration;
	}

	public int getReturnKind() {
		return fReturnKind;
	}

	public boolean extractsExpression() {
		return fReturnKind == EXPRESSION;
	}

	public Type getReturnType() {
		return fReturnType;
	}

	public ITypeBinding getReturnTypeBinding() {
		return fReturnTypeBinding;
	}

	public boolean generateImport() {
		switch (fReturnKind) {
			case EXPRESSION:
				return true;
			default:
				return false;
		}
	}

	public IVariableBinding[] getArguments() {
		return fArguments;
	}

	public IVariableBinding[] getMethodLocals() {
		return fMethodLocals;
	}

	public IVariableBinding getReturnValue() {
		return fReturnValue;
	}

	public IVariableBinding[] getCallerLocals() {
		return fCallerLocals;
	}

	public IVariableBinding getReturnLocal() {
		return fReturnLocal;
	}

	public ITypeBinding getExpressionBinding() {
		return fExpressionBinding;
	}

	public boolean getForceStatic() {
		return fForceStatic;
	}

	public ITypeBinding[] getTypeVariables() {
		return fTypeVariables;
	}

	public boolean isSelectionChanged() {
		return fSelectionChanged;
	}

	//---- Activation checking ---------------------------------------------------------------------------

	public boolean isValidDestination(ASTNode node) {
		return !(node instanceof AnnotationTypeDeclaration);
	}

	public RefactoringStatus checkInitialConditions(ImportRewrite rewriter) {
		RefactoringStatus result= getStatus();
		checkExpression(result);
		if (result.hasFatalError())
			return result;

		List<ASTNode> validDestinations= new ArrayList<>();
		ASTNode destination= ASTResolving.findParentType(fEnclosingBodyDeclaration.getParent());
		while (destination != null) {
			if (isValidDestination(destination)) {
				validDestinations.add(destination);
			}
			destination= ASTResolving.findParentType(destination.getParent());
		}
		if (validDestinations.isEmpty()) {
			result.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_no_valid_destination_type);
			return result;
		}

		fReturnKind= UNDEFINED;
		fMaxVariableId= LocalVariableIndex.perform(fEnclosingBodyDeclaration);
		if (analyzeSelection(result).hasFatalError())
			return result;

		int returns= fReturnKind == NO ? 0 : 1;
		if (fReturnValue != null) {
			fReturnKind= ACCESS_TO_LOCAL;
			returns++;
		}
		if (isExpressionSelected()) {
			if (returns == 0 || getFirstSelectedNode().getLocationInParent() != ExpressionStatement.EXPRESSION_PROPERTY) {
				fReturnKind= EXPRESSION;
				returns++;
			} else {
				ASTNode firstParent= getFirstSelectedNode().getParent();
				Selection newSelection= Selection.createFromStartEnd(getSelection().getOffset(),
						firstParent.getStartPosition() + firstParent.getLength());
				setSelection(newSelection);
				reset();
				fSelectionChanged= true;
				return result;
			}
		}
		if (returns > 1) {
			result.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_ambiguous_return_value, JavaStatusContext.create(fCUnit, getSelection()));
			fReturnKind= MULTIPLE;
			return result;
		}

		initReturnType(rewriter);
		return result;
	}

	private void checkExpression(RefactoringStatus status) {
		ASTNode[] nodes= getSelectedNodes();
		if (nodes != null && nodes.length == 1) {
			ASTNode node= nodes[0];
			if (node instanceof Type) {
				status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_type_reference, JavaStatusContext.create(fCUnit, node));
			} else if (node.getLocationInParent() == SwitchCase.EXPRESSION_PROPERTY || node.getLocationInParent() == SwitchCase.EXPRESSIONS2_PROPERTY) {
				status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_switch_case, JavaStatusContext.create(fCUnit, node));
			} else if (node instanceof Annotation || ASTNodes.getParent(node, Annotation.class) != null) {
				status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_from_annotation, JavaStatusContext.create(fCUnit, node));
			}
		}
	}

	private void initReturnType(ImportRewrite rewriter) {
		AST ast= fEnclosingBodyDeclaration.getAST();
		fReturnType= null;
		fReturnTypeBinding= null;
		switch (fReturnKind) {
			case ACCESS_TO_LOCAL:
				VariableDeclaration declaration= ASTNodes.findVariableDeclaration(fReturnValue, fEnclosingBodyDeclaration);
				fReturnType= ASTNodeFactory.newNonVarType(ast, declaration, rewriter, new ContextSensitiveImportRewriteContext(declaration, rewriter));
				if (declaration.resolveBinding() != null) {
					fReturnTypeBinding= declaration.resolveBinding().getType();
				}
				break;
			case EXPRESSION:
				Expression expression= (Expression)getFirstSelectedNode();
				if (expression.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) {
					fExpressionBinding= ((ClassInstanceCreation)expression).getType().resolveBinding();
				} else {
					fExpressionBinding= expression.resolveTypeBinding();
				}
				if (fExpressionBinding != null) {
					if (fExpressionBinding.isNullType()) {
						getStatus().addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_null_type, JavaStatusContext.create(fCUnit, expression));
					} else {
						ITypeBinding normalizedBinding= Bindings.normalizeForDeclarationUse(fExpressionBinding, ast);
						if (normalizedBinding != null) {
							ImportRewriteContext context= new ContextSensitiveImportRewriteContext(fEnclosingBodyDeclaration, rewriter);
							fReturnType= rewriter.addImport(normalizedBinding, ast, context, TypeLocation.RETURN_TYPE);
							fReturnTypeBinding= normalizedBinding;
						}
					}
				} else {
					fReturnType= ast.newPrimitiveType(PrimitiveType.VOID);
					fReturnTypeBinding= ast.resolveWellKnownType("void"); //$NON-NLS-1$
					getStatus().addError(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_determine_return_type, JavaStatusContext.create(fCUnit, expression));
				}
				break;
			case RETURN_STATEMENT_VALUE:
				LambdaExpression enclosingLambdaExpr= ASTResolving.findEnclosingLambdaExpression(getFirstSelectedNode());
				if (enclosingLambdaExpr != null) {
					fReturnType= ASTNodeFactory.newReturnType(enclosingLambdaExpr, ast, rewriter, null);
					IMethodBinding methodBinding= enclosingLambdaExpr.resolveMethodBinding();
					fReturnTypeBinding= methodBinding != null ? methodBinding.getReturnType() : null;
				} else if (fEnclosingBodyDeclaration.getNodeType() == ASTNode.METHOD_DECLARATION) {
					fReturnType= ((MethodDeclaration) fEnclosingBodyDeclaration).getReturnType2();
					fReturnTypeBinding= fReturnType != null ? fReturnType.resolveBinding() : null;
				}
				break;
			default:
				fReturnType= ast.newPrimitiveType(PrimitiveType.VOID);
				fReturnTypeBinding= ast.resolveWellKnownType("void"); //$NON-NLS-1$
		}
		if (fReturnType == null) {
			fReturnType= ast.newPrimitiveType(PrimitiveType.VOID);
			fReturnTypeBinding= ast.resolveWellKnownType("void"); //$NON-NLS-1$
		}
	}

	//	 !!! -- +/- same as in ExtractTempRefactoring
	public boolean isLiteralNodeSelected() {
		ASTNode[] nodes= getSelectedNodes();
		if (nodes.length != 1)
			return false;
		ASTNode node= nodes[0];
		switch (node.getNodeType()) {
			case ASTNode.BOOLEAN_LITERAL :
			case ASTNode.CHARACTER_LITERAL :
			case ASTNode.NULL_LITERAL :
			case ASTNode.NUMBER_LITERAL :
				return true;

			default :
				return false;
		}
	}

	//---- Input checking -----------------------------------------------------------------------------------

	public void checkInput(RefactoringStatus status, String methodName, ASTNode destination) {
		ITypeBinding[] arguments= getArgumentTypes();
		ITypeBinding type= ASTNodes.getEnclosingType(destination);
		status.merge(Checks.checkMethodInType(type, methodName, arguments));
		ITypeBinding superClass= type.getSuperclass();
		if (superClass != null) {
			status.merge(Checks.checkMethodInHierarchy(superClass, methodName, null, arguments));
		}
		for (ITypeBinding superInterface : type.getInterfaces()) {
			status.merge(Checks.checkMethodInHierarchy(superInterface, methodName, null, arguments));
		}
	}

	private ITypeBinding[] getArgumentTypes() {
		ITypeBinding[] result= new ITypeBinding[fArguments.length];
		for (int i= 0; i < fArguments.length; i++) {
			result[i]= fArguments[i].getType();
		}
		return result;
	}

	private RefactoringStatus analyzeSelection(RefactoringStatus status) {
		fInputFlowContext= new FlowContext(0, fMaxVariableId + 1);
		fInputFlowContext.setConsiderAccessMode(true);
		fInputFlowContext.setComputeMode(FlowContext.ARGUMENTS);

		InOutFlowAnalyzer flowAnalyzer= new InOutFlowAnalyzer(fInputFlowContext);
		fInputFlowInfo= flowAnalyzer.perform(getSelectedNodes());

		if (fInputFlowInfo.branches()) {
			String canHandleBranchesProblem= canHandleBranches();
			if (canHandleBranchesProblem != null) {
				status.addFatalError(canHandleBranchesProblem, JavaStatusContext.create(fCUnit, getSelection()));
				fReturnKind= ERROR;
				return status;
			}
		}
		if (fInputFlowInfo.isValueReturn()) {
			fReturnKind= RETURN_STATEMENT_VALUE;
		} else  if (fInputFlowInfo.isVoidReturn() || (fInputFlowInfo.isPartialReturn() && isVoidMethod() && isLastStatementSelected())) {
			if (getSelectedNodes().length == 1 && getSelectedNodes()[0] instanceof ReturnStatement) {
				status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_return, JavaStatusContext.create(fCUnit, getSelection()));
				fReturnKind= ERROR;
				return status;
			}
			fReturnKind= RETURN_STATEMENT_VOID;
		} else if (fInputFlowInfo.isNoReturn() || fInputFlowInfo.isThrow() || fInputFlowInfo.isUndefined()) {
			fReturnKind= NO;
		}

		if (fReturnKind == UNDEFINED) {
			status.addError(RefactoringCoreMessages.FlowAnalyzer_execution_flow, JavaStatusContext.create(fCUnit, getSelection()));
			fReturnKind= NO;
		}
		String checkForFinalFieldsProblem= checkForFinalFields();
		if (checkForFinalFieldsProblem != null) {
			status.addFatalError(checkForFinalFieldsProblem, JavaStatusContext.create(fCUnit, getSelection()));
			fReturnKind= ERROR;
			return status;
		}
		computeInput();
		computeExceptions();
		computeOutput(status);
		if (status.hasFatalError())
			return status;

		adjustArgumentsAndMethodLocals();
		compressArrays();
		return status;
	}

	private String checkForFinalFields() {
		ASTNode[] selectedNodes= getSelectedNodes();
		final String[] problems= new String[] { null };
		for (ASTNode astNode : selectedNodes) {
			try {
				astNode.accept(new ASTVisitor() {
					@Override
					public boolean visit(Assignment node) {
						IBinding binding= null;
						if (node.getLeftHandSide() instanceof FieldAccess fieldAccess) {
							binding= fieldAccess.resolveFieldBinding();
						} else if (node.getLeftHandSide() instanceof SimpleName simpleName) {
							binding= simpleName.resolveBinding();
						}
						if (binding instanceof IVariableBinding varBinding && varBinding.isField() && Modifier.isFinal(varBinding.getModifiers())) {
							problems[0]= RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_final_field_assignment;
							throw new AbortSearchException();
						}
						return false;
					}
				});
			} catch (AbortSearchException e) {
				// do nothing, just a way to exit early
			}
		}
		return problems[0];
	}

	public List<SimpleName> findFieldReferencesForType(AbstractTypeDeclaration type) {
		final List<SimpleName> fieldList= new ArrayList<>();
		ASTNode[] selectedNodes= getSelectedNodes();
		ITypeBinding typeBinding= type.resolveBinding();
		if (typeBinding != null) {
			for (ASTNode astNode : selectedNodes) {
				astNode.accept(new ASTVisitor() {
					@Override
					public boolean visit(SimpleName node) {
						IBinding binding= node.resolveBinding();
						if (binding instanceof IVariableBinding varBinding && varBinding.isField()) {
							if (typeBinding.isEqualTo(varBinding.getDeclaringClass())) {
								fieldList.add(node);
							}
						}
						return true;
					}
				});
			}
		}
		return fieldList;
	}

	private String canHandleBranches() {
		if (fReturnValue != null)
			return RefactoringCoreMessages.ExtractMethodAnalyzer_branch_mismatch;

		ASTNode[] selectedNodes= getSelectedNodes();
		final ASTNode lastSelectedNode= selectedNodes[selectedNodes.length - 1];
		Statement body= getParentLoopBody(lastSelectedNode.getParent());
		if (!(body instanceof Block))
			return RefactoringCoreMessages.ExtractMethodAnalyzer_branch_mismatch;

		if (body != lastSelectedNode) {
			Block block= (Block)body;
			List<Statement> statements= block.statements();
			ASTNode lastStatementInLoop= statements.get(statements.size() - 1);
			if (lastSelectedNode != lastStatementInLoop)
				return RefactoringCoreMessages.ExtractMethodAnalyzer_branch_mismatch;
		}

		final Map<ASTNode, String> pendingProblems= new LinkedHashMap<>();
		for (ASTNode astNode : selectedNodes) {
			astNode.accept(new ASTVisitor() {
				ArrayList<String> fLocalLoopLabels= new ArrayList<>();
				ArrayList<Statement> fBreakTargets= new ArrayList<>();

				@Override
				public boolean visit(BreakStatement node) {
					SimpleName label= node.getLabel();
					if (label != null && !fLocalLoopLabels.contains(label.getIdentifier())) {
						pendingProblems.put(label, Messages.format(
							RefactoringCoreMessages.ExtractMethodAnalyzer_branch_break_mismatch,
							new Object[] { ("break " + label.getIdentifier()) })); //$NON-NLS-1$
					} else if (label == null) {
						ASTNode parentStatement= ASTNodes.getFirstAncestorOrNull(node, WhileStatement.class, ForStatement.class,
								DoStatement.class, SwitchStatement.class, EnhancedForStatement.class);
						if (parentStatement != null && !fBreakTargets.contains(parentStatement)) {
							pendingProblems.put(parentStatement, RefactoringCoreMessages.ExtractMethodAnalyzer_break_parent_missing);
						}
					}
					return false;
				}

				@Override
				public boolean visit(LabeledStatement node) {
					SimpleName label= node.getLabel();
					if (label != null) {
						fLocalLoopLabels.add(label.getIdentifier());
					}
					return true;
				}

				@Override
				public void endVisit(ForStatement node) {
					pendingProblems.remove(node);
					fBreakTargets.add(node);
				}

				@Override
				public void endVisit(EnhancedForStatement node) {
					pendingProblems.remove(node);
					fBreakTargets.add(node);
				}

				@Override
				public void endVisit(DoStatement node) {
					pendingProblems.remove(node);
					fBreakTargets.add(node);
				}

				@Override
				public void endVisit(SwitchStatement node) {
					pendingProblems.remove(node);
					fBreakTargets.add(node);
				}

				@Override
				public void endVisit(WhileStatement node) {
					pendingProblems.remove(node);
					fBreakTargets.add(node);
				}

				@Override
				public void endVisit(ContinueStatement node) {
					SimpleName label= node.getLabel();
					if (label != null && !fLocalLoopLabels.contains(label.getIdentifier())) {
						if (fEnclosingLoopLabel == null || ! label.getIdentifier().equals(fEnclosingLoopLabel.getIdentifier())) {
							pendingProblems.put(node, Messages.format(
								RefactoringCoreMessages.ExtractMethodAnalyzer_branch_continue_mismatch,
								new Object[] { "continue " + label.getIdentifier() })); //$NON-NLS-1$
						}
					}
				}
			});
		}
		if (!pendingProblems.isEmpty()) {
			return pendingProblems.values().iterator().next();
		}
		return null;
	}

	private Statement getParentLoopBody(ASTNode node) {
		Statement stmt= null;
		ASTNode start= node;
		while (start != null
				&& !(start instanceof ForStatement)
				&& !(start instanceof DoStatement)
				&& !(start instanceof WhileStatement)
				&& !(start instanceof EnhancedForStatement)
				&& !(start instanceof SwitchStatement)) {
			start= start.getParent();
		}
		if (start instanceof ForStatement) {
			stmt= ((ForStatement)start).getBody();
		} else if (start instanceof DoStatement) {
			stmt= ((DoStatement)start).getBody();
		} else if (start instanceof WhileStatement) {
			stmt= ((WhileStatement)start).getBody();
		} else if (start instanceof EnhancedForStatement) {
			stmt= ((EnhancedForStatement)start).getBody();
		}
		if (start != null && start.getParent() instanceof LabeledStatement) {
			LabeledStatement labeledStatement= (LabeledStatement)start.getParent();
			fEnclosingLoopLabel= labeledStatement.getLabel();
		}
		return stmt;
	}

	private boolean isVoidMethod() {
		ITypeBinding binding= null;
		LambdaExpression enclosingLambdaExpr= ASTResolving.findEnclosingLambdaExpression(getFirstSelectedNode());
		if (enclosingLambdaExpr != null) {
			IMethodBinding methodBinding= enclosingLambdaExpr.resolveMethodBinding();
			if (methodBinding != null) {
				binding= methodBinding.getReturnType();
			}
		} else {
			// if we have an initializer
			if (fEnclosingMethodBinding == null)
				return true;
			binding= fEnclosingMethodBinding.getReturnType();
		}
		if (fEnclosingBodyDeclaration.getAST().resolveWellKnownType("void").equals(binding)) //$NON-NLS-1$
			return true;
		return false;
	}

	public boolean isLastStatementSelected() {
		return fIsLastStatementSelected;
	}

	private void computeLastStatementSelected() {
		ASTNode[] nodes= getSelectedNodes();
		if (nodes.length == 0) {
			fIsLastStatementSelected= false;
		} else {
			Block body= null;
			LambdaExpression enclosingLambdaExpr= ASTResolving.findEnclosingLambdaExpression(getFirstSelectedNode());
			if (enclosingLambdaExpr != null) {
				ASTNode lambdaBody= enclosingLambdaExpr.getBody();
				if (lambdaBody instanceof Block) {
					body= (Block) lambdaBody;
				} else {
					fIsLastStatementSelected= true;
					return;
				}
			} else {
				if (fEnclosingBodyDeclaration instanceof MethodDeclaration) {
					body= ((MethodDeclaration) fEnclosingBodyDeclaration).getBody();
				} else if (fEnclosingBodyDeclaration instanceof Initializer) {
					body= ((Initializer) fEnclosingBodyDeclaration).getBody();
				}
			}
			if (body != null) {
				List<Statement> statements= body.statements();
				if (statements.size() > 0) {
					fIsLastStatementSelected= nodes[nodes.length - 1] == statements.get(statements.size() - 1);
				} else {
					fIsLastStatementSelected= true;
				}
			}
		}
	}

	private void computeInput() {
		int argumentMode= FlowInfo.READ | FlowInfo.READ_POTENTIAL | FlowInfo.WRITE_POTENTIAL | FlowInfo.UNKNOWN;
		fArguments= removeSelectedDeclarations(fInputFlowInfo.get(fInputFlowContext, argumentMode));
		fMethodLocals= removeSelectedDeclarations(fInputFlowInfo.get(fInputFlowContext, FlowInfo.WRITE | FlowInfo.WRITE_POTENTIAL));
		fTypeVariables= computeTypeVariables(fInputFlowInfo.getTypeVariables());
	}

	private IVariableBinding[] removeSelectedDeclarations(IVariableBinding[] bindings) {
		List<IVariableBinding> result= new ArrayList<>(bindings.length);
		Selection selection= getSelection();
		for (IVariableBinding binding : bindings) {
			ASTNode decl= ((CompilationUnit)fEnclosingBodyDeclaration.getRoot()).findDeclaringNode(binding);
			if (!selection.covers(decl)) {
				result.add(binding);
			}
		}
		return result.toArray(new IVariableBinding[result.size()]);
	}

	private ITypeBinding[] computeTypeVariables(ITypeBinding[] bindings) {
		Selection selection= getSelection();
		Set<ITypeBinding> result= new HashSet<>();
		// first remove all type variables that come from outside of the method
		// or are covered by the selection
		CompilationUnit compilationUnit= (CompilationUnit)fEnclosingBodyDeclaration.getRoot();
		for (ITypeBinding binding : bindings) {
			ASTNode decl= compilationUnit.findDeclaringNode(binding);
			if (decl == null || (!selection.covers(decl) && decl.getParent() instanceof MethodDeclaration)) {
				result.add(binding);
			}
		}
		// all all type variables which are needed since a local variable uses it
		for (IVariableBinding arg : fArguments) {
			ITypeBinding type= arg.getType();
			if (type != null && type.isTypeVariable()) {
				ASTNode decl= compilationUnit.findDeclaringNode(type);
				if (decl == null || (!selection.covers(decl) && decl.getParent() instanceof MethodDeclaration))
					result.add(type);
			}
		}
		return result.toArray(new ITypeBinding[result.size()]);
	}

	private class LocalWriteVisitor extends ASTVisitor {
		final List<IVariableBinding> fRetValues;
		final List<IVariableBinding> fOriginalRetValues;
		final int fMinPosition;
		final int fMaxPosition;
		public LocalWriteVisitor(IRegion selectionRegion, List<IVariableBinding> returnValues) {
			fRetValues= returnValues;
			fOriginalRetValues= new ArrayList<>(returnValues);
			fMinPosition= selectionRegion.getOffset();
			fMaxPosition= fMinPosition + selectionRegion.getLength();
		}

		@Override
		public boolean visit(SimpleName node) {
			if (node.getStartPosition() > fMinPosition && node.getStartPosition() < fMaxPosition) {
				if (node.getParent() instanceof VariableDeclarationFragment) {
					IBinding binding= node.resolveBinding();
					IVariableBinding foundValue= null;
					if (binding instanceof IVariableBinding varBinding && !varBinding.isField()) {
						for (IVariableBinding retValue : fRetValues) {
							if (retValue.isEqualTo(binding)) {
								foundValue= retValue;
								break;
							}
						}
						if (foundValue != null) {
							fRetValues.remove(foundValue);
						}
					}
				}
			} else {
				IBinding binding= node.resolveBinding();
				IVariableBinding foundValue= null;
				if (binding instanceof IVariableBinding varBinding && !varBinding.isField()) {
					for (IVariableBinding origRetValue : fOriginalRetValues) {
						if (origRetValue.isEqualTo(binding)) {
							foundValue= origRetValue;
							break;
						}
					}
					if (foundValue != null && !fRetValues.contains(foundValue)) {
						fRetValues.add(foundValue);
					}
				}
			}
			return super.visit(node);
		}
	}

	private void computeOutput(RefactoringStatus status) {
		// First find all writes inside the selection.
		FlowContext flowContext= new FlowContext(0, fMaxVariableId + 1);
		flowContext.setConsiderAccessMode(true);
		flowContext.setComputeMode(FlowContext.RETURN_VALUES);
		FlowInfo returnInfo= new InOutFlowAnalyzer(flowContext).perform(getSelectedNodes());
		IVariableBinding[] returnValues= returnInfo.get(flowContext, FlowInfo.WRITE | FlowInfo.WRITE_POTENTIAL | FlowInfo.UNKNOWN);

		// Remove all local variables declared in the selected region from potential return values
		List<IVariableBinding> returnValueList= new ArrayList<>(Arrays.asList(returnValues));
		LocalWriteVisitor visitor= new LocalWriteVisitor(getSelectedNodeRange(), returnValueList);
		if (getLastCoveringNode() != null) {
			getLastCoveringNode().accept(visitor);
			returnValues= returnValueList.toArray(new IVariableBinding[0]);
		}
		// Compute a selection that exactly covers the selected nodes
		IRegion region= getSelectedNodeRange();
		Selection selection= Selection.createFromStartLength(region.getOffset(), region.getLength());

		List<IVariableBinding> localReads= new ArrayList<>();
		flowContext.setComputeMode(FlowContext.ARGUMENTS);
		FlowInfo argInfo= new InputFlowAnalyzer(flowContext, selection, true).perform(fEnclosingBodyDeclaration);
		IVariableBinding[] reads= argInfo.get(flowContext, FlowInfo.READ | FlowInfo.READ_POTENTIAL | FlowInfo.UNKNOWN);
		outer: for (int i= 0; i < returnValues.length && localReads.size() < returnValues.length; i++) {
			IVariableBinding binding= returnValues[i];
			for (IVariableBinding read : reads) {
				if (read.isEqualTo(binding)) {
					localReads.add(binding);
					fReturnValue= binding;
					continue outer;
				}
			}
		}
		switch (localReads.size()) {
			case 0:
				fReturnValue= null;
				break;
			case 1:
				break;
			default:
				fReturnValue= null;
				StringBuilder affectedLocals= new StringBuilder();
				for (int i= 0; i < localReads.size(); i++) {
					IVariableBinding binding= localReads.get(i);
					String bindingName= BindingLabelProviderCore.getBindingLabel(binding, BindingLabelProviderCore.DEFAULT_TEXTFLAGS | JavaElementLabelsCore.F_PRE_TYPE_SIGNATURE);
					affectedLocals.append(bindingName);
					if (i != localReads.size() - 1) {
						affectedLocals.append('\n');
					}
				}
				String message= MessageFormat.format(RefactoringCoreMessages.ExtractMethodAnalyzer_assignments_to_local, affectedLocals.toString());
				status.addFatalError(message, JavaStatusContext.create(fCUnit, getSelection()));
				return;
		}
		List<IVariableBinding> callerLocals= new ArrayList<>(5);
		FlowInfo localInfo= new InputFlowAnalyzer(flowContext, selection, false).perform(fEnclosingBodyDeclaration);
		for (IVariableBinding write : localInfo.get(flowContext, FlowInfo.WRITE | FlowInfo.WRITE_POTENTIAL | FlowInfo.UNKNOWN)) {
			if (getSelection().covers(ASTNodes.findDeclaration(write, fEnclosingBodyDeclaration)))
				callerLocals.add(write);
		}
		fCallerLocals= callerLocals.toArray(new IVariableBinding[callerLocals.size()]);
		if (fReturnValue != null && getSelection().covers(ASTNodes.findDeclaration(fReturnValue, fEnclosingBodyDeclaration)))
			fReturnLocal= fReturnValue;
	}

	private void adjustArgumentsAndMethodLocals() {
		for (int i= 0; i < fArguments.length; i++) {
			IVariableBinding argument= fArguments[i];
			// Both arguments and locals consider FlowInfo.WRITE_POTENTIAL. But at the end a variable
			// can either be a local of an argument. Fix this based on the compute return type which
			// didn't exist when we computed the locals and arguments (see computeInput())
			if (fInputFlowInfo.hasAccessMode(fInputFlowContext, argument, FlowInfo.WRITE_POTENTIAL)) {
				if (argument != fReturnValue)
					fArguments[i]= null;
				// We didn't remove the argument. So we have to remove the local declaration
				if (fArguments[i] != null) {
					for (int l= 0; l < fMethodLocals.length; l++) {
						if (fMethodLocals[l] == argument)
							fMethodLocals[l]= null;
					}
				}
			}
		}
	}

	private void compressArrays() {
		fArguments= compressArray(fArguments);
		fCallerLocals= compressArray(fCallerLocals);
		fMethodLocals= compressArray(fMethodLocals);
	}

	private IVariableBinding[] compressArray(IVariableBinding[] array) {
		if (array == null)
			return null;
		int size= 0;
		for (IVariableBinding binding : array) {
			if (binding != null) {
				size++;
			}
		}
		if (size == array.length)
			return array;
		IVariableBinding[] result= new IVariableBinding[size];
		for (int i= 0, r= 0; i < array.length; i++) {
			if (array[i] != null)
				result[r++]= array[i];
		}
		return result;
	}

	//---- Change creation ----------------------------------------------------------------------------------

	public void aboutToCreateChange() {
	}

	//---- Exceptions -----------------------------------------------------------------------------------------

	public ITypeBinding[] getExceptions(boolean includeRuntimeExceptions) {
		if (includeRuntimeExceptions)
			return fAllExceptions;
		List<ITypeBinding> result= new ArrayList<>(fAllExceptions.length);
		for (ITypeBinding exception : fAllExceptions) {
			if (!includeRuntimeExceptions && Bindings.isRuntimeException(exception))
				continue;
			result.add(exception);
		}
		return result.toArray(new ITypeBinding[result.size()]);
	}

	private void computeExceptions() {
		fAllExceptions= ExceptionAnalyzer.perform(getSelectedNodes());
	}

	//---- Special visitor methods ---------------------------------------------------------------------------

//	@Override
//	protected void handleFirstSelectedNode(ASTNode node) {
//		if (node.getLocationInParent() == ExpressionStatement.EXPRESSION_PROPERTY) {
//			super.handleFirstSelectedNode(node.getParent());
//		} else {
//			super.handleFirstSelectedNode(node);
//		}
//	}
//
	@Override
	protected void handleNextSelectedNode(ASTNode node) {
//		if (node.getLocationInParent() == ExpressionStatement.EXPRESSION_PROPERTY) {
//			super.handleNextSelectedNode(node.getParent());
//		} else {
//			super.handleNextSelectedNode(node);
//		}
		super.handleNextSelectedNode(node);
		checkParent(node);
	}

	@Override
	protected boolean handleSelectionEndsIn(ASTNode node) {
		invalidSelection(RefactoringCoreMessages.StatementAnalyzer_doesNotCover, JavaStatusContext.create(fCUnit, node));
		return super.handleSelectionEndsIn(node);
	}

	private void checkParent(ASTNode node) {
		ASTNode firstParent= getFirstSelectedNode().getParent();
		do {
			node= node.getParent();
			if (node == firstParent)
				return;
		} while (node != null);
		invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_parent_mismatch);
	}

	@Override
	public void endVisit(CompilationUnit node) {
		RefactoringStatus status= getStatus();
		superCall: {
			if (status.hasFatalError())
				break superCall;
			if (!hasSelectedNodes()) {
				ASTNode coveringNode= getLastCoveringNode();
				if (coveringNode instanceof Block && coveringNode.getParent() instanceof MethodDeclaration) {
					MethodDeclaration methodDecl= (MethodDeclaration)coveringNode.getParent();
					Message[] messages= ASTNodes.getMessages(methodDecl, ASTNodes.NODE_ONLY);
					if (messages.length > 0) {
						status.addFatalError(Messages.format(
							RefactoringCoreMessages.ExtractMethodAnalyzer_compile_errors,
							BasicElementLabels.getJavaElementName(methodDecl.getName().getIdentifier())), JavaStatusContext.create(fCUnit, methodDecl));
						break superCall;
					}
				}
				status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_invalid_selection);
				break superCall;
			}
			fEnclosingBodyDeclaration= ASTNodes.getParent(getFirstSelectedNode(), BodyDeclaration.class);
			if (fEnclosingBodyDeclaration == null ||
					(fEnclosingBodyDeclaration.getNodeType() != ASTNode.METHOD_DECLARATION &&
					 fEnclosingBodyDeclaration.getNodeType() != ASTNode.FIELD_DECLARATION &&
					 fEnclosingBodyDeclaration.getNodeType() != ASTNode.INITIALIZER)) {
				status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_invalid_selection);
				break superCall;
			} else if (ASTNodes.getEnclosingType(fEnclosingBodyDeclaration) == null) {
				status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_compile_errors_no_parent_binding);
				break superCall;
			} else if (fEnclosingBodyDeclaration.getNodeType() == ASTNode.METHOD_DECLARATION) {
				fEnclosingMethodBinding= ((MethodDeclaration)fEnclosingBodyDeclaration).resolveBinding();
			}
			if (!isSingleExpressionOrStatementSet()) {
				status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_single_expression_or_set);
				break superCall;
			}
			if (isExpressionSelected()) {
				ASTNode expression= getFirstSelectedNode();
				if (expression instanceof Name) {
					Name name= (Name)expression;
					if (name.resolveBinding() instanceof ITypeBinding) {
						status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_type_reference);
						break superCall;
					}
					if (name.resolveBinding() instanceof IMethodBinding) {
						status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_method_name_reference);
						break superCall;
					}
					if (name.resolveBinding() instanceof IVariableBinding) {
						StructuralPropertyDescriptor locationInParent= name.getLocationInParent();
						boolean isPartOfQualifiedName= false;
						boolean isPartOfQualifier= false;
						if (locationInParent == QualifiedName.NAME_PROPERTY) {
							isPartOfQualifiedName= true;
							QualifiedName qualifiedName= (QualifiedName) name.getParent();
							QualifiedName currParent= qualifiedName;
							while (true) {
								ASTNode parent= currParent.getParent();
								if (parent instanceof QualifiedName) {
									currParent= (QualifiedName) parent;
								} else {
									break;
								}
							}
							if (!qualifiedName.equals(currParent)) {
								isPartOfQualifier= true;
							}
						}
						if ((isPartOfQualifiedName && !isPartOfQualifier)
								|| (locationInParent == FieldAccess.NAME_PROPERTY && !(((FieldAccess) name.getParent()).getExpression() instanceof ThisExpression))) {
							status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_part_of_qualified_name);
							break superCall;
						}
					}
					if (name.isSimpleName() && ((SimpleName)name).isDeclaration()) {
						status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_name_in_declaration);
						break superCall;
					}
				}
				fForceStatic=
					ASTNodes.getParent(expression, ASTNode.SUPER_CONSTRUCTOR_INVOCATION) != null ||
					ASTNodes.getParent(expression, ASTNode.CONSTRUCTOR_INVOCATION) != null;
			}
			status.merge(LocalTypeAnalyzer.perform(fEnclosingBodyDeclaration, getSelection()));
			computeLastStatementSelected();
		}
		super.endVisit(node);
	}

	@Override
	public boolean visit(AnonymousClassDeclaration node) {
		boolean result= super.visit(node);
		if (isFirstSelectedNode(node)) {
			invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_anonymous_type, JavaStatusContext.create(fCUnit, node));
			return false;
		}
		return result;
	}

	@Override
	public boolean visit(Assignment node) {
		boolean result= super.visit(node);
		Selection selection= getSelection();
		ASTNode selectedNode= NodeFinder.perform(node, selection.getOffset(), selection.getLength());
		if ((selectedNode != null && SnippetFinder.isLeftHandSideOfAssignment(selectedNode)) || (selection.covers(node.getLeftHandSide()) && !selection.covers(node.getRightHandSide()))) {
			invalidSelection(
				RefactoringCoreMessages.ExtractMethodAnalyzer_leftHandSideOfAssignment,
				JavaStatusContext.create(fCUnit, node));
			return false;
		}
		return result;
	}

	@Override
	public boolean visit(DoStatement node) {
		boolean result= super.visit(node);

		try {
			int actionStart= getTokenScanner().getTokenEndOffset(ITerminalSymbols.TokenNamedo, node.getStartPosition());
			if (getSelection().getOffset() == actionStart) {
				invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_after_do_keyword, JavaStatusContext.create(fCUnit, getSelection()));
				return false;
			}
		} catch (CoreException e) {
			// ignore
		}

		return result;
	}

	@Override
	public boolean visit(LambdaExpression node) {
		Selection selection= getSelection();
		int selectionStart= selection.getOffset();
		int selectionExclusiveEnd= selection.getExclusiveEnd();
		int lambdaStart= node.getStartPosition();
		int lambdaExclusiveEnd= lambdaStart + node.getLength();
		ASTNode body= node.getBody();
		int bodyStart= body.getStartPosition();
		int bodyExclusiveEnd= bodyStart + body.getLength();

		boolean isValidSelection= false;
		if ((body instanceof Block) && (bodyStart <= selectionStart && selectionExclusiveEnd <= bodyExclusiveEnd)) {
			// if selection is inside lambda body's block
			isValidSelection= true;
		} else if (body instanceof Expression) {
			try {
				TokenScanner scanner= new TokenScanner(fCUnit);
				int arrowExclusiveEnd= scanner.getTokenEndOffset(ITerminalSymbols.TokenNameARROW, lambdaStart);
				if (selectionStart >= arrowExclusiveEnd) {
					isValidSelection= true;
				}
			} catch (CoreException e) {
				// ignore
			}
		}
		if (selectionStart <= lambdaStart && selectionExclusiveEnd >= lambdaExclusiveEnd) {
			// if selection covers the lambda node
			isValidSelection= true;
		}

		if (!isValidSelection) {
			return false;
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		Block body= node.getBody();
		if (body == null)
			return false;
		Selection selection= getSelection();
		int nodeStart= body.getStartPosition();
		int nodeExclusiveEnd= nodeStart + body.getLength();
		// if selection node inside of the method body ignore method
		if ((nodeStart >= selection.getOffset())
				|| (selection.getExclusiveEnd() >= nodeExclusiveEnd))
			return false;
		return super.visit(node);
	}

	@Override
	public boolean visit(ConstructorInvocation node) {
		return visitConstructorInvocation(node, super.visit(node));
	}

	@Override
	public boolean visit(SuperConstructorInvocation node) {
		return visitConstructorInvocation(node, super.visit(node));
	}

	private boolean visitConstructorInvocation(ASTNode node, boolean superResult) {
		if (getSelection().getVisitSelectionMode(node) == Selection.SELECTED) {
			invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_super_or_this, JavaStatusContext.create(fCUnit, node));
			return false;
		}
		return superResult;
	}

	@Override
	public boolean visit(VariableDeclarationFragment node) {
		boolean result= super.visit(node);
		if (isFirstSelectedNode(node)) {
			if (node.getParent() instanceof FieldDeclaration) {
				invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_variable_declaration_fragment_from_field, JavaStatusContext.create(fCUnit, node));
			} else {
				invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_variable_declaration_fragment, JavaStatusContext.create(fCUnit, node));
			}
			return false;
		}
		return result;
	}

	@Override
	public void endVisit(FieldDeclaration node) {
		if (contains(getSelectedNodes(), node.fragments())) {
			invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_variable_declaration_fragment_from_field, JavaStatusContext.create(fCUnit, node));
		}
		super.endVisit(node);
	}

	@Override
	public void endVisit(ForStatement node) {
		if (getSelection().getEndVisitSelectionMode(node) == Selection.AFTER) {
			if (node.initializers().contains(getFirstSelectedNode())) {
				invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_for_initializer, JavaStatusContext.create(fCUnit, getSelection()));
			} else if (node.updaters().contains(getLastSelectedNode())) {
				invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_for_updater, JavaStatusContext.create(fCUnit, getSelection()));
			}
		}
		super.endVisit(node);
	}

	@Override
	public void endVisit(EnhancedForStatement node) {
		if (getSelection().getEndVisitSelectionMode(node) == Selection.AFTER) {
			if (node.getParameter() == getFirstSelectedNode()) {
				invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_for_initializer, JavaStatusContext.create(fCUnit, getSelection()));
			}
		}
		super.endVisit(node);
	}

	@Override
	public void endVisit(QualifiedName node) {
		if (isResourceInTry(node)) {
			invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_resource_used_in_try_with_resources, JavaStatusContext.create(fCUnit, getSelection()));
		}
		super.endVisit(node);
	}

	@Override
	public void endVisit(BreakStatement node) {
		if (isFirstSelectedNode(node)) {
			invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_break, JavaStatusContext.create(fCUnit, getSelection()));
		}
	}

	@Override
	public void endVisit(ContinueStatement node) {
		if (isFirstSelectedNode(node)) {
			invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_continue, JavaStatusContext.create(fCUnit, getSelection()));
		}
	}

	@Override
	public void endVisit(YieldStatement node) {
		if (isFirstSelectedNode(node)) {
			invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_yield, JavaStatusContext.create(fCUnit, getSelection()));
		}
		super.endVisit(node);
	}

	@Override
	public void endVisit(SimpleName node) {
		if (isResourceInTry(node)) {
			invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_resource_used_in_try_with_resources, JavaStatusContext.create(fCUnit, getSelection()));
		}
		super.endVisit(node);
	}

	@Override
	public void endVisit(VariableDeclarationExpression node) {
		if (isResourceInTry(node)) {
			invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_resource_in_try_with_resources, JavaStatusContext.create(fCUnit, getSelection()));
		}
		checkTypeInDeclaration(node.getType());
		super.endVisit(node);
	}

	@Override
	public void endVisit(VariableDeclarationStatement node) {
		checkTypeInDeclaration(node.getType());
		super.endVisit(node);
	}

	private boolean isFirstSelectedNode(ASTNode node) {
		return getSelection().getVisitSelectionMode(node) == Selection.SELECTED && getFirstSelectedNode() == node;
	}

	private void checkTypeInDeclaration(Type node) {
		if (getSelection().getEndVisitSelectionMode(node) == Selection.SELECTED && getFirstSelectedNode() == node) {
			invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_variable_declaration, JavaStatusContext.create(fCUnit, getSelection()));
		}
	}

	private boolean isSingleExpressionOrStatementSet() {
		ASTNode first= getFirstSelectedNode();
		if (first == null)
			return true;
		if (first instanceof Expression && getSelectedNodes().length != 1)
			return false;
		return true;
	}

	private boolean isResourceInTry(Expression node) {
		return getSelection().getEndVisitSelectionMode(node) == Selection.SELECTED && getFirstSelectedNode() == node && node.getLocationInParent() == TryStatement.RESOURCES2_PROPERTY;
	}



}

