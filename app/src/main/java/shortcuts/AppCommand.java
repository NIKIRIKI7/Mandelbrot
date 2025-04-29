// File: app/src/main/java/shortcuts/AppCommand.java
package shortcuts;

/**
 * Интерфейс для команд приложения (паттерн Command).
 * Инкапсулирует действие, выполняемое в ответ на горячую клавишу или пункт меню.
 */
public interface AppCommand {
    /**
     * Название команды для отображения в интерфейсе.
     */
    String getName();
    
    /**
     * Выполнить команду.
     */
    void execute();
    
    /**
     * Проверить, доступна ли команда в данный момент.
     */
    boolean isEnabled();
}
