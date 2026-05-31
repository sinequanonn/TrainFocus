'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import {
  MapContainer,
  TileLayer,
  Marker,
  Polyline,
  useMap,
} from 'react-leaflet';
import { divIcon, type LatLngBoundsExpression } from 'leaflet';
import 'leaflet/dist/leaflet.css';
import type { Station } from '@/lib/api/stations';
import type { LiveRunner } from './KoreaMap';

// 한반도 대략 bbox (제주 포함)
const KOREA_BOUNDS: LatLngBoundsExpression = [
  [33.0, 124.5],
  [39.0, 131.5],
];

// 타일 정의
//  - map: CartoDB Positron (라이트/다크 자동 전환)
//  - satellite: Esri World Imagery (위성, 무료, API 키 불필요)
type TileType = 'map' | 'satellite';

const TILES: Record<TileType, { light: { url: string; attribution: string }; dark: { url: string; attribution: string } }> = {
  map: {
    light: {
      url: 'https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png',
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/attributions">CARTO</a>',
    },
    dark: {
      url: 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/attributions">CARTO</a>',
    },
  },
  satellite: {
    light: {
      url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
      attribution: 'Tiles &copy; Esri &mdash; Source: Esri, Maxar, Earthstar Geographics',
    },
    // 위성은 다크 별도 없음 — 동일 사용
    dark: {
      url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
      attribution: 'Tiles &copy; Esri &mdash; Source: Esri, Maxar, Earthstar Geographics',
    },
  },
};

function useDarkMode() {
  const [isDark, setIsDark] = useState(false);
  useEffect(() => {
    const html = document.documentElement;
    const sync = () => setIsDark(html.classList.contains('dark'));
    sync();
    const observer = new MutationObserver(sync);
    observer.observe(html, { attributes: true, attributeFilter: ['class'] });
    return () => observer.disconnect();
  }, []);
  return isDark;
}

interface Props {
  stations: Station[];
  departureId: string;
  arrivalId: string;
  onStationClick: (id: string) => void;
  progress?: number;
  active?: boolean;
  markerVariant?: 'circle' | 'pin' | 'train';
  maxZoom?: number;
  liveRunners?: LiveRunner[];
}

function makeStationIcon(opts: {
  name: string;
  selected: boolean;
  variant: 'circle' | 'pin' | 'train';
}) {
  const { name, selected, variant } = opts;
  let html: string;

  if (variant === 'circle') {
    html = `
      <div style="display:flex;flex-direction:column;align-items:center;cursor:pointer;">
        <div style="
          width:${selected ? 14 : 10}px;
          height:${selected ? 14 : 10}px;
          background:${selected ? '#2AC1BC' : '#94A3B8'};
          border:2px solid white;
          border-radius:9999px;
          box-shadow:0 1px 2px rgba(0,0,0,0.2);
        "></div>
        <span style="
          margin-top:2px;
          padding:1px 4px;
          font-size:10px;
          font-weight:700;
          color:${selected ? 'white' : '#374151'};
          background:${selected ? '#2AC1BC' : 'rgba(255,255,255,0.85)'};
          border-radius:3px;
          white-space:nowrap;
        ">${name}</span>
      </div>`;
  } else if (variant === 'pin') {
    html = `
      <div style="display:flex;flex-direction:column;align-items:center;cursor:pointer;">
        <div style="
          width:${selected ? 26 : 22}px;
          height:${selected ? 26 : 22}px;
          background:${selected ? '#2AC1BC' : 'white'};
          color:${selected ? 'white' : '#2AC1BC'};
          border:2px solid ${selected ? 'white' : '#2AC1BC'};
          border-radius:9999px;
          display:flex;
          align-items:center;
          justify-content:center;
          font-size:11px;
          font-weight:700;
          box-shadow:0 2px 4px rgba(0,0,0,0.15);
        ">📍</div>
        <span style="
          margin-top:2px;
          padding:1px 4px;
          font-size:10px;
          font-weight:700;
          color:#374151;
          background:rgba(255,255,255,0.9);
          border-radius:3px;
          white-space:nowrap;
        ">${name}</span>
      </div>`;
  } else {
    html = `
      <div style="display:flex;flex-direction:column;align-items:center;cursor:pointer;">
        <div style="
          width:${selected ? 32 : 26}px;
          height:${selected ? 32 : 26}px;
          background:${selected ? '#2AC1BC' : 'white'};
          border:2px solid ${selected ? 'white' : '#2AC1BC'};
          border-radius:9999px;
          display:flex;
          align-items:center;
          justify-content:center;
          font-size:${selected ? 16 : 14}px;
          box-shadow:0 2px 6px rgba(0,0,0,0.18);
        ">🚆</div>
        <span style="
          margin-top:2px;
          padding:1px 4px;
          font-size:10px;
          font-weight:700;
          color:#374151;
          background:rgba(255,255,255,0.9);
          border-radius:3px;
          white-space:nowrap;
        ">${name}</span>
      </div>`;
  }

  return divIcon({
    html,
    className: '',
    iconSize: [0, 0],
    iconAnchor: [0, 0],
  });
}

