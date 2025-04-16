// File: app/src/main/java/view/JuliaSetWindow.java
package view;

import math.FractalFunction;
import math.JuliaFunction;
import model.ColorScheme;
import model.FractalState;
import model.Viewport;
import render.FractalRenderer;
import utils.ComplexNumber;
import viewmodel.FractalViewModel;
import services.FileService; // Импорт для возможного меню сохранения

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Окно JFrame, предназначенное для отображения множества Жюлиа.
 * Множество Жюлиа определяется комплексным параметром 'c', который обычно
 * соответствует точке, выбранной пользователем в основном окне множества Мандельброта.
 * Использует собственные экземпляры {@link FractalViewModel} и {@link FractalRenderer}.
 */
public class JuliaSetWindow extends JFrame {

    /** Независимый ViewModel для управления состоянием этого окна Жюлиа. */
    private final FractalViewModel viewModel;
    /** Независимый рендерер для отрисовки множества Жюлиа в этом окне. */
    private final FractalRenderer renderer;
    /** Панель для отрисовки фрактала Жюлиа, использующая свои viewModel и renderer. */
    private final FractalPanel fractalPanel;
    /**
     * Комплексный параметр 'c', определяющий конкретное множество Жюлиа,
     * отображаемое в этом окне. Является константой для {@link JuliaFunction}.
     */
    private final ComplexNumber c;

    /**
     * Создает новое окно для отображения множества Жюлиа.
     * Инициализирует независимые компоненты рендеринга и управления состоянием.
     * Устанавливает начальное состояние (viewport, итерации, схема) на основе
     * параметров, переданных из основного окна.
     *
     * @param ownerFrame       Родительское окно (обычно {@link MainFrame}), используется
     *                         для позиционирования этого окна. Может быть null.
     * @param c                Комплексный параметр 'c' для функции множества Жюлиа {@link JuliaFunction}. Не может быть null.
     * @param initialColorScheme Начальная цветовая схема, унаследованная из основного окна. Не может быть null.
     * @param initialIterations Начальное максимальное количество итераций, унаследованное из основного окна. Должно быть > 0.
     */
    public JuliaSetWindow(JFrame ownerFrame, ComplexNumber c, ColorScheme initialColorScheme, int initialIterations) {
        // Формируем заголовок окна, отображающий значение 'c'
        super("Julia Set Explorer (c = " + String.format("%.4f %s %.4fi",
                c.getReal(), c.getImaginary() >= 0 ? "+" : "-", Math.abs(c.getImaginary())) + ")");
        this.c = c;

        // Создаем НЕЗАВИСИМЫЕ экземпляры рендерера и ViewModel для этого окна
        this.renderer = new FractalRenderer();
        this.viewModel = new FractalViewModel(renderer);

        // Создаем начальное состояние специально для множества Жюлиа
        FractalState juliaState = createInitialJuliaState(c, initialColorScheme, initialIterations);
        // Загружаем это состояние в ViewModel окна Жюлиа. Это сбрасывает историю Undo.
        this.viewModel.loadState(juliaState);

        // Создаем панель отрисовки, передавая ей НЕЗАВИСИМЫЕ viewModel и renderer
        this.fractalPanel = new FractalPanel(viewModel, renderer);

        // Настройка окна
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Закрывать только это окно, не всё приложение
        setSize(600, 500);                                // Начальный размер окна
        setLocationRelativeTo(ownerFrame);                // Позиционировать относительно родительского окна (или по центру экрана, если ownerFrame == null)
        setLayout(new BorderLayout());                    // Используем BorderLayout
        add(fractalPanel, BorderLayout.CENTER);           // Добавляем панель отрисовки в центр

        // Добавляем слушатель для корректного завершения работы рендерера при закрытии окна
        addWindowListener(new WindowAdapter() {
            /**
             * Вызывается перед закрытием окна. Останавливает потоки рендерера,
             * связанного с этим окном, чтобы предотвратить утечку ресурсов.
             * @param e Событие окна.
             */
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("Shutting down Julia set renderer...");
                // Останавливаем ExecutorService рендерера этого окна
                renderer.shutdown();
                System.out.println("Julia set renderer shut down for c=" + JuliaSetWindow.this.c);
            }
        });

        // Опционально: можно добавить собственное меню для окна Жюлиа
        // Например, только для сохранения изображения
        // MenuBar juliaMenuBar = createJuliaMenuBar();
        // setJMenuBar(juliaMenuBar);

        System.out.println("Julia Set window created for c = " + c);
    }

    /**
     * Создает начальный объект {@link FractalState} для отображения множества Жюлиа.
     * Устанавливает viewport по умолчанию для Жюлиа (обычно [-2, 2] x [-2, 2]),
     * использует переданные цветовую схему и количество итераций, и создает
     * экземпляр {@link JuliaFunction} с заданным параметром 'c'.
     *
     * @param cParam           Параметр 'c' для {@link JuliaFunction}.
     * @param colorScheme      Цветовая схема для рендеринга.
     * @param maxIterations    Максимальное количество итераций.
     * @return Начальное состояние {@link FractalState} для окна Жюлиа.
     */
    private FractalState createInitialJuliaState(ComplexNumber cParam, ColorScheme colorScheme, int maxIterations) {
        // Стандартная область просмотра для большинства множеств Жюлиа
        Viewport juliaViewport = new Viewport(-2.0, 2.0, -2.0, 2.0);
        // Создаем функцию расчета итераций для множества Жюлиа с нашим параметром 'c'
        FractalFunction juliaFunction = new JuliaFunction(cParam);

        // Создаем объект состояния
        return new FractalState(juliaViewport, maxIterations, colorScheme, juliaFunction);
    }

    // Опционально: Метод для создания упрощенного меню для окна Жюлиа
    /*
    private MenuBar createJuliaMenuBar() {
        // Создаем FileService локально или получаем извне, если нужно
        FileService localFileService = new FileService();
        // Создаем MenuBar, передавая *локальные* viewModel, fractalPanel и это окно (this)
        MenuBar juliaMenu = new MenuBar(this.viewModel, localFileService, this.fractalPanel, this);

        // Удаляем ненужные пункты меню (например, Load State, Edit->Undo, View->Scheme/Iterations)
        // Меню File (индекс 0)
        JMenu fileMenu = juliaMenu.getMenu(0);
        if (fileMenu != null) {
            // Удаляем Load, Save State, Separator, Exit (оставляем только Save As Image)
             if (fileMenu.getItemCount() > 2) fileMenu.remove(0); // Load
             if (fileMenu.getItemCount() > 2) fileMenu.remove(0); // Save State (из Save As)
             if (fileMenu.getItemCount() > 1) fileMenu.remove(1); // Separator
             if (fileMenu.getItemCount() > 1) fileMenu.remove(1); // Exit
        }
        // Удаляем меню Edit (индекс 1)
        if (juliaMenu.getMenuCount() > 1) juliaMenu.remove(1);
        // Удаляем меню View (индекс 1 после удаления Edit)
        if (juliaMenu.getMenuCount() > 1) juliaMenu.remove(1);

        return juliaMenu;
    }
    */


    /**
     * Делает окно множества Жюлиа видимым.
     * После вызова этого метода панель {@link FractalPanel} получит события
     * {@code componentShown} или {@code componentResized}, что инициирует
     * первый запуск рендеринга.
     */
    public void display() {
        setVisible(true);
        // Первый рендер запустится автоматически панелью FractalPanel
    }
}