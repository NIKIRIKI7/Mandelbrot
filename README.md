# Mandelbrot Project

## Описание проекта

Проект Mandelbrot представляет собой приложение для визуализации фракталов, таких как множество Мандельброта и множество Жюлиа. Приложение позволяет пользователю создавать анимации, настраивать параметры фракталов и сохранять результаты.

## Архитектура приложения

Проект состоит из нескольких модулей:

- **app**: Основной модуль приложения, содержащий графический интерфейс пользователя (GUI).
- **core**: Ядро приложения, содержащее основные математические функции и модели.
- **coordinate-converter**: Модуль для конвертации координат.
- **merge-java.sh**: Скрипт для объединения Java-файлов.

### Модуль app

Модуль `app` содержит следующие компоненты:

- **MainFrame.java**: Основное окно приложения.
- **FractalPanel.java**: Панель для отображения фракталов.
- **JuliaSetWindow.java**: Окно для настройки множества Жюлиа.
- **KeyframeListPanel.java**: Панель для управления ключевыми кадрами.
- **KeyframeParametersPanel.java**: Панель для настройки параметров ключевых кадров.
- **KeyframePreviewPanel.java**: Панель для предварительного просмотра ключевых кадров.
- **MenuBar.java**: Меню приложения.
- **StatusBar.java**: Статусная строка приложения.
- **AnimationSettingsPanel.java**: Панель для настройки анимации.
- **AnimationSetupDialog.java**: Диалог для настройки анимации.
- **GenerationControlPanel.java**: Панель для управления генерацией фракталов.

### Модуль core

Модуль `core` содержит следующие компоненты:

- **FractalFunction.java**: Базовый класс для математических функций фракталов.
- **JuliaFunction.java**: Класс для вычисления множества Жюлиа.
- **MandelbrotFunction.java**: Класс для вычисления множества Мандельброта.
- **ColorScheme.java**: Базовый класс для схем окрашивания.
- **FractalState.java**: Класс для хранения состояния фрактала.
- **GrayscaleScheme.java**: Схема окрашивания в оттенках серого.
- **NonlinearRGBScheme.java**: Схема окрашивания с нелинейной RGB-шкалой.
- **Viewport.java**: Класс для управления областью просмотра.
- **FractalRenderer.java**: Класс для рендеринга фракталов.
- **Tile.java**: Класс для представления тайлов.
- **TileCalculator.java**: Класс для вычисления тайлов.
- **AnimationService.java**: Сервис для управления анимацией.
- **FileService.java**: Сервис для работы с файлами.
- **ComplexNumber.java**: Класс для работы с комплексными числами.
- **CoordinateConverter.java**: Класс для конвертации координат.
- **FractalViewModel.java**: Модель представления фракталов.
- **Command.java**: Базовый класс для команд.
- **PanCommand.java**: Команда для панорамирования.
- **UndoManager.java**: Менеджер отмены команд.
- **ZoomCommand.java**: Команда для масштабирования.

## Математическая теория

### Множество Мандельброта

Множество Мандельброта определяется следующим образом:
\[ z_{n+1} = z_n^2 + c \]
где \( z \) и \( c \) — комплексные числа, а \( z_0 = 0 \). Если последовательность \( z_n \) остается ограниченной, то \( c \) принадлежит множеству Мандельброта.

### Множество Жюлиа

Множество Жюлиа определяется следующим образом:
\[ z_{n+1} = z_n^2 + c \]
где \( z \) и \( c \) — комплексные числа, а \( z_0 \) — начальное значение. Если последовательность \( z_n \) остается ограниченной, то \( z_0 \) принадлежит множеству Жюлиа.

## Инструкции по установке и использованию

### Установка

1. Клонируйте репозиторий:
   ```sh
   git clone https://github.com/your-username/mandelbrot.git
   ```

2. Перейдите в директорию проекта:
   ```sh
   cd mandelbrot
   ```

3. Соберите проект с помощью Maven:
   ```sh
   mvn clean install
   ```

### Использование

1. Запустите приложение:
   ```sh
   java -jar app/target/app-1.0-SNAPSHOT.jar
   ```

2. В главном окне приложения выберите фрактал, который хотите визуализировать (Мандельброт или Жюлиа).

3. Настройте параметры фрактала и анимации с помощью панелей управления.

4. Нажмите кнопку "Generate" для генерации фрактала.

5. Сохраните результаты в файл с помощью меню "File".

## Примеры кода

### Пример использования FractalRenderer

```java
import core.render.FractalRenderer;
import core.math.FractalFunction;
import core.math.MandelbrotFunction;
import core.model.Viewport;

public class Example {
    public static void main(String[] args) {
        FractalFunction function = new MandelbrotFunction();
        Viewport viewport = new Viewport(-2.0, 1.0, -1.5, 1.5, 800, 600);
        FractalRenderer renderer = new FractalRenderer(function, viewport);
        renderer.render();
    }
}
```

### Пример использования AnimationService

```java
import core.services.AnimationService;
import core.model.Keyframe;

public class Example {
    public static void main(String[] args) {
        AnimationService animationService = new AnimationService();
        Keyframe keyframe1 = new Keyframe(0, -2.0, 1.0, -1.5, 1.5, 800, 600);
        Keyframe keyframe2 = new Keyframe(100, -1.0, 0.5, -1.0, 1.0, 800, 600);
        animationService.addKeyframe(keyframe1);
        animationService.addKeyframe(keyframe2);
        animationService.generateAnimation();
    }
}
```

## Скриншоты

![Главное окно приложения](screenshots/main_window.png)
![Настройка анимации](screenshots/animation_setup.png)

## Лицензия

Проект распространяется под лицензией MIT. Подробности см. в файле [LICENSE](LICENSE).

## Авторы

- **Имя Фамилия** - [GitHub](https://github.com/your-username)

## Контакты

Если у вас есть вопросы или предложения, пожалуйста, свяжитесь со мной по адресу [email@example.com](mailto:email@example.com).
