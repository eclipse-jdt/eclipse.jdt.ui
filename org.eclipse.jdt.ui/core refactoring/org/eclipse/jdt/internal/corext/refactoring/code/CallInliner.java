/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.LocalVariableIndex;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.FlowContext;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.FlowInfo;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.InputFlowAnalyzer;
import org.eclipse.jdt.internal.corext.textmanipulation.MultiTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

public class CallInliner {

	private ICompilationUnit fCUnit;
	private ImportEdit fImportEdit;
	private TextBuffer fBuffer;
	private SourceProvider fSourceProvider;
	
	private MethodInvocation fInvocation;
	private List fUsedNames;
	private ASTRewrite fRewriter;
	private List fStatements;
	private int fInsertionIndex;
	private boolean fNeedsStatement;
	private ASTNode fTargetNode;
	private FlowContext fFlowContext;
	private FlowInfo fFlowInfo;

	public CallInliner(ICompilationUnit unit, SourceProvider sourceContext, CodeGenerationSettings settings) throws CoreException {
		super();
		fCUnit= unit;
		fBuffer= TextBuffer.acquire(getFile(fCUnit));
		fSourceProvider= sourceContext;
		fImportEdit= new ImportEdit(fCUnit, settings);
	}

	public void dispose() {
		TextBuffer.release(fBuffer);
	}
	
	public ImportEdit getImportEdit() {
		return fImportEdit;
	}
	
	public RefactoringStatus initialize(MethodInvocation invocation) {
		RefactoringStatus result= new RefactoringStatus();
		fInvocation= invocation;
		Expression exp= fInvocation.getExpression();
		if (exp != null && exp.resolveTypeBinding() == null) {
			result.addFatalError(
				"Can't determine receiver's type.",
				JavaSourceContext.create(fCUnit, exp));
			return result;
		}
		initializeState(fSourceProvider.getNumberOfStatements());
		int nodeType= fTargetNode.getNodeType();
		if (nodeType == ASTNode.EXPRESSION_STATEMENT) {
			if (fSourceProvider.isExecutionFlowInterrupted()) {
				result.addFatalError(
					"Can't inline call. Return statement in method declaration interrupts execution flow.", 
					JavaSourceContext.create(fSourceProvider.getCompilationUnit(), fSourceProvider.getDeclaration()));	
			}
		} else if (nodeType == ASTNode.METHOD_INVOCATION) {
			if (!(isValidParent(fTargetNode.getParent()) || fSourceProvider.getNumberOfStatements() == 1)) {
				result.addFatalError(
					"Can only inline simple functions (consisting of a return statement) or functions used in an assignment.",
					JavaSourceContext.create(fCUnit, fInvocation));
			}
		}
		return result;
	}
	
	private boolean isValidParent(ASTNode parent) {
		int nodeType= parent.getNodeType();
		if (nodeType == ASTNode.ASSIGNMENT)
			return true;
		if (nodeType == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
			parent= parent.getParent();
			if (parent.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
				VariableDeclarationStatement vs= (VariableDeclarationStatement)parent;
				return vs.fragments().size() == 1;
			}
		}
		return false;
	}
	
	public TextEdit perform() throws CoreException {
		int callType= fTargetNode.getNodeType();
		CallContext context= new CallContext(fUsedNames, callType, fImportEdit);
		
		List locals= new ArrayList(3);
		
		computeRealArguments(context, locals);
		computeReceiver(context, locals);
		 		
		String[] blocks= fSourceProvider.getCodeBlocks(context);
		initializeInsertionPoint(fSourceProvider.getNumberOfStatements() + locals.size());
		
		addNewLocals(locals);
		replaceCall(callType, blocks);
		
		MultiTextEdit result= new MultiTextEdit();
		fRewriter.rewriteNode(fBuffer, result, null);
		fRewriter.removeModifications();
		return result;
	}

	private void computeRealArguments(CallContext context, List locals) {
		List arguments= fInvocation.arguments();
		String[] realArguments= new String[arguments.size()];
		for (int i= 0; i < arguments.size(); i++) {
			Expression expression= (Expression)arguments.get(i);
			ParameterData parameter= fSourceProvider.getParameterData(i);
			if ((ASTNodes.isLiteral(expression) && parameter.isReadOnly()) || canInline(expression)) {
				realArguments[i]= getContent(expression);
			} else {
				String name= proposeName(parameter.getName());
				realArguments[i]= name;
				locals.add(createLocalDeclaration(
					parameter.getTypeBinding(), name, 
					(Expression)fRewriter.createCopy(expression)));
			}
		}
		context.arguments= realArguments;
	}

