'use client';

import { useEffect, useState } from 'react';
import { usePathname } from 'next/navigation';

/**
 * 라이트/다크 테마 토글 버튼 (우상단 고정).
 * - 초기 테마는 layout 의 inline script 가 <html> 에 .dark 클래스로 적용
 * - 클릭 시 .dark 토글 + localStorage 저장
 */
export function ThemeToggle() {
  const pathname = usePathname();
  const [isDark, setIsDark] = useState(false);

  // 마운트 시 현재 <html> 의 .dark 여부로 상태 동기화 (hydration 안전)
  useEffect(() => {
    setIsDark(document.documentElement.classList.contains('dark'));
  }, []);

  function toggle() {
    const next = !isDark;
    setIsDark(next);
    document.documentElement.classList.toggle('dark', next);
    try {
      localStorage.setItem('theme', next ? 'dark' : 'light');
    } catch {
      /* localStorage 접근 불가 시 무시 */
    }
  }

  if (pathname === '/login') return null;

  return (
    <button
      onClick={toggle}
      className="fixed right-4 top-4 z-[60] flex h-10 w-10 items-center justify-center rounded-full border border-gray-200 bg-white text-lg shadow-sm transition hover:scale-105 dark:border-gray-700 dark:bg-gray-800"
      aria-label="테마 전환"
      title={isDark ? '라이트 모드로 전환' : '다크 모드로 전환'}
    >
      {isDark ? '☀️' : '🌙'}
    </button>
  );
}
