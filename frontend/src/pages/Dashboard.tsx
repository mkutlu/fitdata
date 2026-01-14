import { useEffect, useMemo, useState } from "react";
import { ResponsiveGridLayout, useContainerWidth } from "react-grid-layout";
import type { Layout } from "react-grid-layout";
import { StepsChartCard } from "../components/StepsChartCard";
import { WeightChartCard } from "../components/WeightChartCard";
import { HeartRateIntradayCard } from "../components/HeartRateIntradayCard";
import { SleepChartCard } from "../components/SleepChartCard";
import { fetchProfile, type UserProfileDto } from "../api/profileApi";
import { fetchAuthStatus, logout } from "../api/authApi";
import type { StepsRange } from "../api/stepsApi";
import {LiveWorkoutCard} from "../components/LiveWorkoutCard.tsx";
import logo from "../assets/fitdata-logo.png";

const LAYOUT_STORAGE_KEY = "fitdata-dashboard-layout";

const defaultLayouts: { [P: string]: Layout[] } = {
    lg: [
        { i: "steps", x: 0, y: 0, w: 6, h: 12 },
        { i: "weight", x: 6, y: 0, w: 6, h: 12 },
        { i: "heart", x: 0, y: 12, w: 12, h: 12 },
        { i: "sleep", x: 0, y: 24, w: 12, h: 12 },
        { i: "live", x: 0, y: 36, w: 4, h: 5 },
    ],
    md: [
        { i: "steps", x: 0, y: 0, w: 6, h: 12 },
        { i: "weight", x: 6, y: 0, w: 6, h: 12 },
        { i: "heart", x: 0, y: 12, w: 12, h: 12 },
        { i: "sleep", x: 0, y: 24, w: 12, h: 12 },
        { i: "live", x: 0, y: 36, w: 6, h: 5 },
    ],
    sm: [
        { i: "steps", x: 0, y: 0, w: 6, h: 10 },
        { i: "weight", x: 0, y: 10, w: 6, h: 10 },
        { i: "heart", x: 0, y: 20, w: 6, h: 10 },
        { i: "sleep", x: 0, y: 30, w: 6, h: 10 },
        { i: "live", x: 0, y: 40, w: 6, h: 5 },
    ],
};

