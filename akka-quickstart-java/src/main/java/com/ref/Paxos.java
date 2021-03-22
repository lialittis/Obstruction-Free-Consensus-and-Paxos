package com.ref;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Paxos {
    public static int f = 1;
    public static int N = 5;
    public static double t_le = 2;

    public static void main(String[] args) throws InterruptedException {
        final ActorSystem system = ActorSystem.create("system");

        ActorRef[] processes = new ActorRef[N];
        for (int i = 0; i < N; ++i) {
            processes[i] = system.actorOf(Process.createActor(), "p" + i);
        }
        for (int i = 0; i < N; ++i) {
            processes[i].tell(new Messages.ReferencesMessage(processes), ActorRef.noSender());
        }
        for (int i = 0; i < N; ++i) {
            processes[i].tell(new Messages.LaunchMessage(), ActorRef.noSender());
        }
        List<ActorRef> list = Arrays.asList(processes);
		Collections.shuffle(list);
		list.toArray(processes);
        for (int i = 1; i <= f; ++i) {
            processes[i].tell(new Messages.CrashMessage(), ActorRef.noSender());
        }
        Thread.sleep((long) t_le * 1000);
        for (int i = 1; i < N; ++i) {
            processes[i].tell(new Messages.HoldMessage(), ActorRef.noSender());
        }

        try {
            waitBeforeTerminate();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            system.terminate();
        }
    }

    public static void waitBeforeTerminate() throws InterruptedException {
        Thread.sleep(5000);
    }
}
