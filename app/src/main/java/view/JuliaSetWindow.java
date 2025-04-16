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

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Окно для отображения множества Жюлиа, связанного с точкой 'c'
 * из множества Мандельброта.
 */
public class JuliaSetWindow extends JFrame {

    private final FractalViewModel viewModel;
    private final FractalRenderer renderer;
    private final FractalPanel fractalPanel;
    private final ComplexNumber c; // Параметр для этого множества Жюлиа

    /**
     * Создает окно для отображения множества Жюлиа.
     *
     * @param ownerFrame       Родительское окно (для позиционирования).
     * @param c                Параметр 'c' для множества Жюлиа.
     * @param initialColorScheme Начальная цветовая схема (берется из основного окна).
     * @param initialIterations Начальное количество итераций (берется из основного окна).
     */
    public JuliaSetWindow(JFrame ownerFrame, ComplexNumber c, ColorScheme initialColorScheme, int initialIterations) {
        super("Julia Set Explorer (c = " + String.format("%.4f %s %.4fi", c.getReal(), c.getImaginary() >= 0 ? "+" : "-", Math.abs(c.getImaginary())) + ")"); // Заголовок окна
        this.c = c;

        // Создаем СВОИ экземпляры рендерера и ViewModel для этого окна
        this.renderer = new FractalRenderer(); // Независимый рендерер
        this.viewModel = new FractalViewModel(renderer); // Независимый ViewModel

        // Создаем начальное состояние для Жюлиа
        FractalState juliaState = createInitialJuliaState(c, initialColorScheme, initialIterations);
        this.viewModel.loadState(juliaState); // Загружаем начальное состояние (сбросит undo)

        // Создаем панель отрисовки
        this.fractalPanel = new FractalPanel(viewModel, renderer);

        // Настройка окна
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Закрывать только это окно
        setSize(600, 500); // Начальный размер
        setLocationRelativeTo(ownerFrame); // Позиционировать относительно главного окна
        setLayout(new BorderLayout());
        add(fractalPanel, BorderLayout.CENTER);

        // Добавляем Listener для остановки рендерера при закрытии окна
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("Shutting down Julia set renderer...");
                renderer.shutdown(); // Останавливаем потоки рендерера этого окна
                System.out.println("Julia set renderer shut down.");
            }
        });

         // Можно добавить простое меню (например, только Save Image) или не добавлять меню вовсе
         // MenuBar juliaMenuBar = new MenuBar(viewModel, new FileService(), fractalPanel, this);
         // setJMenuBar(juliaMenuBar); // Опционально

        System.out.println("Julia Set window created for c = " + c);
    }

    /**
     * Создает начальное состояние для множества Жюлиа.
     *
     * @param cParam           Параметр 'c'.
     * @param colorScheme      Цветовая схема.
     * @param maxIterations    Максимум итераций.
     * @return Начальное FractalState для множества Жюлиа.
     */
    private FractalState createInitialJuliaState(ComplexNumber cParam, ColorScheme colorScheme, int maxIterations) {
        // Viewport по умолчанию для множества Жюлиа (часто [-2, 2] x [-2, 2])
        Viewport juliaViewport = new Viewport(-2.0, 2.0, -2.0, 2.0);
        // Создаем функцию Жюлиа с нашим параметром 'c'
        FractalFunction juliaFunction = new JuliaFunction(cParam);

        return new FractalState(juliaViewport, maxIterations, colorScheme, juliaFunction);
    }

    // Метод для показа окна (вызывается извне)
    public void display() {
        setVisible(true);
        // Первый рендер запустится панелью при изменении размера или первом показе
    }
}