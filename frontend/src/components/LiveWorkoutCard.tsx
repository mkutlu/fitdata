import { useEffect, useMemo, useRef, useState } from "react";

type LiveSample = {
    ts: number;
    hr: number | null;
    steps: number | null;
    distance_m: number | null;
    calories: number | null;
};

function fmtNumber(n: number | null | undefined, digits = 0) {
    if (n === null || n === undefined || Number.isNaN(n)) return "—";
    return n.toLocaleString(undefined, { maximumFractionDigits: digits });
}

function fmtDistanceMeters(m: number | null | undefined) {
    if (m === null || m === undefined || Number.isNaN(m)) return "—";
    if (m >= 1000) return `${fmtNumber(m / 1000, 2)} km`;
    return `${fmtNumber(m, 0)} m`;
}

export function LiveWorkoutCard() {
    const [live, setLive] = useState<LiveSample | null>(null);
    const [status, setStatus] = useState<"connecting" | "connected" | "error">("connecting");
    const lastEventAtRef = useRef<number>(0);

    const stale = useMemo(() => {
        if (!live) return false;
        const now = Date.now();
        return now - live.ts > 5000; // Stale if no data for 5s
    }, [live]);

    useEffect(() => {
        let es: EventSource | null = null;
        let retryTimer: number | null = null;

        const connect = () => {
            setStatus("connecting");

            // Note: This is sufficient if using Vite proxy or same origin
            es = new EventSource("/api/live/stream");

            es.onopen = () => {
                setStatus("connected");
            };

            es.onmessage = (e) => {
                try {
                    const data: LiveSample = JSON.parse(e.data);
                    setLive(data);
                    lastEventAtRef.current = Date.now();
                } catch {
                    // ignore
                }
            };

            es.onerror = () => {
                setStatus("error");
                try {
                    es?.close();
                } catch {
                    // ignore
                }
                es = null;

                // Simple retry
                if (retryTimer) window.clearTimeout(retryTimer);
                retryTimer = window.setTimeout(connect, 1500);
            };
        };

        connect();

        return () => {
            if (retryTimer) window.clearTimeout(retryTimer);
            try {
                es?.close();
            } catch {
                // ignore
            }
        };
    }, []);

    const statusBadge = (() => {
        if (status === "connected" && !stale) {
            return <span className="rounded-full bg-emerald-500/15 px-2 py-1 text-xs text-emerald-400">LIVE</span>;
        }
        if (status === "connected" && stale) {
            return <span className="rounded-full bg-amber-500/15 px-2 py-1 text-xs text-amber-400">STALE</span>;
        }
        if (status === "connecting") {
            return <span className="rounded-full bg-sky-500/15 px-2 py-1 text-xs text-sky-400">CONNECTING</span>;
        }
        return <span className="rounded-full bg-rose-500/15 px-2 py-1 text-xs text-rose-400">DISCONNECTED</span>;
    })();

    const lastUpdated = live?.ts ? new Date(live.ts).toLocaleTimeString() : "—";

    return (
        <div className="h-full flex flex-col rounded-2xl border border-slate-800 bg-slate-900/40 p-6 shadow-sm backdrop-blur">
            <div className="flex items-center justify-between gap-3">
                <div>
                    <div className="text-sm text-slate-400">Workout</div>
                    <div className="text-xl font-semibold text-slate-100">Live metrics</div>
                </div>
                <div className="flex items-center gap-2">
                    {statusBadge}
                </div>
            </div>

            <div className="mt-6 grid grid-cols-2 gap-4 md:grid-cols-4">
                <div className="rounded-xl border border-slate-800 bg-slate-950/40 p-4">
                    <div className="text-xs text-slate-400 uppercase tracking-wider font-medium">Heart rate</div>
                    <div className="mt-2 text-3xl font-bold text-slate-100">
                        {fmtNumber(live?.hr, 0)} <span className="text-sm font-normal text-slate-400">bpm</span>
                    </div>
                </div>

                <div className="rounded-xl border border-slate-800 bg-slate-950/40 p-4">
                    <div className="text-xs text-slate-400 uppercase tracking-wider font-medium">Steps</div>
                    <div className="mt-2 text-3xl font-bold text-slate-100">{fmtNumber(live?.steps, 0)}</div>
                </div>

                <div className="rounded-xl border border-slate-800 bg-slate-950/40 p-4">
                    <div className="text-xs text-slate-400 uppercase tracking-wider font-medium">Distance</div>
                    <div className="mt-2 text-3xl font-bold text-slate-100">{fmtDistanceMeters(live?.distance_m)}</div>
                </div>

                <div className="rounded-xl border border-slate-800 bg-slate-950/40 p-4">
                    <div className="text-xs text-slate-400 uppercase tracking-wider font-medium">Calories</div>
                    <div className="mt-2 text-3xl font-bold text-slate-100">
                        {fmtNumber(live?.calories, 0)} <span className="text-sm font-normal text-slate-400">kcal</span>
                    </div>
                </div>
            </div>

            <div className="mt-auto pt-4 flex items-center justify-between text-[11px] text-slate-500">
                <div>Last update: {lastUpdated}</div>
                <div className="truncate">
                    Endpoint: <span className="text-slate-400">/api/live/stream</span>
                </div>
            </div>

            {status === "error" && (
                <div className="mt-4 rounded-xl border border-rose-500/30 bg-rose-500/10 p-3 text-sm text-rose-200">
                    Connection lost. Reconnecting…
                </div>
            )}
        </div>
    );
}