/** 동료 마커 — 방 라이브에서 본인 외 멤버 표시 */
function makeRunnerIcon(opts: {
  nickname: string;
  color: string;
  isRunning: boolean;
}) {
  const { nickname, color, isRunning } = opts;
  const size = 28;
  const opacity = isRunning ? 1 : 0.55;
  return divIcon({
    html: `
      <div style="display:flex;flex-direction:column;align-items:center;opacity:${opacity};">
        <div style="
          width:${size}px;height:${size}px;
          background:${color};
          color:white;
          border:2px solid rgba(255,255,255,0.85);
          border-radius:9999px;
          display:flex;align-items:center;justify-content:center;
          font-size:13px;
          box-shadow:0 2px 4px rgba(0,0,0,0.25);
        ">${isRunning ? '🏃' : '⏸'}</div>
        <span style="
          margin-top:2px;
          padding:1px 5px;
          font-size:9px;
          font-weight:700;
          color:white;
          background:${color};
          border-radius:6px;
          white-space:nowrap;
          max-width:80px;
          overflow:hidden;
          text-overflow:ellipsis;
        ">${nickname}</span>
      </div>`,
    className: '',
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
  });
}

/** 기차 아이콘 — focus 모드 현재 위치 (펄스 halo + 🚄) */
const TRAIN_DOT_ICON = divIcon({
  html: `
    <div style="position:relative;width:40px;height:40px;display:flex;align-items:center;justify-content:center;">
      <div style="
        position:absolute;inset:0;
        background:#2AC1BC;
        border-radius:9999px;
        animation:trainPulse 1.6s infinite ease-out;
      "></div>
      <div style="
        position:relative;
        width:34px;height:34px;
        background:white;
        border:2px solid #2AC1BC;
        border-radius:9999px;
        display:flex;align-items:center;justify-content:center;
        font-size:18px;
        box-shadow:0 2px 6px rgba(0,0,0,0.28);
      ">🚄</div>
    </div>`,
  className: '',
  iconSize: [40, 40],
  iconAnchor: [20, 20],
});

function MapBoundsFit({ stations }: { stations: Station[] }) {
  const map = useMap();
  // stations 배열 reference 가 매 렌더마다 새로 생성돼도 실제 역 구성이 같으면
  // fitBounds 재호출 안 하게 — id hash 를 deps 로 사용
  const stationsKey = stations.map((s) => s.id).sort().join(',');
  const stationsRef = useRef(stations);
  stationsRef.current = stations;

  useEffect(() => {
    const current = stationsRef.current;
    if (!current.length) return;
    const bounds = current.map(
      (s) => [s.latitude, s.longitude] as [number, number]
    );
    // animate:false 로 즉시 적용해서 getZoom() 이 정확한 fit zoom 을 반환하게 함
    map.fitBounds(bounds, { padding: [40, 40], animate: false });
    // 초기 fit 줌에서 한 단계만 더 줌아웃 가능 (작은 화면 대응 여유)
    map.setMinZoom(Math.max(1, map.getZoom() - 1));
  }, [stationsKey, map]);
  return null;
}

/**
 * focus 모드 인터랙션 관리:
 * - following=true 면 trainPos 변경 시 자동 panTo
 * - 사용자가 지도 드래그하면 자동으로 following=false (사용자 의도 존중)
 * - recenterRef 를 통해 외부 버튼에서 panTo 호출 가능
 */
function FocusFollow({
  trainPos,
  following,
  onUserDrag,
  recenterRef,
}: {
  trainPos: [number, number] | null;
  following: boolean;
  onUserDrag: () => void;
  recenterRef: React.MutableRefObject<(() => void) | null>;
}) {
  const map = useMap();

  // following 상태에서 기차가 화면 밖으로 나가면 그때만 panTo
  // (매 초 progress 변화로 trigger 되지만, 화면 안이면 아무것도 안 함 → 사용자 줌/탐색 방해 X)
  useEffect(() => {
    if (!following || !trainPos) return;
    const bounds = map.getBounds();
    if (!bounds.contains(trainPos)) {
      map.panTo(trainPos, { animate: true, duration: 0.4 });
    }
  }, [trainPos, following, map]);

  // 사용자 드래그 시 follow 해제
  useEffect(() => {
    const handler = () => onUserDrag();
    map.on('dragstart', handler);
    return () => {
      map.off('dragstart', handler);
    };
  }, [map, onUserDrag]);

  // 외부에서 호출 가능한 recenter 함수 등록
  useEffect(() => {
    recenterRef.current = () => {
      if (trainPos) map.flyTo(trainPos, map.getZoom(), { duration: 0.5 });
    };
  }, [map, trainPos, recenterRef]);

  return null;
}

