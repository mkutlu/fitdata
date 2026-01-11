export type HeartRateIntradayDto = {
    date: string;
    restingHr: number | null;
    minBpm: number;
    maxBpm: number;
    caloriesOut: number | null;
    activityCalories: number | null;
    zones: Array<{ name: string; min: number | null; max: number | null; minutes: number | null }>;
    points: Array<{ time: string; bpm: number }>;
};

export async function fetchHeartRateIntraday(baseDate: string): Promise<HeartRateIntradayDto> {
    const url = `/api/heartrate/intraday?baseDate=${encodeURIComponent(baseDate)}`;
    const res = await fetch(url);

    if (!res.ok) {
        const text = await res.text().catch(() => "");
        throw new Error(`Failed to load heart rate intraday: ${res.status} ${text}`);
    }

    return res.json();
}
