// File: app/src/main/java/shortcuts/KeyboardShortcutManager.java
package shortcuts;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Менеджер горячих клавиш (паттерны Singleton и Flyweight).
 * Централизованно управляет горячими клавишами и связанными командами.
 */
public class KeyboardShortcutManager {
    private static KeyboardShortcutManager instance;
    private final Map<KeyStroke, AppCommand> shortcutMap = new HashMap<>();
    private final Map<JMenuItem, KeyStroke> menuBindings = new HashMap<>();
    
    // Константы для стандартных горячих клавиш
    public static final KeyStroke SHORTCUT_OPEN = KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK);
    public static final KeyStroke SHORTCUT_SAVE = KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK);
    public static final KeyStroke SHORTCUT_EXIT = KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK);
    public static final KeyStroke SHORTCUT_UNDO = KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK);
    
    // Дополнительные горячие клавиши
    public static final KeyStroke SHORTCUT_ANIMATION = KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.CTRL_MASK);
    public static final KeyStroke SHORTCUT_NEW = KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK);
    public static final KeyStroke SHORTCUT_SETTINGS = KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.CTRL_MASK);
    
    private KeyboardShortcutManager() {}
    
    public static KeyboardShortcutManager getInstance() {
        if (instance == null) {
            instance = new KeyboardShortcutManager();
        }
        return instance;
    }
    
    /**
     * Регистрирует команду с горячей клавишей.
     */
    public void registerShortcut(KeyStroke keyStroke, AppCommand command) {
        shortcutMap.put(keyStroke, command);
    }
    
    /**
     * Возвращает команду для указанной горячей клавиши.
     */
    public AppCommand getCommand(KeyStroke keyStroke) {
        return shortcutMap.get(keyStroke);
    }
    
    /**
     * Связывает пункт меню с соответствующей горячей клавишей.
     * @param menuItem Пункт меню
     * @param keyStroke Горячая клавиша
     */
    public void bindMenuItemToShortcut(JMenuItem menuItem, KeyStroke keyStroke) {
        AppCommand command = shortcutMap.get(keyStroke);
        if (command != null) {
            menuItem.setText(command.getName());
            menuItem.setAccelerator(keyStroke);
            menuItem.addActionListener(e -> command.execute());
            menuItem.setEnabled(command.isEnabled());
            menuBindings.put(menuItem, keyStroke);
        }
    }
    
    /**
     * Обновляет состояние всех пунктов меню, связанных с горячими клавишами.
     * @param parentComponent Родительский компонент, содержащий меню итемы
     */
    public void updateMenuItemStates(JComponent parentComponent) {
        for (Map.Entry<JMenuItem, KeyStroke> entry : menuBindings.entrySet()) {
            JMenuItem menuItem = entry.getKey();
            KeyStroke keyStroke = entry.getValue();
            AppCommand command = shortcutMap.get(keyStroke);
            
            if (command != null) {
                menuItem.setEnabled(command.isEnabled());
            }
        }
    }
    
    /**
     * Обновляет состояние пунктов меню, привязанных к горячим клавишам.
     * @param menuBar Панель меню для обновления
     */
    public void updateMenuItemStates(JMenuBar menuBar) {
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            if (menu == null) continue;
            
            for (int j = 0; j < menu.getItemCount(); j++) {
                JMenuItem item = menu.getItem(j);
                if (item == null) continue;
                
                KeyStroke keyStroke = item.getAccelerator();
                if (keyStroke != null && shortcutMap.containsKey(keyStroke)) {
                    AppCommand command = shortcutMap.get(keyStroke);
                    item.setEnabled(command.isEnabled());
                }
            }
        }
    }
}
