/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.internal;

/**
 * Message identifiers for messages sent by the
 * TestRunServer
 */
public class MessageIds {
	public static final int MSG_HEADER_LENGTH= 8;
	
	// messages send by TestRunServer
	public static final String TRACE_START= "%TRACES ";
	public static final String TRACE_END=   "%TRACEE ";
	public static final String TEST_COUNT=  "%TESTC  ";
	public static final String TEST_START=  "%TESTS  ";		
	public static final String TEST_END=    "%TESTE  ";		
	public static final String TEST_ERROR=  "%ERROR  ";		
	public static final String TEST_FAILED= "%FAILED ";			
	public static final String TEST_ELAPSED_TIME="%RUNTIME";	
	public static final String TEST_STOPPED="%TSTSTP ";
	public static final String TEST_TREE="%TSTTREE";
	public static final String TEST_TREE_START="%TREES  ";
			
	// messages understood by the TestRunServer	
	public static final String TEST_STOP=		">STOP   ";
}

