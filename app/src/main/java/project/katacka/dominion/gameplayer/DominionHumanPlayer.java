package project.katacka.dominion.gameplayer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

import project.katacka.dominion.R;
import project.katacka.dominion.gamedisplay.DominionBuyCardAction;
import project.katacka.dominion.gamedisplay.DominionBuyCardInfo;
import project.katacka.dominion.gamedisplay.DominionEndTurnAction;
import project.katacka.dominion.gamedisplay.DominionPlayAllAction;
import project.katacka.dominion.gamedisplay.DominionPlayCardAction;
import project.katacka.dominion.gameframework.GameHumanPlayer;
import project.katacka.dominion.gameframework.GameMainActivity;
import project.katacka.dominion.gameframework.GamePlayer;
import project.katacka.dominion.gameframework.actionMsg.GameAction;
import project.katacka.dominion.gameframework.infoMsg.GameInfo;
import project.katacka.dominion.gameframework.infoMsg.IllegalMoveInfo;
import project.katacka.dominion.gamestate.DominionCardPlace;
import project.katacka.dominion.gamestate.DominionCardState;
import project.katacka.dominion.gameframework.infoMsg.NotYourTurnInfo;
import project.katacka.dominion.gamestate.DominionCardType;
import project.katacka.dominion.gamestate.DominionDeckState;
import project.katacka.dominion.gamestate.DominionGameState;
import project.katacka.dominion.gamestate.DominionPlayerState;
import project.katacka.dominion.gamestate.DominionShopPileState;

/**
 * @author Hayden Liao, Ashika Mulagada, Ryan Regier, Julian Donovan
 * contains all GUI code for Dominion human player interface
 * has shop, player hand, draw and discard piles, opponent hand,
 * menu button, turn tabs, current turn game stats and play all feature
 *
 * Suggested improvements, include inPlay view, have startup menu to select other card combinations
 */
public class DominionHumanPlayer extends GameHumanPlayer {

    private final int ILLEGAL_TOAST_DURATION = 250;
    private final double CARD_WIDTH_RATIO = 0.66;

    private final float TAB_INACTIVE = 0.85f;
    private final float TAB_ACTIVE = 1f;

    private final int EMPTY_PILE = Color.DKGRAY;
    private final int BOUGHT_PILE = 0xFF80dfff;

    private DominionGameState state;
    private ConstraintLayout tabLayout = null;

    private TableLayout shopLayout = null;
    private ArrayList<ConstraintLayout> shopPiles;

    private TableLayout baseLayout = null;
    private ArrayList<ConstraintLayout> basePiles;

    private LinearLayout cardRow = null;
    private HorizontalScrollView cardScroll = null;
    private ArrayList<DominionCardState> hand;

    private final int MAX_HAND_SIZE = 5;
    private int handCardWidth = 0;

    private int pos;

    private ConstraintLayout mainLayout;
    private LayoutInflater inflater;

    private Resources res;

    private Button bEndTurn = null;
    private Button bPlayAll = null;

    private GameMainActivity activity = null;

    private TextView tvActions;
    private TextView tvBuys;
    private TextView tvTreasure;

    private TextView tvOppDraw;
    private ImageView oppDraw;
    private ImageView oppDrawEmpty;
    private TextView tvOppDiscard;
    private ConstraintLayout oppDiscardLayout;
    private ImageView oppEmptyDiscard;

    private TextView tvDrawCount;
    private TextView tvDiscardCount;

    private TextView bMenu;

    private int promptEndTurn = 1;

    private ImageView drawPile;
    private ConstraintLayout discardPile;
    private ImageView emptyDiscardPile;
    private ImageView emptyDrawPile;

    private DominionPlayerState playerState;

    private final GamePlayer thisPlayer = this; //cannot just use "this" in listeners
        // b/c "this" references the listener class, not DominionHumanPlayer

    private final Handler myHandler;
    private Drawable background;
    private Toast illegalMoveToast;

    private boolean isTurn;

    ArrayList<Integer> imageList;
    ImageView imageHelp;
    AlertDialog dialog;

    public DominionHumanPlayer(String name) {
        super(name);
        myHandler = new Handler();
    }

    public String toString(){
        return "CardView Name: " + super.name;
    }

