import { fetchWithRetry } from "./fetchUtils";

export type AuthStatus = {
    authenticated: boolean;
};

export async function fetchAuthStatus(signal?: AbortSignal): Promise<AuthStatus> {
    const res = await fetchWithRetry("/oauth/fitbit/status", { signal });
    if (!res.ok) {
        throw new Error("Failed to fetch auth status");
    }
    return res.json();
}

export async function logout(): Promise<void> {
    const res = await fetchWithRetry("/oauth/fitbit/logout");
    if (!res.ok) {
        throw new Error("Logout failed");
    }
}
