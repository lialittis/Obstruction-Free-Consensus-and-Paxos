package com.paxos;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

//import com.ref.Messages;
//import com.system_creation.ProcessBC.QuorumRequest;
//import com.system_creation.ProcessBC.ReadResponse;
//import com.system_creation.ProcessBC.StampedValue;

public class Process extends UntypedAbstractActor {
    
	private static final boolean Ture = false;
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);// Logger attached to actor
    private final int N; //number of processes
    private final int id; //id of current process
    private int numACK = 0;
    private Members processes; //other processes' references
    private int proposal;
    
    private int ballot;
    private int readballot;
    private int imposeballot;
    private int estimate;
    
    private double probCrash = 0.5;
    static Random random = new Random();

    
    public Process(int ID, int nb) {
        N = nb;
        id = ID;
        ballot = ID - N;
        readballot = 0;
        imposeballot = ID - N;
        estimate = 0;
    }
    
    public String toString() {
        return "Process{" + "id=" + id ;
    }

    /**
     * Static function creating actor
     */
    public static Props createActor(int ID, int nb) {
        return Props.create(Process.class, () -> {
            return new Process(ID, nb);
        });
    }
    
	// state of the process, 1 - active, 2 - faulty, 3 - holding, 4 - silent / crash 5 - decided
    private int state;
    
	static public class State {
		public final int state;

		public State(int state) {
			this.state = state;
		}
	}
    
	// local message/state buffer (to store states received in a Quorum or read phase)
	private ArrayList states = new ArrayList();
    
    
    private boolean isCrashed() {
    	if(this.state == 2 ) {
    		if(random.nextDouble() <= probCrash) {
    			this.state = 4;
    			log.info("p" + id + " crashes now !");
    			return true;
    		}
    	}else if(this.state == 4){
    		return true;
    	}
    	return false;
    }
    
    public void propose(int v) {
        states.clear();
        proposal = v;
        ballot = ballot + N;
        // estimate = proposal; 
        // log.info("Process: p" + id + ": [proposes " + v + ", ballot " + ballot + "]");
        // log.info("processes are:"+ processes.references);
        read();
    }
    
    public void read() {
    	for (ActorRef actor : processes.references) {
            actor.tell(new ReadMsg(ballot), this.getSelf());
            // log.info("Read ballot " + ballot + " msg: p" + self().path().name() + " -> p" + actor.path().name());
        }
    }
    
    private void gather(int newBallot, ActorRef pj) {
    	if(isCrashed()) return;
        // log.info("Received new ballot " + ballot + " msg: p" + self().path().name() + " <- p" + pj.path().name());

    	if (newBallot < readballot || imposeballot > newBallot) {
    		// log.info("ABORT with readballot "+ readballot + " imposeballot " + imposeballot + " newBallot " + newBallot);
    		pj.tell(new GatherMsg(newBallot,"ABORT"),this.getSelf()); 
    	}
    	else {
    		readballot = newBallot ;   
    		//log.info("GATHER with readballot "+ readballot + " imposeballot " + imposeballot + " newBallot " + newBallot);
    		pj.tell(new GatherMsg(newBallot,"GATHER",imposeballot,estimate), this.getSelf());
            
    	}
    }
    
    private void impose(GatherMsg m) {
    	if(isCrashed()) return;
    	
    	if(m.message.equals("ABORT")) {
    		// return;
    		if(this.state <= 2 && ballot == m.ballot)
    			this.propose(proposal);  // RE-propose once it receive a ABORT
//    		else
//    			log.info("FOR REAL!!!");
  	  
    	}
    	else if(m.message.equals("GATHER") && ballot == m.ballot){
  		  this.states.add(m);
  		  
  		  if (states.size()>=N/2+1) { // collected a majority of responses
  			  // log.info("p"+this.id+": received a quorum of read responses ");
  			  int highestballot = 0;
  			  
  			  for (int x = 0; x< states.size(); x = x+1) {
  				  // To find the highest ballot and define the "newest" proposal
  				  int thisballot = ((GatherMsg)states.get(x)).estballot;
  				  if(thisballot > highestballot){
  					  proposal = ((GatherMsg)states.get(x)).estimate;
  					  // log.info("THE¡¡VALUE of PROPOSAL is " + proposal);
  					  highestballot = thisballot;
  				  }
  			  }
  			  
  			  states.clear();
  			  
  			  for(ActorRef actor : processes.references) {
  				  actor.tell(new ImposeMsg("IMPORT", ballot, proposal) ,getSelf()); // send the Impose request
  				  // log.info("Impose ballot " + ballot + " and proposal " + proposal+ " msg: p" + self().path().name() + " -> p" + actor.path().name());
  			  }
  		  }
  	  
  	  
    	}

    }
    
    private void respond(ImposeMsg m, ActorRef pj) {
    	if(isCrashed()) return;
    	if(readballot > m.ballot || imposeballot > m.ballot) {
        	// log.info("Abort : " + getSender().path().name() +" ballot "+ m.ballot +" " + getSelf().path().name()+ " readballot " + readballot + " imposeballot " + imposeballot);
  			pj.tell(new RespondMsg("ABORT", m.ballot), getSelf());
    	}
    	else {
  			this.estimate = m.proposal;
  			this.imposeballot = m.ballot;
  			pj.tell(new RespondMsg("ACK", m.ballot),getSelf());
  		}
    }
    
    
    private void decide(RespondMsg m) {
    	if(isCrashed()) return;
    	
    	if(m.info.equals("ABORT")) {
    		// return;
    		if(this.state <= 2 && ballot == m.ballot)
    			this.propose(proposal);  // RE-propose once it receive a ABORT
//    		else
//    			log.info("FOR REAL???");
    	}
    	else if(ballot == m.ballot) {
    		++numACK;
    		if(numACK >= N/2+1) {
    			for(ActorRef actor : processes.references) {
    	  			actor.tell(new DecideMsg("DECIDE",proposal),getSelf());
    	      	}
    	  		numACK = 0;
    	  	 }
    		
    	}
    	
    }
    
    public void onReceive(Object message) throws Throwable {
    	
    	if (message instanceof State) { 
    		this.state = ((State)message).state;
        }else if (message instanceof Members) {//save the system's info
              Members m = (Members) message;
              processes = m;
              // this.id = Integer.parseInt(self().path().name());
              // log.info("p" + self().path().name() + " received processes info and start to propose message");
              if(isCrashed()) return;
        }else if(message instanceof LaunchMsg && this.state <=2) {
        	if(isCrashed()) return;
        	this.propose(random.nextInt(2));
        }else if(message instanceof HoldMsg && this.state <=3) {
        	HoldMsg m = (HoldMsg)message;
        	if(isCrashed()) return;
        	this.state = m.state;
        	
        }else if (message instanceof ReadMsg && this.state <= 3) {
         
        	ReadMsg m = (ReadMsg) message;
            
        	this.gather(m.ballot, getSender());
          
        }else if (message instanceof GatherMsg && this.state <= 3) {
        	  GatherMsg m = (GatherMsg) message;
        	  // log.info("Gather information is [" + m.message + " , " + m.ballot + " , " +  m.estballot + " , " +  m.estimate +"] from p" + getSender().path().name());
        	  this.impose(m);
          }
          else if(message instanceof ImposeMsg && this.state <= 3 ) {
        	  ImposeMsg m = (ImposeMsg) message;
        	  this.respond(m,getSender());
          }
          else if(message instanceof RespondMsg && this.state <= 3) {
        	  RespondMsg m = (RespondMsg) message;
        	  // send DECIDE v to all
    		  // log.info("DONE:¡¡p" + getSelf().path().name() + " decides proposal : " + proposal);

        	  this.decide(m);
          }
          else if(message instanceof DecideMsg && ((DecideMsg) message).decideInfo.equals("DECIDE") && this.state <= 3) {
        	  if(isCrashed()) return;
        	  DecideMsg m = (DecideMsg)message;
        	  proposal = m.proposal;
        	  if(this.state != 5) {
        		  log.info("DONE:¡¡p" + getSelf().path().name() + " decides proposal : " + m.proposal);
        	  }
        	  this.state = 5;

          }else {
        	  return ;
          }
}
}
