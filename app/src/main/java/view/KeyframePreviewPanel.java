// File: app/src/main/java/view/KeyframePreviewPanel.java
package view;

import model.FractalState;
import model.Viewport; // Добавлен импорт
import render.FractalRenderer;
import viewmodel.FractalViewModel;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Панель, предназначенная для интерактивного предпросмотра и редактирования
 * одного ключевого кадра анимации. Содержит {@link FractalPanel} с собственными
 * {@link FractalViewModel} и {@link FractalRenderer}, а также кнопку для загрузки
 * состояния выбранного кадра.
 * Изменения в предпросмотре (масштаб, перемещение) автоматически применяются
 * к выбранному кадру.
 */
public class KeyframePreviewPanel extends JPanel {

    private final FractalPanel previewPanel;
    private final FractalViewModel previewViewModel;
    private final FractalRenderer previewRenderer;
    // Кнопки удалены по запросу пользователя
    // private final JButton loadToPreviewButton;
    // private final JButton undoButton;
    private Consumer<model.Viewport> viewportChangeListener; // Слушатель для уведомления об изменениях

    /**
     * Создает панель предпросмотра ключевого кадра.
     *
     * @param viewportChangeListener Слушатель, который будет уведомлен об изменении Viewport в этой панели.
     */
    public KeyframePreviewPanel(Consumer<model.Viewport> viewportChangeListener) {
        this.viewportChangeListener = viewportChangeListener;
        setLayout(new BorderLayout(5, 5));
        setBorder(new TitledBorder("Предпросмотр и интерактивное редактирование"));

        // Создание изолированных компонентов для предпросмотра
        previewRenderer = new FractalRenderer();
        previewViewModel = new FractalViewModel(previewRenderer, 1); // История Undo не нужна (1 - минимально)
        // Панель предпросмотра не нуждается в доступе к главному StatusBar, создаем ее напрямую
        previewPanel = new FractalPanel(previewViewModel, previewRenderer, null) { // mainFrame = null
            // Переопределяем, чтобы панель предпросмотра не пыталась использовать StatusBar главного окна
            @Override
            public void triggerRender() {
                int width = getWidth();
                int height = getHeight();

                // Используем protected поле renderer напрямую, так как мы в подклассе
                if (!isShowing() || width <= 0 || height <= 0 || renderer == null) {
                    System.out.println("Пропуск рендера предпросмотра: " + width + "x" + height);
                    // Не обновляем StatusBar, так как его нет
                    super.isRendering = false; // Используем protected поле родителя
                    repaint();
                    return;
                }

                System.out.println("Запуск рендера предпросмотра для размера: " + width + "x" + height);
                super.isRendering = true; // Используем protected поле родителя
                repaint(); // Показываем просто фон, без статуса

                FractalState currentState = previewViewModel.getCurrentState();
                if (currentState == null) {
                    System.err.println("Невозможно рендерить предпросмотр: состояние null.");
                    super.isRendering = false; // Используем protected поле родителя
                    repaint();
                    return;
                }

                // Колбэки для предпросмотра (без StatusBar и прогресса)
                Consumer<BufferedImage> onComplete = newImage -> {
                    super.isRendering = false; // Используем protected поле родителя
                    if (newImage != null) {
                        super.fractalImage = newImage; // Используем protected поле родителя
                    } else {
                        super.fractalImage = null;     // Используем protected поле родителя
                    }
                    repaint();
                };
                Runnable onCancel = () -> {
                    super.isRendering = false; // Используем protected поле родителя
                    repaint();
                };
                Runnable progressUpdater = () -> {}; // Пустой колбэк прогресса

                // Используем renderer текущего объекта (он же previewRenderer)
                renderer.render(currentState, width, height, onComplete, onCancel, progressUpdater);
            }

            // Отключаем обработку двойного клика для окна Жюлиа в предпросмотре
            @Override
            protected void processMouseEvent(MouseEvent e) {
                if (e.getID() == MouseEvent.MOUSE_CLICKED && e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    // Игнорируем двойной клик
                    e.consume();
                    return;
                }
                super.processMouseEvent(e);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(400, 300); // Уменьшили размер по умолчанию
            }
        };

        // Добавляем слушатель изменений Viewport во ViewModel предпросмотра
        previewViewModel.addViewportChangeListener((Viewport newViewport) -> { // Явно указываем тип
            if (this.viewportChangeListener != null) {
                this.viewportChangeListener.accept(newViewport); // Приведение типа больше не нужно
            }
        });

        add(previewPanel, BorderLayout.CENTER);

        // Кнопки удалены по запросу пользователя
        // loadToPreviewButton = new JButton("Загрузить сюда");
        // loadToPreviewButton.setToolTipText("Загрузить состояние выделенного кадра для предпросмотра и редактирования");
        // loadToPreviewButton.setEnabled(false); // Активна только при выборе кадра в списке

        // undoButton = new JButton("Откатить");
        // undoButton.setToolTipText("Отменить последнее изменение масштаба или панорамирования в этом предпросмотре");
        // undoButton.setEnabled(false); // Изначально недоступна
        // undoButton.addActionListener(e -> previewViewModel.undoLastAction());

        // JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        // buttonsPanel.add(loadToPreviewButton);
        // buttonsPanel.add(undoButton);
        // add(buttonsPanel, BorderLayout.SOUTH);

        // Добавляем слушатель для управления доступностью кнопки Undo
        // previewViewModel.addPropertyChangeListener(evt -> { // Добавляем универсального слушателя
        //     // Проверяем, что это нужное нам свойство
        //     if (FractalViewModel.PROPERTY_CAN_UNDO.equals(evt.getPropertyName())) {
        //         // Проверяем тип нового значения и обновляем состояние кнопки
        //         if (evt.getNewValue() instanceof Boolean) {
        //             undoButton.setEnabled((Boolean) evt.getNewValue());
        //         }
        //     }
        // });
    }

