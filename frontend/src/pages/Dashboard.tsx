import { useEffect, useState } from "react";
import { fetchProfile, type UserProfileDto } from "../api/profileApi";
import { ProfileCard } from "../components/ProfileCard";

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
        <main className="min-h-screen bg-slate-950 text-slate-100">
            <div className="mx-auto max-w-3xl p-8">
                <h1 className="text-3xl font-semibold">Fitdata</h1>
                <p className="mt-2 text-slate-300">Dashboard (MVP)</p>

                <div className="mt-8">
                    {loading && <div className="text-slate-300">Loading profile…</div>}

                    {!loading && error && (
                        <div className="rounded-xl border border-red-900/50 bg-red-950/30 p-4 text-red-200">
                            {error}
                        </div>
                    )}

                    {!loading && !error && profile && <ProfileCard profile={profile} />}
                </div>
            </div>
        </main>
    );
}
