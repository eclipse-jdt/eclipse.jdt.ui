package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.dom.GenericVisitor;

public class LocalVariableNameCollector extends GenericVisitor {
	private final List<String> names= new ArrayList<String>();

	@Override
	public boolean visit(VariableDeclarationFragment fragment) {
		names.add(fragment.getName().getIdentifier());
		return false;
	}

	@Override
	public boolean visit(SingleVariableDeclaration node) {
		names.add(node.getName().getIdentifier());
		return false;
	}

	public List<String> getNames() {
		return names;
	}
}
