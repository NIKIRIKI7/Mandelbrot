// File: app/src/main/java/view/KeyframePreviewPanel.java
package view;

import model.FractalState;
import render.FractalRenderer;
import viewmodel.FractalViewModel;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Панель, предназначенная для интерактивного предпросмотра и редактирования
 * одного ключевого кадра анимации. Содержит {@link FractalPanel} с собственными
 * {@link FractalViewModel} и {@link FractalRenderer}, а также кнопки для загрузки
 * состояния выбранного кадра и для обновления выбранного кадра текущим состоянием предпросмотра.
 */
public class KeyframePreviewPanel extends JPanel {

    private final FractalPanel previewPanel;
    private final FractalViewModel previewViewModel;
    private final FractalRenderer previewRenderer;
    private final JButton loadToPreviewButton;
    private final JButton updateKeyframeButton;

    /**
     * Создает панель предпросмотра ключевого кадра.
     *
     * @param loadAction Слушатель {@link ActionListener} для кнопки "Загрузить сюда".
     *                   Вызывается, когда пользователь хочет загрузить состояние
     *                   выбранного в списке кадра в эту панель.
     * @param updateAction Слушатель {@link ActionListener} для кнопки "Запомнить вид в кадр".
     *                     Вызывается, когда пользователь хочет обновить выбранный в списке
     *                     кадр текущим состоянием этой панели предпросмотра.
     */
    public KeyframePreviewPanel(ActionListener loadAction, ActionListener updateAction) {
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


        add(previewPanel, BorderLayout.CENTER);

        // Кнопки управления
        loadToPreviewButton = new JButton("Загрузить сюда");
        loadToPreviewButton.setToolTipText("Загрузить состояние выделенного кадра для предпросмотра и редактирования");
        loadToPreviewButton.setEnabled(false); // Активна только при выборе кадра в списке
        if (loadAction != null) {
            loadToPreviewButton.addActionListener(loadAction);
        }

        updateKeyframeButton = new JButton("Запомнить вид в кадр"); // <-- Переименовано
        updateKeyframeButton.setToolTipText("Обновить выделенный ключевой кадр текущим видом из этой панели предпросмотра"); // <-- Уточнен тултип
        updateKeyframeButton.setEnabled(false); // Активна только при выборе кадра в списке
        if (updateAction != null) {
            updateKeyframeButton.addActionListener(updateAction);
        }

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonsPanel.add(loadToPreviewButton);
        buttonsPanel.add(updateKeyframeButton);
        add(buttonsPanel, BorderLayout.SOUTH);
    }

    /**
     * Загружает указанное состояние фрактала в {@link FractalViewModel} этой панели предпросмотра.
     * Это вызовет перерисовку {@link FractalPanel}.
     *
     * @param state Состояние {@link FractalState} для загрузки и отображения. Если null,
     *              панель может отобразить состояние по умолчанию или остаться пустой.
     */
    public void loadState(FractalState state) {
        previewViewModel.loadState(Objects.requireNonNullElseGet(state, FractalState::createDefault));
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
     * Устанавливает состояние активности (enabled/disabled) для кнопок
     * "Загрузить сюда" и "Запомнить вид в кадр".
     * Обычно вызывается при изменении выбора в списке ключевых кадров.
     *
     * @param enabled {@code true}, чтобы сделать кнопки активными, {@code false} - неактивными.
     */
    public void setControlButtonsEnabled(boolean enabled) {
        loadToPreviewButton.setEnabled(enabled);
        updateKeyframeButton.setEnabled(enabled);
    }

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
        // Активность кнопок управляется через setControlButtonsEnabled в зависимости от выбора в списке
        // Но если панель выключается глобально, кнопки тоже должны выключиться
        if (!enabled) {
            loadToPreviewButton.setEnabled(false);
            updateKeyframeButton.setEnabled(false);
        } else {
            // При включении состояние кнопок зависит от внешнего фактора (выбора кадра)
            // Метод setUIEnabled в диалоге должен вызвать setControlButtonsEnabled
        }
    }
}