import { useEffect, useMemo, useState } from "react";
import { ResponsiveGridLayout, useContainerWidth } from "react-grid-layout";
import type { Layout } from "react-grid-layout";
import { StepsChartCard } from "../components/StepsChartCard";
import { WeightChartCard } from "../components/WeightChartCard";
import { HeartRateIntradayCard } from "../components/HeartRateIntradayCard";
import { SleepChartCard } from "../components/SleepChartCard";
import { ReadinessCard } from "../components/ReadinessCard";
import { fetchProfile, type UserProfileDto } from "../api/profileApi";
import { fetchAuthStatus, logout } from "../api/authApi";
import type { StepsRange } from "../api/stepsApi";
import { createSnapshot, fetchSnapshot } from "../api/snapshotApi";
import logo from "../assets/fitdata-logo.png";

const LAYOUT_STORAGE_KEY = "fitdata-dashboard-layout";

const defaultLayouts: any = {
    lg: [
        { i: "readiness", x: 0, y: 0, w: 12, h: 8 },
        { i: "steps", x: 0, y: 8, w: 6, h: 12 },
        { i: "weight", x: 6, y: 8, w: 6, h: 12 },
        { i: "heart", x: 0, y: 20, w: 12, h: 12 },
        { i: "sleep", x: 0, y: 32, w: 12, h: 12 },
        // { i: "live", x: 0, y: 44, w: 4, h: 5 },
    ],
    md: [
        { i: "readiness", x: 0, y: 0, w: 12, h: 8 },
        { i: "steps", x: 0, y: 8, w: 6, h: 12 },
        { i: "weight", x: 6, y: 8, w: 6, h: 12 },
        { i: "heart", x: 0, y: 20, w: 12, h: 12 },
        { i: "sleep", x: 0, y: 32, w: 12, h: 12 },
        // { i: "live", x: 0, y: 44, w: 6, h: 5 },
    ],
    sm: [
        { i: "readiness", x: 0, y: 0, w: 6, h: 8 },
        { i: "steps", x: 0, y: 8, w: 6, h: 10 },
        { i: "weight", x: 0, y: 18, w: 6, h: 10 },
        { i: "heart", x: 0, y: 28, w: 6, h: 10 },
        { i: "sleep", x: 0, y: 38, w: 6, h: 10 },
        // { i: "live", x: 0, y: 48, w: 6, h: 5 },
    ],
};

function DashboardContent({selectedDate, range, setRange, weightRange, setWeightRange, layouts, onLayoutChange, snapshotData }: any) {
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
                    onLayoutChange={onLayoutChange}
                >
                    <div key="readiness">
                        <div className="h-full w-full flex flex-col">
                            <div className="drag-handle h-6 w-full cursor-move bg-slate-800/20 hover:bg-slate-800/40 rounded-t-2xl flex items-center justify-center">
                                <div className="w-8 h-1 bg-slate-700 rounded-full" />
                            </div>
                            <div className="flex-1 overflow-hidden">
                                <ReadinessCard baseDate={selectedDate} initialData={snapshotData?.readiness} />
                            </div>
                        </div>
                    </div>
                    <div key="steps">
                        <div className="h-full w-full flex flex-col">
                            <div className="drag-handle h-6 w-full cursor-move bg-slate-800/20 hover:bg-slate-800/40 rounded-t-2xl flex items-center justify-center">
                                <div className="w-8 h-1 bg-slate-700 rounded-full" />
                            </div>
                            <div className="flex-1 overflow-hidden">
                                <StepsChartCard baseDate={selectedDate} range={range} onRangeChange={setRange} initialData={snapshotData?.steps} />
                            </div>
                        </div>
                    </div>
                    <div key="weight">
                        <div className="h-full w-full flex flex-col">
                            <div className="drag-handle h-6 w-full cursor-move bg-slate-800/20 hover:bg-slate-800/40 rounded-t-2xl flex items-center justify-center">
                                <div className="w-8 h-1 bg-slate-700 rounded-full" />
                            </div>
                            <div className="flex-1 overflow-hidden">
                                <WeightChartCard baseDate={selectedDate} range={weightRange} onRangeChange={setWeightRange} initialData={snapshotData?.weight} />
                            </div>
                        </div>
                    </div>
                    <div key="heart">
                        <div className="h-full w-full flex flex-col">
                            <div className="drag-handle h-6 w-full cursor-move bg-slate-800/20 hover:bg-slate-800/40 rounded-t-2xl flex items-center justify-center">
                                <div className="w-8 h-1 bg-slate-700 rounded-full" />
                            </div>
                            <div className="flex-1 overflow-hidden">
                                <HeartRateIntradayCard baseDate={selectedDate} initialData={snapshotData?.heartRate} />
                            </div>
                        </div>
                    </div>
                    <div key="sleep">
                        <div className="h-full w-full flex flex-col">
                            <div className="drag-handle h-6 w-full cursor-move bg-slate-800/20 hover:bg-slate-800/40 rounded-t-2xl flex items-center justify-center">
                                <div className="w-8 h-1 bg-slate-700 rounded-full" />
                            </div>
                            <div className="flex-1 overflow-hidden">
                                <SleepChartCard baseDate={selectedDate} initialData={snapshotData?.sleep} />
                            </div>
                        </div>
                    </div>
                    {/* <div key="live">
                        <div className="h-full w-full flex flex-col">
                            <div className="drag-handle h-6 w-full cursor-move bg-slate-800/20 hover:bg-slate-800/40 rounded-t-2xl flex items-center justify-center">
                                <div className="w-8 h-1 bg-slate-700 rounded-full" />
                            </div>
                            <div className="flex-1 overflow-hidden">
                                <LiveWorkoutCard />
                            </div>
                        </div>
                    </div> */}
                </ResponsiveGridLayout>
            )}
        </div>
    );
}

