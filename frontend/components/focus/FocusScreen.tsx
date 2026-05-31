'use client';

import { useCallback, useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { Station } from '@/lib/api/stations';
import { KoreaMap, LiveRunner } from '@/components/map/KoreaMap';
import { RoomResponse } from '@/lib/api/rooms';
import { useDocumentPiP } from '@/lib/hooks/useDocumentPiP';
import { FocusPipContent } from './FocusPipContent';

type TimerMode = 'remaining' | 'elapsed';

// PiP 창 크기 시험 — 한 줄 바꿔서 비교
const PIP_SIZE = { width: 360, height: 140 }; // 중간 (추천)
// const PIP_SIZE = { width: 320, height: 110 }; // 작게
// const PIP_SIZE = { width: 480, height: 170 }; // 크게

function formatTime(totalSeconds: number) {
  const h = Math.floor(totalSeconds / 3600);
  const m = Math.floor((totalSeconds % 3600) / 60);
  const s = totalSeconds % 60;
  if (h > 0) {
    return `${String(h).padStart(2, '0')}:${String(m).padStart(
      2,
      '0'
    )}:${String(s).padStart(2, '0')}`;
  }
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

interface Props {
  /** 전체 stations — 호환성 유지용. 실제 표시에는 departure/arrival만 사용 */
  stations: Station[];
  departure: Station;
  arrival: Station;
  totalTargetSeconds: number;
  accumulatedSeconds: number;
  status: 'RUNNING' | 'PAUSED';
  busy: boolean;
  onPause: () => void;
  onResume: () => void;
  onComplete: () => void;
  onAbort: () => void;
  liveRunners?: LiveRunner[];
  myRooms?: RoomResponse[];
  selectedRoom?: RoomResponse | null;
  onSelectRoom?: (room: RoomResponse | null) => void;
}

export function FocusScreen({
  departure,
  arrival,
  totalTargetSeconds,
  accumulatedSeconds,
  status,
  busy,
  onPause,
  onResume,
  onComplete,
  onAbort,
  liveRunners,
  myRooms = [],
  selectedRoom = null,
  onSelectRoom,
}: Props) {
  const [timerMode, setTimerMode] = useState<TimerMode>('remaining');
  const [open, setOpen] = useState(true);

  const pip = useDocumentPiP();

  const isRunning = status === 'RUNNING';
  const remainingSeconds = Math.max(0, totalTargetSeconds - accumulatedSeconds);
  const targetReached = accumulatedSeconds >= totalTargetSeconds;
  const progressPct =
    totalTargetSeconds > 0
      ? Math.min(100, (accumulatedSeconds / totalTargetSeconds) * 100)
      : 0;
  const displaySeconds =
    timerMode === 'remaining' ? remainingSeconds : accumulatedSeconds;

  const pipActive = !!pip.pipWindow;

  // PiP 는 Chrome user activation 정책으로 자동 열기 불가 — 토글 클릭(user gesture) 로만 열림
  const handleTogglePip = useCallback(async () => {
    if (pip.pipWindow) {
      pip.close();
    } else {
      await pip.open(PIP_SIZE);
    }
  }, [pip]);

  // PiP 창 타이틀을 동적으로 갱신 → 브라우저 chrome UI 상단에 "FocusTrain 47%" 표시
  // Origin(localhost:3000) 자체는 못 숨기지만 사용자 인지를 % 쪽으로 끌어옴
  useEffect(() => {
    if (!pip.pipWindow) return;
    const pct = Math.round(progressPct);
    const statusEmoji = isRunning ? '🚄' : '⏸';
    pip.pipWindow.document.title = `${statusEmoji} FocusTrain ${pct}%`;
  }, [pip.pipWindow, progressPct, isRunning]);

  return (
    <div className="absolute inset-0">
      {/* 지도 풀스크린 — 출발/도착 마커만 (집중 모드, 최대 줌 18) */}
      <div className="absolute inset-0">
        <KoreaMap
          stations={[departure, arrival]}
          departureId={String(departure.id)}
          arrivalId={String(arrival.id)}
          onStationClick={() => {}}
          progress={progressPct / 100}
          active={true}
          maxZoom={18}
          liveRunners={liveRunners}
        />
      </div>

      {/* 지도 우상단 — PiP 미니창 토글 (Chrome/Edge 116+ 지원 시에만) */}
      {pip.supported && (
        <button
          onClick={handleTogglePip}
          className={`pointer-events-auto absolute right-4 top-20 z-20 flex items-center gap-1.5 rounded-full border px-3 py-2 text-xs font-bold shadow-md backdrop-blur transition hover:scale-105 md:right-6 ${
            pipActive
              ? 'border-[#2AC1BC] bg-[#2AC1BC] text-white'
              : 'border-gray-200 bg-white/95 text-gray-700 dark:border-gray-700 dark:bg-gray-800/95 dark:text-gray-200'
          }`}
          title={
            pipActive
              ? '미니창 닫기'
              : '미니창 열기 — 켜두면 브라우저 최소화 후에도 진행률 확인 가능 (Chrome/Edge)'
          }
        >
          🪟 {pipActive ? '미니창 끄기' : '미니창 열기'}
        </button>
      )}

      {open ? (
        /* 좌하단 floating panel */
        <aside className="pointer-events-none absolute inset-x-0 bottom-0 z-20 flex justify-start p-4 md:inset-x-auto md:bottom-6 md:left-6 md:p-0">
          <section className="pointer-events-auto w-full rounded-3xl border border-[#2AC1BC]/30 bg-white/95 p-6 shadow-lg backdrop-blur dark:bg-gray-800/95 md:w-80">
            {/* 상단: 상태 + 진행률 + 미니창 토글 + 최소화 */}
            <div className="mb-3 flex items-center justify-between">
              <span
                className={`rounded-full px-3 py-1 text-xs font-bold ${
                  isRunning
                    ? 'bg-[#EBFBFA] text-[#2AC1BC] dark:bg-[#14302f]'
                    : 'bg-gray-100 text-gray-500 dark:bg-gray-700 dark:text-gray-400'
                }`}
              >
                {isRunning ? '🚄 운행 중' : '⏸ 하차 중'}
              </span>
              <div className="flex items-center gap-1">
                <span className="font-mono text-xs text-gray-400 dark:text-gray-500">
                  {Math.round(progressPct)}%
                </span>
                {pip.supported && (
                  <button
                    onClick={handleTogglePip}
                    className={`flex h-7 w-7 items-center justify-center rounded-full text-sm transition ${
                      pipActive
                        ? 'bg-[#2AC1BC]/15 text-[#2AC1BC]'
                        : 'text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700'
                    }`}
                    aria-label={pipActive ? '미니창 닫기' : '미니창 열기'}
                    title={
                      pipActive
                        ? '미니창 닫기'
                        : '미니창 열기 — 켜두면 브라우저 최소화 후에도 진행률 확인 가능'
                    }
                  >
                    🪟
                  </button>
                )}
                <button
                  onClick={() => setOpen(false)}
                  className="flex h-7 w-7 items-center justify-center rounded-full pb-2 text-xl font-bold text-gray-400 transition hover:bg-gray-100 dark:hover:bg-gray-700"
                  aria-label="패널 최소화"
                  title="최소화 — 지도 전체 보기"
                >
                  —
                </button>
              </div>
            </div>

            {/* 노선 */}
            <div className="mb-3 text-center text-sm font-bold text-gray-700 dark:text-gray-200">
              {departure.name}{' '}
              <span className="text-gray-300 dark:text-gray-600">→</span>{' '}
              {arrival.name}
            </div>

            {/* 함께 달리기 선택 */}
            {myRooms.length > 0 && onSelectRoom && (
              <div className="mb-5 rounded-xl border border-gray-100 bg-gray-50 px-3 py-2 dark:border-gray-700 dark:bg-gray-700">
                <select
                  value={selectedRoom ? String(selectedRoom.id) : ''}
                  onChange={(e) => {
                    const id = e.target.value;
                    if (!id) {
                      onSelectRoom(null);
                    } else {
                      const r = myRooms.find((m) => String(m.id) === id);
                      if (r) onSelectRoom(r);
                    }
                  }}
                  className="w-full bg-transparent text-xs font-medium outline-none dark:text-gray-100"
                  title="방을 선택해서 함께 달리기 현황 보기"
                >
                  <option value="">🏃 혼자 달리기</option>
                  {myRooms.map((r) => (
                    <option key={r.id} value={r.id}>
                      🏃 {r.name}
                    </option>
                  ))}
                </select>
              </div>
            )}

            {/* 타이머 모드 토글 */}
            <div className="mb-3 inline-flex rounded-full border border-gray-200 bg-gray-50 p-1 text-[11px] font-bold dark:border-gray-700 dark:bg-gray-700">
              <button
                onClick={() => setTimerMode('remaining')}
                className={`rounded-full px-3 py-1 transition ${
                  timerMode === 'remaining'
                    ? 'bg-white text-[#2AC1BC] shadow dark:bg-gray-900'
                    : 'text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300'
                }`}
              >
                남은 시간
              </button>
              <button
                onClick={() => setTimerMode('elapsed')}
                className={`rounded-full px-3 py-1 transition ${
                  timerMode === 'elapsed'
                    ? 'bg-white text-[#2AC1BC] shadow dark:bg-gray-900'
                    : 'text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300'
                }`}
              >
                경과 시간
              </button>
            </div>

            <div className="mb-2 font-mono text-5xl font-bold text-[#2AC1BC]">
              {formatTime(displaySeconds)}
            </div>
            <p className="mb-5 text-xs text-gray-400 dark:text-gray-500">
              목표 {Math.floor(totalTargetSeconds / 60)}분
            </p>

            <div className="mb-6 h-2 overflow-hidden rounded-full bg-gray-100 dark:bg-gray-700">
              <div
                className="h-full bg-[#2AC1BC] transition-all duration-500"
                style={{ width: `${progressPct}%` }}
              />
            </div>

            <div className="space-y-2">
              {isRunning ? (
                <button
                  onClick={onPause}
                  disabled={busy}
                  className="w-full rounded-2xl border border-gray-200 bg-white py-3 font-bold text-gray-700 hover:bg-gray-50 disabled:opacity-50 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-200 dark:hover:bg-gray-700"
                >
                  ⏸ 하차 (일시정지)
                </button>
              ) : (
                <button
                  onClick={onResume}
                  disabled={busy}
                  className="w-full rounded-2xl bg-[#2AC1BC] py-3 font-bold text-white hover:opacity-90 disabled:opacity-50"
                >
                  ▶ 재승차
                </button>
              )}

              <button
                onClick={onComplete}
                disabled={busy || isRunning || !targetReached}
                className="w-full rounded-2xl bg-[#2AC1BC] py-3 font-bold text-white hover:opacity-90 disabled:cursor-not-allowed disabled:bg-gray-200 disabled:text-gray-400 dark:disabled:bg-gray-700 dark:disabled:text-gray-500"
                title={
                  isRunning
                    ? '하차(일시정지) 후 완료할 수 있어요'
                    : targetReached
                      ? ''
                      : '목표 시간에 도달해야 완료할 수 있어요'
                }
              >
                🎉 도착 (완료)
              </button>

              <button
                onClick={onAbort}
                disabled={busy || isRunning}
                className="w-full rounded-2xl border border-red-200 bg-red-50 py-3 font-bold text-red-500 hover:bg-red-100 disabled:cursor-not-allowed disabled:border-gray-200 disabled:bg-gray-100 disabled:text-gray-300 dark:border-red-900 dark:bg-red-950 dark:text-red-400 dark:hover:bg-red-900 dark:disabled:border-gray-700 dark:disabled:bg-gray-700 dark:disabled:text-gray-600"
                title={isRunning ? '하차(일시정지) 후 종료할 수 있어요' : ''}
              >
                ⏹ 운행 종료
              </button>
            </div>
          </section>
        </aside>
      ) : (
        /* 접힌 상태 — 상태/타이머 요약 + 펼치기 토글 */
        <button
          onClick={() => setOpen(true)}
          className="pointer-events-auto absolute bottom-4 left-4 z-20 flex items-center gap-2 rounded-full border border-[#2AC1BC]/30 bg-white/95 px-4 py-3 text-sm font-bold shadow-lg backdrop-blur transition hover:scale-105 dark:bg-gray-800/95 md:bottom-6 md:left-6"
          title="패널 펼치기"
        >
          <span
            className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${
              isRunning
                ? 'bg-[#EBFBFA] text-[#2AC1BC] dark:bg-[#14302f]'
                : 'bg-gray-100 text-gray-500 dark:bg-gray-700 dark:text-gray-400'
            }`}
          >
            {isRunning ? '🚄' : '⏸'}
          </span>
          <span className="font-mono text-[#2AC1BC]">
            {formatTime(displaySeconds)}
          </span>
          <span className="text-[10px] text-gray-400 dark:text-gray-500">
            ↗ 펼치기
          </span>
        </button>
      )}

      {/* PiP 창 안에 진행률 + 하차/재승차 컴포넌트 portal 렌더링 */}
      {pip.pipWindow &&
        createPortal(
          <FocusPipContent
            departureName={departure.name}
            arrivalName={arrival.name}
            progress={progressPct / 100}
            isRunning={isRunning}
            busy={busy}
            onPause={onPause}
            onResume={onResume}
          />,
          pip.pipWindow.document.body
        )}
    </div>
  );
}
