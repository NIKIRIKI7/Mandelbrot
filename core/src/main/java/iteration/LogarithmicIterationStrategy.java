package iteration;

/**
 * Логарифмическая стратегия расчёта итераций:
 * <pre>
 *     new = max(initial, ceil(initial * (1 + log10(zoom) * k)))
 * </pre>
 * <p>Если zoomLevelFactor < 1 (отдаление), значение не уменьшается ниже initialIterations.</p>
 */
public class LogarithmicIterationStrategy implements IterationStrategy {

    private double sensitivity;
    private final int maxCap;

    /**
     * @param sensitivity множитель при log10(zoom), контролирует скорость роста.
     * @param maxCap      верхний предел, чтобы избежать чрезмерных значений (<=0 — без ограничения).
     */
    public LogarithmicIterationStrategy(double sensitivity, int maxCap) {
        this.sensitivity = sensitivity;
        this.maxCap = maxCap;
    }
    
    /**
     * Устанавливает коэффициент чувствительности для расчёта итераций.
     * 
     * @param sensitivity новое значение коэффициента (рекомендуется 0.1-2.0)
     */
    public void setSensitivity(double sensitivity) {
        if (sensitivity < 0) {
            throw new IllegalArgumentException("Коэффициент чувствительности не может быть отрицательным");
        }
        this.sensitivity = sensitivity;
    }
    
    /**
     * @return текущий коэффициент чувствительности
     */
    public double getSensitivity() {
        return sensitivity;
    }

    public LogarithmicIterationStrategy(double sensitivity) {
        this(sensitivity, 2_000_000); // дефолтный лимит 2M
    }

    @Override
    public int calculate(int initialIterations, double zoomLevelFactor) {
        if (initialIterations <= 0) {
            throw new IllegalArgumentException("initialIterations must be >0");
        }
        if (zoomLevelFactor <= 0) {
            return initialIterations;
        }
        double delta = Math.log10(zoomLevelFactor) * sensitivity;
        int result = (int) Math.ceil(initialIterations * (1 + delta));
        if (result < initialIterations) {
            result = initialIterations; // не уменьшаем при отдалении
        }
        if (maxCap > 0 && result > maxCap) {
            return maxCap;
        }
        return result;
    }
}
