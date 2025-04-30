// File: app/src/main/java/view/MainFrame.java
package view;


import render.FractalRenderer;
import services.FileService;
import view.handlers.WindowCloseHandler;
import viewmodel.FractalViewModel;

import javax.swing.*;
import java.awt.*;



/**
 * Главное окно приложения "Fractal Explorer". // Изменено название для соответствия
 * Содержит панель отрисовки фрактала ({@link FractalPanel}),
 * главное меню ({@link MenuBar}) и строку состояния ({@link StatusBar}).
 * Инициализирует ключевые компоненты архитектуры.
 */
public class MainFrame extends JFrame {

    private static final int MIN_WIDTH = 400;
    private static final int MIN_HEIGHT = 300;
    private static final String TITLE = "Fractal Explorer";
    private final FractalViewModel viewModel;
    private final FileService fileService;
    private final FractalRenderer renderer;
    private final FractalPanel fractalPanel;
    private final StatusBar statusBar;

    /**
     * Конструктор главного окна приложения.
     * Инициализирует компоненты и компоновку.
     */
    public MainFrame() {
        this(new FileService());
    }

    /**
     * Конструктор для внедрения зависимостей (тестируемость, расширяемость).

     * @param fileService файловый сервис
     */
    public MainFrame(FileService fileService) {

        this.fileService = fileService;
        this.viewModel = new FractalViewModel();
        this.statusBar = new StatusBar();
        this.renderer = new FractalRenderer();
        this.fractalPanel = new FractalPanel(viewModel, renderer, statusBar);

        initializeFrame();
        layoutComponents();
        addWindowListener(new WindowCloseHandler(this));
    }

    /**
     * Получить используемый FractalRenderer (для WindowCloseHandler и др.)
     */
    public FractalRenderer getRenderer() {
        return renderer;
    }

    /**
     * Инициализация параметров окна.
     */
    private void initializeFrame() {
        setTitle(TITLE);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
    }

    /**
     * Компоновка компонентов.
     */
    private void layoutComponents() {
        setLayout(new BorderLayout());
        setJMenuBar(new MenuBar(viewModel, fileService, fractalPanel, statusBar));
        add(fractalPanel, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
        pack();
    }

    /**
     * Возвращает экземпляр строки состояния.
     * @return StatusBar этого окна
     */
    public StatusBar getStatusBar() {
        return statusBar;
    }

    /**
     * Точка входа в приложение.
     * @param args аргументы командной строки
     */
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