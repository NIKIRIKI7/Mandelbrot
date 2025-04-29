package view.handlers;

import view.MainFrame;
import render.FractalRenderer;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Обработчик закрытия главного окна приложения.
 * Реализует подтверждение выхода и корректное завершение работы рендерера.
 * 
 * Улучшенная версия с внедрением зависимостей вместо использования рефлексии.
 */
public class WindowCloseHandler extends WindowAdapter {
    private final MainFrame mainFrame;
    private final FractalRenderer renderer;
    private final String exitMessage;
    private final String exitTitle;
    
    // Константы по умолчанию
    private static final String DEFAULT_EXIT_MESSAGE = "Вы уверены, что хотите выйти?";
    private static final String DEFAULT_EXIT_TITLE = "Подтверждение выхода";

    /**
     * Создает обработчик с настраиваемыми сообщениями для диалога подтверждения выхода.
     *
     * @param mainFrame   главное окно приложения
     * @param renderer    рендерер фрактала для корректного завершения работы
     * @param exitMessage сообщение в диалоге подтверждения выхода
     * @param exitTitle   заголовок диалога подтверждения выхода
     */
    public WindowCloseHandler(MainFrame mainFrame, FractalRenderer renderer, 
                              String exitMessage, String exitTitle) {
        this.mainFrame = mainFrame;
        this.renderer = renderer;
        this.exitMessage = exitMessage;
        this.exitTitle = exitTitle;
    }
    
    /**
     * Создает обработчик с сообщениями по умолчанию.
     *
     * @param mainFrame главное окно приложения
     * @param renderer  рендерер фрактала для корректного завершения работы
     */
    public WindowCloseHandler(MainFrame mainFrame, FractalRenderer renderer) {
        this(mainFrame, renderer, DEFAULT_EXIT_MESSAGE, DEFAULT_EXIT_TITLE);
    }
    
    /**
     * Создает обработчик с автоматическим получением рендерера из mainFrame.
     *
     * @param mainFrame главное окно приложения
     */
    public WindowCloseHandler(MainFrame mainFrame) {
        this(mainFrame, mainFrame.getRenderer());
    }

    @Override
    public void windowClosing(WindowEvent e) {
        int confirmation = JOptionPane.showConfirmDialog(
                mainFrame,
                exitMessage,
                exitTitle,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirmation == JOptionPane.YES_OPTION) {
            System.out.println("Shutting down renderer...");
            renderer.shutdown();
            System.out.println("Завершение работы приложения.");
            mainFrame.dispose();
            System.exit(0);
        }
    }
}