	private void computeReceiver(CallContext context, List locals) {
		Expression receiver= fInvocation.getExpression();
		if (receiver == null)
			return;
		final boolean isName= receiver instanceof Name;
		if (isName)
			context.receiverIsStatic= ((Name)receiver).resolveBinding() instanceof ITypeBinding;
		if (ASTNodes.isLiteral(receiver) || isName) {
			context.receiver= fBuffer.getContent(receiver.getStartPosition(), receiver.getLength());
			return;
		}
		switch(fSourceProvider.getReceiversToBeUpdated()) {
			case 0:
				// Make sure we evaluate the current receiver. Best is to assign to
				// local.
				locals.add(createLocalDeclaration(
					receiver.resolveTypeBinding(), 
					proposeName("r"), 
					(Expression)fRewriter.createCopy(receiver)));
				return;
			case 1:
				context.receiver= fBuffer.getContent(receiver.getStartPosition(), receiver.getLength());
				return;
			default:
				String local= proposeName("r");
				locals.add(createLocalDeclaration(
					receiver.resolveTypeBinding(), 
					local, 
					(Expression)fRewriter.createCopy(receiver)));
				context.receiver= local;
				return;
		}
	}

	private void addNewLocals(List locals) {
		for (Iterator iter= locals.iterator(); iter.hasNext();) {
			ASTNode element= (ASTNode)iter.next();
			fRewriter.markAsInserted(element);
			fStatements.add(fInsertionIndex++, element);
		}
	}

	private void replaceCall(int callType, String[] blocks) {
		// Inline empty body
		if (blocks.length == 0) {
			if (fNeedsStatement) {
				fRewriter.markAsReplaced(fTargetNode, fTargetNode.getAST().newEmptyStatement());
			} else {
				fRewriter.markAsRemoved(fTargetNode);
			}
		} else {
			ASTNode node= null;
			for (int i= 0; i < blocks.length - 1; i++) {
				node= fRewriter.createPlaceholder(blocks[i], ASTRewrite.STATEMENT);
				fRewriter.markAsInserted(node);
				fStatements.add(fInsertionIndex++, node);
			}
			String block= blocks[blocks.length - 1];
			// We can inline a call where the declaration is a function and the call itself
			// is a statement. In this case we have to create a temporary variable if the
			// returned expression must be evaluated.
			if (callType == ASTNode.EXPRESSION_STATEMENT && fSourceProvider.hasReturnValue()) {
				if (fSourceProvider.mustEvaluateReturnValue()) {
					node= createLocalDeclaration(
						fSourceProvider.getReturnType(), 
						proposeName(fSourceProvider.getMethodName()), 
						(Expression)fRewriter.createPlaceholder(block, ASTRewrite.EXPRESSION));
				} else {
					node= null;
				}
			} else if (fTargetNode instanceof Expression) {
				node= fRewriter.createPlaceholder(block, ASTRewrite.EXPRESSION);
				if (needsParenthesis()) {
					ParenthesizedExpression pExp= fTargetNode.getAST().newParenthesizedExpression();
					pExp.setExpression((Expression)node);
					node= pExp;
				}
			} else {
				node= fRewriter.createPlaceholder(block, ASTRewrite.STATEMENT);
			}
			
			// Now replace the target node with the source node
			if (node != null) {
				if (fTargetNode == null) {
					fRewriter.markAsInserted(node);
					fStatements.add(fInsertionIndex++, node);
				} else {
					fRewriter.markAsReplaced(fTargetNode, node);
				}
			} else {
				if (fTargetNode != null) {
					fRewriter.markAsRemoved(fTargetNode);
				}
			}
		}
	}

	private boolean needsParenthesis() {
		if (!fSourceProvider.needsReturnedExpressionParenthesis())
			return false;
		ASTNode parent= fTargetNode.getParent();
		int type= parent.getNodeType();
		return type == ASTNode.METHOD_INVOCATION || (parent instanceof Expression && type != ASTNode.ASSIGNMENT);
	}
	
	private VariableDeclarationStatement createLocalDeclaration(ITypeBinding type, String name, Expression initializer) {
		String typeName;
		if (type.isPrimitive()) {
			typeName= type.getName();
		} else {
			typeName= fImportEdit.addImport(Bindings.getFullyQualifiedImportName(type));
			if (type.isArray()) {
				StringBuffer buffer= new StringBuffer(typeName);
				for (int i= 0; i < type.getDimensions(); i++) {
					buffer.append("[]");
				}
				typeName= buffer.toString();
			}
		}
		VariableDeclarationStatement decl= (VariableDeclarationStatement)ASTNodeFactory.newStatement(
			fInvocation.getAST(), typeName + " " + name + ";");
		((VariableDeclarationFragment)decl.fragments().get(0)).setInitializer(initializer);
		return decl;
	}

