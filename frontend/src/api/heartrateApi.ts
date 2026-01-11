import type { StepsRange } from "./stepsApi";

export type HeartRateDayDto = {
    date: string;
    restingHr: number | null;
    zones: {
        outOfRangeMin: number;
        fatBurnMin: number;
        cardioMin: number;
        peakMin: number;
    };
};

export type HeartRateRangeDto = {
    range: string;
    startDate: string;
    endDate: string;
    points: Array<{
        date: string;
        restingHr: number | null;
        zones: HeartRateDayDto["zones"];
    }>;
};

export async function fetchHeartRateDay(baseDate: string): Promise<HeartRateDayDto> {
    const url = `/api/heartrate?baseDate=${encodeURIComponent(baseDate)}`;
    const res = await fetch(url);

    if (!res.ok) {
        const text = await res.text().catch(() => "");
        throw new Error(`Failed to load heart rate (day): ${res.status} ${text}`);
    }

    return res.json();
}

export async function fetchHeartRateRange(range: StepsRange, baseDate: string): Promise<HeartRateRangeDto> {
    const url = `/api/heartrate/range?range=${encodeURIComponent(range)}&baseDate=${encodeURIComponent(baseDate)}`;
    const res = await fetch(url);

    if (!res.ok) {
        const text = await res.text().catch(() => "");
        throw new Error(`Failed to load heart rate (range): ${res.status} ${text}`);
    }

    return res.json();
}
