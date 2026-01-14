import { useEffect, useMemo, useState } from "react";
import {
    Area,
    AreaChart,
    CartesianGrid,
    ResponsiveContainer,
    Tooltip,
    XAxis,
    YAxis,
} from "recharts";
import { fetchSleep, type SleepDto } from "../api/sleepApi";

type Props = {
    baseDate: string;
};

const LEVEL_COLORS: Record<string, string> = {
    awake: "#ef4444", // red-500
    rem: "#a855f7",   // purple-500
    light: "#38bdf8", // sky-400
    deep: "#1e40af",  // blue-800
};

const LEVEL_VALUE: Record<string, number> = {
    deep: 1,
    light: 2,
    rem: 3,
    awake: 4
};

const LEVEL_LABELS: Record<number, string> = {
    1: "DEEP",
    2: "LIGHT",
    3: "REM",
    4: "AWAKE"
};

const LEVEL_MAP: Record<string, string> = {
    wake: "awake",
    awake: "awake",
    rem: "rem",
    light: "light",
    deep: "deep"
};

export function SleepChartCard({ baseDate }: Props) {
    const [data, setData] = useState<SleepDto | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let alive = true;
        setLoading(true);
        setError(null);

        (async () => {
            try {
                const d = await fetchSleep(baseDate);
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

    const chartData = useMemo(() => {
        if (!data || !data.segments || data.segments.length === 0) return [];

        const points: any[] = [];
        
        data.segments.forEach((s) => {
            const levelStr = LEVEL_MAP[s.level] || s.level;
            const val = LEVEL_VALUE[levelStr] || 0;
            const startTime = new Date(s.startTime);
            
            // Başlangıç noktası
            points.push({
                time: startTime.getTime(),
                level: val,
                levelStr: levelStr,
                duration: s.durationSeconds / 60,
                displayTime: startTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
            });

            // Bitiş noktası (basamaklı görünüm için aynı seviyede devam eder)
            const endTime = new Date(startTime.getTime() + s.durationSeconds * 1000);
            points.push({
                time: endTime.getTime(),
                level: val,
                levelStr: levelStr,
                duration: s.durationSeconds / 60,
                displayTime: endTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
            });
        });

        return points;
    }, [data]);

    const formatDuration = (mins: number) => {
        const h = Math.floor(mins / 60);
        const m = mins % 60;
        return `${h}h ${m}m`;
    };

    const sleepTimeRange = useMemo(() => {
        if (!data?.startTime || !data?.endTime) return null;
        const start = new Date(data.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        const end = new Date(data.endTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        return `${start} - ${end}`;
    }, [data]);

    return (
        <div className="h-full flex flex-col rounded-2xl border border-slate-800 bg-slate-900/40 p-6 shadow-sm backdrop-blur">
            <div className="flex items-start justify-between gap-4">
                <div className="flex gap-8 items-end">
                    <div className="min-w-0">
                        <div className="text-xs text-slate-400 uppercase tracking-wider font-medium">Sleep</div>
                        <div className="mt-1 text-2xl font-bold text-slate-100 whitespace-nowrap">
                            {data && data.totalMinutesAsleep > 0 ? formatDuration(data.totalMinutesAsleep) : "No sleep data"}
                        </div>
                        {sleepTimeRange && (
                            <div className="mt-1 text-xs text-slate-400 font-medium">
                                {sleepTimeRange}
                            </div>
                        )}
                    </div>

                    {data && (
                        <div className="flex flex-wrap gap-x-6 gap-y-2 pb-1">
                            {Object.entries(LEVEL_COLORS).reverse().map(([lvl, color]) => (
                                <div key={lvl} className="flex flex-col items-start">
                                    <div className="flex items-center gap-1.5">
                                        <div className="h-1.5 w-1.5 rounded-full" style={{ backgroundColor: color }} />
                                        <span className="text-[9px] uppercase tracking-wider text-slate-500 font-bold">{lvl}</span>
                                    </div>
                                    <div className="text-[11px] font-semibold text-slate-300">
                                        {formatDuration(data.levelsSummary[lvl as keyof typeof data.levelsSummary] || 0)}
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                {data && data.sleepScore !== null && (
                    <div className="flex flex-col items-end">
                        <div className="text-xs text-slate-400 uppercase tracking-wider font-medium">Score</div>
                        <div className="text-3xl font-black text-sky-400">{data.sleepScore}</div>
                    </div>
                )}
            </div>

            <div className="mt-8 flex-1 min-h-0">
                {loading && <div className="flex h-full items-center justify-center text-sm text-slate-400">Loading sleep data…</div>}

                {!loading && error && (
                    <div className="rounded-xl border border-red-900/50 bg-red-950/30 p-4 text-sm text-red-200">
                        {error}
                    </div>
                )}

                {!loading && !error && chartData.length > 0 && (
                    <div className="w-full h-full flex flex-col">
                        <div className="flex-1 min-h-0">
                            <ResponsiveContainer width="100%" height="100%">
                                <AreaChart
                                    data={chartData}
                                    margin={{ top: 10, right: 10, left: -20, bottom: 0 }}
                                >
                                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#1e293b" />
                                    <XAxis 
                                        dataKey="time" 
                                        type="number"
                                        domain={['dataMin', 'dataMax']}
                                        tickFormatter={(unix) => new Date(unix).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                        tick={{ fontSize: 10, fill: '#64748b' }}
                                        axisLine={false}
                                        tickLine={false}
                                    />
                                    <YAxis 
                                        dataKey="level"
                                        domain={[0.5, 4.5]}
                                        ticks={[1, 2, 3, 4]}
                                        tickFormatter={(val) => LEVEL_LABELS[val] || ""}
                                        tick={{ fontSize: 10, fill: '#64748b', fontWeight: 600 }}
                                        axisLine={false}
                                        tickLine={false}
                                    />
                                    <Tooltip
                                        content={({ active, payload }) => {
                                            if (active && payload && payload.length) {
                                                const d = payload[0].payload;
                                                return (
                                                    <div className="rounded-xl border border-slate-700 bg-slate-900/90 p-3 text-xs shadow-2xl backdrop-blur-md">
                                                        <div className="font-bold text-slate-100 uppercase tracking-widest" style={{ color: LEVEL_COLORS[d.levelStr] }}>
                                                            {d.levelStr}
                                                        </div>
                                                        <div className="mt-1 text-slate-300 font-medium">{Math.round(d.duration)} minutes</div>
                                                        <div className="text-slate-500 mt-0.5">{d.displayTime}</div>
                                                    </div>
                                                );
                                            }
                                            return null;
                                        }}
                                    />
                                    {Object.keys(LEVEL_VALUE).map((lvl) => (
                                        <Area
                                            key={lvl}
                                            type="stepAfter"
                                            dataKey={(d: any) => d.levelStr === lvl ? d.level : null}
                                            stroke={LEVEL_COLORS[lvl]}
                                            strokeWidth={6}
                                            fill="none"
                                            connectNulls={false}
                                            isAnimationActive={false}
                                            activeDot={{ r: 5, strokeWidth: 0, fill: LEVEL_COLORS[lvl] }}
                                        />
                                    ))}
                                </AreaChart>
                            </ResponsiveContainer>
                        </div>
                    </div>
                )}
                
                {!loading && !error && chartData.length === 0 && (
                     <div className="h-full flex items-center justify-center text-sm text-slate-500 font-medium">
                        No sleep stages recorded for this date.
                     </div>
                )}
            </div>
        </div>
    );
}