    /**
     * Загружает состояние фрактала для предпросмотра.
     * @param state Состояние для отображения, или null для очистки.
     */
    public void loadState(FractalState state) {
        if (state != null) {
            previewViewModel.loadState(state);
            System.out.println("Состояние загружено: " + state.toString());
        } else {
            System.out.println("Состояние загружено: null");
        }
        // Автоматически запускаем рендер после загрузки состояния
        System.out.println("Размеры панели предпросмотра: " + previewPanel.getWidth() + "x" + previewPanel.getHeight());
        if (previewPanel.getWidth() <= 0 || previewPanel.getHeight() <= 0) {
            System.out.println("Пропуск рендера предпросмотра: " + previewPanel.getWidth() + "x" + previewPanel.getHeight());
        } else {
            previewPanel.triggerRender();
        }
    }

    /**
     * Возвращает текущее состояние фрактала, отображаемое в панели предпросмотра.
     *
     * @return Текущий {@link FractalState} из {@link FractalViewModel} предпросмотра.
     */
    public FractalState getCurrentState() {
        return previewViewModel.getCurrentState();
    }

    /**
     * Устанавливает состояние активности (enabled/disabled) для кнопки
     * "Загрузить сюда".
     * Обычно вызывается при изменении выбора в списке ключевых кадров.
     *
     * @param enabled {@code true}, чтобы сделать кнопку активной, {@code false} - неактивной.
     */
    // public void setLoadButtonEnabled(boolean enabled) {
    //     loadToPreviewButton.setEnabled(enabled);
    // }

    /**
     * Останавливает потоки рендерера {@link FractalRenderer}, связанного с этой панелью предпросмотра.
     * Этот метод **необходимо** вызывать при закрытии родительского окна или диалога.
     */
    public void shutdownRenderer() {
        System.out.println("Завершение работы рендерера предпросмотра (из KeyframePreviewPanel)...");
        if (previewRenderer != null) {
            previewRenderer.shutdown();
        }
        System.out.println("Рендерер предпросмотра остановлен (из KeyframePreviewPanel).");
    }

    /**
     * Переопределяем setEnabled для блокировки всех интерактивных компонентов панели.
     * @param enabled {@code true} для включения, {@code false} для выключения.
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        previewPanel.setEnabled(enabled); // Блокируем саму панель фрактала
        // Активность кнопки управляется через setLoadButtonEnabled в зависимости от выбора в списке
        // Но если панель выключается глобально, кнопка тоже должна выключиться
        // if (!enabled) {
        //     loadToPreviewButton.setEnabled(false);
        //     undoButton.setEnabled(false);
        // } else {
        //     // При включении состояние кнопки загрузки зависит от внешнего фактора (выбора кадра)
        //     // Метод setUIEnabled в диалоге должен вызвать setLoadButtonEnabled
        // }
    }

    /**
     * Добавляет слушателя изменений Viewport.
     * @param listener слушатель
     */
    public void setViewportChangeListener(Consumer<model.Viewport> listener) {
        this.viewportChangeListener = listener;
    }

    /**
     * Возвращает текущий ViewModel панели предпросмотра.
     * @return FractalViewModel
     */
    public FractalViewModel getPreviewViewModel() {
        return previewViewModel;
    }
}