export function KoreaMapInner({
  stations,
  departureId,
  arrivalId,
  onStationClick,
  progress = 0,
  active = false,
  markerVariant = 'circle',
  maxZoom = 16,
  liveRunners = [],
}: Props) {
  const isDark = useDarkMode();
  const [tileType, setTileType] = useState<TileType>('map');
  const tile = isDark ? TILES[tileType].dark : TILES[tileType].light;

  // focus 모드 follow 상태
  const [following, setFollowing] = useState(true);
  const recenterRef = useRef<(() => void) | null>(null);
  const handleUserDrag = useCallback(() => setFollowing(false), []);

  const departure = stations.find((s) => String(s.id) === departureId);
  const arrival = stations.find((s) => String(s.id) === arrivalId);

  const line: [number, number][] =
    departure && arrival
      ? [
          [departure.latitude, departure.longitude],
          [arrival.latitude, arrival.longitude],
        ]
      : [];

  const trainPos: [number, number] | null =
    active && departure && arrival
      ? [
          departure.latitude +
            (arrival.latitude - departure.latitude) * progress,
          departure.longitude +
            (arrival.longitude - departure.longitude) * progress,
        ]
      : null;

  return (
    <div className="relative h-full w-full">
      <MapContainer
        bounds={KOREA_BOUNDS}
        maxBounds={KOREA_BOUNDS}
        maxBoundsViscosity={0.8}
        minZoom={6}
        maxZoom={maxZoom}
        scrollWheelZoom
        style={{ width: '100%', height: '100%', minHeight: 400 }}
        className="z-0"
      >
        <TileLayer
          // tileType 또는 다크모드 변경 시 새 레이어로 강제 재마운트
          key={`${tileType}-${isDark ? 'dark' : 'light'}`}
          url={tile.url}
          attribution={tile.attribution}
          subdomains={tileType === 'map' ? ['a', 'b', 'c', 'd'] : []}
        />
        <MapBoundsFit stations={stations} />

        {active && (
          <FocusFollow
            trainPos={trainPos}
            following={following}
            onUserDrag={handleUserDrag}
            recenterRef={recenterRef}
          />
        )}

        {stations.map((s) => {
          const isSelected =
            String(s.id) === departureId || String(s.id) === arrivalId;
          return (
            <Marker
              key={s.id}
              position={[s.latitude, s.longitude]}
              icon={makeStationIcon({
                name: s.name,
                selected: isSelected,
                variant: markerVariant,
              })}
              eventHandlers={{
                click: () => !active && onStationClick(String(s.id)),
              }}
            />
          );
        })}

        {line.length === 2 && (
          <Polyline
            positions={line}
            pathOptions={{
              color: '#2AC1BC',
              weight: 4,
              opacity: 0.85,
              dashArray: active ? undefined : '8 6',
            }}
          />
        )}

        {trainPos && <Marker position={trainPos} icon={TRAIN_DOT_ICON} />}

        {liveRunners.map((r) => (
          <Polyline
            key={`live-line-${r.userId}`}
            positions={[
              [r.departure.lat, r.departure.lng],
              [r.arrival.lat, r.arrival.lng],
            ]}
            pathOptions={{
              color: r.color,
              weight: 2,
              opacity: r.isRunning ? 0.55 : 0.3,
              dashArray: r.isRunning ? '4 4' : '6 6',
            }}
          />
        ))}

        {liveRunners.map((r) => {
          const lat =
            r.departure.lat + (r.arrival.lat - r.departure.lat) * r.progress;
          const lng =
            r.departure.lng + (r.arrival.lng - r.departure.lng) * r.progress;
          return (
            <Marker
              key={`live-runner-${r.userId}`}
              position={[lat, lng]}
              icon={makeRunnerIcon({
                nickname: r.nickname,
                color: r.color,
                isRunning: r.isRunning,
              })}
              interactive={false}
            />
          );
        })}
      </MapContainer>

      {/* "현재 위치로" 버튼 — focus 모드에서 follow 꺼져 있을 때만 표시 */}
      {active && trainPos && !following && (
        <button
          onClick={() => {
            setFollowing(true);
            recenterRef.current?.();
          }}
          className="absolute bottom-16 right-4 z-[1000] flex items-center gap-1.5 rounded-full border border-gray-200 bg-white/95 px-4 py-2 text-xs font-bold text-gray-700 shadow-md backdrop-blur transition hover:scale-105 dark:border-gray-700 dark:bg-gray-800/95 dark:text-gray-200"
          title="기차 현재 위치로 이동"
        >
          🎯 현재 위치
        </button>
      )}

      {/* 타일 토글 (지도 / 위성) — 우하단 floating */}
      <div className="absolute bottom-4 right-4 z-[1000] flex overflow-hidden rounded-full border border-gray-200 bg-white/95 shadow-md backdrop-blur dark:border-gray-700 dark:bg-gray-800/95">
        <button
          onClick={() => setTileType('map')}
          className={`px-3 py-1.5 text-xs font-bold transition ${
            tileType === 'map'
              ? 'bg-[#2AC1BC] text-white'
              : 'text-gray-500 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-700'
          }`}
          title="지도"
        >
          🗺 지도
        </button>
        <button
          onClick={() => setTileType('satellite')}
          className={`px-3 py-1.5 text-xs font-bold transition ${
            tileType === 'satellite'
              ? 'bg-[#2AC1BC] text-white'
              : 'text-gray-500 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-700'
          }`}
          title="위성"
        >
          🛰 위성
        </button>
      </div>
    </div>
  );
}
