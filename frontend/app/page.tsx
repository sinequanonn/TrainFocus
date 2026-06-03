'use client';

import { useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/hooks/useAuth';
import { signOut } from '@/lib/firebase/auth';
import { getMe, MeResponse } from '@/lib/api/auth';
import { getStations, Station } from '@/lib/api/stations';
import { getDuration } from '@/lib/api/routes';
import {
  createSession,
  pauseSession,
  resumeSession,
  completeSession,
  abortSession,
  getActiveSession,
  FocusSessionDetailResponse,
} from '@/lib/api/sessions';
import { ApiError } from '@/lib/api/client';
import { GuideModal } from '@/components/ui/GuideModal';
import { getMyRooms, getRoomLive, RoomResponse } from '@/lib/api/rooms';
import { LiveRunner } from '@/components/map/KoreaMap';
import { BookingScreen } from '@/components/booking/BookingScreen';
import { TicketScreen } from '@/components/booking/TicketScreen';
import { FocusScreen } from '@/components/focus/FocusScreen';
import { ArrivalModal } from '@/components/focus/ArrivalModal';

const GUIDE_SEEN_KEY = 'trainfocus.guide.seen';

const LIVE_POLL_INTERVAL_MS = 5000;
const RUNNER_COLORS = [
  '#2AC1BC',
  '#F59E0B',
  '#8B5CF6',
  '#EC4899',
  '#10B981',
  '#3B82F6',
];
function colorForUser(userId: number) {
  return RUNNER_COLORS[Math.abs(userId) % RUNNER_COLORS.length];
}

type Screen = 'booking' | 'ticket' | 'focus';

export default function HomePage() {
  const router = useRouter();
  const { user, loading } = useAuth();

  const [me, setMe] = useState<MeResponse | null>(null);
  const [stations, setStations] = useState<Station[]>([]);

  // 예매 폼
  const [departureId, setDepartureId] = useState<string>('');
  const [arrivalId, setArrivalId] = useState<string>('');
  const [delayMinutes, setDelayMinutes] = useState<number>(0);
  const [previewMinutes, setPreviewMinutes] = useState<number | null>(null);

  // 세션
  const [session, setSession] = useState<FocusSessionDetailResponse | null>(
    null
  );
  const [accumulatedSeconds, setAccumulatedSeconds] = useState(0);

  // 화면 단계
  const [screen, setScreen] = useState<Screen>('booking');

  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [guideOpen, setGuideOpen] = useState(false);
  const [myRooms, setMyRooms] = useState<RoomResponse[]>([]);
  const [selectedRoom, setSelectedRoom] = useState<RoomResponse | null>(null);
  const [liveRunners, setLiveRunners] = useState<LiveRunner[]>([]);
  const tickRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // 도착 안내 모달 — 수동/자동 도착 모두 사용
  const [arrivalInfo, setArrivalInfo] = useState<{
    arrivalName: string;
    accumulatedSeconds: number;
    totalTargetSeconds: number;
    auto: boolean;
    newDepartureName: string | null;
  } | null>(null);
  // 자동 도착 한 세션 1회 보장 — 세션 바뀌면 리셋
  const autoCompletedRef = useRef(false);

  // 첫 방문 가이드
  useEffect(() => {
    if (typeof window === 'undefined') return;
    const seen = window.localStorage.getItem(GUIDE_SEEN_KEY);
    if (!seen) {
      setGuideOpen(true);
      window.localStorage.setItem(GUIDE_SEEN_KEY, '1');
    }
  }, []);

  useEffect(() => {
    if (!loading && !user) router.replace('/login');
  }, [user, loading, router]);

  useEffect(() => {
    if (!user) return;
    (async () => {
      try {
        const [meRes, stationsRes, activeRes] = await Promise.all([
          getMe(),
          getStations(),
          getActiveSession(),
        ]);
        // 출발역 미설정 → 온보딩 화면으로
        if (meRes.departureStationId === null) {
          router.replace('/onboarding');
          return;
        }
        setMe(meRes);
        setStations(stationsRes.stations);
        // 출발역 기본값 = 사용자의 저장된 출발역
        setDepartureId(String(meRes.departureStationId));
        if (activeRes.hasActiveSession && activeRes.session) {
          setSession(activeRes.session);
          setAccumulatedSeconds(activeRes.session.accumulatedSeconds);
          setScreen('focus');
        }
      } catch (e) {
        handleError(e);
      }
    })();
  }, [user, router]);

  // 예매 미리보기 (출발/도착 둘 다 있으면)
  useEffect(() => {
    if (!departureId || !arrivalId || departureId === arrivalId) {
      setPreviewMinutes(null);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const res = await getDuration(Number(departureId), Number(arrivalId));
        if (!cancelled) setPreviewMinutes(res.durationMinutes);
      } catch {
        if (!cancelled) setPreviewMinutes(null);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [departureId, arrivalId]);

  // 로컬 타이머
  useEffect(() => {
    if (tickRef.current) clearInterval(tickRef.current);
    if (session?.status === 'RUNNING') {
      tickRef.current = setInterval(() => {
        setAccumulatedSeconds((prev) => prev + 1);
      }, 1000);
    }
    return () => {
      if (tickRef.current) clearInterval(tickRef.current);
    };
  }, [session?.status]);

  // 세션 바뀌면 자동 도착 가드 ref 리셋 (다음 세션에서 다시 트리거 가능)
  useEffect(() => {
    autoCompletedRef.current = false;
  }, [session?.sessionId]);

  // 자동 도착 — 브라우저 타이머 기반 (D안)
  // RUNNING + 누적 ≥ 목표 도달 시 자동 complete 호출. 한 세션당 1회만.
  // 백엔드 조회 시 보정(A안)은 fallback 으로 유지됨.
  //
  // +1 마진: 브라우저 setInterval 과 백엔드 시간 계산이 미세하게 어긋날 때
  // 백엔드의 SESSION_TARGET_NOT_REACHED 거부를 회피.
  // 참고: [[발생가능한_오류모음들/브라우저-백엔드 시간 racing — 자동 도착]]
  const AUTO_ARRIVE_MARGIN_SECONDS = 1;
  useEffect(() => {
    if (!session) return;
    if (session.status !== 'RUNNING') return;
    if (autoCompletedRef.current) return;
    if (
      accumulatedSeconds <
      session.totalTargetSeconds + AUTO_ARRIVE_MARGIN_SECONDS
    )
      return;

    autoCompletedRef.current = true;
    void handleAutoComplete();
    // handleAutoComplete 는 함수형이라 deps 에서 안전하게 제외
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [accumulatedSeconds, session?.status, session?.totalTargetSeconds]);

  // 내 방 목록 — 헤더 드롭다운 옵션
  useEffect(() => {
    if (!user) return;
    let cancelled = false;
    (async () => {
      try {
        const rooms = await getMyRooms();
        if (!cancelled) setMyRooms(rooms);
      } catch (e) {
        if (e instanceof Error) console.warn('[my rooms]', e.message);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [user]);

  // 선택된 방의 동료 라이브 폴링 (본인 제외)
  useEffect(() => {
    if (!selectedRoom || !me) {
      setLiveRunners([]);
      return;
    }
    let cancelled = false;
    const myUserId = me.userId;
    const roomId = selectedRoom.id;
    const fetchLive = async () => {
      try {
        const list = await getRoomLive(roomId);
        if (cancelled) return;
        const runners: LiveRunner[] = list
          .filter((m) => m.session !== null && m.userId !== myUserId)
          .map((m) => {
            const s = m.session!;
            const ratio = Math.min(
              1,
              s.accumulatedSeconds / Math.max(1, s.totalTargetSeconds)
            );
            return {
              userId: m.userId,
              nickname: m.nickname,
              isRunning: s.status === 'RUNNING',
              color: colorForUser(m.userId),
              departure: {
                lat: Number(s.departure.latitude),
                lng: Number(s.departure.longitude),
                name: s.departure.name,
              },
              arrival: {
                lat: Number(s.arrival.latitude),
                lng: Number(s.arrival.longitude),
                name: s.arrival.name,
              },
              progress: ratio,
            };
          });
        setLiveRunners(runners);
      } catch (e) {
        if (e instanceof Error) console.warn('[live poll]', e.message);
      }
    };
    void fetchLive();
    const id = setInterval(fetchLive, LIVE_POLL_INTERVAL_MS);
    return () => {
      cancelled = true;
      clearInterval(id);
    };
  }, [selectedRoom, me]);

  function handleError(e: unknown) {
    if (e instanceof ApiError) {
      setError(`[${e.status}] ${e.errorCode}: ${e.message}`);
    } else if (e instanceof Error) {
      setError(e.message);
    } else {
      setError('알 수 없는 오류');
    }
  }

  async function handleLogout() {
    await signOut();
    router.replace('/login');
  }

  function handleStationClick(stationId: string) {
    if (screen !== 'booking') return;
    // 출발역은 사용자가 온보딩/마이페이지에서 설정한 값으로 고정 — 지도에서는 도착역만 선택
    if (stationId === departureId) return;
    setArrivalId(stationId);
  }

  // 1단계: 예매 완료! → 표 화면 (백엔드 호출 없음)
  function handleBookingSubmit() {
    if (!departureId || !arrivalId || departureId === arrivalId) return;
    if (previewMinutes === null) return;
    setError(null);
    setScreen('ticket');
  }

  // 2단계: 탑승하기 → 백엔드 세션 생성 → 집중 화면
  async function handleBoardTrain() {
    setError(null);
    setBusy(true);
    try {
      await createSession({
        departureStationId: Number(departureId),
        arrivalStationId: Number(arrivalId),
        delayMinutes: delayMinutes || 0,
      });
      const active = await getActiveSession();
      if (active.hasActiveSession && active.session) {
        setSession(active.session);
        setAccumulatedSeconds(active.session.accumulatedSeconds);
        setScreen('focus');
      }
    } catch (e) {
      handleError(e);
    } finally {
      setBusy(false);
    }
  }

  async function handlePause() {
    if (!session) return;
    setBusy(true);
    try {
      const res = await pauseSession(session.sessionId);
      setSession({ ...session, status: res.status });
      setAccumulatedSeconds(res.accumulatedSeconds);
    } catch (e) {
      handleError(e);
    } finally {
      setBusy(false);
    }
  }

  async function handleResume() {
    if (!session) return;
    setBusy(true);
    try {
      const res = await resumeSession(session.sessionId);
      setSession({ ...session, status: res.status });
      setAccumulatedSeconds(res.accumulatedSeconds);
    } catch (e) {
      handleError(e);
    } finally {
      setBusy(false);
    }
  }

  async function handleComplete() {
    if (!session) return;
    setBusy(true);
    try {
      const res = await completeSession(session.sessionId);
      applyDepartureChange(res.newDepartureStationId, res.newDepartureStationName);
      // 모달 띄우고 닫을 때 booking 으로 복귀
      setArrivalInfo({
        arrivalName: session.arrival.name,
        accumulatedSeconds,
        totalTargetSeconds: session.totalTargetSeconds,
        auto: false,
        newDepartureName: res.newDepartureStationName,
      });
    } catch (e) {
      handleError(e);
    } finally {
      setBusy(false);
    }
  }

  async function handleAutoComplete() {
    if (!session) return;
    setBusy(true);
    try {
      const res = await completeSession(session.sessionId);
      applyDepartureChange(res.newDepartureStationId, res.newDepartureStationName);
      setArrivalInfo({
        arrivalName: session.arrival.name,
        accumulatedSeconds: session.totalTargetSeconds, // 도달 시점 기준 cap
        totalTargetSeconds: session.totalTargetSeconds,
        auto: true,
        newDepartureName: res.newDepartureStationName,
      });
    } catch (e) {
      // 실패해도 ref 유지 — 무한 재시도 방지. 사용자가 수동 도착 버튼으로 재시도 가능.
      handleError(e);
    } finally {
      setBusy(false);
    }
  }

  function applyDepartureChange(
    newId: number | null,
    newName: string | null
  ) {
    if (newId === null || newName === null) return;
    setMe((prev) =>
      prev
        ? { ...prev, departureStationId: newId, departureStationName: newName }
        : prev
    );
    setDepartureId(String(newId));
  }

  function handleArrivalModalClose() {
    setArrivalInfo(null);
    resetToBooking();
  }

  async function handleAbort() {
    if (!session) return;
    if (!confirm('정말 종료할까요? 진행 기록은 보존됩니다.')) return;
    setBusy(true);
    try {
      await abortSession(session.sessionId);
      resetToBooking();
    } catch (e) {
      handleError(e);
    } finally {
      setBusy(false);
    }
  }

  function resetToBooking() {
    setSession(null);
    setAccumulatedSeconds(0);
    setDepartureId(me?.departureStationId ? String(me.departureStationId) : '');
    setArrivalId('');
    setDelayMinutes(0);
    setScreen('booking');
  }

  if (loading || !user) {
    return <main className="p-8 text-gray-500 dark:text-gray-400">로딩 중...</main>;
  }

  const isRunning = session?.status === 'RUNNING';
  const isFocusScreen = screen === 'focus';

  return (
    <main className="relative h-screen w-screen overflow-hidden">
      {/* 헤더 floating */}
      <header className="pointer-events-none absolute inset-x-0 top-0 z-30 flex items-start justify-end p-4 md:p-6">
        {!isFocusScreen && (
          <div className="pointer-events-auto mr-14 flex items-center gap-2 rounded-2xl border border-gray-100 bg-white/95 px-3 py-2 shadow-md backdrop-blur dark:border-gray-700 dark:bg-gray-800/95">
            {isRunning ? (
              <span
                className="cursor-not-allowed rounded-lg bg-gray-100 px-3 py-1.5 text-xs font-medium text-gray-300 dark:bg-gray-700 dark:text-gray-600"
                title="운행 중에는 이동할 수 없습니다"
              >
                내 프로필
              </span>
            ) : (
              <Link
                href="/profile"
                className="rounded-lg px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50 dark:text-gray-300 dark:hover:bg-gray-700"
              >
                내 프로필
              </Link>
            )}
            {isRunning ? (
              <span
                className="cursor-not-allowed rounded-lg bg-gray-100 px-3 py-1.5 text-xs font-medium text-gray-300 dark:bg-gray-700 dark:text-gray-600"
                title="운행 중에는 이동할 수 없습니다"
              >
                함께 몰입
              </span>
            ) : (
              <Link
                href="/rooms"
                className="rounded-lg px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50 dark:text-gray-300 dark:hover:bg-gray-700"
              >
                함께 몰입
              </Link>
            )}
            {isRunning ? (
              <span
                className="cursor-not-allowed rounded-lg bg-gray-100 px-3 py-1.5 text-xs font-medium text-gray-300 dark:bg-gray-700 dark:text-gray-600"
                title="운행 중에는 이동할 수 없습니다"
              >
                운행 일지
              </span>
            ) : (
              <Link
                href="/history"
                className="rounded-lg px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50 dark:text-gray-300 dark:hover:bg-gray-700"
              >
                운행 일지
              </Link>
            )}
            <button
              onClick={handleLogout}
              disabled={isRunning}
              className="rounded-lg px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50 disabled:cursor-not-allowed disabled:text-gray-300 dark:text-gray-300 dark:hover:bg-gray-700 dark:disabled:text-gray-600"
              title={isRunning ? '운행 중에는 로그아웃할 수 없습니다' : ''}
            >
              로그아웃
            </button>
          </div>
        )}
      </header>

      {error && (
        <div className="pointer-events-auto absolute left-1/2 top-20 z-30 -translate-x-1/2 rounded-xl border border-red-200 bg-red-50/95 px-4 py-2 text-sm text-red-700 shadow-md backdrop-blur dark:border-red-900 dark:bg-red-950/95 dark:text-red-300">
          {error}
        </div>
      )}

      {screen === 'booking' && (
        <BookingScreen
          stations={stations}
          departureId={departureId}
          arrivalId={arrivalId}
          delayMinutes={delayMinutes}
          previewMinutes={previewMinutes}
          onArrivalChange={setArrivalId}
          onDelayChange={setDelayMinutes}
          onStationClick={handleStationClick}
          onSubmit={handleBookingSubmit}
          busy={busy}
          liveRunners={liveRunners}
          myRooms={myRooms}
          selectedRoom={selectedRoom}
          onSelectRoom={setSelectedRoom}
          onOpenGuide={() => setGuideOpen(true)}
        />
      )}

      {screen === 'ticket' &&
        previewMinutes !== null &&
        departureId &&
        arrivalId && (
          <div className="absolute inset-0 z-10 flex items-center justify-center bg-black/30 backdrop-blur-sm">
            <TicketScreen
              departure={stations.find((s) => String(s.id) === departureId)!}
              arrival={stations.find((s) => String(s.id) === arrivalId)!}
              baseDurationMinutes={previewMinutes}
              delayMinutes={delayMinutes}
              onBack={() => setScreen('booking')}
              onConfirm={handleBoardTrain}
              busy={busy}
            />
          </div>
        )}

      {screen === 'focus' && session && (
        <FocusScreen
          stations={stations}
          departure={session.departure}
          arrival={session.arrival}
          totalTargetSeconds={session.totalTargetSeconds}
          accumulatedSeconds={accumulatedSeconds}
          status={session.status === 'RUNNING' ? 'RUNNING' : 'PAUSED'}
          busy={busy}
          onPause={handlePause}
          onResume={handleResume}
          onComplete={handleComplete}
          onAbort={handleAbort}
          liveRunners={liveRunners}
          myRooms={myRooms}
          selectedRoom={selectedRoom}
          onSelectRoom={setSelectedRoom}
        />
      )}

      <GuideModal open={guideOpen} onClose={() => setGuideOpen(false)} />

      {arrivalInfo && (
        <ArrivalModal
          open
          arrivalName={arrivalInfo.arrivalName}
          accumulatedSeconds={arrivalInfo.accumulatedSeconds}
          totalTargetSeconds={arrivalInfo.totalTargetSeconds}
          auto={arrivalInfo.auto}
          newDepartureName={arrivalInfo.newDepartureName}
          onClose={handleArrivalModalClose}
        />
      )}
    </main>
  );
}
