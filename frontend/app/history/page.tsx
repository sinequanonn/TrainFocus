'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/hooks/useAuth';
import {
  getRunningLogCalendar,
  getRunningLogDaily,
  getRunningLogPeriod,
  RunningLogCalendarDay,
  RunningLogDailyResponse,
  RunningLogPeriodResponse,
  RunningLogTicket,
} from '@/lib/api/sessions';

const WEEKDAY_LABELS = ['일', '월', '화', '수', '목', '금', '토'];

function formatSeconds(sec: number) {
  const m = Math.round(sec / 60);
  const h = Math.floor(m / 60);
  const mm = m % 60;
  if (h > 0) return `${h}h ${mm}m`;
  return `${mm}m`;
}

function startOfToday() {
  const t = new Date();
  t.setHours(0, 0, 0, 0);
  return t;
}

function toDateStr(d: Date) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(
    d.getDate()
  ).padStart(2, '0')}`;
}

function buildMonth(year: number, month: number): (Date | null)[] {
  const first = new Date(year, month, 1);
  const startPad = first.getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const cells: (Date | null)[] = [];
  for (let i = 0; i < startPad; i++) cells.push(null);
  for (let d = 1; d <= daysInMonth; d++) cells.push(new Date(year, month, d));
  return cells;
}

function readDateFromUrl(): Date | null {
  if (typeof window === 'undefined') return null;
  const param = new URLSearchParams(window.location.search).get('date');
  if (!param) return null;
  const d = new Date(`${param}T00:00:00`);
  return Number.isNaN(d.getTime()) ? null : d;
}

export default function RunningLogPage() {
  const router = useRouter();
  const { user, loading } = useAuth();

  const [selected, setSelected] = useState<Date | null>(
    () => readDateFromUrl() ?? startOfToday()
  );

  const handleSelect = (d: Date) => {
    setSelected(d);
    window.history.replaceState(null, '', `/history?date=${toDateStr(d)}`);
  };

  useEffect(() => {
    if (!loading && !user) router.replace('/login');
  }, [user, loading, router]);

  if (loading || !user) {
    return <main className="p-8 text-gray-500 dark:text-gray-400">로딩 중...</main>;
  }

  return (
    <main className="mx-auto max-w-5xl p-6 md:p-10">
      <header className="mb-6">
        <Link
          href="/"
          className="mb-2 inline-block text-xs text-gray-400 hover:text-[#2AC1BC] dark:text-gray-500"
        >
          ← 메인으로
        </Link>
        <h1 className="text-2xl md:text-3xl font-bold tracking-tight">
          <span className="text-[#2AC1BC]">운행</span>{' '}
          <span className="text-gray-800 dark:text-gray-100">일지</span>
        </h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          날짜를 선택해 그날 발행된 승차권을 확인하세요
        </p>
      </header>

      <div className="flex flex-col gap-6 lg:flex-row lg:items-start">
        <div className="lg:w-[340px] lg:shrink-0">
          <Calendar selected={selected} onSelect={handleSelect} />
          <PeriodSummary date={selected} />
        </div>
        <div className="min-w-0 flex-1">
          <DayPanel date={selected} />
        </div>
      </div>
    </main>
  );
}

function Calendar({
  selected,
  onSelect,
}: {
  selected: Date | null;
  onSelect: (d: Date) => void;
}) {
  const today = startOfToday();
  const [view, setView] = useState(() => {
    const a = selected ?? today;
    return { year: a.getFullYear(), month: a.getMonth() };
  });
  const [days, setDays] = useState<RunningLogCalendarDay[]>([]);

  useEffect(() => {
    if (selected) {
      setView({ year: selected.getFullYear(), month: selected.getMonth() });
    }
  }, [selected]);

  useEffect(() => {
    let alive = true;
    getRunningLogCalendar(view.year, view.month + 1)
      .then((r) => {
        if (alive) setDays(r.days);
      })
      .catch(() => {
        if (alive) setDays([]);
      });
    return () => {
      alive = false;
    };
  }, [view.year, view.month]);

  const issued = new Set(days.map((d) => d.date));
  const cells = buildMonth(view.year, view.month);
  const isCurrentMonth =
    view.year === today.getFullYear() && view.month === today.getMonth();

  const goPrev = () =>
    setView((v) =>
      v.month === 0
        ? { year: v.year - 1, month: 11 }
        : { year: v.year, month: v.month - 1 }
    );
  const goNext = () => {
    if (isCurrentMonth) return;
    setView((v) =>
      v.month === 11
        ? { year: v.year + 1, month: 0 }
        : { year: v.year, month: v.month + 1 }
    );
  };

  return (
    <div className="rounded-3xl border border-gray-100 bg-white p-5 shadow-sm dark:border-gray-700 dark:bg-gray-800">
      <div className="mb-4 flex items-center justify-between">
        <button
          onClick={goPrev}
          className="flex h-7 w-7 items-center justify-center rounded-full border border-gray-200 text-gray-500 transition hover:border-[#2AC1BC] hover:text-[#2AC1BC] dark:border-gray-600 dark:text-gray-400"
          aria-label="이전 달"
        >
          ◀
        </button>
        <span className="text-sm font-bold text-gray-700 dark:text-gray-200">
          {view.year}년 {view.month + 1}월
        </span>
        <button
          onClick={goNext}
          disabled={isCurrentMonth}
          className="flex h-7 w-7 items-center justify-center rounded-full border border-gray-200 text-gray-500 transition hover:border-[#2AC1BC] hover:text-[#2AC1BC] disabled:cursor-not-allowed disabled:opacity-30 dark:border-gray-600 dark:text-gray-400"
          aria-label="다음 달"
        >
          ▶
        </button>
      </div>

      <div className="mb-1 grid grid-cols-7 gap-1">
        {WEEKDAY_LABELS.map((w) => (
          <div
            key={w}
            className="py-1 text-center text-[10px] font-bold text-gray-400 dark:text-gray-500"
          >
            {w}
          </div>
        ))}
      </div>

      <div className="grid grid-cols-7 gap-1">
        {cells.map((cell, i) => {
          if (!cell) return <div key={i} />;
          const isFuture = cell > today;
          const isToday = cell.getTime() === today.getTime();
          const hasLog = issued.has(toDateStr(cell));
          const isSel =
            selected !== null && cell.getTime() === selected.getTime();
          return (
            <button
              key={i}
              onClick={() => onSelect(cell)}
              disabled={isFuture}
              className={`relative flex aspect-square flex-col items-center justify-center rounded-xl text-sm transition ${
                isFuture
                  ? 'text-gray-300 dark:text-gray-600'
                  : 'hover:bg-gray-50 dark:hover:bg-gray-700'
              } ${isSel ? 'ring-2 ring-[#2AC1BC]' : ''} ${
                hasLog
                  ? 'bg-[#EBFBFA] font-bold text-[#2AC1BC] dark:bg-[#14302f]'
                  : 'text-gray-600 dark:text-gray-300'
              } ${isToday ? 'border border-[#2AC1BC]/50' : ''}`}
            >
              <span>{cell.getDate()}</span>
              {hasLog && (
                <span className="absolute bottom-1 h-1 w-1 rounded-full bg-[#2AC1BC]" />
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
}

function PeriodSummary({ date }: { date: Date | null }) {
  const [data, setData] = useState<RunningLogPeriodResponse | null>(null);

  useEffect(() => {
    if (!date) {
      setData(null);
      return;
    }
    let alive = true;
    getRunningLogPeriod(toDateStr(date))
      .then((r) => {
        if (alive) setData(r);
      })
      .catch(() => {
        if (alive) setData(null);
      });
    return () => {
      alive = false;
    };
  }, [date]);

  if (!date || !data) return null;

  return (
    <div className="mt-4 rounded-3xl border border-gray-100 bg-white p-5 shadow-sm dark:border-gray-700 dark:bg-gray-800">
      <div className="flex gap-8">
        <div>
          <p className="text-xl font-bold text-[#2AC1BC]">
            {formatSeconds(data.weekRunSeconds)}
          </p>
          <p className="text-xs text-gray-500 dark:text-gray-400">이 주 운행</p>
        </div>
        <div>
          <p className="text-xl font-bold text-gray-800 dark:text-gray-100">
            {formatSeconds(data.monthRunSeconds)}
          </p>
          <p className="text-xs text-gray-500 dark:text-gray-400">이 달 운행</p>
        </div>
      </div>
    </div>
  );
}

function DayPanel({ date }: { date: Date | null }) {
  const [data, setData] = useState<RunningLogDailyResponse | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const today = startOfToday();
    if (!date || date > today) {
      setData(null);
      return;
    }
    let alive = true;
    setLoading(true);
    getRunningLogDaily(toDateStr(date))
      .then((r) => {
        if (alive) {
          setData(r);
          setLoading(false);
        }
      })
      .catch(() => {
        if (alive) {
          setData(null);
          setLoading(false);
        }
      });
    return () => {
      alive = false;
    };
  }, [date]);

  if (!date) {
    return (
      <div className="rounded-3xl border border-dashed border-gray-200 p-10 text-center text-sm text-gray-400 dark:border-gray-700 dark:text-gray-500">
        날짜를 선택하세요
      </div>
    );
  }

  const today = startOfToday();
  const dateLabel = `${date.getMonth() + 1}월 ${date.getDate()}일 (${WEEKDAY_LABELS[date.getDay()]})`;

  if (date > today) {
    return (
      <div className="rounded-3xl border border-dashed border-gray-200 p-10 text-center text-sm text-gray-400 dark:border-gray-700 dark:text-gray-500">
        아직 오지 않은 날입니다
      </div>
    );
  }

  if (loading) {
    return (
      <div className="rounded-3xl border border-gray-100 p-10 text-center text-sm text-gray-400 dark:border-gray-700 dark:text-gray-500">
        불러오는 중...
      </div>
    );
  }

  if (!data || data.sessionCount === 0) {
    return (
      <div className="rounded-3xl border border-dashed border-gray-200 p-10 text-center text-sm text-gray-400 dark:border-gray-700 dark:text-gray-500">
        {dateLabel} · 운행 기록이 없습니다
      </div>
    );
  }

  return (
    <div>
      <div className="rounded-3xl border border-gray-100 bg-white p-5 shadow-sm dark:border-gray-700 dark:bg-gray-800">
        <p className="text-sm font-bold text-gray-700 dark:text-gray-200">
          {dateLabel}
        </p>
        <div className="mt-3 flex gap-8">
          <div>
            <p className="text-2xl font-bold text-[#2AC1BC]">
              {formatSeconds(data.runSeconds)}
            </p>
            <p className="text-xs text-gray-500 dark:text-gray-400">
              총 운행 시간
            </p>
          </div>
          <div>
            <p className="text-2xl font-bold text-gray-800 dark:text-gray-100">
              {data.arrivedCount}회
            </p>
            <p className="text-xs text-gray-500 dark:text-gray-400">무사 도착</p>
          </div>
          <div>
            <p className="text-2xl font-bold text-gray-800 dark:text-gray-100">
              {data.sessionCount}건
            </p>
            <p className="text-xs text-gray-500 dark:text-gray-400">운행</p>
          </div>
        </div>
      </div>

      <div className="mt-4 space-y-4">
        {data.tickets.map((t) => (
          <Ticket key={t.sessionId} t={t} />
        ))}
      </div>
    </div>
  );
}

function Ticket({ t }: { t: RunningLogTicket }) {
  return (
    <Link href={`/history/${t.sessionId}`} className="group block">
      <div className="relative overflow-hidden rounded-2xl bg-[#f4f0e6] shadow-md ring-1 ring-black/5 transition group-hover:-translate-y-0.5 group-hover:shadow-lg">
        <div className="absolute inset-y-0 left-0 flex w-8 items-center justify-center">
          <span className="-rotate-90 whitespace-nowrap font-mono text-[9px] tracking-[0.2em] text-gray-400">
            N° {String(t.sessionId).padStart(9, '0')}
          </span>
        </div>

        <div className="ml-8">
          <div className="flex items-center justify-between bg-[#2AC1BC] px-5 py-2 text-white">
            <span className="text-base font-bold tracking-wide opacity-90">
              승차권
            </span>
            <span className="text-sm font-bold tracking-tight">FocusTrain</span>
          </div>

          <div className="px-5 pb-3 pt-4 text-center">
            <p className="text-4xl font-extrabold tracking-tight text-[#1f9e9a]">
              {formatSeconds(t.focusSeconds)}
            </p>
            <p className="mt-0.5 text-xs font-medium text-gray-500">집중 시간</p>
          </div>

          <div className="relative">
            <div className="absolute left-0 top-1/2 h-4 w-4 -translate-x-1/2 -translate-y-1/2 rounded-full bg-white dark:bg-gray-950" />
            <div className="absolute right-0 top-1/2 h-4 w-4 translate-x-1/2 -translate-y-1/2 rounded-full bg-white dark:bg-gray-950" />
            <div className="mx-4 border-t-2 border-dashed border-gray-300/70" />
          </div>

          <div className="flex items-center justify-between gap-2 px-5 py-4">
            <div className="shrink-0">
              <p className="text-2xl font-bold text-gray-800">
                {formatSeconds(t.targetSeconds)}
              </p>
              <p className="text-[11px] font-medium text-gray-500">
                예상 운행 시간
              </p>
            </div>
            <div className="text-center text-sm font-bold text-gray-700">
              <span>{t.departure.name}</span>
              <span className="mx-1.5 text-[#2AC1BC]">→</span>
              <span>{t.arrival.name}</span>
            </div>
            <div className="flex w-20 shrink-0 justify-center">
              {t.completed ? (
                <div className="-rotate-12 rounded border-2 border-[#a23b2d]/60 px-2 py-1 text-center text-[#a23b2d]/80">
                  <p className="text-[11px] font-extrabold leading-none">
                    무사 도착
                  </p>
                  <p className="mt-0.5 text-[7px] tracking-[0.2em]">ARRIVED</p>
                </div>
              ) : (
                <span className="text-[10px] text-gray-400">미도착</span>
              )}
            </div>
          </div>
        </div>
      </div>
    </Link>
  );
}
