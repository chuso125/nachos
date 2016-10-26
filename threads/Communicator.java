package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    Lock comLock = new Lock();
    Condition2 okToListen = new Condition2(comLock);
    Condition2 okToSpeak = new Condition2(comLock);
    int message = 0;
    boolean canSpeak = true;
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param   word    the integer to transfer.
     */
    public void speak(int word) 
    {
        comLock.acquire();
        while(!canSpeak){
            okToSpeak.sleep();
        }
        canSpeak = false;
        comLock.release();
        message = word;
        System.out.println("Thread: "+KThread.currentThread()+ " wrote: "+ message);
        comLock.acquire();
        if(canSpeak){
            okToSpeak.wake();
        }
        else if(!canSpeak){
            okToListen.wake();
        }
        comLock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return  the integer transferred.
     */    
    public int listen() 
    {
       comLock.acquire();
       while(canSpeak){
            okToListen.sleep();
       } 
       canSpeak = true;
       comLock.release();
       System.out.println("Thread: "+KThread.currentThread()+ " read: "+ message);
       comLock.acquire();
       if(canSpeak){
            okToSpeak.wake();
       }
       comLock.release();
       return message;
    }
}
