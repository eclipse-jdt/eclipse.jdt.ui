package org.eclipse.jdt.ui.text;/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */


import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.RuleBasedPartitioner;
import org.eclipse.jface.text.rules.RuleBasedScanner;

import org.eclipse.jdt.internal.ui.text.JavaColorManager;
import org.eclipse.jdt.internal.ui.text.JavaPartitionScanner;
import org.eclipse.jdt.internal.ui.text.java.JavaCodeScanner;
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocScanner;


/**
 * Tools required to configure a Java text viewer. 
 * The color manager and all scanner exist only one time, i.e.
 * the same instances are returned to all clients. Thus, clients
 * share those tools.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class JavaTextTools {
	
	/** The color manager */
	private IColorManager fColorManager;
	/** The Java source code scanner */
	private JavaCodeScanner fCodeScanner;
	/** The JavaDoc scanner */
	private JavaDocScanner fJavaDocScanner;
	/** The Java partitions scanner */
	private JavaPartitionScanner fPartitionScanner;
	
	
	/**
	 * Creates a new Java text tools collection.
	 */
	public JavaTextTools() {
		fColorManager= new JavaColorManager();
		fCodeScanner= new JavaCodeScanner(fColorManager);
		fJavaDocScanner= new JavaDocScanner(fColorManager);
		fPartitionScanner= new JavaPartitionScanner();
	}
	
	/**
	 * Disposes all the individual tools of this tools collection.
	 */
	public void dispose() {
		
		fCodeScanner= null;
		fJavaDocScanner= null;
		fPartitionScanner= null;
		
		fColorManager.dispose();
		fColorManager= null;
	}
	
	/**
	 * Returns the color manager which is used to manage
	 * any Java-specific colors needed for such things like syntax highlighting.
	 *
	 * @return the color manager to be used for Java text viewers
	 */
	public IColorManager getColorManager() {
		return fColorManager;
	}
	
	/**
	 * Returns a scanner which is configured to scan Java source code.
	 *
	 * @return a Java source code scanner
	 */
	public RuleBasedScanner getCodeScanner() {
		return fCodeScanner;
	}
	
	/**
	 * Returns a scanner which is configured to scan JavaDoc compliant comments.
	 * Notes that the start sequence "/**" and the corresponding end sequence
	 * are part of the JavaDoc comment.
	 *
	 * @return a JavaDoc scanner
	 */
	public RuleBasedScanner getJavaDocScanner() {
		return fJavaDocScanner;
	}
	
	/**
	 * Returns a scanner which is configured to scan 
	 * Java-specific partitions, which are multi-line comments,
	 * JavaDoc comments, and regular Java source code.
	 *
	 * @return a Java partition scanner
	 */
	public RuleBasedScanner getPartitionScanner() {
		return fPartitionScanner;
	}
	
	/**
	 * Factory method for creating a Java-specific document partitioner
	 * using this object's partitions scanner. This method is a 
	 * convenience method.
	 *
	 * @return a newly created Java document partitioner
	 */
	public IDocumentPartitioner createDocumentPartitioner() {
		
		String[] types= new String[] {
			JavaPartitionScanner.JAVA_DOC,
			JavaPartitionScanner.JAVA_MULTILINE_COMMENT
		};
		
		return new RuleBasedPartitioner(getPartitionScanner(), types);
	}
	
	/**
	 * Returns the names of the document position categories used by the document
	 * partitioners created by this object to manage their partition information.
	 * If the partitioners don't use document position categories, the returned
	 * result is <code>null</code>.
	 *
	 * @return the partition managing position categories or <code>null</code> 
	 * 			if there is none
	 */
	public String[] getPartitionManagingPositionCategories() {
		return new String[] { RuleBasedPartitioner.CONTENT_TYPES_CATEGORY };
	}
}