package project.katacka.dominion.gamestate;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * A data class intended to represent the state of a player object
 * @author Ryan Regier, Julian Donovan, Ashika Mulagada, Hayden Liao
 */
public class DominionPlayerState implements Serializable{

    //Player fields.
    protected final String name;
    protected final DominionDeckState deck;
    private int turnsPlayed; //Used to break ties.

    public int getTurnsPlayed() {
        return turnsPlayed;
    }

    public void startTurn(){
        turnsPlayed++;
    }

    /**
     * Constructor.
     * @param name The player's name
     * @param copperPile The pile where copper is stored. Used to create starting deck.
     * @param estate The estate card. Used to create starting deck.
     */
    public DominionPlayerState(String name, DominionShopPileState copperPile, DominionCardState estate) {
        this.name = name;

        //Initializes player deck
        this.deck = new DominionDeckState();
        populateStartingDeck(copperPile, estate);

        deck.drawMultiple(5);

        turnsPlayed = 0;
    }

    /**
     * Copy constructor.
     * Obfuscates if requested.
     * @param playerState The player to copy
     * @param isThisPlayer Whether or not to obfuscate
     */
    public DominionPlayerState(DominionPlayerState playerState, boolean isThisPlayer){
        this.name = playerState.name;
        this.deck = new DominionDeckState(playerState.deck, isThisPlayer);
        this.turnsPlayed = playerState.turnsPlayed;
    }

    /**
     * Populates deck's discard member variable with 7 copper and 3 estates for start of game.
     * Removes copper from the pile used.
     * Does not remove Estates.
     *
     * @param copper A DominionCardState object containing data pertaining to the copper card
     * @param estate A DominionCardState object containing data pertaining to the estate card
     */
    public void populateStartingDeck(DominionShopPileState copper, DominionCardState estate) {
        deck.addManyToDiscard(copper.getCard(), 7);
        copper.removeAmount(7); //Removes 7 copper from the base card's draw pile
        deck.addManyToDiscard(estate, 3);
    }

    public DominionDeckState getDeck() {
        return deck;
    }

    public String getName() { return name; }

    /**
     * Overrides the default inherited toString() behavior, properly displaying object data
     * @return A String containing object type, name, deck and hand info
     */
    @Override
    public String toString(){
        return String.format("Player: %s\n%s", name, deck.toString());
    }
}
