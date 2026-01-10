import { useEffect, useMemo, useState } from "react";
import {
    CartesianGrid,
    Line,
    LineChart,
    ResponsiveContainer,
    Tooltip,
    XAxis,
    YAxis,
} from "recharts";
import { fetchSteps, type StepsRange, type StepsSeriesDto } from "../api/stepsApi";

const options: Array<{ value: StepsRange; label: string }> = [
    { value: "LAST_7_DAYS", label: "Last 7 days" },
    { value: "LAST_14_DAYS", label: "Last 14 days" },
    { value: "CURRENT_WEEK", label: "Current week" },
    { value: "LAST_30_DAYS", label: "Last 30 days" },
];

export function StepsChartCard() {
    const [range, setRange] = useState<StepsRange>("LAST_7_DAYS");
    const [data, setData] = useState<StepsSeriesDto | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let alive = true;
        setLoading(true);
        setError(null);

        (async () => {
            try {
                const d = await fetchSteps(range);
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
    }, [range]);

    const chartData = useMemo(() => {
        return (data?.points ?? []).map((p) => ({
            date: p.date.slice(5), // MM-DD
            steps: p.steps,
        }));
    }, [data]);

    return (
        <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4 shadow-sm backdrop-blur">
            <div className="flex items-center justify-between gap-3">
                <div>
                    <div className="text-xs text-slate-400">Steps</div>
                    <div className="text-base font-semibold text-slate-100">Daily steps</div>
                    {data && (
                        <div className="mt-1 text-xs text-slate-400">
                            {data.startDate} → {data.endDate}
                        </div>
                    )}
                </div>

                <select
                    value={range}
                    onChange={(e) => setRange(e.target.value as StepsRange)}
                    className="rounded-xl border border-slate-800 bg-slate-950/40 px-3 py-2 text-sm text-slate-100 outline-none"
                >
                    {options.map((o) => (
                        <option key={o.value} value={o.value}>
                            {o.label}
                        </option>
                    ))}
                </select>
            </div>

            <div className="mt-3 h-56">
                {loading && <div className="text-sm text-slate-300">Loading…</div>}

                {!loading && error && (
                    <div className="rounded-xl border border-red-900/50 bg-red-950/30 p-3 text-sm text-red-200">
                        {error}
                    </div>
                )}

                {!loading && !error && (
                    <ResponsiveContainer width="100%" height="100%">
                        <LineChart data={chartData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis dataKey="date" tick={{ fontSize: 12 }} />
                            <YAxis tick={{ fontSize: 12 }} />
                            <Tooltip />
                            <Line type="monotone" dataKey="steps" strokeWidth={2} dot={false} />
                        </LineChart>
                    </ResponsiveContainer>
                )}
            </div>
        </div>
    );
}
