package org.eclipse.jdt.internal.corext.refactoring.code;

import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.SimpleName;

class JdtASTMatcher extends ASTMatcher {

	public boolean match(SimpleName node, Object other) {
		boolean isomorphic= super.match(node, other);
		if (! isomorphic || !(other instanceof SimpleName))
			return false;
		SimpleName name= (SimpleName)other;
		if (node.resolveBinding() != name.resolveBinding())
			return false;
		if (node.resolveTypeBinding() != name.resolveTypeBinding())
			return false;
		return true;	
	}
}
