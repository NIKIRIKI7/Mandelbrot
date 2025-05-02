// File: app/src/main/java/view/FractalPanel.java
package view;

import listeners.MousePanListener;
import listeners.MouseZoomListener;
import model.ColorScheme;
import model.FractalState;
import render.FractalRenderer;
import utils.ComplexNumber;
import utils.Converter;
// Импорт ViewModel для управления состоянием фрактала
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Компонент JPanel, отвечающий за отображение фрактала.
 * Слушает обновления от {@link FractalViewModel}, обрабатывает взаимодействия с мышью
 * и управляет рендерингом через {@link FractalRenderer}.
 * Может опционально использовать StatusBar родительского (Window) SwingUtilities.getWindowAncestor(this).
 */
public class FractalPanel extends JPanel implements PropertyChangeListener {
    /**
     * Дополнительный конструктор для совместимости с MainFrame.
     * Использует дефолтный FractalRenderer.
     */
    public FractalPanel(FractalViewModel viewModel, StatusBar statusBar) {
        this(viewModel, new FractalRenderer(), statusBar);
    }


    // ViewModel для управления состоянием и логикой отображения фрактала
protected final FractalViewModel viewModel;
    protected volatile BufferedImage fractalImage; // <-- protected
    protected final FractalRenderer renderer; // <-- protected
    protected final MouseZoomListener zoomListener; // <-- protected
    protected final MousePanListener panListener; // <-- protected
    protected volatile boolean isRendering = false; // <-- protected
    protected final StatusBar statusBar;

