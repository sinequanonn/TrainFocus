import type { Metadata } from "next";
import { Noto_Sans_KR } from "next/font/google";
import { Analytics } from "@vercel/analytics/next";
import { SpeedInsights } from "@vercel/speed-insights/next";
import "./globals.css";
import { ThemeToggle } from "@/components/ui/ThemeToggle";

const notoSansKr = Noto_Sans_KR({
  variable: "--font-noto-sans-kr",
  subsets: ["latin"],
  weight: ["300", "400", "500", "700"],
});

// 페이지 렌더 전에 테마를 적용해 화면 깜빡임(FOUC)을 막는 스크립트
const themeInitScript = `
(function() {
  try {
    var t = localStorage.getItem('theme');
    if (t === 'dark' || (!t && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
      document.documentElement.classList.add('dark');
    }
  } catch (e) {}
})();
`;

export const metadata: Metadata = {
  title: "FocusTrain | 우아한 몰입 훈련",
  description: "대한민국을 가로지르며 완성하는 나만의 몰입 시간",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="ko"
      suppressHydrationWarning
      className={`${notoSansKr.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col">
          <script dangerouslySetInnerHTML={{ __html: themeInitScript }} />
          <ThemeToggle />
          {children}
          <Analytics />
          <SpeedInsights />
        </body>
    </html>
  );
}