	private  void initializeState(int sourceStatements) {
		fRewriter= new ASTRewrite(ASTNodes.getParent(fInvocation, ASTNode.BLOCK));
		fUsedNames= collectUsedNames();
		ASTNode parent= fInvocation.getParent();
		int nodeType= parent.getNodeType();
		if (nodeType == ASTNode.EXPRESSION_STATEMENT || nodeType == ASTNode.RETURN_STATEMENT) {
			fTargetNode= parent;
		} else {
			fTargetNode= fInvocation;
		}
		
		MethodDeclaration decl= (MethodDeclaration)ASTNodes.getParent(fInvocation, ASTNode.METHOD_DECLARATION);
		int numberOfLocals= LocalVariableIndex.perform(decl);
		fFlowContext= new FlowContext(0, numberOfLocals + 1);
		fFlowContext.setConsiderAccessMode(true);
		fFlowContext.setComputeMode(FlowContext.ARGUMENTS);
		Selection selection= Selection.createFromStartLength(fInvocation.getStartPosition(), fInvocation.getLength());
		fFlowInfo= new InputFlowAnalyzer(fFlowContext, selection).perform(decl);
	}

	private boolean canInline(Expression expression) {
		if (expression instanceof Name) {
			IBinding binding= ((Name)expression).resolveBinding();
			if (binding instanceof IVariableBinding) {
				IVariableBinding vb= (IVariableBinding)binding;
				if (!vb.isField()) {
					return fFlowInfo.hasAccessMode(fFlowContext, vb, FlowInfo.UNUSED | FlowInfo.WRITE);
				}
			}
			return true;
		} else if (expression instanceof FieldAccess) {
			return canInline(((FieldAccess)expression).getExpression());
		} else if (expression instanceof ThisExpression) {
			return canInline(((ThisExpression)expression).getQualifier());
		} else if (expression instanceof SuperFieldAccess) {
			return canInline(((SuperFieldAccess)expression).getQualifier());
		}
		return false;
	}
	
	private String proposeName(String defaultName) {
		String result= defaultName;
		int i= 1;
		while (fUsedNames.contains(result)) {
			result= result + i++;
		}
		return result;
	}
	
	private void initializeInsertionPoint(int nos) {
		fStatements= null;
		fInsertionIndex= -1;
		fNeedsStatement= false;
		ASTNode parentStatement= ASTNodes.getParent(fInvocation, Statement.class);
		ASTNode container= parentStatement.getParent();
		int type= container.getNodeType();
		if (type == ASTNode.BLOCK) {
			fStatements= ((Block)container).statements();
			fInsertionIndex= fStatements.indexOf(parentStatement);
		} else if (isControlStatement(container)) {
			fNeedsStatement= true;
			if (nos > 1) {
				Block block= fInvocation.getAST().newBlock();
				fStatements= block.statements();
				fInsertionIndex= 0;
				Statement currentStatement= null;
				switch(type) {
					case ASTNode.FOR_STATEMENT:
						currentStatement= ((ForStatement)container).getBody();
						break;
					case ASTNode.WHILE_STATEMENT:
						currentStatement= ((WhileStatement)container).getBody();
						break;
					case ASTNode.DO_STATEMENT:
						currentStatement= ((DoStatement)container).getBody();
						break;
					case ASTNode.IF_STATEMENT:
						IfStatement node= (IfStatement)container;
						Statement thenPart= node.getThenStatement();
						if (fTargetNode == thenPart || ASTNodes.isParent(fTargetNode, thenPart)) {
							currentStatement= thenPart;
						} else {
							currentStatement= node.getElseStatement();
						}
						break;
				}
				Assert.isNotNull(currentStatement);
				// The method to be inlined is not the body of the control statement.
				if (currentStatement != fTargetNode) {
					ASTNode copy= fRewriter.createCopy(currentStatement);
					fStatements.add(copy);
				} else {
					// We can't replace a copy with something else. So we
					// have to insert all statements to be inlined.
					fTargetNode= null;
				}
				fRewriter.markAsReplaced(currentStatement, block);
			}
		}
		// We only insert one new statement or we delete the existing call. 
		// So there is no need to have an insertion index.
	}

	private String getContent(ASTNode node) {
		return fBuffer.getContent(node.getStartPosition(), node.getLength());
	}

	private static IFile getFile(ICompilationUnit cu) throws CoreException {
		return (IFile)WorkingCopyUtil.getOriginal(cu).getResource();
	}
	
	private List collectUsedNames() {
		BodyDeclaration decl= (BodyDeclaration)ASTNodes.getParent(fInvocation, BodyDeclaration.class);
		NameCollector collector= new NameCollector(fInvocation);
		decl.accept(collector);
		List result= collector.getNames();
		result.remove(fInvocation.getName().getIdentifier());
		return result;
	}
	
	private boolean isControlStatement(ASTNode node) {
		int type= node.getNodeType();
		return type == ASTNode.IF_STATEMENT || type == ASTNode.FOR_STATEMENT ||
		        type == ASTNode.WHILE_STATEMENT || type == ASTNode.DO_STATEMENT;
	}
}
