// File: app/src/main/java/view/KeyframePreviewPanel.java
package view;

import model.FractalState;
import render.FractalRenderer;
import viewmodel.FractalViewModel;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Objects;

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
     * @param updateAction Слушатель {@link ActionListener} для кнопки "Обновить кадр из предпросмотра".
     *                     Вызывается, когда пользователь хочет обновить выбранный в списке
     *                     кадр текущим состоянием этой панели предпросмотра.
     */
    public KeyframePreviewPanel(ActionListener loadAction, ActionListener updateAction) {
        setLayout(new BorderLayout(5, 5));
        setBorder(new TitledBorder("Предпросмотр и интерактивное редактирование"));

        // Создание изолированных компонентов для предпросмотра
        previewRenderer = new FractalRenderer();
        previewViewModel = new FractalViewModel(previewRenderer, 1); // История Undo не нужна (1 - минимально)
        previewPanel = new FractalPanel(previewViewModel, previewRenderer);
        // Устанавливаем предпочтительный размер для панели предпросмотра
        previewPanel.setPreferredSize(new Dimension(450, 350));

        add(previewPanel, BorderLayout.CENTER);

        // Кнопки управления
        loadToPreviewButton = new JButton("Загрузить сюда");
        loadToPreviewButton.setToolTipText("Загружает состояние выбранного в списке кадра в эту панель");
        loadToPreviewButton.setEnabled(false); // Активна только при выборе кадра в списке
        if (loadAction != null) {
            loadToPreviewButton.addActionListener(loadAction);
        }

        updateKeyframeButton = new JButton("Обновить кадр из предпросмотра");
        updateKeyframeButton.setToolTipText("Обновляет выбранный в списке кадр текущим состоянием этой панели");
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
     *              панель может отобразить состояние по умолчанию или остаться пустой (текущая реализация ViewModel загрузит дефолт).
     */
    public void loadState(FractalState state) {
        previewViewModel.loadState(Objects.requireNonNullElseGet(state, FractalState::createDefault));
        // Если state == null, загружаем состояние по умолчанию
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
     * "Загрузить сюда" и "Обновить кадр из предпросмотра".
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
     * Этот метод **необходимо** вызывать при закрытии родительского окна или диалога,
     * чтобы предотвратить утечку ресурсов и потоков.
     */
    public void shutdownRenderer() {
        System.out.println("Завершение работы рендерера предпросмотра (из KeyframePreviewPanel)...");
        if (previewRenderer != null) {
            previewRenderer.shutdown();
        }
        System.out.println("Рендерер предпросмотра остановлен (из KeyframePreviewPanel).");
    }
}