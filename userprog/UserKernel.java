package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import java.util.*; 

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
	super();
    }

    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    public void initialize(String[] args) {
	super.initialize(args);

	console = new SynchConsole(Machine.console());
	
	Machine.processor().setExceptionHandler(new Runnable() {
		public void run() { exceptionHandler(); }
	    });
    int numPhysPages = Machine.processor().getNumPhysPages();     
    for(int i = 0; i < numPhysPages; i++)                                  
        pageTable.add(i);                                         

    }

    /**
     * Test the console device.
     */	
    public void selfTest() {
	super.selfTest();

	System.out.println("Testing the console device. Typed characters");
	System.out.println("will be echoed until q is typed.");

	char c;

	do {
	    c = (char) console.readByte(true);
	    console.writeByte(c);
	}
	while (c != 'q');

	System.out.println("");
    }

    /**
     * Returns the current process.
     *
     * @return	the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
	if (!(KThread.currentThread() instanceof UThread))
	    return null;
	
	return ((UThread) KThread.currentThread()).process;
    }

    /**
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     *
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    public void exceptionHandler() {
	Lib.assertTrue(KThread.currentThread() instanceof UThread);

	UserProcess process = ((UThread) KThread.currentThread()).process;
	int cause = Machine.processor().readRegister(Processor.regCause);
	process.handleException(cause);
    }

    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see	nachos.machine.Machine#getShellProgramName
     */
    public void run() {
	super.run();

	UserProcess process = UserProcess.newUserProcess();
	
	String shellProgram = Machine.getShellProgramName();	
	Lib.assertTrue(process.execute(shellProgram, new String[] { }));

	KThread.currentThread().finish();
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }
    //metodo para regresar el numero de una pagina libre
    public static int getFreePage(){
        int pageNumber = -1;
        Machine.interrupt().disable();
        if(pageTable.isEmpty()== false){
            pageNumber = pageTable.removeFirst();
        }
        Machine.interrupt().enabled();
        return pageNumber;
    }
    //metodo para agregar una pagina nueva
    public static void setFreePage(int number){
        Lib.assertTrue(number<Machine.processor().getNumPhysPages());
        Machine.interrupt().disable();
        pageTable.add(number);
        Machine.interrupt().enabled();
    }
    //obtener el siguiente id
    public static int getNextPId(){
        int ret;
        Machine.interrupt().disable();
        ret = pId++;
        Machine.interrupt().enabled();
        return ret;
    }
    public static UserProcess getProcessById(int id){
        System.out.println(processList);
        return processList.get(id);
    }
    public static UserProcess setProcess(int id, UserProcess process) {  
        UserProcess ret;                              
        Machine.interrupt().disable();                           
        ret = processList.put(id, process);            
        Machine.interrupt().enabled();                          
        return ret;                                     
    }
    public static UserProcess removeProcess(int id) {  
        UserProcess ret;                              
        Machine.interrupt().disable();                           
        ret = processList.remove(id);            
        Machine.interrupt().enabled();                          
        return ret;                                     
    }
    // lista de physical pages libres
    private static LinkedList<Integer> pageTable = new LinkedList<Integer>();

    //id del proximo proceso 
    private static int pId;
    //array de los procesos con su respectivo pId 
    private static HashMap<Integer, UserProcess>  processList = new HashMap<Integer, UserProcess>();

    /** Globally accessible reference to the synchronized console. */
    public static SynchConsole console;

    // dummy variables to make javac smarter
    private static Coff dummy1 = null;
}