function DashboardContent({selectedDate, range, setRange, weightRange, setWeightRange, layouts, onLayoutChange }: any) {
    const { containerRef, width } = useContainerWidth();
    const [mounted, setMounted] = useState(false);

    useEffect(() => {
        setMounted(true);
    }, []);

    return (
        <div ref={containerRef} className="mt-6">
            {width > 0 && mounted && (
                <ResponsiveGridLayout
                    className="layout"
                    layouts={layouts}
                    width={width}
                    breakpoints={{ lg: 1200, md: 996, sm: 768, xs: 480, xxs: 0 }}
                    cols={{ lg: 12, md: 12, sm: 6, xs: 4, xxs: 2 }}
                    rowHeight={30}
                    draggableHandle=".drag-handle"
                    onLayoutChange={onLayoutChange}
                >
                    <div key="steps">
                        <div className="h-full w-full flex flex-col">
                            <div className="drag-handle h-6 w-full cursor-move bg-slate-800/20 hover:bg-slate-800/40 rounded-t-2xl flex items-center justify-center">
                                <div className="w-8 h-1 bg-slate-700 rounded-full" />
                            </div>
                            <div className="flex-1 overflow-hidden">
                                <StepsChartCard baseDate={selectedDate} range={range} onRangeChange={setRange} />
                            </div>
                        </div>
                    </div>
                    <div key="weight">
                        <div className="h-full w-full flex flex-col">
                            <div className="drag-handle h-6 w-full cursor-move bg-slate-800/20 hover:bg-slate-800/40 rounded-t-2xl flex items-center justify-center">
                                <div className="w-8 h-1 bg-slate-700 rounded-full" />
                            </div>
                            <div className="flex-1 overflow-hidden">
                                <WeightChartCard baseDate={selectedDate} range={weightRange} onRangeChange={setWeightRange} />
                            </div>
                        </div>
                    </div>
                    <div key="heart">
                        <div className="h-full w-full flex flex-col">
                            <div className="drag-handle h-6 w-full cursor-move bg-slate-800/20 hover:bg-slate-800/40 rounded-t-2xl flex items-center justify-center">
                                <div className="w-8 h-1 bg-slate-700 rounded-full" />
                            </div>
                            <div className="flex-1 overflow-hidden">
                                <HeartRateIntradayCard baseDate={selectedDate} />
                            </div>
                        </div>
                    </div>
                    <div key="sleep">
                        <div className="h-full w-full flex flex-col">
                            <div className="drag-handle h-6 w-full cursor-move bg-slate-800/20 hover:bg-slate-800/40 rounded-t-2xl flex items-center justify-center">
                                <div className="w-8 h-1 bg-slate-700 rounded-full" />
                            </div>
                            <div className="flex-1 overflow-hidden">
                                <SleepChartCard baseDate={selectedDate} />
                            </div>
                        </div>
                    </div>
                    <div key="live">
                        <div className="h-full w-full flex flex-col">
                            <div className="drag-handle h-6 w-full cursor-move bg-slate-800/20 hover:bg-slate-800/40 rounded-t-2xl flex items-center justify-center">
                                <div className="w-8 h-1 bg-slate-700 rounded-full" />
                            </div>
                            <div className="flex-1 overflow-hidden">
                                <LiveWorkoutCard />
                            </div>
                        </div>
                    </div>
                </ResponsiveGridLayout>
            )}
        </div>
    );
}

