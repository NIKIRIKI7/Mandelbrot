// File: app/src/main/java/view/FractalPanel.java
package view;

import listeners.MousePanListener;
import listeners.MouseZoomListener;
import model.ColorScheme; // Импорт нужен для передачи в JuliaSetWindow
import model.FractalState;
import render.FractalRenderer;
import utils.ComplexNumber; // Импорт нужен для координат Жюлиа
import utils.CoordinateConverter; // Импорт нужен для координат Жюлиа
import viewmodel.FractalViewModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Компонент JPanel, отвечающий за отображение отрисованного изображения фрактала.
 * Слушает обновления от {@link FractalViewModel}, обрабатывает взаимодействия с мышью
 * (масштабирование выделением, панорамирование, вызов окна множества Жюлиа по двойному клику)
 * и управляет процессом рендеринга через {@link FractalRenderer}.
 * Реализует {@link PropertyChangeListener} для реакции на изменения состояния модели.
 */
public class FractalPanel extends JPanel implements PropertyChangeListener {

    /** ViewModel приложения, управляющая состоянием фрактала. */
    private final FractalViewModel viewModel;
    /**
     * Изображение фрактала. Используется {@code volatile} для обеспечения видимости
     * между потоком рендеринга и потоком диспетчеризации событий (EDT).
     */
    private volatile BufferedImage fractalImage;
    /** Рендерер, выполняющий вычисления и отрисовку фрактала в отдельном потоке. */
    private final FractalRenderer renderer;
    /** Слушатель для обработки масштабирования (зума) выделением области левой кнопкой мыши. */
    private final MouseZoomListener zoomListener;
    /** Слушатель для обработки панорамирования (перетаскивания) правой кнопкой мыши. */
    private final MousePanListener panListener;
    /**
     * Флаг, указывающий, идет ли процесс рендеринга в данный момент.
     * Используется {@code volatile} для потокобезопасного доступа из EDT и потока рендеринга.
     */
    private volatile boolean isRendering = false;
    /**
     * Статусное сообщение, отображаемое на панели (например, "Rendering...", "Ready.", координаты).
     * Используется {@code volatile} для потокобезопасного доступа.
     */
    private volatile String statusMessage = "Initializing...";

