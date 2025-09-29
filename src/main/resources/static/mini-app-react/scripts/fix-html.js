#!/usr/bin/env node

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Пути к файлам
const distDir = path.join(__dirname, '..', 'dist');
const sourceIndexPath = path.join(__dirname, '..', 'index.html');
const distIndexPath = path.join(distDir, 'index.html');

console.log('🔧 Исправляем HTML после сборки...');

try {
  // Читаем собранный index.html из dist
  const distHtml = fs.readFileSync(distIndexPath, 'utf8');
  
  // Копируем собранный HTML обратно в корень
  fs.writeFileSync(sourceIndexPath, distHtml);
  
  console.log('✅ HTML файл обновлен с правильными ссылками на assets');
  
  // Выводим информацию о сгенерированных файлах
  const files = fs.readdirSync(path.join(distDir, 'assets'));
  const jsFile = files.find(f => f.startsWith('index-') && f.endsWith('.js'));
  const cssFile = files.find(f => f.startsWith('index-') && f.endsWith('.css'));
  
  console.log(`📄 JS файл: ${jsFile}`);
  console.log(`🎨 CSS файл: ${cssFile}`);
  
} catch (error) {
  console.error('❌ Ошибка при исправлении HTML:', error.message);
  process.exit(1);
}
