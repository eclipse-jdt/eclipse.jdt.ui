package org.eclipse.jdt.internal.ui.text.correction;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.internal.corext.codemanipulation.NameProposer;
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
	}
	
	private void getAssignToVariableProposals(ICorrectionContext context, List resultingCollections) throws CoreException {
		ASTNode node= context.getCoveringNode();
		
		Statement statement= ASTResolving.findParentStatement(node);
		if (!(statement instanceof ExpressionStatement)) {
			return;
		}
		ExpressionStatement expressionStatement= (ExpressionStatement) statement;

		Expression expression= expressionStatement.getExpression();
		ITypeBinding typeBinding= expression.resolveTypeBinding();
		typeBinding= ASTResolving.normalizeTypeBinding(typeBinding);
		if (typeBinding == null) {
			return;
		}
		ASTRewrite rewrite= new ASTRewrite(expressionStatement.getParent());
		AST ast= statement.getAST();
		NameProposer nameProposer= new NameProposer();
		String[] varName= nameProposer.proposeLocalVariableName(typeBinding.getName());

		VariableDeclarationFragment newDeclFrag= ast.newVariableDeclarationFragment();
		VariableDeclarationStatement newDecl= ast.newVariableDeclarationStatement(newDeclFrag);
		
		newDecl.setType(ASTResolving.getTypeFromTypeBinding(ast, typeBinding));
		newDeclFrag.setName(ast.newSimpleName(varName[0]));
		newDeclFrag.setInitializer((Expression) rewrite.createCopy(expression));
		
		rewrite.markAsReplaced(expressionStatement, newDecl);
		
		String label= CorrectionMessages.getString("QuickAssistProcessor.assigntolocal.description");
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		proposal.addImport(typeBinding);
		proposal.ensureNoModifications();
		resultingCollections.add(proposal);
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
	
	
	

}
