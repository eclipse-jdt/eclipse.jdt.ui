/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial code based on SurroundWithTryCatchRefactoring
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.surround;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodReference;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.CodeScopeBuilder;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModelCore;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;

import org.eclipse.jdt.internal.ui.text.correction.QuickAssistProcessorUtil;

/**
 * Surround a set of statements with a try-with-resources block.
 *
 * Special case:
 *
 * URL url= file.toURL();
 *
 * In this case the variable declaration statement gets convert into a
 * declaration without initializer. So the body of the try/catch block
 * only consists of new assignments. In this case we can't move the
 * selected nodes (e.g. the declaration) into the try block.
 */
public class SurroundWithTryWithResourcesRefactoringCore extends Refactoring {

	public final String GROUP_EXC_TYPE= "exc_type"; //$NON-NLS-1$
	public final String GROUP_EXC_NAME= "exc_name"; //$NON-NLS-1$
	public final String GROUP_TRY_STATEMENT= "try_stmt"; //$NON-NLS-1$

	private Selection fSelection;
	private SurroundWithTryWithResourcesAnalyzer fAnalyzer;
	private boolean fLeaveDirty;

	private ICompilationUnit fCUnit;
	private CompilationUnit fRootNode;
	private ASTRewrite fRewriter;
	private ImportRewrite fImportRewrite;
	private CodeScopeBuilder.Scope fScope;
	private ASTNode[] fSelectedNodes;
	private List<ASTNode> fAutoClosableNodes;

	private LinkedProposalModelCore fLinkedProposalModel;

	protected SurroundWithTryWithResourcesRefactoringCore(ICompilationUnit cu, Selection selection) {
		fCUnit= cu;
		fSelection= selection;
		fLeaveDirty= false;
	}

	public static SurroundWithTryWithResourcesRefactoringCore create(ICompilationUnit cu, int offset, int length) {
		return new SurroundWithTryWithResourcesRefactoringCore(cu, Selection.createFromStartLength(offset, length));
	}

	public static SurroundWithTryWithResourcesRefactoringCore create(ICompilationUnit cu, Selection selection) {
		return new SurroundWithTryWithResourcesRefactoringCore(cu, selection);
	}

	public LinkedProposalModelCore getLinkedProposalModelCore() {
		return fLinkedProposalModel;
	}

	public void setLeaveDirty(boolean leaveDirty) {
		fLeaveDirty= leaveDirty;
	}

	public boolean stopExecution() {
		if (fAnalyzer == null)
			return true;
		ITypeBinding[] exceptions= fAnalyzer.getExceptions(fAnalyzer.getSelection());
		List<ASTNode> autoClosableNodes= fAnalyzer.getCoveredAutoClosableNodes();
		return (exceptions == null || exceptions.length == 0) && (autoClosableNodes == null || autoClosableNodes.isEmpty());
	}

	/* non Java-doc
	 * @see IRefactoring#getName()
	 */
	@Override
	public String getName() {
		return RefactoringCoreMessages.SurroundWithTryWithResourcesRefactoring_name;
	}

	public RefactoringStatus checkActivationBasics(CompilationUnit rootNode) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		fRootNode= rootNode;

