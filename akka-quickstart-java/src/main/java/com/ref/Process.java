package com.ref;

import java.util.Random;

import akka.actor.Props;
import akka.event.Logging;
import akka.actor.ActorRef;
import akka.actor.AbstractActor;
import akka.event.LoggingAdapter;

public class Process extends AbstractActor {

    int ballot;
    public int id;
    int numGathered;
    int N = Paxos.N;
    ActorRef[] refs;
    int imposeballot;
    int proposalValue;
    int readballot = 0;
    int numAcknowledged;
    double crashProb = .5;
    Integer estimate = null;
    boolean isSilent = false;
    boolean isDecided = false;
    boolean isHolding = false;
    boolean isProposing = false;
    boolean isFaultProne = false;
    static Random random = new Random();
    Integer[][] states = new Integer[N][2];
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public Process() {
        id = Integer.parseInt(getSelf().path().name().substring(1));
        ballot = id - N;
        imposeballot = id - N;
        initStates();
    }

    public static Props createActor() {
        return Props.create(Process.class, Process::new);
    }

    int getSenderId() {
        return Integer.parseInt(getSender().path().name().substring(1));
    }

    void initStates() {
        for (int i = 0; i < N; ++i) {
            states[i][0] = null;
            states[i][1] = 0;
        }
    }

    void tryCrash() {
        if (random.nextDouble() <= crashProb) {
            isSilent = true;
            log.info("process " + id + " has crashed!");
        }
    }

    boolean fault() {
        if (!isSilent && isFaultProne) {
            tryCrash();
        }
        return isSilent;
    }

    public void propose(int v) {
        numGathered = 0;
        isProposing = true;
        proposalValue = v;
        ballot = ballot + N;
        initStates();
        log.info("process " + id + " proposes " + v + " with ballot " + ballot);
        for (ActorRef ref : refs) {
            ref.tell(new Messages.ReadMessage(ballot), getSelf());
        }
    }

    void onRead(Messages.ReadMessage m) {
        if (fault()) return;
        if (readballot > m.ballot || imposeballot > m.ballot) {
            getSender().tell(new Messages.AbortMessage(m.ballot), getSelf());
        } else {
            readballot = m.ballot;
            getSender().tell(new Messages.GatherMessage(m.ballot, estimate, imposeballot), getSelf());
        }
    }

    void onAbort(Messages.AbortMessage m) {
        if (fault()) return;
        if (ballot == m.ballot) {
            isProposing = false;
            if (!isDecided && !isHolding) {
                propose(proposalValue);
            }
        }
    }

    void onGather(Messages.GatherMessage m) {
        if (fault()) return;
        if (isProposing && ballot == m.ballot) {
            ++numGathered;
            int id = getSenderId();
            states[id][0] = m.est;
            states[id][1] = m.estballot;

            if (numGathered > N / 2) {
                int maxId = 0;
                for (int i = 0; i < N; ++i) {
                    if (states[maxId][1] < states[i][1]) {
                        maxId = i;
                    }
                }
                if (states[maxId][1] > 0) {
                    proposalValue = states[maxId][0];
                }
                numGathered = 0;
                numAcknowledged = 0;
                for (ActorRef ref : refs) {
                    ref.tell(new Messages.ImposeMessage(ballot, proposalValue), getSelf());
                }
            }
        }
    }

    void onImpose(Messages.ImposeMessage m) {
        if (fault()) return;
        if (readballot > m.ballot || imposeballot > m.ballot) {
            getSender().tell(new Messages.AbortMessage(m.ballot), getSelf());
        } else {
            estimate = m.value;
            imposeballot = m.ballot;
            getSender().tell(new Messages.AckMessage(m.ballot), getSelf());
        }
    }

    void onAck(Messages.AckMessage m) {
        if (fault()) return;
        if (isProposing && ballot == m.ballot) {
            ++numAcknowledged;
            if (numAcknowledged > N / 2) {
                for (ActorRef ref : refs) {
                    ref.tell(new Messages.DecideMessage(proposalValue), getSelf());
                }
                numAcknowledged = 0;
            }
        }
    }

    void onDecide(Messages.DecideMessage m) {
        if (fault()) return;
        if (!isDecided) {
            log.info(id + " decided " + m.value);
            isProposing = false;
            isDecided = true;
            for (ActorRef ref : refs) {
                ref.tell(new Messages.DecideMessage(m.value), getSelf());
            }
        }
    }

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(Messages.AckMessage.class, this::onAck)
                .match(Messages.ReadMessage.class, this::onRead)
                .match(Messages.AbortMessage.class, this::onAbort)
                .match(Messages.DecideMessage.class, this::onDecide)
                .match(Messages.GatherMessage.class, this::onGather)
                .match(Messages.ImposeMessage.class, this::onImpose)
                .match(Messages.HoldMessage.class, m -> isHolding = true)
                .match(Messages.CrashMessage.class, m -> isFaultProne = true)
                .match(Messages.ReferencesMessage.class, m -> this.refs = m.refs)
                .match(Messages.LaunchMessage.class, m -> propose(random.nextInt(2)))
                .build();
    }
}
