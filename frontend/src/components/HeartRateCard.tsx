import { useEffect, useMemo, useState } from "react";
import {
    Bar,
    BarChart,
    CartesianGrid,
    Line,
    LineChart,
    ResponsiveContainer,
    Tooltip,
    XAxis,
    YAxis,
} from "recharts";
import type { StepsRange } from "../api/stepsApi";
import {
    fetchHeartRateDay,
    fetchHeartRateRange,
    type HeartRateDayDto,
    type HeartRateRangeDto,
} from "../api/heartrateApi";

type Props = {
    baseDate: string;
    range: StepsRange;
};

type Mode = "DAY" | "TREND";

export function HeartRateCard({ baseDate, range }: Props) {
    const [mode, setMode] = useState<Mode>("DAY");

    const [day, setDay] = useState<HeartRateDayDto | null>(null);
    const [trend, setTrend] = useState<HeartRateRangeDto | null>(null);

    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let alive = true;
        setLoading(true);
        setError(null);

        (async () => {
            try {
                if (mode === "DAY") {
                    const d = await fetchHeartRateDay(baseDate);
                    if (!alive) return;
                    setDay(d);
                } else {
                    const r = await fetchHeartRateRange(range, baseDate);
                    if (!alive) return;
                    setTrend(r);
                }
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
    }, [mode, baseDate, range]);

    const zoneData = useMemo(() => {
        const z = day?.zones;
        if (!z) return [];
        return [
            {
                name: "Zones",
                out: z.outOfRangeMin,
                fat: z.fatBurnMin,
                cardio: z.cardioMin,
                peak: z.peakMin,
            },
        ];
    }, [day]);

    const trendData = useMemo(() => {
        const pts = trend?.points ?? [];
        return pts.map((p) => ({
            date: p.date.slice(5),
            resting: p.restingHr,
        }));
    }, [trend]);

    const dayTotalMin =
        (day?.zones.outOfRangeMin ?? 0) +
        (day?.zones.fatBurnMin ?? 0) +
        (day?.zones.cardioMin ?? 0) +
        (day?.zones.peakMin ?? 0);

    const hasZoneData = zoneData.length > 0;

    return (
        <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-6 shadow-sm backdrop-blur">
            <div className="flex items-start justify-between gap-4">
                <div className="min-w-0">
                    <div className="text-xs text-slate-400">Heart rate</div>
                    <div className="mt-1 text-xl font-semibold text-slate-100">
                        {mode === "DAY" ? "Daily summary" : "Resting HR trend"}
                    </div>
                    <div className="mt-2 text-sm text-slate-300">
                        {mode === "DAY" ? baseDate : `${range} • anchored to ${baseDate}`}
                    </div>
                </div>

                <div className="flex items-center gap-2">
                    <ModeButton active={mode === "DAY"} onClick={() => setMode("DAY")} label="Day" />
                    <ModeButton active={mode === "TREND"} onClick={() => setMode("TREND")} label="Trend" />
                </div>
            </div>

            {mode === "DAY" && (
                <div className="mt-4 flex flex-wrap gap-2">
                    <StatPill label="Resting HR" value={day?.restingHr == null ? "—" : String(day.restingHr)} />
                    <StatPill label="Zone minutes" value={day ? String(dayTotalMin) : "—"} />
                    <StatPill label="Peak" value={day ? String(day.zones.peakMin) : "—"} />
                </div>
            )}

            {mode === "TREND" && (
                <div className="mt-4 flex flex-wrap gap-2">
                    <StatPill label="From" value={trend?.startDate ?? "—"} />
                    <StatPill label="To" value={trend?.endDate ?? "—"} />
                    <StatPill label="Points" value={trend ? String(trend.points.length) : "—"} />
                </div>
            )}

            <div className="mt-5 h-56 sm:h-64 lg:h-[340px]">
                {loading && <div className="text-sm text-slate-300">Loading…</div>}

                {!loading && error && (
                    <div className="rounded-xl border border-red-900/50 bg-red-950/30 p-3 text-sm text-red-200">
                        {error}
                    </div>
                )}

                {!loading && !error && mode === "DAY" && !hasZoneData && (
                    <div className="rounded-xl border border-slate-800 bg-slate-950/25 p-4 text-sm text-slate-300">
                        No heart rate data available for this date.
                    </div>
                )}

                {!loading && !error && mode === "DAY" && hasZoneData && (
                    <ResponsiveContainer width="100%" height="100%">
                        <BarChart data={zoneData} margin={{ top: 10, right: 16, left: 0, bottom: 0 }}>
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis dataKey="name" hide />
                            <YAxis tick={{ fontSize: 12 }} />
                            <Tooltip
                                formatter={(value, key) => [`${value} min`, zoneLabel(String(key))]}
                            />
                            <Bar dataKey="out" stackId="a" fill="#94a3b8" />
                            <Bar dataKey="fat" stackId="a" fill="#22c55e" />
                            <Bar dataKey="cardio" stackId="a" fill="#f59e0b" />
                            <Bar dataKey="peak" stackId="a" fill="#ef4444" />
                        </BarChart>
                    </ResponsiveContainer>
                )}

                {!loading && !error && mode === "TREND" && (
                    <ResponsiveContainer width="100%" height="100%">
                        <LineChart data={trendData} margin={{ top: 10, right: 16, left: 0, bottom: 0 }}>
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis dataKey="date" tick={{ fontSize: 12 }} />
                            <YAxis tick={{ fontSize: 12 }} />
                            <Tooltip
                                formatter={(value) => [value == null ? "—" : `${value} bpm`, "Resting HR"]}
                            />
                            <Line
                                type="monotone"
                                dataKey="resting"
                                stroke="#a78bfa"
                                strokeWidth={2}
                                dot={false}
                                connectNulls={false}
                            />
                        </LineChart>
                    </ResponsiveContainer>
                )}
            </div>
        </div>
    );
}

function ModeButton({ active, onClick, label }: { active: boolean; onClick: () => void; label: string }) {
    return (
        <button
            type="button"
            onClick={onClick}
            className={[
                "rounded-xl border px-3 py-2 text-sm",
                active
                    ? "border-slate-700 bg-slate-950/50 text-slate-100"
                    : "border-slate-800 bg-slate-950/25 text-slate-300 hover:bg-slate-950/40",
            ].join(" ")}
        >
            {label}
        </button>
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

function zoneLabel(key: string) {
    if (key === "out") return "Out of range";
    if (key === "fat") return "Fat burn";
    if (key === "cardio") return "Cardio";
    if (key === "peak") return "Peak";
    return key;
}