    @Override
    public void setAsGui(GameMainActivity activity) {
        //remember activity
        this.activity = activity;

        //set display based XML resource
        activity.setContentView(R.layout.activity_main);

        //making array list of tablerows for shop and base cards
        shopLayout = activity.findViewById(R.id.Shop_Cards);
        baseLayout = activity.findViewById(R.id.Base_Cards);

        ///////////////////
        //Save views
        /////////////////

        tabLayout = activity.findViewById(R.id.Player_Tabs);
        bEndTurn = activity.findViewById(R.id.buttonEndTurn);
        bPlayAll = activity.findViewById(R.id.buttonPlayAll);

        //Text views showing actions, buys, and treasures
        tvActions = activity.findViewById(R.id.tvActions);
        tvBuys = activity.findViewById(R.id.tvBuys);
        tvTreasure = activity.findViewById(R.id.tvTreasures);

        //Set the default values
        updateTurnInfo(0, 0, 0);

        //Player's hand
        cardRow = activity.findViewById(R.id.User_Cards);
        cardScroll = activity.findViewById(R.id.User_Cards_Scroll);

        //Player's draw and discard piles
        tvDrawCount = activity.findViewById(R.id.textViewDrawCount);
        tvDiscardCount = activity.findViewById(R.id.textViewDiscardCount);
        tvDrawCount.setText("0");
        tvDiscardCount.setText("0");

        drawPile = activity.findViewById(R.id.ivDrawCard);
        discardPile = activity.findViewById(R.id.imageViewDiscard);
        emptyDiscardPile = activity.findViewById(R.id.imageViewDiscardEmpty);
        emptyDrawPile = activity.findViewById(R.id.imageViewDrawEmpty);

        //Opponent draw and discard piles
        tvOppDraw = activity.findViewById(R.id.textViewOppDraw);
        oppDraw = activity.findViewById(R.id.ivOppDrawCard);
        oppDrawEmpty = activity.findViewById(R.id.oppDrawEmpty);
        tvOppDiscard = activity.findViewById(R.id.textViewOppDiscard);
        oppDiscardLayout = activity.findViewById(R.id.oppDiscardCard);
        oppDiscardLayout.setRotation(180);
        oppEmptyDiscard = activity.findViewById(R.id.oppDiscardEmpty);
        tvOppDraw.setText("5");
        tvOppDiscard.setText("0");

        //Main view
        mainLayout = activity.findViewById(R.id.constraintMain);
        background = mainLayout.getBackground(); //Used for flashing

        //Menu
        bMenu = activity.findViewById(R.id.bMenu);

        //Resources.
        //Used to load card images
        res = activity.getResources();
        inflater = activity.getLayoutInflater();

        setShopArray();
        setBaseArray();
    }

    public void setShopArray() {
        shopPiles = new ArrayList<>();
        for (int i = 0, j = shopLayout.getChildCount(); i < j; i++) {
            View shopRow = shopLayout.getChildAt(i);

            //should always be true
            if (shopRow instanceof TableRow) {

                //cards are ConstraintLayouts in XML
                for (int k = 0; k < 5; k++) {
                    ConstraintLayout shopCard = ((ConstraintLayout) ((TableRow) shopRow).getVirtualChildAt(k));
                    shopPiles.add(shopCard);
                    shopCard.setOnClickListener(shopClickListener);
                    shopCard.setOnLongClickListener(shopLongClickListener);
                }
            }
        }
    }

    public void setBaseArray() {
        basePiles = new ArrayList<>();
        for (int i = 0, j = baseLayout.getChildCount(); i < j; i++) {
            View baseRow = baseLayout.getChildAt(i);

            //should always be true
            if (baseRow instanceof TableRow) {

                //cards are ConstraintLayouts in XML
                for (int k = 0; k < 2; k++) {
                    ConstraintLayout baseCard = ((ConstraintLayout) ((TableRow) baseRow).getVirtualChildAt(k));
                    basePiles.add(baseCard);
                    baseCard.setOnClickListener(shopClickListener);
                }
            }
        }
    }

    /**
     * perform any initialization that needs to be done after the player
     * knows what their game-position and opponents' names are.
     */
    protected void initAfterReady() {
        //Sets tab names
        for(int i = 0; i < tabLayout.getChildCount(); i++) {
            if (i < allPlayerNames.length) {
                ((TextView) tabLayout.getChildAt(i).findViewById(R.id.playerName)).setText(allPlayerNames[i]);
            }
            else tabLayout.getChildAt(i).setVisibility(View.GONE);
        }
    }

    @Override
    public View getTopView() {
        return mainLayout;
    }

    /**
     * sets tabs so the tab of the current player is 100% of the constraint width
     * other players are 85% of constraint width
     * @param activePlayer player to set active tab for
     *
     */
    private void updateTabs(int activePlayer){
        ConstraintSet c = new ConstraintSet();
        //clone Player_tabs (tabs wrapper) constraints
        c.clone(tabLayout);
        //set default individual tab widths as percentages of the parents constraints
        //by default, tab1 is active

        int[] playerTabs = {R.id.playerTab1, R.id.playerTab2, R.id.playerTab3, R.id.playerTab4};
        for(int i = 0; i < state.getNumPlayers(); i++){
            if(i == activePlayer){
                c.constrainPercentWidth(playerTabs[i], TAB_ACTIVE);
            } else {
                c.constrainPercentWidth(playerTabs[i], TAB_INACTIVE);
            }
        }

        c.applyTo(tabLayout);
    }