export function Dashboard() {
    const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
    const [profile, setProfile] = useState<UserProfileDto | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);

    const [selectedDate, setSelectedDate] = useState<string>(() => toIsoDate(new Date()));
    const readableDate = useMemo(() => formatReadable(selectedDate), [selectedDate]);

    const [range, setRange] = useState<StepsRange>("LAST_7_DAYS");
    const [weightRange, setWeightRange] = useState<StepsRange>("LAST_7_DAYS");

    const [layouts, setLayouts] = useState(() => {
        const saved = localStorage.getItem(LAYOUT_STORAGE_KEY);
        try {
            if (saved) {
                const parsed = JSON.parse(saved);
                // Validation: Ensure it has the necessary breakpoints and they are not empty
                if ((parsed.lg && parsed.lg.length > 0) || 
                    (parsed.md && parsed.md.length > 0) || 
                    (parsed.sm && parsed.sm.length > 0)) {
                    return parsed;
                }
            }
        } catch (e) {
            console.error("Failed to parse saved layout", e);
        }
        return JSON.parse(JSON.stringify(defaultLayouts));
    });

    const onLayoutChange = (currentLayout: Layout[], allLayouts: { [key: string]: Layout[] }) => {
        // Only save if we have all the expected items to avoid saving partial or "minimum size" defaults
        const expectedItems = defaultLayouts.lg.length;
        if (currentLayout.length >= expectedItems) {
            setLayouts(allLayouts);
            localStorage.setItem(LAYOUT_STORAGE_KEY, JSON.stringify(allLayouts));
        }
    };

    useEffect(() => {
        let alive = true;

        (async () => {
            try {
                const auth = await fetchAuthStatus();
                if (!alive) return;
                setIsAuthenticated(auth.authenticated);

                if (auth.authenticated) {
                    const data = await fetchProfile();
                    if (!alive) return;
                    setProfile(data);
                } else {
                    // Reset layouts when not authenticated to ensure clean state on next login
                    setLayouts(JSON.parse(JSON.stringify(defaultLayouts)));
                }
            } catch (e) {
                if (!alive) return;
                console.error("Auth/Profile fetch failed", e);
                // Don't show technical error if it's just a missing token
                if (!e.message.includes("401") && !e.message.includes("No Fitbit token")) {
                    setError(e instanceof Error ? e.message : "Unknown error");
                }
            } finally {
                setLoading(false);
            }
        })();

        return () => {
            alive = false;
        };
    }, []);

    const handleLogin = () => {
        window.location.href = "/oauth/fitbit/start";
    };

    const handleLogout = async () => {
        try {
            await logout();
            setIsAuthenticated(false);
            setProfile(null);
            // Reset layouts to default on logout
            setLayouts(JSON.parse(JSON.stringify(defaultLayouts)));
        } catch (e) {
            console.error("Logout failed", e);
        }
    };

    return (
        <main className="min-h-screen text-slate-100 bg-slate-950">
            <div className="pointer-events-none fixed inset-0 bg-[radial-gradient(1400px_circle_at_15%_0%,rgba(56,189,248,0.14),transparent_58%),radial-gradient(1100px_circle_at_85%_20%,rgba(99,102,241,0.12),transparent_58%)]" />
            <div className="pointer-events-none fixed inset-0 opacity-[0.07] [background-image:linear-gradient(to_right,rgba(148,163,184,0.18)_1px,transparent_1px),linear-gradient(to_bottom,rgba(148,163,184,0.18)_1px,transparent_1px)] [background-size:72px_72px]" />

            <div className="relative mx-auto w-full max-w-[1520px] px-4 sm:px-6 lg:px-10 py-10">
                <header className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
                    <div>
                        <img src={logo} alt="Fitdata" className="h-10 w-auto" />
                    </div>

                    <div className="flex flex-wrap gap-2 items-center">
                        <HeaderChip label="Data source" value="Fitbit" />
                        <HeaderChip label="View" value="Overview" />
                        {!loading && (
                            isAuthenticated ? (
                                <button
                                    onClick={handleLogout}
                                    className="ml-2 rounded-xl border border-slate-800 bg-slate-900/50 px-4 py-2 text-sm font-medium text-slate-300 hover:bg-slate-800 hover:text-slate-100 transition-colors"
                                >
                                    Logout
                                </button>
                            ) : (
                                <button
                                    onClick={handleLogin}
                                    className="ml-2 rounded-xl bg-sky-500 px-4 py-2 text-sm font-semibold text-white hover:bg-sky-400 transition-colors shadow-lg shadow-sky-500/20"
                                >
                                    Login with Fitbit
                                </button>
                            )
                        )}
                    </div>
                </header>

                <div className="mt-8">
                    {loading && <div className="text-slate-300">Loading profile…</div>}

                    {!loading && error && (
                        <div className="rounded-2xl border border-red-900/50 bg-red-950/30 p-5 text-red-200">
                            {error}
                        </div>
                    )}
                </div>

                {!loading && !error && isAuthenticated && profile && (
                    <>
                        <div className="mt-8 grid grid-cols-1 gap-4 lg:grid-cols-12 lg:items-center">
                            <div className="order-2 lg:order-1 lg:col-span-5 xl:col-span-4">
                                <DateBarCompact selectedDate={selectedDate} readableDate={readableDate} onChange={setSelectedDate} />
                            </div>

                            <div className="order-1 lg:order-2 lg:col-span-7 xl:col-span-8">
                                <ProfileCompactBarCompact profile={profile} />
                            </div>
                        </div>

                        <DashboardContent
                            selectedDate={selectedDate}
                            range={range}
                            setRange={setRange}
                            weightRange={weightRange}
                            setWeightRange={setWeightRange}
                            layouts={layouts}
                            onLayoutChange={onLayoutChange}
                        />
                    </>
                )}

                {!loading && !isAuthenticated && (
                    <div className="mt-20 flex flex-col items-center justify-center text-center">
                        <div className="max-w-md">
                            <h2 className="text-3xl font-bold text-slate-100">Welcome to Fitdata</h2>
                            <p className="mt-4 text-slate-400">
                                Connect your Fitbit account to visualize your health data, track your workouts, and monitor your sleep patterns in a beautiful dashboard.
                            </p>
                            <button
                                onClick={handleLogin}
                                className="mt-8 rounded-2xl bg-sky-500 px-8 py-4 text-lg font-bold text-white hover:bg-sky-400 transition-all shadow-xl shadow-sky-500/25 hover:scale-105 active:scale-95"
                            >
                                Get Started with Fitbit
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </main>
    );
}

function DateBarCompact({
                            selectedDate,
                            readableDate,
                            onChange,
                        }: {
    selectedDate: string;
    readableDate: string;
    onChange: (next: string) => void;
}) {
    const onPrev = () => onChange(addDaysIso(selectedDate, -1));
    const onNext = () => onChange(addDaysIso(selectedDate, 1));

    return (
        <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4 shadow-sm backdrop-blur">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div className="min-w-0">
                    <div className="text-xs text-slate-400">Dashboard date</div>
                    <div className="truncate text-base font-semibold text-slate-100">{readableDate}</div>
                </div>

                <div className="flex items-center gap-2">
                    <button
                        type="button"
                        onClick={onPrev}
                        className="rounded-xl border border-slate-800 bg-slate-950/35 px-3 py-2 text-sm text-slate-100 hover:bg-slate-950/55"
                    >
                        ←
                    </button>

                    <input
                        type="date"
                        value={selectedDate}
                        onChange={(e) => onChange(e.target.value)}
                        className="rounded-xl border border-slate-800 bg-slate-950/35 px-3 py-2 text-sm text-slate-100 outline-none"
                    />

                    <button
                        type="button"
                        onClick={onNext}
                        className="rounded-xl border border-slate-800 bg-slate-950/35 px-3 py-2 text-sm text-slate-100 hover:bg-slate-950/55"
                    >
                        →
                    </button>
                </div>
            </div>
        </div>
    );
}

function ProfileCompactBarCompact({ profile }: { profile: UserProfileDto }) {
    return (
        <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4 shadow-sm backdrop-blur">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div className="min-w-0">
                    <div className="text-xs text-slate-400">Connected user</div>
                    <div className="truncate text-base font-semibold text-slate-100">{profile.displayName}</div>
                </div>

                <div className="flex flex-wrap gap-2">
                    <Pill label="User ID" value={profile.id} />
                    <Pill label="Age" value={String(profile.age)} />
                    <Pill label="Gender" value={profile.gender} />
                </div>
            </div>
        </div>
    );
}

function Pill({ label, value }: { label: string; value: string }) {
    return (
        <div className="inline-flex items-center gap-2 rounded-full border border-slate-800 bg-slate-950/25 px-3 py-1.5">
            <span className="text-[11px] text-slate-400">{label}</span>
            <span className="text-xs font-semibold text-slate-100">{value}</span>
        </div>
    );
}

function HeaderChip({ label, value }: { label: string; value: string }) {
    return (
        <div className="inline-flex items-center gap-2 rounded-full border border-slate-800 bg-slate-950/30 px-3.5 py-2">
            <span className="text-[11px] text-slate-400">{label}</span>
            <span className="text-sm font-semibold text-slate-100">{value}</span>
        </div>
    );
}

function toIsoDate(d: Date) {
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, "0");
    const dd = String(d.getDate()).padStart(2, "0");
    return `${yyyy}-${mm}-${dd}`;
}

function addDaysIso(iso: string, delta: number) {
    const d = new Date(`${iso}T00:00:00`);
    d.setDate(d.getDate() + delta);
    return toIsoDate(d);
}

function formatReadable(iso: string) {
    const d = new Date(`${iso}T00:00:00`);
    return d.toLocaleDateString(undefined, { weekday: "short", year: "numeric", month: "short", day: "2-digit" });
}
