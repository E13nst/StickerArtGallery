#!/usr/bin/env node

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Пути к файлам
const distDir = path.join(__dirname, '..', 'dist');
const distIndexPath = path.join(distDir, 'index.html');

console.log('🔧 Проверяем результаты сборки...');

try {
  // Проверяем, что dist/index.html существует
  if (!fs.existsSync(distIndexPath)) {
    throw new Error('dist/index.html не найден');
  }
  
  // Читаем собранный index.html из dist
  const distHtml = fs.readFileSync(distIndexPath, 'utf8');
  
  // Проверяем, что файл содержит ссылки на assets
  if (!distHtml.includes('/mini-app-react/assets/index-')) {
    throw new Error('В dist/index.html нет ссылок на собранные assets');
  }
  
  console.log('✅ Сборка завершена успешно');
  console.log('📦 Production файлы находятся в dist/');
  
  // Выводим информацию о сгенерированных файлах
  const files = fs.readdirSync(path.join(distDir, 'assets'));
  const jsFile = files.find(f => f.startsWith('index-') && f.endsWith('.js'));
  const cssFile = files.find(f => f.startsWith('index-') && f.endsWith('.css'));
  
  console.log(`📄 JS файл: ${jsFile}`);
  console.log(`🎨 CSS файл: ${cssFile}`);
  console.log('');
  console.log('⚠️  Важно: Скопируйте dist/index.html в корень ВРУЧНУЮ перед деплоем:');
  console.log('   cp dist/index.html index.html');
  
} catch (error) {
  console.error('❌ Ошибка при проверке сборки:', error.message);
  process.exit(1);
}
