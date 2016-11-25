package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.*; 

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    private OpenFile[] openFiles;
    public UserProcess() {
        this.openFiles = new OpenFile[16];
        this.openFiles[0] = UserKernel.console.openForReading();
        this.openFiles[1] = UserKernel.console.openForWriting();

	int numPhysPages = Machine.processor().getNumPhysPages();
	pageTable = new TranslationEntry[numPhysPages];
	for (int i=0; i<numPhysPages; i++)
	    pageTable[i] = new TranslationEntry(i,i, true,false,false,false);
    //agregar este proceso a la lista de procesos
    UserKernel.setProcess(UserKernel.getNextPId(), this);
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
	if (!load(name, args))
	    return false;
	//settearlo a la variable que creamos
	thread = new UThread(this);
    thread.setName(name).fork();

	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);
    //la memoria de cada proceso
	byte[] memory = Machine.processor().getMemory();

        Processor procesador = Machine.processor();
        //metodos que ya existen en processor
        int vpn = procesador.pageFromAddress(vaddr);
        
        //la variable de la pagina para traducir de virtual a  fisica
        TranslationEntry pagina = pageTable[vpn];
        //ponemos la variable used a true como dice la documentacion de TranslationEntry
        pagina.used = true;
        int ppn = pagina.ppn;

        int virtualOffset = procesador.offsetFromAddress(vaddr);
        //la cantidad de paginas por su tama;o mas el offset virtual es la traduccion para la direccion fisica
        int physicalAddress = (ppn*pageSize)+virtualOffset;

        if( ppn > procesador.getNumPhysPages() || ppn<0){
            System.out.println("Physical page number out of range!");
            return 0;
        }
        //cambiamos a physicalAddress porque antes la virtual era la misma que la fisica 
        //ahora es diferente porque leemos de la virtual a la fisica
    	int amount = Math.min(length, memory.length-physicalAddress);
        // arraycopy(src,srcPos, dest, destPos, length)
    	System.arraycopy(memory, physicalAddress, data, offset, amount);

	return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	// for now, just assume that virtual addresses equal physical addresses
	if (vaddr < 0 || vaddr >= memory.length)
	    return 0;

        Processor procesador = Machine.processor();
        //metodos que ya existen en processor
        int vpn = procesador.pageFromAddress(vaddr);
        //la variable de la pagina para traducir de virtual a  fisica
        TranslationEntry pagina = pageTable[vpn];
        //ponemos la variable used a true como dice la documentacion de TranslationEntry
        pagina.used = true;
        //ponemos la variable dirty tambien dicho en TranslationEntry
        pagina.dirty = true;
        //si es readonly es error
        if(pagina.readOnly){
            System.out.println("Page is read only!");
            return 0;
        }
        int ppn = pagina.ppn;

        int virtualOffset = procesador.offsetFromAddress(vaddr);
        //la cantidad de paginas por su tama;o mas el offset virtual es la traduccion para la direccion fisica
        int physicalAddress = (ppn*pageSize)+virtualOffset;

        if( ppn > procesador.getNumPhysPages() || ppn<0){
            System.out.println("Physical page number out of range!");
            return 0;
        }
    // este no se cambia porque si se copia de la memoria virtual
	int amount = Math.min(length, memory.length-vaddr);
	System.arraycopy(data, offset, memory, vaddr, amount);
        //System.out.println("Copied amount: "+amount);
	return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}

	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}

	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
	    numPages += section.getLength();
	}

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}

	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
	numPages++;

        // instanciar la pageTable con la cantidad de paginas que necesita el coff
        pageTable = new TranslationEntry[numPages]; 
        for (int i = 0; i < numPages; i++) {                                               
            int ppn = UserKernel.getFreePage(); 
            pageTable[i] =  new TranslationEntry(i, ppn, true, false, false, false);       
        } 

	if (!loadSections())
	    return false;

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;
	
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}

	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
	if (numPages > Machine.processor().getNumPhysPages()) {
	    coff.close();
	    Lib.debug(dbgProcess, "\tinsufficient physical memory");
	    return false;
	}

    	// load sections
    	for (int s=0; s<coff.getNumSections(); s++) {
    	    CoffSection section = coff.getSection(s);
    	    
    	    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
    		      + " section (" + section.getLength() + " pages)");

    	    for (int i=0; i<section.getLength(); i++) {
        		int vpn = section.getFirstVPN()+i;
                //vamos a cargar la pagina de la virtual memory 
                // y vamos a agarrar su ppn para hacer luego el loadPage ya con este ppn y no vpn
                TranslationEntry pagina = pageTable[vpn];
                pagina.readOnly = section.isReadOnly();
                int ppn = pagina.ppn;
        		// ahora se carga la physical no la virtual 
        		section.loadPage(i, ppn);
    	    }
    	}
	
	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        //liberar todas las paginas que ya fueron usadas;
        //si se pone valid igual a false se pueden usar, como dice en TranslationEntry
        for (int i = 0; i < numPages;i++) {
            UserKernel.setFreePage(pageTable[i].ppn);
            pageTable[i].valid = false;
            
        }
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }
    private int getFreeSpace(){
        for (int i = 0 ; i<this.openFiles.length ;i++ ) {
            if(this.openFiles[i]== null)
                return i;
        }
        return -1;
    }
    // handle creat
    private int handleCreat(int positionOfName){
        String fileName = readVirtualMemoryString(positionOfName, 256);
        //falta ver si es valido
        //ver si ya esta abierto
        for (int i = 0 ;i< this.openFiles.length; i++) {
            if(this.openFiles[i] != null && this.openFiles[i].getName().equals(fileName))
                return i;
        }
        //indice en donde hay espacio libre
        int index = getFreeSpace();
        if(index != -1){
            OpenFile file = ThreadedKernel.fileSystem.open(fileName, true);
            this.openFiles[index] = file;
        }
        return index;
    }
    private int handleOpen( int positionOfName){
        String fileName = readVirtualMemoryString(positionOfName, 256);
        //falta ver si es valido
        //ver si ya esta abierto
        for (int i = 0 ;i< this.openFiles.length; i++) {
            if(this.openFiles[i] != null && this.openFiles[i].getName().equals(fileName))
                return i;
        }
        //indice en donde hay espacio libre
        int index = getFreeSpace();
        if(index != -1){
            OpenFile file = ThreadedKernel.fileSystem.open(fileName, false);
            this.openFiles[index] = file;
        }
        return index;
    }
    private int handleRead(int index, int bufferStart, int bytesCount){
        OpenFile file = this.openFiles[index];
        byte[] bytes = new byte[bytesCount];
        //se lee la cantidad de bytes del archivo
        int bytesRead = file.read(bytes, 0, bytesCount);
        // se escribe en memoria virtual desde el inicio del buffer mandado hasta la cantidad de bytes a leer
        int bytesWritten = writeVirtualMemory(bufferStart, bytes, 0, bytesRead);
        return bytesWritten;
    }
    private int handleWrite(int index, int bufferStart, int bytesCount){
        OpenFile file = this.openFiles[index];
        byte[] bytes = new byte[bytesCount];
        // se lee desde memoria virtual desde el inicio del buffer mandado hasta la cantidad de bytes a leer
        int bytesRead = readVirtualMemory(bufferStart, bytes);
        //se escribe en el archivo
        int bytesWritten = file.write(bytes, 0, bytesRead);
        return bytesWritten;
    }
    private int handleClose(int index){
        OpenFile file = this.openFiles[index];
        this.openFiles[index] = null;
        file.close();
        return 0;
    }
    /*
    PARAMS: a0, es el address del filename    
    */
    public int handleUnlink(int a0){
            System.out.println("Entro al unlink");
            boolean returned = true;
            String filename = readVirtualMemoryString(a0,256);
            
            System.out.println("El archivo es: " + filename);

            if(filename == null){
                returned = false;
            }
            
            boolean val=ThreadedKernel.fileSystem.remove(filename);
            
            if(returned){
                return 0;
            }
            else{
                return -1;
            }
    }
    // cerrar el proceso
    public void handleExit(int status){
        // se cierran todos los archivos abiertos del proceso
        for (int i = 0; i< 16 ;i++ ) {
            if(openFiles[i] != null){
                handleClose(i);
            }
        }
        // se settea a todos los procesos hijos el id 1 del proceso principal
        while(childProcesses != null && !childProcesses.isEmpty()){
            int cId = childProcesses.removeFirst();
            UserProcess cProcess = UserKernel.getProcessById(cId);
            cProcess.parentId = 1;
        }
        //poner el status general al status que mando el llamado de la funcion
        exitStatus = status;
        //liberar la memoria que uso el proceso (las pages)
        unloadSections();
        //terminar si es el proceso principal
        if(this.pId == 1){
            Kernel.kernel.terminate();
        }else{//sino es solo cerrar el thread actual
            KThread.currentThread().finish();
        }

    }
    //hacer el join del proceso hijo
    public int handleJoin(int processId, int status){
        boolean found = false;
        //ver si el proceso esta entre los procesos hijos del proceso padre
        for (int i = 0;i<childProcesses.size() ;i++ ) {
            if(processId == childProcesses.get(i)){
                childProcesses.remove(i);
                found = true;
            }
        }
        if(!found){
            System.out.println("Process: "+ pId + " doesn't have child: "+processId+"!");
            return -1;
        }
        //hacer join al proceso hijo
        UserProcess childProcess = UserKernel.getProcessById(processId);

        childProcess.thread.join();
        //quitar el proceso de la lista de procesos
        UserKernel.removeProcess(processId);
        //
        byte temp[] = new byte[4];                                         
        temp=Lib.bytesFromInt(childProcess.exitStatus);                    
        int cntBytes = writeVirtualMemory(status, temp);                
        if (cntBytes != 4)                                                 
            return 1;                                                      
        else                                                               
           return 0;

    }
    //argc es el numero de parametros
    //argv puntero donde inician los parametros
    private int handleExec(int file, int argc, int argv) { 
        // argc es el numero de parametros a pasarle al hijo
        if( argc <1 ){
            System.out.println("argc < 1");
            return -1;
        }
        // el archivo tiene que tener ".coff"
        String filename = readVirtualMemoryString(file, 256);
        System.out.println(filename );
        if(filename == null){
            System.out.println("null filename!");
        }
        String cff = filename.substring(filename.length()-4, filename.length());
        if(cff.equals(".coff")){
            System.out.println("Wrong extension!");
        }
        // get the parameters of the child process
        String args[] = new String[argc];
        byte temp[] = new byte[4];
        for (int i = 0; i<argc ;i++) {
            //ver si la cantidad de bytes esta bien en el parametro
            //se lee el parametro y se pone en temp
            int bytes = readVirtualMemory(argv+i*4, temp);
            if(bytes != 4)
                return -1;
            // se transforma el temp a int para poderlo leer en readVirtualMemoryString
            int stringAddress = Lib.bytesToInt(temp, 0);
            // se guarda el parametro en la lista de parametros
            args[i] = readVirtualMemoryString(stringAddress, 256);

        }
        //crear el proceso hijo
        UserProcess process = UserProcess.newUserProcess();
        //seteamos el id del padre del hijo como el id actual
        process.parentId = this.pId;
        //agregamos a la lista de hijos
        childProcesses.add(process.pId);
        boolean ret = process.execute(filename, args);
        if(ret)
            return process.pId;
        else
            return -1;

    }
    /**OpenFile file = this.openFiles[index];
        byte[] bytes = new byte[bytesCount];
        //se lee la cantidad de bytes que dijeron
        int bytesRead = file.read(bytes, 0, bytesCount)
        // se escribe en memoria virtual desde el inicio del buffer mandado hasta la cantidad de bytes a leer
        int bytesWritten = file.writeVirtualMemory(bufferStart, bytes, 0, bytesRead)
        return bytesWritten;
     * Handle the halt() system call. 
     */
    private int handleHalt() {

	Machine.halt();
	
	Lib.assertNotReached("Machine.halt() did not halt machine!");
	return 0;
    }


    private static final int
        syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
    	switch (syscall) {
    	case syscallHalt:
    	    return handleHalt();
        case syscallCreate:
            return handleCreat(a0);
        case syscallOpen:
            return handleOpen(a0);
        case syscallRead:
            return handleRead(a0, a1, a2);
        case syscallWrite:
            return handleWrite(a0, a1, a2);
        case syscallClose:
            return handleClose(a0);
        case syscallUnlink:
            return handleUnlink(a0);
        case syscallExit:
            handleExit(a0);
            return 0;
        //case syscallJoin:
            //return handleJoin(a0, a1);
        case syscallExec:
            return handleExec(a0, a1, a2);
    	default:
    	    Lib.debug(dbgProcess, "Unknown syscall " + syscall);
    	    //Lib.assertNotReached("Unknown system call!");
    	}
    	return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;				       
				       
	default:
	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
        handleExit(-1);
	    Lib.assertNotReached("Unexpected exception");
	}
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    public int pId;
    private int parentId;
    private LinkedList<Integer> childProcesses = new LinkedList<Integer>();
    private int exitStatus;
    private UThread thread;
}
