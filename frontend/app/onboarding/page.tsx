'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/hooks/useAuth';
import { signOut } from '@/lib/firebase/auth';
import { getMe, updateDepartureStation } from '@/lib/api/auth';
import { getStations, Station } from '@/lib/api/stations';
import { ApiError } from '@/lib/api/client';
import { KoreaMap } from '@/components/map/KoreaMap';

export default function OnboardingPage() {
  const router = useRouter();
  const { user, loading } = useAuth();

  const [stations, setStations] = useState<Station[]>([]);
  const [selectedId, setSelectedId] = useState<string>('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [open, setOpen] = useState(true);

  // 비로그인은 로그인 페이지로
  useEffect(() => {
    if (!loading && !user) router.replace('/login');
  }, [user, loading, router]);

  // 이미 출발역 설정된 사용자는 메인으로
  useEffect(() => {
    if (!user) return;
    (async () => {
      try {
        const [meRes, stationsRes] = await Promise.all([
          getMe(),
          getStations(),
        ]);
        if (meRes.departureStationId !== null) {
          router.replace('/');
          return;
        }
        setStations(stationsRes.stations);
      } catch (e) {
        handleError(e);
      }
    })();
  }, [user, router]);

  function handleError(e: unknown) {
    if (e instanceof ApiError) {
      setError(`[${e.status}] ${e.errorCode}: ${e.message}`);
    } else if (e instanceof Error) {
      setError(e.message);
    } else {
      setError('알 수 없는 오류');
    }
  }

  function handleMarkerClick(id: string) {
    setOpen(true);
    setSelectedId(id);
  }

  async function handleLogout() {
    await signOut();
    router.replace('/login');
  }

  async function handleConfirm() {
    if (!selectedId) return;
    setError(null);
    setSubmitting(true);
    try {
      await updateDepartureStation(Number(selectedId));
      router.replace('/');
    } catch (e) {
      handleError(e);
    } finally {
      setSubmitting(false);
    }
  }

  if (loading || !user) {
    return (
      <main className="p-8 text-gray-500 dark:text-gray-400">로딩 중...</main>
    );
  }

  const selected = stations.find((s) => String(s.id) === selectedId);

  return (
    <main className="relative h-screen w-screen overflow-hidden">
      {/* 헤더 floating */}
      <header className="pointer-events-none absolute inset-x-0 top-0 z-30 flex items-start justify-between p-4 md:p-6">
        <div className="pointer-events-auto rounded-2xl border border-gray-100 bg-white/95 px-4 py-2 shadow-md backdrop-blur dark:border-gray-700 dark:bg-gray-800/95">
          <h1 className="text-xl font-bold tracking-tight md:text-2xl">
            <span className="text-[#2AC1BC]">Focus</span>{' '}
            <span className="text-gray-800 dark:text-gray-100">Train</span>
          </h1>
          <p className="text-[10px] text-gray-500 dark:text-gray-400">
            첫 탑승 준비 — 출발역을 선택해주세요
          </p>
        </div>

        <nav className="pointer-events-auto mr-14 flex items-center gap-1 rounded-full border border-gray-200/60 bg-white/85 px-1.5 py-1 shadow-sm backdrop-blur-xl dark:border-white/10 dark:bg-gray-900/60">
          <Link
            href="/profile"
            className="rounded-full px-3 py-1.5 text-xs font-medium text-gray-600 transition hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-white/10"
          >
            프로필
          </Link>
          <span className="h-3 w-px bg-gray-300/60 dark:bg-white/10" aria-hidden />
          <Link
            href="/history"
            className="rounded-full px-3 py-1.5 text-xs font-medium text-gray-600 transition hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-white/10"
          >
            운행 일지
          </Link>
          <span className="h-3 w-px bg-gray-300/60 dark:bg-white/10" aria-hidden />
          <button
            onClick={handleLogout}
            className="rounded-full px-3 py-1.5 text-xs font-medium text-gray-600 transition hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-white/10"
          >
            로그아웃
          </button>
        </nav>
      </header>

      {error && (
        <div className="pointer-events-auto absolute left-1/2 top-20 z-30 -translate-x-1/2 rounded-xl border border-red-200 bg-red-50/95 px-4 py-2 text-sm text-red-700 shadow-md backdrop-blur dark:border-red-900 dark:bg-red-950/95 dark:text-red-300">
          {error}
        </div>
      )}

      {/* 지도 풀스크린 — 출발역만 선택, 도착 자리는 비움 */}
      <div className="absolute inset-0">
        <KoreaMap
          stations={stations}
          departureId={selectedId}
          arrivalId=""
          onStationClick={handleMarkerClick}
          progress={0}
          active={false}
        />
      </div>

      {open ? (
        /* 좌측 floating panel (모바일은 하단) */
        <aside className="pointer-events-none absolute inset-x-0 bottom-0 z-20 flex justify-start p-4 md:inset-x-auto md:bottom-6 md:left-6 md:p-0">
          <section className="pointer-events-auto w-full rounded-3xl border border-gray-100 bg-white/95 p-6 shadow-lg backdrop-blur dark:border-gray-700 dark:bg-gray-800/95 md:w-80">
            <div className="mb-5 flex items-center justify-between">
              <h2 className="flex items-center gap-2 text-lg font-bold">
                <span className="rounded bg-[#2AC1BC] px-1.5 py-0.5 text-sm text-white">
                  📍
                </span>{' '}
                나의 출발역
              </h2>
              <button
                onClick={() => setOpen(false)}
                className="flex h-7 w-7 items-center justify-center rounded-full pb-2 text-xl font-bold text-gray-400 transition hover:bg-gray-100 dark:hover:bg-gray-700"
                aria-label="패널 최소화"
                title="최소화 — 지도 전체 보기"
              >
                —
              </button>
            </div>

            <p className="mb-4 text-xs leading-relaxed text-gray-500 dark:text-gray-400">
              FocusTrain을 시작할 출발역을 골라주세요.
              <br />
              지도에서 마커를 누르거나 아래에서 선택할 수 있어요.
            </p>

            <div className="space-y-4">
              <div>
                <label className="mb-1 block text-xs font-bold uppercase text-gray-400 dark:text-gray-500">
                  출발역
                </label>
                <select
                  value={selectedId}
                  onChange={(e) => setSelectedId(e.target.value)}
                  className="w-full rounded-lg border border-gray-200 bg-gray-50 p-3 outline-none focus:border-[#2AC1BC] dark:border-gray-700 dark:bg-gray-700 dark:text-gray-100"
                >
                  <option value="">출발역 선택</option>
                  {stations.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name}
                    </option>
                  ))}
                </select>
              </div>

              {selected && (
                <div className="rounded-2xl border border-[#2AC1BC]/20 bg-[#EBFBFA] p-4 text-center dark:bg-[#14302f]">
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    선택한 출발역
                  </p>
                  <p className="text-2xl font-bold text-[#2AC1BC]">
                    {selected.name}
                  </p>
                </div>
              )}

              <button
                onClick={handleConfirm}
                disabled={!selectedId || submitting}
                className="w-full rounded-2xl bg-[#2AC1BC] py-4 text-base font-bold text-white shadow-sm hover:opacity-90 disabled:cursor-not-allowed disabled:bg-gray-200 disabled:text-gray-400 dark:disabled:bg-gray-700 dark:disabled:text-gray-500"
              >
                {submitting ? '저장 중...' : '여기서 시작! 🚄'}
              </button>
            </div>
          </section>
        </aside>
      ) : (
        /* 접힌 상태 — 작은 토글 버튼만 */
        <button
          onClick={() => setOpen(true)}
          className="pointer-events-auto absolute bottom-4 left-4 z-20 flex items-center gap-2 rounded-full border border-gray-100 bg-white/95 px-4 py-3 text-sm font-bold shadow-lg backdrop-blur transition hover:scale-105 dark:border-gray-700 dark:bg-gray-800/95 md:bottom-6 md:left-6"
          title="출발역 패널 열기"
        >
          <span className="rounded bg-[#2AC1BC] px-1.5 py-0.5 text-xs text-white">
            📍
          </span>
          출발역 패널 열기
          {selectedId && (
            <span className="ml-1 rounded-full bg-[#2AC1BC]/10 px-2 py-0.5 text-[10px] text-[#2AC1BC]">
              선택 중
            </span>
          )}
        </button>
      )}
    </main>
  );
}
