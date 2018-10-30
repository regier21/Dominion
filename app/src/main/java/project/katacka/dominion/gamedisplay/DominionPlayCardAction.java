package project.katacka.dominion.gamedisplay;

import project.katacka.dominion.gameframework.GamePlayer;
import project.katacka.dominion.gameframework.actionMsg.GameAction;

public class DominionPlayCardAction extends GameAction {
    int cardIndex;
    /**
     * constructor for DominionPlayCardAction
     *
     * @param player
     * 		the player who created the action
     */
    public DominionPlayCardAction(GamePlayer player) {
        super(player);
        this.cardIndex = cardIndex;
    }

}
