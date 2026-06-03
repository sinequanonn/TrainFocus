'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { signInWithGoogle, getIdToken } from '@/lib/firebase/auth';
import { login as backendLogin } from '@/lib/api/auth';
import { ApiError } from '@/lib/api/client';
import { useAuth } from '@/lib/hooks/useAuth';

export default function LoginPage() {
  const router = useRouter();
  const { user, loading } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!loading && user) router.replace('/');
  }, [user, loading, router]);

  async function handleGoogleLogin() {
    setError(null);
    setSubmitting(true);
    try {
      await signInWithGoogle();
      const idToken = await getIdToken();
      if (!idToken) throw new Error('ID Token을 가져올 수 없습니다.');
      await backendLogin(idToken);
      router.replace('/');
    } catch (e) {
      if (e instanceof ApiError && e.errorCode === 'USER_NOT_FOUND') {
        router.replace('/signup');
        return;
      }
      setError(e instanceof Error ? e.message : '로그인 실패');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="relative flex min-h-screen flex-col overflow-hidden bg-gradient-to-b from-[#EBFBFA] via-[#F6F6F6] to-white">
      {/* 배경 한반도 실루엣 */}
      <svg
        viewBox="0 0 400 600"
        className="pointer-events-none absolute -right-20 top-1/2 hidden h-[120%] -translate-y-1/2 opacity-[0.06] lg:block"
        aria-hidden
      >
        <path
          d="M145,55 C160,45 190,40 210,42 C230,45 250,55 265,70 C280,85 290,110 295,130 C300,150 315,180 325,210 C335,240 340,280 338,310 C335,340 320,380 310,410 C300,440 295,480 285,510 C275,540 240,560 210,565 C180,570 140,560 110,540 C80,520 60,480 55,440 C50,400 65,340 60,300 C55,260 50,220 52,180 C55,140 70,100 100,75 C120,60 135,60 145,55 Z"
          fill="#2AC1BC"
        />
        <circle cx={160} cy={585} r={15} fill="#2AC1BC" />
      </svg>

      <div className="relative z-10 mx-auto flex w-full max-w-6xl flex-1 items-center px-6 py-12 md:px-10">
        <div className="grid w-full grid-cols-1 items-center gap-12 lg:grid-cols-2">
          {/* 좌측: 소개 */}
          <section>
            <h1 className="mb-4 text-4xl font-bold leading-tight md:text-5xl">
              <span className="text-[#2AC1BC]">Focus</span>{' '}
              <span className="text-gray-800">Train</span>
            </h1>
            <p className="mb-6 text-lg leading-relaxed text-gray-600">
              당신의 다음 집중지는 어디입니까?
              <br />
              기차를 타고 떠나는 우아한 몰입 훈련.
            </p>

            {/* 가치 키워드 */}
            <div className="mb-8 flex gap-2">
              {['집중', '여정', '기록'].map((k) => (
                <span
                  key={k}
                  className="rounded-full bg-white/70 px-3 py-1 text-xs font-bold text-[#2AC1BC] shadow-sm backdrop-blur"
                >
                  # {k}
                </span>
              ))}
            </div>

            {/* 3단계 미리보기 */}
            <ol className="space-y-3">
              <Step
                icon="🎫"
                title="예매하기"
                desc="출발역과 도착역, 지연 시간을 선택"
              />
              <Step
                icon="🚄"
                title="운행하기"
                desc="목표 시간 동안 기차가 도착역까지 이동"
              />
              <Step
                icon="🎉"
                title="도착하기"
                desc="완주한 여정이 기록으로 누적"
              />
            </ol>

            <p className="mt-8 text-xs text-gray-400">
              현재 <b className="text-gray-600">50+개 역 · 2,700+ 노선</b> 운행 중
            </p>
          </section>

          {/* 우측: 로그인 카드 */}
          <section className="flex justify-center lg:justify-end">
            <div className="w-full max-w-sm rounded-3xl border border-white bg-white/80 p-8 shadow-xl backdrop-blur-md">
              <h2 className="mb-2 text-2xl font-bold">탑승 준비</h2>
              <p className="mb-6 text-sm text-gray-500">
                Google 계정으로 5초만에 시작하세요.
              </p>

              <button
                onClick={handleGoogleLogin}
                disabled={submitting}
                className="flex w-full items-center justify-center gap-3 rounded-xl bg-white border border-gray-200 py-3 font-bold text-gray-700 shadow-sm transition hover:border-[#2AC1BC] hover:shadow disabled:cursor-not-allowed disabled:opacity-50"
              >
                {submitting ? (
                  '기차표 발권 중...'
                ) : (
                  <>
                    <GoogleIcon />
                    <span>Google로 로그인</span>
                  </>
                )}
              </button>

              {error && (
                <p className="mt-4 rounded-lg border border-red-200 bg-red-50 p-3 text-xs text-red-700">
                  {error}
                </p>
              )}

              <div className="mt-6 border-t border-gray-100 pt-4 text-[10px] leading-relaxed text-gray-400">
                <p className="mt-1">
                  별도 비밀번호 없이 Google 계정 정보만 사용해요.
                </p>
              </div>
            </div>
          </section>
        </div>
      </div>

      <footer className="relative z-10 py-6 text-center text-[11px] text-gray-400">
        © {new Date().getFullYear()} FocusTrain
      </footer>
    </main>
  );
}

function Step({
  icon,
  title,
  desc,
}: {
  icon: string;
  title: string;
  desc: string;
}) {
  return (
    <li className="flex items-center gap-3 rounded-2xl border border-gray-100 bg-white/70 p-3 backdrop-blur-sm">
      <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#EBFBFA] text-xl">
        {icon}
      </span>
      <div>
        <p className="text-sm font-bold text-gray-800">{title}</p>
        <p className="text-xs text-gray-500">{desc}</p>
      </div>
    </li>
  );
}

// Google 공식 G 로고 (브랜드 가이드라인 색상)
function GoogleIcon() {
  return (
    <svg width={18} height={18} viewBox="0 0 24 24" aria-hidden>
      <path
        fill="#4285F4"
        d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
      />
      <path
        fill="#34A853"
        d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
      />
      <path
        fill="#FBBC05"
        d="M5.84 14.1c-.22-.66-.35-1.36-.35-2.1s.13-1.44.35-2.1V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.83z"
      />
      <path
        fill="#EA4335"
        d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
      />
    </svg>
  );
}
