// File: core/src/main/java/model/Keyframe.java
package model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Представляет ключевой кадр для анимации фрактала.
 * Содержит состояние фрактала {@link FractalState} в этот момент
 * и необязательное имя для идентификации в UI.
 * Реализует {@link Serializable} для возможного сохранения/загрузки настроек анимации.
 */
public class Keyframe implements Serializable {
    private static final long serialVersionUID = 1L;

    private final FractalState state;
    private String name; // Имя кадра (может быть изменено пользователем)

    /**
     * Создает ключевой кадр с заданным состоянием и именем по умолчанию.
     * Имя по умолчанию генерируется на основе Viewport и итераций.
     * @param state Состояние фрактала для этого кадра. Не может быть null.
     */
    public Keyframe(FractalState state) {
        this(state, generateDefaultName(state));
    }

    /**
     * Создает ключевой кадр с заданным состоянием и именем.
     * @param state Состояние фрактала для этого кадра. Не может быть null.
     * @param name Имя кадра.
     */
    public Keyframe(FractalState state, String name) {
        this.state = Objects.requireNonNull(state, "Состояние FractalState не может быть null");
        this.name = (name != null && !name.isBlank()) ? name : generateDefaultName(state);
    }

    /**
     * Генерирует имя по умолчанию для кадра на основе его состояния.
     * @param state Состояние кадра.
     * @return Строка с описанием вида.
     */
    private static String generateDefaultName(FractalState state) {
        if (state == null || state.getViewport() == null) {
            return "Invalid State";
        }
        Viewport vp = state.getViewport();
        // Генерируем краткое имя, например, центр и ширина
        double centerX = (vp.getMinX() + vp.getMaxX()) / 2.0;
        double centerY = (vp.getMinY() + vp.getMaxY()) / 2.0;
        return String.format("Center=(%.3g, %.3g), W=%.3g, Iter=%d",
                             centerX, centerY, vp.getWidth(), state.getMaxIterations());
    }

    /**
     * Возвращает состояние фрактала для этого ключевого кадра.
     * @return {@link FractalState}.
     */
    public FractalState getState() {
        return state;
    }

    /**
     * Возвращает имя ключевого кадра.
     * @return Имя кадра.
     */
    public String getName() {
        return name;
    }

    /**
     * Устанавливает новое имя для ключевого кадра.
     * @param name Новое имя. Если null или пустое, имя не меняется.
     */
    public void setName(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }

    /**
     * Возвращает имя кадра для отображения, например, в JList.
     * Переопределяем toString для удобства.
     * @return Имя кадра.
     */
    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Keyframe keyframe = (Keyframe) o;
        // Сравниваем по состоянию, имя может быть разным
        return Objects.equals(state, keyframe.state);
    }

    @Override
    public int hashCode() {
        // Хэш-код зависит только от состояния
        return Objects.hash(state);
    }
}