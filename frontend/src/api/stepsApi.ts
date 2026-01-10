export type StepsRange =
    | "LAST_7_DAYS"
    | "LAST_14_DAYS"
    | "CURRENT_WEEK"
    | "LAST_30_DAYS";

export type StepsSeriesDto = {
    range: string;
    startDate: string;
    endDate: string;
    points: Array<{ date: string; steps: number }>;
};

export async function fetchSteps(range: StepsRange, baseDate: string): Promise<StepsSeriesDto> {
    const url = `/api/steps?range=${encodeURIComponent(range)}&baseDate=${encodeURIComponent(baseDate)}`;
    const res = await fetch(url);

    if (!res.ok) {
        const text = await res.text().catch(() => "");
        throw new Error(`Failed to load steps: ${res.status} ${text}`);
    }

    return res.json();
}