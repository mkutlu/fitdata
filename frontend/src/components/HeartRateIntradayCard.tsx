import { useEffect, useMemo, useState } from "react";
import {
    Bar,
    BarChart,
    CartesianGrid,
    Cell,
    Line,
    LineChart,
    ReferenceArea,
    ReferenceLine,
    ResponsiveContainer,
    Tooltip,
    XAxis,
    YAxis,
} from "recharts";
import { fetchHeartRateIntraday, type HeartRateIntradayDto } from "../api/heartrateIntradayApi";

type Props = {
    baseDate: string;
};

export function HeartRateIntradayCard({ baseDate }: Props) {
    const [data, setData] = useState<HeartRateIntradayDto | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let alive = true;
        setLoading(true);
        setError(null);

        (async () => {
            try {
                const d = await fetchHeartRateIntraday(baseDate);
                if (!alive) return;
                setData(d);
            } catch (e) {
                if (!alive) return;
                setError(e instanceof Error ? e.message : "Unknown error");
            } finally {
                setLoading(false);
            }
        })();

        return () => {
            alive = false;
        };
    }, [baseDate]);

    const hasPoints = (data?.points?.length ?? 0) > 0;

    const zones = useMemo(() => {
        const z = data?.zones ?? [];
        const mapped = z
            .map((x) => ({
                name: x.name,
                min: x.min ?? null,
                max: x.max ?? null,
                minutes: x.minutes ?? 0,
            }))
            .filter((x) => x.min != null && x.max != null);

        mapped.sort((a, b) => (a.min ?? 0) - (b.min ?? 0));
        return mapped;
    }, [data]);

    const zoneBarData = useMemo(() => {
        const z = data?.zones ?? [];
        return z.map((x) => ({
            name: x.name,
            minutes: x.minutes ?? 0,
            fill: zoneFill(x.name),
        }));
    }, [data]);

    const totalZoneMinutes = useMemo(() => {
        return (data?.zones ?? []).reduce((acc, z) => acc + (z.minutes ?? 0), 0);
    }, [data]);

    const chartData = useMemo(() => {
        return (data?.points ?? []).map((p) => ({
            t: p.time.slice(0, 5),
            bpm: p.bpm,
        }));
    }, [data]);

    const headline = useMemo(() => {
        if (!data) return "—";
        if (hasPoints) return `${data.minBpm}–${data.maxBpm} bpm`;
        const min = zones[0]?.min;
        const max = zones[zones.length - 1]?.max;
        if (min != null && max != null) return `${min}–${max} bpm`;
        return "—";
    }, [data, hasPoints, zones]);

    const subline = useMemo(() => {
        if (!data) return "";
        if (hasPoints) return `High of ${data.maxBpm} bpm`;
        return "No intraday points available for this date.";
    }, [data, hasPoints]);

    const yDomain = useMemo(() => {
        if (hasPoints) return ["dataMin - 5", "dataMax + 5"] as any;
        const min = zones[0]?.min;
        const max = zones[zones.length - 1]?.max;
        if (min != null && max != null) return [min - 5, max + 5];
        return [40, 200];
    }, [hasPoints, zones]);

    return (
        <div className="h-full flex flex-col rounded-2xl border border-slate-800 bg-slate-900/40 p-6 shadow-sm backdrop-blur overflow-auto">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                <div className="min-w-0">
                    <div className="text-xs text-slate-400">Heart</div>
                    <div className="mt-1 text-3xl font-semibold text-slate-100">{headline}</div>
                    <div className="mt-1 text-sm text-slate-300">{subline}</div>
                    <div className="mt-2 text-xs text-slate-400">{baseDate}</div>
                </div>

                <div className="flex flex-wrap gap-2">
                    <StatPill label="Resting HR" value={data?.restingHr == null ? "—" : `${data.restingHr} bpm`} />
                    <StatPill label="Total time" value={data ? `${totalZoneMinutes} min` : "—"} />
                    <StatPill label="Energy burned" value={data?.caloriesOut == null ? "—" : `${data.caloriesOut} cal`} />
                </div>
            </div>

            <div className="mt-5 h-64 sm:h-72 lg:h-[380px] flex-shrink-0">
                {loading && <div className="text-sm text-slate-300">Loading…</div>}

                {!loading && error && (
                    <div className="rounded-xl border border-red-900/50 bg-red-950/30 p-3 text-sm text-red-200">
                        {error}
                    </div>
                )}

                {!loading && !error && !hasPoints && (
                    <div className="rounded-xl border border-slate-800 bg-slate-950/25 p-4 text-sm text-slate-300">
                        No intraday heart rate points available for this date.
                    </div>
                )}

                {!loading && !error && hasPoints && data && (
                    <div className="w-full h-full">
                        <ResponsiveContainer width="100%" height="100%">
                            <LineChart data={chartData} margin={{ top: 10, right: 16, left: 0, bottom: 0 }}>
                                <CartesianGrid strokeDasharray="3 3" />
                                <XAxis dataKey="t" tick={{ fontSize: 12 }} minTickGap={24} />
                                <YAxis tick={{ fontSize: 12 }} domain={yDomain} />
                                <Tooltip formatter={(v) => [`${v} bpm`, "HR"]} />

                                {zones.map((z, idx) => (
                                    <ReferenceArea
                                        key={`${z.name}-${idx}`}
                                        y1={z.min as number}
                                        y2={z.max as number}
                                        fillOpacity={0.10}
                                        fill={zoneFill(z.name)}
                                    />
                                ))}

                                <ReferenceLine y={data.minBpm} strokeDasharray="4 4" />
                                <ReferenceLine y={data.maxBpm} strokeDasharray="4 4" />

                                <Line type="monotone" dataKey="bpm" stroke="#38bdf8" strokeWidth={2} dot={false} />
                            </LineChart>
                        </ResponsiveContainer>
                    </div>
                )}
            </div>

            <div className="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-2">
                <div className="rounded-2xl border border-slate-800 bg-slate-950/20 p-5">
                    <div className="text-sm font-semibold text-slate-100">Heart rate zones</div>
                    <div className="mt-2 text-sm text-slate-300">{data ? `${totalZoneMinutes} min total` : "—"}</div>

                    <div className="mt-4 h-44">
                        <div className="w-full h-full">
                            <ResponsiveContainer width="100%" height="100%">
                                <BarChart data={zoneBarData} margin={{ top: 10, right: 16, left: 0, bottom: 0 }}>
                                    <CartesianGrid strokeDasharray="3 3" />
                                    <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                                    <YAxis tick={{ fontSize: 12 }} />
                                    <Tooltip formatter={(v) => [`${v} min`, "Minutes"]} />
                                    <Bar dataKey="minutes" radius={[10, 10, 10, 10]}>
                                        {zoneBarData.map((entry, idx) => (
                                            <Cell key={`cell-${idx}`} fill={entry.fill} />
                                        ))}
                                    </Bar>
                                </BarChart>
                            </ResponsiveContainer>
                        </div>
                    </div>
                </div>

                <div className="rounded-2xl border border-slate-800 bg-slate-950/20 p-5">
                    <div className="text-sm font-semibold text-slate-100">Energy</div>
                    <div className="mt-3 space-y-2 text-sm text-slate-300">
                        <div className="flex items-center justify-between">
                            <span>Calories out</span>
                            <span className="font-semibold text-slate-100">{data?.caloriesOut ?? "—"}</span>
                        </div>
                        <div className="flex items-center justify-between">
                            <span>Activity calories</span>
                            <span className="font-semibold text-slate-100">{data?.activityCalories ?? "—"}</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

function StatPill({ label, value }: { label: string; value: string }) {
    return (
        <div className="inline-flex items-center gap-2 rounded-full border border-slate-800 bg-slate-950/25 px-3 py-1.5">
            <span className="text-[11px] text-slate-400">{label}</span>
            <span className="text-xs font-semibold text-slate-100">{value}</span>
        </div>
    );
}

function zoneFill(name: string) {
    const n = name.toLowerCase();
    if (n.includes("peak")) return "#ef4444";
    if (n.includes("cardio") || n.includes("vigorous")) return "#f59e0b";
    if (n.includes("fat") || n.includes("moderate")) return "#22c55e";
    if (n.includes("out") || n.includes("light")) return "#60a5fa";
    return "#94a3b8";
}
