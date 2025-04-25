// File: app/src/main/java/view/MainFrame.java
package view;

import render.FractalRenderer;
import services.FileService;
import viewmodel.FractalViewModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Главное окно приложения "Fractal Explorer". // Изменено название для соответствия
 * Содержит панель отрисовки фрактала ({@link FractalPanel}),
 * главное меню ({@link MenuBar}) и строку состояния ({@link StatusBar}).
 * Инициализирует ключевые компоненты архитектуры.
 */
public class MainFrame extends JFrame {

    private final FractalViewModel viewModel;
    private final FractalRenderer renderer;
    private final FileService fileService;
    private final FractalPanel fractalPanel;
    private final StatusBar statusBar; // <-- Добавлено

    /**
     * Конструирует главное окно приложения.
     */
    public MainFrame() {
        renderer = new FractalRenderer();
        viewModel = new FractalViewModel(renderer);
        fileService = new FileService();

        setTitle("Fractal Explorer"); // Изменено название
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        // Создание строки состояния ДО панели фрактала
        statusBar = new StatusBar(); // <-- Создаем StatusBar

        // Модифицируем создание FractalPanel и MenuBar, передавая StatusBar или MainFrame
        fractalPanel = new FractalPanel(viewModel, renderer, this); // Передаем MainFrame
        MenuBar menuBar = new MenuBar(viewModel, fileService, fractalPanel, this); // MenuBar уже получает MainFrame

        setJMenuBar(menuBar);
        setLayout(new BorderLayout()); // Убедимся, что используется BorderLayout
        add(fractalPanel, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH); // <-- Добавляем StatusBar вниз

        pack();
        setMinimumSize(new Dimension(400, 300)); // Установим минимальный разумный размер

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClose();
            }
        });
    }

    /**
     * Возвращает экземпляр строки состояния.
     * @return StatusBar этого окна.
     */
    public StatusBar getStatusBar() { // <-- Метод для доступа к StatusBar
        return statusBar;
    }

    // Метод handleWindowClose остается без изменений

    // Метод main остается без изменений
    private void handleWindowClose() {
        int confirmation = JOptionPane.showConfirmDialog(
                this,
                "Вы уверены, что хотите выйти?", // Сообщение на русском
                "Подтверждение выхода", // Заголовок на русском
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirmation == JOptionPane.YES_OPTION) {
            System.out.println("Shutting down renderer...");
            renderer.shutdown();
            // Убедимся, что рендерер в панели предпросмотра анимации тоже остановлен, если диалог был открыт
            // (Хотя закрытие диалога должно было это сделать)
            System.out.println("Завершение работы приложения.");
            dispose();
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Не удалось установить системный стиль: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}