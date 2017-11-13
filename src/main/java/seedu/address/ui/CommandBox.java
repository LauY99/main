package seedu.address.ui;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.events.ui.NewResultAvailableEvent;
import seedu.address.logic.ListElementPointer;
import seedu.address.logic.Logic;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.logic.trie.Trie;

/**
 * The UI component that is responsible for receiving user command inputs.
 */
public class CommandBox extends UiPart<Region> {

    public static final String ERROR_STYLE_CLASS = "error";
    private static final String FXML = "CommandBox.fxml";

    private Trie commandTrie;
    private Set<String> commandSet;
    private final Logger logger = LogsCenter.getLogger(CommandBox.class);
    private final Logic logic;
    private final ContextMenu autoCompleteBox;

    private ListElementPointer historySnapshot;

    @FXML
    private TextField commandTextField;

    public CommandBox(Logic logic) {
        super(FXML);
        this.logic = logic;
        commandTrie = logic.getCommandTrie();
        commandSet = commandTrie.getCommandSet();
        // calls #setStyleToDefault() whenever there is a change to the text of the command box.
        commandTextField.textProperty().addListener((unused1, unused2, unused3) -> setStyleToDefault());
        historySnapshot = logic.getHistorySnapshot();
        autoCompleteBox = new ContextMenu();
        commandTextField.setContextMenu(autoCompleteBox);
    }

    /**
     * Handles the key press event, {@code keyEvent}.
     */
    @FXML
    private void handleKeyPress(KeyEvent keyEvent) {
        switch (keyEvent.getCode()) {
        case UP:
            // As up and down buttons will alter the position of the caret,
            // consuming it causes the caret's position to remain unchanged
            keyEvent.consume();
            navigateToPreviousInput();
            break;
        case DOWN:
            keyEvent.consume();
            navigateToNextInput();
            break;
        case TAB:
            keyEvent.consume();
            handleAutoComplete();
            break;
        default:
            if (autoCompleteBox.isShowing()) {
                autoCompleteBox.hide();
            }
            // let JavaFx handle the keypress
        }
    }

    /**
     * Updates the text field with the previous input in {@code historySnapshot},
     * if there exists a previous input in {@code historySnapshot}
     */
    private void navigateToPreviousInput() {
        assert historySnapshot != null;
        if (!historySnapshot.hasPrevious()) {
            return;
        }

        replaceText(historySnapshot.previous());
    }

    /**
     * Updates the text field with the next input in {@code historySnapshot},
     * if there exists a next input in {@code historySnapshot}
     */
    private void navigateToNextInput() {
        assert historySnapshot != null;
        if (!historySnapshot.hasNext()) {
            return;
        }

        replaceText(historySnapshot.next());
    }

    //@@author grantcm
    /**
     * Handles the Tab button pressed event.
     */
    private void handleAutoComplete() {
        String input = commandTextField.getText();
        try {
            String command = commandTrie.attemptAutoComplete(input);
            if (input.equals(command)) {
                //Multiple options for autocomplete
                setStyleToIndicateCommandFailure();
                showAutoCompleteOptions(commandTrie.getOptions(input));
            } else if (commandSet.contains(command)) {
                //Able to autocomplete to a correct command
                this.replaceText(command);
                logger.info("Autocomplete successful with input: " + input + " to " + command);
            } else if (commandSet.contains(input)) {
                //Add parameters
                this.replaceText(input + command);
                logger.info("Autocomplete successful with input: " + input + " to " + input + command);
            }
        } catch (NullPointerException e) {
            //No command exists in trie or no trie exists
            setStyleToIndicateCommandFailure();
            logger.info("Autocomplete failed with input: " + input);
        }
    }

    /**
     * Handles the construction of the ContextMenu for autocomplete failure
     * @param options representing potential completion options
     */
    private void showAutoCompleteOptions(List<String> options) {
        for (String option : options) {
            MenuItem item = new MenuItem(option);
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    replaceText(item.getText());
                    autoCompleteBox.getItems().clear();
                }
            });
            autoCompleteBox.getItems().add(item);
        }
        logger.info("Autocomplete returned possible options.");
        autoCompleteBox.show(commandTextField, Side.BOTTOM, 0.0, 0.0);
    }
    //@@author

    /**
     * Sets {@code CommandBox}'s text field with {@code text} and
     * positions the caret to the end of the {@code text}.
     */
    private void replaceText(String text) {
        commandTextField.setText(text);
        commandTextField.positionCaret(commandTextField.getText().length());
    }

    /**
     * Handles the Enter button pressed event.
     */
    @FXML
    private void handleCommandInputChanged() {
        try {
            CommandResult commandResult = logic.execute(commandTextField.getText());
            initHistory();
            historySnapshot.next();
            // process result of the command
            commandTextField.setText("");
            logger.info("Result: " + commandResult.feedbackToUser);
            raise(new NewResultAvailableEvent(commandResult.feedbackToUser));

        } catch (CommandException | ParseException e) {
            initHistory();
            // handle command failure
            setStyleToIndicateCommandFailure();
            logger.info("Invalid command: " + commandTextField.getText());
            raise(new NewResultAvailableEvent(e.getMessage()));
        }
    }

    /**
     * Takes a command transparently to the CLI
     */
    @FXML
    public void handleCommandInputChanged(String command) {
        logger.info("Trying to handle " + command + " transparently");
        try {
            CommandResult commandResult = logic.execute(command);
            initHistory();
            historySnapshot.next();
            // process result of the command
            commandTextField.setText("");
            logger.info("Result: " + commandResult.feedbackToUser);
            raise(new NewResultAvailableEvent(commandResult.feedbackToUser));

        } catch (CommandException | ParseException e) {
            initHistory();
            // handle command failure
            setStyleToIndicateCommandFailure();
            logger.info("Invalid command: " + commandTextField.getText());
            raise(new NewResultAvailableEvent(e.getMessage()));
        }
    }

    /**
     * Initializes the history snapshot.
     */
    private void initHistory() {
        historySnapshot = logic.getHistorySnapshot();
        // add an empty string to represent the most-recent end of historySnapshot, to be shown to
        // the user if she tries to navigate past the most-recent end of the historySnapshot.
        historySnapshot.add("");
    }

    /**
     * Sets the command box style to use the default style.
     */
    private void setStyleToDefault() {
        commandTextField.getStyleClass().remove(ERROR_STYLE_CLASS);
    }

    /**
     * Sets the command box style to indicate a failed command.
     */
    private void setStyleToIndicateCommandFailure() {
        ObservableList<String> styleClass = commandTextField.getStyleClass();

        if (styleClass.contains(ERROR_STYLE_CLASS)) {
            return;
        }

        styleClass.add(ERROR_STYLE_CLASS);
    }
}
