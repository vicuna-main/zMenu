package fr.maxlego08.menu.button.loader;

import fr.maxlego08.menu.api.ButtonManager;
import fr.maxlego08.menu.api.MenuPlugin;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.button.DefaultButtonValue;
import fr.maxlego08.menu.api.button.InputType;
import fr.maxlego08.menu.api.loader.ButtonLoader;
import fr.maxlego08.menu.api.requirement.Action;
import fr.maxlego08.menu.button.buttons.ZInputButton;
import fr.maxlego08.menu.listener.InputManager;
import fr.maxlego08.menu.zcore.logger.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class InputLoader extends ButtonLoader {

    private final MenuPlugin menuPlugin;
    private final InputManager inputManager;

    public InputLoader(MenuPlugin plugin, InputManager inputManager) {
        super(plugin, "INPUT");
        this.menuPlugin = plugin;
        this.inputManager = inputManager;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Button load(@NonNull YamlConfiguration configuration, @NonNull String path, @NonNull DefaultButtonValue defaultButtonValue) {

        File file = defaultButtonValue.getFile();
        ButtonManager buttonManager = this.menuPlugin.getButtonManager();

        InputType inputType = InputType.from(configuration.getString(path + "inputType", configuration.getString(path + "input-type", "TEXT")));

        String regexString = configuration.getString(path + "conditions.regex", null);
        Pattern regex = null;
        if (regexString != null && !regexString.isEmpty()) {
            try {
                regex = Pattern.compile(regexString);
            } catch (PatternSyntaxException exception) {
                Logger.info("Invalid regex \"" + regexString + "\" for the input button " + path + " in file " + file.getAbsolutePath(), Logger.LogType.ERROR);
            }
        }

        double min = configuration.getDouble(path + "conditions.min", 0);
        double max = configuration.getDouble(path + "conditions.max", 0);

        List<Map<String, Object>> successMaps = (List<Map<String, Object>>) configuration.getList(path + "success-actions", new ArrayList<>());
        List<Action> successActions = buttonManager.loadActions(successMaps, path + "success-actions", file);

        List<Map<String, Object>> errorMaps = (List<Map<String, Object>>) configuration.getList(path + "error-actions", new ArrayList<>());
        List<Action> errorActions = buttonManager.loadActions(errorMaps, path + "error-actions", file);

        List<String> promptMessages = configuration.getStringList(path + "input-message");
        if (promptMessages.isEmpty()) {
            promptMessages = configuration.getStringList(path + "messages");
        }

        String promptTitle = configuration.getString(path + "input-title", null);
        String promptSubtitle = configuration.getString(path + "input-subtitle", "");
        String promptActionbar = configuration.getString(path + "input-actionbar", configuration.getString(path + "input-action-bar", null));

        if (promptTitle == null && !promptMessages.isEmpty()) {
            promptTitle = promptMessages.getFirst();
        }
        if (promptActionbar == null && !promptMessages.isEmpty()) {
            promptActionbar = promptMessages.getFirst();
        }

        return new ZInputButton(this.menuPlugin, this.inputManager, inputType, regex, min, max, successActions, errorActions, promptMessages, promptTitle, promptSubtitle, promptActionbar);
    }
}
