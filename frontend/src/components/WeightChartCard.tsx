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
import { fetchWeight, type WeightSeriesDto } from "../api/weightApi";
import type { StepsRange } from "../api/stepsApi";

const options: Array<{ value: StepsRange; label: string }> = [
    { value: "LAST_7_DAYS", label: "Last 7 days" },
    { value: "LAST_14_DAYS", label: "Last 14 days" },
    { value: "CURRENT_WEEK", label: "Current week" },
    { value: "LAST_30_DAYS", label: "Last 30 days" },
];

type Props = {
    baseDate: string;
    range: StepsRange;
    onRangeChange: (next: StepsRange) => void;
    initialData?: WeightSeriesDto;
};

export function WeightChartCard({ baseDate, range, onRangeChange, initialData }: Props) {
    const [data, setData] = useState<WeightSeriesDto | null>(initialData || null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(!initialData);

    useEffect(() => {
        if (initialData) {
            setData(initialData);
            setLoading(false);
            return;
        }
        const controller = new AbortController();
        setLoading(true);
        setError(null);

        (async () => {
            try {
                const d = await fetchWeight(range, baseDate, controller.signal);
                setData(d);
            } catch (e) {
                if (e instanceof Error && e.name === "AbortError") return;
                setError(e instanceof Error ? e.message : "Unknown error");
            } finally {
                setLoading(false);
            }
        })();

        return () => {
            controller.abort();
        };
    }, [range, baseDate]);

    const chartData = useMemo(() => {
        return (data?.points ?? []).map((p) => ({
            date: p.date.slice(5),
            weight: p.weight,
        }));
    }, [data]);

    return (
        <div className="h-full flex flex-col rounded-2xl border border-slate-800 bg-slate-900/40 p-6 shadow-sm backdrop-blur">
            <div className="flex items-start justify-between gap-4">
                <div className="min-w-0">
                    <div className="text-xs text-slate-400">Weight</div>
                    <div className="mt-1 text-xl font-semibold text-slate-100">Weight Log</div>
                    {data && (
                        <div className="mt-2 text-sm text-slate-300">
                            {data.startDate} → {data.endDate}
                        </div>
                    )}
                </div>

                <select
                    value={range}
                    onChange={(e) => onRangeChange(e.target.value as StepsRange)}
                    className="rounded-xl border border-slate-800 bg-slate-950/40 px-4 py-2.5 text-sm text-slate-100 outline-none"
                >
                    {options.map((o) => (
                        <option key={o.value} value={o.value}>
                            {o.label}
                        </option>
                    ))}
                </select>
            </div>

            <div className="mt-5 flex-1 min-h-0">
                {loading && <div className="text-sm text-slate-300">Loading…</div>}

                {!loading && error && (
                    <div className="rounded-xl border border-red-900/50 bg-red-950/30 p-3 text-sm text-red-200">
                        {error}
                    </div>
                )}

                {!loading && !error && (
                    <div className="w-full h-full">
                        <ResponsiveContainer width="100%" height="100%">
                            <LineChart data={chartData} margin={{ top: 10, right: 16, left: 0, bottom: 0 }}>
                                <CartesianGrid strokeDasharray="3 3" />
                                <XAxis dataKey="date" tick={{ fontSize: 12 }} />
                                <YAxis domain={['auto', 'auto']} tick={{ fontSize: 12 }} />
                                <Tooltip />
                                <Line type="monotone" dataKey="weight" stroke="#10b981" strokeWidth={2} dot={true} />
                            </LineChart>
                        </ResponsiveContainer>
                    </div>
                )}
            </div>
        </div>
    );
}