    /**
     * Updates textViews that provide player info
     * @param actions The current number of player's actions
     * @param buys The current number of player's buys
     * @param treasure The current number of player's treasures
     */
    private void updateTurnInfo(int actions, int buys, int treasure){
        /**
         * External Citation
         * Date: 11/6/18
         * Problem: Needed to combine text and number in text view
         * Resource:
         *  https://developer.android.com/guide/topics/resources/string-resource#java
         * Solution: Set up XML strings properly to allow format parameters when read
         */
        tvActions.setText(activity.getString(R.string.actions, actions));
        tvBuys.setText(activity.getString(R.string.buys, buys));
        tvTreasure.setText(activity.getString(R.string.treasure, treasure));
    }

    /**
     * Updates player's draw and discard piles to be an empty pile or display top most card
     */
    private void updateDrawDiscard(){
        DominionDeckState deck = playerState.getDeck();
        int drawSize = deck.getDrawSize();
        int discardSize = deck.getDiscardSize();

        tvDrawCount.setText(Integer.toString(drawSize));
        tvDiscardCount.setText(Integer.toString(discardSize));

        if(drawSize == 0){
            drawPile.setVisibility(View.INVISIBLE);
            emptyDrawPile.setVisibility(View.VISIBLE);

        } else {
            drawPile.setVisibility(View.VISIBLE);
            emptyDrawPile.setVisibility(View.INVISIBLE);
        }
        if(discardSize == 0){
            discardPile.setVisibility(View.INVISIBLE);
            emptyDiscardPile.setVisibility(View.VISIBLE);
        } else {
            discardPile.setVisibility(View.VISIBLE);
            emptyDiscardPile.setVisibility(View.INVISIBLE);
            updateCardView(discardPile, playerState.getDeck().getLastDiscard(), -1);
        }
    }

    /**
     * Updates player's hand to reflect state
     */
    private void updatePlayerHand(){
        cardRow.removeAllViews();
        hand = state.getDominionPlayer(playerNum).getDeck().getHand();
        if (handCardWidth == 0) setHandCardWidth();

        for(int i = 0; i < hand.size(); i++){
            ConstraintLayout cardLayout = (ConstraintLayout) inflater.inflate(R.layout.player_card, cardRow, false);
            cardLayout.setMinWidth(handCardWidth);
            cardLayout.setOnClickListener(handClickListener);

            DominionCardState card = hand.get(i);
            updateCardView(cardLayout, card, -1);
            setHighlight(cardLayout, isTurn && (card.getType() != DominionCardType.ACTION || state.getActions() > 0));
            cardRow.addView(cardLayout);
        }
    }

    /**
     * Adjusts hand card size for number of cards in hand
     */
    private void setHandCardWidth() {
        float d = res.getDisplayMetrics().density;
        float childComp = 8 * MAX_HAND_SIZE * d;
        handCardWidth = Math.round(cardScroll.getWidth() - childComp)/MAX_HAND_SIZE;
    }

    /**
     * Draws the card at the given view
     * @param cardView The view to draw the card
     * @param card The card to display
     * @param num The amount of cards. If -1, amount is hidden.
     */
    private void updateCardView(ConstraintLayout cardView, DominionCardState card, int num){
        TextView cost = cardView.findViewById(R.id.textViewCost);
        cost.setText(Integer.toString(card.getCost()));

        TextView title = cardView.findViewById(R.id.textViewTitle);
        title.setText(card.getTitle());

        FrameLayout layout = cardView.findViewById(R.id.frameLayoutAmount);
        if (num == -1){
            layout.setVisibility(View.INVISIBLE);
        } else {
            layout.setVisibility(View.VISIBLE);
            TextView amount = cardView.findViewById(R.id.textViewAmount);
            amount.setText(Integer.toString(num));
        }

        TextView description = cardView.findViewById(R.id.tvDescription);
        description.setText(card.getFormattedText());

        TextView type = cardView.findViewById(R.id.textViewType);
        type.setText(card.getType().toString());

        ImageView image = cardView.findViewById(R.id.imageViewArt);

        String name = card.getPhotoId();
        int resID = res.getIdentifier(name, "drawable", "project.katacka.dominion_card_back");
        image.setImageResource(resID);
        /*
         * External Citation
         * Date: 11/5/18
         * Problem: setting imageview using string
         * Source: https://stackoverflow.com/questions/5254100/how-to-set-an-imageviews-image-from-a-string
         * Solution: shows how to convert string to resource id to use to set image view
         */
    }

