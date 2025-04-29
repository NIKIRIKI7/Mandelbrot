package iteration;

/**
 * Стратегия расчёта нового значения <code>maxIterations</code> при изменении масштаба (зума).
 * <p>Позволяет подменять алгоритмы без изменения клиентского кода (паттерн Strategy).</p>
 */
public interface IterationStrategy {
    /**
     * Рассчитать рекомендуемое количество итераций после зума.
     *
     * @param initialIterations  текущее (до зума) значение maxIterations
     * @param zoomLevelFactor   коэффициент зума (>1 — приближение, <1 — отдаление)
     * @return новое значение maxIterations (должно быть >0)
     */
    int calculate(int initialIterations, double zoomLevelFactor);
}
