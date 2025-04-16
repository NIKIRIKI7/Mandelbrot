package view;

import render.FractalRenderer;
import services.FileService;
import viewmodel.FractalViewModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Главное окно приложения "Mandelbrot Set Explorer".
 * Содержит основную панель для отрисовки фрактала ({@link FractalPanel})
 * и главное меню ({@link MenuBar}). Инициализирует ключевые компоненты
 * архитектуры: {@link FractalViewModel}, {@link FractalRenderer}, {@link FileService}.
 * Обрабатывает событие закрытия окна для корректного завершения работы.
 */
public class MainFrame extends JFrame {

    /** ViewModel, управляющая состоянием фрактала Мандельброта. */
    private final FractalViewModel viewModel;
    /** Рендерер, отвечающий за вычисление и отрисовку фрактала. */
    private final FractalRenderer renderer;
    /** Сервис для операций с файлами (сохранение/загрузка состояния и изображений). */
    private final FileService fileService;
    /** Панель, отображающая фрактал и обрабатывающая ввод пользователя. */
    private final FractalPanel fractalPanel;

    /**
     * Конструирует главное окно приложения.
     * Инициализирует ViewModel, Renderer, FileService, FractalPanel и MenuBar.
     * Настраивает основные параметры окна (заголовок, операция закрытия по умолчанию,
     * расположение) и добавляет слушатель для обработки закрытия окна.
     */
    public MainFrame() {
        // Инициализация основных компонентов
        renderer = new FractalRenderer();
        viewModel = new FractalViewModel(renderer); // ViewModel для Мандельброта
        fileService = new FileService();

        // Настройка окна
        setTitle("Mandelbrot Set Explorer");
        // Устанавливаем DO_NOTHING_ON_CLOSE, чтобы обработать закрытие в windowClosing
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null); // Центрировать окно на экране

        // Создание и настройка компонентов GUI
        fractalPanel = new FractalPanel(viewModel, renderer); // Панель для Мандельброта
        MenuBar menuBar = new MenuBar(viewModel, fileService, fractalPanel, this);

        // Сборка интерфейса
        setJMenuBar(menuBar);
        add(fractalPanel, BorderLayout.CENTER); // Добавляем панель в центр окна
        pack(); // Устанавливает размер окна на основе предпочтительных размеров компонентов

        // Добавляем слушатель для обработки события закрытия окна
        addWindowListener(new WindowAdapter() {
            /**
             * Вызывается, когда пользователь пытается закрыть окно (нажимает крестик).
             * Вызывает метод {@link #handleWindowClose()} для подтверждения
             * и корректного завершения работы.
             * @param e Событие окна.
             */
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClose();
            }
        });
    }

    /**
     * Обрабатывает попытку закрытия главного окна.
     * Показывает диалог подтверждения. Если пользователь подтверждает выход,
     * останавливает потоки рендерера, освобождает ресурсы окна и завершает приложение.
     */
    private void handleWindowClose() {
        // Показываем диалог подтверждения
        int confirmation = JOptionPane.showConfirmDialog(
                this, // Родительский компонент для диалога
                "Are you sure you want to exit?", // Сообщение
                "Confirm Exit", // Заголовок диалога
                JOptionPane.YES_NO_OPTION, // Кнопки Да/Нет
                JOptionPane.QUESTION_MESSAGE // Иконка вопроса
        );

        // Если пользователь нажал "Да"
        if (confirmation == JOptionPane.YES_OPTION) {
            System.out.println("Shutting down renderer...");
            // Корректно останавливаем ExecutorService рендерера
            renderer.shutdown();
            System.out.println("Exiting application.");
            // Освобождаем ресурсы окна
            dispose();
            // Завершаем работу JVM
            System.exit(0);
        }
        // Если пользователь нажал "Нет", ничего не делаем, окно остается открытым
    }

    /**
     * Главный метод, точка входа в приложение.
     * Устанавливает системный Look and Feel для нативного вида интерфейса
     * и запускает создание и отображение главного окна в потоке
     * диспетчеризации событий Swing (EDT).
     *
     * @param args Аргументы командной строки (не используются).
     */
    public static void main(String[] args) {
        // Попытка установить системный стиль интерфейса для лучшего внешнего вида
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // В случае ошибки просто выводим сообщение, приложение продолжит работу со стилем по умолчанию
            System.err.println("Couldn't set system look and feel: " + e.getMessage());
        }

        // Запускаем создание и отображение GUI в потоке диспетчеризации событий (EDT)
        // Это стандартная практика для работы со Swing для обеспечения потокобезопасности
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true); // Делаем окно видимым
        });
    }
}