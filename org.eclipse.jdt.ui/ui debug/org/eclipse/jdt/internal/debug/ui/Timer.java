package org.eclipse.jdt.internal.debug.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import org.eclipse.jdt.debug.core.JDIDebugModel;

/**
 * A timer notifies listeners when a specific amount
 * of time has passed.
 * 
 * @see ITimeoutListener
 */
public class Timer {
	
	/**
	 * Listener to notify of a timeout
	 */
	private ITimeoutListener fListener;
	
	/**
	 * Timeout value, in milliseconds
	 */
	private int fTimeout;
	
	/**
	 * Whether this timer's thread is alive
	 */
	private boolean fAlive = true;
	
	/**
	 * Whether this timer has been started and
	 * has not yet timed out or been stopped.
	 */
	private boolean fStarted = false;
	
	/**
	 * The single thread used for each request.
	 */
	private Thread fThread;
	
	/**
	 * Constructs a new timer
	 */
	public Timer() {
		setTimeout(Integer.MAX_VALUE);
		Runnable r = new Runnable() {
			public void run() {
				while (isAlive()) {
					boolean interrupted = false;
					try {
						Thread.sleep(getTimeout());
					} catch (InterruptedException e) {
						interrupted = true;
					}			
					if (!interrupted) {
						if (getListener() != null) {
							setStarted(false);
							setTimeout(Integer.MAX_VALUE);
							getListener().timeout();
							setListener(null);
						}
					}
				}
			}
		};
		setThread(new Thread(r, JDIDebugModel.getPluginIdentifier() + "Debug Timer")); 
		getThread().setDaemon(true);
		getThread().start();
	}

	/**
	 * Starts this timer, and notifies the given listener when
	 * the time has passed. A call to <code>stop</code>, before the
	 * time expires, will cancel the the timer and timeout callback.
	 * This method can only be called if this timer is idle (i.e.
	 * <code>isStarted() == false<code>).
	 * 
	 * @param listener The timer listener
	 * @param ms The number of milliseconds to wait before
	 * 	notifying the listener
	 */
	public void start(ITimeoutListener listener, int ms) {
		if (isStarted()) {
			throw new IllegalStateException("Debug timer already started"); 
		}
		setListener(listener);
		setTimeout(ms);
		setStarted(true);
		getThread().interrupt();
	}
	
	/**
	 * Stops this timer, cancelling any pending timeout
	 * notification.
	 */
	public void stop() {
		setStarted(false);
		setTimeout(Integer.MAX_VALUE);
		getThread().interrupt();
	}
	
	/**
	 * Disposes this timer
	 */
	public void dispose() {
		setAlive(false);
		getThread().interrupt();
		setThread(null);
	}
	
	/**
	 * Returns whether this timer's thread is alive
	 * 
	 * @return whether this timer's thread is alive
	 */
	private boolean isAlive() {
		return fAlive;
	}

	/**
	 * Sets whether this timer's thread is alive. When
	 * set to <code>false</code> this timer's thread
	 * will exit on its next iteration.
	 * 
	 * @param alive whether this timer's thread should
	 * 	be alive
	 * @see #dispose()
	 */
	private void setAlive(boolean alive) {
		fAlive = alive;
	}

	/**
	 * Returns the current timeout listener
	 * 
	 * @return timeout listener
	 */
	protected ITimeoutListener getListener() {
		return fListener;
	}

	/**
	 * Sets the listener to be notified if this
	 * timer times out.
	 * 
	 * @param listener timeout listener
	 */
	private void setListener(ITimeoutListener listener) {
		fListener = listener;
	}

	/**
	 * Returns whether this timer has been started,
	 * and has not yet timed out, or been stopped.
	 * 
	 * @return whether this timer has been started,
	 * and has not yet timed out, or been stopped
	 */
	public boolean isStarted() {
		return fStarted;
	}

	/**
	 * Sets whether this timer has been started,
	 * and has not yet timed out, or been stopped.
	 * 
	 * @param started whether this timer has been started,
	 *  and has not yet timed out, or been stopped
	 */
	private void setStarted(boolean started) {
		fStarted = started;
	}

	/**
	 * Returns this timer's thread
	 * 
	 * @return thread that waits for a timeout
	 */
	private Thread getThread() {
		return fThread;
	}

	/**
	 * Sets this timer's thread used to perform
	 * timeout processing
	 * 
	 * @param thread thread that waits for a timeout
	 */
	private void setThread(Thread thread) {
		fThread = thread;
	}

	/**
	 * Returns the amount of time, in milliseconds, that
	 * this timer is/was waiting for.
	 * 
	 * @return timeout value, in milliseconds
	 */
	protected int getTimeout() {
		return fTimeout;
	}

	/**
	 * Sets the amount of time, in milliseconds, that this
	 * timer will wait for before timing out.
	 * 
	 * @param timeout value, in milliseconds
	 */
	private void setTimeout(int timeout) {
		fTimeout = timeout;
	}
}