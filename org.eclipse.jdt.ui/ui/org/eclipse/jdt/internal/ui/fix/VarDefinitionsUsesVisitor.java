package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;

/** Visitor collecting all definitions and uses of a local variable. */
public final class VarDefinitionsUsesVisitor extends ASTVisitor {
	private final ASTNode variableDeclaration;
	private final ASTNode scopeNode;
	private final boolean includeInnerScopes;
	private final List<SimpleName> writes= new ArrayList<>();
	private final List<SimpleName> reads= new ArrayList<>();

	/**
	 * Builds with the variable binding to look for and the scope where to look for
	 * references.
	 *
	 * @param variableBinding the variable binding to find, cannot be {@code null}
	 * @param scopeNode       the {@link ASTNode} which is the scope of the search
	 * @param includeInnerScopes True if the sub blocks should be analyzed
	 */
	public VarDefinitionsUsesVisitor(final IVariableBinding variableBinding, final ASTNode scopeNode, final boolean includeInnerScopes) {
		this.variableDeclaration= ASTNodes.findDeclaration(variableBinding, scopeNode);
		this.scopeNode= getDeclaringScope(variableDeclaration);
		this.includeInnerScopes= includeInnerScopes;
	}

	private static ASTNode getDeclaringScope(final ASTNode variableDeclaration) {
		ASTNode node= variableDeclaration.getParent();

		while (isVariableDeclaration(node)) {
			node= node.getParent();
		}

		return node;
	}

	private static boolean isVariableDeclaration(final ASTNode node) {
		switch (node.getNodeType()) {
		case ASTNode.SINGLE_VARIABLE_DECLARATION:
		case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
		case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
		case ASTNode.VARIABLE_DECLARATION_STATEMENT:
			return true;

		default:
			return false;
		}
	}

	/**
	 * Finds all the definitions and uses of the variable.
	 *
	 * @return this visitor
	 */
	public VarDefinitionsUsesVisitor find() {
		if (variableDeclaration != null && scopeNode != null) {
			scopeNode.accept(this);
		}

		return this;
	}

	@Override
	public boolean visit(final SimpleName node) {
		ASTNode occurrence= ASTNodes.findDeclaration(node.resolveBinding(), node);

		if (variableDeclaration != null && variableDeclaration.equals(occurrence)) {
			switch (node.getParent().getNodeType()) {
			case ASTNode.ASSIGNMENT:
				addWriteOrRead(node, Assignment.LEFT_HAND_SIDE_PROPERTY);
				break;

			case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
				addWriteOrRead(node, VariableDeclarationFragment.NAME_PROPERTY);
				break;

			case ASTNode.SINGLE_VARIABLE_DECLARATION:
				addWriteOrRead(node, SingleVariableDeclaration.NAME_PROPERTY);
				break;

			default:
				reads.add(node);
				break;
			}
		}

		return true;
	}

	@Override
	public boolean visit(final Block node) {
		return scopeNode == node || includeInnerScopes;
	}

	private void addWriteOrRead(final SimpleName node, final ChildPropertyDescriptor definitionPropertyDescriptor) {
		if (node.getLocationInParent() == definitionPropertyDescriptor) {
			writes.add(node);
		} else {
			reads.add(node);
		}
	}

	/**
	 * Returns all the definitions (declarations and assignments) found.
	 *
	 * @return all the definitions found.
	 */
	public List<SimpleName> getWrites() {
		return writes;
	}

	/**
	 * Returns all the uses found.
	 *
	 * @return all the uses found.
	 */
	public List<SimpleName> getReads() {
		return reads;
	}
}
