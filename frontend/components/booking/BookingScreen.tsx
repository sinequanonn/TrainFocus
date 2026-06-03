'use client';

import { useState } from 'react';
import { Station } from '@/lib/api/stations';
import { KoreaMap, LiveRunner } from '@/components/map/KoreaMap';
import { RoomResponse } from '@/lib/api/rooms';

interface Props {
  stations: Station[];
  departureId: string;
  arrivalId: string;
  delayMinutes: number;
  previewMinutes: number | null;
  onArrivalChange: (id: string) => void;
  onDelayChange: (m: number) => void;
  onStationClick: (id: string) => void;
  onSubmit: () => void;
  busy: boolean;
  liveRunners?: LiveRunner[];
  myRooms?: RoomResponse[];
  selectedRoom?: RoomResponse | null;
  onSelectRoom?: (room: RoomResponse | null) => void;
  onOpenGuide?: () => void;
}

export function BookingScreen({
  stations,
  departureId,
  arrivalId,
  delayMinutes,
  previewMinutes,
  onArrivalChange,
  onDelayChange,
  onStationClick,
  onSubmit,
  busy,
  liveRunners,
  myRooms = [],
  selectedRoom = null,
  onSelectRoom,
  onOpenGuide,
}: Props) {
  // 작은 화면 대응: 패널 접기/펴기. 마커 클릭 시 자동으로 다시 펼침.
  const [open, setOpen] = useState(true);

  const departureStation = stations.find((s) => String(s.id) === departureId);
  // 도착역 후보 — 출발역과 동일한 역은 제외
  const arrivalCandidates = stations.filter(
    (s) => String(s.id) !== departureId
  );

  const canSubmit =
    !busy &&
    !!departureId &&
    !!arrivalId &&
    departureId !== arrivalId &&
    previewMinutes !== null;

  function handleMarkerClick(id: string) {
    setOpen(true);
    onStationClick(id);
  }

  return (
    <div className="absolute inset-0">
      {/* 지도 풀스크린 */}
      <div className="absolute inset-0">
        <KoreaMap
          stations={stations}
          departureId={departureId}
          arrivalId={arrivalId}
          onStationClick={handleMarkerClick}
          progress={0}
          active={false}
          liveRunners={liveRunners}
        />
      </div>

      {open ? (
        /* 좌측 floating panel (모바일은 하단) */
        <aside className="pointer-events-none absolute inset-x-0 bottom-0 z-20 flex justify-start p-4 md:inset-x-auto md:bottom-6 md:left-6 md:p-0">
          <section className="pointer-events-auto w-full rounded-3xl border border-gray-100 bg-white/95 p-6 shadow-lg backdrop-blur dark:border-gray-700 dark:bg-gray-800/95 md:w-80">
            <div className="mb-5 flex items-center justify-between">
              <h2 className="flex items-center gap-2 text-lg font-bold">
                <span className="rounded bg-[#2AC1BC] px-1.5 py-0.5 text-sm text-white">
                  🎫
                </span>{' '}
                승차권 예매
                {onOpenGuide && (
                  <button
                    onClick={onOpenGuide}
                    className="flex h-5 w-5 items-center justify-center rounded-full border border-gray-200 text-xs font-bold text-gray-400 transition hover:border-[#2AC1BC] hover:text-[#2AC1BC] dark:border-gray-700 dark:text-gray-500"
                    title="사용법 보기"
                    aria-label="사용법 보기"
                  >
                    ?
                  </button>
                )}
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

            <div className="space-y-4">
              <div>
                <label className="mb-1 block text-xs font-bold uppercase text-gray-400 dark:text-gray-500">
                  출발역
                </label>
                <div className="flex items-center gap-2 rounded-lg border border-gray-200 bg-gray-100 p-3 dark:border-gray-700 dark:bg-gray-700/50">
                  <span className="rounded bg-[#2AC1BC] px-1.5 py-0.5 text-xs text-white">
                    📍
                  </span>
                  <span className="font-bold text-gray-700 dark:text-gray-200">
                    {departureStation?.name ?? '출발역 미설정'}
                  </span>
                </div>
              </div>
              <div>
                <label className="mb-1 block text-xs font-bold uppercase text-gray-400 dark:text-gray-500">
                  도착역
                </label>
                <select
                  value={arrivalId}
                  onChange={(e) => onArrivalChange(e.target.value)}
                  className="w-full rounded-lg border border-gray-200 bg-gray-50 p-3 outline-none focus:border-[#2AC1BC] dark:border-gray-700 dark:bg-gray-700 dark:text-gray-100"
                >
                  <option value="">도착역 선택</option>
                  {arrivalCandidates.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex items-center justify-between rounded-2xl border border-gray-100 bg-gray-50 px-4 py-3 dark:border-gray-700 dark:bg-gray-700">
                <div>
                  <p className="text-[10px] font-bold uppercase text-gray-400 dark:text-gray-500">
                    Focus Delay
                  </p>
                  <p className="text-xs font-bold text-gray-600 dark:text-gray-300">
                    열차 지연 시간
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <input
                    type="number"
                    min={0}
                    step={10}
                    value={delayMinutes}
                    onChange={(e) =>
                      onDelayChange(Math.max(0, Number(e.target.value) || 0))
                    }
                    className="w-20 rounded-lg border border-gray-200 bg-white p-2 text-center text-sm font-bold outline-none focus:border-[#2AC1BC] dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                  />
                  <span className="text-xs font-bold text-gray-400 dark:text-gray-500">
                    분
                  </span>
                </div>
              </div>

              {previewMinutes !== null && (
                <div className="rounded-2xl border border-[#2AC1BC]/20 bg-[#EBFBFA] p-4 text-center dark:bg-[#14302f]">
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    총 몰입 시간
                  </p>
                  <p className="text-2xl font-bold text-[#2AC1BC]">
                    {previewMinutes + delayMinutes}분
                  </p>
                  <p className="mt-1 text-[10px] text-gray-400 dark:text-gray-500">
                    기본 {previewMinutes}분 + 지연 {delayMinutes}분
                  </p>
                </div>
              )}

              {myRooms.length > 0 && onSelectRoom && (
                <div>
                  <label className="mb-1 block text-xs font-bold uppercase text-gray-400 dark:text-gray-500">
                    🏃 함께 달리기
                  </label>
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
                    className="w-full rounded-lg border border-gray-200 bg-gray-50 p-3 outline-none focus:border-[#2AC1BC] dark:border-gray-700 dark:bg-gray-700 dark:text-gray-100"
                  >
                    <option value="">혼자 달리기</option>
                    {myRooms.map((r) => (
                      <option key={r.id} value={r.id}>
                        {r.name}
                      </option>
                    ))}
                  </select>
                  {selectedRoom && (
                    <p className="mt-1 text-[10px] text-gray-500 dark:text-gray-400">
                      방 멤버의 진행 상황이 지도에 표시됩니다.
                    </p>
                  )}
                </div>
              )}

              <button
                onClick={onSubmit}
                disabled={!canSubmit}
                className="w-full rounded-2xl bg-[#2AC1BC] py-4 text-base font-bold text-white shadow-sm hover:opacity-90 disabled:cursor-not-allowed disabled:bg-gray-200 disabled:text-gray-400 dark:disabled:bg-gray-700 dark:disabled:text-gray-500"
              >
                예매 완료! 🎫
              </button>
            </div>
          </section>
        </aside>
      ) : (
        /* 접힌 상태 — 작은 토글 버튼만 */
        <button
          onClick={() => setOpen(true)}
          className="pointer-events-auto absolute bottom-4 left-4 z-20 flex items-center gap-2 rounded-full border border-gray-100 bg-white/95 px-4 py-3 text-sm font-bold shadow-lg backdrop-blur transition hover:scale-105 dark:border-gray-700 dark:bg-gray-800/95 md:bottom-6 md:left-6"
          title="예매 패널 열기"
        >
          <span className="rounded bg-[#2AC1BC] px-1.5 py-0.5 text-xs text-white">
            🎫
          </span>
          예매 패널 열기
          {(departureId || arrivalId) && (
            <span className="ml-1 rounded-full bg-[#2AC1BC]/10 px-2 py-0.5 text-[10px] text-[#2AC1BC]">
              선택 중
            </span>
          )}
        </button>
      )}
    </div>
  );
}