    /**
     * Updates the shop piles by calling update card view with info from gamestate
     */
    private void updateShopPiles(){

        for(int i = 0; i<shopPiles.size(); i++){
            ConstraintLayout cardLayout = shopPiles.get(i);
            DominionCardState card = state.getShopCards().get(i).getCard();
            int amount = state.getShopCards().get(i).getAmount();
            updateCardView(cardLayout, card, amount);
            setHighlight(cardLayout, canBuy(card, amount));
            if (amount == 0) setColorFilter(cardLayout, EMPTY_PILE);
        }

        /**
         External Citation
         Date: 11/1/18
         Problem: trying to iterate through table layout
         Source: https://stackoverflow.com/questions/3327599/get-all-tablerows-in-a-tablelayout
         Solution: using getChild and for each look to iterate through
         */

    }

    /**
     * Creates a gray filter that is added to the ImageViews of cards in the shop or base piles
     * @param shopCard Specifies the card being grayed out
     */
    private void setColorFilter(ConstraintLayout shopCard, int color) {
        /*
         * External Citation
         * Date: 11/18/18
         * Problem: Trying to use PorterDuffColorFilter
         * Source: https://developer.android.com/reference/android/graphics/PorterDuff.Mode
         * Solution: Used PorterDuff Multiply mode to make color filter
         */
        ColorFilter filter = new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY);
        ((ImageView) shopCard.findViewById(R.id.imageViewArt)).setColorFilter(filter);
        shopCard.getBackground().setColorFilter(filter);
        ((ImageView) shopCard.findViewById(R.id.imageViewCost)).setColorFilter(filter);
        ((ImageView) shopCard.findViewById(R.id.imageViewAmount)).setColorFilter(filter);
    }

    private void clearColorFilter(ConstraintLayout shopCard) {
        ((ImageView) shopCard.findViewById(R.id.imageViewArt)).clearColorFilter();
        shopCard.getBackground().clearColorFilter();
        ((ImageView) shopCard.findViewById(R.id.imageViewCost)).clearColorFilter();
        ((ImageView) shopCard.findViewById(R.id.imageViewAmount)).clearColorFilter();
    }

    /**
     * Sets a green border background for cards that are buyable/playable and a black border background for other cards.
     * @param shopCard Specifies the card being checked for if its buyable or not
     * @param canDo Specifies whether or not the card is buyable
     */
    private void setHighlight (ConstraintLayout shopCard, boolean canDo){
        if (canDo){
            shopCard.setBackgroundResource(R.drawable.dominion_card_border_green);
        } else {
            shopCard.setBackgroundResource(R.drawable.dominion_card_border_squared);
        }

        //Changing the background resets color filters, so we must reapply them.
        //The card art will always have the same filter as the background should, so we just apply its filter.
        shopCard.getBackground().setColorFilter(
                ((ImageView) shopCard.findViewById(R.id.imageViewArt)).getColorFilter());
    }

    private boolean canBuy(DominionCardState card, int amount){
        return isTurn && state.getBuys() > 0 && card.getCost() <= state.getTreasure() && amount > 0;
    }

    /**
     * Updates the base piles according to info from game state
     */
    private void updateBasePiles(){
        for(int i = 0; i<basePiles.size(); i++){
            ConstraintLayout cardLayout = basePiles.get(i);
            DominionCardState card = state.getBaseCards().get(i).getCard();
            int amount = state.getBaseCards().get(i).getAmount();
            updateCardView(cardLayout, card, amount);
            setHighlight(cardLayout, canBuy(card, amount));
            if (amount == 0) setColorFilter(cardLayout, EMPTY_PILE);
        }
    }

    /**
     * Updates opponents draw and discard piles to be an empty pile or display top most card
     */
    private void updateOppDrawDiscard(int player){
        if (player == playerNum) return;
        DominionDeckState currPlayerDeck = state.getDominionPlayer(player).getDeck();
        int drawSize = currPlayerDeck.getDrawSize();
        tvOppDraw.setText(Integer.toString(drawSize));
        if (drawSize > 0){
            oppDraw.setVisibility(View.VISIBLE);
            oppDrawEmpty.setVisibility(View.INVISIBLE);
        } else {
            oppDraw.setVisibility(View.INVISIBLE);
            oppDrawEmpty.setVisibility(View.VISIBLE);
        }
        int discardSize = currPlayerDeck.getDiscardSize();
        tvOppDiscard.setText(Integer.toString(discardSize));
        if (discardSize > 0) {
            updateCardView(oppDiscardLayout, currPlayerDeck.getLastDiscard(), -1);
            oppDiscardLayout.setVisibility(View.VISIBLE);
            oppEmptyDiscard.setVisibility(View.INVISIBLE);
        }
        else {
            oppDiscardLayout.setVisibility(View.INVISIBLE);
            oppEmptyDiscard.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Updates opponents hand to display the number of cards in their hand
     */
    private void updateOppHand(int player){
        //Finds how many cards to display
        int handSize;
        if (player == playerNum){
            handSize = 5;
        } else {
            handSize = state.getDominionPlayer(player).getDeck().getHandSize();
        }

        ConstraintLayout oppCardsLayout = activity.findViewById(R.id.Cards_Inplay);
        oppCardsLayout.removeAllViews();

        //Creates new image views and puts them in layout
        ImageView[] cards = new ImageView[handSize];
        for (int i = 0; i < handSize; i++){
            cards[i] = new ImageView(activity);
            cards[i].setScaleType(ImageView.ScaleType.FIT_START);
            cards[i].setImageResource(R.drawable.dominion_opponent_card_back);
            cards[i].setId(View.generateViewId()); //Needed to allow constraints
            oppCardsLayout.addView(cards[i]);
        }

        ConstraintSet set = new ConstraintSet();
        set.clone(oppCardsLayout);
        float biasMultiplier = Math.min(0.2f, 1/(float)handSize); //How far apart the cards should be, as a percentage
        @IdRes int layoutID = oppCardsLayout.getId();

        //Add constraints to every card image
        for (int i = 0; i < handSize; i++){
            ImageView card = cards[i];
            @IdRes int id = card.getId();

            //Constrain to all four edges of the layout
            set.connect(id, ConstraintSet.LEFT, layoutID, ConstraintSet.LEFT);
            set.connect(id, ConstraintSet.RIGHT, layoutID, ConstraintSet.RIGHT);
            set.connect(id, ConstraintSet.TOP, layoutID, ConstraintSet.TOP);
            set.connect(id, ConstraintSet.BOTTOM, layoutID, ConstraintSet.BOTTOM);

            //Have it fill the height it can
            set.constrainHeight(id, ConstraintSet.MATCH_CONSTRAINT);
            //Have it be wide enough to maintain aspect ration
            set.constrainWidth(id, ConstraintSet.WRAP_CONTENT);

            //Position the card in the correct position
            //This is the entire reason we use a constraint layout
            set.setHorizontalBias(id, i*biasMultiplier);
        }
        set.applyTo(oppCardsLayout);
    }

    /**
     * Updates opponents hand to display the number of cards in their hand
     */
    private void updateCardsPlayed(int player){
        //Finds how many cards to display
        int cardsPlayed;
        if (player == playerNum){
            cardsPlayed = 0;
        } else {
            cardsPlayed = state.getDominionPlayer(player).getDeck().getInPlaySize();
        }

        ConstraintLayout inPlayLayout = activity.findViewById(R.id.Cards_Inplay);
        inPlayLayout.removeAllViews();

        //Creates new image views and puts them in layout
        ImageView[] cards = new ImageView[cardsPlayed];
        for (int i = 0; i < cardsPlayed; i++){
            cards[i] = new ImageView(activity);
            cards[i].setScaleType(ImageView.ScaleType.FIT_START); //fits one axis, starting at top left
            //TODO: set image based on what cards was played
            cards[i].setImageResource(R.drawable.dominion_opponent_card_back);
            cards[i].setId(View.generateViewId()); //Needed to allow constraints
            inPlayLayout.addView(cards[i]);
        }

        ConstraintSet set = new ConstraintSet();
        set.clone(inPlayLayout);
        float biasMultiplier = Math.min(0.2f, 1/(float)cardsPlayed); //How far apart the cards should be, as a percentage
        @IdRes int layoutID = inPlayLayout.getId();

        //Add constraints to every card image
        for (int i = 0; i < cardsPlayed; i++){
            ImageView card = cards[i];
            @IdRes int id = card.getId();

            //Constrain to all four edges of the layout
            set.connect(id, ConstraintSet.LEFT, layoutID, ConstraintSet.LEFT);
            set.connect(id, ConstraintSet.RIGHT, layoutID, ConstraintSet.RIGHT);
            set.connect(id, ConstraintSet.TOP, layoutID, ConstraintSet.TOP);
            set.connect(id, ConstraintSet.BOTTOM, layoutID, ConstraintSet.BOTTOM);

            //Have it fill the height it can
            set.constrainHeight(id, ConstraintSet.MATCH_CONSTRAINT);
            //Have it be wide enough to maintain aspect ration
            set.constrainWidth(id, ConstraintSet.WRAP_CONTENT);

            //Position the card in the correct position
            //This is the entire reason we use a constraint layout
            set.setHorizontalBias(id, i*biasMultiplier);
        }
        set.applyTo(inPlayLayout);
    }

    /**
     * Prompts user for an alert dialog regarding ending their turn
     */
    private void promptEndTurn() {
        if (isTurn && (hand.size() == 0 || state.getActions() == 0) && state.getBuys() == 0) {
            if(promptEndTurn == 1) {
                AlertDialog.Builder endTurnPrompt = new AlertDialog.Builder(activity);
                endTurnPrompt.setMessage("End Turn?");

                endTurnPrompt.setPositiveButton(
                    "Yes",
                    (DialogInterface dialog, int id) -> {
                        endTurnMsg();
                        game.sendAction(new DominionEndTurnAction(thisPlayer));
                    }
                );

                endTurnPrompt.setNegativeButton(
                    "No",
                    (DialogInterface dialog, int id) -> dialog.dismiss()
                );

                CheckBox displayDialogCheck = new CheckBox(activity);

                endTurnPrompt.setView(displayDialogCheck);
                endTurnPrompt.create();
                final AlertDialog show = endTurnPrompt.show();

                displayDialogCheck.setText(R.string.display_dialog_check);
                displayDialogCheck.setOnClickListener((View v) -> {
                    if (displayDialogCheck.isChecked()) {
                        if (show != null) show.dismiss();
                        promptEndTurnSettings();
                    }
                    else promptEndTurn = 1;
                });
            }
            else if (promptEndTurn == -1) {
                endTurnMsg();
                game.sendAction(new DominionEndTurnAction(thisPlayer));
            }
        }
    }

    /**
     * Prompts user for an alert dialog regarding automated turn ending
     */
    private void promptEndTurnSettings() {
        AlertDialog.Builder endTurnSettings = new AlertDialog.Builder(activity);
        endTurnSettings.setMessage("Enable automatic turn ending?");

        endTurnSettings.setPositiveButton(
                "Yes",
                (DialogInterface dialog, int id) -> {
                    promptEndTurn = -1;
                    dialog.dismiss();
                    endTurnMsg();
                    game.sendAction(new DominionEndTurnAction(thisPlayer));
                }
        );

        endTurnSettings.setNegativeButton(
                "No",
                (DialogInterface dialog, int id) -> {
                    promptEndTurn = 0;
                    dialog.dismiss();
                    endTurnMsg();
                    game.sendAction(new DominionEndTurnAction(thisPlayer));
                }
        );

        endTurnSettings.create().show();
    }

    private void endTurnMsg() {
        Toast.makeText(activity, "Turn ended", Toast.LENGTH_SHORT).show();
    }

    /**
     * Receives game state info and calls update methods to reflect state, flashes red screen for illegal moves
     * @param info Game information
     */
    @Override
    public void receiveInfo(GameInfo info) {
        //get updated info
        if(info instanceof DominionGameState){
            state = (DominionGameState) info;
            playerState = state.getDominionPlayer(playerNum);
            isTurn = playerNum == state.getCurrentTurn();

            //Update tabs to reflect turn
            if (state.getIsAttackTurn()) {
                updateTabs(state.getCurrentTurn());
                updateOppDrawDiscard(state.getAttackTurn());
                updateOppHand(state.getAttackTurn());
            } else {
                updateTabs(state.getCurrentTurn());
                updateOppDrawDiscard(state.getCurrentTurn());
                updateOppHand(state.getCurrentTurn());
            }

            updateTurnInfo(state.getActions(), state.getBuys(), state.getTreasure());
            updateDrawDiscard();
            updateShopPiles();
            updateBasePiles();
            updatePlayerHand();

            //set listeners
            bEndTurn.setOnClickListener(handClickListener);
            bPlayAll.setOnClickListener(handClickListener);
            bMenu.setOnClickListener(menuClickListener);

            promptEndTurn();
        } else if(info instanceof NotYourTurnInfo) {
            Log.i("DominionHumanPlayer: receiveInfo", "Not your turn.");
            flash(Color.RED, ILLEGAL_TOAST_DURATION);
            if (illegalMoveToast != null){
                illegalMoveToast.cancel();
            }
            illegalMoveToast = Toast.makeText(activity, "Excuse you. It is not your turn.\n" +
                    "Please wait patiently like a well-mannered citizen.\n\n~~~~~~~~~~~~~~Thank you~~~~~~~~~~~~~~", Toast.LENGTH_SHORT);
            illegalMoveToast.show();

        } else if (info instanceof IllegalMoveInfo){
            flash(Color.RED, ILLEGAL_TOAST_DURATION);
            Log.i("HumanPlayer", "Illegal move");
            if (illegalMoveToast != null){
                illegalMoveToast.cancel();
            }
            illegalMoveToast = Toast.makeText(activity, "Illegal move", Toast.LENGTH_SHORT);
            illegalMoveToast.show();
        } else if (info instanceof DominionBuyCardInfo){
            DominionBuyCardInfo buyInfo = (DominionBuyCardInfo) info;

            //Get information needed to find the card layout
            int index = buyInfo.getCardIndex();
            DominionCardPlace place = buyInfo.getPlace();

            //Find the card layout
            ConstraintLayout cardView;
            if (place == DominionCardPlace.BASE_CARD){
                cardView = basePiles.get(index);
            } else if (place == DominionCardPlace.SHOP_CARD){
                cardView = shopPiles.get(index);
            } else return;

            //Flash the card layout
            setColorFilter(cardView, BOUGHT_PILE);
            myHandler.postDelayed(new ResetBackground(index, place), 500);
        }
    }

    @Override
    protected void gameIsOver(String message){
        super.gameIsOver(message);
        TextView tv = activity.findViewById(R.id.textViewGameOver);
        tv.setVisibility(View.VISIBLE);
        tv.setText(message);
    }

    /**
     * Handles playing all treasures, ending a turn, and playing cards in the hand
     */
    private final View.OnClickListener handClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v){
            if(v == null) { return; }

            GameAction action;
            if(v == bPlayAll){ //clicked the play all button
               Log.i("DomHumPlayer: HandClickListener onClick: ", "Play all button clicked");

                action = new DominionPlayAllAction(thisPlayer);
            } else if(v == bEndTurn) { //clicked the end turn button
                Log.i("TAG: ", "" + state.getCurrentTurn());
                Log.i("DomHumPlayer: onClick", "End turn button clicked.");
                endTurnMsg();

                action = new DominionEndTurnAction(thisPlayer);
            //} else if(v instanceof ConstraintLayout){ //v is one of the playerCards
            } else { //v is one of the playerCards
                Log.i("DomHumPlayer: onClick", "Player's card button clicked.");

                int index = cardRow.indexOfChild(v);
                action = new DominionPlayCardAction(thisPlayer, index);
            }
            game.sendAction(action);
        }
    };

    /**
     * Buys the card in shop tapped on
     */
    private final View.OnClickListener shopClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            DominionCardPlace place;
            boolean canBuy;

            TableRow parentView = (TableRow) v.getParent();

            //This is the table row the top row or bottom row
            TableLayout parentLayout = (TableLayout) parentView.getParent();
            int offSet = parentLayout.indexOfChild(parentView) * parentView.getVirtualChildCount();
            int rawIndex = parentView.indexOfChild(v);
            int desiredIndex = rawIndex + offSet;

            //TODO fix this shit
            if (basePiles.contains(v)) {
                place = DominionCardPlace.BASE_CARD;
            }
            else {
                place = DominionCardPlace.SHOP_CARD;
            }

            TextView cardTitle = parentView.getChildAt(rawIndex).findViewById(R.id.textViewTitle);
            if (state.isLegalBuy(playerNum, desiredIndex, place)) Toast.makeText(activity, "Bought a " + cardTitle.getText(), Toast.LENGTH_SHORT).show();

            game.sendAction(new DominionBuyCardAction(thisPlayer, desiredIndex, place));
        }
    };

    /**
     * Displays a help menu dialog with multiple images and buttons to navigate
     */
    private final View.OnClickListener menuClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            /*
             * External Citation:
             * Date: November 26, 2018
             * Resource:
             *  https://www.geeksforgeeks.org/arrays-aslist-method-in-java-with-examples/
             * Problem: Wanted to init Arraylist with vals  instead of adding one at a time.
             * Solution: use Arrays.asList in ArrayList constructor.
             */

            imageList = new ArrayList<Integer>
                    (Arrays.asList(R.drawable.rules_instructions,
                                    R.drawable.rules_play_card,
                                    R.drawable.rules_buy_card,
                                    R.drawable.rules_longpress,
                                    R.drawable.rules_end_turn,
                                    R.drawable.rules_swipe_cards,
                                    R.drawable.rules_switch));

            pos = 0;

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setPositiveButton("Next", null);
            builder.setNegativeButton("Previous", null);

            dialog = builder.create();
            LayoutInflater inflater = activity.getLayoutInflater();
            View dialogLayout = inflater.inflate(R.layout.dialog_help, null);
            dialog.setView(dialogLayout);
            imageHelp = dialogLayout.findViewById(R.id.image_help);

            imageHelp.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageHelp.setImageResource(imageList.get(pos)); //set dialog image to first image in array list

            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                 @Override
                 public void onShow(DialogInterface dialog) { //to make sure dialog doesn't close when a button is clicked
                     Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                     Button prevButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
                     button.setTextColor(Color.parseColor("#ff0000"));
                     prevButton.setTextColor(Color.parseColor("#d3d3d3"));
                     Log.i("Dominion Human Player", "on show");
                     button.setOnClickListener(helpClickListener);
                     prevButton.setOnClickListener(helpClickListener);
                 }
            });

            dialog.show();

                Window window = dialog.getWindow();
                double width = mainLayout.getWidth() * 0.75;
            window.setLayout((int)(width),(int)(width *0.71));

        }
    };

    private final View.OnClickListener helpClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(v.getId() == dialog.getButton(AlertDialog.BUTTON_POSITIVE).getId() ){
                Log.i("Dominion human player", "On click");
                Button nextButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                Button prevButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
                if(pos< (imageList.size()-1)){
                    pos++;
                }
                imageHelp.setScaleType(ImageView.ScaleType.FIT_CENTER); //setting image to next image in array list
                imageHelp.setImageResource(imageList.get(pos));
                if (pos == (imageList.size()-1)) nextButton.setTextColor(Color.parseColor("#d3d3d3"));
                else {
                    Log.i("On help click", "resetting to red");
                    prevButton.setTextColor(Color.parseColor("#ff0000"));
                    nextButton.setTextColor(Color.parseColor("#ff0000"));
                }
            }
            else if(v.getId() == dialog.getButton(AlertDialog.BUTTON_NEGATIVE).getId()){
                Button prevButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
                Button nextButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                if(pos > 0) {
                    pos--;
                }
                imageHelp.setScaleType(ImageView.ScaleType.FIT_CENTER); //setting image to previous image in array list
                imageHelp.setImageResource(imageList.get(pos));
                if (pos == 0) prevButton.setTextColor(Color.parseColor("#d3d3d3"));
                else {
                    prevButton.setTextColor(Color.parseColor("#ff0000"));
                    nextButton.setTextColor(Color.parseColor("#ff0000"));
                }
            }
        }
    };

    /**
     * Displays a dialog with card and description for the card in shop that is long pressed
     */
    private final View.OnLongClickListener shopLongClickListener = new View.OnLongClickListener(){
      @Override
      public boolean onLongClick(View v) {
          TableRow parentView = (TableRow) v.getParent();
          //is the table row the top row or bottom row
          TableLayout parentLayout = (TableLayout) parentView.getParent();
          int offSet = parentLayout.indexOfChild(parentView) * parentView.getVirtualChildCount();
          int desiredIndex = parentView.indexOfChild(v) + offSet;

          //get dominion shop pile state
          DominionShopPileState pileState = state.getShopCards().get(desiredIndex);

          final Dialog dialog = new Dialog(activity);
          dialog.setContentView(populateCardLayout(pileState));
          dialog.show();
          Window window = dialog.getWindow();
          double height = mainLayout.getHeight() * 0.50;
          window.setLayout((int) (height * CARD_WIDTH_RATIO), (int) height);

          return true;
      }
    };

    /**
     * Creates new cardView and populates it (for card description dialogue)
     * @param pile Shop pile that will be used for card description dialogue
     * @return cardView to be displayed
     */
    protected View populateCardLayout(DominionShopPileState pile){
        ConstraintLayout cardView = (ConstraintLayout) LayoutInflater.from(activity).inflate(R.layout.player_card, mainLayout, false);
        DominionCardState card = pile.getCard();
        updateCardView(cardView, card, pile.getAmount());
        return cardView;
    }

    /**
     * Flashes a background color
     * @param color the color to flash
     * @param duration how long to flash for
     */
    @Override
    protected void flash(int color, int duration) {
        View top = this.getTopView();
        if (top == null) return;

        //This part is different
        //Background is not saved - this is done when GUI is set
        //This prevents a race condition where the "flashed" background gets saved
        top.setBackgroundColor(color);
        Log.i("Human", "Starting flash");

        myHandler.postDelayed(new Unflasher(getTopView(), background), duration);
    }

    /**
    * helper-class to finish a "flash".
     * * Making our own so that a image background can be supported
    *
    */
    private class Unflasher implements Runnable {

        private final View view;
        private final Drawable background;

        // constructor
        public Unflasher(View view, Drawable background) {
            this.view = view;
            this.background = background;
        }

        // method to run at the appropriate time: sets background color
        // back to the original
        public void run() {
            if (view == null) return;
            view.setBackground(background);
            Log.i("Human", "Ending flash");
        }
    }

    /**
     * Helper class to flash the background of cards.
     * Cannot use Unflasher, because setting background drawables
     *  was causing scaling issues
     */
    private class ResetBackground implements Runnable {

        private final int index;
        private final DominionCardPlace place;

        //Constructor
        public ResetBackground(int index, DominionCardPlace place){
            this.index = index;
            this.place = place;
        }

        /**
         * Resets the card's color filter.
         * If the card pile is empty, readds the empty pile filter.
         */
        public void run(){
            //Vars
            ConstraintLayout cardView;
            DominionShopPileState pile;

            //Find the corresponding card data
            switch(place){
                case BASE_CARD:
                    cardView = basePiles.get(index);
                    pile = state.getBaseCards().get(index);
                    break;
                case SHOP_CARD:
                    cardView = shopPiles.get(index);
                    pile = state.getShopCards().get(index);
                    break;
                default:
                    return;
            }

            //Reset filters
            clearColorFilter(cardView);

            //Deal with empty pile case.
            if (pile.getAmount() == 0){
                setColorFilter(cardView, EMPTY_PILE);
            }
        }
    }
}