'use client';

import { use, useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/hooks/useAuth';
import {
  getSessionHistoryDetail,
  FocusSessionHistoryDetailResponse,
} from '@/lib/api/sessions';
import { ApiError } from '@/lib/api/client';

function hhmm(iso: string) {
  return new Date(iso).toLocaleTimeString('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
  });
}

function formatDuration(seconds: number) {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  if (h > 0) return `${h}시간 ${m}분 ${s}초`;
  if (m > 0) return `${m}분 ${s}초`;
  return `${s}초`;
}

export default function HistoryDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const router = useRouter();
  const { user, loading } = useAuth();
  const { id } = use(params);

  const [data, setData] = useState<FocusSessionHistoryDetailResponse | null>(
    null
  );
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!loading && !user) router.replace('/login');
  }, [user, loading, router]);

  useEffect(() => {
    if (!user) return;
    (async () => {
      try {
        const res = await getSessionHistoryDetail(Number(id));
        setData(res);
      } catch (e) {
        if (e instanceof ApiError) {
          setError(`[${e.status}] ${e.errorCode}: ${e.message}`);
        } else if (e instanceof Error) {
          setError(e.message);
        }
      }
    })();
  }, [user, id]);

  if (loading || !user) {
    return <main className="p-8 text-gray-500 dark:text-gray-400">로딩 중...</main>;
  }

  return (
    <main className="mx-auto max-w-3xl p-6 md:p-10">
      <Link
        href={data ? `/history?date=${data.session.startedAt.slice(0, 10)}` : '/history'}
        className="mb-2 inline-block text-xs text-gray-400 hover:text-[#2AC1BC] dark:text-gray-500"
      >
        ← 운행 일지
      </Link>

      {error && (
        <div className="mb-6 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:border-red-900 dark:bg-red-950 dark:text-red-300">
          {error}
        </div>
      )}

      {!data ? (
        <p className="text-sm text-gray-400 dark:text-gray-500">불러오는 중...</p>
      ) : (
        <>
          {/* 세션 요약 */}
          <section className="mb-6 rounded-3xl border border-gray-100 bg-white p-6 shadow-sm dark:border-gray-700 dark:bg-gray-800">
            <div className="mb-4 flex items-center gap-2">
              <span
                className={`rounded-full px-3 py-1 text-xs font-bold ${
                  data.session.status === 'COMPLETED'
                    ? 'bg-[#EBFBFA] text-[#2AC1BC] dark:bg-[#14302f]'
                    : 'bg-gray-100 text-gray-500 dark:bg-gray-700 dark:text-gray-400'
                }`}
              >
                {data.session.status === 'COMPLETED' ? '무사 도착' : '중단'}
              </span>
            </div>

            <h1 className="mb-6 text-2xl md:text-3xl font-bold dark:text-gray-100">
              {data.session.departure.name}{' '}
              <span className="text-gray-300 dark:text-gray-600">→</span>{' '}
              {data.session.arrival.name}
            </h1>

            <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
              <Stat
                label="총 집중 시간"
                value={formatDuration(data.session.totalFocusSeconds)}
                highlight
              />
              <Stat
                label="목표 시간"
                value={formatDuration(data.session.totalTargetSeconds)}
              />
              <Stat
                label="달성률"
                value={`${Math.round(
                  (data.session.totalFocusSeconds /
                    data.session.totalTargetSeconds) *
                    100
                )}%`}
              />
              <Stat label="구간 수" value={`${data.legs.length}개`} />
            </div>
          </section>

          {/* Leg 목록 */}
          <section className="rounded-3xl border border-gray-100 bg-white p-6 shadow-sm dark:border-gray-700 dark:bg-gray-800">
            <h2 className="mb-4 text-lg font-bold dark:text-gray-100">탑승 구간 (Legs)</h2>
            {data.legs.length === 0 ? (
              <p className="text-sm italic text-gray-400 dark:text-gray-500">구간 정보가 없습니다.</p>
            ) : (
              <ol className="space-y-2">
                {data.legs.map((leg) => (
                  <li
                    key={leg.legNumber}
                    className="flex items-center justify-between rounded-2xl border border-gray-100 bg-gray-50 p-4 dark:border-gray-700 dark:bg-gray-700"
                  >
                    <div className="flex items-center gap-3">
                      <span className="flex h-8 w-8 items-center justify-center rounded-full bg-[#2AC1BC] text-xs font-bold text-white">
                        {leg.legNumber}
                      </span>
                      <div className="flex items-center gap-2 text-sm font-bold text-gray-800 dark:text-gray-100">
                        <span>{hhmm(leg.startedAt)}</span>
                        <span className="text-gray-300 dark:text-gray-600">~</span>
                        <span>{leg.endedAt ? hhmm(leg.endedAt) : '진행 중'}</span>
                      </div>
                    </div>
                    <span className="text-sm font-bold text-[#2AC1BC]">
                      {formatDuration(leg.durationSeconds ?? 0)}
                    </span>
                  </li>
                ))}
              </ol>
            )}
          </section>
        </>
      )}
    </main>
  );
}

function Stat({
  label,
  value,
  highlight,
}: {
  label: string;
  value: string;
  highlight?: boolean;
}) {
  return (
    <div className="rounded-2xl border border-gray-100 bg-gray-50 p-3 dark:border-gray-700 dark:bg-gray-700">
      <p className="text-[10px] font-bold uppercase text-gray-400 dark:text-gray-500">{label}</p>
      <p
        className={`mt-1 text-base font-bold ${
          highlight ? 'text-[#2AC1BC]' : 'text-gray-800 dark:text-gray-100'
        }`}
      >
        {value}
      </p>
    </div>
  );
}