export function Dashboard() {
    const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
    const [profile, setProfile] = useState<UserProfileDto | null>(null);
    const [loading, setLoading] = useState(true);
    const [snapshotData, setSnapshotData] = useState<any>(null);
    const [sharing, setSharing] = useState(false);

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
        const controller = new AbortController();

        (async () => {
            const params = new URLSearchParams(window.location.search);
            const shareId = params.get("share");

            if (shareId) {
                try {
                    const data = await fetchSnapshot(shareId, controller.signal);
                    setSnapshotData(data);
                    setProfile(data.profile.user);
                    setSelectedDate(data.selectedDate);
                    setRange(data.stepsRange);
                    setWeightRange(data.weightRange);
                    setIsAuthenticated(true); // Treat as authenticated for layout purposes
                    setLoading(false);
                    return;
                } catch (e) {
                    console.error("Failed to fetch snapshot", e);
                    // Fallback to normal auth check
                }
            }

            try {
                const auth = await fetchAuthStatus(controller.signal);
                setIsAuthenticated(auth.authenticated);

                if (auth.authenticated) {
                    const data = await fetchProfile(controller.signal);
                    setProfile(data);
                } else {
                    // Reset layouts when not authenticated to ensure clean state on next login
                    setLayouts(JSON.parse(JSON.stringify(defaultLayouts)));
                }
            } catch (err) {
                if (err instanceof Error && err.name === "AbortError") return;
                console.error("Auth/Profile fetch failed", err);
                const errorMessage = err instanceof Error ? err.message : String(err);
                
                // If it's a network error/timeout (not a deliberate 401), we might still be authenticated
                // but just couldn't reach the server. Let's not reset the whole UI to login screen immediately
                // unless it's clearly a "not authorized" case.
                const isAuthError = errorMessage.includes("401") || errorMessage.includes("No Fitbit token") || errorMessage.includes("Unauthorized");
                
                if (isAuthError) {
                    setIsAuthenticated(false);
                    setLayouts(JSON.parse(JSON.stringify(defaultLayouts)));
                } else {
                    // It's likely a timeout or server error. 
                    // We don't change isAuthenticated, keeping current (false) but NOT showing error
                    // so the page is not blocked and user can see the login button.
                    setIsAuthenticated(false);
                    console.error("Status check failed or timed out:", errorMessage);
                }
            } finally {
                setLoading(false);
            }
        })();

        return () => {
            controller.abort();
        };
    }, []);

    const handleLogin = () => {
        const API_BASE_URL = (window as any).ENV?.VITE_API_BASE_URL !== "__VITE_API_BASE_URL__"
            ? (window as any).ENV?.VITE_API_BASE_URL
            : (import.meta.env.VITE_API_BASE_URL || "");
        window.location.href = `${API_BASE_URL}/oauth/fitbit/start`;
    };

    const handleLogout = async () => {
        try {
            await logout();
            setIsAuthenticated(false);
            setProfile(null);
            setSnapshotData(null);
            // Reset layouts to default on logout
            setLayouts(JSON.parse(JSON.stringify(defaultLayouts)));
            // Remove share param if present
            const url = new URL(window.location.href);
            url.searchParams.delete("share");
            window.history.replaceState({}, "", url.toString());
        } catch (e) {
            console.error("Logout failed", e);
        }
    };

    const handlePrint = () => {
        window.print();
    };

    const handleShare = async () => {
        if (sharing) return;
        setSharing(true);
        try {
            const uuid = await createSnapshot({
                selectedDate,
                stepsRange: range,
                weightRange
            });
            const url = new URL(window.location.href);
            url.searchParams.set("share", uuid);
            await navigator.clipboard.writeText(url.toString());
            alert("Shareable link copied to clipboard!");
        } catch (e) {
            console.error("Sharing failed", e);
            alert("Failed to create share link.");
        } finally {
            setSharing(false);
        }
    };

    return (
        <main className="min-h-screen text-slate-100 bg-slate-950">
            <div className="pointer-events-none fixed inset-0 bg-[radial-gradient(1400px_circle_at_15%_0%,rgba(56,189,248,0.14),transparent_58%),radial-gradient(1100px_circle_at_85%_20%,rgba(99,102,241,0.12),transparent_58%)]" />
            <div className="pointer-events-none fixed inset-0 opacity-[0.07] [background-image:linear-gradient(to_right,rgba(148,163,184,0.18)_1px,transparent_1px),linear-gradient(to_bottom,rgba(148,163,184,0.18)_1px,transparent_1px)] [background-size:72px_72px]" />

            <div className="relative mx-auto w-full max-w-[1520px] px-4 sm:px-6 lg:px-10 py-10">
                <header className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
                    <div className="pl-2.5">
                        <img src={logo} alt="Fitdata" className="h-14 w-auto" />
                    </div>

                    <div className="flex flex-wrap gap-2 items-center">
                        {isAuthenticated && (
                            <div className="flex gap-2 mr-2 no-print">
                                <button
                                    onClick={handleShare}
                                    disabled={sharing || !!snapshotData}
                                    title={!!snapshotData ? "Snapshot view" : "Share Snapshot"}
                                    className={`rounded-xl border border-slate-800 bg-slate-900/50 p-2 text-slate-300 transition-all ${!!snapshotData ? 'opacity-50 cursor-not-allowed' : 'hover:bg-slate-800 hover:text-emerald-400'}`}
                                >
                                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"></path>
                                        <polyline points="16 6 12 2 8 6"></polyline>
                                        <line x1="12" y1="2" x2="12" y2="15"></line>
                                    </svg>
                                </button>
                                <button
                                    onClick={handlePrint}
                                    title="Print Dashboard"
                                    className="rounded-xl border border-slate-800 bg-slate-900/50 p-2 text-slate-300 hover:bg-slate-800 hover:text-sky-400 transition-all"
                                >
                                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <polyline points="6 9 6 2 18 2 18 9"></polyline>
                                        <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"></path>
                                        <rect x="6" y="14" width="12" height="8"></rect>
                                    </svg>
                                </button>
                            </div>
                        )}
                        <HeaderChip label="Data source" value={snapshotData ? "Snapshot" : "Fitbit"} />
                        <HeaderChip label="View" value={snapshotData ? "Historical" : "Overview"} />
                        {!loading && (
                            isAuthenticated ? (
                                <button
                                    onClick={handleLogout}
                                    className="ml-2 rounded-xl border border-slate-800 bg-slate-900/50 px-4 py-2 text-sm font-medium text-slate-300 hover:bg-slate-800 hover:text-slate-100 transition-colors"
                                >
                                    {snapshotData ? "Exit Snapshot" : "Logout"}
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
                    {snapshotData && (
                        <div className="mb-4 inline-flex items-center gap-2 rounded-lg bg-emerald-500/10 border border-emerald-500/20 px-4 py-2 text-emerald-400 text-sm no-print">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="12" y1="16" x2="12" y2="12"></line>
                                <line x1="12" y1="8" x2="12.01" y2="8"></line>
                            </svg>
                            You are viewing a shared snapshot from {new Date(snapshotData.selectedDate).toLocaleDateString()}.
                        </div>
                    )}
                </div>

                {!loading && isAuthenticated && profile && (
                    <>
                        <div className="mt-8 grid grid-cols-1 gap-4 lg:grid-cols-12 lg:items-center no-print">
                            <div className="order-2 lg:order-1 lg:col-span-6">
                                {snapshotData ? (
                                    <div className="rounded-2xl border border-slate-800 bg-slate-900/40 px-6 py-4">
                                        <div className="text-xs text-slate-400 uppercase tracking-widest font-semibold">Snapshot Date</div>
                                        <div className="mt-1 text-xl font-bold text-slate-100">{readableDate}</div>
                                    </div>
                                ) : (
                                    <DateBarCompact selectedDate={selectedDate} readableDate={readableDate} onChange={setSelectedDate} />
                                )}
                            </div>

                            <div className="order-1 lg:order-2 lg:col-span-6">
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
                            snapshotData={snapshotData}
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
