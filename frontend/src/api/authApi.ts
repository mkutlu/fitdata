export type AuthStatus = {
    authenticated: boolean;
};

export async function fetchAuthStatus(): Promise<AuthStatus> {
    const res = await fetch("/oauth/fitbit/status");
    if (!res.ok) {
        throw new Error("Failed to fetch auth status");
    }
    return res.json();
}

export async function logout(): Promise<void> {
    const res = await fetch("/oauth/fitbit/logout");
    if (!res.ok) {
        throw new Error("Logout failed");
    }
}
