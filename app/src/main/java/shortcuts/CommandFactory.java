package shortcuts;

import java.awt.Window;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import services.FileService;
import view.MenuBar;
import viewmodel.FractalViewModel;

/**
 * Фабрика команд приложения.
 * Реализует централизованное создание команд для устранения дублирования кода.
 * 
 * Эта фабрика использует паттерн Singleton, чтобы обеспечить
 * единую точку создания команд во всем приложении. Команды создаются
 * с использованием паттерна Command, что позволяет отделить запрос от
 * его обработчика и привязать их к разным интерфейсным элементам.
 */
public class CommandFactory {
    private static CommandFactory instance;
    private final KeyboardShortcutManager shortcutManager;
    
    private CommandFactory() {
        shortcutManager = KeyboardShortcutManager.getInstance();
    }
    
    public static CommandFactory getInstance() {
        if (instance == null) {
            instance = new CommandFactory();
        }
        return instance;
    }
    
    /**
     * Инициализирует и регистрирует все стандартные команды приложения.
     * Центральный метод, который регистрирует все команды приложения в менеджере горячих клавиш.
     * 
     * @param viewModel ViewModel приложения
     * @param fileService Сервис для файловых операций
     * @param menuBar Ссылка на панель меню для доступа к методам взаимодействия с пользователем
     */
    public void registerStandardCommands(FractalViewModel viewModel, FileService fileService, MenuBar menuBar) {
        // Команда загрузки файла
        registerCommand(
            KeyboardShortcutManager.SHORTCUT_OPEN,
            "Загрузить состояние...",
            menuBar::loadFractal,
            () -> true
        );
        
        // Команда сохранения файла
        registerCommand(
            KeyboardShortcutManager.SHORTCUT_SAVE,
            "Сохранить...",
            menuBar::saveUsingFileChooser,
            () -> true
        );
        
        // Команда выхода из приложения
        registerCommand(
            KeyboardShortcutManager.SHORTCUT_EXIT,
            "Выход",
            () -> {
                Window window = SwingUtilities.getWindowAncestor(menuBar);
                if (window != null) {
                    window.dispatchEvent(new java.awt.event.WindowEvent(
                        window, java.awt.event.WindowEvent.WINDOW_CLOSING));
                }
            },
            () -> true
        );
        
        // Команда отмены действия
        registerCommand(
            KeyboardShortcutManager.SHORTCUT_UNDO,
            "Отменить",
            viewModel::undoLastAction,
            () -> viewModel.getUndoManager().canUndo()
        );
    }
    
    /**
     * Регистрирует команду в менеджере горячих клавиш.
     * Этот метод используется для создания и регистрации команд в одном месте,
     * что устраняет дублирование кода при создании команд.
     * 
     * @param keyStroke Горячая клавиша для команды
     * @param name Название команды
     * @param action Действие, выполняемое командой
     * @param enabledCheck Функция проверки доступности команды
     */
    public void registerCommand(KeyStroke keyStroke, String name, Runnable action, 
                               java.util.function.BooleanSupplier enabledCheck) {
        AppCommand command = new AppCommand() {
            @Override
            public String getName() {
                return name;
            }
            
            @Override
            public void execute() {
                action.run();
            }
            
            @Override
            public boolean isEnabled() {
                return enabledCheck.getAsBoolean();
            }
        };
        
        shortcutManager.registerShortcut(keyStroke, command);
    }
}
