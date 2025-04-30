package shortcuts;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
     * 
     * Архитектура привязки клавиатурных сочетаний в проекте построена на нескольких классических шаблонах проектирования:

        1. Паттерн Command (Команда)
        Этот паттерн позволяет инкапсулировать запрос как объект, позволяя:

        Параметризовать клиентов с различными запросами
        Ставить запросы в очередь
        Поддерживать отмену операций
        В коде это реализовано через интерфейс AppCommand, который требует реализации трёх методов:

        getName() - возвращает название команды для отображения
        execute() - выполняет саму команду
        isEnabled() - определяет, доступна ли команда в данный момент
        2. Паттерн Singleton (Одиночка)
        KeyboardShortcutManager реализован как одиночка, что обеспечивает:
        Единую точку управления всеми горячими клавишами
        Предотвращение конфликтов между разными частями приложения
        Централизованное хранение привязок клавиш к командам

     * Механизм привязки отмены к клавиатурному сочетанию:
     * 1. В KeyboardShortcutManager определена константа SHORTCUT_UNDO = Ctrl+Z (KeyEvent.VK_Z, ActionEvent.CTRL_MASK)
     * 2. При создании меню "Правка" выполняется несколько шагов (MenuBar):
     *    а) Создаётся пункт меню undoMenuItem
     *    б) Создаётся объект команды через createUndoCommand() (реализует интерфейс AppCommand)
     *    в) Команда регистрируется в KeyboardShortcutManager и привязывается к клавише Ctrl+Z
     *    г) Пункт меню связывается с горячей клавишей через bindMenuItemToShortcut
     * 3. При нажатии Ctrl+Z происходит следующее: 
     *    а) KeyboardShortcutManager перехватывает нажатие и находит соответствующую команду
     *    б) Вызывается метод execute() объекта команды
     *    в) Метод execute() вызывает viewModel.undoLastAction(), что выполняет отмену
     * 4. Состояние кнопки отмены (активна/неактивна) обновляется через updateUndoState() и
     *    далее updateShortcutStates(), что обновляет состояние и горячей клавиши,
     *    и пункта меню в зависимости от возможности отмены (canUndo).
     */


/**
 * Менеджер горячих клавиш
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
