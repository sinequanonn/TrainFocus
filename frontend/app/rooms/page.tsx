'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/hooks/useAuth';
import {
  createRoom,
  getMyRooms,
  joinRoom,
  RoomResponse,
} from '@/lib/api/rooms';
import { ApiError } from '@/lib/api/client';

export default function RoomsPage() {
  const router = useRouter();
  const { user, loading } = useAuth();

  const [rooms, setRooms] = useState<RoomResponse[]>([]);
  const [error, setError] = useState<string | null>(null);

  const [showCreate, setShowCreate] = useState(false);
  const [showJoin, setShowJoin] = useState(false);

  useEffect(() => {
    if (!loading && !user) router.replace('/login');
  }, [user, loading, router]);

  useEffect(() => {
    if (!user) return;
    void loadRooms();
  }, [user]);

  async function loadRooms() {
    setError(null);
    try {
      const list = await getMyRooms();
      setRooms(list);
    } catch (e) {
      if (e instanceof ApiError) {
        setError(`[${e.status}] ${e.errorCode}: ${e.message}`);
      } else if (e instanceof Error) {
        setError(e.message);
      }
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
        href="/"
        className="mb-2 inline-block text-xs text-gray-400 hover:text-[#2AC1BC] dark:text-gray-500"
      >
        ← 홈
      </Link>

      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl font-bold dark:text-gray-100 md:text-3xl">
          함께 몰입
        </h1>
        <div className="flex gap-2">
          <button
            onClick={() => setShowJoin(true)}
            className="rounded-full border border-gray-200 px-4 py-2 text-sm font-medium hover:bg-gray-50 dark:border-gray-700 dark:text-gray-200 dark:hover:bg-gray-800"
          >
            방 입장
          </button>
          <button
            onClick={() => setShowCreate(true)}
            className="rounded-full bg-[#2AC1BC] px-4 py-2 text-sm font-bold text-white hover:bg-[#239994]"
          >
            방 만들기
          </button>
        </div>
      </div>

      {error && (
        <div className="mb-6 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:border-red-900 dark:bg-red-950 dark:text-red-300">
          {error}
        </div>
      )}

      {rooms.length === 0 ? (
        <p className="text-sm italic text-gray-400 dark:text-gray-500">
          참여 중인 방이 없습니다.
        </p>
      ) : (
        <ul className="space-y-3">
          {rooms.map((room) => (
            <li key={room.id}>
              <Link
                href={`/rooms/${room.id}`}
                className="block rounded-3xl border border-gray-100 bg-white p-5 shadow-sm transition hover:border-[#2AC1BC] dark:border-gray-700 dark:bg-gray-800 dark:hover:border-[#2AC1BC]"
              >
                <div className="flex items-center justify-between">
                  <h2 className="text-lg font-bold dark:text-gray-100">
                    {room.name}
                  </h2>
                  <span
                    className={`rounded-full px-3 py-1 text-xs font-bold ${
                      room.role === 'OWNER'
                        ? 'bg-[#EBFBFA] text-[#2AC1BC] dark:bg-[#14302f]'
                        : 'bg-gray-100 text-gray-500 dark:bg-gray-700 dark:text-gray-400'
                    }`}
                  >
                    {room.role === 'OWNER' ? '방장' : '멤버'}
                  </span>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}

      {showCreate && (
        <CreateRoomModal
          onClose={() => setShowCreate(false)}
          onCreated={() => {
            setShowCreate(false);
            void loadRooms();
          }}
        />
      )}
      {showJoin && (
        <JoinRoomModal
          onClose={() => setShowJoin(false)}
          onJoined={(room) => {
            setShowJoin(false);
            router.push(`/rooms/${room.id}`);
          }}
        />
      )}
    </main>
  );
}

function CreateRoomModal({
  onClose,
  onCreated,
}: {
  onClose: () => void;
  onCreated: () => void;
}) {
  const [name, setName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function submit() {
    setError(null);
    setSubmitting(true);
    try {
      await createRoom(name.trim());
      onCreated();
    } catch (e) {
      if (e instanceof ApiError) setError(e.message);
      else if (e instanceof Error) setError(e.message);
      setSubmitting(false);
    }
  }

  return (
    <ModalShell title="방 만들기" onClose={onClose}>
      <input
        autoFocus
        value={name}
        onChange={(e) => setName(e.target.value)}
        placeholder="방 이름 (최대 20자)"
        maxLength={20}
        className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm dark:border-gray-700 dark:bg-gray-900 dark:text-gray-100"
      />
      {error && <p className="mt-2 text-xs text-red-600">{error}</p>}
      <div className="mt-4 flex justify-end gap-2">
        <button
          onClick={onClose}
          className="rounded-full px-4 py-2 text-sm text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700"
        >
          취소
        </button>
        <button
          onClick={submit}
          disabled={!name.trim() || submitting}
          className="rounded-full bg-[#2AC1BC] px-4 py-2 text-sm font-bold text-white hover:bg-[#239994] disabled:opacity-50"
        >
          {submitting ? '생성 중...' : '만들기'}
        </button>
      </div>
    </ModalShell>
  );
}

function JoinRoomModal({
  onClose,
  onJoined,
}: {
  onClose: () => void;
  onJoined: (room: RoomResponse) => void;
}) {
  const [code, setCode] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function submit() {
    setError(null);
    setSubmitting(true);
    try {
      const joined = await joinRoom(code.trim().toUpperCase());
      onJoined(joined);
    } catch (e) {
      if (e instanceof ApiError) setError(e.message);
      else if (e instanceof Error) setError(e.message);
      setSubmitting(false);
    }
  }

  return (
    <ModalShell title="방 입장" onClose={onClose}>
      <input
        autoFocus
        value={code}
        onChange={(e) =>
          setCode(e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, ''))
        }
        placeholder="초대 코드 8자리"
        maxLength={8}
        className="w-full rounded-xl border border-gray-200 px-4 py-3 text-center font-mono text-lg tracking-widest dark:border-gray-700 dark:bg-gray-900 dark:text-gray-100"
      />
      {error && <p className="mt-2 text-xs text-red-600">{error}</p>}
      <div className="mt-4 flex justify-end gap-2">
        <button
          onClick={onClose}
          className="rounded-full px-4 py-2 text-sm text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700"
        >
          취소
        </button>
        <button
          onClick={submit}
          disabled={code.length !== 8 || submitting}
          className="rounded-full bg-[#2AC1BC] px-4 py-2 text-sm font-bold text-white hover:bg-[#239994] disabled:opacity-50"
        >
          {submitting ? '입장 중...' : '입장'}
        </button>
      </div>
    </ModalShell>
  );
}

function ModalShell({
  title,
  onClose,
  children,
}: {
  title: string;
  onClose: () => void;
  children: React.ReactNode;
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
        <h2 className="mb-4 text-lg font-bold dark:text-gray-100">{title}</h2>
        {children}
      </div>
    </div>
  );
}
