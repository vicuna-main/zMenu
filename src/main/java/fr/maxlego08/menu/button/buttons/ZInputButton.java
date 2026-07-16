package fr.maxlego08.menu.button.buttons;

import fr.maxlego08.menu.api.Inventory;
import fr.maxlego08.menu.api.MenuPlugin;
import fr.maxlego08.menu.api.button.InputType;
import fr.maxlego08.menu.api.button.buttons.InputButton;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.requirement.Action;
import fr.maxlego08.menu.api.utils.Placeholders;
import fr.maxlego08.menu.listener.InputManager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Implementation of the {@code INPUT} button type. When clicked it closes the
 * inventory, prompts the player to type a value in chat and then runs the
 * success or error actions depending on the validation result.
 */
public class ZInputButton extends InputButton {

    private final MenuPlugin plugin;
    private final InputManager inputManager;
    private final InputType inputType;
    private final Pattern regex;
    private final double min;
    private final double max;
    private final List<Action> successActions;
    private final List<Action> errorActions;
    private final List<String> promptMessages;
    private final String promptTitle;
    private final String promptSubtitle;
    private final String promptActionbar;

    public ZInputButton(MenuPlugin plugin, InputManager inputManager, InputType inputType, Pattern regex, double min, double max, List<Action> successActions, List<Action> errorActions, List<String> promptMessages, String promptTitle, String promptSubtitle, String promptActionbar) {
        this.plugin = plugin;
        this.inputManager = inputManager;
        this.inputType = inputType;
        this.regex = regex;
        this.min = min;
        this.max = max;
        this.successActions = successActions;
        this.errorActions = errorActions;
        this.promptMessages = promptMessages;
        this.promptTitle = promptTitle;
        this.promptSubtitle = promptSubtitle;
        this.promptActionbar = promptActionbar;
    }

    @Override
    public InputType getInputType() {
        return this.inputType;
    }

    public List<Action> getSuccessActions() {
        return this.successActions;
    }

    public List<Action> getErrorActions() {
        return this.errorActions;
    }

    /**
     * Validates the raw chat input against the configured input type and
     * conditions (regex, min, max).
     *
     * @param input the raw chat input
     * @return true if the input is valid
     */
    public boolean isValid(String input) {
        if (input == null) return false;

        if (this.regex != null && !this.regex.matcher(input).matches()) {
            return false;
        }

        if (!this.inputType.isParsable(input)) {
            return false;
        }

        double value = this.inputType.getComparableValue(input);
        if (value < this.min) {
            return false;
        }
        return this.max <= 0 || value <= this.max;
    }

    @Override
    public void onClick(@NonNull Player player, @NonNull InventoryClickEvent event, @NonNull InventoryEngine inventory, int slot, @NonNull Placeholders placeholders) {

        Inventory menuInventory = inventory.getMenuInventory();
        int page = inventory.getPage();
        List<Inventory> oldInventories = new ArrayList<>(inventory.getOldInventories());

        this.inputManager.await(player, new InputManager.PendingInput(this, menuInventory, page, oldInventories, placeholders));

        player.closeInventory();

        if (!this.promptMessages.isEmpty()) {
            for (String message : this.promptMessages) {
                this.plugin.getMetaUpdater().sendMessage(player, this.parsePrompt(player, placeholders, message));
            }
        }

        if (this.promptTitle != null && !this.promptTitle.isEmpty()) {
            String title = this.parsePrompt(player, placeholders, this.promptTitle);
            String subtitle = this.promptSubtitle == null ? "" : this.parsePrompt(player, placeholders, this.promptSubtitle);
            this.plugin.getMetaUpdater().sendTitle(player, title, subtitle, 5, 60, 10);
        }

        if (this.promptActionbar != null && !this.promptActionbar.isEmpty()) {
            this.plugin.getMetaUpdater().sendAction(player, this.parsePrompt(player, placeholders, this.promptActionbar));
        }
    }

    private String parsePrompt(Player player, Placeholders placeholders, String message) {
        return this.plugin.parse(player, placeholders.parse(message));
    }
}
