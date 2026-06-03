'use client';

import { use, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/hooks/useAuth';
import { getMe } from '@/lib/api/auth';
import {
  deleteRoom,
  getRoomDetail,
  getRoomLive,
  kickMember,
  leaveRoom,
  reIssueCode,
  renameRoom,
  RoomResponse,
  RoomRankingEntry,
  RoomUserLiveResponse,
  getRoomRanking,
  transferOwner,
} from '@/lib/api/rooms';
import { ApiError } from '@/lib/api/client';

export default function RoomDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const router = useRouter();
  const { user, loading } = useAuth();
  const { id } = use(params);
  const roomId = Number(id);

  const [room, setRoom] = useState<RoomResponse | null>(null);
  const [members, setMembers] = useState<RoomUserLiveResponse[]>([]);
  const [myUserId, setMyUserId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const [showInvite, setShowInvite] = useState(false);
  const [showRename, setShowRename] = useState(false);

  useEffect(() => {
    if (!loading && !user) router.replace('/login');
  }, [user, loading, router]);

  useEffect(() => {
    if (!user) return;
    void load();
  }, [user, roomId]);

  async function load() {
    setError(null);
    try {
      const [r, m, me] = await Promise.all([
        getRoomDetail(roomId),
        getRoomLive(roomId),
        getMe(),
      ]);
      setRoom(r);
      setMembers(m);
      setMyUserId(me.userId);
    } catch (e) {
      handleError(e);
    }
  }

  async function reload() {
    try {
      const [r, m] = await Promise.all([
        getRoomDetail(roomId),
        getRoomLive(roomId),
      ]);
      setRoom(r);
      setMembers(m);
    } catch (e) {
      handleError(e);
    }
  }

  function handleError(e: unknown) {
    if (e instanceof ApiError) {
      setError(`[${e.status}] ${e.errorCode}: ${e.message}`);
    } else if (e instanceof Error) {
      setError(e.message);
    }
  }

  async function handleCopyCode() {
    if (!room) return;
    try {
      await navigator.clipboard.writeText(room.code);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      /* ignore */
    }
  }

  async function handleReIssueCode() {
    if (
      !confirm(
        '새 초대 코드를 발급하면 이전 코드는 사용할 수 없습니다. 진행할까요?'
      )
    )
      return;
    setError(null);
    try {
      const updated = await reIssueCode(roomId);
      setRoom(updated);
    } catch (e) {
      handleError(e);
    }
  }

  async function handleLeave() {
    if (!confirm('정말 이 방에서 나가시겠어요?')) return;
    setError(null);
    try {
      await leaveRoom(roomId);
      router.push('/rooms');
    } catch (e) {
      handleError(e);
    }
  }

  async function handleDeleteRoom() {
    if (!confirm('방을 삭제하면 복구할 수 없습니다. 정말 삭제할까요?')) return;
    setError(null);
    try {
      await deleteRoom(roomId);
      router.push('/rooms');
    } catch (e) {
      handleError(e);
    }
  }

  async function handleKick(targetUserId: number, nickname: string) {
    if (!confirm(`${nickname} 님을 강퇴할까요?`)) return;
    setError(null);
    try {
      await kickMember(roomId, targetUserId);
      await reload();
    } catch (e) {
      handleError(e);
    }
  }

  async function handleTransfer(targetUserId: number, nickname: string) {
    if (
      !confirm(`${nickname} 님에게 방장을 양도할까요? 본인은 멤버가 됩니다.`)
    )
      return;
    setError(null);
    try {
      await transferOwner(roomId, targetUserId);
      await reload();
    } catch (e) {
      handleError(e);
    }
  }

  if (loading || !user) {
    return (
      <main className="p-8 text-gray-500 dark:text-gray-400">로딩 중...</main>
    );
  }

  return (
    <main className="mx-auto max-w-3xl p-6 md:p-10">
      <Link
        href="/rooms"
        className="mb-2 inline-block text-xs text-gray-400 hover:text-[#2AC1BC] dark:text-gray-500"
      >
        ← 방 목록
      </Link>

      {error && (
        <div className="mb-6 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:border-red-900 dark:bg-red-950 dark:text-red-300">
          {error}
        </div>
      )}

      {!room ? (
        <p className="text-sm text-gray-400 dark:text-gray-500">
          불러오는 중...
        </p>
      ) : (
        <>
          <section className="mb-6 rounded-3xl border border-gray-100 bg-white p-6 shadow-sm dark:border-gray-700 dark:bg-gray-800">
            <div className="mb-4 flex items-start justify-between">
              <span
                className={`rounded-full px-3 py-1 text-xs font-bold ${
                  room.role === 'OWNER'
                    ? 'bg-[#EBFBFA] text-[#2AC1BC] dark:bg-[#14302f]'
                    : 'bg-gray-100 text-gray-500 dark:bg-gray-700 dark:text-gray-400'
                }`}
              >
                {room.role === 'OWNER' ? '방장' : '멤버'}
              </span>
              <KebabMenu
                items={
                  room.role === 'OWNER'
                    ? [
                        {
                          label: '이름 수정',
                          onClick: () => setShowRename(true),
                        },
                        {
                          label: '방 삭제',
                          onClick: handleDeleteRoom,
                          danger: true,
                        },
                      ]
                    : [
                        {
                          label: '방 나가기',
                          onClick: handleLeave,
                          danger: true,
                        },
                      ]
                }
              />
            </div>

            <h1 className="text-2xl font-bold dark:text-gray-100 md:text-3xl">
              {room.name}
            </h1>

            <div className="mt-6 border-t border-gray-100 pt-5 dark:border-gray-700">
              <div className="mb-4 flex items-center justify-between">
                <h2 className="text-lg font-bold dark:text-gray-100">
                  멤버 ({members.length})
                </h2>
                <button
                  onClick={() => setShowInvite(true)}
                  className="rounded-full border border-gray-200 px-3 py-1.5 text-xs font-medium hover:border-[#2AC1BC] hover:text-[#2AC1BC] dark:border-gray-600 dark:text-gray-200"
                >
                  + 초대
                </button>
              </div>
              {members.length === 0 ? (
                <p className="text-sm italic text-gray-400 dark:text-gray-500">
                  멤버가 없습니다.
                </p>
              ) : (
                <ul className="space-y-2">
                  {members.map((m) => {
                    const isMe = m.userId === myUserId;
                    return (
                      <li
                        key={m.userId}
                        className="rounded-2xl border border-gray-100 bg-gray-50 p-4 dark:border-gray-700 dark:bg-gray-700"
                      >
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-3">
                            <span className="text-sm font-medium text-gray-800 dark:text-gray-100">
                              {m.nickname}
                              {isMe && (
                                <span className="ml-1 text-xs text-gray-400 dark:text-gray-500">
                                  (나)
                                </span>
                              )}
                            </span>
                            <span
                              className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${
                                m.role === 'OWNER'
                                  ? 'bg-[#EBFBFA] text-[#2AC1BC] dark:bg-[#14302f]'
                                  : 'bg-gray-100 text-gray-500 dark:bg-gray-700 dark:text-gray-400'
                              }`}
                            >
                              {m.role === 'OWNER' ? '방장' : '멤버'}
                            </span>
                          </div>
                          {room.role === 'OWNER' && !isMe && (
                            <KebabMenu
                              items={[
                                {
                                  label: '방장 양도',
                                  onClick: () =>
                                    handleTransfer(m.userId, m.nickname),
                                },
                                {
                                  label: '강퇴',
                                  onClick: () =>
                                    handleKick(m.userId, m.nickname),
                                  danger: true,
                                },
                              ]}
                            />
                          )}
                        </div>
                        <MemberStatus session={m.session} />
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>
          </section>

          <RoomRanking roomId={roomId} myUserId={myUserId} />

          {showInvite && (
            <InviteModal
              code={room.code}
              copied={copied}
              canReIssue={room.role === 'OWNER'}
              onCopy={handleCopyCode}
              onReIssue={handleReIssueCode}
              onClose={() => setShowInvite(false)}
            />
          )}

          {showRename && (
            <RenameRoomModal
              roomId={roomId}
              currentName={room.name}
              onClose={() => setShowRename(false)}
              onRenamed={(updated) => {
                setRoom(updated);
                setShowRename(false);
              }}
              onError={handleError}
            />
          )}
        </>
      )}
    </main>
  );
}

type RankWindow = 'day' | 'week' | 'month';

const RANK_WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토'];

const RANK_WINDOW_TABS: Record<RankWindow, string> = {
  day: '오늘',
  week: '주간',
  month: '월간',
};

function rankToday(): Date {
  const t = new Date();
  t.setHours(0, 0, 0, 0);
  return t;
}

function rankMonthCells(year: number, month: number): (Date | null)[] {
  const first = new Date(year, month, 1);
  const pad = first.getDay();
  const total = new Date(year, month + 1, 0).getDate();
  const cells: (Date | null)[] = [];
  for (let i = 0; i < pad; i++) cells.push(null);
  for (let d = 1; d <= total; d++) cells.push(new Date(year, month, d));
  return cells;
}

function rankDateStr(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(
    d.getDate()
  ).padStart(2, '0')}`;
}

function formatRun(sec: number): string {
  const h = Math.floor(sec / 3600);
  const m = Math.floor((sec % 3600) / 60);
  if (h > 0 && m > 0) return `${h}시간 ${m}분`;
  if (h > 0) return `${h}시간`;
  return `${m}분`;
}

function RoomRanking({
  roomId,
  myUserId,
}: {
  roomId: number;
  myUserId: number | null;
}) {
  const [selected, setSelected] = useState<Date>(() => rankToday());
  const [range, setRange] = useState<RankWindow>('day');
  const [entries, setEntries] = useState<RoomRankingEntry[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let alive = true;
    setLoading(true);
    getRoomRanking(roomId, rankDateStr(selected), range)
      .then((r) => {
        if (alive) {
          setEntries(r.entries);
          setLoading(false);
        }
      })
      .catch(() => {
        if (alive) {
          setEntries([]);
          setLoading(false);
        }
      });
    return () => {
      alive = false;
    };
  }, [roomId, selected, range]);

  const max = entries[0]?.runSeconds ?? 0;

  return (
    <section className="mb-6 rounded-3xl border border-gray-100 bg-white p-6 shadow-sm dark:border-gray-700 dark:bg-gray-800">
      <h2 className="mb-4 text-lg font-bold dark:text-gray-100">몰입 순위</h2>

      <div className="flex flex-col gap-5 lg:flex-row lg:items-start">
        <div className="lg:w-[300px] lg:shrink-0">
          <RankCalendar selected={selected} onSelect={setSelected} />
        </div>

        <div className="min-w-0 flex-1">
          <div className="mb-3 flex gap-1 rounded-full bg-gray-100 p-1 dark:bg-gray-700">
            {(['day', 'week', 'month'] as RankWindow[]).map((w) => (
              <button
                key={w}
                onClick={() => setRange(w)}
                className={`flex-1 rounded-full px-3 py-1 text-xs font-medium transition ${
                  range === w
                    ? 'bg-white text-[#2AC1BC] shadow-sm dark:bg-gray-800'
                    : 'text-gray-500 dark:text-gray-400'
                }`}
              >
                {RANK_WINDOW_TABS[w]}
              </button>
            ))}
          </div>

          {loading ? (
            <p className="text-sm text-gray-400 dark:text-gray-500">
              불러오는 중...
            </p>
          ) : entries.length === 0 ? (
            <p className="text-sm text-gray-400 dark:text-gray-500">
              기록이 없습니다.
            </p>
          ) : (
            <ol className="space-y-2">
              {entries.map((e) => {
                const isMe = e.userId === myUserId;
                return (
                  <li
                    key={e.userId}
                    className={`flex items-center gap-3 rounded-2xl border p-3 ${
                      isMe
                        ? 'border-[#2AC1BC] bg-[#EBFBFA] dark:bg-[#14302f]'
                        : 'border-gray-100 bg-gray-50 dark:border-gray-700 dark:bg-gray-700'
                    }`}
                  >
                    <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-gray-100 text-sm font-bold text-gray-500 dark:bg-gray-600 dark:text-gray-300">
                      {e.rank}
                    </span>
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-1.5">
                        <span className="truncate text-sm font-bold text-gray-800 dark:text-gray-100">
                          {e.nickname}
                        </span>
                        {isMe && (
                          <span className="text-xs text-gray-400 dark:text-gray-500">
                            (나)
                          </span>
                        )}
                      </div>
                      <div className="mt-1.5 h-1.5 w-full overflow-hidden rounded-full bg-gray-200 dark:bg-gray-600">
                        <div
                          className="h-full rounded-full bg-[#2AC1BC]"
                          style={{
                            width:
                              max > 0 ? `${(e.runSeconds / max) * 100}%` : '0%',
                          }}
                        />
                      </div>
                    </div>
                    <span
                      className={`shrink-0 text-sm font-bold tabular-nums ${
                        e.runSeconds > 0
                          ? 'text-[#2AC1BC]'
                          : 'text-gray-400 dark:text-gray-500'
                      }`}
                    >
                      {e.runSeconds > 0 ? formatRun(e.runSeconds) : '기록 없음'}
                    </span>
                  </li>
                );
              })}
            </ol>
          )}
        </div>
      </div>
    </section>
  );
}

function RankCalendar({
  selected,
  onSelect,
}: {
  selected: Date;
  onSelect: (d: Date) => void;
}) {
  const today = rankToday();
  const [view, setView] = useState(() => ({
    year: selected.getFullYear(),
    month: selected.getMonth(),
  }));

  const cells = rankMonthCells(view.year, view.month);
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
        {RANK_WEEKDAYS.map((w) => (
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
          const isSel = cell.getTime() === selected.getTime();
          return (
            <button
              key={i}
              onClick={() => onSelect(cell)}
              disabled={isFuture}
              className={`flex aspect-square flex-col items-center justify-center rounded-xl text-sm transition ${
                isFuture
                  ? 'text-gray-300 dark:text-gray-600'
                  : 'text-gray-600 hover:bg-gray-50 dark:text-gray-300 dark:hover:bg-gray-700'
              } ${isSel ? 'ring-2 ring-[#2AC1BC]' : ''} ${
                isToday ? 'border border-[#2AC1BC]/50' : ''
              }`}
            >
              <span>{cell.getDate()}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}

function MemberStatus({
  session,
}: {
  session: RoomUserLiveResponse['session'];
}) {
  const { label, dot, text } = (() => {
    if (!session) {
      return {
        label: '대기 중',
        dot: 'bg-gray-300 dark:bg-gray-500',
        text: 'text-gray-500 dark:text-gray-400',
      };
    }
    if (session.status === 'RUNNING') {
      return {
        label: `집중 중 · → ${session.arrival.name}`,
        dot: 'bg-[#2AC1BC]',
        text: 'text-[#2AC1BC]',
      };
    }
    return {
      label: '쉬는 중',
      dot: 'bg-amber-400',
      text: 'text-amber-600 dark:text-amber-400',
    };
  })();

  return (
    <div className={`mt-2 flex items-center gap-1.5 text-xs ${text}`}>
      <span className={`inline-block h-2 w-2 rounded-full ${dot}`} />
      <span className="font-medium">{label}</span>
    </div>
  );
}

type KebabItem = {
  label: string;
  onClick: () => void;
  danger?: boolean;
};

function KebabMenu({ items }: { items: KebabItem[] }) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    function onDocClick(e: MouseEvent) {
      if (!ref.current?.contains(e.target as Node)) setOpen(false);
    }
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') setOpen(false);
    }
    document.addEventListener('mousedown', onDocClick);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onDocClick);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  if (items.length === 0) return null;

  return (
    <div ref={ref} className="relative">
      <button
        onClick={() => setOpen((v) => !v)}
        aria-label="더보기"
        className="flex h-9 w-9 items-center justify-center rounded-full text-gray-400 hover:bg-gray-100 dark:text-gray-500 dark:hover:bg-gray-700"
      >
        <span className="text-xl leading-none">⋯</span>
      </button>
      {open && (
        <div className="absolute right-0 z-20 mt-1 w-36 overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-lg dark:border-gray-700 dark:bg-gray-800">
          {items.map((item, i) => (
            <button
              key={i}
              onClick={() => {
                setOpen(false);
                item.onClick();
              }}
              className={`block w-full px-4 py-2 text-left text-sm font-medium ${
                item.danger
                  ? 'text-red-600 hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-950'
                  : 'text-gray-700 hover:bg-gray-50 dark:text-gray-200 dark:hover:bg-gray-700'
              }`}
            >
              {item.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function InviteModal({
  code,
  copied,
  canReIssue,
  onCopy,
  onReIssue,
  onClose,
}: {
  code: string;
  copied: boolean;
  canReIssue: boolean;
  onCopy: () => void;
  onReIssue: () => void;
  onClose: () => void;
}) {
  return (
    <div
      onClick={onClose}
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl dark:bg-gray-800"
      >
        <h2 className="mb-1 text-lg font-bold dark:text-gray-100">방 초대</h2>
        <p className="mb-5 text-xs text-gray-500 dark:text-gray-400">
          아래 코드를 공유하면 함께 달릴 수 있어요.
        </p>

        <div className="mb-4 rounded-2xl border border-gray-100 bg-gray-50 p-5 text-center dark:border-gray-700 dark:bg-gray-900">
          <p className="font-mono text-3xl font-bold tracking-widest text-[#2AC1BC]">
            {code}
          </p>
        </div>

        <div className="flex justify-end gap-2">
          {canReIssue && (
            <button
              onClick={onReIssue}
              className="rounded-full px-4 py-2 text-sm font-medium text-gray-500 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-700"
            >
              재발급
            </button>
          )}
          <button
            onClick={onCopy}
            className="rounded-full bg-[#2AC1BC] px-4 py-2 text-sm font-bold text-white hover:bg-[#239994]"
          >
            {copied ? '복사됨' : '코드 복사'}
          </button>
        </div>
      </div>
    </div>
  );
}

function RenameRoomModal({
  roomId,
  currentName,
  onClose,
  onRenamed,
  onError,
}: {
  roomId: number;
  currentName: string;
  onClose: () => void;
  onRenamed: (room: RoomResponse) => void;
  onError: (e: unknown) => void;
}) {
  const [name, setName] = useState(currentName);
  const [submitting, setSubmitting] = useState(false);

  const trimmed = name.trim();
  const unchanged = trimmed === currentName;

  async function submit() {
    if (!trimmed || unchanged) return;
    setSubmitting(true);
    try {
      const updated = await renameRoom(roomId, trimmed);
      onRenamed(updated);
    } catch (e) {
      onError(e);
      setSubmitting(false);
    }
  }

  return (
    <div
      onClick={onClose}
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl dark:bg-gray-800"
      >
        <h2 className="mb-4 text-lg font-bold dark:text-gray-100">
          방 이름 수정
        </h2>
        <input
          autoFocus
          value={name}
          onChange={(e) => setName(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') void submit();
          }}
          placeholder="방 이름 (최대 20자)"
          maxLength={20}
          className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm dark:border-gray-700 dark:bg-gray-900 dark:text-gray-100"
        />
        <div className="mt-4 flex justify-end gap-2">
          <button
            onClick={onClose}
            className="rounded-full px-4 py-2 text-sm text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700"
          >
            취소
          </button>
          <button
            onClick={submit}
            disabled={!trimmed || unchanged || submitting}
            className="rounded-full bg-[#2AC1BC] px-4 py-2 text-sm font-bold text-white hover:bg-[#239994] disabled:opacity-50"
          >
            {submitting ? '저장 중...' : '저장'}
          </button>
        </div>
      </div>
    </div>
  );
}
