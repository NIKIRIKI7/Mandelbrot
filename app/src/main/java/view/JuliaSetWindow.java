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
// import services.FileService; // Закомментировано, так как меню не используется

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
        super("Julia Set Explorer (c = " + String.format(java.util.Locale.US, "%.4f %s %.4fi", // Используем Locale.US для точки
                c.getReal(), c.getImaginary() >= 0 ? "+" : "-", Math.abs(c.getImaginary())) + ")");
        this.c = c;

        // Создаем НЕЗАВИСИМЫЕ экземпляры рендерера и ViewModel для этого окна
        this.renderer = new FractalRenderer();
        this.viewModel = new FractalViewModel(renderer); // Используем конструктор по умолчанию для истории Undo

        // Создаем начальное состояние специально для множества Жюлиа
        FractalState juliaState = createInitialJuliaState(c, initialColorScheme, initialIterations);
        // Загружаем это состояние в ViewModel окна Жюлиа. Это сбрасывает историю Undo.
        this.viewModel.loadState(juliaState);

        // Создаем панель отрисовки, передавая null для MainFrame
        this.fractalPanel = new FractalPanel(viewModel, renderer, null); // <-- Передаем null

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
                System.out.println("Завершение работы рендерера окна Жюлиа...");
                // Останавливаем ExecutorService рендерера этого окна
                renderer.shutdown();
                System.out.println("Рендерер окна Жюлиа остановлен для c=" + JuliaSetWindow.this.c);
                // dispose() вызовется автоматически после выполнения всех WindowListener'ов при DISPOSE_ON_CLOSE
            }
        });

        // Опционально: можно добавить собственное меню для окна Жюлиа
        // MenuBar juliaMenuBar = createJuliaMenuBar();
        // setJMenuBar(juliaMenuBar);

        System.out.println("Окно множества Жюлиа создано для c = " + c);
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
        // Убедимся, что итерации положительные
        int validIterations = Math.max(1, maxIterations);
        return new FractalState(juliaViewport, validIterations, colorScheme, juliaFunction);
    }

    /**
     * Делает окно множества Жюлиа видимым.
     * После вызова этого метода панель {@link FractalPanel} получит события
     * {@code componentShown} или {@code componentResized}, что инициирует
     * первый запуск рендеринга.
     */
    public void display() {
        // Убедимся, что окно показывается в потоке EDT
        if (SwingUtilities.isEventDispatchThread()) {
            setVisible(true);
        } else {
            SwingUtilities.invokeLater(() -> setVisible(true));
        }
        // Первый рендер запустится автоматически панелью FractalPanel после отображения
    }
}