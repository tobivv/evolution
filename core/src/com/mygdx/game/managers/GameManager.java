package com.mygdx.game.managers;

import com.mygdx.game.logic.Client;
import components.objects.Player;
import components.enums.*;
import static components.fun.*;

import java.util.Vector;

public class GameManager {
    boolean clientConnected = false;
    Client c;
    public Player player = new Player();
    public Vector<Player> otherPlayers = new Vector<Player>();
    public boolean turnStart=true;

    public GameState state = GameState.BEGIN;
    public Command command = Command.NONE;
    // zeby nie byl to null
    public int turn = -1;
    public int amountOfFood;

    public void startClient() {
        if (!clientConnected) {
            clientConnected = true;
            c = new Client("localhost", 5055, this);
        }
    }

    public void addAnimal(int place) {
        c.send(new byte[]{Command.ADD.getId(), (byte) place});
    }

    public void addFeature(int place, Card cardName) {
        c.send(new byte[]{Command.EVOLUTION.getId(), (byte) place, (byte) cardName.getId()});
    }

    public void kill(int player, int place){
        c.send(new byte[]{Command.KILL.getId(),(byte) player, (byte) place});
    }

    public void pass() {
        c.send(new byte[]{Command.PASS.getId()});
    }

    public void endRound() {
        c.send(new byte[]{Command.ENDROUND.getId()});
    }

    public void feed(int animal, int amount) {
        c.send(new byte[]{Command.FEED.getId(), (byte) animal, (byte) amount, (byte) amountOfFood});
    }

    public void handleData(byte[] recv) {
        // jesli chcesz sprawdzic caly napis uzywasz recvData
        // jesli jego czesc uzywasz recv

        command = Command.fromInt((int) recv[0]);
        System.out.println(command);
        if (command == Command.GETNAME) {
            c.send(concat(new byte[]{Command.NAME.getId()}, new String ("TOBI").getBytes()));
        }
        else if (command == Command.ID) {
            player.number = recv[1];
        }
        else if (command == Command.NAME) {
            if (recv[1] == player.number) {
                player.name = stringFromBytes(recv, 2, recv.length - 2);
            } else {
                otherPlayers.addElement(new Player(recv[1], stringFromBytes(recv, 2, recv.length - 2), 6));
            }
        }
        else if (command == Command.CARDS) {
            if (recv[1] == player.number) {

                for (int i = 2; i < recv.length; i++) {
                    player.addCard(Card.fromInt(recv[i]));
                }
                player.numberOfCards += recv.length - 1;
            }
            else {
                for (Player player : otherPlayers) {
                    if (player.number == recv[1]) {
                        player.numberOfCards += recv[2];
                    }
                }
            }
        }
        else if (command == Command.FOOD){
            amountOfFood=recv[1];
        }
        else if (command == Command.STATE) {
            if( recv[1] == GameState.ERROR.getId()){
                c.send(new byte[] {Command.GETGAME.getId()});
            }
            else {
                state = GameState.fromInt(recv[1]);
            }
        }
        // ustawia czyja tura
        else if (command == Command.TURN) {
            turn = recv[1];
            turnStart=true;
        }
        else if (command == Command.ADD) {
            if (recv[2] != player.number) {
                for (Player otherPlayer : otherPlayers) {
                    if (otherPlayer.number == recv[2]) {
                        // bo dodal zwierze
                        otherPlayer.numberOfCards -= 1;
                        // dodajemy zwierze
                        otherPlayer.addAnimal(recv[1]);
                    }
                }
            }
        }
        else if (command == Command.KILL) {
            if (recv[3] != player.number) {
                if (player.number == recv[1]) {
                    // ubijamy
                    player.killAnimal(recv[2]);
                }
            }
        }
        else if (command == Command.FEED) {
            if (recv[4] != player.number) {
                for (Player otherPlayer : otherPlayers) {
                    if (otherPlayer.number == recv[4]) {
                        // karmimy
                        otherPlayer.animals[recv[1]].feed(recv[2]);
                        amountOfFood=recv[3];
                    }
                }
            }
        }
        else if (command == Command.EVOLUTION) {
            if (recv[1] != player.number) {
                for (Player player : otherPlayers) {
                    if (player.number == recv[1])

                        // bo dodal zwierze
                        player.numberOfCards -= 1;
                    // dodajemy ceche
                    player.animals[recv[2]].addFeature(Card.fromInt(recv[3]));
                }
            }

        }
    }
}

