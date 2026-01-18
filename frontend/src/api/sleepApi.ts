import { fetchWithRetry } from "./fetchUtils";

export type SleepLevelSegment = {
    startTime: string;
    level: "awake" | "rem" | "light" | "deep" | string;
    durationSeconds: number;
};

export type SleepDto = {
    date: string;
    totalMinutesAsleep: number;
    totalTimeInBed: number;
    sleepScore: number | null;
    startTime: string;
    endTime: string;
    levelsSummary: {
        deep: number;
        light: number;
        rem: number;
        awake: number;
    };
    segments: SleepLevelSegment[];
};

export async function fetchSleep(date: string, signal?: AbortSignal): Promise<SleepDto> {
    const url = `/api/sleep?date=${encodeURIComponent(date)}`;
    const res = await fetchWithRetry(url, { signal });

    if (!res.ok) {
        const text = await res.text().catch(() => "");
        throw new Error(`Failed to load sleep: ${res.status} ${text}`);
    }

    return res.json();
}
