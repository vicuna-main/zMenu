package fr.maxlego08.menu.api.button.buttons;

import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.button.InputType;

/**
 * Represents a button that captures text input from the player through chat.
 * <p>
 * When clicked, the inventory is closed and the player is prompted to type a
 * value in chat. The value is validated against the configured conditions
 * ({@code regex}, {@code min}, {@code max}) and, depending on the result, the
 * {@code success-actions} or {@code error-actions} are executed. The typed
 * value is exposed through the {@code %input%} placeholder.
 */
public abstract class InputButton extends Button {

    /**
     * Returns the type of value expected from the player.
     *
     * @return the input type
     */
    public abstract InputType getInputType();
}
