export type ReadinessCardDto = {
    date: string;
    readinessScore: number | null;
    cardioLoadScore: number | null;
    cardioLoadTargetMin: number | null;
    cardioLoadTargetMax: number | null;
    readinessStatus: string | null;
    cardioLoadStatus: string | null;
    vo2Max: string | null;
};

export async function fetchReadiness(baseDate: string): Promise<ReadinessCardDto> {
    const url = `/api/readiness?baseDate=${encodeURIComponent(baseDate)}`;
    const res = await fetch(url);

    if (!res.ok) {
        const text = await res.text().catch(() => "");
        throw new Error(`Failed to load readiness: ${res.status} ${text}`);
    }

    return res.json();
}
