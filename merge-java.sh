#!/bin/bash

output_file="combined.txt"

# Удаляем предыдущий результат, если существует
rm -f "$output_file"

# Поиск и обработка Java-файлов (с поддержкой пробелов в именах)
find . -type f -name "*.java" -print0 | while IFS= read -r -d '' file; do
    # Добавляем заголовок с именем файла (без ./ в начале)
    printf "// File: %s\n\n" "${file#./}" >> "$output_file"
    
    # Добавляем содержимое файла
    cat "$file" >> "$output_file"
    
    # Добавляем разделитель между файлами
    printf "\n\n" >> "$output_file"
done

# Проверка результата
if [ -f "$output_file" ]; then
    echo "Все Java-файлы объединены в: $output_file"
else
    echo "Java-файлы не найдены"
    exit 1
fi