package fr.maxlego08.menu.listener;

import fr.maxlego08.menu.api.Inventory;
import fr.maxlego08.menu.api.InventoryManager;
import fr.maxlego08.menu.api.MenuPlugin;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.engine.InventoryResult;
import fr.maxlego08.menu.api.requirement.Action;
import fr.maxlego08.menu.api.utils.CompatibilityUtil;
import fr.maxlego08.menu.api.utils.Placeholders;
import fr.maxlego08.menu.button.buttons.ZInputButton;
import fr.maxlego08.menu.inventory.VInventory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles the chat capture for {@code INPUT} buttons. When an input button is
 * clicked, it registers a pending request through {@link #await(Player, PendingInput)}.
 * The next chat message from the player is then intercepted, validated and the
 * matching actions are executed.
 */
public class InputManager implements Listener {

    private static final Set<String> VISIBLE_INVENTORY_ACTION_TYPES = Set.of(
            "refresh",
            "refresh-inventory",
            "refresh_inventory",
            "refresh inventory",
            "ri",
            "refresh-slot",
            "refresh_slot",
            "refresh slot",
            "set-item",
            "set_item",
            "set item"
    );

    private final MenuPlugin plugin;
    private final InventoryManager inventoryManager;
    private final Map<UUID, PendingInput> pendingInputs = new ConcurrentHashMap<>();

    public InputManager(MenuPlugin plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
    }

    /**
     * Registers a player as waiting for a chat input.
     *
     * @param player  the player who must type in chat
     * @param pending the pending input request
     */
    public void await(Player player, PendingInput pending) {
        this.pendingInputs.put(player.getUniqueId(), pending);
    }

    /**
     * Returns whether the given player is currently waiting to type an input.
     *
     * @param player the player to check
     * @return true if an input is pending for this player
     */
    public boolean isWaiting(Player player) {
        return this.pendingInputs.containsKey(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PendingInput pending = this.pendingInputs.remove(player.getUniqueId());
        if (pending == null) return;

        event.setCancelled(true);
        String input = event.getMessage();

        // The chat event is asynchronous, switch back to the player thread before
        // executing the actions.
        this.plugin.getScheduler().runAtEntity(player, w -> this.handle(player, pending, input));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.pendingInputs.remove(event.getPlayer().getUniqueId());
    }

    private void handle(Player player, PendingInput pending, String input) {
        ZInputButton button = pending.button();
        boolean valid = button.isValid(input);

        Placeholders placeholders = new Placeholders();
        placeholders.merge(pending.placeholders());
        placeholders.register("input", input);
        placeholders.register("player", player.getName());

        List<Action> actions = valid ? button.getSuccessActions() : button.getErrorActions();
        InventoryEngine engine;
        if (this.requiresVisibleInventory(actions)) {
            this.inventoryManager.openInventory(player, pending.inventory(), pending.page(), pending.oldInventories());
            engine = this.resolveEngine(player);
        } else {
            engine = new PendingInputInventoryEngine(this.plugin, player, pending.inventory(), pending.page(), pending.oldInventories());
        }

        for (Action action : actions) {
            action.preExecute(player, button, engine, placeholders);
        }
    }

    private boolean requiresVisibleInventory(List<Action> actions) {
        for (Action action : actions) {
            if (this.requiresVisibleInventory(action, new HashSet<>())) {
                return true;
            }
        }
        return false;
    }

    private boolean requiresVisibleInventory(Action action, Set<Action> visited) {
        if (action == null || !visited.add(action)) {
            return false;
        }

        String type = action.getType();
        if (type != null && VISIBLE_INVENTORY_ACTION_TYPES.contains(type.trim().toLowerCase(Locale.ROOT))) {
            return true;
        }

        for (Action denyChanceAction : action.getDenyChanceActions()) {
            if (this.requiresVisibleInventory(denyChanceAction, visited)) {
                return true;
            }
        }
        return false;
    }

    private InventoryEngine resolveEngine(Player player) {
        var topInventory = CompatibilityUtil.getTopInventory(player);
        if (topInventory != null) {
            InventoryHolder holder = topInventory.getHolder();
            if (holder instanceof InventoryEngine inventoryEngine) {
                return inventoryEngine;
            }
        }
        return this.inventoryManager.getFakeInventory();
    }

    /**
     * Holds the context needed to resume an {@code INPUT} button once the player
     * has typed a value in chat.
     *
     * @param button         the input button awaiting a value
     * @param inventory      the inventory to reopen after the input
     * @param page           the page to reopen
     * @param oldInventories the navigation history to restore
     * @param placeholders   the placeholders captured when the button was clicked
     */
    public record PendingInput(ZInputButton button, Inventory inventory, int page, List<Inventory> oldInventories,
                               Placeholders placeholders) {
    }

    private static class PendingInputInventoryEngine extends VInventory implements InventoryEngine {

        private final Inventory menuInventory;
        private final List<Inventory> oldInventories;
        private int maxPage = 1;

        private PendingInputInventoryEngine(MenuPlugin plugin, Player player, Inventory menuInventory, int page, List<Inventory> oldInventories) {
            this.setPlugin(plugin);
            this.player = player;
            this.menuInventory = menuInventory;
            this.page = page;
            this.oldInventories = new ArrayList<>(oldInventories);
        }

        @Override
        public InventoryResult openInventory(MenuPlugin main, Player player, int page, Object... args) {
            return InventoryResult.SUCCESS;
        }

        @Override
        public @NotNull List<Inventory> getOldInventories() {
            return new ArrayList<>(this.oldInventories);
        }

        @Override
        public @NotNull List<fr.maxlego08.menu.api.button.Button> getButtons() {
            return Collections.emptyList();
        }

        @Override
        public void buildButton(fr.maxlego08.menu.api.button.Button button) {
        }

        @Override
        public void buildButton(fr.maxlego08.menu.api.button.Button button, @NotNull Placeholders placeholders) {
        }

        @Override
        public void displayButton(@NotNull fr.maxlego08.menu.api.button.Button button) {
        }

        @Override
        public void displayButton(@NotNull fr.maxlego08.menu.api.button.Button button, @NotNull Placeholders placeholders) {
        }

        @Override
        public void displayFinalButton(@NotNull fr.maxlego08.menu.api.button.Button button, int... slots) {
        }

        @Override
        public void displayFinalButton(@NotNull fr.maxlego08.menu.api.button.Button button, @NotNull Placeholders placeholders, int... slots) {
        }

        @Override
        public Inventory getMenuInventory() {
            return this.menuInventory;
        }

        @Override
        public int getMaxPage() {
            return this.maxPage;
        }

        @Override
        public void setMaxPage(int maxPage) {
            this.maxPage = maxPage;
        }

        @Override
        public void cancel(int slot) {
        }
    }
}
