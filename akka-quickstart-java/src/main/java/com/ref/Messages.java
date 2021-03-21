package com.ref;

import akka.actor.ActorRef;

public class Messages {

    public static class HoldMessage {
    }

    public static class CrashMessage {
    }

    public static class LaunchMessage {
    }

    public static class ReadMessage {
        int ballot;
        ReadMessage(int ballot) {
            this.ballot = ballot;
        }
    }

    public static class AckMessage {
        int ballot;
        AckMessage(int ballot) {
            this.ballot = ballot;
        }
    }

    public static class DecideMessage {
        int value;
        DecideMessage(int value) {
            this.value = value;
        }
    }

    public static class AbortMessage {
        int ballot;
        AbortMessage(int ballot) {
            this.ballot = ballot;
        }
    }

    public static class ImposeMessage {
        int ballot;
        int value;
        ImposeMessage(int ballot, int value) {
            this.ballot = ballot;
            this.value = value;
        }
    }

    public static class GatherMessage {
        int ballot;
        Integer est;
        int estballot;
        GatherMessage(int ballot, Integer est, int estballot) {
            this.ballot = ballot;
            this.est = est;
            this.estballot = estballot;
        }
    }

    public static class ReferencesMessage {
        ActorRef[] refs;
        ReferencesMessage(ActorRef[] refs) {
            this.refs = refs;
        }
    }

}