		fAnalyzer= new SurroundWithTryWithResourcesAnalyzer(fCUnit, fSelection);
		fRootNode.accept(fAnalyzer);
		result.merge(fAnalyzer.getStatus());
		fAutoClosableNodes= fAnalyzer.getCoveredAutoClosableNodes();
		if (fAutoClosableNodes == null || fAutoClosableNodes.isEmpty()) {
			result.merge(RefactoringStatus.createWarningStatus(RefactoringCoreMessages.SurroundWithTryWithResourcesRefactoring_notAutoclosable));
		}
		return result;
	}


	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		CompilationUnit rootNode= new RefactoringASTParser(IASTSharedValues.SHARED_AST_LEVEL).parse(fCUnit, true, pm);
		return checkActivationBasics(rootNode);
	}

	/*
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		return Checks.validateModifiesFiles(
			ResourceUtil.getFiles(new ICompilationUnit[]{fCUnit}),
			getValidationContext(), pm);
	}

	protected LinkedProposalModelCore createLinkedProposalModel() {
		return new LinkedProposalModelCore();
	}

	/* non Java-doc
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException {
		final String NN= ""; //$NON-NLS-1$
		if (pm == null) pm= new NullProgressMonitor();
		pm.beginTask(NN, 2);
		try {
			final CompilationUnitChange result= new CompilationUnitChange(getName(), fCUnit);
			if (fLeaveDirty)
				result.setSaveMode(TextFileChange.LEAVE_DIRTY);
			MultiTextEdit root= new MultiTextEdit();
			result.setEdit(root);
			fRewriter= ASTRewrite.create(fAnalyzer.getEnclosingBodyDeclaration().getAST());
			fImportRewrite= StubUtility.createImportRewrite(fRootNode, true);

			fLinkedProposalModel= createLinkedProposalModel();

			fScope= CodeScopeBuilder.perform(fAnalyzer.getEnclosingBodyDeclaration(), fSelection).
				findScope(fSelection.getOffset(), fSelection.getLength());
			fScope.setCursor(fSelection.getOffset());

			fSelectedNodes= fAnalyzer.getSelectedNodes();

			createTryWithResourcesStatement(fCUnit.getBuffer(), fCUnit.findRecommendedLineSeparator());

			if (fImportRewrite.hasRecordedChanges()) {
				TextEdit edit= fImportRewrite.rewriteImports(null);
				root.addChild(edit);
				result.addTextEditGroup(new TextEditGroup(NN, new TextEdit[] {edit} ));
			}
			TextEdit change= fRewriter.rewriteAST();
			root.addChild(change);
			result.addTextEditGroup(new TextEditGroup(NN, new TextEdit[] {change} ));
			return result;
		} finally {
			pm.done();
		}
	}

	private AST getAST() {
		return fRootNode.getAST();
	}

	@SuppressWarnings("null")
	private void createTryWithResourcesStatement(org.eclipse.jdt.core.IBuffer buffer, String lineDelimiter) throws CoreException {
		List<Statement> result= new ArrayList<>(1);
		boolean modifyExistingTry= false;
		TryStatement tryStatement= null;
		ITypeBinding[] exceptions= fAnalyzer.getExceptions(fAnalyzer.getSelection());
		ImportRewriteContext context= new ContextSensitiveImportRewriteContext(fAnalyzer.getEnclosingBodyDeclaration(), fImportRewrite);

		TryStatement enclosingTry= (TryStatement)ASTResolving.findAncestor(fSelectedNodes[0], ASTNode.TRY_STATEMENT);
		ListRewrite resourcesRewriter= null;
		ListRewrite clausesRewriter= null;
		ListRewrite statements= null;
		if (enclosingTry == null || enclosingTry.getBody() == null || enclosingTry.getBody().statements().get(0) != fSelectedNodes[0]) {
			tryStatement= getAST().newTryStatement();
			statements= fRewriter.getListRewrite(tryStatement.getBody(), Block.STATEMENTS_PROPERTY);
		} else {
			modifyExistingTry= true;
			resourcesRewriter= fRewriter.getListRewrite(enclosingTry, TryStatement.RESOURCES2_PROPERTY);
			clausesRewriter= fRewriter.getListRewrite(enclosingTry, TryStatement.CATCH_CLAUSES_PROPERTY);
		}

		CatchClause catchClause= getAST().newCatchClause();
		SingleVariableDeclaration decl= getAST().newSingleVariableDeclaration();
		String varName= StubUtility.getExceptionVariableName(fCUnit.getJavaProject());
		String name= fScope.createName(varName, false);
		decl.setName(getAST().newSimpleName(name));


		boolean selectedNodeRemoved= false;
		ASTNode expressionStatement= null;
		ASTNode replacementNode= null;
		List<ASTNode> nodesInRange= new ArrayList<>();

		if (!modifyExistingTry) {
			List<ASTNode> variableDeclarations= getSpecialVariableDeclarationStatements();
			for (ASTNode node : fSelectedNodes) {
				if (fAutoClosableNodes.contains(node)) {
					continue;
				}
				if (node instanceof VariableDeclarationStatement && variableDeclarations.contains(node)) {
					AST ast= getAST();
					VariableDeclarationStatement statement= (VariableDeclarationStatement)node;
					// Create a copy and remove the initializer
					VariableDeclarationStatement copy= (VariableDeclarationStatement)ASTNode.copySubtree(ast, statement);
					List<IExtendedModifier> modifiers= copy.modifiers();
					for (Iterator<IExtendedModifier> iter= modifiers.iterator(); iter.hasNext();) {
						IExtendedModifier modifier= iter.next();
						if (modifier.isModifier() && Modifier.isFinal(((Modifier)modifier).getKeyword().toFlagValue())) {
							iter.remove();
						}
					}
					List<VariableDeclarationFragment> fragments= copy.fragments();
					for (VariableDeclarationFragment fragment : fragments) {
						fragment.setInitializer(null);
					}

					// "var" type cannot have null initializer, so change to inferred type
					if (ASTNodes.isVarType(statement, fRootNode)) {
						ITypeBinding binding= statement.getType().resolveBinding();
						if (binding != null) {
							Type varType= fImportRewrite.addImport(binding, getAST(), context, TypeLocation.LOCAL_VARIABLE);
							copy.setType(varType);
						}
					}

					CompilationUnit root= (CompilationUnit)statement.getRoot();
					int extendedStart= root.getExtendedStartPosition(statement);
					// we have a leading comment and the comment is covered by the selection
					if (extendedStart != statement.getStartPosition() && extendedStart >= fSelection.getOffset()) {
						String commentToken= buffer.getText(extendedStart, statement.getStartPosition() - extendedStart);
						commentToken= Strings.trimTrailingTabsAndSpaces(commentToken);
						Type type= statement.getType();
						String typeName= buffer.getText(type.getStartPosition(), type.getLength());
						copy.setType((Type)fRewriter.createStringPlaceholder(commentToken + typeName, type.getNodeType()));
					}
					result.add(copy);
					// convert the fragments into expression statements
					fragments= statement.fragments();
					if (!fragments.isEmpty()) {
						List<ExpressionStatement> newExpressionStatements= new ArrayList<>();
						for (VariableDeclarationFragment fragment : fragments) {
							Expression initializer= fragment.getInitializer();
							if (initializer != null) {
								Assignment assignment= ast.newAssignment();
								assignment.setLeftHandSide((Expression)fRewriter.createCopyTarget(fragment.getName()));
								assignment.setRightHandSide((Expression)fRewriter.createCopyTarget(initializer));
								newExpressionStatements.add(ast.newExpressionStatement(assignment));
							}
						}
						if (!newExpressionStatements.isEmpty()) {
							if (fSelectedNodes.length == 1) {
								expressionStatement= fRewriter.createGroupNode(newExpressionStatements.toArray(new ASTNode[newExpressionStatements.size()]));
							} else {
								fRewriter.replace(
										statement,
										fRewriter.createGroupNode(newExpressionStatements.toArray(new ASTNode[newExpressionStatements.size()])),
										null);
							}
						} else {
							fRewriter.remove(statement, null);
							selectedNodeRemoved= true;
						}
					} else {
						fRewriter.remove(statement, null);
						selectedNodeRemoved= true;
					}
				}
			}
			result.add(tryStatement);
			if (result.size() == 1) {
				replacementNode= result.get(0);
			} else {
				replacementNode= fRewriter.createGroupNode(result.toArray(new ASTNode[result.size()]));
			}

			ASTNode node= fSelectedNodes[0];
			List<ASTNode> coveredStatements= new ArrayList<>();
			for (ASTNode coveredNode : fSelectedNodes) {
				Statement statement= ASTResolving.findParentStatement(coveredNode);
				if (statement == null) {
					continue;
				}
				if (!coveredStatements.contains(statement)) {
					coveredStatements.add(statement);
				}
			}

			Selection nodesInRangeSelection= fAnalyzer.getSelection();
			if (!fAutoClosableNodes.isEmpty()) {
				ASTNode parentBodyDeclaration= (node instanceof Block || node instanceof BodyDeclaration) ?
						node : ASTNodes.getFirstAncestorOrNull(node, Block.class,  BodyDeclaration.class);
				int start= fAutoClosableNodes.get(0).getStartPosition();
				ASTNode lastSelectedNode= fSelectedNodes[fSelectedNodes.length - 1];
				int end= lastSelectedNode.getStartPosition() + lastSelectedNode.getLength();

				for (ASTNode astNode : fAutoClosableNodes) {
					int endPosition= findEndPosition(astNode);
					end= Math.max(end, endPosition);
				}

				// recursive loop to find all nodes affected by wrapping in try block
				nodesInRange= findNodesInRange(parentBodyDeclaration, start, end);
				int oldEnd= end;
				int newEnd= end;
				while (true) {
					newEnd= oldEnd;
					for (ASTNode astNode : nodesInRange) {
						int endPosition= findEndPosition(astNode);
						newEnd= Math.max(newEnd, endPosition);
					}
					if (newEnd > oldEnd) {
						oldEnd= newEnd;
						nodesInRange= findNodesInRange(parentBodyDeclaration, start, newEnd);
						continue;
					}
					break;
				}
				if (nodesInRange.size() > 0) {
					// must recalculate exceptions as additional lines are now in try statement
					ASTNode lastNode= nodesInRange.get(nodesInRange.size() - 1);
					nodesInRangeSelection= Selection.createFromStartEnd(start, lastNode.getStartPosition() + lastNode.getLength());
					exceptions= fAnalyzer.getExceptions(nodesInRangeSelection);
				}
				nodesInRange.removeAll(fAutoClosableNodes);
				nodesInRange.removeAll(Arrays.asList(fSelectedNodes));
			}
		}

		// add required resource statements
		CompilationUnit cu= (CompilationUnit)fSelectedNodes[0].getRoot();
		AST ast= fSelectedNodes[0].getAST();
		Set<String> resourceNameList= new HashSet<>();
		List<ITypeBinding> allExceptions= new ArrayList<>(Arrays.asList(exceptions));
		for (ASTNode coveredNode : fAutoClosableNodes) {
			ASTNode findAncestor= ASTResolving.findAncestor(coveredNode, ASTNode.VARIABLE_DECLARATION_STATEMENT);
			if (findAncestor == null) {
				findAncestor= ASTResolving.findAncestor(coveredNode, ASTNode.ASSIGNMENT);
			}
			if (findAncestor instanceof VariableDeclarationStatement) {
				VariableDeclarationStatement vds= (VariableDeclarationStatement) findAncestor;
				String commentToken= null;
				int extendedStatementStart= cu.getExtendedStartPosition(vds);
				if(vds.getStartPosition() > extendedStatementStart) {
					commentToken= buffer.getText(extendedStatementStart, vds.getStartPosition() - extendedStatementStart);
				}
				Type type= vds.getType();
				ITypeBinding typeBinding= type.resolveBinding();
				if (typeBinding != null) {
					IMethodBinding close= findAutocloseMethod(typeBinding);
					if (close != null) {
						for (ITypeBinding exceptionType : close.getExceptionTypes()) {
							if (!allExceptions.contains(exceptionType)) {
								allExceptions.add(exceptionType);
							}
						}
					}
				}
				String typeName= buffer.getText(type.getStartPosition(), type.getLength());

				for (Object object : vds.fragments()) {
					VariableDeclarationFragment variableDeclarationFragment= (VariableDeclarationFragment) object;
					VariableDeclarationFragment newVariableDeclarationFragment= ast.newVariableDeclarationFragment();
					SimpleName vdsName= variableDeclarationFragment.getName();

					if(commentToken == null) {
						int extendedStart= cu.getExtendedStartPosition(variableDeclarationFragment);
						commentToken= buffer.getText(extendedStart, variableDeclarationFragment.getStartPosition() - extendedStart);
					}
					commentToken= Strings.trimTrailingTabsAndSpaces(commentToken);
					commentToken += commentToken.isEmpty() ? "" : " "; //$NON-NLS-1$ //$NON-NLS-2$

					newVariableDeclarationFragment.setName(ast.newSimpleName(vdsName.getIdentifier()));
					Expression newExpression= null;
					Expression initializer= variableDeclarationFragment.getInitializer();
					if (initializer == null) {
						fRewriter.remove(coveredNode, null);
						continue;
					} else {
						newExpression= (Expression) fRewriter.createMoveTarget(initializer);
					}
					resourceNameList.add(vdsName.getIdentifier());
					newVariableDeclarationFragment.setInitializer(newExpression);
					VariableDeclarationExpression newVariableDeclarationExpression= ast.newVariableDeclarationExpression(newVariableDeclarationFragment);
					newVariableDeclarationExpression.setType(
							(Type) fRewriter.createStringPlaceholder(commentToken + typeName, type.getNodeType()));
					if (modifyExistingTry) {
						if (enclosingTry.resources().isEmpty()) {
							resourcesRewriter.insertFirst(newVariableDeclarationExpression, null);
						} else {
							resourcesRewriter.insertLast(newVariableDeclarationExpression, null);
						}
					} else {
						tryStatement.resources().add(newVariableDeclarationExpression);
					}
					commentToken= null;
				}
//				String commentToken2= ""; //$NON-NLS-1$
//				int extendedStart= cu.getExtendedStartPosition(vds);
//				int extendedLength= cu.getExtendedLength(vds);
//				int endCommentLength= extendedLength - (vds.getStartPosition() - extendedStart) - vds.getLength();
//				if (endCommentLength > 0) {
//					commentToken2= buffer.getText(vds.getStartPosition() + vds.getLength(),
//							endCommentLength);
//					commentToken2= Strings.trimLeadingTabsAndSpaces(commentToken2);
//				}
			}
		}

		List<ITypeBinding> mustRethrowList= new ArrayList<>();
		List<ITypeBinding> catchExceptions= fAnalyzer.calculateCatchesAndRethrows(ASTNodes.filterSubtypes(allExceptions), mustRethrowList);
		List<ITypeBinding> filteredExceptions= ASTNodes.filterSubtypes(catchExceptions);
		if (catchExceptions.size() > 0) {
			LinkedProposalModelCore linkedProposalModel= new LinkedProposalModelCore();
			int i= 0;
			if (!modifyExistingTry) {
				for (ITypeBinding mustThrow : mustRethrowList) {
					CatchClause newClause= ast.newCatchClause();
					SingleVariableDeclaration newDecl= ast.newSingleVariableDeclaration();
					newDecl.setName(ast.newSimpleName(name));
					Type importType= fImportRewrite.addImport(mustThrow, ast, context, TypeLocation.EXCEPTION);
					newDecl.setType(importType);
					newClause.setException(newDecl);
					ThrowStatement newThrowStatement= ast.newThrowStatement();
					newThrowStatement.setExpression(ast.newSimpleName(name));
					linkedProposalModel.getPositionGroup(GROUP_EXC_NAME + i, true).addPosition(fRewriter.track(decl.getName()), false);
					newClause.getBody().statements().add(newThrowStatement);
					tryStatement.catchClauses().add(newClause);
					++i;
				}
			}
			UnionType unionType= getAST().newUnionType();
			List<Type> types= unionType.types();
			for (ITypeBinding exception : filteredExceptions) {
				Type type= fImportRewrite.addImport(exception, getAST(), context, TypeLocation.EXCEPTION);
				types.add(type);
				fLinkedProposalModel.getPositionGroup(GROUP_EXC_TYPE + i, true).addPosition(fRewriter.track(type), i == 0);
				i++;
			}

			decl.setType(unionType);
			catchClause.setException(decl);
			fLinkedProposalModel.getPositionGroup(GROUP_EXC_NAME + 0, true).addPosition(fRewriter.track(decl.getName()), false);
			Statement st= getCatchBody("Exception", name, lineDelimiter); //$NON-NLS-1$
			if (st != null) {
				catchClause.getBody().statements().add(st);
			}
			if (modifyExistingTry) {
				clausesRewriter.insertLast(catchClause, null);
			} else {
				tryStatement.catchClauses().add(catchClause);
			}
		}


		if (fSelectedNodes.length == 1 && fAutoClosableNodes.isEmpty()) {
			ASTNode selectedNode= fSelectedNodes[0];

			if (selectedNode instanceof MethodReference) {
				MethodReference methodReference= (MethodReference) selectedNode;
				IMethodBinding functionalMethod= QuickAssistProcessorUtil.getFunctionalMethodForMethodReference(methodReference);
				// functionalMethod is non-null and non-generic. See ExceptionAnalyzer.handleMethodReference(MethodReference node).
				Assert.isTrue(functionalMethod != null && !functionalMethod.isGenericMethod());
				LambdaExpression lambda= QuickAssistProcessorUtil.convertMethodRefernceToLambda(methodReference, functionalMethod, fRootNode, fRewriter, null, true);
				ASTNode statementInBlock= (ASTNode) ((Block) lambda.getBody()).statements().get(0);
				fRewriter.replace(statementInBlock, replacementNode, null);
				statements.insertLast(statementInBlock, null);
				return;
			}

			LambdaExpression enclosingLambda= ASTResolving.findEnclosingLambdaExpression(selectedNode);
			if (enclosingLambda != null && selectedNode.getLocationInParent() == LambdaExpression.BODY_PROPERTY && enclosingLambda.resolveMethodBinding() != null) {
				QuickAssistProcessorUtil.changeLambdaBodyToBlock(enclosingLambda, getAST(), fRewriter);
				Block blockBody= (Block) fRewriter.get(enclosingLambda, LambdaExpression.BODY_PROPERTY);
				ASTNode statementInBlock= (ASTNode) blockBody.statements().get(0);
				fRewriter.replace(statementInBlock, replacementNode, null);
				statements.insertLast(statementInBlock, null);
				return;
			}

			if (expressionStatement != null) {
				statements.insertLast(expressionStatement, null);
			} else {
				if (!selectedNodeRemoved)
					statements.insertLast(fRewriter.createMoveTarget(selectedNode), null);
			}
			fRewriter.replace(selectedNode, replacementNode, null);
		} else if (modifyExistingTry) {
			ListRewrite source= fRewriter.getListRewrite(enclosingTry.getBody(), Block.STATEMENTS_PROPERTY);
			for (ASTNode node : fAutoClosableNodes) {
				source.remove(node, null);
			}
		} else {
			ListRewrite source= fRewriter.getListRewrite(
					fSelectedNodes[0].getParent(),
					(ChildListPropertyDescriptor)fSelectedNodes[0].getLocationInParent());
			List<ASTNode> nodes= new ArrayList<>(Arrays.asList(fSelectedNodes));
			if (!nodesInRange.isEmpty()) {
				nodes.addAll(nodesInRange);
			}
			int index= fAutoClosableNodes.size();
			if (index < nodes.size()) {
				ASTNode toMove= source.createMoveTarget(nodes.get(index), nodes.get(nodes.size() - 1),
						index == 0 ? replacementNode : null, null);
				statements.insertLast(toMove, null);
			}
			if (index > 0) {
				source.replace(fAutoClosableNodes.get(0), replacementNode, null);
				for (int i= 1; i < index; ++i) {
					source.remove(fAutoClosableNodes.get(i), null);
				}
			}
		}

	}

	public static IMethodBinding findAutocloseMethod(ITypeBinding type) {
		while (type != null) {
			IMethodBinding[] methods= type.getDeclaredMethods();
			for (IMethodBinding method : methods) {
				if ("close".equals(method.getName()) && method.getParameterTypes().length == 0) { //$NON-NLS-1$
					return method;
				}
			}
			type= type.getSuperclass();
		}
		return null;
	}

	private int findEndPosition(ASTNode node) {
		int end= node.getStartPosition() + node.getLength();
		Map<SimpleName, IVariableBinding> nodeSimpleNameBindings= fAnalyzer.getVariableStatementBinding(node);
		List<SimpleName> nodeNames= new ArrayList<>(nodeSimpleNameBindings.keySet());
		if (nodeNames.isEmpty()) {
			return -1;
		}
		SimpleName nodeSimpleName= nodeNames.get(0);
		SimpleName[] coveredNodeBindings= LinkedNodeFinder.findByNode(node.getRoot(), nodeSimpleName);
		if (coveredNodeBindings.length == 0) {
			return -1;
		}
		for (ASTNode astNode : coveredNodeBindings) {
			end= Math.max(end, (astNode.getStartPosition() + astNode.getLength()));
		}
		return end;
	}

	// find all nodes (statements) that are within the start/end positions
	public static List<ASTNode> findNodesInRange(ASTNode astNode, final int start, final int end) {
		List<ASTNode> nodesInRange= new ArrayList<>();
		astNode.accept(new ASTVisitor() {
			int pre= start;

			@Override
			public void preVisit(ASTNode preNode) {
				pre= preNode.getStartPosition();
				super.preVisit(preNode);
			}

			@Override
			public void postVisit(ASTNode postNode) {
				int post= postNode.getStartPosition() + postNode.getLength();
				if (pre >= start && post <= end) {
					Statement statement= ASTResolving.findParentStatement(postNode);
					while (statement != null && statement.getParent() != astNode) {
						ASTNode parent= statement.getParent();
						if (parent == null) {
							return;
						}
						statement= ASTResolving.findParentStatement(parent);
					}
					if (statement != null && !nodesInRange.contains(statement)) {
						nodesInRange.add(statement);
					}
				}
				super.postVisit(postNode);
			}
		});
		return nodesInRange;
	}

	private List<ASTNode> getSpecialVariableDeclarationStatements() {
		List<ASTNode> result= new ArrayList<>(3);
		for (VariableDeclaration local : fAnalyzer.getAffectedLocals()) {
			ASTNode parent= local.getParent();
			if (parent instanceof VariableDeclarationStatement && !result.contains(parent))
				result.add(parent);
		}
		return result;
	}

	private Statement getCatchBody(String type, String name, String lineSeparator) throws CoreException {
		String s= StubUtility.getCatchBodyContent(fCUnit, type, name, fSelectedNodes[0], lineSeparator);
		if (s == null) {
			return null;
		} else {
			return (Statement)fRewriter.createStringPlaceholder(s, ASTNode.RETURN_STATEMENT);
		}
	}
}
