import { apiClient } from './client';

export type RoomUserRole = 'OWNER' | 'MEMBER';

export interface RoomResponse {
  id: number;
  name: string;
  code: string;
  role: RoomUserRole;
}

export interface RoomUserResponse {
  userId: number;
  nickname: string;
  role: RoomUserRole;
}

export async function createRoom(name: string): Promise<RoomResponse> {
  return apiClient<RoomResponse>('/api/rooms', {
    method: 'POST',
    body: JSON.stringify({ name }),
  });
}

export async function getMyRooms(): Promise<RoomResponse[]> {
  return apiClient<RoomResponse[]>('/api/rooms/me');
}

export async function getRoomDetail(roomId: number): Promise<RoomResponse> {
  return apiClient<RoomResponse>(`/api/rooms/${roomId}`);
}

export async function getRoomMembers(
  roomId: number
): Promise<RoomUserResponse[]> {
  return apiClient<RoomUserResponse[]>(`/api/rooms/${roomId}/users`);
}

export async function joinRoom(code: string): Promise<RoomResponse> {
  return apiClient<RoomResponse>('/api/rooms/join', {
    method: 'POST',
    body: JSON.stringify({ code }),
  });
}

export async function leaveRoom(roomId: number): Promise<void> {
  await apiClient<void>(`/api/rooms/${roomId}/users/me`, { method: 'DELETE' });
}

export async function kickMember(
  roomId: number,
  userId: number
): Promise<void> {
  await apiClient<void>(`/api/rooms/${roomId}/users/${userId}`, {
    method: 'DELETE',
  });
}

export async function transferOwner(
  roomId: number,
  targetUserId: number
): Promise<void> {
  await apiClient<void>(`/api/rooms/${roomId}/owner`, {
    method: 'PUT',
    body: JSON.stringify({ targetUserId }),
  });
}

export async function reIssueCode(roomId: number): Promise<RoomResponse> {
  return apiClient<RoomResponse>(`/api/rooms/${roomId}/code/reset`, {
    method: 'POST',
  });
}

export async function renameRoom(
  roomId: number,
  name: string
): Promise<RoomResponse> {
  return apiClient<RoomResponse>(`/api/rooms/${roomId}`, {
    method: 'PATCH',
    body: JSON.stringify({ name }),
  });
}

export async function deleteRoom(roomId: number): Promise<void> {
  await apiClient<void>(`/api/rooms/${roomId}`, { method: 'DELETE' });
}

export interface SessionLiveInfo {
  sessionId: number;
  status: 'RUNNING' | 'PAUSED';
  departure: {
    id: number;
    name: string;
    latitude: number;
    longitude: number;
  };
  arrival: {
    id: number;
    name: string;
    latitude: number;
    longitude: number;
  };
  totalTargetSeconds: number;
  accumulatedSeconds: number;
  startedAt: string;
  plannedEndAt: string;
}

export interface RoomUserLiveResponse {
  userId: number;
  nickname: string;
  role: RoomUserRole;
  session: SessionLiveInfo | null;
}

export async function getRoomLive(
  roomId: number
): Promise<RoomUserLiveResponse[]> {
  return apiClient<RoomUserLiveResponse[]>(`/api/rooms/${roomId}/live`);
}

export interface RoomRankingEntry {
  rank: number;
  userId: number;
  nickname: string;
  runSeconds: number;
}

export interface RoomRankingResponse {
  date: string;
  period: string;
  entries: RoomRankingEntry[];
}

export async function getRoomRanking(
  roomId: number,
  date: string,
  period: string
): Promise<RoomRankingResponse> {
  return apiClient<RoomRankingResponse>(
    `/api/rooms/${roomId}/ranking?date=${date}&period=${period}`
  );
}
