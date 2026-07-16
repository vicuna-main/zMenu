package fr.maxlego08.menu.api.button;

/**
 * Represents the kind of value an {@code INPUT} button expects the player to
 * type in chat. The type controls how the {@code conditions.min} and
 * {@code conditions.max} values are interpreted and how the raw chat message is
 * validated.
 */
public enum InputType {

    /**
     * Any text. {@code min} / {@code max} are applied to the length of the input.
     */
    TEXT,
    /**
     * An integer value ({@link Integer}). {@code min} / {@code max} are applied to the value.
     */
    NUMBER,
    /**
     * A long value ({@link Long}). {@code min} / {@code max} are applied to the value.
     */
    LONG,
    /**
     * A decimal value ({@link Double}). {@code min} / {@code max} are applied to the value.
     */
    DECIMAL;

    /**
     * Returns the {@link InputType} matching the given name, or {@link #TEXT} if
     * the name is null or unknown.
     *
     * @param name the name of the input type, case-insensitive
     * @return the matching input type, or {@link #TEXT} as a fallback
     */
    public static InputType from(String name) {
        if (name == null) return TEXT;
        try {
            return InputType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return TEXT;
        }
    }

    /**
     * Returns whether the given raw input can be parsed as this type.
     *
     * @param input the raw chat input
     * @return true if the input is a valid representation of this type
     */
    public boolean isParsable(String input) {
        try {
            switch (this) {
                case NUMBER -> Integer.parseInt(input);
                case LONG -> Long.parseLong(input);
                case DECIMAL -> Double.parseDouble(input);
                default -> {
                    return true;
                }
            }
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    /**
     * Returns the value used for the min/max comparison for the given input.
     * For {@link #TEXT} this is the length of the input, otherwise the parsed
     * numeric value.
     *
     * @param input the raw chat input, expected to be {@link #isParsable(String) parsable}
     * @return the comparable value
     */
    public double getComparableValue(String input) {
        return switch (this) {
            case TEXT -> input.length();
            case NUMBER -> Integer.parseInt(input);
            case LONG -> Long.parseLong(input);
            case DECIMAL -> Double.parseDouble(input);
        };
    }
}
