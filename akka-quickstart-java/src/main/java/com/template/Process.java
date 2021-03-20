package com.template;

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

import com.system_creation.ProcessBC.QuorumRequest;
import com.system_creation.ProcessBC.ReadResponse;
import com.system_creation.ProcessBC.StampedValue;

public class Process extends UntypedAbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);// Logger attached to actor
    private final int N; //number of processes
    private final int id; //id of current process
    private Members processes; //other processes' references
    private Integer proposal;
    
    private int ballot;
    private int readballot;
    private int imposeballot;
    private Integer estimate;
    
	// local message/state buffer (to store states received in a Quorum or read phase)
	private ArrayList states = new ArrayList();
    
    public Process(int ID, int nb) {
        N = nb;
        id = ID;
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
    
    
    private void ofconsProposeReceived(Integer v) {

        proposal = v;
        log.info("Proposal is " + v);
        ballot = ballot;
        for (ActorRef actor : processes.references) {
            actor.tell(new ReadMsg(ballot), this.getSelf());
            log.info("Read ballot " + ballot + " msg: ps" + self().path().name() + " -> p" + actor.path().name());
        }
    }
    
    private void readReceived(int newBallot, ActorRef pj) {
            log.info("Read received " + self().path().name() + " from p" + pj.path().name());
            if (newBallot < readballot || imposeballot > newBallot) {
            	pj.tell(new GatherMsg(newBallot,"ABORT"),this.getSelf());
            }
            else {
            	readballot = newBallot ;
            	pj.tell(new GatherMsg(newBallot,"GATHER",imposeballot,estimate), this.getSelf());
            }
    }
    
    
    public void onReceive(Object message) throws Throwable {
          if (message instanceof Members) {//save the system's info
              Members m = (Members) message;
              processes = m;
              // this.id = Integer.parseInt(self().path().name());
              ballot = id - N;
              readballot = 0;
              imposeballot = id - N;
              estimate = 0;
              
              log.info("p" + self().path().name() + " received processes info");
          }
          else if (message instanceof OfconsProposerMsg) {
              OfconsProposerMsg m = (OfconsProposerMsg) message;
              log.info("Received proposal msg is " + m.v);
              this.ofconsProposeReceived(m.v);
      
          }
          else if (message instanceof ReadMsg) {
              ReadMsg m = (ReadMsg) message;
              this.readReceived(m.ballot, getSender());
          }
          else if (message instanceof GatherMsg) {
        	  GatherMsg m = (GatherMsg) message;
        	  log.info("Gahter information is [" + m.message + " , " + m.ballot + "] from p" + getSender().path().name());
        	  if(m.message.equals("ABORT")) {
        		  unhandled(m);
        	  } else{
        		  this.states.add(m);
        		  if (states.size()>=this.processes.references.size()/2+1) { // collected a majority of responses
        			  log.info("p"+this.id+": received a quorum of read responses ");
        			  for (int x = 0; x< states.size(); x = x+1) {
        				  int highestballot = 0;
        				  int thisballot = ((GatherMsg)states.get(x)).estballot;
        				  if(thisballot > highestballot){
        					  proposal = ((GatherMsg)states.get(x)).estimate;
        					  highestballot = thisballot;
        				  }
        			  }
        				  
        			  states.clear();
        				
        			  for(int x = 0; x < this.processes.references.size(); x = x + 1) {
        				  processes.references.get(x).tell(new ImposeMsg("IMPORT", ballot, proposal) ,getSelf()); // send the Impose request
        			  }
        		  }
        	  
        	  }
          }
          else if(message instanceof ImposeMsg) {
        	  ImposeMsg m = (ImposeMsg) message;
        	  ActorRef pj = getSender();
        	  if(readballot > m.ballot || imposeballot > m.ballot) {
        		  pj.tell(new ResposeMsg("ABORT", m.ballot), getSelf());
        	  }
        	  else {
        		  this.estimate = m.proposal;
        		  this.imposeballot = m.ballot;
        		  pj.tell(new ResposeMsg("ACK", m.ballot),getSelf());
        	  }
          }
          else if(message instanceof ResposeMsg) {
        	  ResposeMsg m = (ResposeMsg) message;
        	  this.states.add(m);
        	  if(states.size()>this.processes.references.size()/2+1) {
        		  for(int x = 0; x < this.processes.references.size();x= x+1) {
            		  if(m.info.equals("ACK")) {
            			  this.processes.references.get(x).tell(new DecideMsg("DECIDE",proposal),getSelf());
            		  }
            	  }
        	  }
          }
          else if(message instanceof DecideMsg && ((DecideMsg) message).decideInfo.equals("DECIDE")) {
        	  // send DECIDE v to all ???
        	  DecideMsg m = (DecideMsg)message;
        	  log.info("DONE " + + this.id + " complete decide proposal " + m.proposal);
          }
}
}
