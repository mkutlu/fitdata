import { useEffect, useState } from "react";
import { StepsChartCard } from "../components/StepsChartCard";
import { fetchProfile, type UserProfileDto } from "../api/profileApi";
import fitdataLogo from "../assets/fitdata-logo.png";

export function Dashboard() {
    const [profile, setProfile] = useState<UserProfileDto | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let alive = true;

        (async () => {
            try {
                const data = await fetchProfile();
                if (!alive) return;
                setProfile(data);
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
    }, []);

    return (
        <main className="min-h-screen text-slate-100 bg-slate-950">
            <div className="pointer-events-none fixed inset-0 bg-[radial-gradient(1400px_circle_at_15%_0%,rgba(56,189,248,0.14),transparent_58%),radial-gradient(1100px_circle_at_85%_20%,rgba(99,102,241,0.12),transparent_58%)]" />
            <div className="pointer-events-none fixed inset-0 opacity-[0.07] [background-image:linear-gradient(to_right,rgba(148,163,184,0.18)_1px,transparent_1px),linear-gradient(to_bottom,rgba(148,163,184,0.18)_1px,transparent_1px)] [background-size:72px_72px]" />

            <div className="relative mx-auto w-full max-w-[1520px] px-4 sm:px-6 lg:px-10 py-10">
                <header className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
                    <div className="flex items-center gap-4">
                        <img
                            src={fitdataLogo}
                            alt="Fitdata"
                            className="h-12 sm:h-14 lg:h-16 drop-shadow-[0_0_18px_rgba(56,189,248,0.35)]"
                        />
                        <div className="flex flex-col">
                            <div className="text-lg sm:text-xl font-semibold tracking-tight">Fitbit</div>
                            <div className="text-sm text-slate-300">Dashboard (MVP)</div>
                        </div>
                    </div>

                    <div className="flex flex-wrap gap-2">
                        <HeaderChip label="Data source" value="Fitbit" />
                        <HeaderChip label="View" value="Overview" />
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

                {!loading && !error && profile && (
                    <>
                        <div className="mt-8">
                            <ProfileCompactBar profile={profile} />
                        </div>

                        <div className="mt-6 grid grid-cols-1 gap-5 lg:grid-cols-12 lg:items-start">
                            <div className="lg:col-span-8 xl:col-span-9">
                                <StepsChartCard />
                            </div>

                            <div className="lg:col-span-4 xl:col-span-3 space-y-5">
                                <MiniStatCard title="User" value={profile.displayName} subtitle={`ID ${profile.id}`} />
                                <MiniStatCard title="Gender" value={profile.gender} subtitle={`Age ${profile.age}`} />
                                <PlaceholderCard title="Next" subtitle="Sleep / HR / Calories cards will live here" />
                            </div>
                        </div>
                    </>
                )}
            </div>
        </main>
    );
}

function ProfileCompactBar({ profile }: { profile: UserProfileDto }) {
    return (
        <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-6 shadow-sm backdrop-blur">
            <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
                <div className="min-w-0">
                    <div className="text-xs text-slate-400">Connected user</div>
                    <div className="mt-1 truncate text-xl font-semibold text-slate-100">{profile.displayName}</div>
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
        <div className="inline-flex items-center gap-2 rounded-full border border-slate-800 bg-slate-950/25 px-3.5 py-2">
            <span className="text-[11px] text-slate-400">{label}</span>
            <span className="text-sm font-semibold text-slate-100">{value}</span>
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

function MiniStatCard({ title, value, subtitle }: { title: string; value: string; subtitle?: string }) {
    return (
        <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-5 shadow-sm backdrop-blur">
            <div className="text-xs text-slate-400">{title}</div>
            <div className="mt-2 truncate text-lg font-semibold text-slate-100">{value}</div>
            {subtitle && <div className="mt-1 text-sm text-slate-300">{subtitle}</div>}
        </div>
    );
}

function PlaceholderCard({ title, subtitle }: { title: string; subtitle: string }) {
    return (
        <div className="rounded-2xl border border-slate-800 bg-slate-900/20 p-5 shadow-sm backdrop-blur">
            <div className="text-xs text-slate-400">{title}</div>
            <div className="mt-2 text-sm text-slate-300">{subtitle}</div>
        </div>
    );
}
