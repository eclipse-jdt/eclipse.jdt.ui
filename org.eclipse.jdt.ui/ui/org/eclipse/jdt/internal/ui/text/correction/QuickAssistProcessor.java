package org.eclipse.jdt.internal.ui.text.correction;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;

import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
  */
public class QuickAssistProcessor implements ICorrectionProcessor {

	/**
	 * Constructor for CodeManipulationProcessor.
	 */
	public QuickAssistProcessor() {
		super();
	}
	
	public void process(ICorrectionContext context, List resultingCollections) throws CoreException {
		int id= context.getProblemId();
		if (id != 0) { // no proposals for problem locations
			return;
		}
		getAssignToVariableProposals(context, resultingCollections);
		getCatchClauseToThrowsProposals(context, resultingCollections);
		getRenameLocalProposals(context, resultingCollections);
	}
	
	private void getAssignToVariableProposals(ICorrectionContext context, List resultingCollections) throws CoreException {
		ASTNode node= context.getCoveringNode();
		
		Statement statement= ASTResolving.findParentStatement(node);
		if (!(statement instanceof ExpressionStatement)) {
			return;
		}
		ExpressionStatement expressionStatement= (ExpressionStatement) statement;

		Expression expression= expressionStatement.getExpression();
		if (expression.getNodeType() == ASTNode.ASSIGNMENT) {
			return; // too confusing and not helpful
		}
		
		
		ITypeBinding typeBinding= expression.resolveTypeBinding();
		typeBinding= ASTResolving.normalizeTypeBinding(typeBinding);
		if (typeBinding == null) {
			return;
		}
		ICompilationUnit cu= context.getCompilationUnit();
		
		AssignToVariableAssistProposal localProposal= new AssignToVariableAssistProposal(cu, AssignToVariableAssistProposal.LOCAL, expressionStatement, typeBinding, 2);
		resultingCollections.add(localProposal);	
		
		ASTNode type= ASTResolving.findParentType(expression);
		if (type != null) {
			AssignToVariableAssistProposal fieldProposal= new AssignToVariableAssistProposal(cu, AssignToVariableAssistProposal.FIELD, expressionStatement, typeBinding, 1);
			resultingCollections.add(fieldProposal);
		}				
	}
	
	private void getCatchClauseToThrowsProposals(ICorrectionContext context, List resultingCollections) throws CoreException {
		ASTNode node= context.getCoveringNode();
		CatchClause catchClause= (CatchClause) ASTResolving.findAncestor(node, ASTNode.CATCH_CLAUSE);
		if (catchClause == null) {
			return;
		}
		Type type= catchClause.getException().getType();
		if (!type.isSimpleType()) {
			return;
		}
		
		BodyDeclaration bodyDeclaration= ASTResolving.findParentBodyDeclaration(catchClause);
		if (!(bodyDeclaration instanceof MethodDeclaration)) {
			return;
		}
		
		MethodDeclaration methodDeclaration= (MethodDeclaration) bodyDeclaration;
		
		ASTRewrite rewrite= new ASTRewrite(methodDeclaration);
		AST ast= methodDeclaration.getAST();
		
		TryStatement tryStatement= (TryStatement) catchClause.getParent();
		if (tryStatement.catchClauses().size() > 1 || tryStatement.getFinally() != null) {
			rewrite.markAsRemoved(catchClause);
		} else {
			List statements= tryStatement.getBody().statements();
			if (statements.size() > 0) {
				ASTNode placeholder= rewrite.createCopy((ASTNode) statements.get(0), (ASTNode) statements.get(statements.size() - 1));
				rewrite.markAsReplaced(tryStatement, placeholder);
			} else {
				rewrite.markAsRemoved(tryStatement);
			}
		}
		Name name= ((SimpleType) type).getName();
		Name newName= (Name) ASTNode.copySubtree(ast, name);
		rewrite.markAsInserted(newName);
		methodDeclaration.thrownExceptions().add(newName);
	
		String label= CorrectionMessages.getString("QuickAssistProcessor.catchclausetothrows.description");
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		proposal.ensureNoModifications();
		resultingCollections.add(proposal);

	}
	
	private void getRenameLocalProposals(ICorrectionContext context, List resultingCollections) throws CoreException {
		ASTNode node= context.getCoveringNode();
		if (!(node instanceof SimpleName)) {
			return;
		}
		SimpleName name= (SimpleName) node;
		IBinding binding= name.resolveBinding();
		if (binding == null || binding.getKind() == IBinding.PACKAGE) {
			return;
		}
		LinkedNamesAssistProposal proposal= new LinkedNamesAssistProposal(name);
		resultingCollections.add(proposal);
	}
	
	

}