    /**
     * Конструирует FractalPanel.
     *
     * @param viewModel ViewModel для управления состоянием
     * @param renderer рендерер фракталов
     * @param statusBar строка состояния
     */
    public FractalPanel(FractalViewModel viewModel, FractalRenderer renderer, StatusBar statusBar) {
        this.viewModel = viewModel;
        this.renderer = renderer;
        this.statusBar = statusBar;
        this.fractalImage = null;
        this.zoomListener = new MouseZoomListener(viewModel, this);
        this.panListener = new MousePanListener(viewModel, this);
        setFocusable(true);
        addMouseListener(zoomListener);
        addMouseMotionListener(zoomListener);
        addMouseListener(panListener);
        addMouseMotionListener(panListener);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (isShowing() && getWidth() > 0 && getHeight() > 0) {
                    System.out.println("Panel resized ("+ getWidth() + "x" + getHeight() +"). Triggering render.");
                    triggerRender();
                }
            }
            @Override
            public void componentShown(ComponentEvent e) {
                if (getWidth() > 0 && getHeight() > 0) {
                    System.out.println("Panel shown ("+ getWidth() + "x" + getHeight() +"). Triggering render.");
                    triggerRender();
                }
            }
        });

        viewModel.addPropertyChangeListener(this);
        setBackground(Color.BLACK);
        setOpaque(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    System.out.println("Double-click detected at: " + e.getPoint());
                    openJuliaSetWindow(e.getPoint());
                }
            }
        });

        // Начальный статус при инициализации (только если StatusBar есть)
        if ((Window) SwingUtilities.getWindowAncestor(this) != null) {
            statusBar.setStatus("Инициализация...");
        } else {
            System.out.println("FractalPanel: StatusBar не доступен, инициализация без статуса.");
        }
    }

    /**
     * Открывает окно для отображения множества Жюлиа.
     * @param screenPoint Точка на панели (в экранных координатах).
     */
    private void openJuliaSetWindow(Point screenPoint) {
        int w = getWidth();
        int h = getHeight();
        FractalState currentState = viewModel.getCurrentState();
        if (w <= 0 || h <= 0 || currentState == null) {
            String errorMsg = "Невозможно открыть окно Жюлиа: некорректный размер панели или состояние фрактала.";
            System.err.println(errorMsg);
            statusBar.setStatus("Ошибка: " + errorMsg);
            JOptionPane.showMessageDialog(this, errorMsg, "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ComplexNumber c = Converter.screenToComplex(screenPoint.x, screenPoint.y, w, h, currentState.getViewport());

        if (c == null) {
            String errorMsg = "Невозможно открыть окно Жюлиа: не удалось преобразовать координаты.";
            System.err.println(errorMsg);
            statusBar.setStatus("Ошибка: " + errorMsg);
            JOptionPane.showMessageDialog(this, errorMsg, "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ColorScheme currentScheme = currentState.getColorScheme();
        int currentIterations = currentState.getMaxIterations();
        JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(this);

        SwingUtilities.invokeLater(() -> {
            JuliaSetWindow juliaWindow = new JuliaSetWindow(owner, c, currentScheme, currentIterations);
            juliaWindow.display();
        });
    }


    /**
     * Инициирует асинхронный процесс рендеринга фрактала.
     * Обновляет статус и прогресс в {@link StatusBar}, если он доступен.
     */
    public void triggerRender() {
        int width = getWidth();
        int height = getHeight();
        if (statusBar != null) // Получаем StatusBar безопасно

        if (!isShowing() || width <= 0 || height <= 0 || renderer == null) {
            String msg = (width <= 0 || height <= 0) ? "Некорректный размер панели." : "Панель не готова.";
            System.out.println("Пропуск рендера: " + msg);
            if (statusBar != null) {
                statusBar.setStatus("Ошибка: " + msg);
                statusBar.setProgressVisible(false);
            }
            isRendering = false;
            repaint(); // Перерисовываем, чтобы показать фон или старое изображение
            return;
        }

        System.out.println("Запуск рендера для размера: " + width + "x" + height);
        isRendering = true;
        if (statusBar != null) {
            statusBar.setStatus("Рендеринг..."); // Обновляем статус в StatusBar
            statusBar.setProgress(0);         // Сбрасываем прогресс
            statusBar.setProgressVisible(true); // Показываем прогресс бар
        }
        repaint(); // Перерисовываем, чтобы показать старое изображение (если есть) без индикатора

        FractalState currentState = viewModel.getCurrentState();
        if (currentState == null) {
            System.err.println("Невозможно рендерить: текущее состояние null.");
            if (statusBar != null) {
                statusBar.setStatus("Ошибка: Состояние фрактала не доступно.");
                statusBar.setProgressVisible(false);
            }
            isRendering = false;
            repaint();
            return;
        }

        // --- Подготовка колбэков для рендерера ---
        AtomicInteger renderedTiles = new AtomicInteger(0);
        // Рассчитываем общее количество тайлов для прогресса (используем константу из рендерера TILE_SIZE)
        final int totalTiles = render.TileCalculator.calculateTiles(width, height, 32).size();

        // Callback для обновления прогресса (вызывается из потока рендеринга)
        Runnable progressUpdater = () -> {
            int completed = renderedTiles.incrementAndGet();
            int progressPercent = (totalTiles > 0) ? (int) (((double) completed / totalTiles) * 100.0) : 0;
            if (statusBar != null) { // Обновляем прогресс только если есть StatusBar
                statusBar.setProgress(progressPercent);
            }
        };

        // Callback при завершении рендеринга (вызывается рендерером в EDT)
        Consumer<BufferedImage> onComplete = newImage -> {
            isRendering = false;
            if (statusBar != null) statusBar.setProgressVisible(false); // Скрываем прогресс бар
            if (newImage != null) {
                this.fractalImage = newImage;
                FractalState completedState = viewModel.getCurrentState();
                // Проверка на null перед доступом к completedState (на всякий случай)
                if(completedState != null && completedState.getViewport() != null) {
                    String statusText = String.format("Готово. Область: X=[%.4g, %.4g], Y=[%.4g, %.4g], Итер: %d",
                            completedState.getViewport().getMinX(), completedState.getViewport().getMaxX(),
                            completedState.getViewport().getMinY(), completedState.getViewport().getMaxY(),
                            completedState.getMaxIterations());
                    if (statusBar != null) statusBar.setStatus(statusText);
                } else {
                    if (statusBar != null) statusBar.setStatus("Готово (состояние не доступно).");
                }
            } else {
                if (statusBar != null) statusBar.setStatus("Ошибка во время рендеринга.");
                this.fractalImage = null;
            }
            repaint(); // Перерисовываем панель с новым изображением или статусом ошибки
        };

        // Callback при отмене рендеринга (вызывается рендерером в EDT)
        Runnable onCancel = () -> {
            isRendering = false;
            if (statusBar != null) statusBar.setProgressVisible(false); // Скрываем прогресс бар
            FractalState cancelledState = viewModel.getCurrentState();
            String statusText;
            if (cancelledState != null && cancelledState.getViewport() != null) {
                statusText = String.format("Отменено. Область: X=[%.4g, %.4g], Y=[%.4g, %.4g], Итер: %d",
                        cancelledState.getViewport().getMinX(), cancelledState.getViewport().getMaxX(),
                        cancelledState.getViewport().getMinY(), cancelledState.getViewport().getMaxY(),
                        cancelledState.getMaxIterations());
            } else {
                statusText = "Отменено. (Состояние недоступно)";
            }
            if (statusBar != null) statusBar.setStatus(statusText);
            // Не очищаем изображение, оставляем то, что успело отрисоваться
            repaint(); // Перерисовываем для обновления статус бара (хотя панель не меняется)
        };

        // Вызываем асинхронный метод рендерера с новыми колбэками
        renderer.render(currentState, width, height, onComplete, onCancel, progressUpdater);
    }

    /**
     * Отрисовывает содержимое компонента.
     * @param g Графический контекст для отрисовки.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Отрисовка фона
        BufferedImage currentImage = this.fractalImage; // Атомарное чтение
        if (currentImage != null) {
            g.drawImage(currentImage, 0, 0, this);
        } else {
            // Если изображения нет, просто рисуем фон
            g.setColor(Color.BLACK); // Или другой фоновый цвет
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        // Рисуем прямоугольник выделения зума
        zoomListener.drawSelectionRectangle(g);
    }

    /**
     * Обрабатывает события изменения свойств от {@link FractalViewModel}.
     * @param evt Событие изменения свойства.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (FractalViewModel.PROPERTY_STATE.equals(evt.getPropertyName())) {
            System.out.println("FractalPanel получил обновление состояния. Запуск рендера.");
            // Запускаем рендеринг в EDT
            if (SwingUtilities.isEventDispatchThread()) {
                triggerRender();
            } else {
                SwingUtilities.invokeLater(this::triggerRender);
            }
            // Обновляем StatusBar после изменения состояния (если он есть и рендер не идет)
            if (statusBar != null)
            if (statusBar != null && !isRendering()) {
                FractalState newState = (FractalState) evt.getNewValue();
                if (newState != null && newState.getViewport() != null) {
                    statusBar.setStatus(
                            String.format("Готово. Область: X=[%.4g, %.4g], Y=[%.4g, %.4g], Итер: %d",
                                    newState.getViewport().getMinX(), newState.getViewport().getMaxX(),
                                    newState.getViewport().getMinY(), newState.getViewport().getMaxY(),
                                    newState.getMaxIterations())
                    );
                } else {
                    statusBar.setStatus("Готово (состояние не доступно).");
                }
            }
        }
        // Обработка PROPERTY_CAN_UNDO здесь не нужна, это делает MenuBar
    }

    /**
     * Возвращает предпочтительный размер панели.
     * @return Рекомендуемый размер (800x600).
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(800, 600);
    }

    /**
     * Возвращает текущее отображаемое изображение фрактала.
     * @return Текущий {@link BufferedImage}, или {@code null}.
     */
    public BufferedImage getCurrentImage() {
        return this.fractalImage;
    }

    /**
     * Проверяет, идет ли рендеринг в данный момент.
     * @return true, если рендеринг активен, иначе false.
     */
    public boolean isRendering() {
        return isRendering;
    }
}