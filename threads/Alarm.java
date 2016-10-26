package nachos.threads;

import nachos.machine.*;

import java.util.*;


/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	ArrayList<WakeUp> sleepQueue = new ArrayList<WakeUp>();
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
    	Machine.interrupt().disable();
    	Iterator iterator = sleepQueue.iterator();
    	while(iterator.hasNext()){
    		WakeUp wakee = (WakeUp) iterator.next();
    		long time = wakee.getWakTime();
    		long now = Machine.timer().getTime();
    		if(time <= now){
    			KThread wake = wakee.getWakeThread();
    			wake.ready();
    			iterator.remove();
    		}
    	}
    	Machine.interrupt().enable();
	KThread.currentThread().yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		long wakeTime = Machine.timer().getTime() + x;
		KThread sleep = KThread.currentThread();
		WakeUp alarmTime = new WakeUp(sleep, wakeTime);
		Machine.interrupt().disable();
		sleepQueue.add(alarmTime);
		sleep.sleep();
		Machine.interrupt().enable();

    }

    //nueva clase 
    private class WakeUp{
    	KThread wake;
    	long wakeTime;
    	public WakeUp(KThread wakeNow, long wakeNowTime){
    		wake = wakeNow;
    		wakeTime = wakeNowTime;
    	}
    	public KThread getWakeThread(){
    		return wake;
    	}
    	public long getWakTime(){
    		return wakeTime;
    	}
    	public void setWakeThread(KThread wakeNow){
    		wake = wakeNow;
    	}
    	public void setWakeTime(long wakeNowTime){
    		wakeTime = wakeNowTime;
    	}

    }
}
