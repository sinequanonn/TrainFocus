'use client';

import dynamic from 'next/dynamic';
import type { Station } from '@/lib/api/stations';

export interface LiveRunner {
  userId: number;
  nickname: string;
  isRunning: boolean;
  color: string;
  departure: { lat: number; lng: number; name: string };
  arrival: { lat: number; lng: number; name: string };
  progress: number;
}

const KoreaMapInner = dynamic(
  () => import('./KoreaMapInner').then((m) => m.KoreaMapInner),
  {
    ssr: false,
    loading: () => (
      <div className="flex h-full w-full items-center justify-center bg-gray-100 dark:bg-gray-800">
        <span className="text-sm text-gray-400">지도 로딩 중...</span>
      </div>
    ),
  }
);

export interface KoreaMapProps {
  stations: Station[];
  departureId: string;
  arrivalId: string;
  onStationClick: (id: string) => void;
  progress?: number;
  active?: boolean;
  /** 마커 디자인 시험용 — 'circle' | 'pin' | 'train' 한 줄 바꿔서 시도 */
  markerVariant?: 'circle' | 'pin' | 'train';
  /** 최대 줌 — 기본 16, focus 모드에서는 18 권장 */
  maxZoom?: number;
  /** 같은 방 동료들의 실시간 진행 — 본인 외 멤버 */
  liveRunners?: LiveRunner[];
}

export function KoreaMap(props: KoreaMapProps) {
  return <KoreaMapInner {...props} />;
}