    /**
     * Конструирует FractalPanel.
     * Инициализирует слушателей мыши для масштабирования, панорамирования и открытия окна множества Жюлиа.
     * Добавляет слушателей для событий изменения размера/видимости компонента и изменений состояния ViewModel
     * для запуска рендеринга.
     *
     * @param viewModel ViewModel приложения. Не может быть null.
     * @param renderer  Рендерер фрактала. Не может быть null.
     */
    public FractalPanel(FractalViewModel viewModel, FractalRenderer renderer) {
        this.viewModel = viewModel;
        this.renderer = renderer;
        this.fractalImage = null; // Изображение изначально отсутствует

        // Подписываемся на изменения в ViewModel
        this.viewModel.addPropertyChangeListener(this);
        setBackground(Color.BLACK); // Цвет фона по умолчанию
        setOpaque(true); // Компонент непрозрачный

        // Инициализация слушателей мыши
        zoomListener = new MouseZoomListener(viewModel, this);
        panListener = new MousePanListener(viewModel, this);

        // Добавление слушателей событий мыши к панели
        addMouseListener(zoomListener);       // Нажатие/отпускание для зума
        addMouseMotionListener(zoomListener); // Перетаскивание для зума
        addMouseListener(panListener);       // Нажатие/отпускание для панорамирования
        addMouseMotionListener(panListener); // Перетаскивание для панорамирования

        // Слушатель для отслеживания изменения размера и момента первого отображения панели
        addComponentListener(new ComponentAdapter() {
            /**
             * Вызывается при изменении размера панели. Запускает рендеринг,
             * если панель видима и имеет корректные размеры.
             * @param e Событие изменения компонента.
             */
            @Override
            public void componentResized(ComponentEvent e) {
                 if (isShowing() && getWidth() > 0 && getHeight() > 0) {
                    System.out.println("Panel resized ("+ getWidth() + "x" + getHeight() +"). Triggering render.");
                    triggerRender(); // Запускаем перерисовку при изменении размера
                }
            }

            /**
             * Вызывается, когда панель становится видимой. Запускает рендеринг,
             * если панель имеет корректные размеры. Важно для инициализации
             * после того, как компоновщик определил размеры.
             * @param e Событие изменения компонента.
             */
            @Override
            public void componentShown(ComponentEvent e) {
                 if (getWidth() > 0 && getHeight() > 0) {
                   System.out.println("Panel shown ("+ getWidth() + "x" + getHeight() +"). Triggering render.");
                   triggerRender(); // Запускаем перерисовку при первом отображении
               }
            }
       });

       // Слушатель для обработки двойного клика левой кнопкой мыши для открытия окна Жюлиа
       addMouseListener(new MouseAdapter() {
            /**
             * Вызывается при клике мыши. Проверяет двойной клик левой кнопкой
             * и вызывает {@link #openJuliaSetWindow(Point)} для открытия
             * соответствующего окна множества Жюлиа.
             * @param e Событие мыши.
             */
            @Override
            public void mouseClicked(MouseEvent e) {
                // Проверяем двойной клик левой кнопкой
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    System.out.println("Double-click detected at: " + e.getPoint());
                    openJuliaSetWindow(e.getPoint()); // Открываем окно Жюлиа
                }
            }
        });
    }

    /**
     * Открывает окно для отображения множества Жюлиа, соответствующего комплексной
     * координате 'c', которая соответствует точке {@code screenPoint} на панели.
     * Получает текущую цветовую схему и количество итераций из {@link FractalViewModel}
     * для инициализации вида множества Жюлиа.
     *
     * @param screenPoint Точка на панели (в экранных координатах), где произошел двойной клик.
     */
    private void openJuliaSetWindow(Point screenPoint) {
        int w = getWidth();
        int h = getHeight();
        FractalState currentState = viewModel.getCurrentState(); // Получаем текущее состояние

        // Проверка валидности размеров панели и состояния
        if (w <= 0 || h <= 0 || currentState == null) {
            System.err.println("Cannot open Julia Set: Panel size or state invalid.");
            JOptionPane.showMessageDialog(this,
                "Cannot open Julia Set window.\nPanel size or fractal state is invalid.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Преобразуем экранные координаты клика в комплексное число 'c'
        ComplexNumber c = CoordinateConverter.screenToComplex(
            screenPoint.x, screenPoint.y, w, h, currentState.getViewport()
        );

        // Проверка успешности конвертации
        if (c == null) {
            System.err.println("Cannot open Julia Set: Failed to convert screen coordinates.");
             JOptionPane.showMessageDialog(this,
                "Cannot open Julia Set window.\nFailed to determine complex coordinates for the clicked point.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Получаем текущие настройки для передачи в окно Жюлиа
        ColorScheme currentScheme = currentState.getColorScheme();
        int currentIterations = currentState.getMaxIterations();

        // Находим родительское окно JFrame для позиционирования окна Жюлиа
        JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(this);
        if (owner == null) {
             System.err.println("Cannot determine owner frame for Julia Set window. It will be centered on screen.");
             // Если родительское окно не найдено, окно Жюлиа будет центрировано на экране (setLocationRelativeTo(null))
             owner = null;
        }

        // Создаем и отображаем окно Жюлиа в потоке диспетчеризации событий (EDT)
        // Используем final переменную для лямбда-выражения
        final JFrame finalOwner = owner;
        SwingUtilities.invokeLater(() -> {
            JuliaSetWindow juliaWindow = new JuliaSetWindow(finalOwner, c, currentScheme, currentIterations);
            juliaWindow.display(); // Показываем окно
        });
    }


    /**
     * Инициирует асинхронный процесс рендеринга фрактала.
     * Использует текущее состояние {@link FractalViewModel} и текущие размеры панели.
     * Отменяет любой предыдущий незавершенный процесс рендеринга перед запуском нового.
     * Обновляет статусное сообщение и запрашивает перерисовку панели для отображения индикатора загрузки.
     */
    public void triggerRender() {
        int width = getWidth();
        int height = getHeight();

        // Пропускаем рендеринг, если панель не готова или имеет некорректные размеры
        if (!isShowing() || width <= 0 || height <= 0 || renderer == null) {
            System.out.println("Skipping render: Size " + width + "x" + height + ", Showing: " + isShowing() + ", Renderer valid: " + (renderer!=null));
            statusMessage = (width <= 0 || height <= 0) ? "Panel size invalid." : "Panel not ready.";
            isRendering = false;
            repaint(); // Перерисовываем, чтобы показать статусное сообщение
            return;
        }

        System.out.println("Triggering render for size: " + width + "x" + height);
        isRendering = true; // Устанавливаем флаг рендеринга
        statusMessage = "Rendering..."; // Обновляем статус
        repaint(); // Перерисовываем для отображения "Rendering..." и индикатора загрузки

        FractalState currentState = viewModel.getCurrentState();
        // Проверка, что состояние доступно
        if (currentState == null) {
            System.err.println("Cannot render: Current state is null.");
            statusMessage = "Error: State is null.";
            isRendering = false;
            repaint();
            return;
        }

        // Вызываем асинхронный метод рендерера
        renderer.render(currentState, width, height,
                // Callback, вызываемый при успешном завершении рендеринга (в EDT)
                newImage -> {
                    isRendering = false; // Сбрасываем флаг рендеринга
                    if (newImage != null) {
                        this.fractalImage = newImage; // Обновляем изображение фрактала
                        // Обновляем статусное сообщение с деталями текущего вида
                        FractalState completedState = viewModel.getCurrentState(); // Получаем состояние на момент завершения
                        statusMessage = String.format("Ready. Viewport: X=[%.4g, %.4g], Y=[%.4g, %.4g], Iter: %d",
                                completedState.getViewport().getMinX(), completedState.getViewport().getMaxX(),
                                completedState.getViewport().getMinY(), completedState.getViewport().getMaxY(),
                                completedState.getMaxIterations());
                    } else {
                        // Обработка ошибки рендеринга
                        statusMessage = "Error during rendering.";
                        this.fractalImage = null; // Убираем старое изображение
                    }
                    repaint(); // Перерисовываем панель с новым изображением или статусом ошибки
                },
                // Callback, вызываемый при отмене рендеринга (в EDT)
                () -> {
                    isRendering = false; // Сбрасываем флаг рендеринга
                    FractalState cancelledState = viewModel.getCurrentState(); // Получаем состояние на момент отмены
                    // Обновляем статус, указывая на отмену и параметры на тот момент
                     if (cancelledState != null) { // Добавим проверку, вдруг состояние успело стать null
                        statusMessage = String.format("Cancelled. Viewport: X=[%.4g, %.4g], Y=[%.4g, %.4g], Iter: %d",
                                cancelledState.getViewport().getMinX(), cancelledState.getViewport().getMaxX(),
                                cancelledState.getViewport().getMinY(), cancelledState.getViewport().getMaxY(),
                                cancelledState.getMaxIterations());
                     } else {
                         statusMessage = "Cancelled. (State unavailable)";
                     }
                    // Не очищаем изображение, оставляем то, что успело отрисоваться или было до этого
                    repaint(); // Перерисовываем для обновления статусного сообщения
                }
        );
    }

    /**
     * Отрисовывает содержимое компонента.
     * Вызывается системой Swing при необходимости перерисовки панели (например, после вызова repaint()).
     * Рисует фон, затем актуальное изображение фрактала (если доступно) или статусное сообщение.
     * Если идет рендеринг, поверх рисуется индикатор загрузки.
     * Также рисует прямоугольник выделения для зума, если пользователь его рисует.
     *
     * @param g Графический контекст для отрисовки.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Отрисовка фона панели

        // Атомарно получаем текущие значения volatile переменных
        BufferedImage currentImage = this.fractalImage;
        boolean renderingNow = this.isRendering;
        String currentStatus = this.statusMessage;

        // Рисуем изображение фрактала, если оно есть
        if (currentImage != null) {
            g.drawImage(currentImage, 0, 0, this);
        } else {
            // Если изображения нет, рисуем фон и статусное сообщение по центру
            g.setColor(Color.DARK_GRAY); // Цвет фона для текста
            g.fillRect(0, 0, getWidth(), getHeight()); // Заливаем фон, чтобы скрыть возможные артефакты
            g.setColor(Color.WHITE); // Цвет текста
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            FontMetrics fm = g.getFontMetrics();
            String message = currentStatus != null ? currentStatus : "Status unavailable";
            int stringWidth = fm.stringWidth(message);
            int stringAscent = fm.getAscent();
            // Центрируем текст по горизонтали и вертикали
            int x = (getWidth() - stringWidth) / 2;
            int y = (getHeight() - stringAscent) / 2 + fm.getAscent(); // Более точное центрирование по вертикали
            g.drawString(message, x, y);
        }

        // Рисуем индикатор загрузки поверх всего, если идет рендеринг
        if (renderingNow) {
            drawLoadingIndicator(g);
        }

        // Рисуем прямоугольник выделения для зума (если пользователь тянет мышь)
        // Метод drawSelectionRectangle сам проверит, нужно ли рисовать
        zoomListener.drawSelectionRectangle(g);
    }

    /**
     * Отрисовывает индикатор загрузки: полупрозрачный прямоугольник с текстом "Rendering...".
     *
     * @param g Графический контекст для отрисовки.
     */
    private void drawLoadingIndicator(Graphics g) {
        // Сохраняем текущие настройки Graphics
        Graphics2D g2d = (Graphics2D) g.create();
        try {
            // Полупрозрачный черный фон для индикатора
            g2d.setColor(new Color(0, 0, 0, 150)); // Черный с альфа-каналом
            int indicatorHeight = 40;
            int indicatorY = getHeight() / 2 - indicatorHeight / 2; // Центрируем по вертикали
            g2d.fillRect(0, indicatorY, getWidth(), indicatorHeight);

            // Текст "Rendering..." белым цветом
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
            FontMetrics fm = g2d.getFontMetrics();
            String loadingText = "Rendering...";
            int stringWidth = fm.stringWidth(loadingText);
            // Центрируем текст внутри прямоугольника индикатора
            int textX = (getWidth() - stringWidth) / 2;
            int textY = indicatorY + (indicatorHeight - fm.getHeight()) / 2 + fm.getAscent();
            g2d.drawString(loadingText, textX, textY);
        } finally {
            g2d.dispose(); // Восстанавливаем исходные настройки Graphics
        }
    }

    /**
     * Обрабатывает события изменения свойств, поступающие от {@link FractalViewModel}.
     * Реагирует на изменение свойства {@code FractalViewModel.PROPERTY_STATE},
     * запуская перерисовку фрактала {@link #triggerRender()}.
     *
     * @param evt Событие изменения свойства {@link PropertyChangeEvent}.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // Проверяем, изменилось ли свойство состояния фрактала
        if (FractalViewModel.PROPERTY_STATE.equals(evt.getPropertyName())) {
            System.out.println("FractalPanel received state update. Triggering render.");
            // Запускаем рендеринг. Если мы уже в EDT, вызываем напрямую,
            // иначе - через invokeLater для безопасности Swing.
             if (SwingUtilities.isEventDispatchThread()) {
                 triggerRender();
             } else {
                 SwingUtilities.invokeLater(this::triggerRender);
             }
        }
    }

    /**
     * Возвращает предпочтительный размер для этой панели.
     * Используется менеджерами компоновки Swing.
     *
     * @return Рекомендуемый размер {@link Dimension} (800x600).
     */
    @Override
    public Dimension getPreferredSize() {
        // Задаем стандартный начальный размер
        return new Dimension(800, 600);
    }

    /**
     * Возвращает текущее отображаемое (или только что отрисованное) изображение фрактала.
     * Может быть использовано, например, для сохранения изображения в файл.
     * Выполняет volatile чтение поля {@code fractalImage}.
     *
     * @return Текущий {@link BufferedImage}, или {@code null}, если изображение еще не
     *         отрисовано или произошла ошибка рендеринга.
     */
    public BufferedImage getCurrentImage() {
        // Возвращаем volatile ссылку на текущее изображение
        return this.fractalImage;
    }
}