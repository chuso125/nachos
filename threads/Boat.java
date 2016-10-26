package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
	static Communicator comu;
	static Lock miniLock;
	static int passengers;
	static Island oahu;
	static Island molokai;
	static int canoe = 1;
    static BoatGrader bg;
    static Communicator c;
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
	System.out.println("\n ***Testing Boats with only 2 children***");
	begin(10, 10, b);

//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
//  	begin(1, 2, b);

//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
//  	begin(3, 3, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
    	miniLock = new Lock();
    	oahu = new Island(adults, children, miniLock);
    	molokai = new Island(0,0,miniLock);

    	for(int i = 0; i < adults; i++){
    		Runnable r = new Runnable() {
		    public void run() {
	                AdultItinerary();
	            }
	        };
	        KThread t = new KThread(r);
	        t.setName("Thread Adult: "+i);
	        t.fork();
    	}
    	for(int i = 0; i < children; i++){
    		Runnable r = new Runnable() {
		    public void run() {
	                ChildItinerary();
	            }
	        };
	        KThread t = new KThread(r);
	        t.setName("Thread Child: "+i);
	        t.fork();
    	}
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;

		c = new Communicator();
		c.listen();
		

    }

    static void AdultItinerary(){
    	int position = 1;
    	miniLock.acquire();
    	while(true){
    		if( position==canoe){
    			// si esta en oahu
    			if(position==1){
    				//si hay al menos 2 ni;os se duerme 
    				if(oahu.getChildrens() >=2){
    					oahu.getWaitingLock().sleep();
    				}else if(passengers == 0){// si no hay pasajeros 
    					//cambia el numero de pasajeros
    					//cambia su posicion
    					//resta la cantidad de adultos en oahu 
    					//suma la cantidad de adultos en molokai
    					//cambia la cantidad de personas que hay en oahu referente a molokai 
    					//despierta a todos
    					//cambia la cantidad de pasajeros
    					//cambia la posicion de la canoa
    					//se duerme
    					passengers = 2;
    					position = 2;
    					oahu.setAdults(oahu.getAdult()-1);
    					bg.AdultRowToMolokai();
    					molokai.setAdults(molokai.getAdult()+1);
    					molokai.setPersons(oahu.getChildrens()+ oahu.getAdult());
    					molokai.getWaitingLock().wakeAll();
    					passengers = 0;
    					canoe = 2;
    					molokai.getWaitingLock().sleep();
    				}
    			}else{// si esta en molokai ya solo se tiene que dormir
    				molokai.getWaitingLock().sleep();
    			}
    		}else{
    			//si la canoa no esta en su isla se duerme
    			if(position == 1){
    				oahu.getWaitingLock().sleep();
    			}else
    				molokai.getWaitingLock().sleep();
    		}
    	}


    }

    static void ChildItinerary(){
    	int position = 1;
    	miniLock.acquire();
    	while(true){
    		if( position==canoe){
    			//si estan en oahu
    			if(position==1){
    				if(oahu.getChildrens() < 2){
    					oahu.getWaitingLock().sleep();
    				}else if( passengers == 0){
    					// se mueve a la otra isla 
    					//pone la cantidad de pasajeros en 1
    					//despierta
    					//se duerme
    					position = 2;
    					passengers = 1;
    					bg.ChildRowToMolokai();
    					oahu.getWaitingLock().wakeAll();
    					molokai.waitingLock.sleep();
    				}else if(passengers == 1){
    					// se mueve a la otra isla
    					// resta los 2 ni;os de oahu
    					// pone la canoa llena
    					// agrega ni;os a molokai
    					// setea la cantidad de ni;os de oahu referente a molokai
    					// libera la canoa
    					// despiera
    					// se duerme
    					position = 2;
    					bg.ChildRideToMolokai();
    					oahu.setChildren(oahu.getChildrens()-2);
    					canoe = 2;
    					molokai.setChildren(molokai.getChildrens()+2);
    					molokai.setPersons(oahu.getChildrens()+oahu.getAdult());
    					passengers = 0;
    					molokai.getWaitingLock().wakeAll();
    					molokai.getWaitingLock().sleep();
    				}
    			}else{// si esta en molokai
    				if(molokai.getPersons() == 0){ // si ya no hay gente en la otra isla termino pero hace un speak para esperar que termine
    					c.speak(4);
    					return;
    				}else{
    					// se sube un ni;o a la canoa
    					// se mueve a la oahu
    					// resta la canitdad de ni;os de molokai
    					// suma la cantidad de ni;os en oahu
    					// vacia la canoa
    					// despierta
    					// se duerme
    					position = 1;
    					canoe = 1;
    					bg.ChildRowToOahu();
    					molokai.setChildren(molokai.getChildrens()-1);
    					oahu.setChildren(oahu.getChildrens()+1);
    					passengers = 0;
    					oahu.getWaitingLock().wakeAll();
    					oahu.getWaitingLock().sleep();
    				}
    			}
    		}else{
    			//si la canoa no esta en su isla se duerme
    			if(position == 1){
    				oahu.getWaitingLock().sleep();
    			}else
    				molokai.getWaitingLock().sleep();
    		}
    	}
    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
    }
    // dos islas a utilizar
    static protected class Island{
    	private int children = 0;
    	private int adults = 0;
    	private Condition2 waitingLock;
    	private int persons = 0;
    	public Island(int adults, int children, Lock lock){
    		this.children = children;
    		this.adults = adults;
    		waitingLock = new Condition2(lock);
    	}
    	public void setChildren(int m){
    		children = m;
    	}
    	public void setAdults(int m){
    		adults = m;
    	}
    	public int getAdult(){
    		return adults;
    	}
    	public int getChildrens(){
    		return children;
    	}
    	public void setPersons(int m){
    		persons = m;
    	}
    	public int getPersons(){
    		return persons;
    	}
    	public Condition2 getWaitingLock(){
    		return waitingLock;
    	}
    }
}
