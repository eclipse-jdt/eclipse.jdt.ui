package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.internal.corext.Assert;

public class JdtASTMatcher extends ASTMatcher {

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
	
	public static boolean doNodesMatch(ASTNode one, ASTNode other) {
		Assert.isNotNull(one);
		Assert.isNotNull(other);
		
		return one.subtreeMatch(new JdtASTMatcher(), other);
	}
}
