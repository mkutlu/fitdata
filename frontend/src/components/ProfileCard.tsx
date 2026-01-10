import type { UserProfileDto } from "../api/profileApi";

type Props = {
    profile: UserProfileDto;
};

function initials(name: string) {
    const parts = name.trim().split(/\s+/).filter(Boolean);
    const a = parts[0]?.[0] ?? "?";
    const b = parts.length > 1 ? parts[parts.length - 1][0] : "";
    return (a + b).toUpperCase();
}

export function ProfileCard({ profile }: Props) {
    const displayName = profile.displayName || "Unknown";
    const avatarText = initials(displayName);

    return (
        <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4 shadow-sm backdrop-blur">
            <div className="flex items-center justify-between gap-3">
                <div className="flex items-center gap-3 min-w-0">
                    <div className="h-10 w-10 shrink-0 rounded-xl border border-slate-800 bg-slate-950/40 grid place-items-center text-sm font-semibold text-slate-100">
                        {avatarText}
                    </div>

                    <div className="min-w-0">
                        <div className="text-xs text-slate-400">Profile</div>
                        <div className="truncate text-base font-semibold text-slate-100">
                            {displayName}
                        </div>
                    </div>
                </div>

                <span className="shrink-0 rounded-full border border-slate-800 bg-slate-950/30 px-2.5 py-1 text-xs font-medium text-slate-200">
          {profile.gender}
        </span>
            </div>

            <div className="mt-3 flex flex-wrap gap-2">
                <Chip label="Age" value={String(profile.age)} />
                <Chip label="User ID" value={profile.id} />
            </div>
        </div>
    );
}

function Chip({ label, value }: { label: string; value: string }) {
    return (
        <div className="inline-flex items-center gap-2 rounded-full border border-slate-800 bg-slate-950/20 px-3 py-1.5">
            <span className="text-[11px] text-slate-400">{label}</span>
            <span className="text-xs font-semibold text-slate-100">{value}</span>
        </div>
    );
}
