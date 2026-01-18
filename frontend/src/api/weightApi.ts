import { fetchWithRetry } from "./fetchUtils";
import type { StepsRange } from "./stepsApi";

export type WeightSeriesDto = {
    range: string;
    startDate: string;
    endDate: string;
    points: Array<{ date: string; weight: number }>;
};

export async function fetchWeight(range: StepsRange, baseDate: string, signal?: AbortSignal): Promise<WeightSeriesDto> {
    const url = `/api/weight/range?range=${encodeURIComponent(range)}&baseDate=${encodeURIComponent(baseDate)}`;
    const res = await fetchWithRetry(url, { signal });

    if (!res.ok) {
        const text = await res.text().catch(() => "");
        throw new Error(`Failed to load weight: ${res.status} ${text}`);
    }

    return res.json();
}
