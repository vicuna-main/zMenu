package fr.maxlego08.menu.requirement.permissible;

import fr.maxlego08.menu.api.MenuPlugin;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.configuration.Configuration;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.enums.PlaceholderAction;
import fr.maxlego08.menu.api.requirement.Action;
import fr.maxlego08.menu.api.requirement.permissible.PlaceholderPermissible;
import fr.maxlego08.menu.api.utils.OfflinePlayerCache;
import fr.maxlego08.menu.api.utils.Placeholders;
import fr.maxlego08.menu.zcore.logger.Logger;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.function.Function;

/**
 * Implementation of the {@link PlaceholderPermissible} interface that checks player permissions
 * based on specified placeholder values and actions.
 */
public class ZPlaceholderPermissible extends PlaceholderPermissible {

    private final PlaceholderAction action;
    private final String placeholder;
    private final String value;
    private final String targetPlayer;
    private final boolean enableMathExpression;

    /**
     * Constructs a ZPlaceholderPermissible with the specified placeholder action, placeholder key, and value.
     *
     * @param action      The {@link PlaceholderAction} to perform.
     * @param placeholder The placeholder key to evaluate.
     * @param value       The value associated with the placeholder.
     */
    public ZPlaceholderPermissible(PlaceholderAction action, String placeholder, String value, String targetPlayer, List<Action> denyActions, List<Action> successActions, boolean enableMathExpression) {
        super(denyActions, successActions);
        this.action = action;
        this.placeholder = placeholder;
        this.value = value;
        this.targetPlayer = targetPlayer;
        this.enableMathExpression = enableMathExpression;
    }

    /**
     * Checks whether the player has the necessary permission based on the specified placeholder values and actions.
     *
     * @param player       The player whose permission is being checked.
     * @param placeholders Placeholders
     * @return {@code true} if the player has the necessary permission, otherwise {@code false}.
     */

    @Override
    public boolean hasPermission(@NonNull Player player, Button button, @NonNull InventoryEngine inventoryEngine, @NonNull Placeholders placeholders) {

        MenuPlugin plugin = inventoryEngine.getPlugin();
        String valueAsString;
        String resultAsString;

        if (this.targetPlayer == null || this.targetPlayer.equalsIgnoreCase("null")) {

            valueAsString = plugin.parse(player, placeholders.parse(resolveNested(this.placeholder, string -> plugin.parse(player, string))));
            resultAsString = plugin.parse(player, placeholders.parse(resolveNested(this.value, string -> plugin.parse(player, string))));
        } else {

            OfflinePlayer offlinePlayer = OfflinePlayerCache.get(plugin.parse(player, placeholders.parse(this.targetPlayer)));
            OfflinePlayer effectivePlayer = offlinePlayer.hasPlayedBefore() ? offlinePlayer : player;
            valueAsString = plugin.parse(effectivePlayer, placeholders.parse(resolveNested(this.placeholder, string -> plugin.parse(effectivePlayer, string))));
            resultAsString = plugin.parse(effectivePlayer, placeholders.parse(resolveNested(this.value, string -> plugin.parse(effectivePlayer, string))));
        }

        if (this.action.equals(PlaceholderAction.BOOLEAN)) {

            try {
                return Boolean.valueOf(valueAsString) == Boolean.valueOf(resultAsString);
            } catch (Exception exception) {
                return false;
            }

        } else if (this.action.isString()) {

            return switch (this.action) {
                case EQUALS_STRING -> valueAsString.equals(resultAsString);
                case DIFFERENT_STRING -> !valueAsString.equals(resultAsString);
                case EQUALSIGNORECASE_STRING -> valueAsString.equalsIgnoreCase(resultAsString);
                case CONTAINS_STRING -> valueAsString.contains(resultAsString);
                default -> false;
            };

        } else {

            try {

                double value;
                double currentValue;

                if (this.enableMathExpression) {
                    value = new ExpressionBuilder(valueAsString).build().evaluate();
                    currentValue = new ExpressionBuilder(resultAsString).build().evaluate();
                } else {
                    value = Double.parseDouble(valueAsString.replace(",", "."));
                    currentValue = Double.parseDouble(resultAsString.replace(",", "."));
                }

                return switch (this.action) {
                    case EQUAL_TO -> value == currentValue;
                    case LOWER -> value < currentValue;
                    case LOWER_OR_EQUAL -> value <= currentValue;
                    case SUPERIOR -> value > currentValue;
                    case SUPERIOR_OR_EQUAL -> value >= currentValue;
                    default -> true;
                };

            } catch (Exception exception) {
                if (Configuration.enableDebug) {
                    exception.printStackTrace();
                }
                return false;
            }

        }
    }

    /**
     * Resolves nested PlaceholderAPI placeholders written with curly braces ({@code {...}}) before the
     * outer {@code %...%} placeholders are parsed. This allows constructs such as
     * {@code %multiverse-core_alias_{some_papi_world}%}, where the inner placeholder is evaluated first
     * and its result is injected into the outer placeholder.
     * <p>
     * If an inner placeholder cannot be resolved (PlaceholderAPI returns it unchanged), the original
     * {@code {...}} text is kept untouched, so this remains backward compatible with strings that
     * legitimately contain curly braces (for example NBT-like content).
     *
     * @param input      the raw string that may contain {@code {...}} placeholders, may be {@code null}.
     * @param papiParser the function used to resolve a single {@code %...%} placeholder.
     * @return the string with resolvable inner placeholders replaced.
     */
    private static String resolveNested(String input, Function<String, String> papiParser) {
        if (input == null || input.indexOf('{') < 0) return input;

        StringBuilder builder = new StringBuilder(input.length());
        int index = 0;
        while (index < input.length()) {
            char character = input.charAt(index);
            if (character == '{') {
                int end = input.indexOf('}', index);
                if (end > index) {
                    String placeholder = "%" + input.substring(index + 1, end) + "%";
                    String resolved = papiParser.apply(placeholder);
                    // Only substitute when PlaceholderAPI actually resolved the placeholder, otherwise keep the literal text.
                    builder.append(resolved.equals(placeholder) ? input.substring(index, end + 1) : resolved);
                    index = end + 1;
                    continue;
                }
            }
            builder.append(character);
            index++;
        }
        return builder.toString();
    }

    /**
     * Gets the {@link PlaceholderAction} associated with this permissible.
     *
     * @return The {@link PlaceholderAction}.
     */
    @Override
    public PlaceholderAction getPlaceholderAction() {
        return this.action;
    }

    /**
     * Gets the placeholder key associated with this permissible.
     *
     * @return The placeholder key.
     */
    @Override
    public String getPlaceholder() {
        return this.placeholder;
    }

    /**
     * Gets the value associated with this permissible.
     *
     * @return The value.
     */
    @Override
    public String getValue() {
        return this.value;
    }

    /**
     * Checks whether the ZPlaceholderPermissible instance is valid.
     *
     * @return {@code true} if the instance is valid, otherwise {@code false}.
     */
    @Override
    public boolean isValid() {
        if (this.value == null) Logger.info("Value is null !", Logger.LogType.WARNING);
        if (this.action == null) Logger.info("Action is null !", Logger.LogType.WARNING);
        if (this.placeholder == null) Logger.info("Placeholder is null !", Logger.LogType.WARNING);
        return this.value != null && this.action != null && this.placeholder != null;
    }
}
