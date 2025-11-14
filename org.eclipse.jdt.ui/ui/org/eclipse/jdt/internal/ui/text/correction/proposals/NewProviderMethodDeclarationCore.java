package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;

public class NewProviderMethodDeclarationCore extends AbstractMethodCorrectionProposalCore {

	private IType fReturnType;

	public NewProviderMethodDeclarationCore(String label, ICompilationUnit targetCU, ASTNode invocationNode, ITypeBinding binding, int relevance, IType returnType) {
		super(label, targetCU, invocationNode, binding, relevance);
		this.fReturnType= returnType;
	}

	@Override
	protected boolean isConstructor() {
		return false;
	}

	@Override
	protected void addNewModifiers(ASTRewrite rewrite, ASTNode targetTypeDecl, List<IExtendedModifier> modifiers) {
		modifiers.addAll(rewrite.getAST().newModifiers(Modifier.PUBLIC | Modifier.STATIC));
	}

	@Override
	protected void addNewTypeParameters(ASTRewrite rewrite, List<String> takenNames, List<TypeParameter> params, ImportRewriteContext context) throws CoreException {
		// No type parameters needed, void type
	}

	@Override
	protected void addNewParameters(ASTRewrite rewrite, List<String> takenNames, List<SingleVariableDeclaration> params, ImportRewriteContext context) throws CoreException {
		// no parameters needed, void type
	}

	@Override
	protected void addNewExceptions(ASTRewrite rewrite, List<Type> exceptions, ImportRewriteContext context) throws CoreException {
		// no exceptions thrown
	}

	@Override
	protected SimpleName getNewName(ASTRewrite rewrite) {
		AST ast= rewrite.getAST();
		SimpleName nameNode= ast.newSimpleName("provider"); //$NON-NLS-1$ // TypeConstants.PROVIDER
		return nameNode;
	}

	@Override
	protected Type getNewMethodType(ASTRewrite rewrite, ImportRewriteContext context) throws CoreException {
		getImportRewrite().addImport(fReturnType.getFullyQualifiedName());
		AST ast= rewrite.getAST();
		Type type= ast.newSimpleType(ast.newSimpleName(fReturnType.getElementName()));
		addLinkedPosition(rewrite.track(type), true, "return_type"); //$NON-NLS-1$
		return type;
	}
}