package com.tibco.devtools.workspace.installer;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Implementation of a progress monitor.
 *
 * <p>The {@link IProgressMonitor} interface defines how a progress
 * monitor should be invoked, but the SDK doesn't really seem to do a full
 * implementation of the interface according to the specs.  This
 * implementation attempts to kowtow to the API carefully.
 * </p>
 * 
 * <p>Since this progress monitor simply logs to the console, it also adds
 * support for subdividing the range of progress into fractions.
 */
public class InstallProgressMonitor implements IProgressMonitor {

	/**
	 * Configure the progress monitor to output progress in fractions
	 * of the total work.
	 * 
	 * @param fractions	How many fractions to show?  This effectively
	 * becomes how many parts of 100% do you want to show.  For printing output
	 * no more frequently than every 5%, set fractions to 20.
	 */
	public InstallProgressMonitor(int fractions) {
		m_fractions = fractions; 
	}
	/**
     * Push the monitor into the begun state....
	 */
    public void beginTask(String name, int totalWork) {
        if (m_state != STATE.NOT_STARTED)
            throw new IllegalArgumentException("Attempting to reuse or restart the progress monitor.");

        m_state = STATE.BEGUN;
        m_limit = totalWork;
        m_name = name;
        System.out.println("Started " + name);
	}

	/**
     * Change the state of monitor.
	 */
    public void done() {
        if (m_state == STATE.NOT_STARTED) {
            throw new IllegalArgumentException("Attempting to finish an unstarted progress monitor.");
        }

        System.out.println("Finished " + m_name + ", progress is " + m_progress);
        m_state = STATE.DONE;
	}

	public void internalWorked(double work) {
        int after = (int)m_progress;
        
        m_progress += work;
        int newSegment = after * m_fractions / m_limit; 
        if ( m_lastReport < newSegment) {
        	m_lastReport = newSegment;
	        System.out.format( "%.0f%%\n", getPercentageDone() );
        }
        if ( (int)m_progress > m_limit) {
            System.out.println("Exceeded limit of task's work with value " + m_progress);
        }
	}

	public boolean isCanceled() {
		return false;
	}

	public void setCanceled(boolean value) {
        throw new UnsupportedOperationException("Cancel is not supported on this progress monitor.");
	}

	public void setTaskName(String name) {
	}

	public void subTask(String name) {
		if (name != null && name.length() > 0) {
	        System .out.format( "%.0f%% - "  + name + "\n", getPercentageDone() );
		}
	}

	public void worked(int work) {
        m_progress += work;
        if ( (int)m_progress > m_limit) {
            throw new IllegalStateException("Exceeded limit of task's work.");
        }
	}

	private double getPercentageDone() {
		return (m_progress / (double) m_limit) * 100.0;
	}
	
    private enum STATE {
        NOT_STARTED, BEGUN, DONE
    };

    private double m_progress = 0.0;
    private int m_fractions = 20;
    private int m_lastReport = 0;
    
    private int m_limit;

    private STATE m_state = STATE.NOT_STARTED;
    private String m_name;
}
